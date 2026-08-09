package com.group3.vitamins.project.presentation.api.response;

import com.group3.vitamins.project.application.result.ProjectResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "프로젝트 생성 응답")
public record ProjectCreateResponse(

        @Schema(description = "프로젝트 ID", example = "12")
        Long projectId,

        @Schema(description = "과업명", example = "OO시 상수도 관리 용역")
        String name,

        @Schema(description = "발주처", example = "OO시청")
        String clientName,

        @Schema(description = "프로젝트 상태", example = "NOT_STARTED")
        String status,

        @Schema(description = "시작일", example = "2026-08-01")
        LocalDate startedOn,

        @Schema(description = "종료일", example = "2026-12-31")
        LocalDate endedOn,

        @Schema(description = "계약금액", example = "120000000")
        BigDecimal contractAmount,

        @Schema(description = "연결된 사업 카테고리")
        List<BusinessCategorySummaryResponse> businessCategories,

        @Schema(description = "연결된 공고 ID. 공고 없이 생성했으면 null", example = "45", nullable = true)
        Long bidNoticeId,

        @Schema(description = "생성자")
        CreatedByResponse createdBy,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt
) {

    /** 생성 결과를 응답으로 옮긴다. */
    public static ProjectCreateResponse from(ProjectResult result) {
        List<BusinessCategorySummaryResponse> categories = result.businessCategories().stream()
                .map(c -> new BusinessCategorySummaryResponse(c.categoryId(), c.name(), c.code()))
                .toList();

        return new ProjectCreateResponse(
                result.projectId(), result.name(), result.clientName(), result.status(),
                result.startedOn(), result.endedOn(), result.contractAmount(), categories,
                result.bidNoticeId(),
                new CreatedByResponse(result.createdBy().userId(), result.createdBy().name()),
                result.createdAt());
    }

    @Schema(description = "생성자")
    public record CreatedByResponse(
            @Schema(description = "사번", example = "E2024001") String userId,
            @Schema(description = "이름", example = "김용준") String name
    ) {
    }
}