package com.group3.vitamins.approval.application.policy;

import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.BlockSummary;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalBlockEligibilityPolicy {

    private static final String APPROVAL_BLOCK_TYPE = "APPROVAL";

    private final BlockCatalogPort blockCatalogPort;

    /** APR-001: block 존재(404) + type == APPROVAL(400) 확인 */
    public BlockSummary getApprovalBlockOrThrow(Long blockId) {
        BlockSummary block = blockCatalogPort.findBlock(blockId)
                .orElseThrow(() -> {
                    log.warn("결재 블록 생성 - 블록 없음 blockId={}", blockId);
                    return new NotFoundException(ApprovalErrorCode.BLOCK_NOT_FOUND);
                });

        if (!APPROVAL_BLOCK_TYPE.equals(block.type())) {
            log.warn("결재 블록 생성 - 타입 불일치 blockId={}, type={}", blockId, block.type());
            throw new ValidationException(ApprovalErrorCode.BLOCK_TYPE_MISMATCH);
        }
        return block;
    }

    /**
     * `approval.md` 1번 요구사항(BND-001) · `PERMISSION.md` §6 프로젝트 진입 판정 — 요청자가 해당
     * 프로젝트 member 인지 확인(403). `APR-012`는 결재선 결재자 자격 검증(#6 엔드포인트)이라 다른 요구사항이다.
     */
    public void assertProjectMember(Long projectId, String userId) {
        if (!blockCatalogPort.isProjectMember(projectId, userId)) {
            log.warn("결재 블록 생성 - 프로젝트 member 아님 projectId={}, userId={}", projectId, userId);
            throw new ForbiddenException(ApprovalErrorCode.APPROVAL_NOT_PROJECT_MEMBER);
        }
    }
}
