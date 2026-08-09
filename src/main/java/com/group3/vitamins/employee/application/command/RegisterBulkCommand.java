package com.group3.vitamins.employee.application.command;

/**
 * 엑셀 일괄 등록 요청 (employee.md §8). {@code skipErrors=false}(기본)면 오류 행이 하나라도 있으면 등록하지 않고
 * {@code EMP_HAS_ERRORS}(400)로 막고, {@code true}면 유효 행만 등록한다(부분 등록, 화면 "오류 제외하고 등록").
 */
public record RegisterBulkCommand(
        String actorRole,
        byte[] content,
        String originalFilename,
        long size,
        boolean skipErrors
) {
}
