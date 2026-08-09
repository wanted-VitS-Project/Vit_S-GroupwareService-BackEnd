-- 정산 도메인 재설계: 기존 입금확인 블록 상세/원장(payment, block_payment_confirm)과
-- 세금계산서 조회 블록 상세/원장(tax_invoice, tax_invoice_confirm)을
-- 정산 블록(settlement_block) · 입출금 내역(cash_flow) · 세금계산서(tax_invoice) 3개로 교체한다.
--
-- ⚠️ block.type 의 PAYMENT_CONFIRM · TAX_INVOICE_VIEW 값은 이 마이그레이션에서 건드리지 않는다
--    (공용 block 테이블 소관 — Block 도메인 담당자 결정 사항). 두 값은 이후 상세 테이블이 없는
--    상태로 남는다 — BID_NOTICE 초기 상태와 동일하게, 해당 타입 블록의 detail 조회는 null 로 응답된다.

-- 1) FK 를 들고 있는 자식 테이블부터 삭제
DROP TABLE tax_invoice_confirm;
DROP TABLE block_payment_confirm;
DROP TABLE payment;
DROP TABLE tax_invoice;

-- 2) 정산 블록 상세 (block 1:1 · 다형성 역방향 · FK 없음)
CREATE TABLE `settlement_block` (
    `settle_id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '정산 블록 아이디',
    `block_id`           BIGINT         NOT NULL COMMENT '공용 블록 아이디 (다형성 역방향 · FK 없음)',
    `round_no`           INT            NULL     COMMENT '정산 회차 번호',
    `type`               ENUM('INCOME','OUTCOME') NULL COMMENT '입출금 구분',
    `status`             ENUM('PENDING','WAITING','PARTIAL','COMPLETED') NOT NULL DEFAULT 'PENDING'
                         COMMENT '정산 상태 (미연결|정산 대기|부분 정산|정산 완료)',
    `total_amount`       DECIMAL(18,2)  NULL     COMMENT '예정 총 금액',
    `planned_amount`     DECIMAL(18,2)  NULL     COMMENT '예정 회차 금액',
    `planned_tax_amount` DECIMAL(18,2)  NULL     COMMENT '예정 회차 세금 금액',
    `planned_date`       DATE           NULL     COMMENT '예정일',
    `actual_amount`      DECIMAL(18,2)  NULL     COMMENT '실제 금액 (재무팀 입력, 초기 NULL)',
    `actual_date`        DATETIME       NULL     COMMENT '실제 입출금일 (재무팀 입력, 초기 NULL)',
    `trader_name`        VARCHAR(250)   NULL     COMMENT '거래처명(입금자명)',
    `bank_name`          VARCHAR(50)    NULL     COMMENT '은행명 (OUTCOME 타입만 필수)',
    `account_number`     VARCHAR(250)   NULL     COMMENT '계좌번호 — 암호화해서 저장 (OUTCOME 타입만 필수)',
    `account_holder`     VARCHAR(100)   NULL     COMMENT '예금주 (OUTCOME 타입만 필수)',
    `created_at`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME       NULL,
    `deleted_at`         DATETIME       NULL     COMMENT '⚠️ 삭제 판정은 block.deleted_at 이 우선 (BLK-007)',
    PRIMARY KEY (`settle_id`),
    CONSTRAINT `uk_settlement_block_block` UNIQUE (`block_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='정산 블록';

-- 3) 입출금 내역 (은행 거래 원장, 정산 블록에 선택적으로 연결)
--    dedup 기준: 은행명 · 거래일시 · 금액이 모두 같으면 중복건으로 보고 저장하지 않는다.
CREATE TABLE `cash_flow` (
    `cash_flow_id`    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '입출금 내역 아이디',
    `settle_block_id` BIGINT        NULL     COMMENT '연결된 정산 블록 아이디 (NULL = 미연결)',
    `bank_name`       VARCHAR(50)   NOT NULL COMMENT '은행명',
    `type`            ENUM('INCOME','OUTCOME') NOT NULL COMMENT '입출금 구분',
    `traded_at`       DATETIME      NOT NULL COMMENT '거래일시',
    `amount`          DECIMAL(18,2) NOT NULL COMMENT '금액',
    `depositor_name`  VARCHAR(250)  NOT NULL COMMENT '내용 · 입금자명/수취인명',
    `bank_memo`       VARCHAR(500)  NULL     COMMENT '적요/메모',
    `source_type`     ENUM('MANUAL','CSV','API') NOT NULL COMMENT '수집 경로',
    `bank_txn_id`     VARCHAR(100)  NULL     COMMENT '거래고유번호 (은행명 4자리+거래일시+순번 조합)',
    `linked_by`       VARCHAR(20)   NULL     COMMENT '사번 · 정산 블록 연결자 (미연결이면 NULL)',
    `linked_at`       DATETIME      NULL     COMMENT '정산 블록 연결일시',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME      NULL,
    `deleted_at`      DATETIME      NULL,
    `is_excluded`     BOOLEAN       NOT NULL DEFAULT FALSE COMMENT '프로젝트와 무관해 미연결 건수 집계에서 뺄 대상',
    PRIMARY KEY (`cash_flow_id`),
    -- 은행명 · 거래일시 · 금액이 겹치면 중복건이므로 DB에 저장되지 않도록 막는다.
    CONSTRAINT `uk_cash_flow_dedup` UNIQUE (`bank_name`, `traded_at`, `amount`),
    CONSTRAINT `fk_cash_flow_settle_block` FOREIGN KEY (`settle_block_id`) REFERENCES `settlement_block` (`settle_id`),
    CONSTRAINT `fk_cash_flow_linked_by`    FOREIGN KEY (`linked_by`)       REFERENCES `employee` (`user_id`),
    KEY `idx_cash_flow_settle_block` (`settle_block_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='입출금 내역';

-- 4) 세금계산서 (신규 스키마 — 옛 tax_invoice 를 대체, 승인번호 중복 불가)
CREATE TABLE `tax_invoice` (
    `tax_id`           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '세금계산서 아이디',
    `settle_block_id`  BIGINT         NULL     COMMENT '연결된 정산 블록 아이디 (NULL = 미연결)',
    `type`             ENUM('INCOME','OUTCOME') NOT NULL COMMENT '입출금(매출/매입) 구분',
    `approval_no`      VARCHAR(100)   NOT NULL COMMENT '승인번호',
    `issued_no`        DATE           NOT NULL COMMENT '발행일',
    `supplier_biz_no`  VARCHAR(20)    NULL     COMMENT '공급자 사업자번호 (외주 구분용)',
    `supply_amount`    DECIMAL(18,2)  NOT NULL COMMENT '공급가액',
    `tax_amount`       DECIMAL(18,2)  NOT NULL COMMENT '세액',
    `total_amount`     DECIMAL(18,2)  NOT NULL COMMENT '총액',
    `buyer_name`       VARCHAR(250)   NOT NULL COMMENT '공급받는자 상호명',
    `buyer_biz_no`     VARCHAR(20)    NOT NULL COMMENT '공급받는자 사업자번호',
    `sub_biz_no`       VARCHAR(10)    NULL     COMMENT '종사업장번호',
    `ceo_name`         VARCHAR(100)   NULL     COMMENT '대표자명',
    `item_name`        VARCHAR(500)   NULL     COMMENT '품목명',
    `memo`             TEXT           NULL     COMMENT '비고/메모',
    `source_type`      ENUM('CSV','HOMETAX_API') NOT NULL COMMENT '수집 경로',
    `linked_by`        VARCHAR(20)    NULL     COMMENT '정산 블록 연결자 사번 (미연결이면 NULL)',
    `linked_at`        DATETIME       NULL     COMMENT '정산 블록 연결일시',
    `created_at`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME       NULL,
    `deleted_at`       DATETIME       NULL,
    `is_excluded`      BOOLEAN        NOT NULL DEFAULT FALSE COMMENT '프로젝트와 무관해 미연결 건수 집계에서 뺄 대상',
    PRIMARY KEY (`tax_id`),
    -- 승인번호 중복 불가.
    CONSTRAINT `uk_tax_invoice_approval_no`  UNIQUE (`approval_no`),
    CONSTRAINT `fk_tax_invoice_settle_block` FOREIGN KEY (`settle_block_id`) REFERENCES `settlement_block` (`settle_id`),
    CONSTRAINT `fk_tax_invoice_linked_by`    FOREIGN KEY (`linked_by`)       REFERENCES `employee` (`user_id`),
    KEY `idx_tax_invoice_settle_block` (`settle_block_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='세금계산서';
