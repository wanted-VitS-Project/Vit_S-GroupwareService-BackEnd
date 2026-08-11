package com.group3.vitamins.project.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.project.application.port.ProjectMemberQueryPort;
import com.group3.vitamins.project.application.query.MemberListQuery;
import com.group3.vitamins.project.application.result.MemberSummary;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.application.usecase.ProjectMemberQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectMemberQueryService implements ProjectMemberQueryUseCase {

    private final ProjectAccessUseCase projectAccessUseCase;
    private final ProjectMemberQueryPort projectMemberQueryPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    /** 프로젝트 접근 권한을 확인하고 참여자 전원을 돌려준다 (permission=NONE 도 포함). */
    @Override
    public List<MemberSummary> getMembers(MemberListQuery query) {
        projectAccessUseCase.requireAccess(
                query.projectId(), query.requesterUserId(), query.role());

        return projectMemberQueryPort.findMembers(
                query.projectId(), currentCompanyIdProvider.currentCompanyId());
    }
}