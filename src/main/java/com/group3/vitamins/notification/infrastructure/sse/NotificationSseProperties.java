package com.group3.vitamins.notification.infrastructure.sse;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 실시간 알림(SSE) 커넥션 설정 (§5 RT-006).
 *
 * <p>두 값 모두 "브라우저와 서버 사이에 뭐가 끼어 있는지"에 따라 조정해야 하므로 설정으로 뺀다 —
 * 프록시·로드밸런서마다 유휴 커넥션을 끊는 시간이 다르다.
 */
@ConfigurationProperties(prefix = "notification.sse")
public record NotificationSseProperties(

        // 연결 유지 시간. 지나면 서버가 정상 종료하고 브라우저가 자동 재연결한다(RT-006)
        Duration timeout,

        // 하트비트 주기. 중간 프록시의 유휴 타임아웃보다 반드시 짧아야 한다
        Duration heartbeatInterval
) {

    public NotificationSseProperties {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("notification.sse.timeout 은 0 보다 커야 한다");
        }
        if (heartbeatInterval == null || heartbeatInterval.isNegative() || heartbeatInterval.isZero()) {
            throw new IllegalArgumentException("notification.sse.heartbeat-interval 은 0 보다 커야 한다");
        }
        // ⚠️ 하트비트가 타임아웃보다 길면 ping 이 한 번도 못 나가고 연결이 끊긴다.
        //    설정 실수로 "실시간이 몇 분 만에 조용히 죽는" 증상이 되므로 기동 때 잡는다.
        if (heartbeatInterval.compareTo(timeout) >= 0) {
            throw new IllegalArgumentException(
                    "notification.sse.heartbeat-interval(%s) 은 timeout(%s) 보다 짧아야 한다"
                            .formatted(heartbeatInterval, timeout));
        }
    }
}
