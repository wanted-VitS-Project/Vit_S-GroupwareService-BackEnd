-- =====================================================================
-- 멀티테넌트 Phase 0 · S1 — HR 루트 4테이블에 company_id
-- =====================================================================
-- 대상: employee · department · job_position · employee_group (전부 루트).
-- account · page_permission · employee_group_member 는 자식이라 손대지 않는다
-- (employee / group 을 타고 회사가 결정된다).
--
-- ⚠️ DEFAULT 1 은 "임시"다. 지금은 회사가 기본회사(1) 하나뿐이라, 기존 INSERT 가 company_id 를
--    안 넘겨도 1 로 채워져 그대로 동작한다. Phase 1 에서 회사 2가 생기면 각 도메인 생성 코드에
--    TenantContext 스탬핑을 넣고 이 DEFAULT 를 반드시 제거해야 한다 — 안 그러면 새 데이터가
--    조용히 1번 회사로 찍힌다.
--
-- 이름 UNIQUE 는 회사 스코프로 교체한다 (회사마다 같은 부서/직급/그룹명 허용).
--   · department 는 2026-08-06 에 전역 uk_department_name → 형제 스코프 uk_department_parent_name
--     (parent_key = COALESCE(parent_id,0) STORED) 으로 이미 바뀌었다. 그래서 여기서 드롭할 대상은
--     uk_department_parent_name 이고, 회사 스코프는 (company_id, parent_key, name) 이다.

ALTER TABLE department
  ADD COLUMN company_id BIGINT NOT NULL DEFAULT 1 COMMENT '회사(테넌트) · Phase1에서 DEFAULT 제거' AFTER department_id,
  DROP INDEX uk_department_parent_name,
  ADD UNIQUE KEY uk_department_company_parent_name (company_id, parent_key, name),
  ADD CONSTRAINT fk_department_company
    FOREIGN KEY (company_id) REFERENCES company (company_id);

ALTER TABLE job_position
  ADD COLUMN company_id BIGINT NOT NULL DEFAULT 1 COMMENT '회사(테넌트) · Phase1에서 DEFAULT 제거' AFTER job_position_id,
  DROP INDEX uk_job_position_name,
  ADD UNIQUE KEY uk_job_position_company_name (company_id, name),
  ADD CONSTRAINT fk_job_position_company
    FOREIGN KEY (company_id) REFERENCES company (company_id);

-- employee 는 이름 UNIQUE 가 원래 없다 (PK 는 user_id). 회사 FK 만 추가한다.
ALTER TABLE employee
  ADD COLUMN company_id BIGINT NOT NULL DEFAULT 1 COMMENT '회사(테넌트) · Phase1에서 DEFAULT 제거' AFTER user_id,
  ADD CONSTRAINT fk_employee_company
    FOREIGN KEY (company_id) REFERENCES company (company_id);

ALTER TABLE employee_group
  ADD COLUMN company_id BIGINT NOT NULL DEFAULT 1 COMMENT '회사(테넌트) · Phase1에서 DEFAULT 제거' AFTER group_id,
  DROP INDEX uk_employee_group_name,
  ADD UNIQUE KEY uk_employee_group_company_name (company_id, name),
  ADD CONSTRAINT fk_employee_group_company
    FOREIGN KEY (company_id) REFERENCES company (company_id);
