    package com.group3.vitamins.project.block.presentation.api.request;

import com.group3.vitamins.project.block.application.command.CreateBlockCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "블록 생성 요청")
public record BlockCreateRequest(

        @Schema(description = "블록 타입. TEXT · IMAGE · FILE · CHECKLIST · PAYMENT_CONFIRM · "
                + "TAX_INVOICE_VIEW · PERFORMANCE_VIEW · APPROVAL · AI 중 하나. "
                + "BID_NOTICE 는 공고 전환 API 만 생성하므로 보내면 400 이다",
                example = "CHECKLIST", requiredMode = Schema.RequiredMode.REQUIRED)
        String type,

        @Schema(description = "블록 제목 (최대 200자). 생략하면 null 로 만들어지고 "
                + "PATCH /api/v1/blocks/{blockId} 로 나중에 채운다", example = "제출 서류 점검")
        String title,

        @Schema(description = "블록 담당자 사번. 생략 가능", example = "E2024001")
        String owner,

        @Schema(description = "행 인덱스. 생략하면 맨 아래 행", example = "0")
        Integer rowIndex,

        @Schema(description = "행 내 순서. 생략하면 그 행의 맨 뒤", example = "1")
        Integer sortOrder,

        @Schema(description = "열 병합 수 (1~3). 생략하면 1", example = "1")
        Integer colSpan
) {

    /** 요청을 커맨드로 옮긴다. 경로·세션에서 온 값은 여기서 합친다. */
    public CreateBlockCommand toCommand(Long stepId, String requesterUserId, String role) {
        return new CreateBlockCommand(
                stepId, type, title, owner, rowIndex, sortOrder, colSpan, requesterUserId, role);
    }
}