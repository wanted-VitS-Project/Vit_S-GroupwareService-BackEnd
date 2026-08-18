package com.group3.vitamins.employee.presentation.api.response;

import com.group3.vitamins.employee.application.result.BulkRegisterResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "엑셀 일괄 등록 결과 (§8). 화면 ③ 카드 4개 = totalRows·registeredCount·failedCount·emailNotRegistered.length.")
public record BulkRegisterResponse(
        @Schema(description = "요청 건수") int totalRows,
        @Schema(description = "등록 성공 건수") int registeredCount,
        @Schema(description = "실패 건수(검증 오류 + 등록 실패)") int failedCount,
        @Schema(description = "행별 오류(검증 오류 + 등록 실패)") List<BulkRowErrorResponse> errors,
        @Schema(description = "초기 비밀번호 메일 발송 성공 건수") int emailSentCount,
        @Schema(description = "등록됐지만 이메일이 없는 사원") List<BulkEmployeeRefResponse> emailNotRegistered,
        @Schema(description = "autoCreateMasters=true 로 이번 등록에서 새로 만든(또는 동명 매칭한) 전공/자격증. false 면 빈 배열")
        BulkMastersResponse createdMasters
) {

    public static BulkRegisterResponse from(BulkRegisterResult r) {
        return new BulkRegisterResponse(
                r.totalRows(), r.registeredCount(), r.failedCount(),
                r.errors().stream().map(BulkRowErrorResponse::from).toList(),
                r.emailSentCount(),
                r.emailNotRegistered().stream().map(BulkEmployeeRefResponse::from).toList(),
                BulkMastersResponse.from(r.createdMasters()));
    }
}
