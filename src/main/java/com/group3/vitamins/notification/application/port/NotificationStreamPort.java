package com.group3.vitamins.notification.application.port;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 실시간 알림 구독을 여는 포트 (§5).
 *
 * <p>⚠️ <b>이 포트는 반환 타입으로 {@link SseEmitter}(Spring Web 타입)를 노출한다 — 의도된 예외다.</b>
 * SSE 는 응답 그 자체가 기술이라 어떤 식으로든 웹 타입이 한 번은 드러난다. 후보는 둘이었다:
 * <ul>
 *   <li>(A) 포트가 {@code SseEmitter} 를 반환한다 → {@code application} 이 웹 타입 하나를 알게 된다.
 *       의존 <b>방향</b>은 규칙대로다({@code presentation → application ← infrastructure})</li>
 *   <li>(B) 컨트롤러가 커넥션 레지스트리(infrastructure 구체 클래스)를 직접 참조한다
 *       → {@code presentation → infrastructure} 로 <b>의존 방향 자체가 역행</b>한다
 *       ({@code .ai/ARCHITECTURE.md} §3 위반)</li>
 * </ul>
 * 방향이 깨지는 게 더 비싸다고 보고 (A)를 택했다. 알림 도메인 로직은 이 타입을 만지지 않는다 —
 * 컨트롤러가 받아서 그대로 응답으로 반환하고, 채우는 건 어댑터뿐이다.
 */
public interface NotificationStreamPort {

    /**
     * RT-001 — 인자로 받은 사번의 알림만 흘려보내는 연결을 연다. 호출자가 세션에서 꺼낸 사번을 넘긴다.
     *
     * <p>RT-003 — 같은 사번으로 여러 번 호출해도 된다(탭마다 한 연결). 기존 연결을 끊지 않는다.
     *
     * <p>RT-007 — {@code sessionId} 를 함께 받는 이유는 <b>그 세션이 죽으면 이 연결도 끊어야</b>
     * 하기 때문이다. 로그아웃은 세션만 무효화하므로, 이걸 들고 있지 않으면 로그아웃한 뒤에도
     * 최대 30분(emitter timeout)간 알림이 계속 흘러간다.
     */
    SseEmitter subscribe(String userId, String sessionId);
}
