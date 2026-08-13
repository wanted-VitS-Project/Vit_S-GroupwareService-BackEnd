package com.group3.vitamins.file.presentation.api;

import com.group3.vitamins.file.application.query.CompanyFileQuery;
import com.group3.vitamins.file.application.result.CompanyFilePageResult;
import com.group3.vitamins.file.application.usecase.FileListViewUseCase;
import com.group3.vitamins.file.presentation.api.response.CompanyFilePageResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "File - 전사 파일 관리", description = "전사 모든 프로젝트 파일 조회 (ADMIN 전용 · FILE-Q-01)")
@RestController
@RequestMapping("/api/v1/admin/files")
@RequiredArgsConstructor
public class CompanyFileController {

    private final FileListViewUseCase fileListViewUseCase;

    @Operation(summary = "전사 파일 관리 목록",
            description = "회사(테넌트) 안의 모든 프로젝트 파일을 문서 단위 최신 완료 버전으로 페이지 조회한다. ADMIN 전용. "
                    + "프로젝트·확장자·검색어(파일명/원본명/업로더)로 필터한다. 다운로드·미리보기는 클릭 시 기존 §9/§10 을 호출한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED — ADMIN 권한 없음")
    })
    @GetMapping
    public ApiResponse<CompanyFilePageResponse> getCompanyFiles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String extension,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        CompanyFilePageResult result = fileListViewUseCase.getCompanyFiles(new CompanyFileQuery(
                authentication.getName(), RequesterRole.from(authentication),
                keyword, projectId, extension, page, size));

        return ApiResponse.success(FileResponseMessage.COMPANY_FILES, CompanyFilePageResponse.from(result));
    }
}
