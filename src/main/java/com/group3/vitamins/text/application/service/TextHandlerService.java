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

    /** 블록 생성 시 본문이 빈 상세 행을 만든다. 내용 채우기는 TextCommandService 소관이다. */
    public Long create(Long blockId) {
        Long txtId = textRepository.create(blockId);
        log.info("텍스트 블록 상세 행 생성 - blockId={}, txtId={}", blockId, txtId);
        return txtId;
    }

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

        // 활동 로그(블록 삭제, §5.1)는 여기서 발행하지 않는다 — 어댑터가 없는 타입(FILE·APPROVAL 등)의
        // 블록은 이 메서드 자체가 호출되지 않으므로, 이 위치에서 발행하면 그 타입들은 삭제 로그가
        // 영원히 누락된다. 모든 블록 타입에 공통 적용되는 로그이니, 실제 block.deleted_at 을 찍는
        // Block 도메인 쪽 삭제 서비스가 발행해야 한다.
    }
}
