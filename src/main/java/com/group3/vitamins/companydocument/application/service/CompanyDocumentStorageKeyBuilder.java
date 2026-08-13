package com.group3.vitamins.companydocument.application.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 사내 문서 저장소 키 생성기 — 멀티테넌시 키 규약의 단일 출처 (COMPANY-DOC-V1 §2-F).
 *
 * <p>키: {@code companies/{companyId}/documents/{documentId}/versions/{versionNo}/{uuid}[.ext]}.
 * file 의 {@code StorageKeyBuilder}(projects/…)와 최상위 {@code companies/{companyId}/} 접두사는 통일하되,
 * 사내 문서는 프로젝트를 타지 않으므로 {@code documents/} 세그먼트를 쓴다.
 * 경로에 versionId 대신 versionNo 를 쓴다 — versionId 는 INSERT 전에 알 수 없고, uuid 가 유일성을 보장한다.
 */
@Component
public class CompanyDocumentStorageKeyBuilder {

    public String build(long companyId, long documentId, int versionNo, String extension) {
        String uuid = UUID.randomUUID().toString();
        String suffix = (extension == null || extension.isEmpty()) ? "" : "." + extension;
        return "companies/%d/documents/%d/versions/%d/%s%s"
                .formatted(companyId, documentId, versionNo, uuid, suffix);
    }
}
