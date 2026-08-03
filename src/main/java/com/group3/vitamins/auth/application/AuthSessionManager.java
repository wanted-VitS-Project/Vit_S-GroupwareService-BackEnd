package com.group3.vitamins.auth.application;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 세션 수립 · 종료.
 *
 * <p>폼 로그인을 쓰지 않으므로 Spring Security 의 인증 필터가 해주던 일(세션 고정 방어 · 단일 세션 ·
 * SecurityContext 저장)을 <b>여기서 명시적으로</b> 한다. 필터 기반 전략은 우리 로그인 경로에서 동작하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthSessionManager {

    /** 세션 속성 키 — 초기 비밀번호를 아직 안 바꿨는가 */
    public static final String PASSWORD_RESET_REQUIRED = "PASSWORD_RESET_REQUIRED";

    private final SecurityContextRepository securityContextRepository;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

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
    public void openSession(String userId, String role, boolean passwordResetRequired,
                            HttpServletRequest request, HttpServletResponse response) {
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        closeAllSessions(userId);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // 세션이 여기서 생성된다 (HttpSessionSecurityContextRepository)
        securityContextRepository.saveContext(context, request, response);

        // 초기 비밀번호 상태를 세션에 실어둔다 — 게이트 필터가 매 요청 DB 를 치지 않게 하려는 것이다
        request.getSession().setAttribute(PASSWORD_RESET_REQUIRED, passwordResetRequired);
    }

    /** 비밀번호 변경 성공 → 게이트 해제. 세션은 유지한다 (재로그인시키지 않는다) */
    public void clearPasswordResetFlag(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(PASSWORD_RESET_REQUIRED, false);
        }
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
     * <p>단일 세션 정책 외에 <b>계정 잠금 · 비활성화 · role 변경 시 즉시 반영</b>에도 쓴다.
     * JWT 였다면 만료를 기다려야 했을 자리다.
     */
    public void closeAllSessions(String userId) {
        try {
            Map<String, ? extends Session> sessions =
                    sessionRepository.findByPrincipalName(userId);
            sessions.keySet().forEach(sessionRepository::deleteById);
        } catch (RuntimeException e) {
            // 여기서 실패해도 로그인은 진행시킨다. 이전 세션이 남는 것은 4시간 뒤 만료로 정리된다.
            log.warn("기존 세션 정리 실패 — userId={}", userId, e);
        }
    }
}
