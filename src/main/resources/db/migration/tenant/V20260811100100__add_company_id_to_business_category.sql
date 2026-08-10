-- =====================================================================
-- 멀티테넌트 Phase 1 · P1-2b — business_category 에 company_id
-- =====================================================================
-- 대상: business_category (루트). project_business_category 는 조인 행이라 손대지 않는다
-- (양쪽 루트가 격리되면 연결도 회사 안에서만 성립한다 — 앱 계층에서 막는다).
--
-- ⚠️ DEFAULT 1 은 "임시"다. 생성 스탬핑 코드를 넣은 뒤 별도 마이그레이션으로 반드시 제거한다.
--
-- 백필은 하지 않는다. business_category 에는 created_by 가 없어 귀속을 유추할 근거가 없고,
-- 회사가 아직 1개뿐이라 기존 전 건의 정답이 1 이다 (DEFAULT 1 이 그 값을 채운다).
--
-- 이름·업무코드 UNIQUE 를 회사 스코프로 교체한다. 회사 A 의 "건설업" 과 B 의 "건설업" 은
-- 이름만 같은 다른 행이어야 한다 — 전역 UNIQUE 면 두 번째 회사가 같은 이름을 못 써서 잘못된 충돌이 난다.
--   · code 는 NULL 허용이고 MySQL UNIQUE 는 NULL 을 중복으로 보지 않으므로, 복합으로 바꿔도
--     "업무코드 없는 카테고리 여러 개" 는 그대로 허용된다.
--   · uk_bc_company_name 의 선두 컬럼이 company_id 라 fk_business_category_company 의 인덱스도 겸한다.

ALTER TABLE business_category
  ADD COLUMN company_id BIGINT NOT NULL DEFAULT 1 COMMENT '회사(테넌트) · 스탬핑 적용 후 DEFAULT 제거' AFTER business_category_id,
  DROP INDEX uk_bc_name,
  DROP INDEX uk_bc_code,
  ADD UNIQUE KEY uk_bc_company_name (company_id, name),
  ADD UNIQUE KEY uk_bc_company_code (company_id, code),
  ADD CONSTRAINT fk_business_category_company
    FOREIGN KEY (company_id) REFERENCES company (company_id);
