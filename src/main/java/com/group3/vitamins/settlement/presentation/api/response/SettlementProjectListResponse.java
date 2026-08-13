package com.group3.vitamins.settlement.presentation.api.response;

import com.group3.vitamins.settlement.application.usecase.SettlementQueryUseCase.SettlementProjectListView;
import com.group3.vitamins.settlement.application.usecase.SettlementQueryUseCase.SettlementProjectView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record SettlementProjectListResponse(
        @Schema(description = "현재 페이지 번호 (0-base)", example = "0")
        int page,

        @Schema(description = "페이지당 개수", example = "20")
        int size,

        @Schema(description = "전체 항목 수", example = "1")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "1")
        int totalPages,

        @Schema(description = "프로젝트별 정산 현황 목록 (현재 페이지분만)")
        List<SettlementProjectItem> projects
) {

    public static SettlementProjectListResponse from(SettlementProjectListView view) {
        return new SettlementProjectListResponse(
                view.page(), view.size(), view.totalElements(), view.totalPages(),
                view.projects().stream().map(SettlementProjectItem::from).toList()
        );
    }

    public record SettlementProjectItem(
            @Schema(description = "프로젝트 ID", example = "1")
            Long projectId,

            @Schema(description = "과업명", example = "한강 생태교육 환경개선사업")
            String projectName,

            @Schema(description = "발주처", example = "환경부", nullable = true)
            String clientName,

            @Schema(description = "담당자(프로젝트 제작자)", example = "김민준")
            String projectManager,

            @Schema(description = "총 계약금액 — INCOME 정산 블록의 total_amount. INCOME 정산 블록이 "
                    + "하나도 없으면 null", example = "450000000", nullable = true)
            Long totalPlannedAmount,

            @Schema(description = "총 비용 — OUTCOME 정산 블록 실입금(actual_amount) 합계", example = "0")
            Long totalOutcome,

            @Schema(description = "총 수입 — INCOME 정산 블록 실입금(actual_amount) 합계", example = "270000000")
            Long totalIncome,

            @Schema(description = "총 합계 (totalIncome - totalOutcome)", example = "270000000")
            Long totalAmount,

            @Schema(description = "완료된 회차 수", example = "1")
            Integer completedRoundCount,

            @Schema(description = "전체 회차 수", example = "3")
            Integer totalRoundCount,

            @Schema(description = "다음 정산 예정일 — 미완료 회차 중 회차 번호가 가장 낮은 것의 예정일. "
                    + "미완료 회차가 없으면 null", example = "2026-09-10", nullable = true)
            LocalDate nextPlannedDate,

            @Schema(description = "대표 상태 문구. 지금은 \"정산완료\"(모든 회차 완료) 또는 \"미연결 N건\" "
                    + "(미연결 회차 수) 둘 중 하나만 나간다 — 추후 세분화될 수 있다", example = "미연결 2건")
            String settlementStatusSummary,

            @Schema(description = "프로젝트 상태", example = "IN_PROGRESS")
            String projectStatus,

            @Schema(description = "프로젝트 종료일. 종료되지 않았으면 null", example = "2026-09-10", nullable = true)
            LocalDate endedOn
    ) {

        public static SettlementProjectItem from(SettlementProjectView view) {
            return new SettlementProjectItem(
                    view.projectId(),
                    view.projectName(),
                    view.clientName(),
                    view.projectManager(),
                    view.totalPlannedAmount(),
                    view.totalOutcome(),
                    view.totalIncome(),
                    view.totalAmount(),
                    view.completedRoundCount(),
                    view.totalRoundCount(),
                    view.nextPlannedDate(),
                    view.settlementStatusSummary(),
                    view.projectStatus(),
                    view.endedOn()
            );
        }
    }
}
