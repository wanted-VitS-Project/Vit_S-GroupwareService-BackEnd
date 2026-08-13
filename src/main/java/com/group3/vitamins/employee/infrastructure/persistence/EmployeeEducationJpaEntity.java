package com.group3.vitamins.employee.infrastructure.persistence;

import com.group3.vitamins.employee.domain.model.Degree;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@code employee_education} 테이블 매핑 (사원 학력 1:N).
 * {@code created_at} 은 DB 기본값이 관리하므로 매핑하지 않는다. degree 는 enum 이름을 그대로 저장(VARCHAR).
 */
@Entity
@Table(name = "employee_education")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeEducationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_education_id")
    private Long employeeEducationId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "user_id", length = 20, nullable = false)
    private String userId;

    @Column(name = "major_id", nullable = false)
    private Long majorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "degree", length = 20, nullable = false)
    private Degree degree;

    @Column(name = "school", length = 100)
    private String school;

    public EmployeeEducationJpaEntity(Long companyId, String userId, Long majorId, Degree degree, String school) {
        this.companyId = companyId;
        this.userId = userId;
        this.majorId = majorId;
        this.degree = degree;
        this.school = school;
    }
}
