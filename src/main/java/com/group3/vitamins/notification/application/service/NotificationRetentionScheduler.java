package com.group3.vitamins.notification.application.service;

import com.group3.vitamins.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * RET-001 — 개별 삭제 API 대신, 생성된 지 일정 기간이 지난 알림을 매일 자동으로 논리 삭제한다.
 * 사용자가 직접 호출하는 REST 엔드포인트가 아니라 서버 내부 배치라 알림 API 명세에는 없다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetentionScheduler {

    /**
     * 보존 기간(개월). 운영 튜닝값이 아니라 {@code RET-001}이 정한 정책값이라 설정으로 빼지 않는다 —
     * 바뀌면 이 상수와 {@code NOTI-V1.md}의 수용 기준을 같이 고치는 PR이 필요하다.
     */
    private static final int RETENTION_MONTHS = 3;

    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "${notification.retention.cron:0 0 3 * * *}")
    @Transactional
    public void deleteExpiredNotifications() {
        LocalDateTime createdBefore = LocalDateTime.now().minusMonths(RETENTION_MONTHS);
        int deletedCount = notificationRepository.deleteCreatedBefore(createdBefore, LocalDateTime.now());

        log.info("알림 자동 정리 완료 - createdBefore={}, deletedCount={}", createdBefore, deletedCount);
    }
}
