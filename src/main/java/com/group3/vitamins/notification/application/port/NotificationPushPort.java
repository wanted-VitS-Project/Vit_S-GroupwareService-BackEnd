package com.group3.vitamins.notification.application.port;

import com.group3.vitamins.notification.application.result.NotificationResult;

/**
 * 저장된 알림을 수신자에게 즉시 밀어주는 아웃바운드 포트 (§5 RT-002).
 *
 * <p>구현체를 갈아끼우는 지점이다 — 지금은 인메모리 SSE 어댑터 하나뿐이고, 앱 서버를 2대 이상으로
 * 늘리면 Redis Pub/Sub 어댑터로 교체·중첩한다({@code SseNotificationStreamAdapter} 주석 참고).
 */
public interface NotificationPushPort {

    /**
     * RT-003 · RT-004 — 해당 사번의 열린 연결 <b>전부</b>에 보낸다. 연결이 하나도 없거나 전송이
     * 실패해도 예외를 던지지 않는다. 알림 row 는 이미 저장돼 있어 목록 조회로 보이기 때문이다.
     */
    void push(String userId, NotificationResult notification);
}
