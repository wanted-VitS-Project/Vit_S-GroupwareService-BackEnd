package com.group3.vitamins.file.presentation.api;

import com.group3.vitamins.file.application.usecase.FileQueryUseCase;
import com.group3.vitamins.file.presentation.api.response.BlockFileListResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "BlockFile - 블록 파일", description = "블록에 붙은 파일 목록 — 스텝 권한을 따른다")
@RestController
@RequestMapping("/api/v1/blocks")
@RequiredArgsConstructor
public class BlockFileController {

    private final FileQueryUseCase fileQueryUseCase;

    @Operation(summary = "블록 파일 목록 조회",
            description = "블록에 붙은 문서들을 최신 완료 버전 기준으로 돌려준다(연결일 오름차순). 완료 버전이 0개인 문서는 제외된다. "
                    + "deleted=true 면 휴지통을 조회한다. 같은 데이터·같은 권한이라 API 를 나누지 않았다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FILE_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FILE_BLOCK_NOT_FOUND — 블록 없음/삭제")
    })
    @GetMapping("/{blockId}/files")
    public ApiResponse<BlockFileListResponse> getBlockFiles(
            @PathVariable Long blockId,
            @RequestParam(name = "deleted", required = false, defaultValue = "false") boolean deleted,
            Authentication authentication
    ) {
        BlockFileListResponse data = BlockFileListResponse.from(
                fileQueryUseCase.getBlockFiles(
                        blockId, deleted, authentication.getName(), RequesterRole.from(authentication)));

        return ApiResponse.success(FileResponseMessage.BLOCK_FILES, data);
    }
}
