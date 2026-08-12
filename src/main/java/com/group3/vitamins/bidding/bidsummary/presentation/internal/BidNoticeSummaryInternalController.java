package com.group3.vitamins.bidding.bidsummary.presentation.internal;

import com.group3.vitamins.bidding.bidsummary.application.query.GetBidNoticeSummaryJobQuery;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryCallbackResult;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryJobResult;
import com.group3.vitamins.bidding.bidsummary.application.usecase.GetBidNoticeSummaryJobUseCase;
import com.group3.vitamins.bidding.bidsummary.application.usecase.HandleBidNoticeSummaryCallbackUseCase;
import com.group3.vitamins.bidding.bidsummary.presentation.internal.dto.request.BidNoticeSummaryCallbackRequest;
import com.group3.vitamins.bidding.bidsummary.presentation.internal.dto.response.BidNoticeSummaryCallbackResponse;
import com.group3.vitamins.bidding.bidsummary.presentation.internal.dto.response.BidNoticeSummaryJobResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Hidden
@Tag(
        name = "Bidding Summary Internal",
        description = "Python 입찰 공고 AI 요약 worker 내부 API"
)
@RestController
@RequestMapping("/internal/v1/bidding/summaries")
@RequiredArgsConstructor
public class BidNoticeSummaryInternalController {

    private final GetBidNoticeSummaryJobUseCase getSummaryJobUseCase;
    private final HandleBidNoticeSummaryCallbackUseCase callbackUseCase;

    @Operation(
            summary = "Python 입찰 요약 작업 조회",
            description = "Python worker가 현재 시도의 프롬프트와 공고 스냅샷을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "작업 입력 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "BIDDING_INVALID_SUMMARY_JOB_REQUEST"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "BIDDING_SUMMARY_JOB_NOT_FOUND"
            )
    })
    @GetMapping("/{summaryId}/jobs/{attemptId}")
    public ResponseEntity<BidNoticeSummaryJobResponse> getJob(
            @Parameter(description = "AI 요약 ID", example = "31")
            @PathVariable Long summaryId,

            @Parameter(description = "현재 작업 시도 ID")
            @PathVariable String attemptId
    ) {
        BidNoticeSummaryJobResult result = getSummaryJobUseCase.handle(
                new GetBidNoticeSummaryJobQuery(summaryId, attemptId)
        );

        return ResponseEntity.ok(
                BidNoticeSummaryJobResponse.from(result)
        );
    }

    @Operation(
            summary = "Python 입찰 요약 결과 callback",
            description = "Python worker가 Gemini 처리 결과를 Spring Boot에 저장합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "callback 처리 성공 또는 멱등 거절"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "BIDDING_INVALID_SUMMARY_CALLBACK"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "BIDDING_SUMMARY_NOT_FOUND"
            )
    })
    @PostMapping("/{summaryId}/callback")
    public ResponseEntity<BidNoticeSummaryCallbackResponse> callback(
            @Parameter(description = "AI 요약 ID", example = "31")
            @PathVariable Long summaryId,

            @Valid @RequestBody BidNoticeSummaryCallbackRequest request
    ) {
        BidNoticeSummaryCallbackResult result = callbackUseCase.handle(
                request.toCommand(summaryId)
        );

        return ResponseEntity.ok(
                BidNoticeSummaryCallbackResponse.from(result)
        );
    }
}