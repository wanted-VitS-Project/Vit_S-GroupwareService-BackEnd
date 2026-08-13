package com.group3.vitamins.companydocument.domain.repository;

import com.group3.vitamins.companydocument.domain.model.CompanyDocument;

import java.util.Optional;

/**
 * 사내 문서(논리 문서) 영속성 아웃바운드 포트.
 *
 * <p>구현체는 {@code infrastructure/persistence/CompanyDocumentRepositoryAdapter} (JPA · saveAndFlush).
 * 목록 같은 화면용 조회는 별도 MyBatis 조회 포트({@code CompanyDocumentQueryPort})가 담당한다.
 * file 과 달리 낙관락·물리삭제가 없어 포트가 단순하다(soft delete 는 {@code save} 로 처리).
 */
public interface CompanyDocumentRepository {

    /** 생성·수정·soft delete 전이를 저장한다. 제약 위반을 쓰기 시점에 내려면 어댑터는 saveAndFlush 로 구현한다. */
    CompanyDocument save(CompanyDocument companyDocument);

    /** 삭제 여부와 무관하게 문서를 찾는다(복구·상태 판정은 서비스가 deletedAt 으로 한다). */
    Optional<CompanyDocument> findById(Long companyDocumentId);
}
