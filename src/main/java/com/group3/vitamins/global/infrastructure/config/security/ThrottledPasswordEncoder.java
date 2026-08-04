package com.group3.vitamins.global.infrastructure.config.security;

import com.group3.vitamins.global.domain.common.error.exception.PasswordHashingBusyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Argon2 해시의 <b>동시 실행 개수를 제한</b>하는 데코레이터.
 *
 * <h3>왜 필요한가</h3>
 * 해시 1회는 {@code memoryKb} 만큼(현재 64MB) 힙을 잡는다. Tomcat 워커 스레드는 기본 200개이고,
 * 로그인 API 는 인증 없이 호출된다. 즉 <b>동시에 몇 개가 돌지를 공격자가 정한다.</b>
 *
 * <pre>
 *   없는 사번으로 동시 30건 (요청 하나 200바이트)
 *     → 30 × 64MB = 1.9GB 요청 → OOM → 로그인뿐 아니라 앱 전체 정지
 * </pre>
 *
 * <p>계정이 없어도 더미 해시를 돌려야 한다(응답 시간 차이로 사번 존재 여부가 새는 계정 열거 방지).
 * 그래서 <b>미인증 공격자도 메모리를 태울 수 있다.</b>
 *
 * <h3>Argon2PasswordEncoder 를 직접 빈으로 노출하지 않는 이유</h3>
 * 감싼 것만 주입되게 해야 누가 어디서 호출하든 반드시 이 제한을 통과한다. 우회 경로를 남기지 않는다.
 */
@Slf4j
public class ThrottledPasswordEncoder implements PasswordEncoder {

    private final PasswordEncoder delegate;
    private final Semaphore permits;
    private final Duration loginWait;
    private final Duration bulkWait;

    /**
     * @param permitCount 동시 해시 허용 개수 (= 메모리 상한을 정하는 값)
     * @param loginWait   로그인·단건 경로에서 permit 을 못 잡았을 때 기다릴 시간. <b>무한 대기는 금지</b> —
     *                    Tomcat 스레드가 전부 줄에 묶여 다른 API 까지 멈춘다
     * @param bulkWait    요청 1건이 해시 N 회를 도는 경로(비밀번호 일괄 재설정)의 대기 한도.
     *                    로그인보다 길게 잡아 몰릴 때 성급히 503 을 내지 않게 한다
     */
    public ThrottledPasswordEncoder(PasswordEncoder delegate, int permitCount,
                                    Duration loginWait, Duration bulkWait) {
        // 설정 오타 한 줄로 인증 전체가 마비된다. permit 이 0 이면 모든 로그인이 503 이다.
        // 조용히 뜨는 것보다 기동을 막는 편이 안전하다.
        if (delegate == null) {
            throw new IllegalArgumentException("delegate 는 필수다");
        }
        if (permitCount < 1) {
            throw new IllegalArgumentException("permitCount 는 1 이상이어야 한다: " + permitCount);
        }
        if (isNonPositive(loginWait) || isNonPositive(bulkWait)) {
            throw new IllegalArgumentException(
                    "loginWait · bulkWait 은 0 보다 커야 한다: loginWait=" + loginWait + " bulkWait=" + bulkWait);
        }
        this.delegate = delegate;
        // fair = true : 붐빌 때 나중에 온 요청이 새치기해 먼저 온 사용자가 계속 밀리는 것을 막는다
        this.permits = new Semaphore(permitCount, true);
        this.loginWait = loginWait;
        this.bulkWait = bulkWait;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return withPermit(loginWait, () -> delegate.encode(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return withPermit(loginWait, () -> delegate.matches(rawPassword, encodedPassword));
    }

    /**
     * 일괄 재설정용 해시. {@link #encode} 와 <b>같은 세마포어</b>를 쓰되 대기 한도만 {@code bulkWait} 다.
     *
     * <p>🚨 별도 인코더 빈으로 분리하면 세마포어가 2개가 되어 동시 해시 상한이 {@code 2 × permit} 으로
     * 뚫린다 — 힙 안전밸브가 무너진다. 그래서 메서드로만 노출하고 permit 은 공유한다.
     */
    public String encodeBulk(CharSequence rawPassword) {
        return withPermit(bulkWait, () -> delegate.encode(rawPassword));
    }

    /**
     * ⚠️ 반드시 위임해야 한다. {@link PasswordEncoder} 의 기본 구현은 {@code false} 라
     * 오버라이드하지 않으면 <b>조용히 재해싱 경로가 죽는다</b>.
     *
     * <p>이 메서드가 살아 있어야 나중에 파라미터를 올렸을 때 로그인 시점에 새 파라미터로 다시 저장할 수 있다.
     * 즉 오늘 정한 m·t 는 되돌릴 수 없는 결정이 아니다.
     */
    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return delegate.upgradeEncoding(encodedPassword);
    }

    private <T> T withPermit(Duration waitTimeout, Supplier<T> task) {
        boolean acquired = false;
        try {
            acquired = permits.tryAcquire(waitTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("비밀번호 해시 대기 시간 초과 — 동시 요청이 몰렸다. 대기 한도={}", waitTimeout);
                throw new PasswordHashingBusyException();
            }
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PasswordHashingBusyException(e);
        } finally {
            // 예외가 나도 반드시 반납한다. 하나라도 새면 permit 이 0 이 되어 아무도 로그인하지 못한다.
            if (acquired) {
                permits.release();
            }
        }
    }

    private static boolean isNonPositive(Duration duration) {
        return duration == null || duration.isNegative() || duration.isZero();
    }
}
