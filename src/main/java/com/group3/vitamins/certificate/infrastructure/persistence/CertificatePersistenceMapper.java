package com.group3.vitamins.certificate.infrastructure.persistence;

import com.group3.vitamins.certificate.domain.model.Certificate;

/** {@link Certificate} 도메인 ↔ {@link CertificateJpaEntity} 변환. createdAt 은 DB 관리라 복원 시 null(응답에 안 쓰임). */
public final class CertificatePersistenceMapper {

    private CertificatePersistenceMapper() {
    }

    public static Certificate toDomain(CertificateJpaEntity e) {
        return Certificate.restore(e.getCertificateId(), e.getCompanyId(), e.getName(), null);
    }

    public static CertificateJpaEntity toEntity(Certificate d) {
        return new CertificateJpaEntity(d.getCertificateId(), d.getCompanyId(), d.getName());
    }
}
