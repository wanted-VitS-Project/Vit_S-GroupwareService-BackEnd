package com.group3.vitamins.project.application.result;

import java.time.LocalDateTime;

/** version 은 저장 후의 새 값이다. */
public record ProjectStatusResult(
        Long projectId,
        String status,
        LocalDateTime updatedAt,
        int version
) {
}