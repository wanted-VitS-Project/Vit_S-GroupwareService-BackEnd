package com.group3.vitamins.issue.application.usecase;

import com.group3.vitamins.issue.application.query.IssueListQuery;
import com.group3.vitamins.issue.application.result.IssueListResult;

public interface IssueQueryUseCase {

    IssueListResult getIssues(IssueListQuery query);
}
