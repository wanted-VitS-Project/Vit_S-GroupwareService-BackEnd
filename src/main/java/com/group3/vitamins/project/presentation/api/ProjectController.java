package com.group3.vitamins.project.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.project.application.result.ProjectResult;
import com.group3.vitamins.project.application.usecase.ProjectCommandUseCase;
import com.group3.vitamins.project.presentation.api.request.ProjectCreateRequest;
import com.group3.vitamins.project.presentation.api.response.ProjectDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Project - 프로젝트", description = "프로젝트 생성 / 조회 / 수정 / 삭제 (담당: 동훈)")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectCommandUseCase projectCommandUseCase;

    @Operation(summary = "프로젝트 생성",
            description = "프로젝트를 생성한다. bidNoticeId(선택)를 보내면 공고와 연결된 채로 생성한다. "
                    + "상태는 NOT_STARTED 로 고정되고, 생성자는 자동으로 EDITOR 참여자가 된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "PROJECT_NAME_REQUIRED / _NAME_TOO_LONG / _DATE_RANGE_INVALID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "BUSINESS_CATEGORY_NOT_FOUND — 사업 카테고리가 존재하지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "PROJECT_BID_NOTICE_ALREADY_LINKED — 이미 다른 프로젝트가 연결된 공고")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> createProject(
            @RequestBody ProjectCreateRequest request,
            Authentication authentication
    ) {
        ProjectResult result = projectCommandUseCase.createProject(
                request.toCommand(authentication.getName()));

        return ResponseEntity.status(201).body(
                ApiResponse.created(ProjectResponseMessage.SUCCESS,
                        ProjectDetailResponse.from(result)));
    }
}