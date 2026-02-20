package com.example.sdlc.mapping.service;

import com.example.sdlc.mapping.model.*;
import com.example.sdlc.mapping.repository.MappingRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MappingService {

    private final MappingRepository repository;
    private final RuleEvaluator ruleEvaluator;

    public MappingService(MappingRepository repository, RuleEvaluator ruleEvaluator) {
        this.repository = repository;
        this.ruleEvaluator = ruleEvaluator;
    }

    public MappingRecommendResponse recommend(MappingRecommendRequest req) {
        Stage stage = repository.findStageByCode(req.getStageCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid stageCode: " + req.getStageCode()));

        Map<String, Object> ctx = req.getContext() == null ? new HashMap<>() : req.getContext();
        Trace trace = new Trace();

        List<ScoredItem> paradigms = repository.findStageParadigmMaps(stage.id()).stream()
                .filter(m -> evalOrDrop(m.conditionExpr(), ctx, trace, "stage_paradigm_map:" + m.id()))
                .map(m -> scoreItem("PARADIGM", m.paradigmId(), m.priority(), ctx, null))
                .sorted(Comparator.comparingDouble(ScoredItem::score).reversed())
                .limit(req.limitParadigms())
                .toList();

        Set<Long> paradigmIds = paradigms.stream().map(ScoredItem::refId).collect(Collectors.toSet());

        List<ScoredItem> metas = repository.findParadigmMetaMaps(paradigmIds).stream()
                .filter(m -> evalOrDrop(m.conditionExpr(), ctx, trace, "paradigm_meta_map:" + m.id()))
                .map(m -> scoreItem("META", m.metaId(), m.priority(), ctx, parentScore(paradigms, m.paradigmId())))
                .collect(Collectors.toMap(ScoredItem::refId, s -> s, this::keepHigherScore))
                .values().stream()
                .sorted(Comparator.comparingDouble(ScoredItem::score).reversed())
                .limit(req.limitMetaMethodologies())
                .toList();

        Set<Long> metaIds = metas.stream().map(ScoredItem::refId).collect(Collectors.toSet());

        List<ScoredItem> methodologies = repository.findMetaMethodologyMaps(metaIds).stream()
                .filter(m -> evalOrDrop(m.conditionExpr(), ctx, trace, "meta_methodology_map:" + m.id()))
                .map(m -> scoreItem("METHODOLOGY", m.methodologyId(), m.priority(), ctx, parentScore(metas, m.metaId())))
                .collect(Collectors.toMap(ScoredItem::refId, s -> s, this::keepHigherScore))
                .values().stream()
                .sorted(Comparator.comparingDouble(ScoredItem::score).reversed())
                .limit(req.limitMethodologies())
                .toList();

        Set<Long> methodologyIds = methodologies.stream().map(ScoredItem::refId).collect(Collectors.toSet());

        List<MethodPick> methodPicks = repository.findMethodologyMethodMaps(methodologyIds).stream()
                .filter(m -> evalOrDrop(m.conditionExpr(), ctx, trace, "methodology_method_map:" + m.id()))
                .map(m -> {
                    double parent = parentScore(methodologies, m.methodologyId());
                    double score = (1000.0 - 100.0) + parent * 0.15 + (m.requiredFlag() ? 30.0 : 0.0);
                    return new MethodPick(m.methodId(), m.methodologyId(), m.seqNo(), m.requiredFlag(), score);
                })
                .sorted(Comparator.comparingDouble(MethodPick::score).reversed())
                .limit(req.limitMethods())
                .toList();

        MappingRecommendResponse resp = new MappingRecommendResponse();
        resp.setStage(req.getStageCode());
        resp.setRecommendedParadigms(repository.findParadigms(paradigms.stream().map(ScoredItem::refId).toList()));
        resp.setRecommendedMetaMethodologies(repository.findMetaMethodologies(metas.stream().map(ScoredItem::refId).toList()));
        resp.setRecommendedMethodologies(repository.findMethodologies(methodologies.stream().map(ScoredItem::refId).toList()));
        resp.setRecommendedMethods(repository.findMethods(methodPicks.stream().map(MethodPick::methodId).toList()));
        resp.setTrace(trace);
        return resp;
    }

    private boolean evalOrDrop(String expr, Map<String, Object> ctx, Trace trace, String ruleId) {
        boolean pass = ruleEvaluator.evaluate(expr, ctx);
        if (pass) {
            trace.getAppliedRules().add(ruleId);
        } else {
            trace.getDroppedByCondition().add(ruleId);
        }
        return pass;
    }

    private ScoredItem scoreItem(String type, Long refId, int priority, Map<String, Object> ctx, Double parentScore) {
        double score = 1000.0 - priority;
        if ("L3".equals(ctx.get("complianceLevel"))) {
            score += 20.0;
        }
        if ("tight".equals(ctx.get("timelineLevel"))) {
            score -= 10.0;
        }
        if (parentScore != null) {
            score += parentScore * 0.2;
        }
        return new ScoredItem(type, refId, score);
    }

    private double parentScore(List<ScoredItem> parentItems, Long parentId) {
        return parentItems.stream()
                .filter(i -> i.refId().equals(parentId))
                .mapToDouble(ScoredItem::score)
                .findFirst()
                .orElse(0.0);
    }

    private ScoredItem keepHigherScore(ScoredItem a, ScoredItem b) {
        return a.score() >= b.score() ? a : b;
    }

    private record ScoredItem(String type, Long refId, double score) {
    }

    private record MethodPick(Long methodId, Long methodologyId, int seqNo, boolean required, double score) {
    }
}
