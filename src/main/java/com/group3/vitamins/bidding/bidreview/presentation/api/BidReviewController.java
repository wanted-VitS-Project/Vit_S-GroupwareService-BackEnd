package com.group3.vitamins.bidding.bidreview.presentation.api;

import com.group3.vitamins.bidding.bidreview.application.command.AbandonBidReviewCommand;
import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewDetailQuery;
import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewHistoryQuery;
import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewSourcesQuery;
import com.group3.vitamins.bidding.bidreview.application.result.AbandonBidReviewResult;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewDetailResult;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewHistoryResult;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewSourcesResult;
import com.group3.vitamins.bidding.bidreview.application.result.CreateBidReviewResult;
import com.group3.vitamins.bidding.bidreview.application.usecase.AbandonBidReviewUseCase;
import com.group3.vitamins.bidding.bidreview.application.usecase.CreateBidReviewUseCase;
import com.group3.vitamins.bidding.bidreview.application.usecase.GetBidReviewSourcesUseCase;
import com.group3.vitamins.bidding.bidreview.application.usecase.GetBidReviewDetailUseCase;
import com.group3.vitamins.bidding.bidreview.application.usecase.GetBidReviewHistoryUseCase;
import com.group3.vitamins.bidding.bidreview.presentation.api.request.CreateBidReviewRequest;
import com.group3.vitamins.bidding.bidreview.presentation.api.response.AbandonBidReviewResponse;
import com.group3.vitamins.bidding.bidreview.presentation.api.response.BidReviewDetailResponse;
import com.group3.vitamins.bidding.bidreview.presentation.api.response.BidReviewHistoryResponse;
import com.group3.vitamins.bidding.bidreview.presentation.api.response.BidReviewSourcesResponse;
import com.group3.vitamins.bidding.bidreview.presentation.api.response.CreateBidReviewResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Bidding - 입찰 문서 검토",
        description = "입찰 공고 첨부와 사내 기준자료를 비교하는 AI 문서 검토를 관리합니다."
)
@RestController
@RequestMapping("/api/v1/bidding")
@RequiredArgsConstructor
public class BidReviewController {

    private final CreateBidReviewUseCase createBidReviewUseCase;
    private final GetBidReviewSourcesUseCase getBidReviewSourcesUseCase;
    private final GetBidReviewDetailUseCase getBidReviewDetailUseCase;
    private final GetBidReviewHistoryUseCase getBidReviewHistoryUseCase;
    private final AbandonBidReviewUseCase abandonBidReviewUseCase;

