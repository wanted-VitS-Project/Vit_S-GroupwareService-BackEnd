package com.group3.vitamins.file.presentation.api;

import com.group3.vitamins.file.application.usecase.FileQueryUseCase;
import com.group3.vitamins.file.presentation.api.response.ProjectFileListResponse;
import com.group3.vitamins.file.presentation.api.response.ProjectFileResponse;
import com.group3.vitamins.file.presentation.api.response.ProjectTrashFileListResponse;
import com.group3.vitamins.file.presentation.api.response.ProjectTrashFileResponse;
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

    @Operation(summary = "프로젝트 휴지통 모아보기",
            description = "프로젝트에서 삭제(휴지통)된 문서를 프로젝트 전체 범위로 돌려준다(§13). 블록이 삭제돼 블록 단위 휴지통(§3)으로는 "
                    + "볼 수 없는 고아 파일도 여기서 회수한다(blockId·blockTitle=null·blockDeleted=true). 복구(§6)·영구삭제(§7) 전용 화면이라 "
                    + "다운로드 진입점(previewable·최신 버전 ID 등)은 내리지 않고 휴지통 진입 시각(deletedAt)을 준다. 정렬은 deletedAt 내림차순.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FILE_ACCESS_PERMISSION_REQUIRED — 프로젝트 접근(열람) 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND — 프로젝트 없음")
    })
    @GetMapping("/{projectId}/files/trash")
    public ApiResponse<ProjectTrashFileListResponse> getProjectTrashFiles(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        // ⚠️ §12 와 동일하게 계약이 data.files[] 라 배열을 files 로 감싼다. data 로 바로 내리면 프론트가 못 읽는다.
        List<ProjectTrashFileResponse> files = fileQueryUseCase.getProjectTrashFiles(
                        projectId, authentication.getName(), RequesterRole.from(authentication))
                .stream()
                .map(ProjectTrashFileResponse::from)
                .toList();

        return ApiResponse.success(FileResponseMessage.PROJECT_TRASH_FILES, new ProjectTrashFileListResponse(files));
    }
}
