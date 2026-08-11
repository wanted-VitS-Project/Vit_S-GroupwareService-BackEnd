package com.group3.vitamins.project.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.application.policy.ProjectAccessPolicy;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.domain.repository.ProjectMemberRepository;
import com.group3.vitamins.project.domain.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectAccessService implements ProjectAccessUseCase {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectAccessPolicy projectAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public MemberPermission requireAccess(Long projectId, String requesterUserId, String role) {
        requireProjectExists(projectId);
        return projectAccessPolicy.resolvePermission(role, findPermission(projectId, requesterUserId));
    }

    @Override
    public void requireEditable(Long projectId, String requesterUserId, String role) {
        requireProjectExists(projectId);
        projectAccessPolicy.requireEditor(role, findPermission(projectId, requesterUserId));
    }

    /** 존재 확인을 권한 판정보다 먼저 한다 — 404 가 403 보다 앞선다 (상세 조회와 동일 순서). */
    private void requireProjectExists(Long projectId) {
        if (!existsInCurrentCompany(projectId)) {
            throw new NotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND);
        }
    }

    /** 참여자 행이 없으면 null 을 넘긴다 — 판정은 정책이 한다. */
    private MemberPermission findPermission(Long projectId, String requesterUserId) {
        return projectMemberRepository.findPermission(projectId, requesterUserId).orElse(null);
    }

    @Override
    public MemberPermission resolvePermission(Long projectId, String requesterUserId, String role) {
        if (!existsInCurrentCompany(projectId)) {
            return MemberPermission.NONE;
        }
        return projectAccessPolicy.resolvePermissionOrNone(
                role, findPermission(projectId, requesterUserId));
    }

    /** 현재 로그인 회사가 소유한, 논리 삭제되지 않은 프로젝트인지 확인한다. */
    private boolean existsInCurrentCompany(Long projectId) {
        return projectRepository
                .findById(projectId, currentCompanyIdProvider.currentCompanyId())
                .isPresent();
    }
}
