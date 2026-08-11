package com.group3.vitamins.project.presentation.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.group3.vitamins.project.application.result.ProjectPageResult;
import com.group3.vitamins.project.application.result.ProjectSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "프로젝트 목록 조회 응답")
public record ProjectListResponse(

        @Schema(description = "프로젝트 목록 (최근 생성순)")
        List<ProjectItemResponse> content,

        @Schema(description = "현재 페이지 (0-base)", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "20")
        int size,

        @Schema(description = "필터를 적용한 전체 건수", example = "1")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "1")
        int totalPages
) {

    /** 조회 결과를 응답으로 옮긴다. */
    public static ProjectListResponse from(ProjectPageResult result) {
        return new ProjectListResponse(
                result.content().stream().map(ProjectItemResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @Schema(description = "프로젝트 카드")
    public record ProjectItemResponse(

            @Schema(description = "프로젝트 ID", example = "12")
            Long projectId,

            @Schema(description = "과업명", example = "OO시 상수도 관리 용역")
            String name,

            @Schema(description = "발주처", example = "OO시청", nullable = true)
            String clientName,

            @Schema(description = "프로젝트 상태", example = "IN_PROGRESS")
            String status,

            @Schema(description = "시작일", example = "2026-08-01", nullable = true)
            LocalDate startedOn,

            @Schema(description = "종료일", example = "2026-12-31", nullable = true)
            LocalDate endedOn,

            @Schema(description = "계약금액", example = "120000000", nullable = true)
            BigDecimal contractAmount,

            @Schema(description = "진척률(%) = 완료 스텝 / 전체 스텝. 스텝이 0개면 응답에서 빠진다",
                    example = "40")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Integer progressRate,

            @Schema(description = "연결된 사업 카테고리")
            List<BusinessCategorySummaryResponse> businessCategories,

            @Schema(description = "참여자 (이름 오름차순). 카드 아바타용이라 permission=NONE 은 빠진다")
            List<MemberBriefResponse> members,

            @Schema(description = "이 프로젝트에서 내가 담당한 진행중 이슈 수", example = "3")
            int myIssueInProgressCount,

            @Schema(description = "이 프로젝트에서 내가 기안한 진행중 결재 수", example = "1")
            int myApprovalInProgressCount,

            @Schema(description = "낙관적 락 버전. 목록에서 바로 수정을 시작할 때 이 값을 그대로 실어 보낸다",
                    example = "7")
            int version
    ) {

        static ProjectItemResponse from(ProjectSummary summary) {
            List<BusinessCategorySummaryResponse> categories = summary.businessCategories().stream()
                    .map(c -> new BusinessCategorySummaryResponse(c.categoryId(), c.name(), c.code(), c.deleted()))
                    .toList();
            List<MemberBriefResponse> members = summary.members().stream()
                    .map(m -> new MemberBriefResponse(m.userId(), m.name(), m.deleted()))
                    .toList();

            return new ProjectItemResponse(
                    summary.projectId(), summary.name(), summary.clientName(), summary.status(),
                    summary.startedOn(), summary.endedOn(), summary.contractAmount(),
                    summary.progressRate(), categories, members,
                    summary.myIssueInProgressCount(), summary.myApprovalInProgressCount(),
                    summary.version());
        }
    }

    @Schema(description = "참여자 요약. 프로필 이미지는 스키마에 없어 FE 가 이름 이니셜로 그린다")
    public record MemberBriefResponse(

            @Schema(description = "사원 사번. 아바타 클릭 시 참여자 진입 키", example = "E2024001")
            String userId,

            @Schema(description = "이름. 사번이 사원 정보에 없으면 null", example = "김용준", nullable = true)
            String name,

            @Schema(description = "이 사원이 삭제됐는지 (배지 표시용). "
                    + "true 여도 name 은 그대로 내려간다", example = "false")
            boolean deleted
    ) {
    }
}