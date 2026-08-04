package com.group3.vitamins.text.presentation;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.text.application.command.UpdateTextContentCommand;
import com.group3.vitamins.text.application.usecase.TextCommandUseCase;
import com.group3.vitamins.text.application.usecase.TextCommandUseCase.UpdateTextContentView;
import com.group3.vitamins.text.presentation.api.request.TextUpdateRequest;
import com.group3.vitamins.text.presentation.api.response.UpdateTextContentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 텍스트 블록 API.
 *
 * <p>생성·삭제는 이 컨트롤러에 없다 — 블록 생성/삭제는 Block 도메인(동훈님)이 전부 처리하고,
 * 삭제 시 발행하는 이벤트를 리스너로 받아 이 쪽 데이터를 정리한다. 여기는 본문 수정만 남는다.
 */
@Tag(name = "Text", description = "텍스트 블록 API")
@RestController
@RequestMapping("/api/v1/blocks/texts")
@RequiredArgsConstructor
public class TextController {

    private final TextCommandUseCase textCommandUseCase;

    @Operation(summary = "텍스트 본문 수정", description = "텍스트 블록의 본문 전체를 사용자가 수정한 내용으로 교체한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "텍스트 본문 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "내용을 입력해 주세요. (TXT-003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (TXT-001) / 초기 비밀번호를 먼저 변경해 주세요. (AUTH_PASSWORD_RESET_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 블록입니다. (TXT-002)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류입니다.")
    })
    @PatchMapping("/{txtId}")
    public ResponseEntity<ApiResponse<UpdateTextContentResponse>> updateTextContent(
            @Parameter(description = "수정할 텍스트 항목 ID", example = "1")
            @PathVariable Long txtId,
            @RequestBody TextUpdateRequest request,
            @AuthenticationPrincipal String userId
    ) {
        UpdateTextContentView view = textCommandUseCase.updateContent(
                new UpdateTextContentCommand(userId, txtId, request.content()));
        UpdateTextContentResponse data =
                new UpdateTextContentResponse(view.txtId(), view.content(), view.updatedAt());

        return ResponseEntity.ok(ApiResponse.success("텍스트 본문 수정 성공", data));
    }
}
