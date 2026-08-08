package com.group3.vitamins.file.presentation.api;

import com.group3.vitamins.file.application.usecase.FileQueryUseCase;
import com.group3.vitamins.file.presentation.api.response.ProjectFileListResponse;
import com.group3.vitamins.file.presentation.api.response.ProjectFileResponse;
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

@Tag(name = "File - 프로젝트 파일 모아보기", description = "프로젝트 문서함 — 프로젝트 접근 권한을 따른다")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectFileController {

    private final FileQueryUseCase fileQueryUseCase;

    @Operation(summary = "프로젝트 전체 파일 모아보기 (문서함)",
            description = "프로젝트에 속한 활성 문서를 문서 단위 최신 완료 버전 1행으로 돌려준다. 스텝·블록 위치를 함께 주고 "
                    + "프론트가 스텝→블록 트리로 조합한다. 블록이 삭제된 고아 파일도 포함되며(blockId·blockTitle=null·blockDeleted=true), "
                    + "휴지통 문서는 제외한다(§13). 다운로드는 클릭 시 §9(다운로드 URL 발급)를 호출한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FILE_ACCESS_PERMISSION_REQUIRED — 프로젝트 접근(열람) 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND — 프로젝트 없음")
    })
    @GetMapping("/{projectId}/files")
    public ApiResponse<ProjectFileListResponse> getProjectFiles(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        // ⚠️ 계약이 data.files[] 라 배열을 files 로 감싼다(이미지 모아보기와 구조 통일). data 로 바로 내리면 프론트가 못 읽는다.
        List<ProjectFileResponse> files = fileQueryUseCase.getProjectFiles(
                        projectId, authentication.getName(), RequesterRole.from(authentication))
                .stream()
                .map(ProjectFileResponse::from)
                .toList();

        return ApiResponse.success(FileResponseMessage.PROJECT_FILES, new ProjectFileListResponse(files));
    }
}
