-- Add SETTLEMENT as a block type (BID_NOTICE 선례와 동일한 방식 — 담당자 본인 브랜치에서 enum 값 추가).
-- 상세 테이블: settlement_block (다형성 역방향 · block.type_id -> settlement_block.settle_id).
-- ⚠️ PAYMENT_CONFIRM · TAX_INVOICE_VIEW 값은 그대로 둔다 — 이번 작업 범위 밖(Block 도메인 담당자 결정 사항).

ALTER TABLE block
    MODIFY COLUMN type ENUM(
        'TEXT',
        'IMAGE',
        'FILE',
        'CHECKLIST',
        'PAYMENT_CONFIRM',
        'TAX_INVOICE_VIEW',
        'APPROVAL',
        'AI',
        'BID_NOTICE',
        'SETTLEMENT'
    ) NOT NULL
    COMMENT '10종 · SETTLEMENT 추가 (2026-08-09) · 다형성 판별자';
