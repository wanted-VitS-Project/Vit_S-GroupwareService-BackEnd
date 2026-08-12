package com.group3.vitamins.notification.infrastructure.sse;

import com.group3.vitamins.notification.application.port.NotificationPushPort;
import com.group3.vitamins.notification.application.port.NotificationStreamPort;
import com.group3.vitamins.notification.application.result.NotificationResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 실시간 알림 SSE 어댑터 — 커넥션 보관 · 전송 · 하트비트 · 죽은 세션 정리를 전담한다 (§5).
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

    /** 전송 스레드 수. 2 인 이유는 {@link #sendExecutor} 주석 참고 */
    private static final int SEND_THREADS = 2;

    /** 전송 큐 상한. 넘치면 그 전송을 버린다(알림 row 는 이미 커밋됨 — RT-004) */
    private static final int SEND_QUEUE_CAPACITY = 1_000;

    private final NotificationSseProperties properties;

    /**
     * RT-007 — 세션이 살아 있는지 확인해 죽은 구독을 끊는 데 쓴다.
     *
     * <p>이 프로젝트가 JWT 대신 서버측 세션을 택한 이유가 <b>로그아웃·계정 잠금·권한 변경의 즉시 반영</b>
     * 이다({@code SecurityConfig} 주석). 로그아웃은 세션만 무효화하므로, 이미 열린 SSE 연결을 함께
     * 끊지 않으면 <b>로그아웃한 뒤에도 최대 30분간 알림이 계속 흘러</b> 그 전제가 깨진다.
     */
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    /**
     * 사번 → 그 사용자의 열린 구독들 (RT-003 — 탭마다 하나씩 생긴다).
     *
     * <p>{@code employee.user_id} 가 전역 PK 라 사번만으로 키를 잡아도 회사 간 오배달이 불가능하다.
     */
    private final Map<String, Set<Subscription>> subscriptionsByUserId = new ConcurrentHashMap<>();

    /**
     * 하트비트 전용 스레드 1개. 커넥션 맵을 훑어 <b>전송을 지시</b>하기만 하고 직접 쓰지 않는다
     * ({@link #sendExecutor} 로 넘긴다) — 느린 연결 하나가 다른 연결의 하트비트를 밀어내지 않게.
     *
     * <p>🚨 <b>공유 {@code TaskScheduler} 빈을 쓰지 않는 이유는 {@link NotificationSseConfig} 주석에
     * 있다</b> — 빈으로 만들면 다른 도메인의 {@code @Scheduled} 6개가 조용히 1스레드로 좁아진다.
     */
    private final ScheduledExecutorService heartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor(daemonThreadFactory("notification-sse-heartbeat"));

    /**
     * 실제 write 를 담당하는 전송 스레드 풀.
     *
     * <p>⚠️ <b>왜 필요한가</b> — {@code SseEmitter.send()} 는 서블릿 응답에 쓰고 flush 하므로
     * <b>호출 스레드를 블로킹할 수 있다</b>(절전에 들어간 클라이언트처럼 TCP 창이 막힌 경우).
     * 예전엔 {@code push()} 가 {@code AFTER_COMMIT} 흐름에서 동기 실행돼, 잠든 브라우저 하나가
     * <b>결재 상신 요청 스레드를 붙잡을 수</b> 있었다. 전송을 여기로 넘겨 그 경로를 끊는다.
     *
     * <p>스레드 2개인 이유: 막힌 연결 하나가 큐 전체를 세우지 않게 하려면 1개보다 커야 하고,
     * 알림 규모(인원 30명)에는 그 이상이 필요 없다. 같은 emitter 에 동시 write 가 겹쳐도
     * {@code ResponseBodyEmitter} 내부 {@code writeLock} 이 직렬화하므로 응답이 섞이지 않는다
     * (Spring 6.2 확인). 다만 <b>같은 사용자에게 밀리초 간격으로 두 건이 가면 순서가 바뀔 수 있다</b> —
     * 프론트가 목록을 항상 최신순으로 정렬하므로(VIW-002) 화면에는 영향이 없다.
     *
     * <p>큐가 가득 차면 {@code AbortPolicy} 가 거부하고 {@link #dispatch} 가 로그만 남긴다.
     * 재시도·롤백하지 않는다 — 알림 row 는 이미 커밋돼 목록 조회로 보인다(RT-004).
     */
    private final ExecutorService sendExecutor = new ThreadPoolExecutor(
            SEND_THREADS, SEND_THREADS, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(SEND_QUEUE_CAPACITY),
            daemonThreadFactory("notification-sse-send"));

    /** 커넥션 하나. 세션 ID 를 함께 들고 있어야 로그아웃 시 끊을 수 있다(RT-007) */
    record Subscription(String sessionId, SseEmitter emitter) {
    }

    /**
     * 하트비트를 건다. 중간 프록시·브라우저가 유휴 커넥션을 끊는 것을 막고, 같은 주기에 죽은 세션의
     * 구독을 정리한다(RT-007).
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
    void shutdown() {
        heartbeatExecutor.shutdownNow();
        sendExecutor.shutdownNow();
    }

    /**
     * 커넥션 생성 지점. 운영 동작은 이 한 줄뿐이고, 테스트가 전송 내용을 확인할 수 있게 분리해 뒀다
     * ({@code SseNotificationStreamAdapterTest} 가 대역으로 교체한다).
     */
    SseEmitter newEmitter() {
        return new SseEmitter(properties.timeout().toMillis());
    }

    @Override
    public SseEmitter subscribe(String userId, String sessionId) {
        SseEmitter emitter = newEmitter();
        Subscription subscription = new Subscription(sessionId, emitter);

        register(userId, subscription);

        // 세 콜백 모두에서 회수한다. 어느 경로로 끊겨도 맵에 죽은 연결이 남지 않아야 한다.
        emitter.onCompletion(() -> remove(userId, subscription));
        emitter.onError(throwable -> remove(userId, subscription));
        emitter.onTimeout(() -> {
            // RT-006 — 타임아웃은 오류가 아니다. 정상 종료시키면 브라우저가 알아서 다시 붙는다.
            remove(userId, subscription);
            emitter.complete();
        });

        // ⚠️ 이 첫 이벤트를 빼면 프록시가 응답 헤더를 흘려보내지 않아 브라우저의 onopen 이
        //    한참 뒤에야(또는 첫 알림이 올 때까지) 안 뜬다. 연결 확인 겸 헤더 flush 용이다.
        //    갓 열린 연결이라 소켓 버퍼가 비어 있어 블로킹 위험이 없으므로 요청 스레드에서 바로 보낸다.
        send(userId, subscription, EVENT_CONNECTED, Map.of("userId", userId));

        log.debug("알림 SSE 구독 - userId={} 연결수={}", userId, count(userId));
        return emitter;
    }

    @Override
    public void push(String userId, NotificationResult notification) {
        Set<Subscription> subscriptions = subscriptionsByUserId.get(userId);

        // RT-004 — 접속 중이 아닌 사용자가 대부분이다. 연결이 없는 건 정상이며 오류가 아니다.
        if (subscriptions == null || subscriptions.isEmpty()) {
            return;
        }

        // ⚠️ 여기서 직접 send 하지 마라 — 이 메서드는 AFTER_COMMIT 흐름(= 결재 상신 등의 요청 스레드)에서
        //    호출된다. 막힌 클라이언트가 그 요청을 붙잡는다. sendExecutor 로 넘기는 이유다.
        subscriptions.forEach(subscription ->
                dispatch(() -> send(userId, subscription, EVENT_NOTIFICATION, notification)));
    }

    /**
     * 커넥션 등록.
     *
     * <p>⚠️ <b>{@code computeIfAbsent(...).add(...)} 로 쪼개면 안 된다.</b> 두 연산 사이에 다른
     * 스레드가 마지막 연결을 회수하면({@link #remove} 가 빈 Set 과 함께 키를 지운다) 새 구독이
     * <b>맵에서 떨어진 Set</b> 에 담긴다. 그러면 {@code push()} 가 그 연결을 못 찾아 <b>예외도 없이
     * 알림만 안 간다.</b> 생성과 추가를 {@code compute} 안에서 함께 해 키 단위로 원자화한다.
     */
    private void register(String userId, Subscription subscription) {
        subscriptionsByUserId.compute(userId, (key, subscriptions) -> {
            Set<Subscription> target = (subscriptions == null) ? ConcurrentHashMap.newKeySet() : subscriptions;
            target.add(subscription);
            return target;
        });
    }

    /**
     * 하트비트 1회 — 살아 있는 구독엔 주석({@code :ping})을, 세션이 죽은 구독은 종료시킨다.
     *
     * <p>패키지 전용인 이유: 스케줄러가 부르는 것이 정상 경로이고, 테스트가 주기를 기다리지 않고
     * 직접 한 번 돌리기 위해 열어 둔다.
     */
    void sendHeartbeat() {
        // ⚠️ 이 try 를 걷어내지 마라. scheduleAtFixedRate 는 작업이 예외를 던지면 다음 실행을
        //    **말없이 취소한다** — 이후 하트비트가 영구히 멈추고, 로그도 예외도 남지 않는다.
        try {
            // 탭이 여러 개면 sessionId 가 같다. 라운드 안에서 세션 조회를 한 번으로 줄인다.
            Map<String, Boolean> sessionAliveCache = new HashMap<>();

            subscriptionsByUserId.forEach((userId, subscriptions) -> subscriptions.forEach(subscription -> {
                boolean alive = sessionAliveCache.computeIfAbsent(
                        subscription.sessionId(), this::sessionAlive);

                if (!alive) {
                    // RT-007 — 로그아웃·세션 만료·계정 잠금. 재연결하면 401 이라 여기서 끊는 게 맞다.
                    log.debug("알림 SSE 세션 종료로 연결 정리 - userId={}", userId);
                    discard(userId, subscription);
                    return;
                }
                dispatch(() -> ping(userId, subscription));
            }));
        } catch (Exception e) {
            log.warn("알림 SSE 하트비트 처리 중 예외 - 다음 주기에 다시 시도한다", e);
        }
    }

    /**
     * 세션 생존 확인.
     *
     * <p>⚠️ Redis 장애 시 <b>살아 있다고 본다(fail-open).</b> 저장소 오류로 정상 사용자의 알림을
     * 끊는 쪽이, 로그아웃한 연결이 최대 30분(emitter timeout) 남는 것보다 나쁘다고 판단했다.
     */
    private boolean sessionAlive(String sessionId) {
        try {
            Session session = sessionRepository.findById(sessionId);
            return session != null && !session.isExpired();
        } catch (RuntimeException e) {
            log.warn("알림 SSE 세션 확인 실패 - 연결을 유지한다", e);
            return true;
        }
    }

    /** SSE 주석 줄. {@code EventSource} 이벤트로 올라오지 않으므로 프론트가 처리할 것이 없다. */
    private void ping(String userId, Subscription subscription) {
        try {
            subscription.emitter().send(SseEmitter.event().comment("ping"));
        } catch (Exception e) {
            // 끊긴 연결을 여기서 처음 발견하는 경우가 많다. 콜백이 안 불린 채 죽은 것을 회수한다.
            log.debug("알림 SSE 하트비트 실패로 연결 정리 - userId={} 사유={}", userId, e.toString());
            discard(userId, subscription);
        }
    }

    /**
     * 이벤트 1건 전송.
     *
     * <p>⚠️ {@code data(Object)} 만 쓰면 {@code text/plain} 으로 처리돼 JSON 이 나가지 않는다.
     * {@link MediaType#APPLICATION_JSON} 을 <b>명시</b>해야 목록 API 와 같은 필드 구조로 직렬화된다
     * (프론트가 목록 항목과 같은 렌더러를 쓴다 — §5).
     */
    private void send(String userId, Subscription subscription, String eventName, Object data) {
        try {
            subscription.emitter().send(
                    SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            // IOException(브라우저가 닫음)뿐 아니라 IllegalStateException(이미 완료된 emitter)도 잡는다.
            // RT-004 — 실패를 위로 던지지 않는다. 알림 row 는 이미 커밋돼 목록 조회로 보인다.
            log.debug("알림 SSE 전송 실패로 연결 정리 - userId={} 사유={}", userId, e.toString());
            discard(userId, subscription);
        }
    }

    /**
     * 전송 작업을 전송 스레드로 넘긴다.
     *
     * <p>패키지 전용인 이유: 테스트가 즉시 실행으로 바꿔 검증을 결정적으로 만든다.
     */
    void dispatch(Runnable task) {
        try {
            sendExecutor.execute(task);
        } catch (RejectedExecutionException e) {
            // 큐 포화. 재시도하지 않는다 — 목록 조회가 fallback 이다(RT-004).
            log.warn("알림 SSE 전송 큐 포화 - 이번 실시간 전송을 버린다(알림은 목록 조회로 보인다)");
        }
    }

    /** 연결을 맵에서 빼고 닫는다. 닫는 것까지 실패해도 무시한다(이미 죽은 연결이다). */
    private void discard(String userId, Subscription subscription) {
        remove(userId, subscription);
        try {
            subscription.emitter().complete();
        } catch (Exception ignored) {
            // 이미 닫힌 연결을 다시 닫는 것은 실패해도 문제가 없다
        }
    }

    /**
     * 연결 회수.
     *
     * <p>⚠️ 마지막 연결이 빠지면 사번 키까지 지운다 — 안 지우면 로그인했다 나간 사용자의 <b>빈
     * Set 이 영구히 쌓인다</b>(사용자 수만큼 늘고 줄지 않는다). {@code compute} 로 원자적으로 처리해
     * {@link #register} 와의 경합에서 연결이 사라지지 않게 한다.
     */
    private void remove(String userId, Subscription subscription) {
        subscriptionsByUserId.compute(userId, (key, subscriptions) -> {
            if (subscriptions == null) {
                return null;
            }
            subscriptions.remove(subscription);
            return subscriptions.isEmpty() ? null : subscriptions;
        });
    }

    private int count(String userId) {
        Set<Subscription> subscriptions = subscriptionsByUserId.get(userId);
        return subscriptions == null ? 0 : subscriptions.size();
    }

    private static java.util.concurrent.ThreadFactory daemonThreadFactory(String namePrefix) {
        AtomicInteger sequence = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, namePrefix + "-" + sequence.incrementAndGet());
            // 데몬으로 둬야 종료를 막지 않는다
            thread.setDaemon(true);
            return thread;
        };
    }
}
