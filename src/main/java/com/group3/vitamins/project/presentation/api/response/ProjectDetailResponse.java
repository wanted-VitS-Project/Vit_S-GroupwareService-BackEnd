package com.group3.vitamins.project.presentation.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.group3.vitamins.project.application.result.ProjectDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "프로젝트 상세 조회 응답")
public record ProjectDetailResponse(

        @Schema(description = "프로젝트 ID", example = "12")
        Long projectId,

        @Schema(description = "과업명", example = "OO시 상수도 관리 용역")
        String name,

        @Schema(description = "설명", example = "상수도 관리 시스템 고도화 용역")
        String description,

        @Schema(description = "발주처", example = "OO시청")
        String clientName,

        @Schema(description = "프로젝트 상태", example = "IN_PROGRESS")
        String status,

        @Schema(description = "시작일", example = "2026-08-01")
        LocalDate startedOn,

        @Schema(description = "종료일", example = "2026-12-31")
        LocalDate endedOn,

        @Schema(description = "계약금액", example = "120000000")
        BigDecimal contractAmount,

        @Schema(description = "진척률(%) = 완료 스텝 / 전체 스텝. 스텝이 0개면 응답에서 빠진다", example = "40")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer progressRate,

        @Schema(description = "전체 스텝 수", example = "5")
        int stepCount,

        @Schema(description = "완료 스텝 수", example = "2")
        int doneStepCount,

        @Schema(description = "연결된 사업 카테고리")
        List<BusinessCategorySummaryResponse> businessCategories,

        @Schema(description = "연결된 공고 ID. 공고 없이 생성했으면 null", example = "null")
        Long bidNoticeId,

        @Schema(description = "종결 사유 코드. 종결 건만 값이 있다", example = "null")
        String closeReasonCode,

        @Schema(description = "종결 사유 상세", example = "null")
        String closeReasonNote,

        @Schema(description = "요청자의 프로젝트 권한. FE 편집 버튼 게이팅용", example = "EDITOR")
        String myPermission,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt
) {

    /** 조회 결과를 응답으로 옮긴다. */
    public static ProjectDetailResponse from(ProjectDetailResult result) {
        List<BusinessCategorySummaryResponse> categories = result.businessCategories().stream()
                .map(c -> new BusinessCategorySummaryResponse(c.categoryId(), c.name(), c.code()))
                .toList();

        return new ProjectDetailResponse(
                result.projectId(), result.name(), result.description(), result.clientName(),
                result.status(), result.startedOn(), result.endedOn(), result.contractAmount(),
                result.progressRate(), result.stepCount(), result.doneStepCount(), categories,
                result.bidNoticeId(), result.closeReasonCode(), result.closeReasonNote(),
                result.myPermission(), result.createdAt());
    }
}