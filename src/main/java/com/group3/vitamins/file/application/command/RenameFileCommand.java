package com.group3.vitamins.file.application.command;

/** 문서명 수정 명령(§4). 표시명만 바꾸며 원본 파일명은 건드리지 않는다. */
public record RenameFileCommand(
        Long fileId,
        String name,
        String requesterUserId,
        String role
) {
}
