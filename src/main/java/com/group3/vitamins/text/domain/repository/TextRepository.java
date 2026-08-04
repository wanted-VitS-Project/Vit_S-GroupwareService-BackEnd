package com.group3.vitamins.text.domain.repository;

import com.group3.vitamins.text.domain.model.Text;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 텍스트 도메인이 바라보는 영속성 포트. 구현체는 infrastructure/persistence 에 있다.
 *
 * <p>수정과 삭제를 하나의 범용 save() 로 묶지 않고 상태별로 분리한다 — 그렇지 않으면
 * 읽은 시점의 오래된 deletedAt 값을 수정 흐름이 그대로 다시 써서, 동시에 삭제된 행을
 * 되살릴 수 있다.
 */
public interface TextRepository {

    Text updateContent(Long txtId, String content);

    /**
     * @return 실제로 이번 호출이 삭제 처리했으면 true, 이미 삭제돼 있어 아무것도 안 했으면 false
     *         (중복 삭제 이벤트 판별용)
     */
    boolean markDeleted(Long txtId, LocalDateTime deletedAt);

    Optional<Text> findActiveByTxtId(Long txtId);
}
