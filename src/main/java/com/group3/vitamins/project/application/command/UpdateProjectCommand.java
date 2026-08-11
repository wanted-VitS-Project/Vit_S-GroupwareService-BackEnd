package com.group3.vitamins.project.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 프로젝트 수정. 수정 화면이 폼 전체를 보내므로 받은 값으로 전부 덮어쓴다 (PRJ-006).
 * 그래서 null 은 "그 값을 비운다" 는 뜻이다 — 과업명만 예외로 필수다.
 *
 * @param version   조회에서 받은 버전. 이 값이 DB 와 다르면 409 다
 * @param overwrite true 면 충돌을 무시하고 DB 현재 버전을 기대값으로 써서 덮어쓴다
 */
public record UpdateProjectCommand(
        Long projectId,
        String name,
        String description,
        String clientName,
        LocalDate startedOn,
        LocalDate endedOn,
        BigDecimal contractAmount,
        int version,
        boolean overwrite,
        String requesterUserId,
        String role
) {
}