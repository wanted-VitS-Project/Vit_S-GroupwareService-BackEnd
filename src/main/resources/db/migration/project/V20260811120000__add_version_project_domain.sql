-- 동시수정 정합성 — 낙관적 락 version 컬럼 (.ai/docs/global/CONCURRENCY.md)
--
-- 기존 행은 전부 1 로 시작한다. 프론트가 조회에서 받은 값을 그대로 저장 요청에 실어 보내고,
-- 서버는 `WHERE version = ?` 으로 검사한다. 0 행이면 409 다.
--
-- ⛔ 순서 변경·배치 저장용 묶음 컬럼(stage_order_version 등)은 만들지 않는다.
--    요청에 담긴 항목마다 개별 version 을 검사하고 하나라도 어긋나면 전체 롤백한다
--    (CONCURRENCY.md §4-2 — 2026-08-10 검토에서 묶음 컬럼 방식 폐기).

ALTER TABLE project ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT '낙관적 락 버전';
ALTER TABLE stage   ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT '낙관적 락 버전';
ALTER TABLE step    ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT '낙관적 락 버전';
ALTER TABLE block   ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT '낙관적 락 버전';
