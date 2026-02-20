package com.example.sdlc.mapping.persistence.jpa;

import com.example.sdlc.mapping.persistence.entity.MethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MethodJpaRepository extends JpaRepository<MethodEntity, Long> {
    List<MethodEntity> findByIdIn(List<Long> ids);
}
