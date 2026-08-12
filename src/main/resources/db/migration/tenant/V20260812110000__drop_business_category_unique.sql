-- =====================================================================
-- business_category — soft delete 와 UNIQUE 공존 해소 (DELETE.md D-7 · §6-1)
-- =====================================================================
-- 문제: V20260811100100 이 uk_bc_company_name·uk_bc_company_code 를 걸어둔 탓에,
--   soft delete(deleted_at) 된 이름·업무코드가 슬롯을 계속 점유한다 → 같은 이름 재등록이 1062 로 막힌다.
--   (관리자 화면의 "삭제된 카테고리에 같은 이름이 있습니다" 봉쇄의 원인)
--
-- 해법(DELETE.md D-7 — "걸어야 하면 삭제 시 그 컬럼을 NULL 로 비운다"):
--   실제 name·code 는 이력용으로 그대로 두고, 「활성 행일 때만 값이 있고 삭제되면 NULL」이 되는
--   그림자 생성 컬럼(active_name·active_code)에 UNIQUE 를 건다.
--     · 활성 행: active_* = 원본값 → 회사 범위 유니크가 그대로 강제된다 (동시 생성 경합도 DB 가 최종 차단).
--     · 삭제 행: active_* = NULL → MySQL UNIQUE 는 NULL 을 중복으로 보지 않아 같은 이름을 다시 만들 수 있다.
--   → 앱(BusinessCategoryCommandService)은 활성 행만 선검사하고, 경합 시 발생하는 UNIQUE 위반은
--     DataIntegrityViolation → 409 로 변환한다 (department·job_position 선례와 동일).
--
-- ⚠️ FK 인덱스: uk_bc_company_name 이 fk_business_category_company 의 인덱스를 겸했다.
--   새 uk_bc_active_name (company_id, active_name) 도 선두가 company_id 라 FK 인덱스를 그대로 겸한다 → 별도 인덱스 불필요.
--
-- 기존 데이터 안전: 직전 제약이 (company_id, name)·(company_id, code) 전역 유니크였으므로
--   현재 활성 행은 이미 회사 내 유일하다 → 새 UNIQUE 추가 시 위반 없음.

ALTER TABLE business_category
  DROP INDEX uk_bc_company_name,
  DROP INDEX uk_bc_company_code,
  ADD COLUMN active_name VARCHAR(100)
    GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN name ELSE NULL END) VIRTUAL
    COMMENT '활성 행일 때만 name, 삭제되면 NULL — 활성 유니크 전용 그림자 컬럼',
  ADD COLUMN active_code VARCHAR(30)
    GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN code ELSE NULL END) VIRTUAL
    COMMENT '활성 행일 때만 code, 삭제되면 NULL',
  ADD UNIQUE KEY uk_bc_active_name (company_id, active_name),
  ADD UNIQUE KEY uk_bc_active_code (company_id, active_code);
