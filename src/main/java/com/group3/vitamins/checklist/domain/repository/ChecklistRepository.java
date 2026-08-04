package com.group3.vitamins.checklist.domain.repository;

import com.group3.vitamins.checklist.domain.model.ChecklistItem;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 체크리스트 항목(checklist) 도메인이 바라보는 영속성 포트. 구현체는 infrastructure/catalog 에 있다.
 *
 * <p>수정과 삭제를 하나의 범용 save() 로 묶지 않고 상태별로 분리한다 — 그렇지 않으면
 * 읽은 시점의 오래된 deletedAt 값을 수정 흐름이 그대로 다시 써서, 동시에 삭제된 행을
 * 되살릴 수 있다 (텍스트 도메인과 동일한 원칙).
 */
public interface ChecklistRepository {

    ChecklistItem create(Long chkBlockId, String content);

    /**
     * content/completed 중 null 이 아닌 값만 반영한다 (부분 수정).
     *
     * @return 반영 후의 항목. 대상이 이미 삭제돼 있으면 NotFoundException 을 던진다.
     */
    ChecklistItem updateFields(Long chkId, String content, Boolean completed);

    /**
     * @return 실제로 이번 호출이 삭제 처리했으면 true, 이미 삭제돼 있어 아무것도 안 했으면 false
     */
    boolean markDeleted(Long chkId, LocalDateTime deletedAt);

    /**
     * 체크리스트 블록 삭제 이벤트로 그 블록에 속한 활성 항목을 일괄 소프트 삭제한다.
     *
     * @return 실제로 삭제 처리된 항목 수 (중복 이벤트 판별용)
     */
    int markAllDeletedByBlock(Long chkBlockId, LocalDateTime deletedAt);

    Optional<ChecklistItem> findActiveByChkId(Long chkId);

    /** 블록에 속한 활성 항목 전체 개수 */
    int countActiveItems(Long chkBlockId);

    /** 블록에 속한 활성 항목 중 완료된 개수 */
    int countCompletedActiveItems(Long chkBlockId);
}
