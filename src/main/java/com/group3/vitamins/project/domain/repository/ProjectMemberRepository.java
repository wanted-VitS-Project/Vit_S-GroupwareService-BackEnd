package com.group3.vitamins.project.domain.repository;

import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.domain.model.ProjectMember;

import java.util.Optional;

public interface ProjectMemberRepository {
    ProjectMember save(ProjectMember member);

    /** 요청자의 프로젝트 권한을 조회한다. 참여자 행이 없으면 비어 있다. */
    Optional<MemberPermission> findPermission(Long projectId, String userId);
}