package com.group3.vitamins.project.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 프로젝트 상세 조회. 스텝 집계·카테고리·요청자 권한을 한 SQL 로 가져온다.
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 —
 * {@code src/main/resources/mapper/project/ProjectDetailQueryMapper.xml}.
 */
@Mapper
public interface ProjectDetailQueryMapper {

    List<ProjectDetailRow> findDetail(@Param("projectId") Long projectId,
                                      @Param("requesterUserId") String requesterUserId);
}