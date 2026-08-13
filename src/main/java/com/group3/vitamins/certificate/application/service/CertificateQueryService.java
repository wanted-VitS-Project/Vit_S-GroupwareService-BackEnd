package com.group3.vitamins.certificate.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.certificate.application.query.CertificateListQuery;
import com.group3.vitamins.certificate.application.result.CertificateListItemResult;
import com.group3.vitamins.certificate.application.port.CertificateQueryPort;
import com.group3.vitamins.certificate.application.usecase.CertificateQueryUseCase;
import com.group3.vitamins.qualification.application.policy.QualificationAdminPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 자격증 마스터 조회 서비스 (목록 + 사용 사원 수). 읽기 전용, ADMIN 전용. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateQueryService implements CertificateQueryUseCase {

    private final QualificationAdminPolicy adminPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final CertificateQueryPort certificateQueryPort;

    @Override
    public List<CertificateListItemResult> list(CertificateListQuery query) {
        adminPolicy.assertAdmin(query.role());
        long companyId = currentCompanyIdProvider.currentCompanyId();
        return certificateQueryPort.findCertificatesWithCount(companyId, query.keyword()).stream()
                .map(CertificateListItemResult::from)
                .toList();
    }
}
