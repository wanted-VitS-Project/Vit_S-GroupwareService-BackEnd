package com.group3.vitamins.project.step.application.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 스텝 수정 결과. 생성용 StepResult 와 달리 status·createdAt 이 없고 updatedAt 이 있다. */
public record StepUpdateResult(
        Long stepId,
        Long stageId,
        String name,
        LocalDate startedOn,
        LocalDate endedOn,
        StepResult.Owner owner,
        LocalDateTime updatedAt
) {
}
