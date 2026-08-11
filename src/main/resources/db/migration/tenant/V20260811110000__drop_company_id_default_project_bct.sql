-- =====================================================================
-- 멀티테넌트 Phase 1 · P1-2c — project · business_category 의 company_id 임시 DEFAULT 제거
-- =====================================================================
-- 무엇: 두 테이블의 company_id 에서 DEFAULT 1 을 뗀다 (NOT NULL 은 유지).
-- 왜:   P1-2a·2b 의 DEFAULT 1 은 "회사가 하나뿐이라 아무 값이나 넣어도 1" 이라는 임시 조치였다. 이제
--       ProjectCommandService · BusinessCategoryCommandService 가 CurrentCompanyIdProvider 로 company_id 를
--       명시 스탬핑하므로 DEFAULT 가 불필요하고, 남겨두면 스탬핑을 빠뜨린 INSERT 가 조용히 1번 회사로 찍히는
--       landmine 이 된다. DEFAULT 를 떼면 그런 누락이 NOT NULL 위반으로 드러난다.
-- ⚠️ 이 마이그레이션은 대응하는 생성 스탬핑 코드와 한 세트다. 코드 없이 이것만 적용하면 기존 생성 API 가 깨진다.

ALTER TABLE project           ALTER COLUMN company_id DROP DEFAULT;
ALTER TABLE business_category ALTER COLUMN company_id DROP DEFAULT;
