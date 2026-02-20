package com.example.sdlc.mapping.persistence.jpa;

import com.example.sdlc.mapping.persistence.entity.MethodologyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MethodologyJpaRepository extends JpaRepository<MethodologyEntity, Long> {
    List<MethodologyEntity> findByIdIn(List<Long> ids);
}
