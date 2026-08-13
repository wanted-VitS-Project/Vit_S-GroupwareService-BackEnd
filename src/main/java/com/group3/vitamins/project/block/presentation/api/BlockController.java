package com.group3.vitamins.project.block.presentation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.project.block.domain.exception.BlockErrorCode;
import com.group3.vitamins.project.block.application.command.DeleteBlockCommand;
import com.group3.vitamins.project.block.application.command.UpdateBlockCommand;
import com.group3.vitamins.project.block.application.result.BlockMoveResult;
import com.group3.vitamins.project.block.application.result.BlockUpdateResult;
import com.group3.vitamins.project.block.application.usecase.BlockCommandUseCase;
import com.group3.vitamins.project.block.presentation.api.request.BlockMoveRequest;
import com.group3.vitamins.project.block.presentation.api.request.BlockUpdateRequest;
import com.group3.vitamins.project.block.presentation.api.response.BlockMoveResponse;
import com.group3.vitamins.project.block.presentation.api.response.BlockUpdateResponse;
import com.group3.vitamins.project.presentation.api.ProjectResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 블록 단건 수정·삭제. 경로에 stepId 가 없어 StepBlockController 에 붙일 수 없다.
 * Swagger 그룹은 @Tag 이름을 같게 둬서 하나로 묶인다.
 */
@Tag(name = "Block - 블록", description = "블록 조회 / 생성 / 배치 / 삭제 (담당: 동훈)")
@RestController
@RequestMapping("/api/v1/blocks")
@RequiredArgsConstructor
public class BlockController {

    private final BlockCommandUseCase blockCommandUseCase;

