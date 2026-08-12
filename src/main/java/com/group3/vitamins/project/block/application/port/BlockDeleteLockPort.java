package com.group3.vitamins.project.block.application.port;

import com.group3.vitamins.project.block.domain.model.BlockType;

/**
 * 삭제 잠금 4종의 확장점 (BLK-008 · BLOCK.md §8). 블록 도메인은 잠금 조건을 알지 않는다 —
 * ① 입금 연결 입금확인 ② 계산서 연결 조회 ③ 진행 중 결재 ④ 결재 대상 파일 은 전부 남의 테이블 사정이다.
 *
 * <p>구현체가 없으면 그 타입은 잠금 없이 삭제된다 (500 이 아니다). 담당자가 어댑터만 추가하면
 * 블록 도메인 코드는 한 줄도 안 바뀐다 — BlockDetailPort 와 같은 방식이다.
 *
 * <p>⚠️ supportedType() 은 타입당 여러 개가 허용된다. FILE 블록의 잠금은 파일 도메인이 아니라
 * 결재 도메인이 안다 — 한 타입을 두 도메인이 각자 잠글 수 있다.
 */
public interface BlockDeleteLockPort {

    /** 이 어댑터가 잠금을 판정하는 블록 타입. */
    BlockType supportedType();

    /**
     * 삭제가 잠겼는지 판정한다. typeId 는 상세 PK 로, 상세 행이 없는 타입(FILE·TAX_INVOICE_VIEW)은
     * null 이다 — 그런 타입은 blockId 로 판정해야 한다.
     */
    boolean isDeleteLocked(Long blockId, Long typeId);
}