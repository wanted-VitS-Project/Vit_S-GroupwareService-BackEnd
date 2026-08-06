package com.group3.vitamins.project.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 참여자 목록 조회 — {@code project_member} 에 {@code employee}·{@code department} 를 조인한다.
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 (팀 MyBatis 컨벤션) —
 * {@code src/main/resources/mapper/project/ProjectMemberQueryMapper.xml}.
 */
@Mapper
public interface ProjectMemberQueryMapper {

    List<ProjectMemberRow> findMembers(@Param("projectId") Long projectId);
}