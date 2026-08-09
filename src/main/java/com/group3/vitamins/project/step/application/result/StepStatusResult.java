package com.group3.vitamins.project.step.application.result;

import java.time.LocalDateTime;

/** 스텝 상태 변경 결과. */
public record StepStatusResult(Long stepId, String status, LocalDateTime updatedAt) {
}
