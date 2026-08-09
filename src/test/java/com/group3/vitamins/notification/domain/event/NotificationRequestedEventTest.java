package com.group3.vitamins.notification.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 알림 생성 계약(GEN-001 · GEN-005)의 입력 경계 테스트.
 *
 * <p>이 이벤트는 <b>모든 도메인이 알림을 만드는 유일한 통로</b>라, 여기서 막지 못한 잘못된 조합은
 * 그대로 {@code notification} 행이 된다. DB {@code ck_notification_target} 제약과 같은 규칙을
 * 발행 시점에 먼저 강제하는지 확인한다.
 */
@DisplayName("NotificationRequestedEvent")
class NotificationRequestedEventTest {

    private static final String USER_ID = "EMP003";
    private static final String TYPE = "APPROVAL_REQUESTED";
    private static final String TITLE = "결재 요청";
    private static final String MESSAGE = "출장비 정산 결재 요청이 도착했습니다.";

    @Nested
    @DisplayName("이동 대상(GEN-005)")
    class Target {

        @Test
        @DisplayName("대상과 부가 식별값을 함께 담아 생성한다")
        void withTarget() {
            NotificationRequestedEvent event = NotificationRequestedEvent.of(
                    USER_ID, TYPE, TITLE, MESSAGE, "APPROVAL", 55L, Map.of("revisionId", 56L));

            assertThat(event.targetType()).isEqualTo("APPROVAL");
            assertThat(event.targetId()).isEqualTo(55L);
            assertThat(event.targetContext()).containsEntry("revisionId", 56L);
        }

        @Test
        @DisplayName("이동 대상이 없는 알림은 세 값이 모두 null 이다")
        void withoutTarget() {
            NotificationRequestedEvent event =
                    NotificationRequestedEvent.of(USER_ID, TYPE, TITLE, MESSAGE);

            assertThat(event.targetType()).isNull();
            assertThat(event.targetId()).isNull();
            assertThat(event.targetContext()).isNull();
        }

        @Test
        @DisplayName("targetType 만 있고 targetId 가 없으면 거부한다")
        void typeWithoutId() {
            assertThatThrownBy(() -> NotificationRequestedEvent.of(
                    USER_ID, TYPE, TITLE, MESSAGE, "APPROVAL", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("both present or both absent");
        }

        @Test
        @DisplayName("targetId 만 있고 targetType 이 없으면 거부한다")
        void idWithoutType() {
            assertThatThrownBy(() -> NotificationRequestedEvent.of(
                    USER_ID, TYPE, TITLE, MESSAGE, null, 55L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("both present or both absent");
        }

        @Test
        @DisplayName("공백 targetType 은 거부한다 — 해석 불가능한 대상이 저장되는 걸 막는다")
        void blankTargetType() {
            assertThatThrownBy(() -> NotificationRequestedEvent.of(
                    USER_ID, TYPE, TITLE, MESSAGE, "   ", 55L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("targetType must not be blank");
        }

        @Test
        @DisplayName("대상 없이 targetContext 만 주면 거부한다 — 쓰이지 않는 값이 영속되는 걸 막는다")
        void contextWithoutTarget() {
            assertThatThrownBy(() -> NotificationRequestedEvent.of(
                    USER_ID, TYPE, TITLE, MESSAGE, null, null, Map.of("revisionId", 56L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("targetContext requires a target");
        }

        @Test
        @DisplayName("빈 targetContext 는 null 로 눕힌다 — 대상이 없어도 통과한다")
        void emptyContextNormalizedToNull() {
            NotificationRequestedEvent event = NotificationRequestedEvent.of(
                    USER_ID, TYPE, TITLE, MESSAGE, null, null, Map.of());

            assertThat(event.targetContext()).isNull();
        }

        @Test
        @DisplayName("targetContext 는 방어 복사한다 — 발행 후 원본을 바꿔도 이벤트는 안 변한다")
        void contextIsDefensivelyCopied() {
            Map<String, Object> mutable = new HashMap<>();
            mutable.put("revisionId", 56L);

            NotificationRequestedEvent event = NotificationRequestedEvent.of(
                    USER_ID, TYPE, TITLE, MESSAGE, "APPROVAL", 55L, mutable);
            mutable.put("revisionId", 999L);

            assertThat(event.targetContext()).containsEntry("revisionId", 56L);
        }
    }

    @Nested
    @DisplayName("필수 값")
    class Required {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("recipientUserId 가 비어 있으면 거부한다")
        void blankRecipient(String recipientUserId) {
            assertThatThrownBy(() -> NotificationRequestedEvent.of(recipientUserId, TYPE, TITLE, MESSAGE))
                    .isInstanceOf(RuntimeException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("notificationType 이 비어 있으면 거부한다")
        void blankNotificationType(String notificationType) {
            assertThatThrownBy(() -> NotificationRequestedEvent.of(USER_ID, notificationType, TITLE, MESSAGE))
                    .isInstanceOf(RuntimeException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("title 이 비어 있으면 거부한다")
        void blankTitle(String title) {
            assertThatThrownBy(() -> NotificationRequestedEvent.of(USER_ID, TYPE, title, MESSAGE))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("message 는 비어도 된다 — 제목만으로 충분한 알림이 있다")
        void messageIsOptional() {
            assertThatCode(() -> NotificationRequestedEvent.of(USER_ID, TYPE, TITLE, null))
                    .doesNotThrowAnyException();
        }
    }
}
