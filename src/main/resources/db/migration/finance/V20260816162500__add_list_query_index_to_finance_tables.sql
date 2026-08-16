-- =====================================================================
-- cash_flow / tax_invoice 목록 조회용 복합 인덱스 추가
-- =====================================================================
-- 무엇: 두 테이블에 (company_id, deleted_at, 정렬 컬럼) 복합 인덱스를 추가한다.
-- 왜:   목록 조회(findCashFlows/findTaxInvoices)가 회사 단위로 좁힌 뒤 거래일시/발행일 순으로
--       정렬하는데, 이를 받쳐줄 인덱스가 없어 매번 정렬 작업(Using filesort)이 발생한다
--       (2026-08-16, 로컬 벌크 데이터로 실측 확인 — 223건에선 무해, 1만 건대부터 나타남).
-- ⚠️ 지금 실제 데이터량(회사당 수백 건)에선 체감 차이가 없다 — 회사 수·연차가 누적돼
--    수천~수만 건 규모가 됐을 때를 대비한 선제 조치다.
-- ⚠️ 기존 uk_cash_flow_dedup(company_id, bank_name, ...)은 목적이 달라(중복 방지) 이 용도로
--    못 쓴다 — company_id 다음이 bank_name이라 정렬(traded_at)까지 이어지지 않는다.

ALTER TABLE cash_flow
  ADD INDEX idx_cash_flow_list (company_id, deleted_at, traded_at);

ALTER TABLE tax_invoice
  ADD INDEX idx_tax_invoice_list (company_id, deleted_at, issued_no);
