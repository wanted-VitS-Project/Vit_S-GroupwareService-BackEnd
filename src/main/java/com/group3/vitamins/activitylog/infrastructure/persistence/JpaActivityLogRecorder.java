package com.group3.vitamins.activitylog.infrastructure.persistence;

import com.group3.vitamins.activitylog.application.ActivityLogRecorder;
import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JpaActivityLogRecorder implements ActivityLogRecorder {

    private final ActivityLogJpaRepository activityLogJpaRepository;

    @Override
    public void record(ActivityOccurredEvent event) {
        List<ActivityLogEntity> logs = event.changes().stream()
                .map(change -> toEntity(event, change))
                .toList();

        activityLogJpaRepository.saveAll(logs);
    }

    private ActivityLogEntity toEntity(ActivityOccurredEvent event, ActivityFieldChange change) {
        return ActivityLogEntity.record(
                event.action(),
                event.blockId(),
                event.resourceId(),
                change.field(),
                change.beforeValue(),
                change.afterValue(),
                event.actorId()
        );
    }
}
