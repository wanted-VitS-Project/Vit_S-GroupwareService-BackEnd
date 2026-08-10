package com.group3.vitamins.project.step.application.result;

/** 재정렬 후 서버가 확정한 스텝 위치. FE 는 이 값으로 보드를 다시 그린다. */
public record StepOrderResult(Long stepId, Long stageId, int sortOrder) {
}