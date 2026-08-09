package com.group3.vitamins.project.application.result;

import java.time.LocalDateTime;

public record ProjectCloseResult(
        Long projectId,
        String status,
        String closeReasonCode,
        String closeReasonNote,
        LocalDateTime closedAt
) {
}