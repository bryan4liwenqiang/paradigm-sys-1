package com.example.sdlc.mapping.persistence.jpa;

import com.example.sdlc.mapping.persistence.entity.StageParadigmMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StageParadigmMapJpaRepository extends JpaRepository<StageParadigmMapEntity, Long> {
    List<StageParadigmMapEntity> findByStageIdAndEnabledTrueOrderByPriorityAsc(Long stageId);
}
