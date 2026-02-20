package com.example.sdlc.mapping.persistence.jpa;

import com.example.sdlc.mapping.persistence.entity.StageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StageJpaRepository extends JpaRepository<StageEntity, Long> {
    Optional<StageEntity> findByCode(String code);
}
