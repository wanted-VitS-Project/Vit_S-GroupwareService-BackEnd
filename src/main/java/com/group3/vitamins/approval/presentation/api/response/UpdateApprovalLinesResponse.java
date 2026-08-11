package com.group3.vitamins.approval.presentation.api.response;

import com.group3.vitamins.approval.application.result.ApprovalLineView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record UpdateApprovalLinesResponse(
        @Schema(description = "치환된 결재선 전체 목록")
        List<LineItem> lines
) {

    @Schema(name = "UpdateApprovalLinesResponseLineItem")
    public record LineItem(
            @Schema(description = "결재선 구분 번호", example = "1")
            Long lineId,

            @Schema(description = "결재자 구분 번호(사번)", example = "EMP2024001")
            String approverId,

            @Schema(description = "결재자 이름(라이브 조회)", example = "홍길동")
            String approverName,

            @Schema(description = "결재자 직책(라이브 조회)", example = "과장")
            String approverPosition,

            @Schema(description = "결재자 부서(라이브 조회)", example = "기술본부 / 개발팀")
            String approverDepartment,

            @Schema(description = "결재 순서", example = "1")
            int order
    ) {
    }

    public static UpdateApprovalLinesResponse from(List<ApprovalLineView> views) {
        List<LineItem> items = views.stream()
                .map(v -> new LineItem(v.lineId(), v.approverId(), v.approverName(),
                        v.approverPosition(), v.approverDepartment(), v.order()))
                .toList();
        return new UpdateApprovalLinesResponse(items);
    }
}
