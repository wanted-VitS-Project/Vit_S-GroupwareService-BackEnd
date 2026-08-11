package com.group3.vitamins.project.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 참여자를 사원·부서와 함께 조회한다.
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 —
 * {@code src/main/resources/mapper/project/ProjectMemberQueryMapper.xml}.
 */
@Mapper
public interface ProjectMemberQueryMapper {

    List<ProjectMemberRow> findMembers(@Param("projectId") Long projectId,
                                       @Param("companyId") Long companyId);
}