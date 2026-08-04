package com.group3.vitamins.project.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectBusinessCategoryRepository
        extends JpaRepository<ProjectBusinessCategoryJpaEntity, Long> {
}