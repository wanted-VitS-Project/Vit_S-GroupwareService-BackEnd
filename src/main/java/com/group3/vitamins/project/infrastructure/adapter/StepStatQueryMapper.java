package com.group3.vitamins.project.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 스텝 애그리게이트 소관 테이블({@code step}) 직접 조회.
 * 진척률은 프로젝트의 속성이라(PRJ-013) 집계는 프로젝트 쪽에서 한다 — 명세도 진척률 조회를 Project 도메인으로 분류한다.
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 (팀 MyBatis 컨벤션) —
 * {@code src/main/resources/mapper/project/StepStatQueryMapper.xml}.
 */
@Mapper
public interface StepStatQueryMapper {

    StepStatRow countByProjectId(@Param("projectId") Long projectId);
}