package com.group3.vitamins.employee.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 회사코드 조회 MyBatis 매퍼. SQL 은
 * {@code src/main/resources/mapper/employee/CompanyCodeQueryMapper.xml} 에 둔다 (팀 MyBatis 컨벤션).
 */
@Mapper
public interface CompanyCodeQueryMapper {

    String findCodeByCompanyId(@Param("companyId") Long companyId);
}
