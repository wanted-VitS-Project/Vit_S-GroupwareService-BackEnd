package com.group3.vitamins.settlement.application.port;

/**
 * 정산 블록 상세 행을 만들 때 스탬핑할 {@code project_id} 를 찾는 조회 전용 포트.
 *
 * <p>왜 정산이 직접 찾나 — 공용 확장점 {@code BlockDetailPort.createDetail(Long blockId)} 은
 * blockId 만 넘긴다. 시그니처에 projectId 를 추가하면 구현체 6개(approval·checklist·image·
 * settlement·text·vitamate)를 전부 고쳐야 하는데, 그 값이 필요한 건 정산 하나뿐이다.
 * 블록 생성은 드문 쓰기라 조회 1회를 정산 쪽에서 감당하는 편이 싸다.
 *
 * <p>{@link SettlementSiblingLookupPort} 와 분리한다 — 저쪽은 이미 만들어진 정산 블록의
 * "같은 프로젝트 형제"를 훑는 비즈니스 조회고, 이건 생성 직전에 소속을 확인하는 조회다.
 */
public interface SettlementProjectLookupPort {

    /**
     * 이 블록이 놓인 스텝의 프로젝트 아이디. 블록·스텝이 없으면 null.
     *
     * <p>삭제 여부는 보지 않는다 — 블록 생성 트랜잭션 안에서 방금 만든 블록을 조회하는 용도라
     * 삭제 상태일 수 없고, 여기에 {@code deleted_at} 조건을 걸면 동시 삭제와 경합할 뿐이다.
     */
    Long findProjectIdByBlockId(Long blockId);
}
