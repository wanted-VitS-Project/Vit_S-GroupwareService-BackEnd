package com.group3.vitamins.employee.presentation.api.response;

import com.group3.vitamins.employee.application.result.BulkValidateResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "엑셀 일괄 등록 검증 결과 (§7). 오류가 있어도 200 이며 errors 에 행별 오류를 담는다.")
public record BulkValidateResponse(
        @Schema(description = "데이터 행 수") int totalRows,
        @Schema(description = "등록 가능한 행 수") int validCount,
        @Schema(description = "오류 행 수") int errorCount,
        @Schema(description = "행별 오류 목록") List<BulkRowErrorResponse> errors,
        @Schema(description = "이메일 없는(등록 가능) 행 수 — 초기 비밀번호를 못 받는다") int emailNotRegisteredCount,
        @Schema(description = "autoCreateMasters=true 일 때 등록 시 새로 생성될 전공/자격증(유효 행 기준). false 면 빈 배열 — 화면은 이걸 등록 전에 반드시 보여준다")
        BulkMastersResponse newMasters
) {

    public static BulkValidateResponse from(BulkValidateResult r) {
        return new BulkValidateResponse(
                r.totalRows(), r.validCount(), r.errorCount(),
                r.errors().stream().map(BulkRowErrorResponse::from).toList(),
                r.emailNotRegisteredCount(), BulkMastersResponse.from(r.newMasters()));
    }
}
