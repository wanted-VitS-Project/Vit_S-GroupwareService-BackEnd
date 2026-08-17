-- =====================================================================
-- 01. 테넌트 — 회사 · 부서 7 · 직급 6 · 사원 12 · 사업 카테고리 2
-- ---------------------------------------------------------------------
-- 무엇: company_id = 2 (`vitawear`) 를 새로 만들고 그 안에 조직·인원을 채운다.
-- 왜:   기존 `vitas`(company_id 1) 는 팀이 쓰고 있다. 데모 데이터를 같은 테넌트에
--       섞으면 사번이 충돌하고 목록·권한 화면이 서로 오염된다.
--
-- 🚨 접두사만 바꾸면 안 되는 이유
--    앱이 사번을 직접 만든다 — EmployeeCommandService·EmployeeBulkService 둘 다
--      userId = companyCode + "-" + baseUserId
--    company_id 1 (code 'vitas') 에 'vitawear-VW101' 을 넣으면, 같은 회사에서
--    새로 만든 사원은 'vitas-...' 가 되어 **한 회사 안에 접두사 두 종류**가 생긴다.
--    그래서 회사를 새로 만든다. (MAX_USER_ID = 20 · 'vitawear-VW101' 은 14자라 여유)
--
-- ⚠️ 회사가 갈리면 마스터도 전부 갈린다
--    department 의 부모 FK 가 복합키 (company_id, parent_id) 이고
--    employee 의 부서·직급 FK 도 (company_id, department_id) · (company_id, job_position_id) 다
--    (tenant/V20260814150000). company 1 의 '본사'(1)·'사원'(1)·'팀장'(2)·'대표'(4) 를
--    그대로 참조할 수 없다 — 회사 2 의 것을 새로 만들어야 한다.
--
-- 🚨 account(로그인 계정)는 넣지 않는다 — FLYWAY.md §4 「더미 비밀번호 금지」. PUBLIC 레포다.
--    ⚠️ 회사 2 에는 아직 ADMIN 이 없다. 회사 1 의 관리자로는 회사 2 사원을 만들 수 없다
--       (사번 접두사를 자기 회사 코드로 붙이기 때문). 첫 계정 부트스트랩은 §하단 참고.
--
-- 되돌리기:
--   DELETE FROM business_category WHERE company_id = 2;
--   DELETE FROM employee          WHERE company_id = 2;
--   DELETE FROM job_position      WHERE company_id = 2;
--   DELETE FROM department        WHERE company_id = 2 AND parent_id IS NOT NULL;
--   DELETE FROM department        WHERE company_id = 2;
--   DELETE FROM company           WHERE company_id = 2;
-- =====================================================================


-- ── 1. 회사(테넌트) ──────────────────────────────────────────────────
-- ⚠️ company_code 가 사번 접두사가 된다. UNIQUE 이므로 'vitas' 와 안 겹친다.
INSERT IGNORE INTO company (company_id, name, company_code) VALUES
(2, '주식회사 비타웨어', 'vitawear');


-- ── 2. 부서 7 — 회사 2 전용 (본사 포함) ──────────────────────────────
-- ⚠️ 본사(9009)를 먼저 넣어야 한다. 나머지가 부모로 참조한다.
--    UNIQUE 는 (company_id, parent_key, name) 이고 parent_key 는 생성 컬럼이라 안 넣는다.
INSERT IGNORE INTO department (department_id, company_id, name, parent_id) VALUES
(9009, 2, '본사',        NULL);

INSERT IGNORE INTO department (department_id, company_id, name, parent_id) VALUES
(9010, 2, '브랜드팀',   9009),
(9011, 2, '디자인팀',   9009),
(9012, 2, '물류·CS팀',  9009),
(9013, 2, '재무팀',     9009),
(9014, 2, '영업팀',     9009),
(9015, 2, '생산관리팀', 9009);


-- ── 3. 직급 6 — 회사 2 전용 ──────────────────────────────────────────
-- ⚠️ 사원·팀장·대표까지 새로 만든다. 회사 1 의 직급을 참조하면 복합 FK 가 깨진다.
INSERT IGNORE INTO job_position (job_position_id, company_id, name, sort_order) VALUES
(9007, 2, '사원',   1),
(9008, 2, '대리',   2),
(9009, 2, '과장',   3),
(9010, 2, '팀장',   4),
(9011, 2, '본부장', 5),
(9012, 2, '대표',   6);


