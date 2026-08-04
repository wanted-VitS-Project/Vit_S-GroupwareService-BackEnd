package com.group3.vitamins.project.domain.model;

import java.time.LocalDateTime;

public class ProjectMember {

    private final Long projectMemberId;
    private final Long projectId;
    private final String userId;
    private final MemberPermission permission;
    private final LocalDateTime createdAt;

    private ProjectMember(Long projectMemberId, Long projectId, String userId,
                          MemberPermission permission, LocalDateTime createdAt) {
        this.projectMemberId = projectMemberId;
        this.projectId = projectId;
        this.userId = userId;
        this.permission = permission;
        this.createdAt = createdAt;
    }

    /** 프로젝트 생성자를 EDITOR 참여자로 등록한다. */
    public static ProjectMember createEditor(Long projectId, String userId, LocalDateTime now) {
        return new ProjectMember(null, projectId, userId, MemberPermission.EDITOR, now);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static ProjectMember restore(Long projectMemberId, Long projectId, String userId,
                                        MemberPermission permission, LocalDateTime createdAt) {
        return new ProjectMember(projectMemberId, projectId, userId, permission, createdAt);
    }

    public Long getProjectMemberId() { return projectMemberId; }
    public Long getProjectId() { return projectId; }
    public String getUserId() { return userId; }
    public MemberPermission getPermission() { return permission; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}