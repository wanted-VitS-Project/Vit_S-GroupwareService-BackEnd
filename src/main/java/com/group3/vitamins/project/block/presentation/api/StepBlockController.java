package com.group3.vitamins.project.block.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.project.block.application.query.BlockListQuery;
import com.group3.vitamins.project.block.application.result.BlockSummary;
import com.group3.vitamins.project.block.application.usecase.BlockCommandUseCase;
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
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.group3.vitamins.project.block.application.result.BlockResult;
import com.group3.vitamins.project.block.presentation.api.request.BlockCreateRequest;
import com.group3.vitamins.project.block.presentation.api.response.BlockCreateResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.group3.vitamins.project.block.application.result.BlockLayoutResult;
import com.group3.vitamins.project.block.presentation.api.request.BlockLayoutUpdateRequest;
import com.group3.vitamins.project.block.presentation.api.response.BlockLayoutListResponse;

@Tag(name = "Block - 블록", description = "블록 조회 / 생성 / 배치 / 삭제 (담당: 동훈)")
@RestController
@RequestMapping("/api/v1/steps/{stepId}/blocks")
@RequiredArgsConstructor
public class StepBlockController {

    private final BlockQueryUseCase blockQueryUseCase;
    private final BlockCommandUseCase blockCommandUseCase;

    @Operation(summary = "블록 생성",
            description = "스텝에 블록을 추가하고 타입별 상세 빈 행을 같은 트랜잭션에서 만든다. "
                    + "제목·담당자는 생략할 수 있고 추가 직후 빈 카드 상태가 된다. "
                    + "BID_NOTICE 는 공고 전환 API 만 생성하므로 보내면 400 이다. "
                    + "PAYMENT_CONFIRM·TAX_INVOICE_VIEW 는 스텝당 1개만 허용된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "BLOCK_TYPE_INVALID / BLOCK_TITLE_TOO_LONG / BLOCK_COL_SPAN_INVALID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "STEP_EDIT_DENIED — 스텝 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "STEP_NOT_FOUND / USER_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "PAYMENT_CONFIRM_BLOCK_DUPLICATED / TAX_INVOICE_VIEW_BLOCK_DUPLICATED")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<BlockCreateResponse>> createBlock(
            @Parameter(description = "블록을 추가할 스텝 ID")
            @PathVariable Long stepId,
            @RequestBody BlockCreateRequest request,
            Authentication authentication
    ) {
        BlockResult result = blockCommandUseCase.createBlock( request.toCommand(
                stepId, authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.status(201).body(
                ApiResponse.created(ProjectResponseMessage.SUCCESS,
                        BlockCreateResponse.from(result)));
    }


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


    @Operation(summary = "블록 배치 변경",
            description = "드래그 결과를 한 트랜잭션에서 일괄 반영한다. 총 열 수는 3 고정이고, "
                    + "같은 행에 순서가 겹쳐도 허용한다(드래그 중간 상태) — UNIQUE 를 걸지 않은 이유다. "
                    + "다른 스텝의 블록이 섞이면 400 이다. 제목·담당자는 PATCH /api/v1/blocks/{blockId} 담당이다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "배치 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "BLOCK_COL_SPAN_INVALID / BLOCK_LAYOUT_INVALID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "STEP_EDIT_DENIED — 스텝 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "STEP_NOT_FOUND / BLOCK_NOT_FOUND")
    })
    @PatchMapping("/layout")
    public ResponseEntity<ApiResponse<BlockLayoutListResponse>> updateLayout(
            @Parameter(description = "배치를 바꿀 스텝 ID")
            @PathVariable Long stepId,
            @RequestBody BlockLayoutUpdateRequest request,
            Authentication authentication
    ) {
        List<BlockLayoutResult> results = blockCommandUseCase.updateLayout(
                request.toCommand(stepId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        BlockLayoutListResponse.from(results)));
    }
}