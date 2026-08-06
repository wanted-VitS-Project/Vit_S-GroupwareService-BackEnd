package com.group3.vitamins.issue.application.port;

public interface IssueStepAccessPort {

    StepAccessView requireAccess(Long stepId, String requesterUserId, String role);

    StepAccessView requireIssueAccess(Long stepId, String requesterUserId, String role);

    StepAccessView requireEditable(Long stepId, String requesterUserId, String role);

    record StepAccessView(Long stepId, Long projectId) {
    }
}
