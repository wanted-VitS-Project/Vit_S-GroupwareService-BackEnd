-- Add BID_NOTICE as a block type.
-- Bid notice blocks are read-only entry points.
-- They resolve the notice through: block -> step -> project -> project.bid_notice_id -> bid_notice.

ALTER TABLE block
    MODIFY COLUMN type ENUM(
        'TEXT',
        'IMAGE',
        'FILE',
        'CHECKLIST',
        'PAYMENT_CONFIRM',
        'TAX_INVOICE_VIEW',
        'PERFORMANCE_VIEW',
        'APPROVAL',
        'AI',
        'BID_NOTICE'
    ) NOT NULL
    COMMENT '10종 · MEMO 폐기 (2026-08-03) · BID_NOTICE 추가 (2026-08-04) · 다형성 판별자';
