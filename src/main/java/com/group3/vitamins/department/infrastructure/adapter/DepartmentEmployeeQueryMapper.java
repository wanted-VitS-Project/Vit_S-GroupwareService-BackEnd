package com.group3.vitamins.department.infrastructure.adapter;

import com.group3.vitamins.department.application.result.DepartmentEmployeeCountRow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 부서 조회 중 <b>부서와 사원을 가로지르는</b> 집계 (사원 도메인 소관 {@code employee} 테이블).
 * 남의 테이블에 JPA 엔티티를 만들면 소유가 흐려지므로 MyBatis 로 읽는다 ({@code AuthQueryMapper} 선례).
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 (팀 MyBatis 컨벤션) —
 * {@code src/main/resources/mapper/department/DepartmentEmployeeQueryMapper.xml}. 여기엔 메서드 선언만 있다.
 * XML 의 {@code namespace} 는 이 인터페이스 전체 경로, {@code id} 는 메서드명과 같다.
 */
@Mapper
public interface DepartmentEmployeeQueryMapper {

    /**
     * 전체 부서를 직속 인원 수와 함께 조회한다 (정렬 {@code department_id} 오름차순 = 생성 순).
     * 인원 집계에서 시스템 계정·퇴사자는 제외한다 (`.ai/api/department.md` §1).
     */
    List<DepartmentEmployeeCountRow> findAllWithDirectEmployeeCount();

    /**
     * 부서 1건의 직속 사원 수 — 삭제 차단({@code DEPT_HAS_EMPLOYEES}) 판정용.
     * 목록의 집계와 같은 기준(시스템 계정·퇴사자 제외)을 쓴다.
     */
    long countDirectEmployees(Long departmentId);
}
