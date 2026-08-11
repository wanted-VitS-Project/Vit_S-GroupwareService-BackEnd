package com.group3.vitamins.issue.application.command;

public record ChangeIssueStatusCommand(
        Long issueId,
        String status,
        Integer version,
        String requesterUserId,
        String role,
        boolean internal
) {

    public ChangeIssueStatusCommand(
            Long issueId, String status, Integer version, String requesterUserId, String role
    ) {
        this(issueId, status, version, requesterUserId, role, false);
    }

    /** Step 완료 같은 내부 연동은 현재 Issue 버전을 기대값으로 사용한다. */
    public ChangeIssueStatusCommand(Long issueId, String status, String requesterUserId, String role) {
        this(issueId, status, null, requesterUserId, role, true);
    }
}
