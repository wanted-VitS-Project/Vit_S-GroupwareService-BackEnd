package com.group3.vitamins.vitamate.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.vitamate.application.command.CreateVitamateAnalysisCommand;
import com.group3.vitamins.vitamate.application.query.GetVitamateAnalysisQuery;
import com.group3.vitamins.vitamate.application.result.CreateVitamateAnalysisResult;
import com.group3.vitamins.vitamate.application.result.VitamateAnalysisDetailResult;
import com.group3.vitamins.vitamate.application.usecase.CreateVitamateAnalysisUseCase;
import com.group3.vitamins.vitamate.application.usecase.GetVitamateAnalysisUseCase;
import com.group3.vitamins.vitamate.presentation.api.dto.request.CreateVitamateAnalysisRequest;
import com.group3.vitamins.vitamate.presentation.api.dto.response.CreateVitamateAnalysisResponse;
import com.group3.vitamins.vitamate.presentation.api.dto.response.VitamateAnalysisResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 비타메이트 분석 요청과 조회 API를 제공하는 컨트롤러
@Tag(name = "Vitamate - 비타메이트", description = "비타메이트 AI 분석 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VitamateAnalysisController {

    private final CreateVitamateAnalysisUseCase createUseCase;
    private final GetVitamateAnalysisUseCase getAnalysisUseCase;

    @Operation(summary = "문서 분석 요청", description = "선택한 문서 버전과 프롬프트를 기준으로 AI 분석을 요청한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "분석 요청 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VITAMATE_INVALID_REQUEST — 잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "VITAMATE_BLOCK_NOT_FOUND · VITAMATE_FILE_VERSION_INVALID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "VITAMATE_IDEMPOTENCY_CONFLICT — 같은 키로 다른 요청")
    })
    @PostMapping("/blocks/{blockId}/vitamate/analyses")
    // HTTP 요청값을 command로 변환하고 분석 요청 생성 유스케이스를 호출한다.
    public ResponseEntity<ApiResponse<CreateVitamateAnalysisResponse>> createAnalysis(
            @AuthenticationPrincipal String userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable Long blockId,
            @Valid @RequestBody CreateVitamateAnalysisRequest request
    ) {
        CreateVitamateAnalysisResult result = createUseCase.handle(
                new CreateVitamateAnalysisCommand(
                        blockId,
                        userId,
                        idempotencyKey,
                        request.fileVersionIds(),
                        request.prompt()
                )
        );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of(
                        202,
                        "비타메이트 분석 요청이 생성되었습니다.",
                        CreateVitamateAnalysisResponse.from(result)
                ));
    }

    @Operation(summary = "AI 분석 상태 및 결과 조회", description = "분석 요청 정보, 처리 상태, 생성 결과, 실패 메시지, 선택 문서와 근거를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "분석 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VITAMATE_INVALID_REQUEST — 잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "VITAMATE_ANALYSIS_NOT_FOUND — 분석 이력 없음 또는 접근 불가")
    })
    @GetMapping("/vitamate/analyses/{analysisId}")
    // 분석 ID와 요청자 정보를 query로 변환하고 분석 조회 유스케이스를 호출한다.
    public ResponseEntity<ApiResponse<VitamateAnalysisResponse>> getAnalysis(
            @AuthenticationPrincipal String userId,
            @PathVariable Long analysisId
    ) {
        VitamateAnalysisDetailResult result = getAnalysisUseCase.handle(
                new GetVitamateAnalysisQuery(analysisId, userId)
        );

        return ResponseEntity.ok(ApiResponse.of(
                200,
                "비타메이트 분석 조회 성공",
                VitamateAnalysisResponse.from(result)
        ));
    }
}
