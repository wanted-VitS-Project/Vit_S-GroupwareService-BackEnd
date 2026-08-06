package com.group3.vitamins.issue.application.usecase;

import com.group3.vitamins.issue.application.command.CreateIssueCommand;
import com.group3.vitamins.issue.application.command.ChangeIssueStatusCommand;
import com.group3.vitamins.issue.application.command.DeleteIssueCommand;
import com.group3.vitamins.issue.application.result.IssueResult;
import com.group3.vitamins.issue.application.result.IssueStatusResult;

public interface IssueCommandUseCase {

    IssueResult createIssue(CreateIssueCommand command);

    IssueStatusResult changeIssueStatus(ChangeIssueStatusCommand command);

    void deleteIssue(DeleteIssueCommand command);
}
