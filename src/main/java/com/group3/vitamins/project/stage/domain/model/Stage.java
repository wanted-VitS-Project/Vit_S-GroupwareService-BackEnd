package com.group3.vitamins.project.stage.domain.model;

import java.time.LocalDateTime;

public class Stage {

    private static final int INITIAL_VERSION = 1;

    private final Long stageId;
    private final Long projectId;
    private String name;
    private int sortOrder;
    private final int version;
    private final LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    private Stage(Long stageId, Long projectId, String name, int sortOrder, int version,
                  LocalDateTime createdAt, LocalDateTime deletedAt) {
        this.stageId = stageId;
        this.projectId = projectId;
        this.name = name;
        this.sortOrder = sortOrder;
        this.version = version;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    /** 스테이지를 생성한다. 권한·상태를 갖지 않는 분류 전용 객체다 (STG-004 · INV-01). */
    public static Stage create(Long projectId, String name, int sortOrder, LocalDateTime now) {
        return new Stage(null, projectId, name, sortOrder, INITIAL_VERSION, now, null);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static Stage restore(Long stageId, Long projectId, String name, int sortOrder,
                                int version, LocalDateTime createdAt, LocalDateTime deletedAt) {
        return new Stage(stageId, projectId, name, sortOrder, version, createdAt, deletedAt);
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

    /**
     * 조회 시점의 낙관적 락 버전이다 (`.ai/docs/global/CONCURRENCY.md`).
     *
     * <p>⚠️ 도메인은 이 값을 <b>절대 올리지 않는다.</b> {@code +1} 은 {@code WHERE version = ?} 과
     * 같은 UPDATE 문장 안에서 DB 가 한다. 여기서 올리면 덮어쓰기가 쓰는 기대값이 DB+1 이 되어
     * <b>모든 덮어쓰기가 409</b> 가 된다.
     */
    public int getVersion() { return version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}
