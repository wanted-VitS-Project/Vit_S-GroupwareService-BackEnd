package com.group3.vitamins.notification.presentation;

import com.group3.vitamins.notification.application.command.DeleteNotificationCommand;
import com.group3.vitamins.notification.application.command.GetNotificationTargetCommand;
import com.group3.vitamins.notification.application.command.MarkNotificationReadCommand;
import com.group3.vitamins.notification.application.query.ListNotificationsQuery;
import com.group3.vitamins.notification.application.result.MarkNotificationReadResult;
import com.group3.vitamins.notification.application.result.NotificationPageResult;
import com.group3.vitamins.notification.application.result.NotificationTargetResult;
import com.group3.vitamins.notification.application.usecase.NotificationCommandUseCase;
import com.group3.vitamins.notification.application.usecase.NotificationQueryUseCase;
import com.group3.vitamins.notification.presentation.api.response.MarkNotificationReadResponse;
import com.group3.vitamins.notification.presentation.api.response.NotificationListResponse;
import com.group3.vitamins.notification.presentation.api.response.NotificationTargetResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 API — `.ai/api/notification.md`.
 *
 * <p>알림 생성 공개 API는 없다(INV-01) — `#27` 이벤트 인프라가 내부적으로만 생성한다.
 */
@Tag(name = "Notification", description = "알림 API (담당: 이강욱)")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryUseCase notificationQueryUseCase;
    private final NotificationCommandUseCase notificationCommandUseCase;

    @Operation(summary = "알림 목록 조회",
            description = "본인 알림만 최신순으로 조회한다. category 는 notification_type 접두어(예: APPROVAL)를 영문 그대로 받는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 로그인이 필요합니다")
    })
    @GetMapping
    public ApiResponse<NotificationListResponse> listNotifications(
            @Parameter(description = "카테고리 필터(notification_type 접두어). 미지정 시 전체", example = "APPROVAL")
            @RequestParam(required = false) String category,
            @Parameter(description = "안 읽음만 보려면 false")
            @RequestParam(required = false) Boolean isRead,
            @Parameter(description = "페이지 번호(기본 0)")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "페이지 크기(기본 10, 최대 100)")
            @RequestParam(required = false, defaultValue = "10") int size,
            @AuthenticationPrincipal String userId) {

        NotificationPageResult result = notificationQueryUseCase.listNotifications(
                new ListNotificationsQuery(userId, category, isRead, page, size));

        return ApiResponse.success("알림 목록 조회 성공", NotificationListResponse.from(result));
    }

    @Operation(summary = "알림 삭제",
            description = "본인 알림을 논리 삭제한다(하드 삭제 아님). 삭제 후 목록에서 제외된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 로그인이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "NOTIFICATION_FORBIDDEN — 다른 사용자의 알림 삭제 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "NOTIFICATION_NOT_FOUND — 존재하지 않거나 이미 삭제된 알림")
    })
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @Parameter(description = "알림 구분 번호", example = "301")
            @PathVariable Long notificationId,
            @AuthenticationPrincipal String userId) {

        notificationCommandUseCase.deleteNotification(new DeleteNotificationCommand(notificationId, userId));

        // 204 No Content 는 본문을 가질 수 없다(RFC 9110) — ApiResponse 래핑 없이 빈 응답으로 반환한다
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "알림 이동 대상 조회",
            description = "알림 클릭 시 이동 대상을 도메인 무관 구조(type/targetId/extra)로 응답한다. "
                    + "이동 대상이 없으면 type=NONE(에러 아님). 조회 성공 시 자동으로 읽음 처리된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공(이동 대상 없어도 type=NONE 으로 200)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 로그인이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "NOTIFICATION_FORBIDDEN — 다른 사용자의 알림 조회 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "NOTIFICATION_NOT_FOUND — 존재하지 않거나 이미 삭제된 알림")
    })
    @GetMapping("/{notificationId}/target")
    public ApiResponse<NotificationTargetResponse> getTarget(
            @Parameter(description = "이동 대상을 조회할 알림 구분 번호", example = "301")
            @PathVariable Long notificationId,
            @AuthenticationPrincipal String userId) {

        NotificationTargetResult result = notificationCommandUseCase.getTarget(
                new GetNotificationTargetCommand(notificationId, userId));

        return ApiResponse.success("알림 이동 대상 조회 성공", NotificationTargetResponse.from(result));
    }

    @Operation(summary = "알림 읽음 처리",
            description = "이동 없이 읽음만 표시한다. 이미 읽은 알림을 다시 호출해도 최초 읽음 시각을 덮어쓰지 않는다(멱등). "
                    + "알림을 클릭해 이동하는 경우엔 이동 대상 조회가 자동으로 읽음 처리하므로 이 API 를 따로 부를 필요가 없다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "처리 성공(이미 읽은 알림도 200)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 로그인이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "NOTIFICATION_FORBIDDEN — 다른 사용자의 알림 읽음 처리 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "NOTIFICATION_NOT_FOUND — 존재하지 않거나 이미 삭제된 알림")
    })
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<MarkNotificationReadResponse> markRead(
            @Parameter(description = "읽음 처리할 알림 구분 번호", example = "301")
            @PathVariable Long notificationId,
            @AuthenticationPrincipal String userId) {

        MarkNotificationReadResult result = notificationCommandUseCase.markRead(
                new MarkNotificationReadCommand(notificationId, userId));

        return ApiResponse.success("읽음 처리 성공", MarkNotificationReadResponse.from(result));
    }
}
