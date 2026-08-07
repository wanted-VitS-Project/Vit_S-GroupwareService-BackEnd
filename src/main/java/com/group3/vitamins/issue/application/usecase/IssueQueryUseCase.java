package com.group3.vitamins.issue.application.usecase;

import com.group3.vitamins.issue.application.query.IssueCalendarQuery;
import com.group3.vitamins.issue.application.query.IssueDetailQuery;
import com.group3.vitamins.issue.application.query.IssueListQuery;
import com.group3.vitamins.issue.application.result.IssueCalendarResult;
import com.group3.vitamins.issue.application.result.IssueListResult;
import com.group3.vitamins.issue.application.result.IssueResult;

public interface IssueQueryUseCase {

    IssueListResult getIssues(IssueListQuery query);

    IssueResult getIssue(IssueDetailQuery query);

    IssueCalendarResult getMyCalendarIssues(IssueCalendarQuery query);
}
