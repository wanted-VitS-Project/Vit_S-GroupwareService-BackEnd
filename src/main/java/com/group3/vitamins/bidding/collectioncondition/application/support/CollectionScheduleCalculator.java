package com.group3.vitamins.bidding.collectioncondition.application.support;

import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionScheduleType;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class CollectionScheduleCalculator {

    // 등록 또는 수정 시점 이후의 첫 실행 시각을 계산합니다.
    public LocalDateTime firstRunAt(
            CollectionScheduleType scheduleType,
            LocalTime scheduledTime,
            LocalDateTime now
    ) {
        LocalDateTime candidate = now.toLocalDate().atTime(scheduledTime);
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }
        return movePastWeekend(scheduleType, candidate);
    }

    // 누락된 예약을 한 번만 처리하고 현재 시각 이후의 다음 회차를 계산합니다.
    public LocalDateTime nextRunAt(
            CollectionScheduleType scheduleType,
            LocalTime scheduledTime,
            LocalDateTime scheduledAt,
            LocalDateTime now
    ) {
        LocalDateTime candidate = scheduledAt.plusDays(1)
                .toLocalDate()
                .atTime(scheduledTime);

        while (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }
        return movePastWeekend(scheduleType, candidate);
    }

    private LocalDateTime movePastWeekend(
            CollectionScheduleType scheduleType,
            LocalDateTime candidate
    ) {
        if (scheduleType != CollectionScheduleType.WEEKDAYS) {
            return candidate;
        }
        while (candidate.getDayOfWeek() == DayOfWeek.SATURDAY
                || candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }
}
