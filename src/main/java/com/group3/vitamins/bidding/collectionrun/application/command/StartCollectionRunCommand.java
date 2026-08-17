package com.group3.vitamins.bidding.collectionrun.application.command;

import java.time.LocalDateTime;

public record StartCollectionRunCommand(
        Long conditionId,
        String userId,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
    public StartCollectionRunCommand(Long conditionId, String userId) {
        this(conditionId, userId, null, null);
    }
}
