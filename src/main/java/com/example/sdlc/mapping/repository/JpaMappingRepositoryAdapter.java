package com.example.sdlc.mapping.repository;

import com.example.sdlc.mapping.model.NamedRef;
import com.example.sdlc.mapping.model.Stage;
import com.example.sdlc.mapping.persistence.entity.*;
import com.example.sdlc.mapping.persistence.jpa.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@Primary
public class JpaMappingRepositoryAdapter implements MappingRepository {

    private final StageJpaRepository stageJpaRepository;
    private final StageParadigmMapJpaRepository stageParadigmMapJpaRepository;
    private final ParadigmMetaMapJpaRepository paradigmMetaMapJpaRepository;
    private final MetaMethodologyMapJpaRepository metaMethodologyMapJpaRepository;
    private final MethodologyMethodMapJpaRepository methodologyMethodMapJpaRepository;
    private final ParadigmJpaRepository paradigmJpaRepository;
    private final MetaMethodologyJpaRepository metaMethodologyJpaRepository;
    private final MethodologyJpaRepository methodologyJpaRepository;
    private final MethodJpaRepository methodJpaRepository;

    public JpaMappingRepositoryAdapter(
            StageJpaRepository stageJpaRepository,
            StageParadigmMapJpaRepository stageParadigmMapJpaRepository,
            ParadigmMetaMapJpaRepository paradigmMetaMapJpaRepository,
            MetaMethodologyMapJpaRepository metaMethodologyMapJpaRepository,
            MethodologyMethodMapJpaRepository methodologyMethodMapJpaRepository,
            ParadigmJpaRepository paradigmJpaRepository,
            MetaMethodologyJpaRepository metaMethodologyJpaRepository,
            MethodologyJpaRepository methodologyJpaRepository,
            MethodJpaRepository methodJpaRepository
    ) {
        this.stageJpaRepository = stageJpaRepository;
        this.stageParadigmMapJpaRepository = stageParadigmMapJpaRepository;
        this.paradigmMetaMapJpaRepository = paradigmMetaMapJpaRepository;
        this.metaMethodologyMapJpaRepository = metaMethodologyMapJpaRepository;
        this.methodologyMethodMapJpaRepository = methodologyMethodMapJpaRepository;
        this.paradigmJpaRepository = paradigmJpaRepository;
        this.metaMethodologyJpaRepository = metaMethodologyJpaRepository;
        this.methodologyJpaRepository = methodologyJpaRepository;
        this.methodJpaRepository = methodJpaRepository;
    }

    @Override
    public Optional<Stage> findStageByCode(String stageCode) {
        return stageJpaRepository.findByCode(stageCode)
                .map(e -> new Stage(e.getId(), e.getCode(), e.getName(), e.getOrderNo()));
    }

    @Override
    public List<StageParadigmMapRow> findStageParadigmMaps(Long stageId) {
        return stageParadigmMapJpaRepository.findByStageIdAndEnabledTrueOrderByPriorityAsc(stageId).stream()
                .map(e -> new StageParadigmMapRow(e.getId(), e.getParadigmId(), e.getPriority(), e.getConditionExpr()))
                .toList();
    }

    @Override
    public List<ParadigmMetaMapRow> findParadigmMetaMaps(Set<Long> paradigmIds) {
        if (paradigmIds == null || paradigmIds.isEmpty()) {
            return List.of();
        }
        return paradigmMetaMapJpaRepository.findByParadigmIdInAndEnabledTrueOrderByPriorityAsc(paradigmIds).stream()
                .map(e -> new ParadigmMetaMapRow(
                        e.getId(), e.getParadigmId(), e.getMetaMethodologyId(), e.getPriority(), e.getConditionExpr()))
                .toList();
    }

    @Override
    public List<MetaMethodologyMapRow> findMetaMethodologyMaps(Set<Long> metaIds) {
        if (metaIds == null || metaIds.isEmpty()) {
            return List.of();
        }
        return metaMethodologyMapJpaRepository.findByMetaMethodologyIdInAndEnabledTrueOrderByPriorityAsc(metaIds).stream()
                .map(e -> new MetaMethodologyMapRow(
                        e.getId(), e.getMetaMethodologyId(), e.getMethodologyId(), e.getPriority(), e.getConditionExpr()))
                .toList();
    }

    @Override
    public List<MethodologyMethodMapRow> findMethodologyMethodMaps(Set<Long> methodologyIds) {
        if (methodologyIds == null || methodologyIds.isEmpty()) {
            return List.of();
        }
        return methodologyMethodMapJpaRepository.findByMethodologyIdInAndEnabledTrueOrderByMethodologyIdAscSeqNoAsc(methodologyIds).stream()
                .map(e -> new MethodologyMethodMapRow(
                        e.getId(), e.getMethodologyId(), e.getMethodId(), e.getSeqNo(), e.getRequiredFlag(), e.getConditionExpr()))
                .toList();
    }

    @Override
    public List<NamedRef> findParadigms(List<Long> ids) {
        return mapAndKeepInputOrder(ids, paradigmJpaRepository::findByIdIn, e -> new NamedRef(e.getId(), e.getCode(), e.getName()));
    }

    @Override
    public List<NamedRef> findMetaMethodologies(List<Long> ids) {
        return mapAndKeepInputOrder(ids, metaMethodologyJpaRepository::findByIdIn, e -> new NamedRef(e.getId(), e.getCode(), e.getName()));
    }

    @Override
    public List<NamedRef> findMethodologies(List<Long> ids) {
        return mapAndKeepInputOrder(ids, methodologyJpaRepository::findByIdIn, e -> new NamedRef(e.getId(), e.getCode(), e.getName()));
    }

    @Override
    public List<NamedRef> findMethods(List<Long> ids) {
        return mapAndKeepInputOrder(ids, methodJpaRepository::findByIdIn, e -> new NamedRef(e.getId(), e.getCode(), e.getName()));
    }

    private <T> List<NamedRef> mapAndKeepInputOrder(
            List<Long> ids,
            Function<List<Long>, List<T>> finder,
            Function<T, NamedRef> mapper
    ) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> uniqueIds = ids.stream().distinct().toList();
        Map<Long, NamedRef> byId = finder.apply(uniqueIds).stream()
                .map(mapper)
                .collect(Collectors.toMap(NamedRef::id, Function.identity()));
        return uniqueIds.stream().map(byId::get).filter(Objects::nonNull).toList();
    }
}
