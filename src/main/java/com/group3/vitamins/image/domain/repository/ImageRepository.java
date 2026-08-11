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

    /**
     * 기대 버전과 DB 버전이 같을 때만 캡션·순서를 바꾼다(CONCURRENCY.md §4 — 목록 통째 전송 API는
     * 항목별 version + 전체 롤백). 바뀐 행 수를 돌려준다.
     *
     * @return 실제로 갱신된 행 수(0 또는 1) — 대상이 이미 삭제됐거나 그 사이 남이 먼저 저장했으면 0
     */
    int updateCaptionAndOrderIfVersionMatches(Long imgId, Long imgBlockId, String caption, int orderIndex,
                                               int expectedVersion);

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

    /** orderIndex 초과인 활성 항목 중 최솟값 하나. 없으면(=맨 마지막) empty — 순환 여부는 서비스가 판단한다. */
    Optional<ImageItem> findNextByOrderIndex(Long imgBlockId, int orderIndex);

    /** orderIndex 미만인 활성 항목 중 최댓값 하나. 없으면(=맨 처음) empty — 순환 여부는 서비스가 판단한다. */
    Optional<ImageItem> findPreviousByOrderIndex(Long imgBlockId, int orderIndex);

    /** orderIndex가 가장 작은 활성 항목(첫 이미지). "다음"이 없을 때 처음으로 순환하는 데 쓴다. */
    Optional<ImageItem> findFirstActive(Long imgBlockId);

    /** orderIndex가 가장 큰 활성 항목(마지막 이미지). "이전"이 없을 때 마지막으로 순환하는 데 쓴다. */
    Optional<ImageItem> findLastActive(Long imgBlockId);

    /** 블록에 속한 활성 항목 총 개수. */
    int countActive(Long imgBlockId);

    /** 그 블록에 이 orderIndex를 가진 활성 항목이 실제로 있는지 — 다음/이전 조회 전 currentOrderIndex 검증용. */
    boolean existsActiveByOrderIndex(Long imgBlockId, int orderIndex);

    /**
     * imgId 목록에 대응하는 항목을 활성·삭제 여부 상관없이 전부 찾는다. 복구 API의 검증용 —
     * 존재 자체가 없는 imgId와 이미 활성 상태인 imgId를 구분하려면 삭제 필터 없는 조회가 필요하다.
     */
    List<ImageItem> findAllByImgIds(List<Long> imgIds);

    /**
     * 소프트 삭제된 항목을 복구한다(deletedAt → null, orderIndex 갱신, updatedAt 갱신). "확인 후 쓰기"가
     * 아니라 조건부 UPDATE로 원자 처리한다 — 대상이 이미 활성이거나 존재하지 않으면 0건.
     *
     * @return 실제로 복구된 행 수(0 또는 1)
     */
    int restore(Long imgId, Long imgBlockId, int orderIndex);

    /**
     * 완전 삭제(하드 삭제) — 행 자체를 지운다. 되돌릴 수 없다. 소프트 삭제된(휴지통) 항목만 대상으로
     * 조건부 DELETE로 원자 처리한다 — 대상이 활성 상태거나 존재하지 않으면 0건.
     *
     * @return 실제로 삭제된 행 수(0 또는 1)
     */
    int hardDelete(Long imgId);
}
