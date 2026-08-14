package com.group3.vitamins.project.presentation.api.response;

import com.group3.vitamins.project.application.result.ProjectDuplicateResult;
import com.group3.vitamins.project.application.result.ProjectResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "프로젝트 복제 응답 — 생성 응답에 copied·skipped 가 더 붙는다")
public record ProjectDuplicateResponse(

        @Schema(description = "복제본 프로젝트 ID", example = "31")
        Long projectId,

        @Schema(description = "원본 프로젝트 ID", example = "12")
        Long sourceProjectId,

        @Schema(description = "과업명", example = "OO시 상수도 관리 용역 (2차)")
        String name,

        @Schema(description = "발주처", example = "OO시청")
        String clientName,

        @Schema(description = "프로젝트 상태 (NOT_STARTED 고정)", example = "NOT_STARTED")
        String status,

        @Schema(description = "시작일", example = "2027-01-01")
        LocalDate startedOn,

        @Schema(description = "종료일", example = "2027-06-30")
        LocalDate endedOn,

        @Schema(description = "계약금액", example = "120000000")
        BigDecimal contractAmount,

        @Schema(description = "연결된 사업 카테고리")
        List<BusinessCategorySummaryResponse> businessCategories,

        @Schema(description = "연결된 공고 ID. 안 보냈으면 null", example = "45", nullable = true)
        Long bidNoticeId,

        @Schema(description = "복사된 개수")
        CopiedResponse copied,

        @Schema(description = "건너뛴 개수")
        SkippedResponse skipped,

        @Schema(description = "생성자")
        ProjectCreateResponse.CreatedByResponse createdBy,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt
) {

    /** 복제 결과를 응답으로 옮긴다. */
    public static ProjectDuplicateResponse from(ProjectDuplicateResult result) {
        ProjectResult project = result.project();

        List<BusinessCategorySummaryResponse> categories = project.businessCategories().stream()
                .map(c -> new BusinessCategorySummaryResponse(c.categoryId(), c.name(), c.code(), c.deleted()))
                .toList();

        return new ProjectDuplicateResponse(
                project.projectId(), result.sourceProjectId(), project.name(), project.clientName(),
                project.status(), project.startedOn(), project.endedOn(), project.contractAmount(),
                categories, project.bidNoticeId(),
                new CopiedResponse(result.copied().stages(), result.copied().steps(),
                        result.copied().blocks()),
                new SkippedResponse(result.skipped().blocks()),
                new ProjectCreateResponse.CreatedByResponse(
                        project.createdBy().userId(), project.createdBy().name()),
                project.createdAt());
    }

    @Schema(description = "복사된 개수")
    public record CopiedResponse(
            @Schema(description = "복사된 스테이지 수", example = "3") int stages,
            @Schema(description = "복사된 스텝 수", example = "12") int steps,
            @Schema(description = "복사된 블록 수", example = "40") int blocks
    ) {
    }

    @Schema(description = "건너뛴 개수")
    public record SkippedResponse(
            @Schema(description = "건너뛴 블록 수 (BID_NOTICE 타입은 공고 전환 API 만 만든다)", example = "0")
            int blocks
    ) {
    }
}
