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

// ⚠️ 뼈대 단계 - 검증(1~6번)·summary/review 연결(9~10번)·기본 스테이지(13번)는 아직 서비스에서
// TODO로 남아 있다. Swagger 응답 코드도 검증 로직이 채워지면 함께 갱신해야 한다.
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
            description = "COMPLETED 문서 검토(및 선택적으로 확정 AI 요약)를 근거로 프로젝트를 생성하고, "
                    + "검토에서 실제 사용한 공고 첨부를 정식 프로젝트 파일로 귀속합니다."
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
                    responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "BIDDING_NOTICE_NOT_FOUND"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "이미 전환된 공고 등"
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
