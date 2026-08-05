package com.group3.vitamins.issue.application.port;

public interface IssueStepAccessPort {

    StepAccessView requireAccess(Long stepId, String requesterUserId, String role);


    record StepAccessView(Long stepId, Long projectId) {
    }
}
