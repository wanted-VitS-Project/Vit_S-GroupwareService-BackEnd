package com.group3.vitamins.activitylog.infrastructure.persistence;

import com.group3.vitamins.activitylog.application.port.ActivityLogRecordPort;
import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JpaActivityLogRecordAdapter implements ActivityLogRecordPort {

    private final ActivityLogJpaRepository activityLogJpaRepository;

    @Override
    public void record(ActivityOccurredEvent event, Long companyId) {
        List<ActivityLogEntity> logs = event.changes().stream()
                .map(change -> toEntity(event, change, companyId))
                .toList();

        activityLogJpaRepository.saveAll(logs);
    }

    private ActivityLogEntity toEntity(
            ActivityOccurredEvent event,
            ActivityFieldChange change,
            Long companyId
    ) {
        return ActivityLogEntity.record(
                event.action(),
                event.blockId(),
                event.resourceId(),
                event.resourceName(),
                change.field(),
                change.beforeValue(),
                change.afterValue(),
                event.actorId(),
                companyId
        );
    }
}
