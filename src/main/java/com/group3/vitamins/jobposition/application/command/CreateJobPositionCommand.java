package com.group3.vitamins.jobposition.application.command;

/**
 * 직급 생성 커맨드. {@code sortOrder} 가 null 이면 마지막 순서 뒤에 붙인다 (`job-position.md` §2).
 * {@code role} 은 세션에서 온 값이라 요청 바디에 없다.
 */
public record CreateJobPositionCommand(
        String name,
        Integer sortOrder,
        String role
) {
}
