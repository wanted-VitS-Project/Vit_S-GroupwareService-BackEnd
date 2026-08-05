package com.group3.vitamins.jobposition.domain.model;

/**
 * 직급 도메인 객체 (`.ai/api/job-position.md`).
 *
 * <p>순수 도메인이다 — JPA·Spring 에 의존하지 않는다. 권한 판정에는 쓰이지 않고(`POS-009`),
 * 소프트 삭제도 아니다(삭제 시 행을 제거한다).
 */
public class JobPosition {

    private final Long jobPositionId;
    private String name;
    private int sortOrder;

    private JobPosition(Long jobPositionId, String name, int sortOrder) {
        this.jobPositionId = jobPositionId;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    /** 새 직급을 만든다. 아직 저장되지 않았으므로 ID 가 없다. */
    public static JobPosition create(String name, int sortOrder) {
        return new JobPosition(null, name, sortOrder);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static JobPosition restore(Long jobPositionId, String name, int sortOrder) {
        return new JobPosition(jobPositionId, name, sortOrder);
    }

    /** 직급명을 바꾼다. */
    public void rename(String name) {
        this.name = name;
    }

    /** 정렬 순서를 바꾼다. */
    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getJobPositionId() {
        return jobPositionId;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
