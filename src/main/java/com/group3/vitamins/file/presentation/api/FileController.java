package com.group3.vitamins.file.presentation.api;

import com.group3.vitamins.file.application.usecase.FileQueryUseCase;
import com.group3.vitamins.file.presentation.api.response.VersionHistoryResponse;
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

@Tag(name = "File - 문서", description = "문서 단위 조회·수정 — 스텝 권한을 따른다")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileQueryUseCase fileQueryUseCase;

    @Operation(summary = "버전 이력 조회",
            description = "문서의 완료된 버전들을 차수 내림차순으로 돌려준다. 업로드에 실패했거나 미완료인 버전은 제외된다. "
                    + "업로더 정보는 각 버전 시점의 스냅샷이다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FILE_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FILE_NOT_FOUND — 문서 없음 또는 휴지통")
    })
    @GetMapping("/{fileId}/versions")
    public ApiResponse<VersionHistoryResponse> getVersionHistory(
            @PathVariable Long fileId,
            Authentication authentication
    ) {
        VersionHistoryResponse data = VersionHistoryResponse.from(
                fileQueryUseCase.getVersionHistory(
                        fileId, authentication.getName(), RequesterRole.from(authentication)));

        return ApiResponse.success(FileResponseMessage.VERSION_HISTORY, data);
    }
}
