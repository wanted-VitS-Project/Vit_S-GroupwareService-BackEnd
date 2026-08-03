package com.group3.vitamins.global.infrastructure.config.security;

import com.group3.vitamins.global.domain.common.error.exception.PasswordHashingBusyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 세마포어 데코레이터의 불변식 검증.
 *
 * <p>실제 Argon2 를 쓰지 않는다 — 여기서 검증할 것은 해시 성능이 아니라 <b>permit 관리</b>다.
 * 파라미터 실측은 {@code Argon2BenchmarkTest} 가 따로 한다.
 */
@DisplayName("ThrottledPasswordEncoder")
class ThrottledPasswordEncoderTest {

    @Test
    @DisplayName("permit 이 모두 사용 중이면 대기 시간 초과 후 503 예외를 던진다")
    void throwsBusyWhenAllPermitsHeld() throws Exception {
        CountDownLatch insideEncode = new CountDownLatch(1);
        CountDownLatch releaseEncode = new CountDownLatch(1);

        PasswordEncoder encoder = new ThrottledPasswordEncoder(
                blockingEncoder(insideEncode, releaseEncode), 1, Duration.ofMillis(50));

        Thread holder = new Thread(() -> encoder.encode("first"));
        holder.start();
        assertThat(insideEncode.await(1, TimeUnit.SECONDS)).isTrue();   // 첫 스레드가 permit 을 잡을 때까지 대기

        assertThatThrownBy(() -> encoder.encode("second"))
                .isInstanceOf(PasswordHashingBusyException.class)
                .satisfies(e -> assertThat(((PasswordHashingBusyException) e).getHttpStatus()).isEqualTo(503));

        releaseEncode.countDown();
        holder.join(1_000);
    }

    @Test
    @DisplayName("해시 도중 예외가 나도 permit 을 반납한다")
    void releasesPermitWhenDelegateThrows() {
        PasswordEncoder encoder = new ThrottledPasswordEncoder(
                throwingEncoder(), 1, Duration.ofMillis(50));

        // permit 이 1개뿐이므로, 반납되지 않으면 두 번째 호출이 PasswordHashingBusyException 으로 바뀐다.
        for (int attempt = 0; attempt < 3; attempt++) {
            assertThatThrownBy(() -> encoder.encode("boom"))
                    .isInstanceOf(IllegalStateException.class)
                    .isNotInstanceOf(PasswordHashingBusyException.class);
        }
    }

    @Test
    @DisplayName("permit 은 사용 후 반납되어 재사용된다")
    void reusesPermitAfterCompletion() {
        AtomicInteger calls = new AtomicInteger();
        PasswordEncoder encoder = new ThrottledPasswordEncoder(
                countingEncoder(calls), 1, Duration.ofMillis(50));

        for (int i = 0; i < 5; i++) {
            assertThatCode(() -> encoder.encode("pw")).doesNotThrowAnyException();
        }
        assertThat(calls.get()).isEqualTo(5);
    }

    @Test
    @DisplayName("upgradeEncoding 을 위임한다 — 위임하지 않으면 파라미터 상향 시 재해싱이 죽는다")
    void delegatesUpgradeEncoding() {
        PasswordEncoder encoder = new ThrottledPasswordEncoder(
                upgradeRequiredEncoder(), 1, Duration.ofMillis(50));

        assertThat(encoder.upgradeEncoding("$argon2id$...")).isTrue();
    }

    // ===== 테스트 더블 =====

    private PasswordEncoder blockingEncoder(CountDownLatch entered, CountDownLatch release) {
        return new StubPasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                entered.countDown();
                try {
                    release.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "hashed";
            }
        };
    }

    private PasswordEncoder throwingEncoder() {
        return new StubPasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                throw new IllegalStateException("해시 실패");
            }
        };
    }

    private PasswordEncoder countingEncoder(AtomicInteger counter) {
        return new StubPasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                counter.incrementAndGet();
                return "hashed";
            }
        };
    }

    private PasswordEncoder upgradeRequiredEncoder() {
        return new StubPasswordEncoder() {
            @Override
            public boolean upgradeEncoding(String encodedPassword) {
                return true;
            }
        };
    }

    private abstract static class StubPasswordEncoder implements PasswordEncoder {

        @Override
        public String encode(CharSequence rawPassword) {
            return "hashed";
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return true;
        }
    }
}
