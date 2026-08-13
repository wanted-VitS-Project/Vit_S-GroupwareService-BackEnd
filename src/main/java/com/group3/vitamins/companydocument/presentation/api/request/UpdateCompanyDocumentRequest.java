package com.group3.vitamins.companydocument.presentation.api.request;

import com.group3.vitamins.companydocument.application.command.UpdateCompanyDocumentCommand;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사내 문서 수정 요청(§4). name·category 는 보낸 것만 반영한다(둘 다 선택, 최소 1개 필요 — 검증은 서비스).
 */
@Schema(description = "사내 문서 표시명·카테고리 수정 요청")
public record UpdateCompanyDocumentRequest(
        @Schema(description = "새 표시명(최대 255자)", example = "2026년 재무제표(확정)", nullable = true)
        String name,

        @Schema(description = "새 카테고리 enum", example = "FINANCE", nullable = true)
        String category
) {

    public UpdateCompanyDocumentCommand toCommand(Long companyDocumentId, String requesterUserId, String role) {
        return new UpdateCompanyDocumentCommand(companyDocumentId, name, category, requesterUserId, role);
    }
}
