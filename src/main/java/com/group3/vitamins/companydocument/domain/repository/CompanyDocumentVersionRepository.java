package com.group3.vitamins.companydocument.domain.repository;

import com.group3.vitamins.companydocument.domain.model.CompanyDocumentVersion;

import java.util.Optional;

/**
 * 사내 문서 버전 영속성 아웃바운드 포트.
 *
 * <p>구현체는 {@code infrastructure/persistence/CompanyDocumentVersionRepositoryAdapter} (JPA).
 * 버전 이력 목록 같은 화면용 조회는 MyBatis 조회 포트가 담당한다.
 * 영구삭제가 없어 물리 삭제 메서드가 없고, 귀속이 아니라 멱등키 조회도 없다.
 */
public interface CompanyDocumentVersionRepository {

    /** 업로드 시작(UPLOADING)·완료 통보(COMPLETED/FAILED) 전이를 저장한다. */
    CompanyDocumentVersion save(CompanyDocumentVersion version);

    /** 업로드 완료 통보(§2)에서 대상 버전을 찾는다. */
    Optional<CompanyDocumentVersion> findById(Long versionId);

    /** 새 버전 차수 계산용 — 해당 문서의 현재 최대 versionNo(없으면 0). */
    int findMaxVersionNo(Long companyDocumentId);
}
