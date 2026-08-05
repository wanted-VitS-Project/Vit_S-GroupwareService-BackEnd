package com.group3.vitamins.notification.application.port;

/**
 * 공용 block 테이블 조회 결과 중 알림 도메인이 필요로 하는 최소 정보.
 *
 * <p>{@code type} 은 {@code project.block.domain.model.BlockType} 의 {@code name()} 문자열이다 —
 * 알림 도메인이 그 enum 타입 자체에 의존하지 않게 문자열로만 받는다
 * (`approval.application.port.BlockSummary` 와 동일한 이유).
 */
public record BlockRef(String type, Long typeId) {
}
