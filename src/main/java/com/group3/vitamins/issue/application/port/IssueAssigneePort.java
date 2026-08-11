package com.group3.vitamins.issue.application.port;

import java.time.LocalDate;
import java.util.List;

public interface IssueAssigneePort {

    List<AssigneeView> validateAssignable(Long projectId, List<String> userIds);

    record AssigneeView(String userId, String name, LocalDate resignedAt) {
    }
}
