package com.group3.vitamins.bidding.projectconversion.presentation.api.request;

import com.group3.vitamins.bidding.projectconversion.application.command.ConvertNoticeToProjectCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record ConvertNoticeToProjectRequest(

        @NotNull(message = "COMMON_INVALID_REQUEST|검토 ID를 입력해 주세요.")
        @Positive(message = "COMMON_INVALID_REQUEST|검토 ID가 올바르지 않습니다.")
        @Schema(description = "같은 공고·회사·요청자의 COMPLETED 문서 검토 ID", example = "71")
        Long reviewId,

        @Positive(message = "COMMON_INVALID_REQUEST|요약 ID가 올바르지 않습니다.")
        @Schema(description = "확정된 AI 요약 ID. 지정하면 bid_notice_summary.project_id에 연결",
                example = "12", nullable = true)
        Long summaryId,

        @NotBlank(message = "COMMON_INVALID_REQUEST|프로젝트명을 입력해 주세요.")
        @Schema(description = "프로젝트명", example = "스마트시티 통합관제 플랫폼 구축")
        String name,

        @Schema(description = "설명", example = "2026년 서울시 스마트시티 통합관제 플랫폼 구축 사업", nullable = true)
        String description,

        @NotNull(message = "COMMON_INVALID_REQUEST|사업 카테고리를 선택해 주세요.")
        @Positive(message = "COMMON_INVALID_REQUEST|사업 카테고리 ID가 올바르지 않습니다.")
        @Schema(description = "사업 카테고리 ID", example = "3")
        Long businessCategoryId,

        @NotNull(message = "COMMON_INVALID_REQUEST|시작일을 입력해 주세요.")
        @Schema(description = "시작일", example = "2026-09-01")
        LocalDate startedOn,

        @NotNull(message = "COMMON_INVALID_REQUEST|종료일을 입력해 주세요.")
        @Schema(description = "종료일", example = "2027-02-28")
        LocalDate endedOn,

        @Schema(description = "추가 참여자 user ID 목록. 전환 요청자는 서버가 자동 포함",
                example = "[\"EMP002\", \"EMP003\"]", nullable = true)
        List<@NotBlank(message = "COMMON_INVALID_REQUEST|참여자 user ID가 올바르지 않습니다.") String> memberIds
) {

    public ConvertNoticeToProjectCommand toCommand(Long noticeId, String requesterUserId, String role) {
        return new ConvertNoticeToProjectCommand(
                noticeId, reviewId, summaryId, name, description,
                businessCategoryId, startedOn, endedOn, memberIds,
                requesterUserId, role
        );
    }
}
