package com.group3.vitamins.notification.infrastructure.sse;

import com.group3.vitamins.notification.application.result.NotificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("SseNotificationStreamAdapter — 실시간 전송 (§5 RT-001~004)")
class SseNotificationStreamAdapterTest {

    private static final String USER_A = "EMP001";
    private static final String USER_B = "EMP003";

    /** 와이어 형식 {@code "event:{이름}\ndata:"} 에서 이벤트 이름만 뽑는다 */
    private static final Pattern EVENT_NAME = Pattern.compile("event:(\\S+)");

    private SseNotificationStreamAdapter adapter;

    /** {@code subscribe()} 가 꺼내 쓸 대역 커넥션. 넣어둔 순서대로 나간다 */
    private Deque<SseEmitter> emittersToHandOut;

    @BeforeEach
    void setUp() {
        emittersToHandOut = new ArrayDeque<>();

        NotificationSseProperties properties =
                new NotificationSseProperties(Duration.ofMinutes(30), Duration.ofSeconds(15));

        // 실제 SseEmitter 는 서블릿 비동기 컨텍스트 없이는 전송 내용을 확인할 수 없어 대역으로 교체한다.
        // 교체하는 것은 커넥션 생성뿐이고 등록·팬아웃·회수 로직은 운영 코드 그대로 돈다.
        // 하트비트는 @PostConstruct 라 직접 생성하는 이 테스트에서는 돌지 않는다.
        adapter = new SseNotificationStreamAdapter(properties) {
            @Override
            SseEmitter newEmitter() {
                return emittersToHandOut.poll();
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

            adapter.subscribe(USER_A);

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
            adapter.subscribe(USER_A);
            adapter.subscribe(USER_A);

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
            adapter.subscribe(USER_A);
            adapter.subscribe(USER_B);

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
            adapter.subscribe(USER_A);
            doThrow(new IOException("broken pipe"))
                    .when(broken).send(any(SseEmitter.SseEventBuilder.class));

            assertThatCode(() -> adapter.push(USER_A, notification())).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("전송에 실패한 연결은 회수된다 — 다음 알림을 그 연결로 다시 시도하지 않는다")
        void 실패한_연결은_회수된다() throws IOException {
            SseEmitter broken = handOut();
            adapter.subscribe(USER_A);
            doThrow(new IOException("broken pipe"))
                    .when(broken).send(any(SseEmitter.SseEventBuilder.class));

            adapter.push(USER_A, notification());   // 실패 → 회수
            adapter.push(USER_A, notification());   // 회수됐으므로 시도조차 하지 않는다

            // connected(1) + 첫 push(1) = 2회. 회수가 안 되면 3회가 된다.
            verify(broken, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        }
    }

    @Nested
    @DisplayName("연결 회수")
    class Cleanup {

        @Test
        @DisplayName("브라우저가 연결을 닫으면(onCompletion) 이후 전송 대상에서 빠진다")
        void 완료된_연결은_대상에서_빠진다() throws IOException {
            SseEmitter emitter = handOut();
            adapter.subscribe(USER_A);

            triggerCompletion(emitter);
            adapter.push(USER_A, notification());

            assertThat(sentEventNames(emitter)).containsExactly("connected");
        }
    }

    // ── 헬퍼 ────────────────────────────────────────────────

    private SseEmitter handOut() {
        SseEmitter emitter = mock(SseEmitter.class);
        emittersToHandOut.add(emitter);
        return emitter;
    }

    /** {@code onCompletion} 으로 등록된 콜백을 실제로 실행해 브라우저 종료를 재현한다. */
    private void triggerCompletion(SseEmitter emitter) {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(emitter).onCompletion(captor.capture());
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
