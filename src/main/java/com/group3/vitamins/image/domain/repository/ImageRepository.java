package com.group3.vitamins.image.domain.repository;

import com.group3.vitamins.image.domain.model.ImageItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 이미지 항목(image) 도메인이 바라보는 영속성 포트. 구현체는 infrastructure/catalog 에 있다.
 */
public interface ImageRepository {

    /** 업로드가 끝난 항목들을 한 번에 저장한다 (한 요청의 files 전체를 하나의 배치로 처리). */
    List<ImageItem> createAll(List<ImageItem> items);

    /** 블록에 속한 활성 항목의 현재 최대 orderIndex. 없으면 0 (다음 항목은 이 값 + 1부터). */
    int findMaxOrderIndex(Long imgBlockId);

    /** 블록에 속한 활성 항목 전체. 수정 요청이 보낸 목록과 대조(IMG-005)하고 변경 전 값을 확보하는 용도. */
    List<ImageItem> findAllActiveByImgBlockId(Long imgBlockId);

    /** 단건 삭제 API용 — imgId 하나로 활성 항목을 찾는다(속한 imgBlockId 확인·권한 판정에 씀). */
    Optional<ImageItem> findActiveByImgId(Long imgId);

    /** @return 실제로 갱신된 행 수(0 또는 1) — 대상이 이미 삭제돼 있으면 0 */
    int updateCaptionAndOrder(Long imgId, Long imgBlockId, String caption, int orderIndex);

    /**
     * 수정 요청 배열에서 빠진 이미지 = 삭제로 간주한다 (2026-08-04 결정). 소프트 삭제만 한다 —
     * S3 객체는 지우지 않는다(하드 삭제 정책이 나오기 전까지 보류, `.ai/api/image.md` 참고).
     *
     * @return 실제로 삭제 처리된 행 수(0 또는 1) — 대상이 이미 삭제돼 있으면 0
     */
    int markDeleted(Long imgId, Long imgBlockId, LocalDateTime deletedAt);

    /**
     * 블록 삭제 이벤트로 인한 일괄 삭제 — 그 블록의 활성 항목 전부를 소프트 삭제한다.
     *
     * @return 실제로 삭제 처리된 항목 수 (중복 이벤트 판별용)
     */
    int markAllDeletedByBlock(Long imgBlockId, LocalDateTime deletedAt);
}
