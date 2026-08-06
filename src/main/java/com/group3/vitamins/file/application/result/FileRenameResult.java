package com.group3.vitamins.file.application.result;

/** 문서명 수정 결과(§4). */
public record FileRenameResult(
        Long fileId,
        String name
) {
}
