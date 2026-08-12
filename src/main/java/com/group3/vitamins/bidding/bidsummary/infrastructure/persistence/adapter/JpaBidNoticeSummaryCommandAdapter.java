package com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryCommandPort;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryNoticePort.BidNoticeSnapshot;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummary;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity.BidNoticeSummaryJpaEntity;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity.BidNoticeSummaryOutboxJpaEntity;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.repository.BidNoticeSummaryJpaRepository;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.repository.BidNoticeSummaryOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaBidNoticeSummaryCommandAdapter
        implements BidNoticeSummaryCommandPort {

    private static final String SUMMARY_REQUESTED_EVENT =
            "BIDDING_SUMMARY_REQUESTED";

    private static final List<BidNoticeSummaryStatus> IN_PROGRESS_STATUSES =
            List.of(
                    BidNoticeSummaryStatus.PENDING,
                    BidNoticeSummaryStatus.PROCESSING
            );

    private final BidNoticeSummaryJpaRepository summaryRepository;
    private final BidNoticeSummaryOutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    // 현재 사용자에게 동일 공고의 처리 중인 요약이 있는지 확인합니다.
    @Override
    @Transactional(readOnly = true)
    public boolean existsInProgress(
            Long companyId,
            Long noticeId,
            String requestedBy
    ) {
        return summaryRepository
                .existsByCompanyIdAndNoticeIdAndRequestedByAndSummaryStatusInAndDeletedAtIsNull(
                        companyId,
                        noticeId,
                        requestedBy,
                        IN_PROGRESS_STATUSES
                );
    }

    // 요약 요청과 Redis 발행 Outbox를 하나의 트랜잭션으로 저장합니다.
    @Override
    @Transactional
    public BidNoticeSummary savePendingWithOutbox(
            BidNoticeSummary summary,
            BidNoticeSnapshot noticeSnapshot
    ) {
        JsonNode snapshotJson = objectMapper.valueToTree(noticeSnapshot);

        BidNoticeSummaryJpaEntity savedSummary =
                summaryRepository.saveAndFlush(
                        BidNoticeSummaryJpaEntity.pending(
                                summary,
                                snapshotJson
                        )
                );

        JsonNode payload = createPayload(savedSummary);

        BidNoticeSummaryOutboxJpaEntity outbox =
                BidNoticeSummaryOutboxJpaEntity.pending(
                        UUID.randomUUID().toString(),
                        savedSummary.getSummaryId(),
                        savedSummary.getProcessingAttemptId(),
                        SUMMARY_REQUESTED_EVENT,
                        payload,
                        summary.createdAt()
                );

        outboxRepository.save(outbox);

        return savedSummary.toDomain();
    }

    // Redis에는 작업 식별에 필요한 최소 정보만 저장합니다.
    private JsonNode createPayload(BidNoticeSummaryJpaEntity summary) {
        return objectMapper.createObjectNode()
                .put("summaryId", summary.getSummaryId())
                .put("companyId", summary.getCompanyId())
                .put("attemptId", summary.getProcessingAttemptId())
                .put("retryCount", summary.getRetryCount());
    }
}