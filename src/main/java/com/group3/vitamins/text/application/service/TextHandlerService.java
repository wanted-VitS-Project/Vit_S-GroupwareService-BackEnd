package com.group3.vitamins.text.application.service;

import com.group3.vitamins.text.domain.repository.TextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TextHandlerService {

    private final TextRepository textRepository;

    public void delete(Long txtId, String userId, String blockTitle, LocalDateTime deletedAt) {
        log.info("텍스트 블록 삭제(이벤트 수신) - txtId={}, userId={}", txtId, userId);

        // deletedAt 이 null 이면 "WHERE deleted_at IS NULL" 조건에 걸려 SET deleted_at = NULL 로
        // 아무 값도 안 바뀌었는데 영향받은 행이 있다는 이유로 삭제 성공(true)으로 오판할 수 있다.
        Objects.requireNonNull(deletedAt, "deletedAt은 null일 수 없습니다.");

        boolean deleted = textRepository.markDeleted(txtId, deletedAt);
        if (!deleted) {
            // 조건부 UPDATE가 0건 갱신 = 이미 삭제돼 있었다는 뜻. 이벤트가 중복 전달됐을 때
            // 정상적으로 발생할 수 있는 상황이라 에러로 취급하지 않고 멱등하게 무시한다.
            log.info("이미 삭제 처리된 텍스트 블록 - 중복 이벤트로 판단하고 무시 - txtId={}", txtId);
            return;
        }

        log.info("텍스트 블록 삭제 완료 - txtId={}", txtId);

        // TODO: 활동 로그(블록 삭제) 이벤트 발행 — 활동 로그 인프라(ActivityOccurredEvent 등)가 아직
        //       실제로 만들어지지 않아 주석으로만 남긴다. resourceId=null, 기록 정보=삭제 전 Block명 (§5.1 Block 공통).
        //       blockId 가 필요하면 markDeleted 이전에 findActiveByTxtId 로 미리 읽어두거나,
        //       삭제된 행도 조회 가능한 별도 메서드를 추가해야 한다 (지금은 조회 안 함).
        // activityEventPublisher.publish(
        //         ActivityOccurredEvent.deleted(blockId, null, userId, blockTitle)
        // );
    }
}
