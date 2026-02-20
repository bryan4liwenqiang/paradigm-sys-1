package com.example.sdlc.mapping.persistence.jpa;

import com.example.sdlc.mapping.persistence.entity.MethodologyMethodMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MethodologyMethodMapJpaRepository extends JpaRepository<MethodologyMethodMapEntity, Long> {
    List<MethodologyMethodMapEntity> findByMethodologyIdInAndEnabledTrueOrderByMethodologyIdAscSeqNoAsc(Collection<Long> methodologyIds);
}
