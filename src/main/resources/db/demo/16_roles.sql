-- =====================================================================
-- 16. 계정 구성 재편 — MEMBER 6 / ADMIN 6 / MASTER 1 (총 13)
-- ---------------------------------------------------------------------
-- 시연 시나리오: MEMBER 6명이 한 팀으로 같은 프로젝트를 본다.
--                ADMIN 6명은 관리자 콘솔·재무·권한 관리를 시연한다.
--                MASTER 1명은 회사 전체 권한.
--
-- ⚠️ 이전 문서에 "ADMIN 은 개발자가 발급하는 시스템 계정이라 사원에게 주지 않는다"는
--    주의가 있었다. 이번 시연 요구가 ADMIN 6개라 그 방침을 따르지 않는다.
--    운영 DB 에는 이 배정을 그대로 옮기지 마라.
--
-- 배정 근거 — MEMBER 6명은 실제로 26 S/S 프로젝트를 굴린 실무자다.
--   브랜드 3 (김서연·박준호·이현우) + 디자인 1 (정민아) + 물류 1 (최동석) + 영업 1 (한지훈)
--   ADMIN 6명은 결재 승인권자·재무·관리 쪽으로 몰아둔다.
--
-- 되돌리기: DELETE FROM account WHERE account_id=9013;
--           DELETE FROM employee WHERE user_id='vitawear-VW113';
--           UPDATE account SET role='MEMBER' WHERE account_id BETWEEN 9002 AND 9011;
--           UPDATE account SET role='ADMIN'  WHERE account_id=9001;
-- =====================================================================

-- ── 사원 1명 추가 (ADMIN 6명을 채우려면 13명이 필요하다) ─────────────
INSERT INTO employee (company_id, user_id, name, is_system, department_id, job_position_id, email, hired_at) VALUES
(2, 'vitawear-VW113', '문가영', 0, 9009, 9008, 'vw113@vitawear.example', '2024-03-04');

-- ── 계정 1건 추가 ─────────────────────────────────────────────────
-- 해시는 계정마다 salt 가 다르다. 평문은 이 파일에 없다.
INSERT IGNORE INTO account
  (account_id, user_id, password, role, status, must_change_password, terms_agreed_at) VALUES
(9013, 'vitawear-VW113',
 '$argon2id$v=19$m=65536,t=3,p=1$OLTvVQVD4g3OsDPYB9XN2w$fkl+siMgLMp/pfefxBnDOH2GKKo9C7+/OkScbw+DfH0',
 'ADMIN', 'ACTIVE', 0, NOW());

-- ── 역할 재배정 ───────────────────────────────────────────────────
-- MEMBER 6 — 26 S/S 프로젝트 실무 팀
UPDATE account SET role='MEMBER' WHERE user_id IN (
  'vitawear-VW101',  -- 김서연 브랜드팀 대리   (주담당)
  'vitawear-VW102',  -- 박준호 브랜드팀 사원   (MD 커뮤니케이션)
  'vitawear-VW103',  -- 이현우 브랜드팀 팀장   (검토·결재)
  'vitawear-VW104',  -- 정민아 디자인팀 대리   (룩북·제품컷)
  'vitawear-VW105',  -- 최동석 물류·CS팀 대리  (입고·검품·CS)
  'vitawear-VW106'   -- 한지훈 영업팀 본부장   (승인)
);

-- ADMIN 6 — 결재 승인권자 · 재무 · 관리
UPDATE account SET role='ADMIN' WHERE user_id IN (
  'vitawear-VW107',  -- 서영광 본사 대표
  'vitawear-VW108',  -- 조은비 재무팀 과장
  'vitawear-VW109',  -- 윤태경 영업팀 과장
  'vitawear-VW110',  -- 강민석 영업팀 사원
  'vitawear-VW111',  -- 노현주 생산관리팀 대리
  'vitawear-VW113'   -- 문가영 본사 대리
);

-- MASTER 1
UPDATE account SET role='MASTER' WHERE user_id='vitawear-VW112';  -- 배수진 본사 과장

-- ── 화면 접근 권한 ────────────────────────────────────────────────
-- ADMIN 6명에게 관리자 콘솔·재무를, MEMBER 6명에게 프로젝트 권한을 준다.
DELETE FROM page_permission WHERE user_id LIKE 'vitawear-%';

INSERT INTO page_permission (page_code, user_id, permission)
SELECT 'MY_PROJECT', a.user_id, 'EDITOR' FROM account a WHERE a.user_id LIKE 'vitawear-%';

INSERT INTO page_permission (page_code, user_id, permission)
SELECT 'PROJECT_CREATE', a.user_id, 'EDITOR' FROM account a
 WHERE a.user_id IN ('vitawear-VW101','vitawear-VW103','vitawear-VW106','vitawear-VW112');

INSERT INTO page_permission (page_code, user_id, permission)
SELECT 'ADMIN_CONSOLE', a.user_id, 'EDITOR' FROM account a WHERE a.role IN ('ADMIN','MASTER');

INSERT INTO page_permission (page_code, user_id, permission)
SELECT 'FINANCE', a.user_id, 'EDITOR' FROM account a
 WHERE a.user_id IN ('vitawear-VW108','vitawear-VW112','vitawear-VW113');
