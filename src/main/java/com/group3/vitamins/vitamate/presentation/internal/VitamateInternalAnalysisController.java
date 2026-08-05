package com.group3.vitamins.vitamate.presentation.internal;

import com.group3.vitamins.vitamate.application.query.GetVitamateAnalysisJobQuery;
import com.group3.vitamins.vitamate.application.result.VitamateAnalysisCallbackResult;
import com.group3.vitamins.vitamate.application.result.VitamateAnalysisJobDetailResult;
import com.group3.vitamins.vitamate.application.usecase.GetVitamateAnalysisJobUseCase;
import com.group3.vitamins.vitamate.application.usecase.HandleVitamateAnalysisCallbackUseCase;
import com.group3.vitamins.vitamate.presentation.internal.dto.request.VitamateAnalysisCallbackRequest;
import com.group3.vitamins.vitamate.presentation.internal.dto.response.VitamateAnalysisCallbackResponse;
import com.group3.vitamins.vitamate.presentation.internal.dto.response.VitamateAnalysisJobResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Python worker가 호출하는 비타메이트 내부 API 컨트롤러
@Hidden
@Tag(name = "Vitamate Internal", description = "비타메이트 Python worker 내부 API")
@RestController
@RequestMapping("/internal/v1/vitamate/analyses")
@RequiredArgsConstructor
public class VitamateInternalAnalysisController {

    private final GetVitamateAnalysisJobUseCase getAnalysisJobUseCase;
    private final HandleVitamateAnalysisCallbackUseCase callbackUseCase;

    // 분석 ID와 attemptId로 현재 worker가 처리할 작업 입력을 조회한다.
    @Operation(summary = "Python 분석 작업 조회", description = "Python worker가 큐 메시지를 소비한 뒤 분석 입력을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "작업 입력 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VITAMATE_INVALID_REQUEST — 잘못된 내부 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "VITAMATE_WORKER_UNAUTHORIZED — worker token 누락 또는 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMON_FORBIDDEN — worker 전용 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "VITAMATE_ANALYSIS_NOT_FOUND — 처리 가능한 분석 작업 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류")
    })
    @GetMapping("/{analysisId}/jobs/{attemptId}")
    public ResponseEntity<VitamateAnalysisJobResponse> getAnalysisJob(
            @Parameter(description = "분석 ID", example = "501")
            @PathVariable Long analysisId,
            @Parameter(description = "큐 메시지에 포함된 워커 실행 토큰", example = "9f6c3e6b-8974-4f8d-8c88-2e1d3e0d3138")
            @PathVariable String attemptId
    ) {
        VitamateAnalysisJobDetailResult result = getAnalysisJobUseCase.handle(
                new GetVitamateAnalysisJobQuery(analysisId, attemptId)
        );

        return ResponseEntity.ok(VitamateAnalysisJobResponse.from(result));
    }

    // Python worker가 처리한 분석 결과를 받아 상태와 citation을 저장한다.
    @Operation(summary = "Python 분석 결과 콜백", description = "Python worker가 처리한 분석 결과를 Spring Boot에 전달한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "callback 수신 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VITAMATE_INVALID_REQUEST — 잘못된 callback 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "VITAMATE_WORKER_UNAUTHORIZED — worker token 누락 또는 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMON_FORBIDDEN — worker 전용 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류")
    })
    @PostMapping("/{analysisId}/callback")
    public ResponseEntity<VitamateAnalysisCallbackResponse> handleCallback(
            @Parameter(description = "분석 ID", example = "501")
            @PathVariable Long analysisId,
            @Valid @RequestBody VitamateAnalysisCallbackRequest request
    ) {
        VitamateAnalysisCallbackResult result = callbackUseCase.handle(request.toCommand(analysisId));
        return ResponseEntity.ok(VitamateAnalysisCallbackResponse.from(result));
    }
}
