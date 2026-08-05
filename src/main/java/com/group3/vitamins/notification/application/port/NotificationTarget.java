package com.group3.vitamins.notification.application.port;

import java.util.Map;

/** 이동 대상 조회 결과 — 도메인별 실제 구분 번호와 부가 정보(`extra`). */
public record NotificationTarget(Long targetId, Map<String, Object> extra) {

    public static NotificationTarget of(Long targetId, Map<String, Object> extra) {
        return new NotificationTarget(targetId, extra);
    }
}
