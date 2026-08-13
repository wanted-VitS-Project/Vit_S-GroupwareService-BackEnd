package com.group3.vitamins.employee.application.command;

/**
 * 등록·수정 요청의 자격증 한 건 (`employee.md` §3·§4). 컨트롤러가 받은 <b>가공 전</b> 값을 담는다 —
 * {@code acquiredDate} 파싱, 마스터 존재검사는 {@code EmployeeCommandService} 가 한다.
 *
 * @param certificateId 자격증 마스터 ID (필수 — 없으면 EMP_INVALID_REQUEST)
 * @param acquiredDate  취득일 {@code yyyy-MM-dd} (선택)
 */
public record CertificateItem(
        Long certificateId,
        String acquiredDate
) {
}
