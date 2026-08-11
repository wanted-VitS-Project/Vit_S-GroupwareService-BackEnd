package com.group3.vitamins.project.step.application.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 스텝 수정 결과. 생성용 StepResult 와 달리 status·createdAt 이 없고 updatedAt 이 있다.
 *
 * @param version 저장 후의 새 버전. 프론트는 이 값으로 화면 상태를 교체해야
 *                <b>다음 저장이 409 가 되지 않는다</b>
 */
public record StepUpdateResult(
        Long stepId,
        Long stageId,
        String name,
        LocalDate startedOn,
        LocalDate endedOn,
        StepResult.Owner owner,
        LocalDateTime updatedAt,
        int version
) {
}
