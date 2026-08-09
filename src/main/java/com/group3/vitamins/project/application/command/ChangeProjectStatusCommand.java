package com.group3.vitamins.project.application.command;

/** 상태 변경. status 는 문자열로 받아 서비스가 판정한다 — 잘못된 값에 PROJECT_STATUS_INVALID 를 내기 위해서다. */
public record ChangeProjectStatusCommand(
        Long projectId,
        String status,
        String requesterUserId,
        String role
) {
}