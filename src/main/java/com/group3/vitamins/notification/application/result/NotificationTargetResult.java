package com.group3.vitamins.notification.application.result;

import java.util.Map;

public record NotificationTargetResult(String type, Long targetId, Map<String, Object> extra) {

    private static final String TYPE_NONE = "NONE";

    /** 매핑 없음(연결 block 없음·지원 안 하는 타입) — 에러가 아니라 정상 200 응답이다. */
    public static NotificationTargetResult none() {
        return new NotificationTargetResult(TYPE_NONE, null, null);
    }
}
