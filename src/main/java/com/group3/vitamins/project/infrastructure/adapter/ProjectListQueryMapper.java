package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.application.query.ProjectListCriteria;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 프로젝트 목록 조회. 접근 범위·필터·페이징을 SQL 에서 처리한다.
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 —
 * {@code src/main/resources/mapper/project/ProjectListQueryMapper.xml}.
 */
@Mapper
public interface ProjectListQueryMapper {

    List<ProjectListRow> findPage(ProjectListCriteria criteria);

    long count(ProjectListCriteria criteria);
}