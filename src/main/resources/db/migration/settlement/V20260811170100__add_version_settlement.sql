-- =====================================================================
-- settlement_block 낙관적 락 버전 컬럼 추가
-- =====================================================================
-- 무엇: settlement_block 테이블에 version 컬럼 추가.
-- 왜:   CONCURRENCY.md 표준 — PATCH /blocks/settlements/{settleId}/items 동시 수정 시 lost update를
--       막는다. 저장 시점에 WHERE version = ? 조건부 UPDATE로 검사한다(.ai/docs/global/CONCURRENCY.md §3).
-- ⚠️ CONCURRENCY.md §7-1 표는 정산 담당에게 V20260811150000을 배정했으나, 실제로는 issue 도메인이
--    이미 그 번호를 단독으로 쓰고 있어(issue/V20260811150000__add_version_issue.sql) 충돌한다
--    (2026-08-11 확인). out-of-order:false라 재사용 불가 — V20260811170000으로 옮겼더니 그 사이
--    develop에 머지된 employee 도메인(V20260811170000__add_employee_profile_image_key.sql)과
--    또 충돌해서(2026-08-11 재확인), 지금 비어있는 V20260811170100으로 다시 옮김. 문서(CONCURRENCY.md)는
--    김동현님 소유라 여기 파일 주석에만 남기고 직접 고치지 않는다 — 발견한 충돌은 팀에 별도로 전달할 것.

ALTER TABLE settlement_block ADD COLUMN version INT NOT NULL DEFAULT 1;
