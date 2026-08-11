package com.group3.vitamins.employee.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 부서·직급 존재 확인 MyBatis 매퍼 (`employee.md` §3·§4 FK 검증). SQL 은
 * {@code src/main/resources/mapper/employee/EmployeeReferenceQueryMapper.xml} 에 둔다 (팀 MyBatis 컨벤션).
 */
@Mapper
public interface EmployeeReferenceQueryMapper {

    boolean departmentExists(@Param("departmentId") Long departmentId, @Param("companyId") Long companyId);

    boolean jobPositionExists(@Param("jobPositionId") Long jobPositionId, @Param("companyId") Long companyId);
}
