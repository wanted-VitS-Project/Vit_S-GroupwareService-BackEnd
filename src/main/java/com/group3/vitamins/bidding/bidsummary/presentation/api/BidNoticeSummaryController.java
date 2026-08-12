package com.group3.vitamins.bidding.bidsummary.presentation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.bidding.bidsummary.application.command.ConfirmBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.query.GetBidNoticeSummaryQuery;
import com.group3.vitamins.bidding.bidsummary.application.query.GetBidNoticeSummaryHistoryQuery;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryResult;
import com.group3.vitamins.bidding.bidsummary.application.result.CreateBidNoticeSummaryResult;
import com.group3.vitamins.bidding.bidsummary.application.result.ConfirmBidNoticeSummaryResult;
import com.group3.vitamins.bidding.bidsummary.application.usecase.ConfirmBidNoticeSummaryUseCase;
import com.group3.vitamins.bidding.bidsummary.application.usecase.CreateBidNoticeSummaryUseCase;
import com.group3.vitamins.bidding.bidsummary.application.usecase.GetBidNoticeSummaryUseCase;
import com.group3.vitamins.bidding.bidsummary.application.usecase.GetBidNoticeSummaryHistoryUseCase;
import com.group3.vitamins.bidding.bidsummary.application.usecase.UpdateBidNoticeSummaryUseCase;
import com.group3.vitamins.bidding.bidsummary.presentation.api.request.CreateBidNoticeSummaryRequest;
import com.group3.vitamins.bidding.bidsummary.presentation.api.request.UpdateBidNoticeSummaryRequest;
import com.group3.vitamins.bidding.bidsummary.presentation.api.response.BidNoticeSummaryResponse;
import com.group3.vitamins.bidding.bidsummary.presentation.api.response.BidNoticeSummaryHistoryResponse;
import com.group3.vitamins.bidding.bidsummary.presentation.api.response.ConfirmBidNoticeSummaryResponse;
import com.group3.vitamins.bidding.bidsummary.presentation.api.response.CreateBidNoticeSummaryResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Bidding - 입찰 AI 요약",
        description = "입찰 공고의 AI 요약 요청과 결과를 관리합니다."
)
@RestController
@RequestMapping("/api/v1/bidding")
@RequiredArgsConstructor
public class BidNoticeSummaryController {

    private final CreateBidNoticeSummaryUseCase createUseCase;
    private final GetBidNoticeSummaryHistoryUseCase getHistoryUseCase;
    private final GetBidNoticeSummaryUseCase getUseCase;
    private final UpdateBidNoticeSummaryUseCase updateUseCase;
    private final ConfirmBidNoticeSummaryUseCase confirmUseCase;

