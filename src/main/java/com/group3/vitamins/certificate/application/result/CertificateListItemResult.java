package com.group3.vitamins.certificate.application.result;

/** 자격증 목록 항목 결과 — 자격증 + 사용 사원 수(MAJ-003) + 삭제 가능 여부(파생). */
public record CertificateListItemResult(Long certificateId, String name, int employeeCount, boolean deletable) {

    public static CertificateListItemResult from(CertificateListProjection p) {
        return new CertificateListItemResult(p.certificateId(), p.name(), p.employeeCount(), p.employeeCount() == 0);
    }
}
