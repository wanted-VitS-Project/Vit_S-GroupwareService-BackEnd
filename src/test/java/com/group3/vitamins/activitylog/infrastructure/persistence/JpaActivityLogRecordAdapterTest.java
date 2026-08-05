package com.group3.vitamins.activitylog.infrastructure.persistence;

import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JpaActivityLogRecordAdapterTest {

    private final ActivityLogJpaRepository activityLogJpaRepository = mock(ActivityLogJpaRepository.class);
    private final JpaActivityLogRecordAdapter adapter = new JpaActivityLogRecordAdapter(activityLogJpaRepository);

    @Test
    @DisplayName("변경 필드 목록을 activity_log 행 단위로 풀어서 저장한다")
    void record_multipleChanges() {
        ActivityOccurredEvent event = ActivityOccurredEvent.of(
                ActivityLogAction.MODIFY,
                10L,
                20L,
                "체크리스트 항목",
                "EMP001",
                List.of(
                        new ActivityFieldChange("content", "기존 항목", "수정 항목"),
                        new ActivityFieldChange("isCompleted", "false", "true")
                )
        );

        adapter.record(event);

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Iterable<ActivityLogEntity>> captor = ArgumentCaptor.forClass((Class) Iterable.class);
        verify(activityLogJpaRepository).saveAll(captor.capture());

        List<ActivityLogEntity> logs = toList(captor.getValue());
        assertThat(logs).hasSize(2);
        assertThat(logs)
                .extracting(ActivityLogEntity::getField)
                .containsExactly("content", "isCompleted");
        assertThat(logs)
                .allSatisfy(log -> {
                    assertThat(log.getAct()).isEqualTo(ActivityLogAction.MODIFY);
                    assertThat(log.getBlockId()).isEqualTo(10L);
                    assertThat(log.getResourceId()).isEqualTo(20L);
                    assertThat(log.getResourceName()).isEqualTo("체크리스트 항목");
                    assertThat(log.getUserId()).isEqualTo("EMP001");
                });
    }

    private List<ActivityLogEntity> toList(Iterable<ActivityLogEntity> logs) {
        return StreamSupport.stream(logs.spliterator(), false)
                .toList();
    }
}
