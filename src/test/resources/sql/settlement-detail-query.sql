-- =====================================================================
-- SettlementDetailMapper 진행률 집계 조회 검증용 픽스처 (H2 · MySQL 모드)
-- =====================================================================
-- ⚠️ 운영 스키마의 미러가 아니다. 이 쿼리가 실제로 읽는 컬럼만 담았다.
--    실제 스키마는 Flyway(`db/migration`)가 정본이며, MySQL 전용 문법(ENUM ·
--    DECIMAL 정밀도 · ON UPDATE CURRENT_TIMESTAMP)이 H2 에서 그대로 안 돌아간다.
--    여기에 컬럼을 늘려 실제 스키마를 흉내내면 두 벌을 관리하게 된다 — 늘리지 말 것.
--
-- ⚠️ block · step 테이블이 없다. 그게 이 테스트의 요점이다 —
--    project_id 비정규화(V20260816170100) 이후 이 쿼리는 두 테이블을 타지 않는다.
--    조인이 되살아나면 이 픽스처에서 "테이블 없음"으로 즉시 깨진다.
--
-- 데이터 구성 — settle 1·2·3 은 project 100, settle 4 는 project 200(다른 프로젝트).
--   settle 1  project 100 INCOME  actual 1000              → 조회 대상
--   settle 2  project 100 INCOME  actual  500              → 형제(요청 안 함) · 합계에 포함돼야 한다
--   settle 3  project 100 OUTCOME actual 9999              → 타입 다름 · 섞이면 안 된다
--   settle 4  project 200 INCOME  actual 7777              → 프로젝트 다름 · 섞이면 안 된다
--   settle 5  project 100 INCOME  actual  300 · 삭제됨      → 합계에서 빠져야 한다
--   settle 6  project 100 type NULL(빈 상세 행)             → actualAmountSum 이 null 이어야 한다
--   settle 7  project 100 INCOME · 삭제됨                   → 결과 자체에 안 나와야 한다

-- ⚠️ 테스트마다 다시 실행된다. @MybatisTest 의 트랜잭션 롤백은 INSERT 만 되돌리고 DDL 은 커밋된 채
--    남으므로, 드롭 없이는 두 번째 테스트에서 "테이블 이미 존재"로 깨진다.
DROP TABLE IF EXISTS tax_invoice;
DROP TABLE IF EXISTS settlement_block;

CREATE TABLE settlement_block (
    settle_id            BIGINT PRIMARY KEY,
    block_id             BIGINT       NOT NULL,
    project_id           BIGINT       NOT NULL,
    round_no             INT          NULL,
    type                 VARCHAR(10)  NULL,
    status               VARCHAR(20)  NOT NULL,
    total_amount         DECIMAL(18, 2) NULL,
    planned_amount       DECIMAL(18, 2) NULL,
    planned_tax_amount   DECIMAL(18, 2) NULL,
    planned_date         DATE         NULL,
    tax_invoice_due_date DATE         NULL,
    actual_amount        DECIMAL(18, 2) NULL,
    actual_date          TIMESTAMP    NULL,
    trader_name          VARCHAR(250) NULL,
    bank_name            VARCHAR(50)  NULL,
    account_number       VARCHAR(250) NULL,
    account_holder       VARCHAR(100) NULL,
    created_at           TIMESTAMP    NOT NULL,
    deleted_at           TIMESTAMP    NULL,
    version              INT          NOT NULL
);

CREATE TABLE tax_invoice (
    tax_id          BIGINT PRIMARY KEY,
    settle_block_id BIGINT    NULL,
    deleted_at      TIMESTAMP NULL
);

INSERT INTO settlement_block
(settle_id, block_id, project_id, round_no, type, status, total_amount, planned_amount, planned_tax_amount,
 planned_date, tax_invoice_due_date, actual_amount, actual_date, trader_name, bank_name, account_number,
 account_holder, created_at, deleted_at, version)
VALUES
-- 조회 대상 — 필드 위치 매핑 확인용으로 문자열 컬럼을 서로 다른 값으로 채운다.
(1, 11, 100, 1, 'INCOME', 'PENDING', 5000, 1200, 120,
 DATE '2026-08-20', DATE '2026-08-25', 1000, TIMESTAMP '2026-08-18 10:00:00', '거래처가', '국민', 'ENC-1',
 '예금주가', TIMESTAMP '2026-08-01 09:00:00', NULL, 3),
-- 같은 프로젝트·같은 타입 형제 — 요청하지 않았지만 합계에는 들어가야 한다.
(2, 12, 100, 2, 'INCOME', 'COMPLETED', 5000, 800, 80,
 NULL, NULL, 500, NULL, '거래처나', NULL, NULL, NULL, TIMESTAMP '2026-08-02 09:00:00', NULL, 1),
-- 같은 프로젝트지만 타입이 다르다.
(3, 13, 100, 1, 'OUTCOME', 'COMPLETED', 9999, NULL, NULL,
 NULL, NULL, 9999, NULL, NULL, NULL, NULL, NULL, TIMESTAMP '2026-08-03 09:00:00', NULL, 1),
-- 다른 프로젝트.
(4, 14, 200, 1, 'INCOME', 'COMPLETED', 7777, NULL, NULL,
 NULL, NULL, 7777, NULL, NULL, NULL, NULL, NULL, TIMESTAMP '2026-08-04 09:00:00', NULL, 1),
-- 같은 프로젝트·같은 타입인데 삭제됨.
(5, 15, 100, 3, 'INCOME', 'COMPLETED', 5000, NULL, NULL,
 NULL, NULL, 300, NULL, NULL, NULL, NULL, NULL, TIMESTAMP '2026-08-05 09:00:00', TIMESTAMP '2026-08-06 09:00:00', 1),
-- 아직 내용을 안 채운 빈 상세 행(type NULL).
(6, 16, 100, NULL, NULL, 'PENDING', NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, TIMESTAMP '2026-08-07 09:00:00', NULL, 1),
-- 조회 대상인데 삭제된 행.
(7, 17, 100, 4, 'INCOME', 'PENDING', 5000, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, TIMESTAMP '2026-08-08 09:00:00', TIMESTAMP '2026-08-09 09:00:00', 1);

INSERT INTO tax_invoice (tax_id, settle_block_id, deleted_at)
VALUES (900, 1, NULL),                              -- settle 1 → 연결됨
       (901, 2, TIMESTAMP '2026-08-10 09:00:00');   -- settle 2 → 삭제된 계산서라 연결로 보면 안 된다