    @Operation(
            summary = "입찰 문서 검토 요청",
            description = "현재 회사가 조회할 수 있는 공고 첨부와 사내 기준자료를 선택해 AI 비교 검토를 비동기로 요청합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202", description = "요청 접수 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "BIDDING_INVALID_REVIEW_REQUEST"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "AUTH_UNAUTHENTICATED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "BIDDING_ACCESS_PERMISSION_REQUIRED 또는 BIDDING_REVIEW_DOCUMENT_ACCESS_DENIED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "BIDDING_NOTICE_NOT_FOUND 또는 BIDDING_NOTICE_ATTACHMENT_NOT_FOUND"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "BIDDING_REVIEW_ALREADY_PROCESSING 또는 BIDDING_REVIEW_DOCUMENT_NOT_READY"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422", description = "BIDDING_REVIEW_UNSUPPORTED_FILE"
            )
    })
    @PostMapping("/notices/{noticeId}/reviews")
    public ResponseEntity<ApiResponse<CreateBidReviewResponse>> create(
            @Parameter(description = "검토할 입찰 공고 ID")
            @PathVariable Long noticeId,
            @Valid @RequestBody CreateBidReviewRequest request,
            Authentication authentication
    ) {
        CreateBidReviewResult result = createBidReviewUseCase.create(
                request.toCommand(
                        noticeId,
                        authentication.getName(),
                        RequesterRole.from(authentication)
                )
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of(
                        HttpStatus.ACCEPTED.value(),
                        "입찰 문서 검토 요청이 접수되었습니다.",
                        CreateBidReviewResponse.from(result)
                ));
    }

    @Operation(
            summary = "입찰 문서 검토 자료 조회",
            description = "검토 화면에 표시할 공고 첨부파일 메타데이터를 조회합니다. 사내 기준자료 목록은 별도 API로 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "AUTH_UNAUTHENTICATED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "BIDDING_NOTICE_NOT_FOUND"
            )
    })
    @GetMapping("/notices/{noticeId}/review-sources")
    public ResponseEntity<ApiResponse<BidReviewSourcesResponse>> getSources(
            @Parameter(description = "조회할 입찰 공고 ID")
            @PathVariable Long noticeId,
            Authentication authentication
    ) {
        BidReviewSourcesResult result = getBidReviewSourcesUseCase.get(
                new GetBidReviewSourcesQuery(
                        noticeId,
                        authentication.getName(),
                        RequesterRole.from(authentication)
                )
        );

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK.value(),
                "입찰 문서 검토 자료 조회 성공",
                BidReviewSourcesResponse.from(result)
        ));
    }

    @Operation(
            summary = "입찰 문서 검토 조회",
            description = "검토 1건의 상태, 결과, 선택 문서와 근거를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "AUTH_UNAUTHENTICATED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "BIDDING_ACCESS_PERMISSION_REQUIRED 또는 BIDDING_REVIEW_ACCESS_DENIED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "BIDDING_REVIEW_NOT_FOUND"
            )
    })

    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<BidReviewDetailResponse>> getDetail(
            @Parameter(description = "조회할 검토 ID")
            @PathVariable Long reviewId,
            Authentication authentication
    ) {
        BidReviewDetailResult result = getBidReviewDetailUseCase.get(
                new GetBidReviewDetailQuery(
                        reviewId,
                        authentication.getName(),
                        RequesterRole.from(authentication)
                )
        );

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK.value(),
                "입찰 문서 검토 조회 성공",
                BidReviewDetailResponse.from(result)
        ));
    }

    @Operation(
            summary = "공고별 입찰 문서 검토 이력 조회",
            description = "현재 회사에서 본인이 요청한 검토 이력을 최신순으로 최대 20건 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공. 이력이 없으면 빈 배열"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "AUTH_UNAUTHENTICATED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "BIDDING_NOTICE_NOT_FOUND"
            )
    })
    @GetMapping("/notices/{noticeId}/reviews")
    public ResponseEntity<ApiResponse<BidReviewHistoryResponse>> getHistory(
            @Parameter(description = "조회할 입찰 공고 ID")
            @PathVariable Long noticeId,
            Authentication authentication
    ) {
        BidReviewHistoryResult result = getBidReviewHistoryUseCase.get(
                new GetBidReviewHistoryQuery(
                        noticeId,
                        authentication.getName(),
                        RequesterRole.from(authentication)
                )
        );

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK.value(),
                "입찰 문서 검토 이력 조회 성공",
                BidReviewHistoryResponse.from(result)
        ));
    }

    @Operation(
            summary = "입찰 문서 검토 종료",
            description = "프로젝트로 전환하지 않은 검토를 종료하고 임시파일 정리를 즉시 요청합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "종료 접수 및 정리 요청 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "AUTH_UNAUTHENTICATED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "BIDDING_ACCESS_PERMISSION_REQUIRED 또는 BIDDING_REVIEW_ACCESS_DENIED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "BIDDING_REVIEW_NOT_FOUND"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "BIDDING_REVIEW_NOT_ABANDONABLE"
            )
    })
    @PatchMapping("/reviews/{reviewId}/abandon")
    public ResponseEntity<ApiResponse<AbandonBidReviewResponse>> abandon(
            @Parameter(description = "종료할 검토 ID")
            @PathVariable Long reviewId,
            Authentication authentication
    ) {
        AbandonBidReviewResult result = abandonBidReviewUseCase.abandon(
                new AbandonBidReviewCommand(
                        reviewId,
                        authentication.getName(),
                        RequesterRole.from(authentication)
                )
        );

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK.value(),
                "입찰 문서 검토가 종료됐습니다.",
                AbandonBidReviewResponse.from(result)
        ));
    }
}