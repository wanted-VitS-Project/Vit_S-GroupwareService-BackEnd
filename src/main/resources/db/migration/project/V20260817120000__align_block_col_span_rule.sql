-- =====================================================================
-- 블록 col_span 규칙 정합 — FILE·BID_NOTICE 만 col_span 2, 나머지 전 타입 1
-- ---------------------------------------------------------------------
-- 무엇: 프로젝트 샘플 블록(1~14)의 col_span 을 현재 배치 규칙에 맞춘다.
-- 왜:   V20260811180000(reseed) 이 넣은 샘플 블록 14 '제안서 체크리스트'(CHECKLIST)가
--       옛 배치 관례(한 행 col_span 합=3)로 col_span 2 다. 현재 규칙은
--       FILE·BID_NOTICE 만 2 이고 나머지는 무조건 1 이다.
--       앱 검증(BlockCommandService)은 1~3 범위만 보고 "행 합=3" 강제는 없다.
--
-- 🚨 이미 적용된 마이그레이션(V20260805160000·V20260811180000)은 수정하지 않는다
--    (FLYWAY.md §5). reseed 가 그랬듯 새 파일로 교정한다.
--
-- 범위: 마이그레이션이 넣은 company 1 샘플 블록(block_id 1~14)만 대상.
--       실사용자가 만든 블록은 건드리지 않는다.
-- 멱등: DB 가 이미 규칙에 맞으면 0행 갱신이다.
-- =====================================================================

UPDATE block
   SET col_span = 1
 WHERE block_id BETWEEN 1 AND 14
   AND type NOT IN ('FILE', 'BID_NOTICE')
   AND col_span <> 1;

UPDATE block
   SET col_span = 2
 WHERE block_id BETWEEN 1 AND 14
   AND type IN ('FILE', 'BID_NOTICE')
   AND col_span <> 2;
