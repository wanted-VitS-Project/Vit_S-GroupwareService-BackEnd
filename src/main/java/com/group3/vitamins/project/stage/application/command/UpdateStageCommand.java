package com.group3.vitamins.project.stage.application.command;

/**
 * 스테이지 수정. 이름만 바꾼다 — 순서는 순서 변경 API 소관이다.
 *
 * @param version   조회에서 받은 버전. 이 값이 DB 와 다르면 409 다
 * @param overwrite true 면 충돌을 무시하고 DB 현재 버전을 기대값으로 써서 덮어쓴다
 */
public record UpdateStageCommand(
        Long stageId,
        String name,
        int version,
        boolean overwrite,
        String requesterUserId,
        String role
) {
}
