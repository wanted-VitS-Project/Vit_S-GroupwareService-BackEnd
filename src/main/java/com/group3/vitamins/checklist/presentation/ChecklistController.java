package com.group3.vitamins.checklist.presentation;

import com.group3.vitamins.checklist.application.command.CreateChecklistItemCommand;
import com.group3.vitamins.checklist.application.command.DeleteChecklistItemCommand;
import com.group3.vitamins.checklist.application.command.UpdateChecklistItemCommand;
import com.group3.vitamins.checklist.application.usecase.ChecklistCommandUseCase;
import com.group3.vitamins.checklist.application.usecase.ChecklistCommandUseCase.CreateChecklistItemView;
import com.group3.vitamins.checklist.application.usecase.ChecklistCommandUseCase.DeleteChecklistItemView;
import com.group3.vitamins.checklist.application.usecase.ChecklistCommandUseCase.UpdateChecklistItemView;
import com.group3.vitamins.checklist.presentation.api.request.ChecklistItemCreateRequest;
import com.group3.vitamins.checklist.presentation.api.request.ChecklistItemUpdateRequest;
import com.group3.vitamins.checklist.presentation.api.response.CreateChecklistItemResponse;
import com.group3.vitamins.checklist.presentation.api.response.DeleteChecklistItemResponse;
import com.group3.vitamins.checklist.presentation.api.response.UpdateChecklistItemResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 체크리스트 항목 API.
 *
 * <p>체크리스트 블록 자체의 생성·삭제는 이 컨트롤러에 없다 — 블록 생성은 Block 도메인(동훈님)이 전담하고,
 * 삭제 시 발행하는 이벤트를 리스너로 받아 이 쪽 항목들을 정리한다. 여기는 항목 생성·수정·삭제만 다룬다.
 */
@Tag(name = "Checklist", description = "체크리스트 블록 API")
@RestController
@RequestMapping("/api/v1/blocks/checklists")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistCommandUseCase checklistCommandUseCase;

    @Operation(summary = "체크리스트 항목 생성", description = "체크리스트 블록에 새 항목을 추가한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "체크리스트 항목 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "내용을 입력해 주세요. (CHK-004)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (CHK-001) / 초기 비밀번호를 먼저 변경해 주세요. (AUTH_PASSWORD_RESET_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 블록입니다. (CHK-002)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류입니다.")
    })
    @PostMapping("/{chkBlockId}/items")
    public ResponseEntity<ApiResponse<CreateChecklistItemResponse>> createItem(
            @Parameter(description = "체크리스트 항목을 생성할 블록의 ID", example = "1")
            @PathVariable Long chkBlockId,
            @RequestBody ChecklistItemCreateRequest request,
            Authentication authentication
    ) {
        CreateChecklistItemView view = checklistCommandUseCase.create(
                new CreateChecklistItemCommand(authentication.getName(), chkBlockId, request.content(),
                        RequesterRole.from(authentication)));

        CreateChecklistItemResponse data = new CreateChecklistItemResponse(
                view.chkBlockId(),
                view.chkId(),
                view.content(),
                view.completedCount(),
                view.totalCount(),
                view.createdAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("체크리스트 항목 생성 성공", data));
    }

    @Operation(summary = "체크리스트 항목 수정", description = "체크리스트 항목의 내용 또는 완료 상태를 수정한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "체크리스트 항목 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "내용을 입력해 주세요. (CHK-004) / 수정할 내용을 하나 이상 입력해 주세요. (CHK-005)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (CHK-001) / 초기 비밀번호를 먼저 변경해 주세요. (AUTH_PASSWORD_RESET_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 항목입니다. (CHK-003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류입니다.")
    })
    @PatchMapping("/items/{chkId}")
    public ResponseEntity<ApiResponse<UpdateChecklistItemResponse>> updateItem(
            @Parameter(description = "수정할 체크리스트 항목 ID", example = "1")
            @PathVariable Long chkId,
            @RequestBody ChecklistItemUpdateRequest request,
            Authentication authentication
    ) {
        UpdateChecklistItemView view = checklistCommandUseCase.update(
                new UpdateChecklistItemCommand(authentication.getName(), chkId, request.content(),
                        request.changeStatusTo(), RequesterRole.from(authentication)));

        UpdateChecklistItemResponse data = new UpdateChecklistItemResponse(
                view.chkId(),
                view.content(),
                view.isCompleted(),
                view.completedCount(),
                view.totalCount(),
                view.updatedAt()
        );

        return ResponseEntity.ok(ApiResponse.success("체크리스트 항목 수정 성공", data));
    }

    @Operation(summary = "체크리스트 항목 삭제", description = "체크리스트 항목을 삭제한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "체크리스트 항목 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (CHK-001) / 초기 비밀번호를 먼저 변경해 주세요. (AUTH_PASSWORD_RESET_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 항목입니다. (CHK-003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류입니다.")
    })
    @DeleteMapping("/items/{chkId}")
    public ResponseEntity<ApiResponse<DeleteChecklistItemResponse>> deleteItem(
            @Parameter(description = "삭제할 체크리스트 항목 ID", example = "1")
            @PathVariable Long chkId,
            Authentication authentication
    ) {
        DeleteChecklistItemView view = checklistCommandUseCase.delete(new DeleteChecklistItemCommand(
                authentication.getName(), chkId, RequesterRole.from(authentication)));

        DeleteChecklistItemResponse data = new DeleteChecklistItemResponse(view.completedCount(), view.totalCount());

        return ResponseEntity.ok(ApiResponse.success("체크리스트 항목 삭제 성공", data));
    }
}
