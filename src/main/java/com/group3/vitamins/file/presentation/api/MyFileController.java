package com.group3.vitamins.file.presentation.api;

import com.group3.vitamins.file.application.query.MyProjectFileQuery;
import com.group3.vitamins.file.application.usecase.FileListViewUseCase;
import com.group3.vitamins.file.presentation.api.response.FileViewResponse;
import com.group3.vitamins.file.presentation.api.response.MyProjectFileListResponse;
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

import java.util.List;

@Tag(name = "File - 내 프로젝트 파일 모아보기", description = "내가 멤버인 프로젝트의 파일 중 스텝 VIEWER 이상만 (FILE-Q-03)")
@RestController
@RequestMapping("/api/v1/files/my")
@RequiredArgsConstructor
public class MyFileController {

    private final FileListViewUseCase fileListViewUseCase;

    @Operation(summary = "내 프로젝트 파일 모아보기",
            description = "내가 멤버인 모든 프로젝트의 파일을 문서 단위 최신 완료 버전으로 돌려준다. 스텝 VIEWER 이상 권한이 있는 "
                    + "파일만 노출한다 — 스텝 override(NONE)로 강등된 파일은 제외된다. 단 전역 ADMIN·MASTER 는 스텝 접근 정책상 "
                    + "모든 스텝에서 EDITOR 이므로(우회가 아니라 정책상 권한) 자신이 멤버인 프로젝트의 전 파일을 본다. "
                    + "프로젝트 → 스텝 → 블록 순으로 정렬되며 프론트가 projectId 로 그룹핑한다. 프로젝트·확장자·검색어로 필터한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED")
    })
    @GetMapping
    public ApiResponse<MyProjectFileListResponse> getMyProjectFiles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String extension,
            Authentication authentication
    ) {
        List<FileViewResponse> files = fileListViewUseCase.getMyProjectFiles(new MyProjectFileQuery(
                        authentication.getName(), RequesterRole.from(authentication), keyword, projectId, extension))
                .stream()
                .map(FileViewResponse::from)
                .toList();

        return ApiResponse.success(FileResponseMessage.MY_PROJECT_FILES, new MyProjectFileListResponse(files));
    }
}
