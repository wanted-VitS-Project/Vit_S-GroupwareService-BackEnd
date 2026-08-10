package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper.CompanyBidNoticeStateMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CompanyBidNoticeStateRepositoryAdapterTest {

    @Test
    void delegatesDistinctNoticeIdsToAtomicUpsertMapper() {
        CompanyBidNoticeStateMapper mapper = mock(CompanyBidNoticeStateMapper.class);
        Long companyId = 10L;
        Long runId = 20L;
        LocalDateTime now = LocalDateTime.of(2026, 8, 10, 23, 30);

        new CompanyBidNoticeStateRepositoryAdapter(mapper)
                .observeAll(companyId, List.of(100L, 100L, 101L), runId, now);

        verify(mapper).upsertObserved(
                eq(companyId),
                argThat(ids -> ids.size() == 2 && ids.containsAll(List.of(100L, 101L))),
                eq(runId),
                eq(now)
        );
    }
}
