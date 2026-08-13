package com.group3.vitamins.certificate.application.result;

import com.group3.vitamins.certificate.domain.model.Certificate;

/** 자격증 생성·수정 결과. */
public record CertificateResult(Long certificateId, String name) {

    public static CertificateResult of(Certificate certificate) {
        return new CertificateResult(certificate.getCertificateId(), certificate.getName());
    }
}
