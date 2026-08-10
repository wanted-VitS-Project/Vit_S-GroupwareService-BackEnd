package com.group3.vitamins.project.stage.domain.model;

import java.time.LocalDateTime;

public class Stage {

    private final Long stageId;
    private final Long projectId;
    private String name;
    private int sortOrder;
    private final LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    private Stage(Long stageId, Long projectId, String name, int sortOrder,
                  LocalDateTime createdAt, LocalDateTime deletedAt) {
        this.stageId = stageId;
        this.projectId = projectId;
        this.name = name;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    /** 스테이지를 생성한다. 권한·상태를 갖지 않는 분류 전용 객체다 (STG-004 · INV-01). */
    public static Stage create(Long projectId, String name, int sortOrder, LocalDateTime now) {
        return new Stage(null, projectId, name, sortOrder, now, null);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static Stage restore(Long stageId, Long projectId, String name, int sortOrder,
                               LocalDateTime createdAt, LocalDateTime deletedAt) {
        return new Stage(stageId, projectId, name, sortOrder, createdAt, deletedAt);
    }

    /** 이름만 바꾼다 (STG-001). 순서는 순서 변경 API 소관이다. */
    public Stage rename(String name) {
        this.name = name;
        return this;
    }

    /** 정렬 순서를 바꾼다 (STG-002). ⛔ 하위 스텝은 건드리지 않는다. */
    public Stage moveTo(int sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    /** 논리 삭제한다 (INV-05). 하위 스텝은 함께 삭제되지 않고 이전된다 (STG-003). */
    public Stage delete(LocalDateTime now) {
        this.deletedAt = now;
        return this;
    }

    public Long getStageId() { return stageId; }
    public Long getProjectId() { return projectId; }
    public String getName() { return name; }
    public int getSortOrder() { return sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}