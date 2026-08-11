package com.group3.vitamins.file.application.result;

/** 문서명 수정 결과(§4). {@code version} 은 저장 뒤 +1 된 새 낙관락 버전(프론트가 다음 저장에 쓴다). */
public record FileRenameResult(
        Long fileId,
        String name,
        int version
) {
}
