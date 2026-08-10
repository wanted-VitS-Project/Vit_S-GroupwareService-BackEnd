package com.group3.vitamins.project.stage.application.result;

/** 재정렬 후 서버가 확정한 스테이지 순서. */
public record StageOrderResult(Long stageId, int sortOrder) {
}
