package com.group3.vitamins.bidding.bidnotice.presentation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.bidding.bidnotice.application.query.GetBidNoticeDetailQuery;
import com.group3.vitamins.bidding.bidnotice.application.query.SearchBidNoticesQuery;
import com.group3.vitamins.bidding.bidnotice.application.usecase.BidNoticeCommandUseCase;
import com.group3.vitamins.bidding.bidnotice.application.usecase.BidNoticeQueryUseCase;
import com.group3.vitamins.bidding.bidnotice.presentation.api.request.CreateManualBidNoticeRequest;
import com.group3.vitamins.bidding.bidnotice.presentation.api.request.UpdateManualBidNoticeRequest;
import com.group3.vitamins.bidding.bidnotice.presentation.api.request.UpdateManualBidNoticeRequestMapper;
import com.group3.vitamins.bidding.bidnotice.presentation.api.response.BidNoticeDetailResponse;
import com.group3.vitamins.bidding.bidnotice.presentation.api.response.BidNoticeListResponse;
import com.group3.vitamins.bidding.bidnotice.presentation.api.response.ManualBidNoticeResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;

@Tag(name = "Bidding - 입찰 공고", description = "현재 회사가 수집하거나 직접 등록한 입찰 공고를 조회하고 관리합니다.")
@RestController
@RequestMapping("/api/v1/bidding/notices")
@RequiredArgsConstructor
public class BidNoticeController {

    private final BidNoticeQueryUseCase bidNoticeQueryUseCase;
    private final BidNoticeCommandUseCase bidNoticeCommandUseCase;
    private final UpdateManualBidNoticeRequestMapper updateRequestMapper;

    @Operation(summary = "입찰 공고 목록 조회", description = "현재 회사가 수집한 입찰 공고를 검색 조건과 페이징 기준으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입찰 공고 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "BIDDING_INVALID_NOTICE_QUERY"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<BidNoticeListResponse>> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String noticeAgency,
            @RequestParam(required = false) Long businessCategoryId,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Boolean deadlineSoon,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String noticeStatus,
            @RequestParam(defaultValue = "ANNOUNCED_DESC") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        BidNoticeListResponse response = BidNoticeListResponse.from(
                bidNoticeQueryUseCase.handle(new SearchBidNoticesQuery(
                        startDate, endDate, noticeAgency, businessCategoryId, region,
                        deadlineSoon, keyword, noticeStatus, sort, page, size,
                        authentication.getName(), RequesterRole.from(authentication)
                ))
        );
        return ResponseEntity.ok(ApiResponse.success("입찰 공고 목록 조회 성공", response));
    }

    @Operation(summary = "입찰 공고 상세 조회", description = "현재 회사가 수집한 입찰 공고의 상세 정보와 첨부파일을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입찰 공고 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "BIDDING_INVALID_NOTICE_QUERY"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "BIDDING_NOTICE_NOT_FOUND")
    })
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<BidNoticeDetailResponse>> getDetail(
            @Parameter(description = "입찰 공고 ID") @PathVariable Long noticeId,
            Authentication authentication
    ) {
        BidNoticeDetailResponse response = BidNoticeDetailResponse.from(
                bidNoticeQueryUseCase.handle(new GetBidNoticeDetailQuery(
                        noticeId, authentication.getName(), RequesterRole.from(authentication)
                ))
        );
        return ResponseEntity.ok(ApiResponse.success("입찰 공고 상세 조회 성공", response));
    }

    @Operation(
            summary = "입찰 공고 직접 등록",
            description = "자동으로 수집되지 않은 입찰 공고와 공개 첨부 링크를 현재 회사 소유로 등록합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "입찰 공고 직접 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "BIDDING_INVALID_MANUAL_NOTICE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "BIDDING_MANUAL_NOTICE_DUPLICATED")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ManualBidNoticeResponse>> create(
            @RequestBody CreateManualBidNoticeRequest request,
            Authentication authentication
    ) {
        ManualBidNoticeResponse response = ManualBidNoticeResponse.from(
                bidNoticeCommandUseCase.create(request.toCommand(
                        authentication.getName(),
                        RequesterRole.from(authentication)
                ))
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "입찰 공고 직접 등록 성공",
                        response
                ));
    }

    @Operation(
            summary = "직접 등록 입찰 공고 수정",
            description = "현재 회사가 직접 등록한 입찰 공고에서 전달된 필드만 부분 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입찰 공고 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "BIDDING_INVALID_MANUAL_NOTICE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "BIDDING_NOTICE_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "BIDDING_MANUAL_NOTICE_DUPLICATED 또는 BIDDING_NOTICE_EDIT_NOT_ALLOWED"
            )
    })
    @PatchMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<ManualBidNoticeResponse>> update(
            @Parameter(description = "수정할 직접 등록 입찰 공고 ID")
            @PathVariable Long noticeId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateManualBidNoticeRequest.class))
            )
            @RequestBody JsonNode body,
            Authentication authentication
    ) {
        ManualBidNoticeResponse response = ManualBidNoticeResponse.from(
                bidNoticeCommandUseCase.update(
                        updateRequestMapper.toCommand(
                                noticeId,
                                body,
                                authentication.getName(),
                                RequesterRole.from(authentication)
                        )
                )
        );

        return ResponseEntity.ok(ApiResponse.success(
                "입찰 공고 수정 성공",
                response
        ));
    }
}
