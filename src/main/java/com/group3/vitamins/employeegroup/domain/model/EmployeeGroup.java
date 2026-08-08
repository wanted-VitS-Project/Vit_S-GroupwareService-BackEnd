package com.group3.vitamins.employeegroup.domain.model;

/**
 * 사원 그룹 도메인 객체 (JPA·Spring 비의존). 팀 ERD 의 {@code employee_group} 애그리게이트.
 *
 * <p>그룹은 <b>권한이 아니라 선택용 인덱스</b>다 (`.ai/api/employee-group.md`). 그룹명은 전역 유니크,
 * 하드 삭제(구성원은 CASCADE)다. {@code createdBy} 는 생성자 사번이며 감사·목록 표시에만 쓴다.
 */
public class EmployeeGroup {

    private final Long groupId;
    private String name;
    private String description;
    private final String createdBy;

    private EmployeeGroup(Long groupId, String name, String description, String createdBy) {
        this.groupId = groupId;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
    }

    /** 새 그룹을 만든다. 아직 저장 전이라 ID 가 없다. */
    public static EmployeeGroup create(String name, String description, String createdBy) {
        return new EmployeeGroup(null, name, description, createdBy);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static EmployeeGroup restore(Long groupId, String name, String description, String createdBy) {
        return new EmployeeGroup(groupId, name, description, createdBy);
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
