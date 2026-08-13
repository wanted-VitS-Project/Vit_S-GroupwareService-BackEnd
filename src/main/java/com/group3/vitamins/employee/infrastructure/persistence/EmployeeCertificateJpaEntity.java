package com.group3.vitamins.employee.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * {@code employee_certificate} 테이블 매핑 (사원 자격증 1:N).
 * {@code created_at} 은 DB 기본값이 관리하므로 매핑하지 않는다.
 */
@Entity
@Table(name = "employee_certificate")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeCertificateJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_certificate_id")
    private Long employeeCertificateId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "user_id", length = 20, nullable = false)
    private String userId;

    @Column(name = "certificate_id", nullable = false)
    private Long certificateId;

    @Column(name = "acquired_date")
    private LocalDate acquiredDate;

    public EmployeeCertificateJpaEntity(Long companyId, String userId, Long certificateId, LocalDate acquiredDate) {
        this.companyId = companyId;
        this.userId = userId;
        this.certificateId = certificateId;
        this.acquiredDate = acquiredDate;
    }
}
