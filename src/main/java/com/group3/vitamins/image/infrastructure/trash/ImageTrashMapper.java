package com.group3.vitamins.image.infrastructure.trash;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 이미지 휴지통 조회용 — image → image_block → block → step 을 타고 프로젝트에 속한
 * 삭제된 이미지만 배치 조회한다. 여러 테이블 조인이라 JPA 대신 MyBatis를 쓴다 (`.ai/docs/global/MYBATIS.md`).
 */
@Mapper
public interface ImageTrashMapper {

    List<ImageTrashRow> findTrashedByProjectId(@Param("projectId") Long projectId);

    /**
     * 복구·완전 삭제 전용 임시 우회 — imgBlockId가 속한 stepId를 block의 삭제 여부와 무관하게 찾는다.
     * 공유 {@code BlockCatalogPort.hasEditPermission}은 삭제된 블록을 못 찾아 권한 유무와 무관하게
     * 항상 false를 반환해서 이 경로로는 정확한 판정이 불가능하다 — 여기서 stepId만 뽑고, 실제 권한
     * 판정은 그대로 {@code StepAccessUseCase.requireEditable}(기존 메서드, 새로 만든 것 없음)에 넘긴다.
     *
     * <p>⚠️ 동훈님(Block 도메인) 쪽에 "삭제된 블록도 포함해서 stepId를 찾는" 정식 포트 메서드가 생기면
     * 그걸로 교체할 것 — 지금은 이미지 도메인 단독으로 처리하려고 둔 임시 우회다 (2026-08-06 결정,
     * `.ai/api/image.md` 참고).
     */
    Optional<Long> findStepIdByImgBlockId(@Param("imgBlockId") Long imgBlockId);
}
