package com.group3.vitamins.employee.presentation.api.response;

import com.group3.vitamins.employee.application.result.BulkRowError;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "엑셀 일괄 등록 행 오류 (§7·§8)")
public record BulkRowErrorResponse(
        @Schema(description = "엑셀 행 번호(1-base)") int row,
        @Schema(description = "사번 (누락 행이면 null)") String userId,
        @Schema(description = "이름 (누락 행이면 null)") String name,
        @Schema(description = "검증 코드 (REQUIRED_COLUMN·USER_ID_DUPLICATED·DEPARTMENT_NOT_FOUND·ADMIN_ROLE_NOT_ALLOWED)")
        String validation,
        @Schema(description = "사람이 읽는 설명") String message
) {

    public static BulkRowErrorResponse from(BulkRowError e) {
        return new BulkRowErrorResponse(e.row(), e.userId(), e.name(), e.validation().name(), e.message());
    }
}
