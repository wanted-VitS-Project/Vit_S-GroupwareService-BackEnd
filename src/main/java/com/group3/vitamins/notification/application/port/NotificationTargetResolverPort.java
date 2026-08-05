package com.group3.vitamins.notification.application.port;

import java.util.Optional;

/**
 * 타입별 확장(SPI) — VIW-006/GEN-003. 새 도메인(이슈 등)이 이동 대상 조회를 지원하려면
 * 이 인터페이스를 구현한 어댑터를 {@code infrastructure/adapter} 에 추가하는 것으로 끝난다.
 * 알림 도메인 코드(서비스)는 구현체를 컴파일 타임에 알지 않는다 — 스프링이 주입한 목록에서
 * {@link #supportedType()} 으로 고른다 (`project.block.application.port.BlockDetailPort` 와 동일 패턴).
 */
public interface NotificationTargetResolverPort {

    /** 이 어댑터가 담당하는 block 타입 이름 (예: {@code "APPROVAL"}). */
    String supportedType();

    /** typeId(= block.type_id) 로 실제 이동 대상을 조회한다. 대상이 이미 없어졌으면 empty. */
    Optional<NotificationTarget> resolve(Long typeId);
}
