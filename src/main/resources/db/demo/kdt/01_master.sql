-- =====================================================================
-- KDT 01. 테넌트 — 회사 3 · 부서 6 · 직급 7 · 사원 13 · 사업 카테고리 3
-- ---------------------------------------------------------------------
-- 무엇: company_id = 3 (`vitaedu`) 를 새로 만들고 그 안에 조직·인원을 채운다.
-- 왜:   company 1(`vitas`)는 팀이 쓰고 company 2(`vitawear`)는 의류 데모가 쓴다.
--       같은 테넌트에 섞으면 사번이 충돌하고 목록·권한 화면이 서로 오염된다.
--
-- 🚨 접두사만 바꾸면 안 되는 이유
--    앱이 사번을 직접 만든다 — EmployeeCommandService·EmployeeBulkService 둘 다
--      userId = companyCode + "-" + baseUserId
--    company 1 에 'vitaedu-VE101' 을 넣으면 그 회사에서 새로 만든 사원은
--    'vitas-...' 가 되어 **한 회사 안에 접두사 두 종류**가 생긴다.
--    ('vitaedu-VE101' 은 13자 · employee.user_id 는 varchar(20) 이라 여유)
--
-- ⚠️ 회사가 갈리면 마스터도 전부 갈린다
--    department 부모 FK 가 복합키 (company_id, parent_id) 이고
--    employee 의 부서·직급 FK 도 (company_id, department_id)·(company_id, job_position_id) 다.
--    회사 1·2 의 '본사'·'사원'·'팀장'·'대표' 를 그대로 참조할 수 없다.
--
-- 🚨 account(로그인 계정)는 넣지 않는다 — FLYWAY.md §4 「더미 비밀번호 금지」. PUBLIC 레포다.
--    계정 부트스트랩은 README.md 「계정 만들기」 참고.
--
-- 되돌리기:
--   DELETE FROM business_category WHERE company_id = 3;
--   DELETE FROM employee          WHERE company_id = 3;
--   DELETE FROM job_position      WHERE company_id = 3;
--   DELETE FROM department        WHERE company_id = 3 AND parent_id IS NOT NULL;
--   DELETE FROM department        WHERE company_id = 3;
--   DELETE FROM company           WHERE company_id = 3;
-- =====================================================================


-- ── 1. 회사(테넌트) ──────────────────────────────────────────────────
-- ⚠️ company_code 가 사번 접두사가 된다. UNIQUE 이므로 'vitas'·'vitawear' 와 안 겹친다.
INSERT IGNORE INTO company (company_id, name, company_code) VALUES
(3, '주식회사 비타에듀', 'vitaedu');


-- ── 2. 부서 6 ────────────────────────────────────────────────────────
-- 공고문 「운영조직·인력」 심사항목과 운영계획서의 행정인력 / SME / 교·강사 구분을 그대로 옮겼다.
-- ⚠️ 본사(8001)를 먼저 넣어야 한다. 나머지가 부모로 참조한다.
--    UNIQUE 는 (company_id, parent_key, name) 이고 parent_key 는 생성 컬럼이라 안 넣는다.
INSERT IGNORE INTO department (department_id, company_id, name, parent_id) VALUES
(8001, 3, '본사', NULL);

INSERT IGNORE INTO department (department_id, company_id, name, parent_id) VALUES
(8002, 3, '사업기획실',   8001),
(8003, 3, '교육운영팀',   8001),
(8004, 3, '콘텐츠개발팀', 8001),
(8005, 3, '플랫폼팀',     8001),
(8006, 3, '경영지원팀',   8001);


-- ── 3. 직급 7 ────────────────────────────────────────────────────────
-- ⚠️ 본부장을 따로 둔다. 결재선이 담당 → 팀장 → 본부장 → 대표 4단이라
--    팀장과 본부장이 한 직급이면 결재 단계가 화면에서 구분되지 않는다.
INSERT IGNORE INTO job_position (job_position_id, company_id, name, sort_order) VALUES
(8001, 3, '사원',   1),
(8002, 3, '주임',   2),
(8003, 3, '대리',   3),
(8004, 3, '과장',   4),
(8005, 3, '팀장',   5),
(8006, 3, '본부장', 6),
(8007, 3, '대표',   7);


