package com.example.sdlc.mapping.persistence.jpa;

import com.example.sdlc.mapping.persistence.entity.ParadigmMetaMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ParadigmMetaMapJpaRepository extends JpaRepository<ParadigmMetaMapEntity, Long> {
    List<ParadigmMetaMapEntity> findByParadigmIdInAndEnabledTrueOrderByPriorityAsc(Collection<Long> paradigmIds);
}
