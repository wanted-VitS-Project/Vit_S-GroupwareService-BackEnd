package com.group3.vitamins.project.application.result;

import java.time.LocalDateTime;

public record ProjectStatusResult(
        Long projectId,
        String status,
        LocalDateTime updatedAt
) {
}