package com.group3.vitamins.project.stage.application.result;

/** 재정렬 후 서버가 확정한 스테이지 순서. version 은 저장 후의 새 값이다. */
public record StageOrderResult(Long stageId, int sortOrder, int version) {
}
