-- =====================================================================
-- 멀티테넌트 — cash_flow · tax_invoice 에 company_id
-- =====================================================================
-- 무엇: 두 테이블에 company_id 컬럼을 NOT NULL(DEFAULT 없음)로 추가한다.
-- 왜:   cash_flow/tax_invoice는 정산 블록(settlement_block)에 연결되기 전(또는 영원히 미연결로) project를
--       모르는 상태로 존재할 수 있다. project를 거쳐 회사를 유추할 수 없으므로, 이 두 테이블은 그 자체로
--       회사 소속을 들고 있어야 한다. project/step/block/settlement_block은 아직 회사 컬럼이 없어
--       이번 확장과 무관하다(별도 멀티테넌시 확장 대상).
-- ⚠️ HR 4테이블(V20260809101000/V20260809120000)은 DEFAULT 1로 먼저 깔고 나중에(Phase 1) DEFAULT를
--    떼는 2단계로 갔다 — 그건 이미 돌고 있던 기존 INSERT 코드(EmployeeCommandService 등)가 컬럼을
--    모른 채로 계속 동작해야 했기 때문이다. cash_flow/tax_invoice는 애초에 INSERT하는 코드 자체가
--    없어서(업로드 API 미구현, 지금은 전부 수동 SQL) 보호할 기존 경로가 없다 — 이 파일 하나에서
--    ADD COLUMN(기존 테스트 행 백필용 DEFAULT 1) 후 바로 DROP DEFAULT까지 끝낸다.

ALTER TABLE cash_flow
  ADD COLUMN company_id BIGINT NOT NULL DEFAULT 1 COMMENT '회사(테넌트) · 업로드 시점에 명시 스탬핑' AFTER cash_flow_id,
  DROP INDEX uk_cash_flow_dedup,
  ADD UNIQUE KEY uk_cash_flow_dedup (company_id, bank_name, traded_at, amount),
  ADD CONSTRAINT fk_cash_flow_company
    FOREIGN KEY (company_id) REFERENCES company (company_id);

ALTER TABLE cash_flow ALTER COLUMN company_id DROP DEFAULT;

-- tax_invoice의 uk_tax_invoice_approval_no(approval_no)는 회사 스코프를 걸지 않는다 — 세금계산서
-- 승인번호는 국세청이 전국 단위로 발급하는 유일값이라, 회사별로 스코프를 걸면 오히려 같은 승인번호의
-- 위조 중복을 허용하게 된다.
ALTER TABLE tax_invoice
  ADD COLUMN company_id BIGINT NOT NULL DEFAULT 1 COMMENT '회사(테넌트) · 업로드 시점에 명시 스탬핑' AFTER tax_id,
  ADD CONSTRAINT fk_tax_invoice_company
    FOREIGN KEY (company_id) REFERENCES company (company_id);

ALTER TABLE tax_invoice ALTER COLUMN company_id DROP DEFAULT;
