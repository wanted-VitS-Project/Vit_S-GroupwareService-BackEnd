package com.group3.vitamins.companydocument.application.port;

/**
 * 사내 문서 인덱싱 트리거 페이로드 (COMPANY-DOC-V1 §6-2). AI(vitamate) 도메인과 합의한 최소 식별자 —
 * AI 는 {@code companyDocumentVersionId} 를 키로 인덱스 상태를 소유하고, {@code s3Key} 로 원문을 직접 GetObject 한다.
 * {@code companyId} 는 테넌트 스코프(격리·집계)용이다.
 *
 * @param companyDocumentVersionId 인덱싱 대상 버전(AI 인덱스 상태의 키)
 * @param companyId                회사(테넌트)
 * @param s3Key                    원문 객체 키(AI 가 GetObject)
 */
public record CompanyDocumentIndexTarget(
        Long companyDocumentVersionId,
        Long companyId,
        String s3Key
) {
}
