package com.group3.vitamins.companydocument.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사내 문서 Spring Data 리포지토리.
 *
 * <p>낙관락·물리삭제가 없어 커스텀 쿼리가 필요 없다 — {@code save}/{@code findById} 기본 제공으로 충분하다.
 * soft delete 는 {@code deleted_at} 갱신을 {@code saveAndFlush} 로 처리한다.
 */
public interface SpringDataCompanyDocumentRepository extends JpaRepository<CompanyDocumentJpaEntity, Long> {
}
