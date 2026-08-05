package com.group3.vitamins.text.infrastructure.catalog;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.block.domain.model.Block;
import com.group3.vitamins.project.block.domain.model.BlockType;
import com.group3.vitamins.project.block.domain.repository.BlockRepository;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import com.group3.vitamins.text.application.port.BlockCatalogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogBlockAdapter implements BlockCatalogPort {

    private final BlockRepository blockRepository;
    private final StepAccessUseCase stepAccessUseCase;

    @Override
    @Deprecated
    public boolean hasEditPermission(String blockType, Long blockTypeId, String userId) {
        // 체크리스트가 아직 role 을 안 실어 보내는 예전 호출부용 — 기존 스텁 동작(항상 true) 그대로 유지.
        return true;
    }

    @Override
    public boolean hasEditPermission(String blockType, Long blockTypeId, String userId, String role) {
        return findStepId(blockType, blockTypeId)
                .map(stepId -> isEditable(stepId, userId, role))
                .orElse(false);
    }

    @Override
    public boolean hasViewPermission(String blockType, Long blockTypeId, String userId, String role) {
        return findStepId(blockType, blockTypeId)
                .map(stepId -> isAccessible(stepId, userId, role))
                .orElse(false);
    }

    @Override
    public String getBlockTitle(String blockType, Long blockTypeId) {
        return blockRepository.findByTypeAndTypeId(BlockType.valueOf(blockType), blockTypeId)
                .map(Block::getTitle)
                .orElse(null);
    }

    private Optional<Long> findStepId(String blockType, Long blockTypeId) {
        return blockRepository.findByTypeAndTypeId(BlockType.valueOf(blockType), blockTypeId)
                .map(Block::getStepId);
    }

    /** StepAccessUseCase는 없거나 권한이 없으면 예외를 던진다 — 이 포트는 boolean 계약이라 잡아서 변환한다. */
    private boolean isEditable(Long stepId, String userId, String role) {
        try {
            stepAccessUseCase.requireEditable(stepId, userId, role);
            return true;
        } catch (NotFoundException | ForbiddenException e) {
            return false;
        }
    }

    private boolean isAccessible(Long stepId, String userId, String role) {
        try {
            stepAccessUseCase.requireAccess(stepId, userId, role);
            return true;
        } catch (NotFoundException | ForbiddenException e) {
            return false;
        }
    }
}
