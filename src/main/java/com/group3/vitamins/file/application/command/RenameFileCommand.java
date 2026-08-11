package com.group3.vitamins.file.application.command;

/**
 * 문서명 수정 명령(§4). 표시명만 바꾸며 원본 파일명은 건드리지 않는다.
 *
 * <p>{@code version} = 조회에서 받은 낙관락 버전(저장 조건). {@code overwrite} = true 면 충돌을 무시하고
 * DB 현재 버전을 기대값으로 써서 반드시 통과시킨다(CONCURRENCY.md §5 · 충돌 모달의 "덮어쓰기").
 */
public record RenameFileCommand(
        Long fileId,
        String name,
        int version,
        boolean overwrite,
        String requesterUserId,
        String role
) {
}
