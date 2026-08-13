package com.group3.vitamins.companydocument.application.result;

/** 사내 문서 복구(§6) 결과. */
public record CompanyDocumentRestoreResult(
        Long companyDocumentId,
        String name,
        String category
) {
}
