package com.group3.vitamins.employee.application.result;

import java.util.List;

/**
 * 사원 목록 페이징 결과 (`employee.md` §1). 조회된 행과 전체 건수를 담는다.
 *
 * <p>{@code totalPages} 는 전체 건수와 페이지 크기로 파생한다. size 가 0 이면 나눗셈이 터지므로 0 으로 눕힌다.
 *
 * @param content       현재 페이지의 행 목록
 * @param page          0-base 페이지 번호
 * @param size          페이지 크기
 * @param totalElements 필터를 적용한 전체 건수
 */
public record EmployeePage(
        List<EmployeeListRow> content,
        int page,
        int size,
        long totalElements
) {

    public int totalPages() {
        if (size <= 0) {
            return 0;
        }
        return (int) ((totalElements + size - 1) / size);
    }
}
