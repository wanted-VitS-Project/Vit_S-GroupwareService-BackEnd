package com.group3.vitamins.file.application.command;

/**
 * 업로드 시작(§1) 커맨드. {@code fileId} 가 있으면 그 문서의 새 버전, 없으면 새 문서(버전 1)다.
 */
public record StartFileUploadCommand(
        Long blockId,
        String originalFileName,
        long sizeBytes,
        String mimeType,
        String name,
        Long fileId,
        String comment,
        boolean allowDuplicateName,
        String requesterUserId,
        String role
) {
}
