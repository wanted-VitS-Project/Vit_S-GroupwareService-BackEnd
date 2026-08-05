package com.group3.vitamins.project.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataProjectBusinessCategoryRepository
        extends JpaRepository<ProjectBusinessCategoryJpaEntity, Long> {

    List<ProjectBusinessCategoryJpaEntity> findByProjectId(Long projectId);
}