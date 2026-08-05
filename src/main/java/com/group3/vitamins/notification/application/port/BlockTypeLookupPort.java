package com.group3.vitamins.notification.application.port;

import java.util.Optional;

/**
 * Block 도메인(동훈님 소관)에 물어보는 아웃바운드 포트 — VIW-007(이동 대상은 block 경유로 판정).
 * 알림 도메인은 이 인터페이스만 알고, 실제 조회는 infrastructure/adapter 구현체가 처리한다
 * (`approval.application.port.BlockCatalogPort` 와 동일 구조).
 */
public interface BlockTypeLookupPort {

    /** blockId 로 공용 block 테이블을 조회한다. 삭제된 블록은 존재하지 않는 것으로 취급한다. */
    Optional<BlockRef> findBlock(Long blockId);
}
