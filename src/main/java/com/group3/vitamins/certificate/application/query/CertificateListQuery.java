package com.group3.vitamins.certificate.application.query;

/** 자격증 목록 조회 입력. keyword 공백은 null 로 눕힌다. */
public record CertificateListQuery(String keyword, String role) {

    public CertificateListQuery {
        keyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    }
}
