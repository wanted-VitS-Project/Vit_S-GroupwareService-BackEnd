package com.group3.vitamins.certificate.presentation.api;

/** 자격증 마스터 API 성공 응답 메시지 상수. */
public final class CertificateResponseMessage {

    public static final String CERT_LIST = "자격증 목록 조회 성공";
    public static final String CERT_CREATED = "자격증을 등록했습니다.";
    public static final String CERT_UPDATED = "자격증을 수정했습니다.";
    public static final String CERT_DELETED = "자격증을 삭제했습니다.";

    private CertificateResponseMessage() {
    }
}
