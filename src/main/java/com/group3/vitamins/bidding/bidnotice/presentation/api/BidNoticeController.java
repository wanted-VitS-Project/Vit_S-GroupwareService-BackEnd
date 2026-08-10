package com.group3.vitamins.bidding.bidnotice.presentation.api;

import com.group3.vitamins.bidding.bidnotice.application.query.GetBidNoticeDetailQuery;
import com.group3.vitamins.bidding.bidnotice.application.query.SearchBidNoticesQuery;
import com.group3.vitamins.bidding.bidnotice.application.usecase.BidNoticeQueryUseCase;
import com.group3.vitamins.bidding.bidnotice.presentation.api.response.BidNoticeDetailResponse;
import com.group3.vitamins.bidding.bidnotice.presentation.api.response.BidNoticeListResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Bidding - 입찰 공고", description = "현재 회사가 수집한 입찰 공고를 조회합니다.")
@RestController
@RequestMapping("/api/v1/bidding/notices")
@RequiredArgsConstructor
public class BidNoticeController {

    private final BidNoticeQueryUseCase bidNoticeQueryUseCase;

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
}
