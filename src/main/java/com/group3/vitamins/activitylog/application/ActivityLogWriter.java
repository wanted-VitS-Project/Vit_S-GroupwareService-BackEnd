package com.group3.vitamins.activitylog.application;

import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.infrastructure.persistence.ActivityLogEntity;
import com.group3.vitamins.activitylog.infrastructure.persistence.ActivityLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogWriter {

    private final ActivityLogJpaRepository activityLogJpaRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void write(ActivityOccurredEvent event) {
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
