package com.group3.vitamins.issue.application.port;

public interface IssueStepAccessPort {

    StepAccessView requireAccess(Long stepId, String requesterUserId, String role);

    StepAccessView requireIssueAccess(Long stepId, String requesterUserId, String role);

    StepAccessView requireEditable(Long stepId, String requesterUserId, String role);

    /** 프로젝트 단위 이슈 조회용 — Step을 거치지 않고 프로젝트 권한(VIEWER 이상)만 확인한다. 없으면 404, 접근 권한 없으면 403. */
    void requireProjectAccess(Long projectId, String requesterUserId, String role);

    record StepAccessView(Long stepId, Long projectId) {
    }
}
