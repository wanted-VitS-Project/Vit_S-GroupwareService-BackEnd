package com.group3.vitamins.auth.infrastructure.web;

import java.time.LocalDateTime;

/**
 * 현재 세션의 만료 정보 (`.ai/api/auth.md` §7).
 *
 * @param timeoutSeconds   유휴 타임아웃 정책값(초)
 * @param expiresAt        만료 예정 시각 (서버 시각)
 * @param remainingSeconds 만료까지 남은 초 — 조회 시점 기준
 */
public record SessionStatus(
        int timeoutSeconds,
        LocalDateTime expiresAt,
        long remainingSeconds
) {
}
