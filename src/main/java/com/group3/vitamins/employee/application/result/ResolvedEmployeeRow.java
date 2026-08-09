package com.group3.vitamins.employee.application.result;

import java.time.LocalDate;

/**
 * 검증을 통과해 <b>등록 가능한</b> 행 (employee.md §8). 부서명·직급명은 이미 ID 로 해석됐고 입사일도 파싱됐다.
 * 일괄 등록(§8)이 이 값으로 단건 등록과 동일한 경로(해싱→쓰기→메일)를 행 단위로 태운다.
 * {@code jobPositionId} 는 직급 미지정·직급명 불일치면 {@code null} 이다(직급 불일치는 오류가 아니라 null 등록).
 */
public record ResolvedEmployeeRow(
        int rowNumber,
        String userId,
        String name,
        Long departmentId,
        Long jobPositionId,
        LocalDate hiredAt,
        String email,
        String phone,
        String role
) {

    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }
}
