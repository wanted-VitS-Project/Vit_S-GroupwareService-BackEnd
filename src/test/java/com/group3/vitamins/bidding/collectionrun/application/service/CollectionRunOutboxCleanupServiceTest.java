package com.group3.vitamins.bidding.collectionrun.application.service;

import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunOutboxStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionRunOutboxCleanupService")
class CollectionRunOutboxCleanupServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 21, 4, 0);

    @Mock
    private CollectionRunOutboxStorePort outboxStorePort;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                Instant.parse("2026-08-20T19:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
    }

    @Test
    @DisplayName("보관 기간(일) 만큼 뺀 시각을 cutoff로 넘기고 삭제된 행 수를 그대로 반환한다")
    void cleansUpUsingRetentionDaysCutoff() {
        CollectionRunOutboxCleanupService service =
                new CollectionRunOutboxCleanupService(outboxStorePort, clock, 7);
        when(outboxStorePort.deletePublishedBefore(eq(NOW.minusDays(7)))).thenReturn(3);

        int deletedCount = service.cleanupPublished();

        assertThat(deletedCount).isEqualTo(3);
    }

    @Test
    @DisplayName("보관 기간은 1일 이상이어야 한다")
    void rejectsNonPositiveRetentionDays() {
        assertThatThrownBy(() -> new CollectionRunOutboxCleanupService(outboxStorePort, clock, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
