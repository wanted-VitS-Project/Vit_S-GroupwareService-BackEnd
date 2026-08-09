package com.group3.vitamins.project.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 프로젝트 수정. 수정 화면이 폼 전체를 보내므로 받은 값으로 전부 덮어쓴다 (PRJ-006).
 * 그래서 null 은 "그 값을 비운다" 는 뜻이다 — 과업명만 예외로 필수다.
 */
public record UpdateProjectCommand(
        Long projectId,
        String name,
        String description,
        String clientName,
        LocalDate startedOn,
        LocalDate endedOn,
        BigDecimal contractAmount,
        String requesterUserId,
        String role
) {
}