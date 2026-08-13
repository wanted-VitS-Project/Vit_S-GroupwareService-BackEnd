package com.group3.vitamins.companydocument.application.port;

/**
 * 사내 문서 AI 인덱싱 트리거 아웃바운드 포트 (COMPANY-DOC-V1 §2-D · §6-2).
 *
 * <p>사내 문서를 AI 공고 검토의 비교 자료로 쓰기 위해 완료 버전을 인덱싱 대상으로 등록하고, 삭제 시 제외한다.
 * ⚠️ **소비(청킹·임베딩·저장) 방식은 AI(vitamate) 도메인 소관이다(§6-2 미확정).** 우리는 트리거만 발행한다 —
 * file 의 {@code FileIndexTriggerPort} 와 같은 역할이되, 사내 문서 버전을 대상으로 한다.
 * AI 도메인이 소비 경로를 확정하기 전까지는 {@code infrastructure/adapter} 의 no-op 스텁이 구현한다.
 */
public interface CompanyDocumentIndexTriggerPort {

    /** 업로드 완료(§2) 시 이 버전을 인덱싱 대상으로 등록한다(버전 단위). */
    void triggerIndexing(Long versionId);

    /** soft delete(§5) 시 이 문서의 파생 인덱스를 제외 대상으로 등록한다(문서 단위). */
    void triggerRemoval(Long companyDocumentId);

    /** 복구(§6) 시 이 문서의 완료 버전을 인덱싱 대상으로 재등록한다(문서 단위). */
    void triggerReindex(Long companyDocumentId);
}
