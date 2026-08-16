package com.group3.vitamins.image.infrastructure.gallery;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 프로젝트 이미지 모아보기 조회용 — image → image_block → block → step 을 타고 프로젝트에 속한
 * 활성 이미지 전체를 배치 조회한다. 여러 테이블 조인이라 JPA 대신 MyBatis를 쓴다
 * (`.ai/docs/global/MYBATIS.md`). 트래시 조회(`ImageTrashMapper`)와 조인 체인은 같고
 * 삭제 필터 방향만 반대다(활성만 vs 삭제만) — 관심사가 달라 별도 파일로 분리했다.
 */
@Mapper
public interface ImageGalleryMapper {

    List<ImageGalleryRow> findActiveByProjectId(
            @Param("projectId") Long projectId, @Param("limit") int limit, @Param("offset") int offset);

    long countActiveByProjectId(@Param("projectId") Long projectId);
}
