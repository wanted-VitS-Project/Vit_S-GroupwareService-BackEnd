package com.group3.vitamins.employee.presentation.api.response;

import com.group3.vitamins.employee.application.result.BulkEmployeeRef;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사번+이름 최소 참조 (일괄 등록의 emailNotRegistered)")
public record BulkEmployeeRefResponse(
        @Schema(description = "사번") String userId,
        @Schema(description = "이름") String name
) {

    public static BulkEmployeeRefResponse from(BulkEmployeeRef r) {
        return new BulkEmployeeRefResponse(r.userId(), r.name());
    }
}
