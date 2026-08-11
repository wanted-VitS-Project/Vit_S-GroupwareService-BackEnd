package com.group3.vitamins.project.step.application.command;

/**
 * 스텝 상태 변경. status 는 문자열이다 — DONE 금지가 이 API 전용 규칙이라 서비스가 파싱한다.
 *
 * @param version   조회에서 받은 버전. 이 값이 DB 와 다르면 409 다
 * @param overwrite true 면 충돌을 무시하고 DB 현재 버전을 기대값으로 써서 덮어쓴다
 */
public record ChangeStepStatusCommand(
        Long stepId,
        String status,
        int version,
        boolean overwrite,
        String requesterUserId,
        String role
) {
}
