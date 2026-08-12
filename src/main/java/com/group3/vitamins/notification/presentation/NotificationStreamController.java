package com.group3.vitamins.notification.presentation;

import com.group3.vitamins.notification.application.port.NotificationStreamPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 실시간 알림 수신 API — `.ai/api/notification.md` §5.
 *
 * <p>{@link NotificationController} 와 분리한 이유: 응답이 {@code ApiResponse} 래핑이 아니라
 * <b>열린 스트림</b>이다. 같은 클래스에 두면 다음 사람이 다른 메서드에 맞춰 래핑을 씌우기 쉽고,
 * 그러면 스트림이 단발 JSON 이 되어 조용히 망가진다.
 *
 * <p>인증은 기존 세션 쿠키를 그대로 쓴다. Security 설정은 손대지 않았다 —
 * {@code anyRequest().authenticated()} 가 이 경로도 덮고, 비동기 재진입은
 * {@code dispatcherTypeMatchers(ASYNC, ERROR).permitAll()} 이 이미 허용하고 있다.
 */
@Tag(name = "Notification", description = "알림 API (담당: 이강욱)")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationStreamController {

    private final NotificationStreamPort notificationStreamPort;

    /**
     * RT-001 — 구독 대상은 <b>세션의 사번으로 고정</b>한다. 파라미터로 받지 않으므로 남의 알림을
     * 구독할 방법이 없다.
     *
     * <p>⚠️ {@code X-Accel-Buffering: no} 를 빼면 <b>로컬은 실시간인데 배포하면 새로고침이 필요한</b>
     * 증상이 난다 — nginx 가 기본으로 응답을 버퍼링해 이벤트를 모아뒀다가 흘린다. 코드로는 전송이
     * 성공하므로 로그·예외로는 드러나지 않는다.
     */
    @Operation(summary = "실시간 알림 수신(SSE)",
            description = "구독하면 알림이 생기는 즉시 서버가 밀어준다. 이벤트 2종(connected · notification)과 "
                    + "15초 주기 주석 하트비트가 흐른다. connected 를 받을 때마다 프론트는 목록을 재조회한다"
                    + "(재연결 중 발행된 알림은 스트림으로 오지 않는다 — RT-005). "
                    + "실시간은 보조 경로이며 읽음·개수의 정본은 목록 조회 API 다(RT-004).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "구독 성공(스트림 시작)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 로그인이 필요합니다")
    })
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal String userId, HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");

        return notificationStreamPort.subscribe(userId);
    }
}
