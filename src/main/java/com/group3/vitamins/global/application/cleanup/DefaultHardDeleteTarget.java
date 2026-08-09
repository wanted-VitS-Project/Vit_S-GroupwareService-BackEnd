package com.group3.vitamins.global.application.cleanup;

import com.group3.vitamins.global.application.cleanup.port.HardDeleteOperation;
import com.group3.vitamins.global.application.cleanup.port.HardDeleteTarget;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Objects;

/** 도메인이 이름·보존기간·삭제동작 3개만 넘겨 등록할 수 있는 범용 {@link HardDeleteTarget} 구현체. */
public class DefaultHardDeleteTarget implements HardDeleteTarget {

    private final String targetName;
    private final TemporalAmount retention;
    private final HardDeleteOperation operation;

    public DefaultHardDeleteTarget(
            String targetName,
            TemporalAmount retention,
            HardDeleteOperation operation
    ) {
        this.targetName = requireText(targetName, "targetName");
        this.retention = Objects.requireNonNull(retention, "보존 기간은 null일 수 없습니다.");
        this.operation = Objects.requireNonNull(operation, "하드 딜리트 작업은 null일 수 없습니다.");

        if (isNotPositive(retention)) {
            throw new IllegalArgumentException("보존 기간은 0보다 커야 합니다.");
        }
    }

    @Override
    public String targetName() {
        return targetName;
    }

    @Override
    public TemporalAmount retention() {
        return retention;
    }

    @Override
    public int hardDeleteBefore(LocalDateTime threshold) {
        Objects.requireNonNull(threshold, "기준 시각은 null일 수 없습니다.");
        return operation.deleteBefore(threshold);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 비어 있을 수 없습니다.");
        }
        return value;
    }

    private boolean isNotPositive(TemporalAmount retention) {
        if (retention instanceof java.time.Duration duration) {
            return duration.isZero() || duration.isNegative();
        }

        if (retention instanceof Period period) {
            return period.isZero() || period.isNegative();
        }

        LocalDateTime now = LocalDateTime.now();
        return !now.minus(retention).isBefore(now);
    }
}
