package com.group3.vitamins.bidding.bidsummary.infrastructure.query;

import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryHistoryItemResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("MyBatisBidNoticeSummaryHistoryQueryAdapter 이력 조회 위임")
class MyBatisBidNoticeSummaryHistoryQueryAdapterTest {

    @Test
    @DisplayName("회사와 사용자 경계를 포함한 조회 조건을 mapper에 그대로 전달한다")
    void delegatesTenantScopedHistoryQuery() {
        BidNoticeSummaryHistoryQueryMapper mapper =
                mock(BidNoticeSummaryHistoryQueryMapper.class);
        MyBatisBidNoticeSummaryHistoryQueryAdapter adapter =
                new MyBatisBidNoticeSummaryHistoryQueryAdapter(mapper);
        var item = new BidNoticeSummaryHistoryItemResult(
                32L, 31L, 2, "COMPLETED", "보강해줘",
                false, true, null,
                LocalDateTime.of(2026, 8, 12, 10, 0), null
        );
        when(mapper.findHistory(10L, 317L, "vitas-USER001", 20, 20))
                .thenReturn(List.of(item));
        when(mapper.countHistory(10L, 317L, "vitas-USER001"))
                .thenReturn(21L);
        when(mapper.findLatestMineSummaryId(10L, 317L, "vitas-USER001"))
                .thenReturn(32L);

        assertThat(adapter.findHistory(10L, 317L, "vitas-USER001", 20, 20))
                .containsExactly(item);
        assertThat(adapter.countHistory(10L, 317L, "vitas-USER001"))
                .isEqualTo(21L);
        assertThat(adapter.findLatestMineSummaryId(10L, 317L, "vitas-USER001"))
                .isEqualTo(32L);
    }
}
