-- =====================================================================
-- business_category — soft delete 와 UNIQUE 공존 해소 (DELETE.md D-7 · §6-1)
-- =====================================================================
-- 문제: V20260811100100 이 uk_bc_company_name·uk_bc_company_code 를 다시 걸었다.
--   business_category 는 soft delete(deleted_at) 를 쓰는 마스터 데이터라, UNIQUE 가 남아 있으면
--   삭제된 이름·업무코드가 슬롯을 계속 점유한다 → 같은 이름을 다시 만들 때 1062 로 막힌다.
--   (관리자 화면에서 "삭제된 카테고리에 같은 이름이 있습니다" 로 재사용이 봉쇄되던 원인)
--
-- 해법(DELETE.md §6-1 마스터 데이터 행): DB UNIQUE 를 걷고, 중복 검사는 앱이
--   「활성 행만」(deleted_at IS NULL) 대상으로 수행한다.
--     → BusinessCategoryCommandService.checkNameDuplicate / checkCodeDuplicate 가 활성 행만 조회
--
-- ⚠️ 인덱스 겸용 주의: uk_bc_company_name 은 선두 컬럼이 company_id 라
--   fk_business_category_company 의 인덱스를 겸하고 있었다(V20260811100100 §16). UNIQUE 를 그냥 지우면
--   FK 가 인덱스를 잃어 errno 150 으로 실패한다 → 같은 커버리지의 일반 인덱스를 같은 문장에서 대신 만든다.

ALTER TABLE business_category
  DROP INDEX uk_bc_company_name,
  DROP INDEX uk_bc_company_code,
  ADD KEY idx_bc_company (company_id);
