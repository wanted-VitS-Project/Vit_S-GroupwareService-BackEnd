package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidnotice.application.port.CompanyBidNoticeStatePort;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper.CompanyBidNoticeStateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;

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
}