-- ── 4. 사원 12 ───────────────────────────────────────────────────────
-- ⚠️ user_id 접두사는 'vitawear-' 다. company_code 와 반드시 일치해야 한다.
-- 🎲 이메일은 example TLD(예약 도메인)라 실제 주소가 아니다.
INSERT IGNORE INTO employee
  (user_id, company_id, name, is_system, department_id, job_position_id, email, hired_at) VALUES
  -- 참여자 8명
  ('vitawear-VW101', 2, '김서연', 0, 9010, 9008, 'sy.kim@vitawear.example',  '2022-03-02'),
  ('vitawear-VW102', 2, '박준호', 0, 9010, 9007, 'jh.park@vitawear.example', '2024-01-08'),
  ('vitawear-VW103', 2, '이현우', 0, 9010, 9010, 'hw.lee@vitawear.example',  '2019-09-16'),
  ('vitawear-VW104', 2, '정민아', 0, 9011, 9008, 'ma.jung@vitawear.example', '2021-11-01'),
  ('vitawear-VW105', 2, '최동석', 0, 9012, 9008, 'ds.choi@vitawear.example', '2022-06-13'),
  ('vitawear-VW106', 2, '한지훈', 0, 9014, 9011, 'jh.han@vitawear.example',  '2018-04-02'),
  ('vitawear-VW107', 2, '서영광', 0, 9009, 9012, 'yk.seo@vitawear.example',  '2016-01-04'),
  ('vitawear-VW108', 2, '조은비', 0, 9013, 9009, 'eb.cho@vitawear.example',  '2020-07-20'),
  -- 미참여 4명 — 권한 시연용. 이 계정으로 로그인하면 프로젝트가 목록에 안 떠야 한다 (INV-07)
  ('vitawear-VW109', 2, '윤태경', 0, 9014, 9009, 'tk.yoon@vitawear.example', '2021-02-15'),
  ('vitawear-VW110', 2, '강민석', 0, 9014, 9007, 'ms.kang@vitawear.example', '2025-03-03'),
  ('vitawear-VW111', 2, '노현주', 0, 9015, 9008, 'hj.noh@vitawear.example',  '2023-08-21'),
  ('vitawear-VW112', 2, '배수진', 0, 9009, 9009, 'sj.bae@vitawear.example',  '2019-05-27');


-- ── 5. 사업 카테고리 2 — 회사 2 전용 ─────────────────────────────────
-- UNIQUE 는 (company_id, name) · (company_id, code) 다 (tenant/V20260811100100).
-- 회사가 다르므로 회사 1 에 같은 이름이 있어도 안 겹친다.
INSERT IGNORE INTO business_category (business_category_id, company_id, name, code, description) VALUES
(9010, 2, '유통/이커머스', 'ECOMMERCE', '온라인 플랫폼 입점·위탁판매'),
(9011, 2, '제조/의류',     'APPAREL',   '의류 기획·생산·OEM');

-- → 프로젝트 연결(`project_business_category`)은 project 가 있어야 하므로 02_project.sql 에 있다.


-- =====================================================================
-- 🚨 계정 부트스트랩 — 이 덤프 밖에서 해야 하는 일
-- =====================================================================
-- 1) 회사 2 의 첫 ADMIN 계정
--    회사 1 의 관리자 화면으로는 만들 수 없다. 사번 접두사를 자기 회사 코드로 붙이기 때문이다.
--    → account 행 1건을 직접 넣는다 (비밀번호 해시는 각자 생성 · 이 파일에 쓰지 않는다).
--      대상 사번: 'vitawear-VW112' (배수진) 또는 별도 시스템 계정.
--
-- 2) 나머지 11명 계정
--    1)의 ADMIN 으로 로그인해 /settings 에서 발급한다.
--
-- 3) page_permission
--    ⚠️ 이 덤프는 화면 권한을 넣지 않는다. 안 주면 탭이 잠긴다 (PERMISSION.md — 숨김이 아니라 잠금).
--      조은비(VW108)  → FINANCE          : 정산 현황·입출금 매칭
--      김서연(VW101)  → MY_PROJECT · PROJECT_CREATE
--      나머지 참여자   → MY_PROJECT
--      ⛔ 미참여 4명에게는 MY_PROJECT 만 주고 프로젝트 참여자로는 넣지 마라 —
--         "권한이 있어도 참여자가 아니면 안 보인다" 가 §7-2 시연의 요지다.
--
-- 4) 배수진(VW112) 에게 MASTER role
--    ⚠️ 원래 목적이던 privileged_override 시연은 컬럼이 없어 불가능하다 (08 파일 참고).
--       그래도 상위권한 계정 하나는 있어야 권한 화면 설명이 된다.
-- =====================================================================
