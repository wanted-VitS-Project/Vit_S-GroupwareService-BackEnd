package com.group3.vitamins.employee.application.result;

import com.group3.vitamins.employee.domain.model.EmployeeCertificate;
import com.group3.vitamins.employee.domain.model.EmployeeEducation;

import java.time.LocalDate;
import java.util.List;

/**
 * 검증을 통과해 <b>등록 가능한</b> 행 (employee.md §8). 부서명·직급명은 이미 ID 로 해석됐고 입사일도 파싱됐다.
 * 일괄 등록(§8)이 이 값으로 단건 등록과 동일한 경로(해싱→쓰기→메일)를 행 단위로 태운다.
 * {@code jobPositionId} 는 직급 미지정·직급명 불일치면 {@code null} 이다(직급 불일치는 오류가 아니라 null 등록).
 * {@code educations}·{@code certificates} 는 전공명·자격증명을 마스터 ID·학위 enum 으로 해석해둔 것(엑셀엔 학교·취득일 없음 → null).
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
        String role,
        List<EmployeeEducation> educations,
        List<EmployeeCertificate> certificates
) {

    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }
}
