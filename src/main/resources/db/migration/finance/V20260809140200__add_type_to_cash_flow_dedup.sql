-- =====================================================================
-- cash_flow 중복 판정 유니크 제약에 type(입출금 구분) 추가
-- =====================================================================
-- 무엇: uk_cash_flow_dedup에 type을 포함시킨다.
-- 왜:   amount는 항상 절댓값으로 저장한다(CashFlowCsvRowParser.parseAmount().abs()) — 방향은 오직
--       type 컬럼만 책임진다. 그런데 중복 판정 제약(company_id, bank_name, traded_at, amount,
--       balance_after)에 type이 빠져 있어서, 같은 시각·같은 절댓값의 입금 1건과 출금 1건이 완전히
--       같은 키로 취급됐다 — 잔액 컬럼이 없는 은행(balance_after가 둘 다 NULL)이면 서로 다른 정상
--       거래 중 하나가 "이미 등록된 거래"로 조용히 유실될 수 있었다(2026-08-11, CodeRabbit Critical
--       지적으로 발견). 애플리케이션 레벨 사전 중복 조회(findExistingDedupKeys)도 같은 기준으로
--       맞췄다(CashFlowCommandMapper.xml).
-- ⚠️ 이 마이그레이션이 만드는 uk_cash_flow_dedup은 아직 develop에 병합되지 않은 이번 PR 안에서
--    두 번째로 고치는 것이라, 기존 파일(V…140100)을 직접 수정하지 않고 새 마이그레이션으로 얹는다 —
--    이미 로컬에서 그 버전으로 적용해본 사람의 flyway_schema_history 체크섬이 깨지는 걸 피하기 위함.

ALTER TABLE cash_flow
  DROP INDEX uk_cash_flow_dedup,
  ADD UNIQUE KEY uk_cash_flow_dedup (company_id, bank_name, type, traded_at, amount, balance_after);
