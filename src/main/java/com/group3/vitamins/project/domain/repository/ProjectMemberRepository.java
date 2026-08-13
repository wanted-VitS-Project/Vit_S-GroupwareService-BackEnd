package com.group3.vitamins.project.domain.repository;

import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.domain.model.ProjectMember;

import java.util.Optional;

public interface ProjectMemberRepository {

    ProjectMember save(ProjectMember member);

    /** 요청자의 프로젝트 권한을 조회한다. 참여자 행이 없으면 비어 있다. */
    Optional<MemberPermission> findPermission(Long projectId, String userId);

    Optional<ProjectMember> findById(Long projectMemberId);

    void deleteById(Long projectMemberId);

    /**
     * 프로젝트 삭제 시 참여자 행을 전부 지운다 (PRJ-014).
     *
     * <p>⚠️ <b>D-3 위반이 아니다</b> ({@code .ai/docs/global/DELETE.md} §2-2 예외).
     * 프로젝트는 복구가 없어(§6-5) 재초대 대상이 영원히 없고, 조회는 프로젝트를
     * {@code deleted_at IS NULL} 로 이미 거른다.
     */
    void deleteByProjectId(Long projectId);
}