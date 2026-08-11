-- =====================================================================
-- image 낙관적 락 버전 컬럼 추가
-- =====================================================================
-- 무엇: image 테이블(image_block이 아니다 — 캡션·순서는 자식 image 행에 있다)에 version 컬럼 추가.
-- 왜:   CONCURRENCY.md 표준 — PATCH /blocks/images/items/{imgBlockId}는 캡션·순서 배열을 통째로
--       받는 "목록 통째 전송" API다(§4). 항목별 version을 검사해 하나라도 충돌하면 전체를 롤백한다.
-- ⚠️ 배정 번호는 CONCURRENCY.md §7-2 표 기준(정림·image = V20260811140000)이다. 임의로 바꾸지 않는다.

ALTER TABLE image ADD COLUMN version INT NOT NULL DEFAULT 1;
