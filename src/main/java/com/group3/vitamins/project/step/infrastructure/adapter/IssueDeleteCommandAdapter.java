package com.group3.vitamins.project.step.infrastructure.adapter;

import com.group3.vitamins.issue.application.command.DeleteIssueCommand;
import com.group3.vitamins.issue.application.usecase.IssueCommandUseCase;
import com.group3.vitamins.project.step.application.port.IssueDeleteCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueDeleteCommandAdapter implements IssueDeleteCommandPort {

    private final IssueCommandUseCase issueCommandUseCase;

    @Override
    public void delete(Long issueId, String requesterUserId, String role) {
        issueCommandUseCase.deleteIssue(
                new DeleteIssueCommand(issueId, requesterUserId, role));
    }
}
