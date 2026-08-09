package com.group3.vitamins.global.application.cleanup;

import com.group3.vitamins.global.application.cleanup.port.HardDeleteTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * 등록된 모든 {@link HardDeleteTarget}의 보존 기간을 기준으로 삭제 기준 시각을 계산하고 실행한다.
 * 대상 하나가 실패해도 나머지 대상 실행에 영향을 주지 않도록 {@link #executeAll()}에서 예외를 격리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HardDeleteExecutor {

    private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Asia/Seoul");

    private final List<HardDeleteTarget> targets;
    private final Clock clock;

    /**
     * 단일 하드 딜리트 대상을 실행한다.
     * 커스텀 {@code @Modifying} 삭제 쿼리는 자동으로 트랜잭션이 걸리지 않으므로, 이 호출 전체를
     * {@link #executeAll()}의 트랜잭션 경계 안에서 실행한다(자기 자신을 통한 내부 호출이라도
     * {@code executeAll()}이 이미 연 트랜잭션에 그대로 참여한다).
     */
    public int execute(HardDeleteTarget target) {
        Objects.requireNonNull(target, "하드 딜리트 대상은 null일 수 없습니다.");

        // Clock 빈의 시간대가 무엇이든(예: systemDefaultZone) 스케줄은 항상 Asia/Seoul 기준이라,
        // 기준 시각도 항상 Asia/Seoul 벽시계로 계산한다.
        LocalDateTime now = LocalDateTime.now(clock.withZone(SCHEDULE_ZONE));
        LocalDateTime threshold = now.minus(target.retention());

        int deletedCount = target.hardDeleteBefore(threshold);

        log.info(
                "하드 딜리트가 완료되었습니다. 대상={}, 보존기간={}, 삭제기준시각={}, 삭제건수={}",
                target.targetName(),
                target.retention(),
                threshold,
                deletedCount
        );

        return deletedCount;
    }

    /**
     * 스프링 빈으로 등록된 모든 하드 딜리트 대상을 실행한다. 개별 대상의 실패는 로그만 남기고 다음 대상으로 넘어간다.
     * 이 메서드에 트랜잭션 경계를 둔다 — 리포지토리의 커스텀 {@code @Modifying} 쿼리(§2-1 패턴)는
     * 스스로 트랜잭션을 열지 않아서, 여기서 안 열면 매일 밤 {@code TransactionRequiredException}이 나고
     * 그게 아래 {@code catch}에 조용히 삼켜져 "정상 실행"처럼 로그만 남고 실제로는 삭제가 하나도 안 된다.
     */
    @Transactional
    public int executeAll() {
        if (targets.isEmpty()) {
            log.debug("등록된 하드 딜리트 대상이 없습니다.");
            return 0;
        }

        int totalDeletedCount = 0;

        for (HardDeleteTarget target : targets) {
            try {
                totalDeletedCount += execute(target);
            } catch (Exception exception) {
                log.error("하드 딜리트 실행 중 오류가 발생했습니다. 대상={}", target.targetName(), exception);
            }
        }

        return totalDeletedCount;
    }
}
