package com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryManagementPort;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryDetails;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.entity.BidNoticeSummaryJpaEntity;
import com.group3.vitamins.bidding.bidsummary.infrastructure.persistence.repository.BidNoticeSummaryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaBidNoticeSummaryManagementAdapter
        implements BidNoticeSummaryManagementPort {

    private final BidNoticeSummaryJpaRepository repository;

    @Override
    public Optional<BidNoticeSummaryDetails> findAccessible(
            Long companyId, Long summaryId, String userId
    ) {
        return repository.findAccessible(companyId, summaryId, userId)
                .map(BidNoticeSummaryJpaEntity::toDetails);
    }

    @Override
    public Optional<BidNoticeSummaryDetails> findOwnedForUpdate(
            Long companyId, Long summaryId, String userId
    ) {
        return repository.findOwnedForUpdate(companyId, summaryId, userId)
                .map(BidNoticeSummaryJpaEntity::toDetails);
    }

    @Override
    public BidNoticeSummaryDetails updateSummaries(
            Long summaryId, SummaryValues values, LocalDateTime now
    ) {
        BidNoticeSummaryJpaEntity entity = managed(summaryId);
        entity.updateSummaries(
                values.overviewSummary(), values.amountSummary(),
                values.scheduleSummary(), values.qualificationSummary(),
                values.taskSummary(), values.riskSummary(), now
        );
        return entity.toDetails();
    }

    @Override
    public BidNoticeSummaryDetails confirm(
            Long summaryId, String confirmedBy, LocalDateTime now
    ) {
        BidNoticeSummaryJpaEntity entity = managed(summaryId);
        entity.confirm(confirmedBy, now);
        return entity.toDetails();
    }

    @Override
    public BidNoticeSummaryDetails abandon(Long summaryId, LocalDateTime now) {
        BidNoticeSummaryJpaEntity entity = managed(summaryId);
        entity.abandon(now);
        return entity.toDetails();
    }

    private BidNoticeSummaryJpaEntity managed(Long summaryId) {
        return repository.findById(summaryId)
                .orElseThrow(() -> new IllegalStateException(
                        "Locked bidding summary disappeared: " + summaryId
                ));
    }
}
