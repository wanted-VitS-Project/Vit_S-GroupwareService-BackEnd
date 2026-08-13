package com.group3.vitamins.employee.domain.model;

/**
 * 사원 학력 한 건 (`employee.md` §3 · HR-V1 QUAL-003). 전공은 마스터({@code major}) 참조,
 * 학위는 {@link Degree} enum, 학교는 자유입력(선택)이다. 사원에 1:N 으로 붙는다.
 *
 * <p>{@code @OneToMany} 를 쓰지 않고 별도 애그리거트 조각으로 두어, 등록·수정 시 saveAll/전체교체로
 * 배치 반영한다(그룹 구성원 선례). {@code company_id} 는 테넌트 스코프용으로 함께 스탬핑한다.
 *
 * @param companyId 회사(테넌트)
 * @param userId    사원 사번 (employee.user_id)
 * @param majorId   전공 마스터 ID
 * @param degree    학위
 * @param school    학교 (null 허용)
 */
public record EmployeeEducation(
        Long companyId,
        String userId,
        Long majorId,
        Degree degree,
        String school
) {
}
