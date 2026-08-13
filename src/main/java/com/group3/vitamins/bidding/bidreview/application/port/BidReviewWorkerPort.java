package com.group3.vitamins.bidding.bidreview.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Python Worker의 작업 조회·콜백(진행상황·완료·실패)이 공유하는 아웃바운드 포트.
// 구현은 infrastructure/persistence/adapter/JpaBidReviewWorkerAdapter.
// ⚠️ 4개 메서드 모두 findForWorkerUpdate(PESSIMISTIC_WRITE)로 검토 행을 잠근 뒤,
// BidReview 도메인의 matchesAttempt()·acceptsWorkerCallback()으로 attemptId·상태를 함께 확인해야
// 한다 — 하나만 확인하면 이미 끝난 attempt의 뒤늦은 콜백을 걸러내지 못한다.
public interface BidReviewWorkerPort {

    // 현재 attemptId가 PENDING/PROCESSING과 일치하면 작업을 점유하고 PROCESSING으로 전환한다.
    // 일치하지 않으면(이미 처리됨·다른 attempt) empty를 반환 — 서비스가 404로 변환한다.
    Optional<ClaimedJob> claimJob(
            Long reviewId,
            String attemptId,
            LocalDateTime now
    );

    // reviewStatus=PROCESSING 콜백 — 문서별 중간 처리 상태만 갱신한다. result·citations는 다루지 않는다.
    CallbackUpdate reportProgress(
            Long reviewId,
            String attemptId,
            List<DocumentOutcome> documents,
            LocalDateTime now
    );

    // reviewStatus=COMPLETED 콜백 — 결과·문서 처리결과·근거를 함께 저장하고 expiresAt을 계산한다.
    CallbackUpdate complete(
            Long reviewId,
            String attemptId,
            String result,
            List<DocumentOutcome> documents,
            List<CitationInput> citations,
            LocalDateTime now
    );

    // reviewStatus=FAILED 콜백 — retryable이면 새 attemptId를 발급해 재시도 Outbox를 저장하고
    // PROCESSING을 유지한다. 아니거나 최대 재시도를 넘겼으면 최종 FAILED로 종료하고 expiresAt을 계산한다.
    CallbackUpdate fail(
            Long reviewId,
            String attemptId,
            String errorCode,
            String errorMessage,
            boolean retryable,
            List<DocumentOutcome> documents,
            LocalDateTime now
    );

    record ClaimedJob(
            Long reviewId,
            Long companyId,
            Long noticeId,
            String attemptId,
            String prompt,
            List<JobDocument> documents
    ) {
    }

    // documentRole은 "BID_ATTACHMENT" 또는 "INTERNAL_REFERENCE"이며 역할에 맞는 ID만 채워진다.
    record JobDocument(
            String documentRole,
            Long bidAttachmentId,
            Long referenceFileId,
            String fileName
    ) {
    }

    record DocumentOutcome(
            Long bidAttachmentId,
            String processingStatus,
            String temporaryStorageKey,
            Long fileSize,
            String mimeType
    ) {
    }

    record CitationInput(
            int rankOrder,
            String documentRole,
            Long bidAttachmentId,
            Long referenceFileId,
            String fileName,
            Integer pageNumber,
            String sheetName,
            String excerpt
    ) {
    }

    record CallbackUpdate(
            boolean exists,
            boolean accepted,
            String currentStatus,
            String reason
    ) {
    }
}
