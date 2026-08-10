package com.group3.vitamins.project.stage.application.command;

/**
 * 스테이지 삭제. moveToStageId 는 하위 스텝을 옮길 대상이다 (STG-003).
 * {@code 0} 이면 미소속, null 이면 미지정(400)이다 — 둘을 구분해야 해서 서비스가 판정한다.
 */
public record DeleteStageCommand(
        Long stageId,
        Long moveToStageId,
        String requesterUserId,
        String role
) {
}
