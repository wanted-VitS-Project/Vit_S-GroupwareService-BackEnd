package com.group3.vitamins.employee.application.command;

/**
 * 엑셀 일괄 등록 검증 요청 (employee.md §7). 웹 타입(MultipartFile)을 서비스에 노출하지 않도록 컨트롤러가
 * 바이너리·파일명·크기만 뽑아 넘긴다. 파일 없음·형식·크기(5MB) 판정에 {@code content}·{@code originalFilename}·{@code size} 를 쓴다.
 */
public record ValidateBulkCommand(
        String actorRole,
        byte[] content,
        String originalFilename,
        long size
) {
}
