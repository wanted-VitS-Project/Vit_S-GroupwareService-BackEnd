package com.group3.vitamins.project.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectMemberRepository extends JpaRepository<ProjectMemberJpaEntity, Long> {
}