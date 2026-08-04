package com.group3.vitamins.checklist.application.service;

import com.group3.vitamins.checklist.domain.repository.ChecklistBlockRepository;
import com.group3.vitamins.checklist.domain.repository.ChecklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChecklistHandlerService {

    private final ChecklistBlockRepository checklistBlockRepository;
    private final ChecklistRepository checklistRepository;

    /**
     * 체크리스트 블록 삭제 이벤트 수신.
     *
     * <p>텍스트 도메인이 자기 소유 detail 행(text)의 deleted_at 을 직접 찍는 것과 동일하게,
     * 이 도메인도 자기 소유 detail 행(checklist_block)의 deleted_at 을 직접 찍는다.
     * 다만 체크리스트는 1:N 이라 그 블록에 속한 항목(checklist)들까지 함께 정리해야 한다.
     */
    public void deleteByBlock(Long chkBlockId, String userId, String blockTitle, LocalDateTime deletedAt) {
        log.info("체크리스트 블록 삭제(이벤트 수신) - chkBlockId={}, userId={}", chkBlockId, userId);

        boolean blockDeleted = checklistBlockRepository.markDeleted(chkBlockId, deletedAt);
        if (!blockDeleted) {
            // 조건부 UPDATE가 0건 갱신 = 이미 삭제돼 있었다는 뜻. 중복 이벤트로 판단하고 멱등하게 무시한다.
            log.info("이미 삭제 처리된 체크리스트 블록 - 중복 이벤트로 판단하고 무시 - chkBlockId={}", chkBlockId);
            return;
        }

        int deletedItemCount = checklistRepository.markAllDeletedByBlock(chkBlockId, deletedAt);

        log.info("체크리스트 블록 삭제 완료 - chkBlockId={}, 삭제된 항목 수={}", chkBlockId, deletedItemCount);

        // TODO: 활동 로그(블록 삭제) 이벤트 발행 — 활동 로그 인프라(ActivityOccurredEvent 등)가 아직
        //       실제로 만들어지지 않아 주석으로만 남긴다. resourceId=null, 기록 정보=삭제 전 Block명 (§5.1 Block 공통).
        //       블록에 속한 항목 각각의 삭제는 별도로 로그를 남기지 않는다 — Block 삭제 활동으로 대표된다.
        //       blockId 가 필요하면 markDeleted 이전에 BlockCatalogPort 등으로 미리 읽어둬야 한다.
        // activityEventPublisher.publish(
        //         ActivityOccurredEvent.deleted(blockId, null, userId, blockTitle)
        // );
    }
}
