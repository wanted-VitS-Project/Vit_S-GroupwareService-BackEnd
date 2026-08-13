package com.group3.vitamins.employee.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 전공·자격증 마스터 존재 확인 MyBatis 매퍼 (`employee.md` §3·§4 · qualification.md). SQL 은
 * {@code src/main/resources/mapper/employee/QualificationReferenceQueryMapper.xml} 에 둔다 (팀 MyBatis 컨벤션).
 */
@Mapper
public interface QualificationReferenceQueryMapper {

    List<Long> findExistingMajorIds(@Param("majorIds") Collection<Long> majorIds, @Param("companyId") Long companyId);

    List<Long> findExistingCertificateIds(@Param("certificateIds") Collection<Long> certificateIds,
                                          @Param("companyId") Long companyId);
}
