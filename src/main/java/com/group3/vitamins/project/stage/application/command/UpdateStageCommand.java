package com.group3.vitamins.project.stage.application.command;

/** 스테이지 수정. 이름만 바꾼다 — 순서는 순서 변경 API 소관이다. */
public record UpdateStageCommand(
        Long stageId,
        String name,
        String requesterUserId,
        String role
) {
}
