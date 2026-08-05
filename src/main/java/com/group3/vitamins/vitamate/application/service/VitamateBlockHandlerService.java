package com.group3.vitamins.vitamate.application.service;

import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.vitamate.domain.repository.VitamateBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

// Block 도메인이 AI 블록 생명주기를 맞출 때 호출하는 비타메이트 상세 처리 서비스
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VitamateBlockHandlerService {

    private final VitamateBlockRepository vitamateBlockRepository;
    private final DomainEventPublisher domainEventPublisher;

    // AI 블록 생성 시 빈 vitamate_block 행을 만들고 생성 로그를 남긴다.
    public Long create(Long blockId, String userId) {
        Long vitamateBlockId = vitamateBlockRepository.create(blockId);
        log.info("비타메이트 블록 상세 행 생성 - blockId={}, vitamateBlockId={}, userId={}",
                blockId, vitamateBlockId, userId);

        publishBlockDetailEvent(ActivityLogAction.CREATE, blockId, vitamateBlockId, userId);
        return vitamateBlockId;
    }

    // Block 삭제와 같은 트랜잭션에서 AI 블록 상세 행을 논리 삭제하고 삭제 로그를 남긴다.
    public void delete(Long vitamateBlockId, String userId, String blockTitle, LocalDateTime deletedAt) {
        log.info("비타메이트 블록 삭제 요청 - vitamateBlockId={}, userId={}", vitamateBlockId, userId);

        Objects.requireNonNull(deletedAt, "deletedAt은 null일 수 없습니다.");

        Long blockId = vitamateBlockRepository.findBlockId(vitamateBlockId)
                .orElse(null);
        boolean deleted = vitamateBlockRepository.markDeleted(vitamateBlockId, deletedAt);
        if (!deleted) {
            log.info("이미 삭제 처리된 비타메이트 블록 - 중복 호출로 판단하고 무시 - vitamateBlockId={}", vitamateBlockId);
            return;
        }

        log.info("비타메이트 블록 삭제 완료 - vitamateBlockId={}", vitamateBlockId);

        if (blockId == null) {
            log.warn("비타메이트 블록 삭제 로그 생략 - blockId를 찾지 못함, vitamateBlockId={}", vitamateBlockId);
            return;
        }

        publishBlockDetailEvent(ActivityLogAction.DELETE, blockId, vitamateBlockId, userId);
    }

    // AI 블록 상세 row의 생성·삭제는 변경 필드가 없으므로 null change 1개로 기록한다.
    private void publishBlockDetailEvent(
            ActivityLogAction action,
            Long blockId,
            Long vitamateBlockId,
            String userId
    ) {
        domainEventPublisher.publish(ActivityOccurredEvent.of(
                action,
                blockId,
                vitamateBlockId,
                userId,
                List.of(new ActivityFieldChange(null, null, null))
        ));
    }
}
