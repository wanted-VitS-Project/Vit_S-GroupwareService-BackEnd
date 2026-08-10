package com.group3.vitamins.project.step.infrastructure.adapter;

import com.group3.vitamins.issue.application.command.ChangeIssueStatusCommand;
import com.group3.vitamins.issue.application.usecase.IssueCommandUseCase;
import com.group3.vitamins.project.step.application.port.IssueCloseCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueCloseCommandAdapter implements IssueCloseCommandPort {

    private static final String DONE = "DONE";

    private final IssueCommandUseCase issueCommandUseCase;

    @Override
    public void close(Long issueId, String requesterUserId, String role) {
        issueCommandUseCase.changeIssueStatus(
                new ChangeIssueStatusCommand(issueId, DONE, requesterUserId, role));
    }
}
