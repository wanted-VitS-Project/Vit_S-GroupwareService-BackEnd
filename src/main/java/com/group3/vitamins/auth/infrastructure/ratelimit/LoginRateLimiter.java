package com.group3.vitamins.auth.infrastructure.ratelimit;

import com.group3.vitamins.auth.domain.exception.AuthErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.TooManyRequestsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * IP 단위 로그인 요청 제한.
 *
 * <p><b>왜 필요한가</b> — 로그인은 인증 없이 호출되고 요청 1건이 Argon2 해시 1회(64MB · 0.4초)를 태운다.
 * 제한이 없으면 200바이트 요청으로 서버 자원을 갈아 넣을 수 있다.
 *
 * <p>⚠️ <b>반드시 해시 이전에 호출해야 한다.</b> 해시 뒤에 두면 이미 자원을 다 쓴 뒤라 의미가 없다.
 *
 * <p>한도를 인원(30명)의 2배로 잡은 이유는 <b>사무실이 NAT 하나를 공유</b>하기 때문이다.
 * 아침에 25명이 동시에 로그인하면 한 IP 에서 30회가 넘게 나온다.
 */
@Slf4j
@Component
public class LoginRateLimiter {

    private static final String KEY_PREFIX = "auth:login:rate:";
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;
    private final int limitPerMinute;

    public LoginRateLimiter(StringRedisTemplate redisTemplate,
                            @Value("${security.login.ip-rate-limit-per-minute}") int limitPerMinute) {
        this.redisTemplate = redisTemplate;
        this.limitPerMinute = limitPerMinute;
    }

    public void check(String clientIp) {
        String key = KEY_PREFIX + clientIp;

        Long count;
        try {
            count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, WINDOW);
            }
        } catch (RuntimeException e) {
            // fail-open — 레이트리밋은 보조 방어다. 최후 방어선인 해시 세마포어는 여전히 살아 있다.
            // (게다가 세션 저장소도 Redis 라 Redis 가 죽으면 어차피 로그인이 안 된다)
            log.warn("로그인 레이트리밋 확인 실패 — Redis 오류로 통과시킨다. ip={}", clientIp, e);
            return;
        }

        if (count != null && count > limitPerMinute) {
            log.warn("로그인 레이트리밋 초과 — ip={} count={} limit={}", clientIp, count, limitPerMinute);
            throw new TooManyRequestsException(AuthErrorCode.AUTH_TOO_MANY_REQUESTS);
        }
    }
}
