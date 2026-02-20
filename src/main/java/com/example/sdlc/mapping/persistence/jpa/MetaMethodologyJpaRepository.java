package com.example.sdlc.mapping.persistence.jpa;

import com.example.sdlc.mapping.persistence.entity.MetaMethodologyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetaMethodologyJpaRepository extends JpaRepository<MetaMethodologyEntity, Long> {
    List<MetaMethodologyEntity> findByIdIn(List<Long> ids);
}
