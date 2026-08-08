package com.group3.vitamins.global.application.cleanup;

import com.group3.vitamins.global.application.cleanup.port.HardDeleteTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
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

    private final List<HardDeleteTarget> targets;
    private final Clock clock;

    /** 단일 하드 딜리트 대상을 실행한다. */
    public int execute(HardDeleteTarget target) {
        Objects.requireNonNull(target, "하드 딜리트 대상은 null일 수 없습니다.");

        LocalDateTime now = LocalDateTime.now(clock);
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

    /** 스프링 빈으로 등록된 모든 하드 딜리트 대상을 실행한다. 개별 대상의 실패는 로그만 남기고 다음 대상으로 넘어간다. */
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
