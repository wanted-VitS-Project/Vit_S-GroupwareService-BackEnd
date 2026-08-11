package com.group3.vitamins.project.application.command;

/**
 * 상태 변경. status 는 문자열로 받아 서비스가 판정한다 — 잘못된 값에 PROJECT_STATUS_INVALID 를 내기 위해서다.
 *
 * @param version   조회에서 받은 버전. 이 값이 DB 와 다르면 409 다
 * @param overwrite true 면 충돌을 무시하고 DB 현재 버전을 기대값으로 써서 덮어쓴다
 */
public record ChangeProjectStatusCommand(
        Long projectId,
        String status,
        int version,
        boolean overwrite,
        String requesterUserId,
        String role
) {
}