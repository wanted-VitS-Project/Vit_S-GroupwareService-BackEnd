-- =====================================================================
-- settlement_block 낙관적 락 버전 컬럼 추가
-- =====================================================================
-- 무엇: settlement_block 테이블에 version 컬럼 추가.
-- 왜:   CONCURRENCY.md 표준 — PATCH /blocks/settlements/{settleId}/items 동시 수정 시 lost update를
--       막는다. 저장 시점에 WHERE version = ? 조건부 UPDATE로 검사한다(.ai/docs/global/CONCURRENCY.md §3).
-- ⚠️ CONCURRENCY.md §7-1 표는 정산 담당에게 V20260811150000을 배정했으나 issue 도메인과 충돌해서
--    150000 → 170000 → 170100 → 110000/0812까지 옮겼고, 이번엔 develop에 먼저 머지된 tenant(사업카테고리)
--    도메인 마이그레이션이 정확히 같은 번호(V20260812110000__drop_business_category_unique.sql)를 이미
--    써서 또 충돌(CI "마이그레이션 검증" 중복 버전 에러) — V20260812120000으로 재배정함(2026-08-12).
--    문서(CONCURRENCY.md)는 김동현님 소유라 여기 파일 주석에만 남기고 직접 고치지 않는다 — 발견한
--    충돌은 팀에 별도로 전달할 것.

ALTER TABLE settlement_block ADD COLUMN version INT NOT NULL DEFAULT 1;
