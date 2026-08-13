package com.group3.vitamins.companydocument.application.result;

import java.time.LocalDateTime;

/** 사내 문서 삭제(§5) 결과. */
public record CompanyDocumentDeleteResult(
        Long companyDocumentId,
        LocalDateTime deletedAt
) {
}
