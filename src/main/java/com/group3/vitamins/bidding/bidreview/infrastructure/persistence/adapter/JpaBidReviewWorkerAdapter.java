package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewNoticeDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewWorkerPort;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocument;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocumentRole;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewCitationJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewDocumentJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewOutboxJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewCitationJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewDocumentJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaBidReviewWorkerAdapter implements BidReviewWorkerPort {

    private static final String BID_ATTACHMENT = "BID_ATTACHMENT";
    private static final String COMPANY_DOCUMENT_REFERENCE = "COMPANY_DOCUMENT_REFERENCE";
    private static final String FAILED_STATUS = "FAILED";
    private static final String REVIEW_REQUESTED_EVENT = "BID_REVIEW_REQUESTED";
    private static final String DEFAULT_DOCUMENT_FAILURE_MESSAGE = "문서 처리에 실패했습니다.";

    // bid_review.retry_count CHECK 제약(0~3)과 bid.md "최대 3회 재시도"에 맞춘 값.
    private static final int MAX_RETRY_COUNT = 3;
    private static final Duration FIRST_RETRY_DELAY = Duration.ofSeconds(10);
    private static final Duration SECOND_RETRY_DELAY = Duration.ofSeconds(30);
    // bid.md에 세 번째 간격이 명시돼 있지 않아 bidsummary의 10초/30초 진행을 이어 60초로 정했다.
    private static final Duration THIRD_RETRY_DELAY = Duration.ofSeconds(60);

    private final BidReviewJpaRepository reviewRepository;
    private final BidReviewDocumentJpaRepository documentRepository;
    private final BidReviewCitationJpaRepository citationRepository;
    private final BidReviewOutboxJpaRepository outboxRepository;
    private final BidReviewNoticeDocumentPort noticeDocumentPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Optional<ClaimedJob> claimJob(
            Long reviewId,
            String attemptId,
            LocalDateTime now
    ) {
        return reviewRepository.findForWorkerUpdate(reviewId)
                .filter(entity -> isCurrentAttempt(entity, attemptId))
                .map(entity -> {
                    BidReview started = entity.toDomain().startProcessing(now);
                    entity.apply(started);

                    List<JobDocument> documents = documentRepository
                            .findAllByReviewIdOrderByDocumentRoleAscReviewDocumentIdAsc(reviewId)
                            .stream()
                            .map(document -> new JobDocument(
                                    document.getDocumentRole().name(),
                                    document.getBidAttachmentId(),
                                    document.getReferenceFileId(),
                                    document.getCompanyDocumentVersionId(),
                                    document.getFileName()
                            ))
                            .toList();

                    return new ClaimedJob(
                            entity.getReviewId(),
                            entity.getCompanyId(),
                            entity.getNoticeId(),
                            entity.getProcessingAttemptId(),
                            entity.getPrompt(),
                            documents
                    );
                });
    }

    @Override
    @Transactional
    public CallbackUpdate reportProgress(
            Long reviewId,
            String attemptId,
            List<DocumentOutcome> documents,
            LocalDateTime now
    ) {
        Optional<BidReviewJpaEntity> found = reviewRepository.findForWorkerUpdate(reviewId);
        if (found.isEmpty()) {
            return notFound();
        }

        BidReviewJpaEntity entity = found.get();
        if (!isCurrentAttempt(entity, attemptId)) {
            return ignored(entity);
        }

        applyDocumentOutcomes(reviewId, documents, now);

        return accepted(entity);
    }

    @Override
    @Transactional
    public CallbackUpdate complete(
            Long reviewId,
            String attemptId,
            String result,
            List<DocumentOutcome> documents,
            List<CitationInput> citations,
            LocalDateTime now
    ) {
        Optional<BidReviewJpaEntity> found = reviewRepository.findForWorkerUpdate(reviewId);
        if (found.isEmpty()) {
            return notFound();
        }

        BidReviewJpaEntity entity = found.get();
        if (!isCurrentAttempt(entity, attemptId)) {
            return ignored(entity);
        }

        applyDocumentOutcomes(reviewId, documents, now);

        BidReview completed = entity.toDomain().complete(result, now, findNoticeDeadlineAt(entity));
        entity.apply(completed);

        saveCitations(reviewId, citations, now);

        return accepted(entity);
    }

    @Override
    @Transactional
    public CallbackUpdate fail(
            Long reviewId,
            String attemptId,
            String errorCode,
            String errorMessage,
            boolean retryable,
            List<DocumentOutcome> documents,
            LocalDateTime now
    ) {
        Optional<BidReviewJpaEntity> found = reviewRepository.findForWorkerUpdate(reviewId);
        if (found.isEmpty()) {
            return notFound();
        }

        BidReviewJpaEntity entity = found.get();
        BidReview review = entity.toDomain();
        if (!isCurrentAttempt(entity, attemptId)) {
            return ignored(entity);
        }

        applyDocumentOutcomes(reviewId, documents, now);

        if (retryable && review.retryCount() < MAX_RETRY_COUNT) {
            retry(entity, review, errorCode, errorMessage, now);
        } else {
            BidReview failed = review.fail(errorCode, errorMessage, now, findNoticeDeadlineAt(entity));
            entity.apply(failed);
        }

        return accepted(entity);
    }

    // 임시파일 보관 기한을 공고 입찰마감일시까지로 잡기 위해 조회한다(2026-08-13 정책 변경).
    // 공고가 그 사이 회사에서 제외(DISMISSED)됐거나 삭제됐으면 조회되지 않는데, 이 경우
    // BidReview.complete()/fail()이 자체적으로 완료·실패 시각 + 3시간 fallback으로 처리한다.
    private LocalDateTime findNoticeDeadlineAt(BidReviewJpaEntity entity) {
        return noticeDocumentPort
                .findAccessibleNotice(entity.getCompanyId(), entity.getNoticeId())
                .map(BidReviewNoticeDocumentPort.NoticeSnapshot::bidDeadlineAt)
                .orElse(null);
    }

    // 새 attemptId로 상태를 되돌리고 같은 트랜잭션에 지연 Outbox를 저장한다 (bidsummary의 prepareRetry와 동일 패턴).
    private void retry(
            BidReviewJpaEntity entity,
            BidReview review,
            String errorCode,
            String errorMessage,
            LocalDateTime now
    ) {
        String nextAttemptId = UUID.randomUUID().toString();
        BidReview retried = review.retryProcessing(nextAttemptId, errorCode, errorMessage, now);
        entity.apply(retried);

        LocalDateTime availableAt = now.plus(retryDelay(retried.retryCount()));
        JsonNode payload = objectMapper.createObjectNode()
                .put("reviewId", entity.getReviewId())
                .put("companyId", entity.getCompanyId())
                .put("attemptId", nextAttemptId)
                .put("retryCount", retried.retryCount());

        outboxRepository.save(BidReviewOutboxJpaEntity.pending(
                UUID.randomUUID().toString(),
                entity.getReviewId(),
                nextAttemptId,
                REVIEW_REQUESTED_EVENT,
                payload,
                availableAt,
                now
        ));
    }

    private Duration retryDelay(int retryCount) {
        return switch (retryCount) {
            case 1 -> FIRST_RETRY_DELAY;
            case 2 -> SECOND_RETRY_DELAY;
            default -> THIRD_RETRY_DELAY;
        };
    }

    @Override
    @Transactional
    public int recoverOrphaned(LocalDateTime staleBefore, int batchLimit, LocalDateTime now) {
        List<Long> candidateIds = reviewRepository.findOrphanedCandidateIds(staleBefore, batchLimit);
        int recovered = 0;

        for (Long reviewId : candidateIds) {
            Optional<BidReviewJpaEntity> found = reviewRepository.findForWorkerUpdate(reviewId);
            if (found.isEmpty()) {
                continue;
            }
            BidReviewJpaEntity entity = found.get();
            BidReview review = entity.toDomain();

            // 후보 조회와 잠금 사이에 정상 흐름이 이미 처리했을 수 있으니 재확인한다.
            if (!review.reviewStatus().isProcessing()
                    || !entity.getUpdatedAt().isBefore(staleBefore)
                    || outboxRepository.existsByReviewIdAndPublishStatus(reviewId, "PENDING")) {
                continue;
            }

            if (review.retryCount() >= MAX_RETRY_COUNT) {
                BidReview failed = review.fail(
                        "ORPHANED_OUTBOX",
                        "발행 경로가 유실돼 재시도를 소진했습니다.",
                        now,
                        findNoticeDeadlineAt(entity)
                );
                entity.apply(failed);
            } else {
                retry(entity, review, "ORPHANED_OUTBOX", "발행 경로가 유실돼 재큐잉합니다.", now);
            }
            recovered++;
        }

        return recovered;
    }

    // documents[]의 각 항목을 해당 공고 첨부 문서에 반영한다. INTERNAL_REFERENCE·COMPANY_DOCUMENT_REFERENCE
    // 문서는 bidAttachmentId가 없어 조회에 걸리지 않으므로 자연히 건너뛴다.
    private void applyDocumentOutcomes(
            Long reviewId,
            List<DocumentOutcome> documents,
            LocalDateTime now
    ) {
        if (documents == null) {
            return;
        }

        for (DocumentOutcome outcome : documents) {
            documentRepository
                    .findByReviewIdAndBidAttachmentId(reviewId, outcome.bidAttachmentId())
                    .ifPresent(documentEntity -> {
                        BidReviewDocument document = documentEntity.toDomain();
                        BidReviewDocument updated = FAILED_STATUS.equals(outcome.processingStatus())
                                ? document.fail(DEFAULT_DOCUMENT_FAILURE_MESSAGE, now)
                                : document.ready(
                                        outcome.temporaryStorageKey(),
                                        outcome.fileSize(),
                                        outcome.mimeType(),
                                        now
                                );
                        documentEntity.apply(updated);
                    });
        }
    }

    // citations[]가 가리키는 문서 역할별로 bid_review_document_id를 찾아 근거 스냅샷을 저장한다.
    private void saveCitations(
            Long reviewId,
            List<CitationInput> citations,
            LocalDateTime now
    ) {
        if (citations == null || citations.isEmpty()) {
            return;
        }

        List<BidReviewCitationJpaEntity> entities = new ArrayList<>();

        for (CitationInput citation : citations) {
            Long reviewDocumentId = resolveReviewDocumentId(reviewId, citation);

            entities.add(BidReviewCitationJpaEntity.create(
                    reviewId,
                    reviewDocumentId,
                    citation.rankOrder(),
                    citation.fileName(),
                    citation.pageNumber(),
                    citation.sheetName(),
                    citation.excerpt(),
                    now
            ));
        }

        citationRepository.saveAll(entities);
    }

    // documentRole도 조회 조건에 함께 걸어, 식별자 값이 다른 역할의 컬럼과 우연히 겹쳐도
    // 잘못된 문서로 연결되지 않게 한다(CodeRabbit 2026-08-13 피드백). 역할 자체는
    // BidReviewCallbackService가 이미 3개 값 중 하나로만 검증하므로 valueOf가 안전하다.
    private Long resolveReviewDocumentId(Long reviewId, CitationInput citation) {
        BidReviewDocumentRole role = BidReviewDocumentRole.valueOf(citation.documentRole());

        Optional<BidReviewDocumentJpaEntity> document;
        if (BID_ATTACHMENT.equals(citation.documentRole())) {
            document = documentRepository.findByReviewIdAndDocumentRoleAndBidAttachmentId(
                    reviewId, role, citation.bidAttachmentId());
        } else if (COMPANY_DOCUMENT_REFERENCE.equals(citation.documentRole())) {
            document = documentRepository.findByReviewIdAndDocumentRoleAndCompanyDocumentVersionId(
                    reviewId, role, citation.companyDocumentVersionId());
        } else {
            document = documentRepository.findByReviewIdAndDocumentRoleAndReferenceFileId(
                    reviewId, role, citation.referenceFileId());
        }

        return document
                .map(BidReviewDocumentJpaEntity::getReviewDocumentId)
                .orElseThrow(() -> new IllegalStateException(
                        "근거가 가리키는 검토 문서를 찾을 수 없습니다."
                ));
    }

    // 검토가 현재 attempt를 처리 중인지 확인한다 — attemptId만 확인하면 이미 끝난 attempt의
    // 뒤늦은 콜백을 걸러내지 못한다(포트 인터페이스 주석 참고).
    private boolean isCurrentAttempt(BidReviewJpaEntity entity, String attemptId) {
        BidReview review = entity.toDomain();
        return review.matchesAttempt(attemptId)
                && review.reviewStatus().acceptsWorkerCallback();
    }

    private CallbackUpdate notFound() {
        return new CallbackUpdate(false, false, null, null);
    }

    private CallbackUpdate ignored(BidReviewJpaEntity entity) {
        return new CallbackUpdate(
                true,
                false,
                entity.getReviewStatus().name(),
                "attempt_mismatch_or_already_finished"
        );
    }

    private CallbackUpdate accepted(BidReviewJpaEntity entity) {
        return new CallbackUpdate(
                true,
                true,
                entity.getReviewStatus().name(),
                null
        );
    }
}