    @Operation(
            summary = "입찰 공고 AI 요약 요청",
            description = "현재 회사가 조회할 수 있는 입찰 공고를 사용자가 입력한 프롬프트로 비동기 요약합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "AI 요약 요청 접수 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "BIDDING_INVALID_SUMMARY_REQUEST"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "BIDDING_ACCESS_PERMISSION_REQUIRED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "BIDDING_NOTICE_NOT_FOUND"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "BIDDING_SUMMARY_ALREADY_PROCESSING"
            )
    })
    @PostMapping("/notices/{noticeId}/summaries")
    public ResponseEntity<ApiResponse<CreateBidNoticeSummaryResponse>> create(
            @Parameter(description = "요약할 입찰 공고 ID")
            @PathVariable Long noticeId,
            @Valid @RequestBody CreateBidNoticeSummaryRequest request,
            Authentication authentication
    ) {
        CreateBidNoticeSummaryResult result = createUseCase.create(
                request.toCommand(
                        noticeId,
                        authentication.getName(),
                        RequesterRole.from(authentication)
                )
        );

        CreateBidNoticeSummaryResponse response =
                CreateBidNoticeSummaryResponse.from(result);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of(
                        HttpStatus.ACCEPTED.value(),
                        "입찰 공고 AI 요약 요청이 접수되었습니다.",
                        response
                ));
    }

    @Operation(
            summary = "공고별 입찰 AI 요약 이력 조회",
            description = "현재 사용자의 요약과 같은 회사에서 확정된 요약을 최신순으로 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요약 이력 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "BIDDING_INVALID_SUMMARY_REQUEST"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "BIDDING_NOTICE_NOT_FOUND")
    })
    @GetMapping("/notices/{noticeId}/summaries")
    public ResponseEntity<ApiResponse<BidNoticeSummaryHistoryResponse>> getHistory(
            @Parameter(description = "입찰 공고 ID") @PathVariable Long noticeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        var result = getHistoryUseCase.get(new GetBidNoticeSummaryHistoryQuery(
                noticeId, page, size, authentication.getName(),
                RequesterRole.from(authentication)
        ));
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK.value(),
                "입찰 공고 AI 요약 이력 조회 성공",
                BidNoticeSummaryHistoryResponse.from(result)
        ));
    }

    @Operation(summary = "입찰 AI 요약 조회", description = "입찰 공고 AI 요약의 처리 상태와 구조화 결과를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요약 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "BIDDING_INVALID_SUMMARY_REQUEST"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "BIDDING_SUMMARY_NOT_FOUND")
    })
    @GetMapping("/summaries/{summaryId}")
    public ResponseEntity<ApiResponse<BidNoticeSummaryResponse>> get(
            @Parameter(description = "AI 요약 ID") @PathVariable Long summaryId,
            Authentication authentication
    ) {
        BidNoticeSummaryResult result = getUseCase.get(new GetBidNoticeSummaryQuery(
                summaryId, authentication.getName(), RequesterRole.from(authentication)
        ));
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK.value(), "입찰 공고 AI 요약 조회 성공",
                BidNoticeSummaryResponse.from(result)
        ));
    }

    @Operation(summary = "입찰 AI 요약 수정", description = "요청자가 완료된 미확정 AI 요약을 부분 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요약 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "BIDDING_INVALID_SUMMARY_UPDATE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "BIDDING_SUMMARY_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "BIDDING_SUMMARY_NOT_EDITABLE")
    })
    @PatchMapping("/summaries/{summaryId}")
    public ResponseEntity<ApiResponse<BidNoticeSummaryResponse>> update(
            @Parameter(description = "수정할 AI 요약 ID") @PathVariable Long summaryId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateBidNoticeSummaryRequest.class))
            )
            @RequestBody JsonNode body,
            Authentication authentication
    ) {
        BidNoticeSummaryResult result = updateUseCase.update(
                UpdateBidNoticeSummaryRequest.toCommand(
                        summaryId, body, authentication.getName(),
                        RequesterRole.from(authentication)
                )
        );
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK.value(), "입찰 공고 AI 요약 수정 성공",
                BidNoticeSummaryResponse.from(result)
        ));
    }

    @Operation(summary = "입찰 AI 요약 확정", description = "요청자의 검토가 끝난 완료 요약을 최종 확정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요약 확정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "BIDDING_INVALID_SUMMARY_REQUEST"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "BIDDING_SUMMARY_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "BIDDING_SUMMARY_NOT_COMPLETED 또는 BIDDING_SUMMARY_ALREADY_CONFIRMED")
    })
    @PatchMapping("/summaries/{summaryId}/confirm")
    public ResponseEntity<ApiResponse<ConfirmBidNoticeSummaryResponse>> confirm(
            @Parameter(description = "확정할 AI 요약 ID") @PathVariable Long summaryId,
            Authentication authentication
    ) {
        ConfirmBidNoticeSummaryResult result = confirmUseCase.confirm(
                new ConfirmBidNoticeSummaryCommand(
                        summaryId, authentication.getName(),
                        RequesterRole.from(authentication)
                )
        );
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK.value(), "입찰 공고 AI 요약 확정 성공",
                ConfirmBidNoticeSummaryResponse.from(result)
        ));
    }
}
