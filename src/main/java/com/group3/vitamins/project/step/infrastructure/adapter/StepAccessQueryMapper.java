package com.group3.vitamins.project.step.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 스텝 접근 판정 재료 조회. 스텝 · 프로젝트(회사 경계) · 참여자 권한 · 스텝 오버라이드를 한 SQL 로 가져온다.
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 —
 * {@code src/main/resources/mapper/step/StepAccessQueryMapper.xml}.
 */
@Mapper
public interface StepAccessQueryMapper {

    /** 스텝이 없거나 논리 삭제됐으면 {@code null}. */
    StepAccessRow findAccess(@Param("stepId") Long stepId,
                             @Param("requesterUserId") String requesterUserId,
                             @Param("companyId") Long companyId);
}
