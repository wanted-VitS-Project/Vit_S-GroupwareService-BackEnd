package com.group3.vitamins.notification.infrastructure.sse;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 실시간 알림(SSE) 설정값 등록만 한다 (§5).
 *
 * <p>🚨 <b>여기에 {@code TaskScheduler} 빈을 만들면 안 된다.</b> 하트비트용으로 하나 추가했다가
 * 되돌렸다 — 이유가 눈에 안 보이는 종류라 적어 둔다.
 *
 * <p>지금 이 앱의 {@code TaskScheduler} 빈은 {@code biddingTaskScheduler} <b>단 하나</b>이고,
 * {@code scheduler} 속성을 지정하지 않은 {@code @Scheduled} 6개(bidding 아웃박스·Redis 컨슈머·
 * vitamate 정리·파일 인덱스 재시도)가 <b>유일한 후보라는 이유로</b> 그 풀(poolSize=2)에 얹혀 있다.
 * 여기에 두 번째 {@code TaskScheduler} 빈을 추가하면 후보가 둘이 되어 Spring 이
 * {@code taskScheduler} 라는 이름의 빈을 찾다 실패하고, <b>스레드 1개짜리 기본 스케줄러로 조용히
 * 폴백한다.</b> 즉 알림과 무관한 폴링 6개가 스레드 2개 → 1개로 좁아진다. 기동은 성공하고
 * (경고 로그 한 줄만 남는다) 컴파일도 통과하므로 발견이 늦다.
 *
 * <p>그래서 하트비트는 공유 자원을 건드리지 않고 {@link SseNotificationStreamAdapter} 가
 * <b>자기 전용 스레드 1개</b>를 직접 들고 돈다.
 */
@Configuration
@EnableConfigurationProperties(NotificationSseProperties.class)
public class NotificationSseConfig {
}
