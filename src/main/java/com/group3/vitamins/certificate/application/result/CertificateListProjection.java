package com.group3.vitamins.certificate.application.result;

/**
 * 자격증 목록 MyBatis 위치기반 매핑 레코드. ⚠️ 필드 순서 = XML SELECT alias 순서.
 * {@code deletable} 은 파생값이라 여기 없다(서비스가 {@link CertificateListItemResult} 로 만든다).
 */
public record CertificateListProjection(Long certificateId, String name, int employeeCount) {
}
