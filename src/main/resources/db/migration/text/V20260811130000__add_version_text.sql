-- =====================================================================
-- text 낙관적 락 버전 컬럼 추가
-- =====================================================================
-- 무엇: text 테이블에 version 컬럼 추가.
-- 왜:   CONCURRENCY.md 표준 — 본문 PATCH(/blocks/texts/{txtId})가 두 사용자 동시 수정 시
--       나중 저장이 먼저 저장을 조용히 덮어쓰는 lost update를 막는다. 저장 시점에
--       WHERE version = ? 조건부 UPDATE로 검사한다(.ai/docs/global/CONCURRENCY.md §3).
-- ⚠️ 배정 번호는 CONCURRENCY.md §7-2 표 기준(정림·text = V20260811130000)이다. 임의로 바꾸지 않는다.

ALTER TABLE text ADD COLUMN version INT NOT NULL DEFAULT 1;
