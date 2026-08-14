package com.group3.vitamins.bidding.bidnotice.presentation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.bidding.bidnotice.application.query.GetBidNoticeDetailQuery;
import com.group3.vitamins.bidding.bidnotice.application.query.SearchBidNoticesQuery;
import com.group3.vitamins.bidding.bidnotice.application.command.CompleteBidNoticeAttachmentUploadCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.FavoriteBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.RestoreBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.UnfavoriteBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.usecase.BidNoticeCommandUseCase;
import com.group3.vitamins.bidding.bidnotice.application.usecase.BidNoticeQueryUseCase;
import com.group3.vitamins.bidding.bidnotice.presentation.api.request.CreateManualBidNoticeRequest;
import com.group3.vitamins.bidding.bidnotice.presentation.api.request.DismissBidNoticeRequest;
import com.group3.vitamins.bidding.bidnotice.presentation.api.request.StartBidNoticeAttachmentUploadRequest;
import com.group3.vitamins.bidding.bidnotice.presentation.api.request.UpdateManualBidNoticeRequest;
import com.group3.vitamins.bidding.bidnotice.presentation.api.request.UpdateManualBidNoticeRequestMapper;
import com.group3.vitamins.bidding.bidnotice.presentation.api.response.BidNoticeAttachmentUploadCompleteResponse;
import com.group3.vitamins.bidding.bidnotice.presentation.api.response.BidNoticeAttachmentUploadStartResponse;
import com.group3.vitamins.bidding.bidnotice.presentation.api.response.BidNoticeDetailResponse;
import com.group3.vitamins.bidding.bidnotice.presentation.api.response.BidNoticeListResponse;
import com.group3.vitamins.bidding.bidnotice.presentation.api.response.BidNoticeStatusResponse;
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
import jakarta.validation.Valid;


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
            @Parameter(description = "true면 현재 회사가 관심 등록한 공고만 조회")
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(defaultValue = "ANNOUNCED_DESC") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        BidNoticeListResponse response = BidNoticeListResponse.from(
                bidNoticeQueryUseCase.handle(new SearchBidNoticesQuery(
                        startDate, endDate, noticeAgency, businessCategoryId, region,
                        deadlineSoon, keyword, noticeStatus, favorite, sort, page, size,
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
            @Valid @RequestBody CreateManualBidNoticeRequest request,
            Authentication authentication
    ) {
        ManualBidNoticeResponse response = ManualBidNoticeResponse.from(
                bidNoticeCommandUseCase.create(request.toCommand(
                        authentication.getName(),
                        RequesterRole.from(authentication)
                ))
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
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

    @Operation(
            summary = "입찰 공고 제외",
            description = "현재 회사의 입찰 공고를 검토 대상에서 제외합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입찰 공고 제외 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "BIDDING_INVALID_DISMISS_REASON"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "BIDDING_NOTICE_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "BIDDING_NOTICE_ALREADY_DISMISSED")
    })
    @PatchMapping("/{noticeId}/dismiss")
    public ResponseEntity<ApiResponse<BidNoticeStatusResponse>> dismiss(
            @Parameter(description = "제외할 입찰 공고 ID")
            @PathVariable Long noticeId,
            @Valid @RequestBody DismissBidNoticeRequest request,
            Authentication authentication
    ) {
        BidNoticeStatusResponse response = BidNoticeStatusResponse.from(
                bidNoticeCommandUseCase.dismiss(request.toCommand(
                        noticeId,
                        authentication.getName(),
                        RequesterRole.from(authentication)
                ))
        );
        return ResponseEntity.ok(ApiResponse.success("입찰 공고 제외 성공", response));
    }

    @Operation(
            summary = "입찰 공고 복구",
            description = "현재 회사가 제외한 입찰 공고를 검토 대상으로 복구합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입찰 공고 복구 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "BIDDING_NOTICE_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "BIDDING_NOTICE_NOT_DISMISSED")
    })
    @PatchMapping("/{noticeId}/restore")
    public ResponseEntity<ApiResponse<BidNoticeStatusResponse>> restore(
            @Parameter(description = "복구할 입찰 공고 ID")
            @PathVariable Long noticeId,
            Authentication authentication
    ) {
        BidNoticeStatusResponse response = BidNoticeStatusResponse.from(
                bidNoticeCommandUseCase.restore(new RestoreBidNoticeCommand(
                        noticeId,
                        authentication.getName(),
                        RequesterRole.from(authentication)
                ))
        );
        return ResponseEntity.ok(ApiResponse.success("입찰 공고 복구 성공", response));
    }

    @Operation(
            summary = "입찰 공고 관심 등록",
            description = "현재 회사 공용 관심 목록에 공고를 등록합니다. 어느 직원이 등록해도 같은 회사 전원에게 동일하게 보입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관심 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "BIDDING_NOTICE_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "BIDDING_NOTICE_ALREADY_FAVORITED")
    })
    @PatchMapping("/{noticeId}/favorite")
    public ResponseEntity<ApiResponse<BidNoticeStatusResponse>> favorite(
            @Parameter(description = "관심 등록할 입찰 공고 ID")
            @PathVariable Long noticeId,
            Authentication authentication
    ) {
        BidNoticeStatusResponse response = BidNoticeStatusResponse.from(
                bidNoticeCommandUseCase.favorite(new FavoriteBidNoticeCommand(
                        noticeId,
                        authentication.getName(),
                        RequesterRole.from(authentication)
                ))
        );
        return ResponseEntity.ok(ApiResponse.success("입찰 공고 관심 등록 성공", response));
    }

    @Operation(
            summary = "입찰 공고 관심 해제",
            description = "현재 회사 공용 관심 목록에서 공고를 해제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관심 해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "BIDDING_NOTICE_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "BIDDING_NOTICE_NOT_FAVORITED")
    })
    @PatchMapping("/{noticeId}/unfavorite")
    public ResponseEntity<ApiResponse<BidNoticeStatusResponse>> unfavorite(
            @Parameter(description = "관심 해제할 입찰 공고 ID")
            @PathVariable Long noticeId,
            Authentication authentication
    ) {
        BidNoticeStatusResponse response = BidNoticeStatusResponse.from(
                bidNoticeCommandUseCase.unfavorite(new UnfavoriteBidNoticeCommand(
                        noticeId,
                        authentication.getName(),
                        RequesterRole.from(authentication)
                ))
        );
        return ResponseEntity.ok(ApiResponse.success("입찰 공고 관심 해제 성공", response));
    }

    @Operation(
            summary = "직접 등록 공고 첨부 업로드 시작",
            description = "공개 URL이 없는 첨부파일을 위해 직접 등록(MANUAL) 공고에 업로드 슬롯을 만들고 " +
                    "S3 presigned PUT URL을 발급합니다. 클라이언트가 이 URL로 파일을 직접 업로드한 뒤 " +
                    "완료 API를 호출해야 첨부가 실제로 사용 가능해집니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "업로드 슬롯 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "BIDDING_INVALID_MANUAL_NOTICE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "BIDDING_NOTICE_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "BIDDING_NOTICE_EDIT_NOT_ALLOWED(직접 등록 공고가 아님) 또는 " +
                            "BIDDING_MANUAL_NOTICE_ATTACHMENT_LIMIT_EXCEEDED(첨부 10개 초과)"
            )
    })
    @PostMapping("/{noticeId}/attachments/uploads")
    public ResponseEntity<ApiResponse<BidNoticeAttachmentUploadStartResponse>> startAttachmentUpload(
            @Parameter(description = "첨부를 추가할 직접 등록 입찰 공고 ID")
            @PathVariable Long noticeId,
            @Valid @RequestBody StartBidNoticeAttachmentUploadRequest request,
            Authentication authentication
    ) {
        BidNoticeAttachmentUploadStartResponse response = BidNoticeAttachmentUploadStartResponse.from(
                bidNoticeCommandUseCase.startAttachmentUpload(request.toCommand(
                        noticeId,
                        authentication.getName(),
                        RequesterRole.from(authentication)
                ))
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("업로드 슬롯이 생성됐습니다.", response));
    }

    @Operation(
            summary = "직접 등록 공고 첨부 업로드 완료 통보",
            description = "presigned URL로 업로드를 마친 뒤 호출합니다. 서버가 저장소에 실제로 객체가 " +
                    "있는지, 크기가 요청과 일치하는지 확인한 뒤에만 첨부를 사용 가능 상태로 반영합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 완료 확인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "BIDDING_NOTICE_NOT_FOUND 또는 BIDDING_MANUAL_NOTICE_ATTACHMENT_NOT_FOUND"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "BIDDING_NOTICE_EDIT_NOT_ALLOWED · BIDDING_MANUAL_NOTICE_ATTACHMENT_ALREADY_COMPLETED · " +
                            "BIDDING_MANUAL_NOTICE_ATTACHMENT_OBJECT_NOT_FOUND · BIDDING_MANUAL_NOTICE_ATTACHMENT_SIZE_MISMATCH"
            )
    })
    @PostMapping("/{noticeId}/attachments/uploads/{attachmentId}/complete")
    public ResponseEntity<ApiResponse<BidNoticeAttachmentUploadCompleteResponse>> completeAttachmentUpload(
            @Parameter(description = "직접 등록 입찰 공고 ID")
            @PathVariable Long noticeId,
            @Parameter(description = "업로드 시작 응답에서 받은 첨부 ID")
            @PathVariable Long attachmentId,
            Authentication authentication
    ) {
        BidNoticeAttachmentUploadCompleteResponse response = BidNoticeAttachmentUploadCompleteResponse.from(
                bidNoticeCommandUseCase.completeAttachmentUpload(new CompleteBidNoticeAttachmentUploadCommand(
                        noticeId,
                        attachmentId,
                        authentication.getName(),
                        RequesterRole.from(authentication)
                ))
        );

        return ResponseEntity.ok(ApiResponse.success("첨부 업로드 완료가 확인됐습니다.", response));
    }
}
