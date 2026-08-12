package com.group3.vitamins.companydocument.application.port;

import com.group3.vitamins.companydocument.application.query.CompanyDocumentListCriteria;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentListProjection;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentVersionProjection;

import java.util.List;

/**
 * 사내 문서 화면용 조회 아웃바운드 포트 (MyBatis · 조회 전용).
 * 구현은 {@code infrastructure/adapter/CompanyDocumentQueryAdapter}. 회사 스코프는 criteria/파라미터로 항상 전달한다.
 */
public interface CompanyDocumentQueryPort {

    /** 목록(§3) 총 건수 — 문서당 1행이라 COUNT(*) = 문서 수. 회사 스코프. */
    long countDocuments(CompanyDocumentListCriteria criteria);

    /** 목록(§3) 한 페이지 — 문서 단위 최신 완료 버전 1행. 최신 완료 시각 내림차순. */
    List<CompanyDocumentListProjection> findDocuments(CompanyDocumentListCriteria criteria);

    /** 버전 이력(§7) — 완료 버전만, 차수 내림차순. 문서 소유(회사) 검증은 서비스가 먼저 한다. */
    List<CompanyDocumentVersionProjection> findCompletedVersions(Long companyDocumentId);
}
