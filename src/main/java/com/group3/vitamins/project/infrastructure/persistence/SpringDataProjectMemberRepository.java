package com.group3.vitamins.project.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataProjectMemberRepository extends JpaRepository<ProjectMemberJpaEntity, Long> {

    Optional<ProjectMemberJpaEntity> findByProjectIdAndUserId(Long projectId, String userId);

    void deleteByProjectId(Long projectId);
}