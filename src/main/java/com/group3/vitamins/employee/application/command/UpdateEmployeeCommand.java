package com.group3.vitamins.employee.application.command;

/**
 * 사원 정보 수정 커맨드 (`employee.md` §4). <b>전달한 필드만</b> 수정하므로 필드마다 "전달 여부" 플래그를 둔다 —
 * {@code jobPositionId} 에 명시적 {@code null} 을 보내면 직급을 지운다(전달 안 함과 구별해야 한다). 컨트롤러가
 * raw JSON 에서 존재 여부와 타입을 판별해 이 커맨드로 옮긴다.
 *
 * @param actorRole            요청자 전역 권한 (ADMIN 판정용)
 * @param userId               수정 대상 사번 (경로 변수, 변경 불가)
 * @param nameProvided         이름 전달 여부
 * @param name                 이름 (전달됐을 때만 유효)
 * @param phoneProvided        연락처 전달 여부
 * @param phone                연락처
 * @param emailProvided        이메일 전달 여부
 * @param email                이메일
 * @param departmentIdProvided 부서 전달 여부
 * @param departmentId         부서 ID
 * @param jobPositionIdProvided 직급 전달 여부
 * @param jobPositionId        직급 ID ({@code null} + 전달됨 = 직급 미지정으로 변경)
 * @param hiredAtProvided      입사일 전달 여부
 * @param hiredAt              입사일 {@code yyyy-MM-dd}
 * @param educationsProvided   학력 전달 여부 — 전체 교체(QUAL-004). 미전송·명시적 null 은 false(유지)
 * @param educations           학력 (전달됐을 때만 유효). {@code []} = 전부 삭제
 * @param certificatesProvided 자격증 전달 여부
 * @param certificates         자격증 (전달됐을 때만 유효). {@code []} = 전부 삭제
 */
public record UpdateEmployeeCommand(
        String actorRole,
        String userId,
        boolean nameProvided,
        String name,
        boolean phoneProvided,
        String phone,
        boolean emailProvided,
        String email,
        boolean departmentIdProvided,
        Long departmentId,
        boolean jobPositionIdProvided,
        Long jobPositionId,
        boolean hiredAtProvided,
        String hiredAt,
        boolean educationsProvided,
        java.util.List<EducationItem> educations,
        boolean certificatesProvided,
        java.util.List<CertificateItem> certificates
) {

    /** 수정할 필드가 하나라도 전달됐는가 (아무것도 없으면 EMP_INVALID_REQUEST). */
    public boolean hasNoFields() {
        return !(nameProvided || phoneProvided || emailProvided
                || departmentIdProvided || jobPositionIdProvided || hiredAtProvided
                || educationsProvided || certificatesProvided);
    }
}
