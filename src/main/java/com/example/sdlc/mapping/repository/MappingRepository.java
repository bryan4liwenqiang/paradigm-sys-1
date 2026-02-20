package com.example.sdlc.mapping.repository;

import com.example.sdlc.mapping.model.NamedRef;
import com.example.sdlc.mapping.model.Stage;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MappingRepository {
    Optional<Stage> findStageByCode(String stageCode);

    List<StageParadigmMapRow> findStageParadigmMaps(Long stageId);

    List<ParadigmMetaMapRow> findParadigmMetaMaps(Set<Long> paradigmIds);

    List<MetaMethodologyMapRow> findMetaMethodologyMaps(Set<Long> metaIds);

    List<MethodologyMethodMapRow> findMethodologyMethodMaps(Set<Long> methodologyIds);

    List<NamedRef> findParadigms(List<Long> ids);

    List<NamedRef> findMetaMethodologies(List<Long> ids);

    List<NamedRef> findMethodologies(List<Long> ids);

    List<NamedRef> findMethods(List<Long> ids);

    record StageParadigmMapRow(Long id, Long paradigmId, int priority, String conditionExpr) {
    }

    record ParadigmMetaMapRow(Long id, Long paradigmId, Long metaId, int priority, String conditionExpr) {
    }

    record MetaMethodologyMapRow(Long id, Long metaId, Long methodologyId, int priority, String conditionExpr) {
    }

    record MethodologyMethodMapRow(
            Long id, Long methodologyId, Long methodId, int seqNo, boolean requiredFlag, String conditionExpr
    ) {
    }
}
