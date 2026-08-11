package com.group3.vitamins.employee.application.result;

/**
 * 엑셀 일괄 등록의 <b>행 1건 오류</b> (employee.md §7·§8). 한 행에 여러 문제가 있어도 우선순위상 첫 번째 하나만 담는다.
 * {@code userId}·{@code name} 은 화면 표시용이며 누락 행이면 {@code null} 일 수 있다.
 */
public record BulkRowError(
        int row,
        String userId,
        String name,
        BulkValidation validation,
        String message
) {
}
