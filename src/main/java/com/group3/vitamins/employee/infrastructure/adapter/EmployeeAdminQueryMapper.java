package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.employee.application.query.EmployeeListCriteria;
import com.group3.vitamins.employee.application.result.EmployeeCertificateRow;
import com.group3.vitamins.employee.application.result.EmployeeDetailRow;
import com.group3.vitamins.employee.application.result.EmployeeEducationRow;
import com.group3.vitamins.employee.application.result.EmployeeGroupRow;
import com.group3.vitamins.employee.application.result.EmployeeListRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 인사관리용 사원 조회 MyBatis 매퍼 (`employee.md` §1·§2). 계정·사원·부서(자기+상위)·직급·그룹을 가로지른다.
 *
 * <p>SQL 은 XML 에 둔다 — {@code src/main/resources/mapper/employee/EmployeeAdminQueryMapper.xml}
 * (namespace = 이 인터페이스 FQN, 팀 MyBatis 컨벤션). 쓰기는 JPA 담당이며 여기는 읽기 전용이다.
 */
@Mapper
public interface EmployeeAdminQueryMapper {

    List<EmployeeListRow> findPage(EmployeeListCriteria criteria);

    long count(EmployeeListCriteria criteria);

    Optional<EmployeeDetailRow> findDetail(@Param("userId") String userId, @Param("companyId") Long companyId);

    List<EmployeeGroupRow> findGroups(@Param("userId") String userId);

    List<EmployeeEducationRow> findEducations(@Param("userId") String userId);

    List<EmployeeCertificateRow> findCertificates(@Param("userId") String userId);
}
