package com.group3.vitamins.companydocument.application.command;

/** 사내 문서 업로드 완료 통보(§2) 커맨드. {@code checksum} 은 선택(보내면 서버가 대조). */
public record CompleteCompanyDocumentUploadCommand(
        Long versionId,
        String checksum,
        String requesterUserId,
        String role
) {
}
