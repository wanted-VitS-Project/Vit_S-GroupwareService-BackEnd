package com.group3.vitamins.project.step.infrastructure.adapter;

import com.group3.vitamins.project.step.application.port.IssueStatLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IssueStatLookupAdapter implements IssueStatLookupPort {

    private final IssueStatQueryMapper issueStatQueryMapper;

    @Override
    public Map<Long, IssueStatView> countByStepIds(Collection<Long> stepIds) {
        if (stepIds.isEmpty()) {
            return Map.of();
        }
        return issueStatQueryMapper.countByStepIds(stepIds).stream()
                .collect(Collectors.toMap(
                        IssueStatRow::stepId,
                        row -> new IssueStatView(
                                row.totalCount(), row.doneCount(), row.inProgressCount())));
    }
}