    @Operation(summary = "블록 제목·담당자 수정",
            description = "보낸 필드만 반영한다. null 을 명시해서 보내면 해제이고, 생략하면 기존 값을 유지한다. "
                    + "둘 다 생략하면 400 이다. 타입은 생성 후 바꿀 수 없고(상세 테이블이 달라진다) "
                    + "배치는 PATCH /api/v1/steps/{stepId}/blocks/layout 담당이다. "
                    + "조회에서 받은 version 을 함께 보내야 하며, 그 사이 남이 먼저 저장했으면 409 다. "
                    + "409 를 받으면 재조회 / 덮어쓰기(overwrite: true)를 사용자에게 묻는다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(implementation = BlockUpdateRequest.class)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "BLOCK_TITLE_TOO_LONG / BLOCK_UPDATE_FIELD_REQUIRED / "
                            + "BLOCK_VERSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "STEP_EDIT_DENIED — 스텝 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "BLOCK_NOT_FOUND / USER_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "BLOCK_VERSION_CONFLICT — 다른 사용자가 먼저 수정함")
    })
    @PatchMapping("/{blockId}")
    public ResponseEntity<ApiResponse<BlockUpdateResponse>> updateBlock(
            @Parameter(description = "수정할 블록 ID")
            @PathVariable Long blockId,
            @RequestBody JsonNode requestBody,
            Authentication authentication
    ) {
        BlockUpdateResult result = blockCommandUseCase.updateBlock(
                toUpdateCommand(blockId, requestBody, authentication));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        BlockUpdateResponse.from(result)));
    }

    @Operation(summary = "블록 삭제",
            description = "블록을 논리 삭제한다. 하드 삭제 API 는 존재하지 않는다. "
                    + "타입별 상세 행도 같은 트랜잭션에서 함께 삭제된다. "
                    + "⛔ 삭제 잠금 4종은 폐기됐다(2026-08-09) — 막는 대신 옮길 수단을 준다. "
                    + "입금·계산서 연결 해제는 별도 작업으로 붙는다(BLK-013). "
                    + "📌 결재 블록은 상신 이후면 첫 요청을 409 로 되묻는다(DEL-016) — confirmApprovalCancel=true 로 "
                    + "다시 요청하면 삭제된다. 스텝 삭제(DELETE /api/v1/steps/{stepId})에는 이 확인이 없어 "
                    + "결재 상태와 무관하게 함께 삭제된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "STEP_EDIT_DENIED — 스텝 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "BLOCK_NOT_FOUND — 블록이 존재하지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "APPROVAL_DELETE_CONFIRM_REQUIRED — 상신 이후 결재 블록이라 확인이 필요하다. "
                            + "⚠️ 금지가 아니다 — confirmApprovalCancel=true 로 재요청하면 삭제된다. "
                            + "approval.status 가 IN_PROGRESS·REJECTED·COMPLETED 일 때 발생하고 "
                            + "message 는 상태별로 무엇을 잃는지 알려준다(취소됨 / 재상신 불가 / 승인 이력 열람 불가). "
                            + "분기는 code 로 하고 message 는 확인 다이얼로그에 그대로 노출한다")
    })
    @DeleteMapping("/{blockId}")
    public ResponseEntity<ApiResponse<Void>> deleteBlock(
            @Parameter(description = "삭제할 블록 ID")
            @PathVariable Long blockId,
            @Parameter(description = "결재 취소에 동의했는지. 409 를 받은 뒤 사용자가 확인하면 true 로 재요청한다")
            @RequestParam(defaultValue = "false") boolean confirmApprovalCancel,
            Authentication authentication
    ) {
        blockCommandUseCase.deleteBlock(new DeleteBlockCommand(
                blockId, authentication.getName(), RequesterRole.from(authentication),
                confirmApprovalCancel));

        return ResponseEntity.ok(ApiResponse.success(ProjectResponseMessage.SUCCESS));
    }

    @Operation(summary = "블록 이동",
            description = "블록을 같은 프로젝트의 다른 스텝으로 옮긴다(BLK-014). "
                    + "출발·도착 양쪽 스텝의 EDITOR 여야 한다. "
                    + "⚠️ 연결된 이슈가 있으면 issue_block 연결이 끊긴다 — 블록과 이슈는 같은 스텝이어야 하기 때문이다"
                    + "(BLK-009 · INV-06). 끊긴 수는 응답의 unlinkedIssueCount 로 내려간다. "
                    + "배치는 도착 스텝의 맨 아래 새 행에 붙는다 — 정렬은 배치 변경 API 로 조정한다. "
                    + "⛔ 다른 프로젝트로는 못 옮긴다. "
                    + "조회에서 받은 version 을 함께 보내야 하며, 그 사이 남이 먼저 저장했으면 409 다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "이동 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "BLOCK_MOVE_TARGET_REQUIRED / BLOCK_MOVE_TARGET_INVALID / "
                            + "BLOCK_VERSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "STEP_EDIT_DENIED — 출발 또는 도착 스텝의 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "BLOCK_NOT_FOUND / STEP_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "BLOCK_VERSION_CONFLICT — 다른 사용자가 먼저 수정함")
    })
    @PatchMapping("/{blockId}/step")
    public ResponseEntity<ApiResponse<BlockMoveResponse>> moveBlock(
            @Parameter(description = "옮길 블록 ID")
            @PathVariable Long blockId,
            @Valid @RequestBody BlockMoveRequest request,
            Authentication authentication
    ) {
        BlockMoveResult result = blockCommandUseCase.moveBlock(
                request.toCommand(blockId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(ProjectResponseMessage.SUCCESS,
                        BlockMoveResponse.from(result)));
    }

    /**
     * raw JSON 에서 필드 존재 여부(생략 vs null)를 직접 확인해 커맨드로 옮긴다.
     * record 로 받으면 "owner": null 과 생략이 둘 다 null 이 돼 담당자 해제를 표현할 수 없다.
     */
    private UpdateBlockCommand toUpdateCommand(Long blockId, JsonNode body,
                                               Authentication authentication) {
        return new UpdateBlockCommand(
                blockId,
                body.has("title"), textOrNull(body, "title"),
                body.has("owner"), textOrNull(body, "owner"),
                requireVersion(body), body.path("overwrite").asBoolean(false),
                authentication.getName(), RequesterRole.from(authentication));
    }

    /**
     * ⚠️ 여기서 400 으로 막지 않으면 <b>version 0 이 서비스로 흘러가 모든 저장이 409</b> 가 된다.
     * 이 경로는 {@code @Valid} 가 안 도는 JsonNode 바인딩이라 Bean Validation 이 대신 잡아주지 않는다
     * (`.ai/docs/global/CONCURRENCY.md` §6-3).
     */
    private int requireVersion(JsonNode body) {
        JsonNode value = body.get("version");
        if (value == null || !value.canConvertToInt()) {
            throw new ValidationException(BlockErrorCode.BLOCK_VERSION_REQUIRED);
        }
        return value.asInt();
    }

    private String textOrNull(JsonNode body, String field) {
        JsonNode value = body.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }
}