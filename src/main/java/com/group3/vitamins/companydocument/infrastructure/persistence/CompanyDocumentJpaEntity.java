package com.group3.vitamins.companydocument.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * {@code company_document} 테이블 매핑 (사내 문서 · 회사 소속 논리 문서).
 *
 * <p>{@code created_at}·{@code updated_at} 은 DB 기본값이 관리하므로 매핑하지 않는다.
 * {@code deleted_at} 은 soft delete(§5)/복구(§6)에서 갱신하므로 매핑한다.
 * file 과 달리 낙관락 {@code version} 컬럼이 없다(단순화, §6-4).
 */
@Entity
@Table(name = "company_document")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyDocumentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_document_id")
    private Long companyDocumentId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "created_by", nullable = false, length = 20)
    private String createdBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
