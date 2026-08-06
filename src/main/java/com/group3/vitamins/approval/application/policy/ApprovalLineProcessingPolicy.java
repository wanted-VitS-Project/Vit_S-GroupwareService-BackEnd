package com.group3.vitamins.approval.application.policy;

import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 결재선 처리(승인 PRC-001~004 · 반려 PRC-005~009)가 공용으로 쓰는 존재·소유자·ACTIVE 검증 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalLineProcessingPolicy {

    private final ApprovalRepository approvalRepository;

    /**
     * 잠금 조회 + 소유자·상태 검증까지 한 번에 끝낸다. {@code lineId} 자체가 없는 경우도
     * {@code APPROVAL_LINE_FORBIDDEN}(403)으로 흡수한다(리소스 존재 여부 비노출, API 명세 확인 필요 표시됨).
     */
    public ApprovalLine getActiveOwnedLineOrThrow(Long lineId, String requesterId) {
        ApprovalLine line = approvalRepository.findLineByIdForUpdate(lineId)
                .orElseThrow(() -> {
                    log.warn("결재선 없음 - lineId={}", lineId);
                    return new ForbiddenException(ApprovalErrorCode.APPROVAL_LINE_FORBIDDEN);
                });

        if (!line.getApproverId().equals(requesterId)) {
            log.warn("결재선 처리 - 결재자 아님 lineId={}, requesterId={}", lineId, requesterId);
            throw new ForbiddenException(ApprovalErrorCode.APPROVAL_LINE_FORBIDDEN);
        }

        if (line.getStatus() == ApprovalLineStatus.ACTIVE) {
            return line;
        }
        if (line.getStatus() == ApprovalLineStatus.WAITING || line.getStatus() == ApprovalLineStatus.DRAFT) {
            log.warn("결재선 처리 - 아직 ACTIVE 아님 lineId={}, status={}", lineId, line.getStatus());
            throw new ConflictException(ApprovalErrorCode.APPROVAL_LINE_NOT_ACTIVE);
        }
        // APPROVED/REJECTED/CANCELED — 이미 종결된 단계의 중복 처리(동시 요청·이중 클릭)
        log.warn("결재선 처리 - 이미 처리됨 lineId={}, status={}", lineId, line.getStatus());
        throw new ConflictException(ApprovalErrorCode.APPROVAL_LINE_ALREADY_PROCESSED);
    }
}
