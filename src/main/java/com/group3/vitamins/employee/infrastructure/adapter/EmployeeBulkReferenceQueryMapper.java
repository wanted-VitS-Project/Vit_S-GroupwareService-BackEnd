package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.employee.application.result.NameIdRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 엑셀 일괄 등록의 부서명·직급명 → ID 조회 MyBatis 매퍼 (employee.md §7·§8).
 * SQL 은 {@code src/main/resources/mapper/employee/EmployeeBulkReferenceQueryMapper.xml} 에 둔다(팀 컨벤션).
 */
@Mapper
public interface EmployeeBulkReferenceQueryMapper {

    /** 이 회사 안에서 유일하게 매칭되는 부서명만 (name, id). 회사·형제 유니크라 이름이 겹치는 부서는 제외한다. */
    List<NameIdRow> findUniqueDepartmentIdsByName(
            @Param("names") Collection<String> names, @Param("companyId") Long companyId);

    /** 직급명(회사 범위 유니크) → (name, id). */
    List<NameIdRow> findJobPositionIdsByName(
            @Param("names") Collection<String> names, @Param("companyId") Long companyId);

    /** 전공명(회사 범위 유니크) → (name, id). */
    List<NameIdRow> findMajorIdsByName(
            @Param("names") Collection<String> names, @Param("companyId") Long companyId);

    /** 자격증명(회사 범위 유니크) → (name, id). */
    List<NameIdRow> findCertificateIdsByName(
            @Param("names") Collection<String> names, @Param("companyId") Long companyId);

    /** 요청 사번 중 실재하는(PK 점유) 사번 — soft delete 무관. */
    List<String> findExistingUserIds(@Param("userIds") Collection<String> userIds);
}
