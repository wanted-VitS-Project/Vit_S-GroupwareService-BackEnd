package com.group3.vitamins.employee.application.result;

import java.time.LocalDate;

/**
 * 사원 상세의 자격증 한 행 (`employee.md` §2 {@code data.certificates[]}). 자격증 마스터를 조인해
 * 이름을 함께 담는다.
 *
 * @param certificateId   자격증 마스터 ID
 * @param certificateName 자격증명 (마스터 조인)
 * @param acquiredDate    취득일 (null 허용)
 */
public record EmployeeCertificateRow(
        Long certificateId,
        String certificateName,
        LocalDate acquiredDate
) {
}
