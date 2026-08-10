package com.group3.vitamins.project.step.application.command;

/** 스텝 상태 변경. status 는 문자열이다 — DONE 금지가 이 API 전용 규칙이라 서비스가 파싱한다. */
public record ChangeStepStatusCommand(
        Long stepId,
        String status,
        String requesterUserId,
        String role
) {
}
