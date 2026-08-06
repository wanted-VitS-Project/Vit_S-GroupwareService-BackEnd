package com.group3.vitamins.file.presentation.api;

import com.group3.vitamins.file.application.usecase.FileQueryUseCase;
import com.group3.vitamins.file.presentation.api.response.ProjectFileVersionResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "File - 프로젝트 파일 버전 목록", description = "비타메이트 분석 선택용 — 프로젝트 접근 권한을 따른다")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectFileVersionController {

    private final FileQueryUseCase fileQueryUseCase;

    @Operation(summary = "파일 버전 목록 조회 (비타메이트 분석 선택용)",
            description = "프로젝트에 속한 모든 문서의 완료된 버전을 돌려준다(과거 버전 포함). 블록이 삭제돼 고아가 된 파일도 포함된다. "
                    + "휴지통 문서는 제외한다. indexStatus 는 file_index 출처이며, 인덱스 행이 없으면 PENDING 으로 내려온다. "
                    + "프론트는 indexStatus=COMPLETED 인 버전만 선택 가능하게 처리한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FILE_ACCESS_PERMISSION_REQUIRED — 프로젝트 접근(열람) 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND — 프로젝트 없음")
    })
    @GetMapping("/{projectId}/file-versions")
    public ApiResponse<List<ProjectFileVersionResponse>> getProjectFileVersions(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        List<ProjectFileVersionResponse> data = fileQueryUseCase.getProjectFileVersions(
                        projectId, authentication.getName(), RequesterRole.from(authentication))
                .stream()
                .map(ProjectFileVersionResponse::from)
                .toList();

        return ApiResponse.success(FileResponseMessage.PROJECT_FILE_VERSIONS, data);
    }
}
