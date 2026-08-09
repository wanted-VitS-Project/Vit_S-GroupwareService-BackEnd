package com.group3.vitamins.project.step.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 이슈 애그리게이트 소관 테이블({@code issue}) 직접 조회 — 스텝 진척률 집계용.
 * 이슈가 0건인 스텝은 결과 행이 없다.
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 (팀 MyBatis 컨벤션) —
 * {@code src/main/resources/mapper/step/IssueStatQueryMapper.xml}.
 */
@Mapper
public interface IssueStatQueryMapper {

    List<IssueStatRow> countByStepIds(@Param("stepIds") Collection<Long> stepIds);

    List<Long> findOpenIssueIds(@Param("stepId") Long stepId);
}
