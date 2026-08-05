package com.group3.vitamins.employee.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;

/**
 * 사원 JPA 엔티티. 팀 ERD 의 {@code employee} 테이블 (이미 존재 — 새 마이그레이션 없음).
 *
 * <p>도메인 로직은 {@link com.group3.vitamins.employee.domain.model.Employee} 가 갖고, 이 클래스는
 * 순수 영속 매핑만 한다. PK 는 <b>자동 증가가 아니라 할당되는 사번</b>이라 {@code @GeneratedValue} 를 붙이지 않는다.
 *
 * <p>🔑 {@link Persistable} 을 구현하는 이유 — 할당 PK 를 그냥 두면 Spring Data {@code save()} 가
 * {@code merge()}(SELECT 후 UPDATE-or-INSERT)로 동작해, 이미 있는 사번을 저장하면 예외 대신 <b>기존 행을
 * 덮어쓴다.</b> {@code isNew=true} 로 {@code persist()} 를 강제하면 저장이 진짜 INSERT 가 되어 PK 중복이
 * {@code DataIntegrityViolationException} 으로 즉시 터진다 → 등록 유스케이스가 409 로 변환한다.
 * 로드된 엔티티는 {@code @PostLoad} 로 {@code isNew=false} 가 되어 이후 갱신은 정상 UPDATE 로 동작한다.
 *
 * <p>{@code created_at}·{@code updated_at}·{@code deleted_at} 은 DB/다른 경로가 관리하고 등록 응답에 나가지
 * 않으므로 매핑하지 않는다 (매핑하지 않은 DB 컬럼은 {@code ddl-auto: validate} 에 걸리지 않는다).
 */
@Entity
@Table(name = "employee")
// 변경된 컬럼만 UPDATE 한다 — 정보 수정(§4)과 퇴사(§5)가 동시에 실행될 때 서로의 컬럼(정보 vs resigned_at)을
// 덮어쓰는 lost-update 를 막는다. 아래 applyInfo/resign 이 각자 자기 컬럼만 건드리므로 두 작업이 충돌하지 않는다
// (AccountEntity 와 같은 이유·같은 패턴).
@DynamicUpdate
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeJpaEntity implements Persistable<String> {

    @Id
    @Column(name = "user_id", length = 20)
    private String userId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 시스템 계정(ADMIN 가상 사원) 여부. 등록으로 만드는 사원은 항상 {@code false} */
    @Column(name = "is_system", nullable = false)
    private boolean system;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "job_position_id")
    private Long jobPositionId;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "hired_at")
    private LocalDate hiredAt;

    /** 퇴사일. 등록 시 {@code null}(재직). 퇴사 처리(PR-B)에서 채운다 */
    @Column(name = "resigned_at")
    private LocalDate resignedAt;

    /** 신규 여부 — persist(INSERT) 강제용. 영속화되거나 로드되면 false 로 내려간다. 컬럼이 아니다. */
    @Transient
    @Getter(AccessLevel.NONE)
    private boolean isNew = true;

    public EmployeeJpaEntity(String userId, String name, boolean system, Long departmentId, Long jobPositionId,
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

    @Override
    public String getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    /**
     * 정보 수정 (`employee.md` §4). 사번·시스템여부·<b>퇴사일</b>은 건드리지 않는다 — 퇴사일을 함께 쓰면 동시
     * 퇴사 처리를 덮어쓴다. {@code @DynamicUpdate} 와 함께 이 메서드가 바꾼 컬럼만 UPDATE 되므로 퇴사와 충돌하지 않는다.
     */
    void applyInfo(String name, Long departmentId, Long jobPositionId,
                   String email, String phone, LocalDate hiredAt) {
        this.name = name;
        this.departmentId = departmentId;
        this.jobPositionId = jobPositionId;
        this.email = email;
        this.phone = phone;
        this.hiredAt = hiredAt;
    }

    /** 퇴사 처리 (`employee.md` §5). {@code resigned_at} 만 바꾼다 — 정보 컬럼은 건드리지 않아 동시 수정과 충돌하지 않는다. */
    void resign(LocalDate resignedAt) {
        this.resignedAt = resignedAt;
    }
}
