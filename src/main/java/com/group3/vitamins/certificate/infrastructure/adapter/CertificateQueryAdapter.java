package com.group3.vitamins.certificate.infrastructure.adapter;

import com.group3.vitamins.certificate.application.port.CertificateQueryPort;
import com.group3.vitamins.certificate.application.result.CertificateListProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CertificateQueryAdapter implements CertificateQueryPort {

    private final CertificateQueryMapper mapper;

    @Override
    public List<CertificateListProjection> findCertificatesWithCount(Long companyId, String keyword) {
        return mapper.findCertificatesWithCount(companyId, keyword);
    }

    @Override
    public long countReferences(Long certificateId, Long companyId) {
        return mapper.countReferences(certificateId, companyId);
    }
}
