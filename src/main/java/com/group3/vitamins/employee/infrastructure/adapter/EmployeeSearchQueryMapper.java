package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.employee.application.result.EmployeeSearchRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 사원 이름 검색 조회. <b>MyBatis 를 쓰는 이유는 사원·부서·직급 3개 테이블을 가로지르기 때문</b>이다
 * (auth 의 {@code AuthQueryMapper} 선례). JPA 로 짜면 연관관계를 타느라 쿼리가 여러 번 나간다.
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 (팀 MyBatis 컨벤션) —
 * {@code src/main/resources/mapper/employee/EmployeeSearchQueryMapper.xml}. 여기엔 메서드 선언만 있다.
 * XML 의 {@code namespace} 는 이 인터페이스 전체 경로, {@code id} 는 메서드명과 같다.
 */
@Mapper
public interface EmployeeSearchQueryMapper {

    /** 이름 부분 일치 결재자 후보. 시스템 계정·퇴사자·삭제 사원 제외 (`employee.md` §9). */
    List<EmployeeSearchRow> searchByName(@Param("name") String name);
}
