package com.group3.vitamins.employee.presentation.api.response;

import com.group3.vitamins.employee.application.result.PendingMaster;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "엑셀 일괄 등록에서 자동 생성 대상(§7 newMasters) 또는 생성된(§8 createdMasters) 전공/자격증 한 건")
public record BulkMasterPreviewResponse(
        @Schema(description = "전공명 또는 자격증명", example = "정보처리기사") String name,
        @Schema(description = "그 이름을 참조하는 유효 행 수", example = "21") int rowCount
) {
    public static BulkMasterPreviewResponse from(PendingMaster m) {
        return new BulkMasterPreviewResponse(m.name(), m.rowCount());
    }
}
