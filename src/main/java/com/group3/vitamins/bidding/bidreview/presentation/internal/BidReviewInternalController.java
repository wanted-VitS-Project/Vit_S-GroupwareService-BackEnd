package com.group3.vitamins.bidding.bidreview.presentation.internal;

import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewJobQuery;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewCallbackResult;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewJobResult;
import com.group3.vitamins.bidding.bidreview.application.usecase.GetBidReviewJobUseCase;
import com.group3.vitamins.bidding.bidreview.application.usecase.HandleBidReviewCallbackUseCase;
import com.group3.vitamins.bidding.bidreview.presentation.internal.dto.request.BidReviewCallbackRequest;
import com.group3.vitamins.bidding.bidreview.presentation.internal.dto.response.BidReviewCallbackResponse;
import com.group3.vitamins.bidding.bidreview.presentation.internal.dto.response.BidReviewJobResponse;
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
        name = "Bidding Review Internal",
        description = "Python 입찰 문서 검토 worker 내부 API"
)
@RestController
@RequestMapping("/internal/v1/bidding/reviews")
@RequiredArgsConstructor
public class BidReviewInternalController {

    private final GetBidReviewJobUseCase getReviewJobUseCase;
    private final HandleBidReviewCallbackUseCase callbackUseCase;

    @Operation(
            summary = "Python 입찰 문서 검토 작업 조회",
            description = "Python worker가 현재 시도의 프롬프트, 공고 기본 정보, 선택 문서의 다운로드 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "작업 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "BIDDING_INVALID_REVIEW_REQUEST"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "BIDDING_REVIEW_JOB_NOT_FOUND"
            )
    })
    @GetMapping("/{reviewId}/jobs/{attemptId}")
    public ResponseEntity<BidReviewJobResponse> getJob(
            @Parameter(description = "입찰 문서 검토 ID", example = "71")
            @PathVariable Long reviewId,

            @Parameter(description = "현재 작업 시도 ID")
            @PathVariable String attemptId
    ) {
        BidReviewJobResult result = getReviewJobUseCase.handle(
                new GetBidReviewJobQuery(reviewId, attemptId)
        );

        return ResponseEntity.ok(
                BidReviewJobResponse.from(result)
        );
    }

    @Operation(
            summary = "Python 입찰 문서 검토 결과 callback",
            description = "Python worker가 다운로드·추출·비교 진행상황과 최종 결과를 Spring Boot에 저장합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "callback 접수. 멱등 거절도 accepted=false로 200 반환"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "BIDDING_INVALID_REVIEW_CALLBACK"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "BIDDING_REVIEW_NOT_FOUND"
            )
    })
    @PostMapping("/{reviewId}/callback")
    public ResponseEntity<BidReviewCallbackResponse> callback(
            @Parameter(description = "입찰 문서 검토 ID", example = "71")
            @PathVariable Long reviewId,

            @Valid @RequestBody BidReviewCallbackRequest request
    ) {
        BidReviewCallbackResult result = callbackUseCase.handle(
                request.toCommand(reviewId)
        );

        return ResponseEntity.ok(
                BidReviewCallbackResponse.from(result)
        );
    }
}
