package com.group3.vitamins.jobposition.presentation.api.response;

import com.group3.vitamins.jobposition.application.result.JobPositionResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 직급 목록 응답 (`.ai/api/job-position.md` §1).
 *
 * <p>⛔ 페이징이 없다 — 회사당 수십 개 수준이라 전건을 {@code content} 로 내린다.
 */
@Schema(name = "JobPositionList", description = "직급 목록 (정렬 순서 오름차순)")
public record JobPositionListResponse(

        @Schema(description = "직급 목록")
        List<JobPositionResponse> content
) {

    /** 조회 결과 목록을 응답 봉투의 data 로 감싼다. */
    public static JobPositionListResponse from(List<JobPositionResult> results) {
        return new JobPositionListResponse(
                results.stream()
                        .map(JobPositionResponse::from)
                        .toList());
    }
}
