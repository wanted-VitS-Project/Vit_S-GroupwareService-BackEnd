package com.group3.vitamins.settlement.application.service;

import com.group3.vitamins.settlement.application.port.SettlementProjectLookupPort;
import com.group3.vitamins.settlement.domain.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 정산 블록의 빈 상세 행 생성·삭제 — Block 도메인이 {@code BlockDetailPort}
 * ({@link com.group3.vitamins.settlement.infrastructure.blockdetail.SettlementBlockDetailAdapter})로
 * 호출한다. 내용(항목) 작성/수정은 {@link SettlementCommandService} 소관이다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SettlementHandlerService {

    private final SettlementRepository settlementRepository;
    private final SettlementProjectLookupPort settlementProjectLookupPort;

    /**
     * 블록 생성 시 내용이 빈 정산 상세 행을 만든다. 내용 채우기는 SettlementCommandService 소관이다.
     *
     * <p>project_id 를 여기서 찾아 채운다 — 조회 경로(진행률 집계)가 이 값을 조인 없이 읽는다.
     */
    public Long create(Long blockId) {
        Long projectId = settlementProjectLookupPort.findProjectIdByBlockId(blockId);
        if (projectId == null) {
            // 방금 만든 블록의 스텝을 못 찾는 상황이라 정상 흐름에서는 나올 수 없다.
            // 여기서 막지 않으면 NOT NULL 위반으로 INSERT 가 터지는데, 그때는 원인이 안 보인다.
            throw new IllegalStateException(
                    "정산 블록의 프로젝트를 찾지 못했다 - blockId=" + blockId);
        }

        Long settleId = settlementRepository.create(blockId, projectId);
        log.info("정산 블록 상세 행 생성 - blockId={}, projectId={}, settleId={}", blockId, projectId, settleId);
        return settleId;
    }

    public void delete(Long settleId, String userId, String blockTitle, LocalDateTime deletedAt) {
        log.info("정산 블록 삭제(이벤트 수신) - settleId={}, userId={}", settleId, userId);

        // deletedAt 이 null 이면 "WHERE deleted_at IS NULL" 조건에 걸려 SET deleted_at = NULL 로
        // 아무 값도 안 바뀌었는데 영향받은 행이 있다는 이유로 삭제 성공(true)으로 오판할 수 있다.
        Objects.requireNonNull(deletedAt, "deletedAt은 null일 수 없습니다.");

        boolean deleted = settlementRepository.markDeleted(settleId, deletedAt);
        if (!deleted) {
            // 조건부 UPDATE가 0건 갱신 = 이미 삭제돼 있었다는 뜻. 이벤트가 중복 전달됐을 때
            // 정상적으로 발생할 수 있는 상황이라 에러로 취급하지 않고 멱등하게 무시한다.
            log.info("이미 삭제 처리된 정산 블록 - 중복 이벤트로 판단하고 무시 - settleId={}", settleId);
            return;
        }

        log.info("정산 블록 삭제 완료 - settleId={}", settleId);

        // 활동 로그(블록 삭제)는 여기서 발행하지 않는다 — text/checklist 도메인과 동일한 이유로,
        // 어댑터가 없는 타입까지 커버해야 하는 로그라 실제 block.deleted_at 을 찍는
        // Block 도메인 쪽 삭제 서비스가 발행해야 한다.
    }
}
