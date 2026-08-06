package com.group3.vitamins.image.domain.repository;

import java.time.LocalDateTime;

/**
 * 이미지 블록 상세 행({@code image_block}) 포트.
 *
 * <p>그 행의 생성은 Block 도메인(동훈님)이 처리한다 — 블록 골격(block)과 상세 행(image_block)을
 * 한 번에 만든다. 하지만 삭제는 텍스트·체크리스트와 동일하게, Block 도메인이 발행하는 삭제 이벤트를
 * 이 도메인이 받아 {@code deleted_at} 을 직접 찍는다 (도메인마다 정리 로직이 달라 각자 처리).
 */
public interface ImageBlockRepository {

    /** 항목이 0개인 상세 행을 만들고 그 PK 를 돌려준다. */
    Long create(Long blockId);

    /**
     * 존재 확인 + 이번 트랜잭션이 끝날 때까지 이 행에 대한 동시 변경을 막는다.
     * 항목 생성 직전에 호출해서, "확인 → 생성" 사이에 블록이 동시에 삭제되는 것을 막는다.
     * (구현체는 잠금 방식을 자유롭게 고를 수 있다 — 현재는 비관적 락. 체크리스트와 동일한 이유)
     */
    boolean existsActive(Long imgBlockId);

    /**
     * 락 없는 존재 확인 — 조회(GET)처럼 읽기 전용 트랜잭션에서 쓴다. {@link #existsActive} 는
     * 비관적 락이라 읽기 전용 트랜잭션에서 부르면 DB가 거부한다.
     */
    boolean existsActiveReadOnly(Long imgBlockId);

    /** 활동 로그({@code ActivityOccurredEvent})에 실어야 하는 공용 block 테이블의 blockId. */
    Long getBlockId(Long imgBlockId);

    /**
     * @return 실제로 이번 호출이 삭제 처리했으면 true, 이미 삭제돼 있어 아무것도 안 했으면 false
     *         (중복 삭제 이벤트 판별용)
     */
    boolean markDeleted(Long imgBlockId, LocalDateTime deletedAt);
}
