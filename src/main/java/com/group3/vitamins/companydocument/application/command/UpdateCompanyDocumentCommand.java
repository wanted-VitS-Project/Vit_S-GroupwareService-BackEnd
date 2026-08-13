package com.group3.vitamins.companydocument.application.command;

/** 사내 문서 표시명·카테고리 수정(§4) 커맨드. name·category 는 보낸 것만 반영(최소 1개 필요). */
public record UpdateCompanyDocumentCommand(
        Long companyDocumentId,
        String name,
        String category,
        String requesterUserId,
        String role
) {
}
