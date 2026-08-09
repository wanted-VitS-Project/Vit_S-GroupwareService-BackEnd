package com.group3.vitamins.vitamate.filecleanup.presentation.internal;

import com.group3.vitamins.vitamate.filecleanup.application.command.HandleVitamateCleanupCallbackCommand;
import com.group3.vitamins.vitamate.filecleanup.application.result.VitamateCleanupCallbackResult;
import com.group3.vitamins.vitamate.filecleanup.application.usecase.HandleVitamateCleanupCallbackUseCase;
import com.group3.vitamins.vitamate.filecleanup.presentation.internal.dto.request.VitamateCleanupCallbackRequest;
import com.group3.vitamins.vitamate.filecleanup.presentation.internal.dto.response.VitamateCleanupCallbackResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Python worker가 호출하는 ChromaDB 정리 내부 API 컨트롤러입니다.
@Hidden
@Tag(
        name = "Vitamate Internal",
        description = "비타메이트 Python worker 내부 API"
)
@RestController
@RequestMapping("/internal/v1/vitamate/chroma-cleanup-jobs")
@RequiredArgsConstructor
public class VitamateInternalCleanupController {

    private final HandleVitamateCleanupCallbackUseCase callbackUseCase;

    // Python worker가 ChromaDB 정리 처리 상태와 결과를 전달합니다.
    @Operation(
            summary = "ChromaDB 정리 결과 callback",
            description = "Python worker가 파일 영구삭제에 따른 ChromaDB vector 정리 결과를 전달한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "callback 처리 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "VITAMATE_INVALID_REQUEST - callback 입력 형식 위반"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "VITAMATE_WORKER_UNAUTHORIZED - worker token 누락 또는 불일치"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "COMMON_FORBIDDEN - worker 전용 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "VITAMATE_CLEANUP_JOB_NOT_FOUND - 정리 작업 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "COMMON_INTERNAL_ERROR - 서버 내부 오류"
            )
    })
    @PostMapping("/{cleanupJobId}/callback")
    public ResponseEntity<VitamateCleanupCallbackResponse> handleCallback(
            @Parameter(description = "ChromaDB 정리 작업 ID", example = "31")
            @PathVariable Long cleanupJobId,
            @RequestBody(required = false) VitamateCleanupCallbackRequest request
    ) {
        HandleVitamateCleanupCallbackCommand command = request == null
                ? new HandleVitamateCleanupCallbackCommand(
                cleanupJobId,
                null,
                null,
                null,
                null,
                null,
                null
        )
                : request.toCommand(cleanupJobId);

        VitamateCleanupCallbackResult result = callbackUseCase.handle(command);
        return ResponseEntity.ok(VitamateCleanupCallbackResponse.from(result));
    }
}