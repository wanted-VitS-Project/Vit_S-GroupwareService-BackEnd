package com.group3.vitamins.approval.presentation.api.response;

import com.group3.vitamins.approval.application.result.ApprovalListItemResult;
import com.group3.vitamins.approval.application.result.ApprovalListPageResult;
import com.group3.vitamins.approval.application.result.ApprovalLinePreviewResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "결재관리 목록 응답")
public record ApprovalListResponse(

        @Schema(description = "결재 목록")
        List<Item> content,

        @Schema(description = "전체 결재 수", example = "1")
        int totalElements,

        @Schema(description = "전체 페이지 수", example = "1")
        int totalPages
) {

    public record Item(
            @Schema(description = "결재 구분 번호", example = "55")
            Long approvalId,

            @Schema(description = "현재 회차 제목", example = "스마트시티 구축사업 제안서 최종 검토 결재")
            String title,

            @Schema(description = "결재 상태", example = "IN_PROGRESS")
            String status,

            @Schema(description = "현재 회차 번호", example = "3")
            int currentRevisionNo,

            @Schema(description = "기안자 구분 번호(사번)", example = "EMP2024001")
            String drafterId,

            @Schema(description = "기안자 이름", example = "김민준")
            String drafterName,

            @Schema(description = "현재 ACTIVE 결재자 구분 번호(사번)", example = "EMP2024002")
            String currentApproverId,

            @Schema(description = "현재 ACTIVE 결재자 이름", example = "박지훈")
            String currentApproverName,

            @Schema(description = "소속 프로젝트 구분 번호", example = "17")
            Long projectId,

            @Schema(description = "소속 프로젝트명", example = "스마트시티 구축사업")
            String projectName,

            @Schema(description = "소속 스텝 구분 번호", example = "30")
            Long stepId,

            @Schema(description = "소속 스텝명", example = "Step 03 제안서 작성")
            String stepName,

            @Schema(description = "결재선 전체 미리보기(아바타 표시용)")
            List<Line> lines,

            @Schema(description = "생성 일시", example = "2026-07-28T08:00:00")
            LocalDateTime createdAt,

            @Schema(description = "상신 일시", example = "2026-07-28T14:20:00")
            LocalDateTime submittedAt,

            @Schema(description = "완료 일시", example = "null")
            LocalDateTime completedAt
    ) {

        public static Item from(ApprovalListItemResult r) {
            return new Item(r.approvalId(), r.title(), r.status(), r.currentRevisionNo(),
                    r.drafterId(), r.drafterName(), r.currentApproverId(), r.currentApproverName(),
                    r.projectId(), r.projectName(), r.stepId(), r.stepName(),
                    r.lines().stream().map(Line::from).toList(),
                    r.createdAt(), r.submittedAt(), r.completedAt());
        }
    }

    public record Line(
            @Schema(description = "결재자 구분 번호(사번)", example = "EMP2024002")
            String approverId,

            @Schema(description = "결재자 이름", example = "박지훈")
            String approverName,

            @Schema(description = "결재 순서", example = "1")
            int order,

            @Schema(description = "결재 단계 상태", example = "APPROVED")
            String status
    ) {

        public static Line from(ApprovalLinePreviewResult r) {
            return new Line(r.approverId(), r.approverName(), r.order(), r.status());
        }
    }

    public static ApprovalListResponse from(ApprovalListPageResult result) {
        return new ApprovalListResponse(
                result.content().stream().map(Item::from).toList(),
                clampToInt(result.totalElements()),
                result.totalPages());
    }

    /** `NotificationListResponse`와 동일 이유(명세상 totalElements 타입이 int) — 오버플로 대신 최댓값으로 자른다. */
    private static int clampToInt(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }
}
