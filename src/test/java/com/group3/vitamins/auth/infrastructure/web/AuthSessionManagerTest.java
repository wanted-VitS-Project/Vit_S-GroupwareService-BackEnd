package com.group3.vitamins.auth.infrastructure.web;

import com.group3.vitamins.global.infrastructure.session.SessionTerminator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.web.context.SecurityContextRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayName("AuthSessionManager 세션 상태 조회 (auth.md §7)")
class AuthSessionManagerTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Instant NOW = Instant.parse("2026-08-18T02:03:27Z"); // 11:03:27 KST

    private HttpServletRequest request;
    private HttpSession session;
    private AuthSessionManager manager;

    @BeforeEach
    void setUp() {
        request = Mockito.mock(HttpServletRequest.class);
        session = Mockito.mock(HttpSession.class);
        manager = new AuthSessionManager(
                Mockito.mock(SecurityContextRepository.class),
                Mockito.mock(SessionTerminator.class),
                Clock.fixed(NOW, ZONE));
        when(request.getSession(false)).thenReturn(session);
        when(session.getMaxInactiveInterval()).thenReturn(14400);
    }

    @Test
    @DisplayName("방금 접근한 세션 — 만료 시각은 lastAccessed + timeout, 남은 초는 timeout 전체")
    void justAccessed() {
        when(session.getLastAccessedTime()).thenReturn(NOW.toEpochMilli());

        SessionStatus status = manager.currentSessionStatus(request);

        assertThat(status.timeoutSeconds()).isEqualTo(14400);
        assertThat(status.expiresAt()).isEqualTo(LocalDateTime.of(2026, 8, 18, 15, 3, 27));
        assertThat(status.remainingSeconds()).isEqualTo(14400);
    }

    @Test
    @DisplayName("갱신 뒤 몇 ms 지나 계산돼도 올림이라 14399 가 아니라 14400 이다")
    void roundsUpSubSecond() {
        when(session.getLastAccessedTime()).thenReturn(NOW.minusMillis(40).toEpochMilli());

        assertThat(manager.currentSessionStatus(request).remainingSeconds()).isEqualTo(14400);
    }

    @Test
    @DisplayName("lastAccessed 가 과거면 남은 초가 그만큼 줄어든다")
    void partiallyElapsed() {
        when(session.getLastAccessedTime()).thenReturn(NOW.minusSeconds(600).toEpochMilli());

        SessionStatus status = manager.currentSessionStatus(request);

        assertThat(status.remainingSeconds()).isEqualTo(14400 - 600);
        assertThat(status.expiresAt()).isEqualTo(LocalDateTime.of(2026, 8, 18, 14, 53, 27));
    }

    @Test
    @DisplayName("이미 지난 세션이어도 남은 초는 음수가 아니라 0 이다")
    void neverNegative() {
        when(session.getLastAccessedTime()).thenReturn(NOW.minusSeconds(20000).toEpochMilli());

        assertThat(manager.currentSessionStatus(request).remainingSeconds()).isZero();
    }

    @Test
    @DisplayName("세션이 없으면 AuthenticationException → 401 AUTH_UNAUTHENTICATED 경로")
    void noSession() {
        when(request.getSession(false)).thenReturn(null);

        assertThatThrownBy(() -> manager.currentSessionStatus(request))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
