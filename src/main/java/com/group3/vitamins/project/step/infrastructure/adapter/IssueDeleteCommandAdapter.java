package com.group3.vitamins.project.step.infrastructure.adapter;

import com.group3.vitamins.issue.application.usecase.IssueCascadeUseCase;
import com.group3.vitamins.project.step.application.port.IssueDeleteCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class IssueDeleteCommandAdapter implements IssueDeleteCommandPort {

    private final IssueCascadeUseCase issueCascadeUseCase;

    @Override
    public void delete(Collection<Long> issueIds) {
        issueCascadeUseCase.deleteIssues(issueIds);
    }
}
