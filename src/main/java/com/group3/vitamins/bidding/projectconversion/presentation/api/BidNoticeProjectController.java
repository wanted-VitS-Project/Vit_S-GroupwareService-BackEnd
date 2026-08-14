package com.group3.vitamins.bidding.projectconversion.presentation.api;

import com.group3.vitamins.bidding.projectconversion.application.result.ConvertNoticeToProjectResult;
import com.group3.vitamins.bidding.projectconversion.application.usecase.ConvertNoticeToProjectUseCase;
import com.group3.vitamins.bidding.projectconversion.presentation.api.request.ConvertNoticeToProjectRequest;
import com.group3.vitamins.bidding.projectconversion.presentation.api.response.ConvertNoticeToProjectResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Bidding - 공고 프로젝트 전환",
        description = "담당자가 확정한 입찰 문서 검토(및 선택적으로 AI 요약)를 근거로 공고를 프로젝트로 전환합니다."
)
@RestController
@RequestMapping("/api/v1/bidding")
@RequiredArgsConstructor
public class BidNoticeProjectController {

    private final ConvertNoticeToProjectUseCase convertNoticeToProjectUseCase;

    @Operation(
            summary = "공고 프로젝트 전환",
            description = "COMPLETED 문서 검토(및 선택적으로 확정 AI 요약)를 근거로 프로젝트를 생성합니다. "
                    + "전환 요청자는 편집 권한으로 자동 등록되고, 추가 memberIds도 함께 등록됩니다. "
                    + "검토에서 실제 다운로드에 성공한 공고 첨부는 정식 파일로 귀속되고, summaryId를 지정하면 "
                    + "해당 확정 요약에도 생성된 프로젝트가 연결됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "전환 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "COMMON_INVALID_REQUEST"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "AUTH_UNAUTHENTICATED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "BIDDING_ACCESS_PERMISSION_REQUIRED(BIDDING 권한 없음) 또는 "
                            + "BIDDING_REVIEW_ACCESS_DENIED(다른 회사·공고·요청자의 검토)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "BIDDING_NOTICE_NOT_FOUND · BIDDING_REVIEW_NOT_FOUND · BIDDING_SUMMARY_NOT_FOUND"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "PROJECT_BID_NOTICE_ALREADY_LINKED(이미 전환된 공고) · "
                            + "BIDDING_REVIEW_NOT_COMPLETED(검토 미완료) · "
                            + "BIDDING_REVIEW_ALREADY_LINKED_TO_PROJECT(검토가 이미 다른 프로젝트에 연결됨) · "
                            + "BIDDING_SUMMARY_NOT_CONFIRMED(요약 미확정) · "
                            + "BIDDING_SUMMARY_ALREADY_LINKED(요약이 이미 다른 프로젝트에 연결됨)"
            )
    })
    @PostMapping("/notices/{noticeId}/projects")
    public ResponseEntity<ApiResponse<ConvertNoticeToProjectResponse>> convert(
            @Parameter(description = "전환할 입찰 공고 ID")
            @PathVariable Long noticeId,
            @Valid @RequestBody ConvertNoticeToProjectRequest request,
            Authentication authentication
    ) {
        ConvertNoticeToProjectResult result = convertNoticeToProjectUseCase.convert(
                request.toCommand(
                        noticeId,
                        authentication.getName(),
                        RequesterRole.from(authentication)
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        HttpStatus.CREATED.value(),
                        "공고가 프로젝트로 전환됐습니다.",
                        ConvertNoticeToProjectResponse.from(result)
                ));
    }
}
