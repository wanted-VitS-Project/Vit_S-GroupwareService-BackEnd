package com.group3.vitamins.employee.domain.model;

import java.time.LocalDate;

/**
 * 사원 도메인 객체 (JPA·Spring 비의존). 팀 ERD 의 {@code employee} 애그리게이트.
 *
 * <p>PK 는 사번({@code userId})이다 — 자동 증가가 아니라 등록 시 부여받는 값이고, 사람을 가리키는 모든 FK 가
 * 이 값을 참조한다 (`.ai/local` 도메인 규칙). 계정은 별도 애그리게이트라 여기 없다 — 등록은 한 트랜잭션에서
 * 사원과 계정을 각각 INSERT 하며, 계정 쓰기는 {@code AccountProvisioningPort} 가 맡는다.
 *
 * <p>등록으로 만드는 사원은 항상 실제 사람이라 {@code isSystem=false} 다. 시스템 계정(ADMIN 가상 사원)은
 * 이 경로로 만들지 않는다.
 */
public class Employee {

    private final String userId;
    private final String name;
    private final boolean system;
    private final Long departmentId;
    private final Long jobPositionId;
    private final String email;
    private final String phone;
    private final LocalDate hiredAt;
    private final LocalDate resignedAt;

    private Employee(String userId, String name, boolean system, Long departmentId, Long jobPositionId,
                     String email, String phone, LocalDate hiredAt, LocalDate resignedAt) {
        this.userId = userId;
        this.name = name;
        this.system = system;
        this.departmentId = departmentId;
        this.jobPositionId = jobPositionId;
        this.email = email;
        this.phone = phone;
        this.hiredAt = hiredAt;
        this.resignedAt = resignedAt;
    }

    /**
     * 새 사원을 등록한다 (`employee.md` §3). 실제 사람이므로 {@code isSystem=false}, 재직 상태({@code resignedAt=null}).
     * 계정은 이 객체가 아니라 등록 유스케이스가 함께 발급한다.
     */
    public static Employee register(String userId, String name, Long departmentId, Long jobPositionId,
                                    String email, String phone, LocalDate hiredAt) {
        return new Employee(userId, name, false, departmentId, jobPositionId, email, phone, hiredAt, null);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static Employee restore(String userId, String name, boolean system, Long departmentId, Long jobPositionId,
                                   String email, String phone, LocalDate hiredAt, LocalDate resignedAt) {
        return new Employee(userId, name, system, departmentId, jobPositionId, email, phone, hiredAt, resignedAt);
    }

    public boolean isSystem() {
        return system;
    }

    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public Long getJobPositionId() {
        return jobPositionId;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getHiredAt() {
        return hiredAt;
    }

    public LocalDate getResignedAt() {
        return resignedAt;
    }
}
