package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidnotice.application.port.CompanyBidNoticeStatePort;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper.CompanyBidNoticeStateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;

import com.group3.vitamins.bidding.bidnotice.domain.model.BidNoticeCompanyStatus;
import com.group3.vitamins.bidding.bidnotice.domain.model.CompanyBidNoticeState;

@Repository
@RequiredArgsConstructor
public class CompanyBidNoticeStateRepositoryAdapter implements CompanyBidNoticeStatePort {

    private final CompanyBidNoticeStateMapper mapper;

    // 회사별 상태를 한 번에 조회해 공고별 추가 조회 없이 저장합니다.
    @Override
    public void observeAll(
            Long companyId,
            Collection<Long> bidNoticeIds,
            Long runId,
            LocalDateTime observedAt
    ) {
        if (bidNoticeIds == null || bidNoticeIds.isEmpty()) {
            return;
        }

        mapper.upsertObserved(
                companyId,
                new LinkedHashSet<>(bidNoticeIds),
                runId,
                observedAt
        );
    }

    @Override
    public Optional<CompanyBidNoticeState> findForUpdate(
            Long companyId,
            Long bidNoticeId
    ) {
        return mapper.findForUpdate(companyId, bidNoticeId)
                .map(row -> new CompanyBidNoticeState(
                        row.companyId(),
                        row.bidNoticeId(),
                        BidNoticeCompanyStatus.valueOf(row.noticeStatus()),
                        row.dismissReason(),
                        row.isFavorite(),
                        row.updatedAt()
                ));
    }

    @Override
    public void update(CompanyBidNoticeState state) {
        int updated = mapper.updateStatus(
                state.companyId(),
                state.noticeId(),
                state.status().name(),
                state.dismissReason(),
                state.isFavorite(),
                state.updatedAt()
        );
        if (updated != 1) {
            throw new IllegalStateException("회사별 입찰 공고 상태 변경에 실패했습니다.");
        }
    }
}
