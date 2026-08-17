-- =====================================================================
-- 데모 블록 col_span 규칙 정합 — 이미 들어간 행을 덮어쓴다
-- ---------------------------------------------------------------------
-- 무엇: 데모 테넌트(vitawear=company 2 · vitaedu=company 3)의 블록 col_span 을
--       현재 규칙으로 맞춘다 — FILE·BID_NOTICE 만 2, 나머지 전 타입 1.
-- 왜:   시드 파일은 규칙대로 고쳤지만 재적용은 INSERT IGNORE 라 기존 행을 덮지 않는다.
--       이미 DB 에 들어간 옛 col_span 값은 이 UPDATE 로만 교정된다.
--
-- 범위: company 2·3 의 블록만. 실사용 테넌트(company 1)는 건드리지 않는다
--       (company 1 샘플 블록 1~14 는 V20260817120000 마이그레이션이 따로 교정).
-- 멱등: 이미 규칙에 맞는 행은 갱신되지 않는다. 삭제된 블록도 함께 맞춘다(deleted_at 무관).
--
-- 적용: mysqlsh --sql --uri "USER@HOST/vitamins" --file patch_block_col_span.sql
-- =====================================================================

UPDATE block b
  JOIN step s    ON s.step_id = b.step_id
  JOIN project p ON p.project_id = s.project_id
   SET b.col_span = CASE WHEN b.type IN ('FILE', 'BID_NOTICE') THEN 2 ELSE 1 END
 WHERE p.company_id IN (2, 3)
   AND b.col_span <> CASE WHEN b.type IN ('FILE', 'BID_NOTICE') THEN 2 ELSE 1 END;

-- 검증 — 데모 블록 중 규칙 위반 (0 이어야 정상)
-- SELECT COUNT(*) FROM block b
--   JOIN step s ON s.step_id=b.step_id JOIN project p ON p.project_id=s.project_id
--  WHERE p.company_id IN (2,3)
--    AND ((b.type IN ('FILE','BID_NOTICE') AND b.col_span<>2)
--      OR (b.type NOT IN ('FILE','BID_NOTICE') AND b.col_span<>1));
