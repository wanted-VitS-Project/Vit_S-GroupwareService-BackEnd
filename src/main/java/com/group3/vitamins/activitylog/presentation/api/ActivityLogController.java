package com.group3.vitamins.activitylog.presentation.api;

import com.group3.vitamins.activitylog.application.query.ActivityLogListQuery;
import com.group3.vitamins.activitylog.application.result.ActivityLogPageResult;
import com.group3.vitamins.activitylog.application.usecase.ActivityLogQueryUseCase;
import com.group3.vitamins.activitylog.presentation.api.response.ActivityLogListResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ActivityLog - 활동 기록", description = "스텝별 활동 기록 조회")
@RestController
@RequestMapping("/api/v1/steps/{stepId}/activity-logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogQueryUseCase activityLogQueryUseCase;

    @Operation(
            summary = "스텝별 활동 기록 조회",
            description = "현재 Step에 속한 Block 및 Block 내부 데이터의 활동 기록을 최신순으로 조회한다. "
                    + "blockId를 전달하면 특정 Block에서 발생한 활동 기록만 조회한다. 별도의 Block 활동 기록 API는 만들지 않는다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "활동 기록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "ACTIVITY_LOG_CURSOR_INVALID / ACTIVITY_LOG_SIZE_INVALID / ACTIVITY_LOG_BLOCK_STEP_MISMATCH"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_TOKEN_EXPIRED — 인증 토큰 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_ACCESS_DENIED — 프로젝트 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "STEP_NOT_FOUND / BLOCK_NOT_FOUND")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ActivityLogListResponse>> getActivityLogs(
            @Parameter(description = "조회할 Step ID")
            @PathVariable Long stepId,

            @Parameter(description = "해당 Block의 활동 기록만 조회")
            @RequestParam(required = false) Long blockId,

            @Parameter(description = "이전 응답의 nextCursor")
            @RequestParam(required = false) Long cursor,

            @Parameter(description = "조회 개수, 기본값 20")
            @RequestParam(required = false) Integer size,

            Authentication authentication
    ) {
        ActivityLogPageResult result = activityLogQueryUseCase.getActivityLogs(
                new ActivityLogListQuery(
                        stepId,
                        blockId,
                        cursor,
                        size,
                        authentication.getName(),
                        RequesterRole.from(authentication)
                )
        );

        return ResponseEntity.ok(ApiResponse.success(
                ActivityLogResponseMessage.SUCCESS,
                ActivityLogListResponse.from(result)
        ));
    }
}
