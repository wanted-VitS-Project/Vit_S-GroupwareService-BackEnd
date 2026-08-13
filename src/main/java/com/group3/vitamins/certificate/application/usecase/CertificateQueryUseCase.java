package com.group3.vitamins.certificate.application.usecase;

import com.group3.vitamins.certificate.application.query.CertificateListQuery;
import com.group3.vitamins.certificate.application.result.CertificateListItemResult;

import java.util.List;

/** 자격증 마스터 조회 인바운드 포트 (목록 + 사용 사원 수). ADMIN 전용. */
public interface CertificateQueryUseCase {

    List<CertificateListItemResult> list(CertificateListQuery query);
}
