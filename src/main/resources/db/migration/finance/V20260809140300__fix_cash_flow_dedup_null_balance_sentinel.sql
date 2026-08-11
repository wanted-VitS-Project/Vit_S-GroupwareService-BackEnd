-- =====================================================================
-- cash_flow 중복 판정 유니크 제약 — NULL 잔액과 실제 잔액 -1.00의 충돌 방지
-- =====================================================================
-- 무엇: uk_cash_flow_dedup에 balance_after_present(잔액 컬럼 존재 여부) 컬럼을 추가로 포함시킨다.
-- 왜:   V20260809140200에서 NULL을 고정값 -1로 치환한 balance_after_key를 추가했는데, 그러면 잔액
--       컬럼이 없는 은행의 거래(balance_after IS NULL → -1로 치환)와, 실제로 잔액이 -1.00원인
--       정상 거래(과인출·마이너스통장 등)가 완전히 같은 키로 취급돼 서로 다른 정상 거래 중 하나가
--       "이미 등록된 거래"로 거부될 수 있다(2026-08-11, CodeRabbit round 3 지적).
--       애플리케이션 레벨 사전 중복 조회(FinanceCommandService.dedupKey)는 이미 NULL과 실제 "-1"을
--       문자열 "null"/"-1"로 구분해서 처리하고 있었다 — DB 제약만 이 구분이 빠져 있었다.
-- ⚠️ balance_after_key(coalesce 값 자체)는 그대로 둔다 — present 플래그가 NULL 여부를 이미
--    구분해주므로, coalesce 대상값을 뭘로 잡든(-1이든 0이든) 실제 잔액과 충돌할 일이 없어진다.
--    괜히 값을 바꾸면 이미 저장된 행의 balance_after_key만 다시 계산되는 불필요한 변경이 생긴다.
-- ⚠️ 기존 데이터에 이미 이 새 키 기준 중복이 있으면 ADD UNIQUE KEY 자체가 실패한다(CodeRabbit
--    round 3 지적 — Heavy lift). 실제 재무 거래 행을 도구가 임의로 지우거나 병합하면 안 되는
--    영역이라 이 마이그레이션에 자동 정리 로직은 넣지 않는다 — 아래 쿼리로 먼저 직접 확인할 것.
--
--    SELECT company_id, bank_name, type, traded_at, amount,
--           (balance_after IS NOT NULL) AS balance_after_present,
--           COALESCE(balance_after, -1) AS balance_after_key,
--           COUNT(*) AS row_count
--    FROM cash_flow
--    GROUP BY company_id, bank_name, type, traded_at, amount,
--             balance_after_present, balance_after_key
--    HAVING COUNT(*) > 1;
--
--    행이 나오면(소프트 삭제된 deleted_at IS NOT NULL 행도 이 쿼리엔 포함됨 — 인덱스 자체가
--    deleted_at을 조건에 안 걸기 때문) 실제로 중복 거래인지 사람이 확인 후 처리해야 한다.

ALTER TABLE cash_flow
  ADD COLUMN balance_after_present TINYINT(1) AS (balance_after IS NOT NULL) STORED,
  DROP INDEX uk_cash_flow_dedup,
  ADD UNIQUE KEY uk_cash_flow_dedup
      (company_id, bank_name, type, traded_at, amount, balance_after_present, balance_after_key);
