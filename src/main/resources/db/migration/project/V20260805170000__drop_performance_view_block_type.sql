-- =====================================================================
-- block.type ENUM 에서 PERFORMANCE_VIEW 제거 — 10종 → 9종
-- ---------------------------------------------------------------------
-- 배경: PERFORMANCE_VIEW(실적 조회)는 상세 테이블이 없는 채로 T2 미결 상태였고,
--   2026-08-05 팀 결정으로 폐기했다. MEMO 폐기(2026-08-03) 와 같은 처리다.
--   Java `BlockType` enum 에는 애초에 없어서 앱이 이 값을 쓴 적이 없다.
--
-- ⚠️ 왜 가드를 먼저 거는가:
--   MySQL 은 ENUM 에서 값을 빼면 그 값을 쓰던 행을 **예외 없이 '' 로 조용히 바꾼다.**
--   아래 CHECK 제약을 먼저 추가하면 잔존 행이 있을 때 이 마이그레이션이 실패하므로
--   데이터가 조용히 뭉개지는 대신 사람이 먼저 알게 된다. 검증이 끝나면 바로 떼어낸다.
-- =====================================================================

-- 1) 잔존 행 가드 — PERFORMANCE_VIEW 행이 1건이라도 있으면 이 ALTER 가 실패한다.
ALTER TABLE block
    ADD CONSTRAINT ck_block_no_performance_view CHECK (type <> 'PERFORMANCE_VIEW');

-- 2) 검증이 끝났으므로 제약을 떼어낸다 (ENUM 변경과 서로 걸리지 않게 먼저 제거).
ALTER TABLE block
    DROP CHECK ck_block_no_performance_view;

-- 3) ENUM 을 9종으로 재정의한다.
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
        'BID_NOTICE'
    ) NOT NULL
    COMMENT '9종 · 다형성 판별자';
