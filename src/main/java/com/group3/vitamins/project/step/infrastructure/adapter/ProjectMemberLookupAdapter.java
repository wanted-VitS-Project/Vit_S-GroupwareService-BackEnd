package com.group3.vitamins.project.step.infrastructure.adapter;

import com.group3.vitamins.project.application.query.MemberListQuery;
import com.group3.vitamins.project.application.result.MemberSummary;
import com.group3.vitamins.project.application.usecase.ProjectMemberQueryUseCase;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.port.ProjectMemberLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProjectMemberLookupAdapter implements ProjectMemberLookupPort {

    private final ProjectMemberQueryUseCase projectMemberQueryUseCase;

    @Override
    public List<Member> findMembers(Long projectId, String requesterUserId, String role) {
        return projectMemberQueryUseCase
                .getMembers(new MemberListQuery(projectId, requesterUserId, role))
                .stream()
                .map(this::toMember)
                .toList();
    }

    @Override
    public Optional<Member> findMember(Long projectId, String userId,
                                       String requesterUserId, String role) {
        return findMembers(projectId, requesterUserId, role).stream()
                .filter(member -> member.userId().equals(userId))
                .findFirst();
    }

    /** MemberSummary 의 permission 은 문자열이다. 값이 비어 있으면 미참여와 같게 NONE 으로 본다. */
    private Member toMember(MemberSummary summary) {
        MemberPermission permission = summary.permission() == null
                ? MemberPermission.NONE
                : MemberPermission.valueOf(summary.permission());

        return new Member(summary.userId(), summary.name(), permission);
    }
}
