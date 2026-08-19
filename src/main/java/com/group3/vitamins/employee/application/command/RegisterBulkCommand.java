package com.group3.vitamins.employee.application.command;

/**
 * 엑셀 일괄 등록 요청 (employee.md §8). {@code skipErrors=false}(기본)면 오류 행이 하나라도 있으면 등록하지 않고
 * {@code EMP_HAS_ERRORS}(400)로 막고, {@code true}면 유효 행만 등록한다(부분 등록, 화면 "오류 제외하고 등록").
 *
 * <p>{@code autoCreateMasters=true} 면 목록에 없는 전공/자격증을 사원 등록 <b>전에</b> 마스터로 먼저 만든 뒤 참조한다(2026-08-18).
 * ⚠️ 검증(§7) 때 보낸 값과 같아야 검증 화면과 등록 결과가 일치한다.
 */
public record RegisterBulkCommand(
        String actorRole,
        byte[] content,
        String originalFilename,
        long size,
        boolean skipErrors,
        boolean autoCreateMasters
) {
}
