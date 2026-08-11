-- =====================================================================
-- cash_flow 에 거래 후 잔액(balance_after) 추가 — 중복 판정 정밀도 보강
-- =====================================================================
-- 무엇: cash_flow에 balance_after 컬럼을 추가하고, 중복 판정 유니크 제약에 포함시킨다.
-- 왜:   같은 은행·같은 초·같은 금액으로 서로 다른 거래가 실제로 발생할 수 있다(2026-08-10, 실제
--       카카오뱅크 CSV로 확인 — 같은 시각·같은 금액의 출금 2건이 잔액만 다름). 기존
--       uk_cash_flow_dedup(company_id, bank_name, traded_at, amount)만으로는 이 둘을 구분 못 하고
--       두 번째를 중복으로 오판해서 누락시켰다. 잔액을 판정 기준에 추가하면 이런 경우를 구분할 수 있다.
-- ⚠️ CSV마다 "거래 후 잔액" 컬럼이 없을 수 있어 NULL 허용이다. MySQL 유니크 인덱스는 NULL끼리는
--    서로 다른 값으로 취급하므로, 잔액 정보가 없는 은행의 CSV는 이 컬럼 추가 이후에도 기존과 동일한
--    수준의 보호만 받는다(완전한 해결은 아니지만, 잔액을 아는 은행에 한해 정밀도가 올라간다).

ALTER TABLE cash_flow
  ADD COLUMN balance_after DECIMAL(18,2) NULL COMMENT '거래 후 잔액 (CSV에 없으면 NULL)' AFTER amount,
  DROP INDEX uk_cash_flow_dedup,
  ADD UNIQUE KEY uk_cash_flow_dedup (company_id, bank_name, traded_at, amount, balance_after);
