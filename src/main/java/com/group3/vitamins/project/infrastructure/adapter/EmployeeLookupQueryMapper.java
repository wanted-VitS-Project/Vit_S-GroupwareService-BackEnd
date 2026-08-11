package com.group3.vitamins.project.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * {@code employee} 테이블 직접 조회 — 사번으로 이름만 필요하다 ({@code AuthQueryMapper} 선례).
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 (팀 MyBatis 컨벤션) —
 * {@code src/main/resources/mapper/project/EmployeeLookupQueryMapper.xml}.
 */
@Mapper
public interface EmployeeLookupQueryMapper {

    Optional<String> findNameByUserId(@Param("userId") String userId,
                                      @Param("companyId") Long companyId);

    List<EmployeeNameRow> findRefsByUserIds(@Param("userIds") Collection<String> userIds,
                                            @Param("companyId") Long companyId);
}