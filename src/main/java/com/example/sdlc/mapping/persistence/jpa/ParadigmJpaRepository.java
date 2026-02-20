package com.example.sdlc.mapping.persistence.jpa;

import com.example.sdlc.mapping.persistence.entity.ParadigmEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParadigmJpaRepository extends JpaRepository<ParadigmEntity, Long> {
    List<ParadigmEntity> findByIdIn(List<Long> ids);
}
