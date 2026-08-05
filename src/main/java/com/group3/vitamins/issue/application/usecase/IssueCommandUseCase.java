package com.group3.vitamins.issue.application.usecase;

import com.group3.vitamins.issue.application.command.CreateIssueCommand;
import com.group3.vitamins.issue.application.command.DeleteIssueCommand;
import com.group3.vitamins.issue.application.result.IssueResult;

public interface IssueCommandUseCase {

    IssueResult createIssue(CreateIssueCommand command);

    void deleteIssue(DeleteIssueCommand command);
}
