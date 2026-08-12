-- =====================================================================
-- settlement_block 낙관적 락 버전 컬럼 추가
-- =====================================================================
-- 무엇: settlement_block 테이블에 version 컬럼 추가.
-- 왜:   CONCURRENCY.md 표준 — PATCH /blocks/settlements/{settleId}/items 동시 수정 시 lost update를
--       막는다. 저장 시점에 WHERE version = ? 조건부 UPDATE로 검사한다(.ai/docs/global/CONCURRENCY.md §3).
-- ⚠️ CONCURRENCY.md §7-1 표는 정산 담당에게 V20260811150000을 배정했으나 issue 도메인과 충돌해서
--    150000 → 170000 → 170100 → 110000/0812 → 120000/0812까지 옮겼고, 그 사이 develop 기준 최대 버전이
--    계속 올라가서(다른 PR들이 CI 검사 통과 직전에 계속 먼저 머지됨) V20260812150000으로 재배정함
--    (2026-08-12, 여유값 포함). 문서(CONCURRENCY.md)는 김동현님 소유라 여기 파일 주석에만 남기고 직접
--    고치지 않는다 — 이 재배정 자체가 반복되고 있다는 것도 팀에 별도로 전달할 것(CONCURRENCY.md §7-2
--    번호 배분 방식 자체를 재검토해야 할 수 있음).

ALTER TABLE settlement_block ADD COLUMN version INT NOT NULL DEFAULT 1;
