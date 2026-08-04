package com.group3.vitamins.global.infrastructure.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 특정 사용자의 서버측 세션을 즉시 종료한다.
 *
 * <p>서버측 세션을 택한 핵심 이유가 <b>role 변경 · 계정 비활성화 · 잠금의 즉시 반영</b>이다
 * (JWT 였다면 만료를 기다려야 했다). 그 "즉시 무효화" 를 한 곳에 모은 컴포넌트다.
 *
 * <p>단일 세션 정책(로그인 시 기존 세션 정리)과 계정 관리(권한·상태 변경)가 모두 이 연산을 쓰므로,
 * 특정 도메인 패키지에 두지 않고 중립 위치에 둔다 — {@code auth} 와 {@code account} 가 서로를
 * 참조하는 순환을 막는다.
 *
 * <p>🚨 사용자별 조회는 {@code spring.session.redis.repository-type=indexed} 를 전제한다.
 * {@code default} 로 두면 {@link FindByIndexNameSessionRepository#findByPrincipalName} 가 빈 맵을 준다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionTerminator {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    /**
     * 해당 사용자의 모든 세션을 삭제한다.
     *
     * <p>실패해도 예외를 삼킨다 — 호출부(로그인·권한 변경)의 본래 작업을 막지 않는다.
     * 남은 세션은 유휴 타임아웃으로 정리된다. 다만 <b>비활성화·권한 강등의 즉시성은 그만큼 늦어진다.</b>
     */
    public void terminateAll(String userId) {
        try {
            Map<String, ? extends Session> sessions = sessionRepository.findByPrincipalName(userId);
            sessions.keySet().forEach(sessionRepository::deleteById);
        } catch (RuntimeException e) {
            log.warn("세션 정리 실패 — userId={}", userId, e);
        }
    }
}
