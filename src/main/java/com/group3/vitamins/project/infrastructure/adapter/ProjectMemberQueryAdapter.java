package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.application.port.ProjectMemberQueryPort;
import com.group3.vitamins.project.application.result.MemberSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProjectMemberQueryAdapter implements ProjectMemberQueryPort {

    private final ProjectMemberQueryMapper projectMemberQueryMapper;

    @Override
    public List<MemberSummary> findMembers(Long projectId) {
        return projectMemberQueryMapper.findMembers(projectId).stream()
                .map(row -> new MemberSummary(
                        row.memberId(), row.userId(), row.name(),
                        row.department(), row.permission(), row.resigned()))
                .toList();
    }
}