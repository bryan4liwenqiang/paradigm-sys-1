package com.example.sdlc.mapping.persistence.jpa;

import com.example.sdlc.mapping.persistence.entity.MetaMethodologyMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MetaMethodologyMapJpaRepository extends JpaRepository<MetaMethodologyMapEntity, Long> {
    List<MetaMethodologyMapEntity> findByMetaMethodologyIdInAndEnabledTrueOrderByPriorityAsc(Collection<Long> metaIds);
}
