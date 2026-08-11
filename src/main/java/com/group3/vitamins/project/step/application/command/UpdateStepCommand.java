package com.group3.vitamins.project.step.application.command;

import java.time.LocalDate;

/**
 * 스텝 수정. 편집 화면이 폼 전체를 보내는 계약이라 받은 값으로 덮어쓴다.
 * 소속 스테이지는 여기 없다 — 위치 변경은 순서 변경 API 로 일원화했다 (2026-08-09 결정).
 *
 * @param version   조회에서 받은 버전. 이 값이 DB 와 다르면 409 다
 * @param overwrite true 면 충돌을 무시하고 DB 현재 버전을 기대값으로 써서 덮어쓴다
 */
public record UpdateStepCommand(
        Long stepId,
        String name,
        LocalDate startedOn,
        LocalDate endedOn,
        String ownerUserId,
        int version,
        boolean overwrite,
        String requesterUserId,
        String role
) {
}
