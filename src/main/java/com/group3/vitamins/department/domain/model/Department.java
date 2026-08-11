package com.group3.vitamins.department.domain.model;

/**
 * 부서 도메인 객체 (JPA·Spring 비의존). 팀 ERD 의 {@code department} 애그리게이트.
 *
 * <p>계층은 <b>최대 2단</b>이다. {@code parentId} 가 {@code null} 이면 최상위 부서다.
 * self FK 를 참조 대신 {@code parentId} 원시값으로 두는 이유 — 명세가 부서를 {@code parentId} 로만
 * 다루고(트리 조립은 조회에서), 지연로딩·프록시가 필요 없다.
 */
public class Department {

    private final Long departmentId;
    private final Long companyId;
    private String name;
    private final Long parentId;

    private Department(Long departmentId, Long companyId, String name, Long parentId) {
        this.departmentId = departmentId;
        this.companyId = companyId;
        this.name = name;
        this.parentId = parentId;
    }

    /** 새 부서를 만든다. 아직 저장되지 않았으므로 ID 가 없다. {@code parentId} 가 {@code null} 이면 최상위. */
    public static Department create(String name, Long parentId, Long companyId) {
        return new Department(null, companyId, name, parentId);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static Department restore(Long departmentId, Long companyId, String name, Long parentId) {
        return new Department(departmentId, companyId, name, parentId);
    }

    /** 부서명 수정 — 상위 부서는 바꾸지 않는다 (부서 이동 기능 없음, `.ai/api/department.md` §3). */
    public void rename(String name) {
        this.name = name;
    }

    /** 최상위 부서 여부. */
    public boolean isRoot() {
        return parentId == null;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public Long getParentId() {
        return parentId;
    }
}
