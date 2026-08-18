package com.group3.vitamins.employee.application.command;

/**
 * 엑셀 일괄 등록 검증 요청 (employee.md §7). 웹 타입(MultipartFile)을 서비스에 노출하지 않도록 컨트롤러가
 * 바이너리·파일명·크기만 뽑아 넘긴다. 파일 없음·형식·크기(5MB) 판정에 {@code content}·{@code originalFilename}·{@code size} 를 쓴다.
 *
 * <p>{@code autoCreateMasters=true} 면 목록에 없는 전공/자격증을 {@code EDU_NOT_FOUND}/{@code CERT_NOT_FOUND} 오류가 아니라
 * 등록 시 자동 생성 대상({@code newMasters})으로 분류한다(2026-08-18).
 */
public record ValidateBulkCommand(
        String actorRole,
        byte[] content,
        String originalFilename,
        long size,
        boolean autoCreateMasters
) {
}
