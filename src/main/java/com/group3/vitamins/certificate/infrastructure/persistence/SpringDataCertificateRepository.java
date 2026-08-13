package com.group3.vitamins.certificate.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataCertificateRepository extends JpaRepository<CertificateJpaEntity, Long> {

    Optional<CertificateJpaEntity> findByCertificateIdAndCompanyId(Long certificateId, Long companyId);

    Optional<CertificateJpaEntity> findByCompanyIdAndName(Long companyId, String name);
}
