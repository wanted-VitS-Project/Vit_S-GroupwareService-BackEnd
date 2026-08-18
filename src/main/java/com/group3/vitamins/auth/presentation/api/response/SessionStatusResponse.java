package com.group3.vitamins.auth.presentation.api.response;

import com.group3.vitamins.auth.infrastructure.web.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

/** 세션 상태 조회 응답 (`.ai/api/auth.md` §7). 호출 자체가 세션을 연장하므로 remainingSeconds ≈ timeoutSeconds 다. */
@Schema(description = "세션 상태 조회 응답")
public record SessionStatusResponse(

        @Schema(description = "유휴 타임아웃 정책값(초). 기본 14400 (4시간), 배포 환경변수 SESSION_TIMEOUT 을 따라간다",
                example = "14400")
        int timeoutSeconds,

        @Schema(description = "만료 예정 시각 yyyy-MM-dd HH:mm:ss (서버 시각). 표시 전용",
                example = "2026-08-18 15:03:27")
        String expiresAt,

        @Schema(description = "만료까지 남은 초 (호출 직후 기준). 카운트다운 기준값",
                example = "14400")
        long remainingSeconds
) {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static SessionStatusResponse from(SessionStatus status) {
        return new SessionStatusResponse(
                status.timeoutSeconds(),
                status.expiresAt().format(DATE_TIME),
                status.remainingSeconds()
        );
    }
}
