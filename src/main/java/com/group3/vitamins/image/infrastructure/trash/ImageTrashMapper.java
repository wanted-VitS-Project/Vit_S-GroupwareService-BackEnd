package com.group3.vitamins.image.infrastructure.trash;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 이미지 휴지통 조회용 — image → image_block → block → step 을 타고 프로젝트에 속한
 * 삭제된 이미지만 배치 조회한다. 여러 테이블 조인이라 JPA 대신 MyBatis를 쓴다 (`.ai/docs/global/MYBATIS.md`).
 */
@Mapper
public interface ImageTrashMapper {

    List<ImageTrashRow> findTrashedByProjectId(@Param("projectId") Long projectId);
}
