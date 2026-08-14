package com.group3.vitamins.notification.infrastructure.sse;

import com.group3.vitamins.notification.application.result.NotificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SseNotificationStreamAdapter — 실시간 전송 (§5 RT-001~007)")
class SseNotificationStreamAdapterTest {

    private static final String USER_A = "EMP001";
    private static final String USER_B = "EMP003";
    private static final String SESSION_A = "session-a";
    private static final String SESSION_B = "session-b";

    /** 와이어 형식 {@code "event:{이름}\ndata:"} 에서 이벤트 이름만 뽑는다 */
    private static final Pattern EVENT_NAME = Pattern.compile("event:(\\S+)");

    private SseNotificationStreamAdapter adapter;
    private FindByIndexNameSessionRepository<Session> sessionRepository;

    /** {@code subscribe()} 가 꺼내 쓸 대역 커넥션. 넣어둔 순서대로 나간다 */
    private Deque<SseEmitter> emittersToHandOut;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        emittersToHandOut = new ArrayDeque<>();
        sessionRepository = mock(FindByIndexNameSessionRepository.class);

        NotificationSseProperties properties =
                new NotificationSseProperties(Duration.ofMinutes(30), Duration.ofSeconds(15));

        // 대역으로 교체하는 것은 두 가지뿐이다 — 커넥션 생성과 전송 스레드 위임.
        // 등록·팬아웃·회수·세션 확인 로직은 운영 코드 그대로 돈다.
        // ⚠️ dispatch 를 즉시 실행으로 바꾸지 않으면 전송이 다른 스레드로 넘어가 검증이 비결정적이 된다.
        adapter = new SseNotificationStreamAdapter(properties, sessionRepository) {
            @Override
            SseEmitter newEmitter() {
                return emittersToHandOut.poll();
            }

            @Override
            void dispatch(Runnable task) {
                task.run();
            }
        };
    }

    @Nested
    @DisplayName("구독")
    class Subscribe {

        @Test
        @DisplayName("구독 직후 connected 이벤트를 보낸다 — 프록시 헤더 flush 겸 연결 확인용")
        void 구독하면_connected_를_보낸다() throws IOException {
            SseEmitter emitter = handOut();

            adapter.subscribe(USER_A, SESSION_A);

            assertThat(sentEventNames(emitter)).containsExactly("connected");
        }
    }

    @Nested
    @DisplayName("전송")
    class Push {

        @Test
        @DisplayName("RT-003 — 같은 사용자의 연결 전부에 보낸다 (탭 여러 개)")
        void 같은_사용자의_모든_연결에_보낸다() throws IOException {
            SseEmitter tab1 = handOut();
            SseEmitter tab2 = handOut();
            adapter.subscribe(USER_A, SESSION_A);
            adapter.subscribe(USER_A, SESSION_A);

            NotificationResult notification = notification();
            adapter.push(USER_A, notification);

            assertThat(sentEventNames(tab1)).containsExactly("connected", "notification");
            assertThat(sentEventNames(tab2)).containsExactly("connected", "notification");
            assertThat(sentData(tab1)).contains(notification);
            assertThat(sentData(tab2)).contains(notification);
        }

        @Test
        @DisplayName("RT-001 — 다른 사용자의 연결로는 나가지 않는다")
        void 다른_사용자에게는_보내지_않는다() throws IOException {
            SseEmitter emitterA = handOut();
            SseEmitter emitterB = handOut();
            adapter.subscribe(USER_A, SESSION_A);
            adapter.subscribe(USER_B, SESSION_B);

            adapter.push(USER_A, notification());

            assertThat(sentEventNames(emitterA)).contains("notification");
            // B 는 구독 시점의 connected 만 받았어야 한다
            assertThat(sentEventNames(emitterB)).containsExactly("connected");
        }

        @Test
        @DisplayName("RT-004 — 접속 중이 아닌 사용자에게 보내도 예외가 나지 않는다")
        void 연결이_없으면_조용히_넘어간다() {
            assertThatCode(() -> adapter.push(USER_A, notification())).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("RT-004 — 전송이 실패해도 예외를 위로 던지지 않는다 (알림 row 는 이미 저장됐다)")
        void 전송_실패를_삼킨다() throws IOException {
            SseEmitter broken = handOut();
            adapter.subscribe(USER_A, SESSION_A);
            doThrow(new IOException("broken pipe"))
                    .when(broken).send(any(SseEmitter.SseEventBuilder.class));

            assertThatCode(() -> adapter.push(USER_A, notification())).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("전송에 실패한 연결은 회수된다 — 다음 알림을 그 연결로 다시 시도하지 않는다")
        void 실패한_연결은_회수된다() throws IOException {
            SseEmitter broken = handOut();
            adapter.subscribe(USER_A, SESSION_A);
            doThrow(new IOException("broken pipe"))
                    .when(broken).send(any(SseEmitter.SseEventBuilder.class));

            adapter.push(USER_A, notification());   // 실패 → 회수
            adapter.push(USER_A, notification());   // 회수됐으므로 시도조차 하지 않는다

            // connected(1) + 첫 push(1) = 2회. 회수가 안 되면 3회가 된다.
            verify(broken, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        }

        @Test
        @DisplayName("I/O 로 끊긴 연결에는 complete() 를 부르지 않는다 — 부르면 ERROR 500 로그가 남는다")
        void 끊긴_연결은_complete_하지_않는다() throws IOException {
            SseEmitter broken = handOut();
            adapter.subscribe(USER_A, SESSION_A);
            doThrow(new IOException("broken pipe"))
                    .when(broken).send(any(SseEmitter.SseEventBuilder.class));

            adapter.push(USER_A, notification());

            // complete() 는 내부 flush 실패를 deferredResult 로 넘겨 ASYNC ERROR 디스패치를 만든다.
            // 그 결과가 `[500] GET /api/v1/notifications/stream` 이다 — 정상 종료인데 ERROR 로 잡힌다.
            verify(broken, never()).complete();
        }
    }

    @Nested
    @DisplayName("연결 회수")
    class Cleanup {

        @Test
        @DisplayName("브라우저가 연결을 닫으면(onCompletion) 이후 전송 대상에서 빠진다")
        void 완료된_연결은_대상에서_빠진다() throws IOException {
            SseEmitter emitter = handOut();
            adapter.subscribe(USER_A, SESSION_A);

            runCallback(emitter, CallbackKind.COMPLETION);
            adapter.push(USER_A, notification());

            assertThat(sentEventNames(emitter)).containsExactly("connected");
        }

        @Test
        @DisplayName("RT-006 — 30분 타임아웃 시 emitter 를 정상 종료하고 전송 대상에서 뺀다")
        void 타임아웃된_연결은_종료되고_대상에서_빠진다() throws IOException {
            SseEmitter emitter = handOut();
            adapter.subscribe(USER_A, SESSION_A);

            runCallback(emitter, CallbackKind.TIMEOUT);

            // 오류가 아니라 정상 종료다 — completeWithError 가 아니라 complete 여야 브라우저가 조용히 재연결한다
            verify(emitter).complete();

            adapter.push(USER_A, notification());
            assertThat(sentEventNames(emitter)).containsExactly("connected");
        }

        @Test
        @DisplayName("RT-006 — 하트비트는 살아 있는 연결에 ping(주석)을 보낸다. 이벤트가 아니어야 한다")
        void 하트비트는_주석을_보낸다() throws IOException {
            SseEmitter emitter = handOut();
            aliveSession(SESSION_A);
            adapter.subscribe(USER_A, SESSION_A);

            adapter.sendHeartbeat();

            // ping 은 SseEventBuilder 의 comment 라 event 이름이 없다 → 이벤트 목록은 그대로다
            assertThat(sentEventNames(emitter)).containsExactly("connected");
            verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        }
    }

    @Nested
    @DisplayName("RT-007 — 세션이 죽으면 구독도 끊는다")
    class SessionInvalidation {

        @Test
        @DisplayName("로그아웃·세션 만료로 세션이 사라지면 하트비트가 연결을 종료한다")
        void 세션이_사라지면_연결을_끊는다() throws IOException {
            SseEmitter emitter = handOut();
            adapter.subscribe(USER_A, SESSION_A);
            when(sessionRepository.findById(SESSION_A)).thenReturn(null);   // 로그아웃

            adapter.sendHeartbeat();

            verify(emitter).complete();

            // 끊긴 뒤에는 알림이 그 연결로 가지 않는다 — 로그아웃 후 알림 노출 차단
            adapter.push(USER_A, notification());
            assertThat(sentEventNames(emitter)).containsExactly("connected");
        }

        @Test
        @DisplayName("만료된 세션(삭제 전)도 끊는다")
        void 만료된_세션도_끊는다() throws IOException {
            SseEmitter emitter = handOut();
            adapter.subscribe(USER_A, SESSION_A);
            when(sessionRepository.findById(SESSION_A)).thenReturn(expiredSession(SESSION_A));

            adapter.sendHeartbeat();

            verify(emitter).complete();
        }

        @Test
        @DisplayName("세션 저장소 조회가 실패하면 연결을 유지한다(fail-open) — Redis 장애로 정상 사용자를 끊지 않는다")
        void 세션_조회_실패시_유지한다() throws IOException {
            SseEmitter emitter = handOut();
            adapter.subscribe(USER_A, SESSION_A);
            when(sessionRepository.findById(SESSION_A))
                    .thenThrow(new IllegalStateException("redis down"));

            adapter.sendHeartbeat();

            adapter.push(USER_A, notification());
            assertThat(sentEventNames(emitter)).contains("notification");
        }
    }

    // ── 헬퍼 ────────────────────────────────────────────────

    private enum CallbackKind { COMPLETION, TIMEOUT }

    private SseEmitter handOut() {
        SseEmitter emitter = mock(SseEmitter.class);
        emittersToHandOut.add(emitter);
        return emitter;
    }

    private void aliveSession(String sessionId) {
        when(sessionRepository.findById(sessionId)).thenReturn(new MapSession(sessionId));
    }

    private Session expiredSession(String sessionId) {
        MapSession session = new MapSession(sessionId);
        session.setMaxInactiveInterval(Duration.ofMinutes(30));
        session.setLastAccessedTime(Instant.now().minus(Duration.ofHours(1)));
        return session;
    }

    /** emitter 에 등록된 콜백을 실제로 실행해 브라우저 종료·타임아웃을 재현한다. */
    private void runCallback(SseEmitter emitter, CallbackKind kind) {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        if (kind == CallbackKind.COMPLETION) {
            verify(emitter).onCompletion(captor.capture());
        } else {
            verify(emitter).onTimeout(captor.capture());
        }
        captor.getValue().run();
    }

    /** 그 커넥션으로 나간 이벤트 이름들 (보낸 순서대로). 예: {@code ["connected", "notification"]} */
    private List<String> sentEventNames(SseEmitter emitter) throws IOException {
        List<String> names = new ArrayList<>();
        for (Object payload : sentPayloads(emitter)) {
            // 와이어 형식은 "event:{이름}\ndata:" 한 덩어리로 합쳐져 나온다 — 이름만 뽑는다
            if (payload instanceof String text) {
                Matcher matcher = EVENT_NAME.matcher(text);
                if (matcher.find()) {
                    names.add(matcher.group(1));
                }
            }
        }
        return names;
    }

    /** 그 커넥션으로 나간 본문 객체들 (직렬화 전 원본). 와이어 형식 문자열은 제외한다. */
    private List<Object> sentData(SseEmitter emitter) throws IOException {
        return sentPayloads(emitter).stream()
                .filter(payload -> !(payload instanceof String))
                .toList();
    }

    /**
     * 그 커넥션으로 나간 모든 조각(와이어 문자열 + 본문 객체).
     *
     * <p>{@code atLeast(0)} 은 "검증이 아니라 캡처가 목적"이라는 뜻이다 — connected 만 받은 커넥션도
     * 그대로 통과해야 한다.
     */
    private List<Object> sentPayloads(SseEmitter emitter) throws IOException {
        ArgumentCaptor<SseEmitter.SseEventBuilder> captor =
                ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter, atLeast(0)).send(captor.capture());

        List<Object> payloads = new ArrayList<>();
        for (SseEmitter.SseEventBuilder builder : captor.getAllValues()) {
            for (ResponseBodyEmitter.DataWithMediaType data : builder.build()) {
                payloads.add(data.getData());
            }
        }
        return payloads;
    }

    private NotificationResult notification() {
        return new NotificationResult(301L, "APPROVAL_REQUESTED", "결재 요청",
                "출장비 정산 결재 요청이 도착했습니다.", null, LocalDateTime.of(2026, 8, 12, 9, 0));
    }
}
