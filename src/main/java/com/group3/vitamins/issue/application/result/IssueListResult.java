package com.group3.vitamins.issue.application.result;

import java.util.List;

public record IssueListResult(
        List<IssueResult> issues
) {
}