-- ── 4. 사원 13 ───────────────────────────────────────────────────────
-- ⚠️ user_id 접두사는 'vitaedu-' 다. company_code 와 반드시 일치해야 한다.
-- 🎲 이메일은 example TLD(예약 도메인)라 실제 주소가 아니다.
--
-- ⭐ VE104(사업전담인력)와 VE108(LMS·전산 담당)은 없으면 부적합 판정 대상이다.
--    운영계획서 「사업전담인력 1인 이상, 없으면 부적합」 ·
--    공고문 과정인정요건 「전산시스템 및 LMS 관련 업무 담당자 1인 이상 배치」
INSERT IGNORE INTO employee
  (user_id, company_id, name, is_system, department_id, job_position_id, email, hired_at) VALUES
  ('vitaedu-VE101', 3, '강태현', 0, 8002, 8004, 'th.kang@vitaedu.example', '2021-06-14'),
  ('vitaedu-VE102', 3, '윤하람', 0, 8002, 8001, 'hr.yoon@vitaedu.example', '2025-02-03'),
  ('vitaedu-VE103', 3, '남기훈', 0, 8002, 8005, 'kh.nam@vitaedu.example',  '2021-03-02'),
  ('vitaedu-VE104', 3, '배규리', 0, 8003, 8003, 'gr.bae@vitaedu.example',  '2022-08-16'),
  ('vitaedu-VE105', 3, '문지환', 0, 8003, 8001, 'jh.moon@vitaedu.example', '2024-11-04'),
  ('vitaedu-VE106', 3, '오세아', 0, 8004, 8005, 'sa.oh@vitaedu.example',   '2021-05-10'),
  ('vitaedu-VE107', 3, '신재호', 0, 8004, 8003, 'jh.shin@vitaedu.example', '2023-01-09'),
  ('vitaedu-VE108', 3, '임채린', 0, 8005, 8004, 'cr.lim@vitaedu.example',  '2022-04-18'),
  ('vitaedu-VE109', 3, '하성민', 0, 8006, 8004, 'sm.ha@vitaedu.example',   '2021-09-13'),
  ('vitaedu-VE110', 3, '권다인', 0, 8001, 8006, 'di.kwon@vitaedu.example', '2021-03-02'),
  ('vitaedu-VE111', 3, '서정원', 0, 8001, 8007, 'jw.seo@vitaedu.example',  '2021-01-04'),
  -- ⛔ 아래 2명은 ADMIN 계정용이다. 프로젝트 참여자·결재선·기안 어디에도 넣지 마라.
  --    `.ai/api/approval.md` 상 ADMIN 은 모든 범위에서 결재 권한이 없다 (ApprovalListScopePolicy).
  ('vitaedu-VE112', 3, '운영관리자', 1, 8006, 8004, 'ops@vitaedu.example',     '2021-01-04'),
  ('vitaedu-VE113', 3, '계정관리자', 1, 8006, 8003, 'account@vitaedu.example', '2021-01-04');


-- ── 5. 사업 카테고리 3 ───────────────────────────────────────────────
-- UNIQUE 는 (company_id, name)·(company_id, code) 다. 회사가 다르므로 회사 1·2 와 안 겹친다.
INSERT IGNORE INTO business_category (business_category_id, company_id, name, code, description) VALUES
(8001, 3, '정부지원 훈련', 'GOV_TRAINING', 'K-디지털 기초역량훈련·내일배움카드 등 재정지원 훈련'),
(8002, 3, '위탁 교육',     'CONTRACT_EDU', '지자체·기업 위탁 교육 용역'),
(8003, 3, '플랫폼 운영',   'PLATFORM',     '자체 LMS 구축·고도화·인증 대응');

-- → 프로젝트 연결(`project_business_category`)은 project 가 있어야 하므로 03_project.sql 에 있다.


-- =====================================================================
-- 🚨 계정 부트스트랩 — 이 덤프 밖에서 해야 하는 일
-- =====================================================================
-- 1) 회사 3 의 첫 ADMIN 계정
--    다른 회사의 관리자 화면으로는 만들 수 없다 (사번 접두사가 자기 회사 코드로 붙는다).
--    → account 행 1건을 직접 넣는다. 대상 사번 'vitaedu-VE112'.
--      비밀번호 해시는 각자 생성하고 **이 파일에도, 어떤 커밋 파일에도 쓰지 않는다.**
--
-- 2) 나머지 12명 계정은 1)의 ADMIN 으로 로그인해 /settings 에서 발급한다.
--    role 배분: MEMBER 10 (VE101~110) · MASTER 1 (VE111 대표) · ADMIN 2 (VE112·VE113)
--    ⚠️ must_change_password = 0 · terms_agreed_at 채움 —
--       이 둘을 비우면 로그인 직후 비밀번호 변경·약관 화면에 막혀 시연이 안 된다.
--
-- 3) 화면 접근 권한은 10_page_permission.sql 에 있다.
--    ⚠️ BIDDING 이 없으면 공고 화면이 **잠긴다**(숨김이 아니다).
-- =====================================================================
