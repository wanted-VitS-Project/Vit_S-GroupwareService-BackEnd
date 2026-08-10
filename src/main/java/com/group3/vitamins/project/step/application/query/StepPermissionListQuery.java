package com.group3.vitamins.project.step.application.query;

/** 스텝 권한 목록 조회. 권한은 프로젝트 EDITOR 기준이다 — 스텝 권한이 아니다. */
public record StepPermissionListQuery(Long stepId, String requesterUserId, String role) {
}
