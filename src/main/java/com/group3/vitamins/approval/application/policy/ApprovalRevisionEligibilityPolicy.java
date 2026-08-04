package com.group3.vitamins.approval.application.policy;

import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;

/** 회차(revision)를 편집하는 여러 엔드포인트(APR-002 등)가 공용으로 쓰는 존재·기안자·DRAFT 검증 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalRevisionEligibilityPolicy {

    private final ApprovalRepository approvalRepository;

    public Approval getApprovalOrThrow(Long approvalId) {
        return approvalRepository.findApproval(approvalId)
                .orElseThrow(() -> {
                    log.warn("결재 없음 - approvalId={}", approvalId);
                    return new NotFoundException(ApprovalErrorCode.APPROVAL_NOT_FOUND);
                });
    }

    public void assertDrafter(Approval approval, String requesterId) {
        if (!approval.getDrafterId().equals(requesterId)) {
            log.warn("기안자 아님 - approvalId={}, requesterId={}", approval.getApprovalId(), requesterId);
            throw new ForbiddenException(ApprovalErrorCode.APPROVAL_NOT_DRAFTER);
        }
    }

    /** 회차가 이 결재({@code approvalId}) 소속인지까지 확인한다 — 아니면 못 찾은 것과 동일하게 404 */
    public ApprovalRevision getDraftRevisionOrThrow(Long approvalId, Long revisionId) {
        return getDraftRevisionOrThrow(approvalId, revisionId, approvalRepository::findRevisionById);
    }

    /**
     * {@link #getDraftRevisionOrThrow(Long, Long)} 과 동일하지만 잠금 조회를 쓴다 — 결재선 전체
     * 치환(APR-009)처럼 "확인 후 쓰기" 사이에 상신이 끼어들면 안 되는 경우에 쓴다.
     */
    public ApprovalRevision getDraftRevisionForUpdateOrThrow(Long approvalId, Long revisionId) {
        return getDraftRevisionOrThrow(approvalId, revisionId, approvalRepository::findRevisionByIdForUpdate);
    }

    private ApprovalRevision getDraftRevisionOrThrow(Long approvalId, Long revisionId,
                                                       Function<Long, Optional<ApprovalRevision>> lookup) {
        ApprovalRevision revision = lookup.apply(revisionId)
                .filter(r -> r.getApprovalId().equals(approvalId))
                .orElseThrow(() -> {
                    log.warn("회차 없음 - approvalId={}, revisionId={}", approvalId, revisionId);
                    return new NotFoundException(ApprovalErrorCode.APPROVAL_REVISION_NOT_FOUND);
                });

        if (revision.getStatus() != ApprovalStatus.DRAFT) {
            log.warn("DRAFT 아닌 회차 수정 시도 - revisionId={}, status={}", revisionId, revision.getStatus());
            throw new ConflictException(ApprovalErrorCode.APPROVAL_REVISION_NOT_DRAFT);
        }
        return revision;
    }
}
