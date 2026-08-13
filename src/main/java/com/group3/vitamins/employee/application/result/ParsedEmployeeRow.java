package com.group3.vitamins.employee.application.result;

/**
 * 엑셀에서 읽어낸 <b>원시 행</b> (employee.md §7·§8). 모든 값은 셀을 문자열로 읽어 trim 한 것이며, 빈 셀은 {@code null}.
 * {@code rowNumber} 는 사람이 보는 엑셀 행 번호(1-base, 헤더=1). 타입 변환·검증은 서비스가 한다.
 */
public record ParsedEmployeeRow(
        int rowNumber,
        String userId,
        String name,
        String department,
        String jobPosition,
        String hiredAt,
        String email,
        String phone,
        String role,
        String education,   // 원시 셀 "전공:학위; ..." (선택 · null 허용)
        String certificate  // 원시 셀 "자격증명; ..." (선택 · null 허용)
) {
}
