package com.group3.vitamins.notification.infrastructure.sse;

import com.group3.vitamins.notification.application.port.NotificationPushPort;
import com.group3.vitamins.notification.application.port.NotificationStreamPort;
import com.group3.vitamins.notification.application.result.NotificationResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 실시간 알림 SSE 어댑터 — 커넥션 보관 · 전송 · 하트비트를 전담한다 (§5).
 *
 * <p>포트 두 개를 한 클래스가 구현한다. 구독(연결 열기)과 전송(연결에 쓰기)이 <b>같은 커넥션 맵</b>을
 * 봐야 하기 때문이다. 나누면 맵을 세 번째 클래스로 빼야 하는데 얻는 게 없다.
 *
 * <p>🔖 <b>앱 서버를 2대 이상으로 늘리면 이 어댑터만으로는 부족하다.</b> 커넥션 맵은 JVM 메모리라,
 * 사용자가 붙은 인스턴스와 알림을 만든 인스턴스가 다르면 그 알림은 전송되지 않는다(다음 목록 조회에서
 * 보이므로 유실은 아니지만 실시간이 아니다 — RT-004). 그때는 {@link NotificationPushPort} 구현을
 * Redis Pub/Sub 어댑터로 바꿔 발행하고, 각 인스턴스가 구독해서 자기 맵으로 흘리면 된다
 * (Redis 는 세션 저장소로 이미 붙어 있다). <b>포트를 나눠 둔 이유가 이 교체다.</b>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseNotificationStreamAdapter implements NotificationStreamPort, NotificationPushPort {

    /** 구독 직후 1회. 프론트는 이걸 받을 때마다 목록을 재조회한다(RT-005) */
    private static final String EVENT_CONNECTED = "connected";

    /** 알림 1건 도착 */
    private static final String EVENT_NOTIFICATION = "notification";

    private final NotificationSseProperties properties;

    /**
     * 사번 → 그 사용자의 열린 연결들 (RT-003 — 탭마다 하나씩 생긴다).
     *
     * <p>{@code employee.user_id} 가 전역 PK 라 사번만으로 키를 잡아도 회사 간 오배달이 불가능하다.
     */
    private final Map<String, Set<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    /**
     * 하트비트 전용 스레드 1개. 하는 일이 커넥션 맵을 훑으며 주석 한 줄씩 쓰는 것뿐이라 1개로 충분하다.
     *
     * <p>🚨 <b>공유 {@code TaskScheduler} 빈을 쓰지 않는 이유는 {@link NotificationSseConfig} 주석에
     * 있다</b> — 빈으로 만들면 다른 도메인의 {@code @Scheduled} 6개가 조용히 1스레드로 좁아진다.
     * 데몬 스레드로 두어 종료를 막지 않게 한다.
     */
    private final ScheduledExecutorService heartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "notification-sse-heartbeat");
                thread.setDaemon(true);
                return thread;
            });

    /**
     * 하트비트를 건다. 중간 프록시·브라우저가 유휴 커넥션을 끊는 것을 막는다.
     *
     * <p>⚠️ 하트비트가 없으면 <b>연결이 조용히 죽는다</b> — 예외도 로그도 없이 그냥 알림이 안 오고,
     * 사용자는 연결이 끊긴 줄 모른다. 재현도 어렵다(수십 초 기다려야 나타난다).
     */
    @PostConstruct
    void scheduleHeartbeat() {
        long intervalMs = properties.heartbeatInterval().toMillis();
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, intervalMs, intervalMs, TimeUnit.MILLISECONDS);

        log.info("알림 SSE 준비 — timeout={} heartbeat={}", properties.timeout(), properties.heartbeatInterval());
    }

    @PreDestroy
    void stopHeartbeat() {
        heartbeatExecutor.shutdownNow();
    }

    /**
     * 커넥션 생성 지점. 운영 동작은 이 한 줄뿐이고, 테스트가 전송 내용을 확인할 수 있게 분리해 뒀다
     * ({@code SseNotificationStreamAdapterTest} 가 대역으로 교체한다).
     */
    SseEmitter newEmitter() {
        return new SseEmitter(properties.timeout().toMillis());
    }

    @Override
    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = newEmitter();

        emittersByUserId.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(emitter);

        // 세 콜백 모두에서 회수한다. 어느 경로로 끊겨도 맵에 죽은 연결이 남지 않아야 한다.
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onError(throwable -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            // RT-006 — 타임아웃은 오류가 아니다. 정상 종료시키면 브라우저가 알아서 다시 붙는다.
            remove(userId, emitter);
            emitter.complete();
        });

        // ⚠️ 이 첫 이벤트를 빼면 프록시가 응답 헤더를 흘려보내지 않아 브라우저의 onopen 이
        //    한참 뒤에야(또는 첫 알림이 올 때까지) 안 뜬다. 연결 확인 겸 헤더 flush 용이다.
        send(userId, emitter, EVENT_CONNECTED, Map.of("userId", userId));

        log.debug("알림 SSE 구독 - userId={} 연결수={}", userId, count(userId));
        return emitter;
    }

    @Override
    public void push(String userId, NotificationResult notification) {
        Set<SseEmitter> emitters = emittersByUserId.get(userId);

        // RT-004 — 접속 중이 아닌 사용자가 대부분이다. 연결이 없는 건 정상이며 오류가 아니다.
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        emitters.forEach(emitter -> send(userId, emitter, EVENT_NOTIFICATION, notification));
    }

    /**
     * SSE 주석 줄({@code :ping})을 보낸다. {@code EventSource} 이벤트로 올라오지 않으므로
     * 프론트가 처리할 것이 없고, 커넥션만 살아 있게 한다.
     */
    private void sendHeartbeat() {
        // ⚠️ 이 try 를 걷어내지 마라. scheduleAtFixedRate 는 작업이 예외를 던지면 다음 실행을
        //    **말없이 취소한다** — 이후 하트비트가 영구히 멈추고, 로그도 예외도 남지 않는다.
        //    아래 내부 catch 로 대부분 걸리지만 마지막 방어선을 둔다.
        try {
            emittersByUserId.forEach((userId, emitters) -> emitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    // 끊긴 연결을 여기서 처음 발견하는 경우가 많다. 콜백이 안 불린 채 죽은 것을 회수한다.
                    discard(userId, emitter, e);
                }
            }));
        } catch (Exception e) {
            log.warn("알림 SSE 하트비트 처리 중 예외 - 다음 주기에 다시 시도한다", e);
        }
    }

    /**
     * 이벤트 1건 전송.
     *
     * <p>⚠️ {@code data(Object)} 만 쓰면 {@code text/plain} 으로 처리돼 JSON 이 나가지 않는다.
     * {@link MediaType#APPLICATION_JSON} 을 <b>명시</b>해야 목록 API 와 같은 필드 구조로 직렬화된다
     * (프론트가 목록 항목과 같은 렌더러를 쓴다 — §5).
     */
    private void send(String userId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            // IOException(브라우저가 닫음)뿐 아니라 IllegalStateException(이미 완료된 emitter)도 잡는다.
            // RT-004 — 실패를 위로 던지지 않는다. 알림 row 는 이미 커밋돼 목록 조회로 보인다.
            discard(userId, emitter, e);
        }
    }

    /** 전송에 실패한 연결을 맵에서 빼고 닫는다. 닫는 것까지 실패해도 무시한다(이미 죽은 연결이다). */
    private void discard(String userId, SseEmitter emitter, Exception cause) {
        remove(userId, emitter);
        log.debug("알림 SSE 전송 실패로 연결 정리 - userId={} 사유={}", userId, cause.toString());
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // 이미 닫힌 연결을 다시 닫는 것은 실패해도 문제가 없다
        }
    }

    /**
     * 연결 회수.
     *
     * <p>⚠️ 마지막 연결이 빠지면 사번 키까지 지운다 — 안 지우면 로그인했다 나간 사용자의 <b>빈
     * Set 이 영구히 쌓인다</b>(사용자 수만큼 늘고 줄지 않는다). {@code compute} 로 원자적으로 처리해
     * "지우는 사이에 새 탭이 구독하는" 경합에서 연결이 사라지지 않게 한다.
     */
    private void remove(String userId, SseEmitter emitter) {
        emittersByUserId.compute(userId, (key, emitters) -> {
            if (emitters == null) {
                return null;
            }
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }

    private int count(String userId) {
        Set<SseEmitter> emitters = emittersByUserId.get(userId);
        return emitters == null ? 0 : emitters.size();
    }
}
