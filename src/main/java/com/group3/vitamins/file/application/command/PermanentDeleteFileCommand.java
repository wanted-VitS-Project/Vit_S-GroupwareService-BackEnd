package com.group3.vitamins.file.application.command;

/**
 * 영구 삭제 명령(§7). 휴지통에 있는 문서를 전 버전·저장소 객체까지 되돌릴 수 없이 지운다.
 * {@code confirmText} 는 정확히 {@code "영구 삭제"} 여야 한다(서버 검증).
 */
public record PermanentDeleteFileCommand(
        Long fileId,
        String confirmText,
        String requesterUserId,
        String role
) {
}
