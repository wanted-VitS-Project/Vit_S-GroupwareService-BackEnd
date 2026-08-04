package com.group3.vitamins.text.application.service;

import com.group3.vitamins.text.application.command.DeleteTextBlockCommand;
import com.group3.vitamins.text.application.policy.TextEligibilityPolicy;
import com.group3.vitamins.text.domain.model.Text;
import com.group3.vitamins.text.domain.repository.TextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TextHandlerService {
    private final TextEligibilityPolicy eligibilityPolicy;
    private final TextRepository textRepository;


    public void delete(Long txtId, String userId, String blockTitle, LocalDateTime deletedAt) {
        log.info("텍스트 블록 삭제(이벤트 수신) - txtId={}, userId={}", txtId, userId);

        Text text = eligibilityPolicy.getActiveTextOrThrow(txtId);

        text.markDeleted(deletedAt);
        textRepository.save(text);

        log.info("텍스트 블록 삭제 완료 - txtId={}", txtId);

        // TODO: 활동 로그(블록 삭제) 이벤트 발행 — 활동 로그 인프라(ActivityOccurredEvent 등)가 아직
        //       실제로 만들어지지 않아 주석으로만 남긴다. resourceId=null, 기록 정보=삭제 전 Block명 (§5.1 Block 공통).
        //       blockId 는 text.getBlockId() 로 바로 읽으면 된다 (컬럼으로 저장돼 있음).
        // activityEventPublisher.publish(
        //         ActivityOccurredEvent.deleted(text.getBlockId(), null, userId, blockTitle)
        // );
    }
}
