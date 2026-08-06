package com.group3.vitamins.file.application.command;

/** 업로드 완료 통보(§2) 커맨드. checksum 은 선택(보내면 버전에 기록). */
public record CompleteFileUploadCommand(
        Long fileVersionId,
        String checksum,
        String requesterUserId,
        String role
) {
}
