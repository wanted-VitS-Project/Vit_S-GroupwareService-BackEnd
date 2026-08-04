package com.group3.vitamins.project.block.presentation;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.project.block.application.query.BlockListQuery;
import com.group3.vitamins.project.block.application.result.BlockSummary;
import com.group3.vitamins.project.block.application.usecase.BlockQueryUseCase;
import com.group3.vitamins.project.block.presentation.api.response.BlockListResponse;
import com.group3.vitamins.project.presentation.api.ProjectResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Block - 블록", description = "블록 조회 / 생성 / 배치 / 삭제 (담당: 동훈)")
@RestController
@RequestMapping("/api/v1/steps/{stepId}/blocks")
@RequiredArgsConstructor
public class StepBlockController {

    private final BlockQueryUseCase blockQueryUseCase;

    @Operation(summary = "스텝 블록 일괄 조회",
            description = "블록 골격과 타입별 상세를 한 응답에 담아 rowIndex → sortOrder 순으로 내린다. "
                    + "블록은 진행 상태를 갖지 않으므로 status 필드가 없다. "
                    + "typeId 는 내부 식별자라 최상위에 내리지 않고 detail 안에 상세 PK 로만 담는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "STEP_ACCESS_DENIED — 스텝 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "STEP_NOT_FOUND — 스텝이 존재하지 않음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<BlockListResponse>> getBlocks(
            @Parameter(description = "조회할 스텝 ID")
            @PathVariable Long stepId,
            Authentication authentication
    ) {
        List<BlockSummary> blocks = blockQueryUseCase.getBlocks(new BlockListQuery(
                stepId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        BlockListResponse.from(blocks)));
    }
}