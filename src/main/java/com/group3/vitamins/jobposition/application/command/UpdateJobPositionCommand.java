package com.group3.vitamins.jobposition.application.command;

/**
 * 직급 수정 커맨드. 보낸 필드만 바꾼다 (`job-position.md` §3).
 *
 * <p>{@code *Provided} 플래그로 "생략 vs 값 전달" 을 구분한다 — 필드가 하나도 없으면 400.
 */
public record UpdateJobPositionCommand(
        Long jobPositionId,
        boolean nameProvided,
        String name,
        boolean sortOrderProvided,
        Integer sortOrder,
        String role
) {
}
