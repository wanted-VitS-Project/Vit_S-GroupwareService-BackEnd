package com.group3.vitamins.employee.domain.repository;

import com.group3.vitamins.employee.domain.model.EmployeeEducation;

import java.util.List;

/**
 * 사원 학력 쓰기 (`employee.md` §3·§4). 등록은 saveAll, 수정은 전체 교체(deleteByUserId → saveAll).
 * 상세 조회(마스터명 조인)는 MyBatis 가 맡으므로 여기엔 읽기 메서드를 두지 않는다.
 */
public interface EmployeeEducationRepository {

    /** 여러 학력을 한 번에 저장(비어 있으면 아무것도 하지 않는다). */
    void saveAll(List<EmployeeEducation> educations);

    /** 해당 사원의 학력 전부 삭제(전체 교체 수정의 앞 단계). */
    void deleteByUserId(String userId);
}
