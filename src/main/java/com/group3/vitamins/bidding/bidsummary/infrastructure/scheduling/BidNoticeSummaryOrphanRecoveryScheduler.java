package com.group3.vitamins.bidding.bidsummary.infrastructure.scheduling;

import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryWorkerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// 요약과 outbox는 같은 트랜잭션에 저장돼 정상 흐름에서는 나오지 않아야 하는 상태지만, 발행 경로
// 자체가 유실된 채 멈춘 요약을 방어적으로 훑어 재큐잉한다(vitamate 파일 인덱싱 재발행과 동일 취지 -
// 2026-08-14 프론트 리포트, "발행을 한 번 놓치면 복구 경로가 없다").
@Slf4j
@Component
@RequiredArgsConstructor
public class BidNoticeSummaryOrphanRecoveryScheduler {

    // 정상 처리는 보통 1초 주기 Outbox 스케줄러가 곧바로 집어가므로, 이 시간을 넘겨도
    // 살아있는 outbox가 없으면 발행 경로 자체가 유실된 것으로 간주한다.
    private static final long STALE_MINUTES = 10;

    // 한 번에 복구할 최대 건수 - backlog가 커져도 후보 목록과 로그가 무한히 늘지 않게 한다.
    private static final int BATCH_LIMIT = 100;

    private final BidNoticeSummaryWorkerPort workerPort;

    @Scheduled(fixedDelayString = "${bidding.summary.orphan-recovery.fixed-delay-ms:300000}")
    public void recoverOrphanedSummaries() {
        LocalDateTime now = LocalDateTime.now();
        int recovered = workerPort.recoverOrphaned(
                now.minusMinutes(STALE_MINUTES), BATCH_LIMIT, now
        );

        if (recovered > 0) {
            log.warn(
                    "Bidding summary orphan recovery requeued or failed stuck summaries. recoveredCount={}",
                    recovered
            );
        }
    }
}
