package com.group3.vitamins.global.infrastructure.cleanup;

import com.group3.vitamins.global.application.cleanup.HardDeleteExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 등록된 모든 하드 딜리트 대상을 동일한 주기로 실행하는 전역 진입점.
 * 도메인은 이 클래스를 직접 건드리지 않고 {@code {Domain}CleanupConfig}에서
 * {@code HardDeleteTarget} 빈만 등록하면 자동으로 이 스케줄러가 실행해준다.
 * 컨벤션은 {@code .ai/docs/global/CLEANUP.md} 참고.
 */
@Component
@RequiredArgsConstructor
public class HardDeleteScheduler {

    private final HardDeleteExecutor hardDeleteExecutor;

    @Scheduled(cron = "${cleanup.hard-delete.cron:0 0 3 * * *}", zone = "Asia/Seoul")
    public void runDailyHardDelete() {
        hardDeleteExecutor.executeAll();
    }
}
