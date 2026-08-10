package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunConditionSnapshot;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunStatus;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity.CollectionRunJpaEntity;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.mapper.CollectionRunConditionSnapshotJsonMapper;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.repository.SpringDataCollectionRunRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCollectionRunStateAdapter 상태 전이")
class JpaCollectionRunStateAdapterTest {

    private static final Long RUN_ID = 1L;
    private static final Long COMPANY_ID = 10L;
    private static final String ATTEMPT_ID = "attempt-001";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 10, 12, 0);

    @Mock
    private SpringDataCollectionRunRepository repository;
    @Mock
    private CollectionRunConditionSnapshotJsonMapper snapshotMapper;
    @InjectMocks
    private JpaCollectionRunStateAdapter adapter;

    @Test
    @DisplayName("대기 중인 실행을 점유하고 조건 스냅샷을 반환한다")
    void claimsPendingRun() {
        CollectionRunJpaEntity entity = mock(CollectionRunJpaEntity.class);
        JsonNode json = mock(JsonNode.class);
        CollectionRunConditionSnapshot snapshot = mock(CollectionRunConditionSnapshot.class);
        when(repository.claimPendingRun(
                RUN_ID, COMPANY_ID, ATTEMPT_ID, 0, NOW, NOW.plusMinutes(5),
                CollectionRunStatus.PENDING, CollectionRunStatus.PROCESSING
        )).thenReturn(1);
        when(repository.findByCrawlRunIdAndCrawlCondition_CompanyIdAndProcessingAttemptIdAndRunStatusAndDeletedAtIsNull(
                RUN_ID, COMPANY_ID, ATTEMPT_ID, CollectionRunStatus.PROCESSING
        )).thenReturn(Optional.of(entity));
        when(entity.getCrawlRunId()).thenReturn(RUN_ID);
        when(entity.getConditionSnapshot()).thenReturn(json);
        when(snapshotMapper.fromJson(json)).thenReturn(snapshot);

        var claimed = adapter.claim(RUN_ID, COMPANY_ID, ATTEMPT_ID, 0, NOW, NOW.plusMinutes(5));

        assertThat(claimed).isPresent();
        assertThat(claimed.orElseThrow().conditionSnapshot()).isSameAs(snapshot);
    }

    @Test
    @DisplayName("이미 점유된 실행은 다시 조회하지 않는다")
    void rejectsAlreadyClaimedRun() {
        when(repository.claimPendingRun(
                RUN_ID, COMPANY_ID, ATTEMPT_ID, 0, NOW, NOW.plusMinutes(5),
                CollectionRunStatus.PENDING, CollectionRunStatus.PROCESSING
        )).thenReturn(0);

        assertThat(adapter.claim(RUN_ID, COMPANY_ID, ATTEMPT_ID, 0, NOW, NOW.plusMinutes(5)))
                .isEmpty();
        verify(repository, never())
                .findByCrawlRunIdAndCrawlCondition_CompanyIdAndProcessingAttemptIdAndRunStatusAndDeletedAtIsNull(
                        anyLong(), anyLong(), anyString(), any()
                );
    }

    @Test
    @DisplayName("완료 가능한 상태만 저장한다")
    void completesOnlyWithTerminalSuccessStatus() {
        when(repository.completeRun(
                RUN_ID, ATTEMPT_ID, CollectionRunStatus.COMPLETED,
                10, 6, 3, 1, NOW, CollectionRunStatus.PROCESSING
        )).thenReturn(1);

        assertThat(adapter.complete(
                RUN_ID, ATTEMPT_ID, CollectionRunStatus.COMPLETED,
                10, 6, 3, 1, NOW
        )).isTrue();
        assertThatThrownBy(() -> adapter.complete(
                RUN_ID, ATTEMPT_ID, CollectionRunStatus.FAILED,
                0, 0, 0, 0, NOW
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("점유 연장, 재시도 준비와 실패 결과를 변경 행 수로 판단한다")
    void delegatesRemainingTransitions() {
        when(repository.renewLease(
                RUN_ID, ATTEMPT_ID, NOW.plusMinutes(5), NOW,
                CollectionRunStatus.PROCESSING
        )).thenReturn(1);
        when(repository.prepareRetry(
                RUN_ID, ATTEMPT_ID, "TEMPORARY", "timeout", NOW,
                CollectionRunStatus.PENDING, CollectionRunStatus.PROCESSING
        )).thenReturn(0);
        when(repository.failRun(
                RUN_ID, ATTEMPT_ID, "PERMANENT", "invalid response", NOW,
                CollectionRunStatus.PROCESSING, CollectionRunStatus.FAILED
        )).thenReturn(1);

        assertThat(adapter.renewLease(RUN_ID, ATTEMPT_ID, NOW.plusMinutes(5), NOW)).isTrue();
        assertThat(adapter.prepareRetry(RUN_ID, ATTEMPT_ID, "TEMPORARY", "timeout", NOW)).isFalse();
        assertThat(adapter.fail(RUN_ID, ATTEMPT_ID, "PERMANENT", "invalid response", NOW)).isTrue();
    }
}
