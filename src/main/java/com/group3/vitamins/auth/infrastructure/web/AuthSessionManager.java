package com.group3.vitamins.auth.infrastructure.web;

import com.group3.vitamins.global.infrastructure.session.SessionTerminator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 세션 수립 · 종료.
 *
 * <p>폼 로그인을 쓰지 않으므로 Spring Security 의 인증 필터가 해주던 일(세션 고정 방어 · 단일 세션 ·
 * SecurityContext 저장)을 <b>여기서 명시적으로</b> 한다. 필터 기반 전략은 우리 로그인 경로에서 동작하지 않는다.
 *
 * <p>{@code HttpServletRequest/Response}·{@code SecurityContextHolder}·{@code HttpSession} 같은 Servlet/Security
 * 인프라를 직접 다루므로 application 이 아니라 {@code infrastructure/web} 에 둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthSessionManager {

    /** 세션 속성 키 — 초기 비밀번호를 아직 안 바꿨는가 */
    public static final String PASSWORD_RESET_REQUIRED = "PASSWORD_RESET_REQUIRED";

    /** 세션 속성 키 — 약관에 아직 동의하지 않았는가 (최초 로그인 전용, `auth.md` §6-7) */
    public static final String TERMS_AGREEMENT_REQUIRED = "TERMS_AGREEMENT_REQUIRED";

    private final SecurityContextRepository securityContextRepository;
    private final SessionTerminator sessionTerminator;
    private final Clock clock;

    /**
     * 로그인 성공 → 세션 수립.
     *
     * <p>순서가 중요하다.
     * <ol>
     *   <li><b>기존 세션 무효화</b> — 세션 고정(fixation) 공격 방어. 공격자가 심어둔 세션 ID 를 그대로 쓰면 안 된다</li>
     *   <li><b>다른 기기 세션 종료</b> — 단일 세션 정책</li>
     *   <li>새 세션에 SecurityContext 저장 — 이때 Spring Session 이 사용자명 인덱스도 함께 기록한다</li>
     * </ol>
     */
    public void openSession(String userId, String role, Long companyId,
                            boolean termsAgreementRequired, boolean passwordResetRequired,
                            HttpServletRequest request, HttpServletResponse response) {
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        closeAllSessions(userId);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        // 회사(테넌트) 컨텍스트를 details 에 실어둔다 — TenantContext.currentCompanyId() 가 이걸 읽는다.
        // principal 은 사번(String) 그대로라 @AuthenticationPrincipal String userId 는 안 바뀐다.
        authentication.setDetails(companyId);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // 세션이 여기서 생성된다 (HttpSessionSecurityContextRepository)
        securityContextRepository.saveContext(context, request, response);

        // 최초 로그인 상태(약관·초기 비밀번호)를 세션에 실어둔다 — 게이트 필터가 매 요청 DB 를 치지 않게 한다.
        // 순서: 약관 게이트가 비번 게이트보다 앞이다 (auth.md §6-7).
        HttpSession session = request.getSession();
        session.setAttribute(TERMS_AGREEMENT_REQUIRED, termsAgreementRequired);
        session.setAttribute(PASSWORD_RESET_REQUIRED, passwordResetRequired);
    }

    /** 약관 동의 성공 → 약관 게이트 해제. 세션은 유지한다 */
    public void clearTermsAgreementFlag(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(TERMS_AGREEMENT_REQUIRED, false);
        }
    }

    /** 비밀번호 변경 성공 → 게이트 해제. 세션은 유지한다 (재로그인시키지 않는다) */
    public void clearPasswordResetFlag(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(PASSWORD_RESET_REQUIRED, false);
        }
    }

    /**
     * 현재 세션의 만료 정보 (`auth.md` §7).
     *
     * <p>Spring Session 은 요청에서 세션을 처음 만지는 순간 {@code lastAccessedTime} 을 지금으로 갱신한다.
     * 따라서 여기서 읽는 값은 "이 요청 시각"이고, 남은 초는 사실상 타임아웃 전체다 — 조회가 곧 연장이다.
     * 세션이 없으면 미인증이므로 401 로 떨어뜨린다 (인증 필터를 지나왔다면 정상 경로에선 일어나지 않는다).
     */
    public SessionStatus currentSessionStatus(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new AuthenticationCredentialsNotFoundException("세션이 없습니다.");
        }
        int timeoutSeconds = session.getMaxInactiveInterval();
        Instant lastAccessed = Instant.ofEpochMilli(session.getLastAccessedTime());
        Instant expiresAt = lastAccessed.plusSeconds(timeoutSeconds);
        // 올림 — 갱신 직후 몇 ms 지나 계산돼도 14399 가 아니라 14400 으로 나가게 (프론트가 정책값과 비교한다)
        long remainingMillis = Duration.between(clock.instant(), expiresAt).toMillis();
        long remaining = Math.max(0, (remainingMillis + 999) / 1000);
        return new SessionStatus(
                timeoutSeconds,
                LocalDateTime.ofInstant(expiresAt, clock.getZone()),
                remaining);
    }

    /** 로그아웃 — 현재 세션만 끊는다 */
    public void closeCurrentSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    /**
     * 해당 사용자의 모든 세션 종료.
     *
     * <p>단일 세션 정책(로그인 시 기존 세션 정리)에 쓴다. 계정 관리 쪽의 즉시 무효화와 같은 연산이라
     * 공용 {@link SessionTerminator} 에 위임한다.
     */
    public void closeAllSessions(String userId) {
        sessionTerminator.terminateAll(userId);
    }
}
