package com.group3.vitamins.approval.infrastructure.catalog;

import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.BlockSummary;
import com.group3.vitamins.project.block.domain.repository.BlockRepository;
import com.group3.vitamins.project.domain.repository.ProjectRepository;
import com.group3.vitamins.project.domain.repository.ProjectMemberRepository;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.domain.repository.StepPermissionRepository;
import com.group3.vitamins.project.step.domain.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Block/Project 도메인(동훈님 소관) 실 연동. 블록·프로젝트 코드가 실제로 존재하게 된 뒤로는
 * 스텁을 유지할 이유가 없어 정식 리포지토리를 직접 참조한다
 * (`ApprovalEmployeeCatalogAdapter`가 `auth.AuthQueryMapper`를 재사용하는 것과 동일한 패턴).
 */
@Component
@RequiredArgsConstructor
public class ApprovalBlockCatalogAdapter implements BlockCatalogPort {

    private final BlockRepository blockRepository;
    private final StepRepository stepRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final StepPermissionRepository stepPermissionRepository;

    /** {@code block.project_id} 컬럼이 없어(폐기됨) {@code step}을 거쳐야 projectId를 얻는다 */
    @Override
    public Optional<BlockSummary> findBlock(Long blockId) {
        return blockRepository.findById(blockId)
                .flatMap(block -> stepRepository.findById(block.getStepId())
                        .map(step -> new BlockSummary(
                                block.getBlockId(), block.getType().name(), step.getStepId(),
                                step.getProjectId(), block.getCreatedBy())));
    }

    /** 참여자 행이 있으면(권한 레벨 무관) member로 본다 — APR-012는 EDITOR까지 요구하지 않는다 */
    @Override
    public boolean isProjectMember(Long projectId, String userId) {
        return projectMemberRepository.findPermission(projectId, userId).isPresent();
    }

    @Override
    public boolean isBlockInCompany(Long blockId, Long companyId) {
        return findBlock(blockId)
                .flatMap(block -> projectRepository.findById(block.projectId(), companyId))
                .isPresent();
    }

    @Override
    public boolean isStepEditor(Long blockId, String userId, String role) {
        if ("ADMIN".equals(role)) {
            return false;
        }
        if ("MASTER".equals(role)) {
            return true;
        }

        return findBlock(blockId)
                .map(block -> {
                    MemberPermission projectPermission = projectMemberRepository
                            .findPermission(block.projectId(), userId)
                            .orElse(MemberPermission.NONE);
                    if (projectPermission == MemberPermission.NONE) {
                        return false;
                    }
                    MemberPermission effective = stepPermissionRepository
                            .findOverride(block.stepId(), userId)
                            .orElse(projectPermission);
                    return effective == MemberPermission.EDITOR;
                })
                .orElse(false);
    }
}
