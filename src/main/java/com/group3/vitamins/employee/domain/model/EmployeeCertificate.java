package com.group3.vitamins.employee.domain.model;

import java.time.LocalDate;

/**
 * 사원 자격증 한 건 (`employee.md` §3 · HR-V1 QUAL). 자격증은 마스터({@code certificate}) 참조,
 * 취득일은 자유입력(선택)이다. 사원에 1:N 으로 붙는다.
 *
 * @param companyId     회사(테넌트)
 * @param userId        사원 사번 (employee.user_id)
 * @param certificateId 자격증 마스터 ID
 * @param acquiredDate  취득일 (null 허용)
 */
public record EmployeeCertificate(
        Long companyId,
        String userId,
        Long certificateId,
        LocalDate acquiredDate
) {
}
