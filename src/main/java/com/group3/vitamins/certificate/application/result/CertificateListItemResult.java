package com.group3.vitamins.certificate.application.result;

/**
 * 자격증 목록 항목 결과 — 자격증 + 사용 사원 수(활성, MAJ-003) + 삭제 가능 여부.
 * ⚠️ {@code deletable} 은 활성 수가 아니라 전체 참조 수로 판정한다 — 퇴사·시스템 사원의 자격증도 FK 로 삭제를 막기 때문.
 */
public record CertificateListItemResult(Long certificateId, String name, int employeeCount, boolean deletable) {

    public static CertificateListItemResult from(CertificateListProjection p) {
        return new CertificateListItemResult(p.certificateId(), p.name(), p.employeeCount(), p.referenceCount() == 0);
    }
}
