package com.group3.vitamins.project.stage.infrastructure.adapter;

import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.domain.repository.ProjectMemberRepository;
import com.group3.vitamins.project.domain.repository.ProjectRepository;
import com.group3.vitamins.project.stage.application.port.ProjectAccessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 프로젝트 애그리게이트 조회를 스테이지 모듈에 중계한다.
 * 논리 삭제·참여자 조회 SQL 을 복제하지 않기 위해 프로젝트 쪽 포트에 위임한다.
 */
@Component
@RequiredArgsConstructor
public class ProjectAccessAdapter implements ProjectAccessPort {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    public boolean existsProject(Long projectId) {
        return projectRepository.findById(projectId).isPresent();
    }

    @Override
    public Optional<MemberPermission> findPermission(Long projectId, String userId) {
        return projectMemberRepository.findPermission(projectId, userId);
    }
}