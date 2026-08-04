package com.group3.vitamins.checklist.domain.repository;

import java.time.LocalDateTime;

/**
 * 체크리스트 블록 상세 행({@code checklist_block}) 포트.
 *
 * <p>그 행의 생성은 Block 도메인(동훈님)이 처리한다 — 블록 골격(block)과 상세 행(checklist_block)을
 * 한 번에 만든다. 하지만 삭제는 텍스트 도메인과 동일하게, Block 도메인이 발행하는 삭제 이벤트를
 * 이 도메인이 받아 {@code deleted_at} 을 직접 찍는다 (도메인마다 정리 로직이 달라 각자 처리).
 */
public interface ChecklistBlockRepository {

    /**
     * 존재 확인 + 이번 트랜잭션이 끝날 때까지 이 행을 잠근다(PESSIMISTIC_WRITE).
     * 항목 생성 직전에 호출해서, "확인 → 생성" 사이에 블록이 동시에 삭제되는 것을 막는다.
     */
    boolean existsActive(Long chkBlockId);

    /**
     * @return 실제로 이번 호출이 삭제 처리했으면 true, 이미 삭제돼 있어 아무것도 안 했으면 false
     *         (중복 삭제 이벤트 판별용)
     */
    boolean markDeleted(Long chkBlockId, LocalDateTime deletedAt);
}
