-- =====================================================================
-- 02. 프로젝트 · 참여자 · 스테이지 · 스텝 · 스텝 권한
-- ---------------------------------------------------------------------
-- 무엇: 메인 프로젝트 1건(9001) + 목록용 곁들이 2건의 계층 골격.
-- 왜:   블록·이슈·정산이 전부 step_id 를 물고 들어오므로 스텝이 먼저다.
--
-- 선행: 01_master.sql
--
-- ⚠️ 기준 시점 2026-04-08 — 완료 10 / 진행 중 2 / 미시작 3 → 진척률 67%
-- ⚠️ step.sort_order 는 프로젝트 전체 통번호 1~15 다 (STP-002 · #66 확정).
--    스테이지별 1..n 으로 넣으면 화면은 정상인데 순서가 1,2,3,1,2,3 으로 섞인다.
--
-- 되돌리기: DELETE FROM project_business_category WHERE project_business_category_id BETWEEN 9001 AND 9004;
--           DELETE FROM step_permission WHERE step_id BETWEEN 9001 AND 9015;
--           DELETE FROM step   WHERE project_id IN (9001,9002,9003);
--           DELETE FROM stage  WHERE project_id IN (9001,9002,9003);
--           DELETE FROM project_member WHERE project_id IN (9001,9002,9003);
--           DELETE FROM project WHERE project_id IN (9001,9002,9003);
-- =====================================================================


-- ── 1. 프로젝트 3 ────────────────────────────────────────────────────
-- ⚠️ company_id 는 NOT NULL 이고 DEFAULT 가 제거됐다 (tenant/V20260811110000). 명시 필수.
-- ⚠️ contract_amount 는 NULL 이 정상이다 — 위탁판매는 수수료 계약이라 계약금액이 없다 (PRJ-008).
--    발주액·정산액을 여기 넣지 마라. 금액의 유일한 저장 지점이라는 의미가 깨진다.
INSERT IGNORE INTO project
  (project_id, company_id, bid_notice_id, name, description, status,
   client_name, contract_amount, started_on, ended_on,
   closed_at, close_reason_code, close_reason_note, created_by) VALUES
  (9001, 2, NULL,
   '무신사 스토어 입점 및 26 S/S 시즌 운영',
   'VITAWEAR 26 S/S 무신사 스토어 위탁판매 — 입점 검토부터 월정산까지',
   'IN_PROGRESS', '(주)무신사', NULL, '2025-11-04', '2026-07-31',
   NULL, NULL, NULL, 'vitawear-VW101'),

  -- 곁들이 ① 목록에 완료 상태를 띄운다
  (9002, 2, NULL,
   '25 F/W 무신사 입점',
   '전 시즌 운영 — 참고용',
   'COMPLETED', '(주)무신사', NULL, '2025-04-01', '2025-10-31',
   NULL, NULL, NULL, 'vitawear-VW101'),

  -- 곁들이 ② 종결 사유 필수(PRJ-005)를 목록에서 바로 보여준다
  (9003, 2, NULL,
   '29CM 입점 검토',
   '채널 다변화 검토 — 26 F/W 재검토 예정',
   'CLOSED', '(주)무신사트렌비', NULL, '2025-11-04', NULL,
   '2025-11-14 18:00:00', 'NOT_SELECTED', '무신사 단독 추진으로 결정 — 26 F/W 재검토',
   'vitawear-VW101');


-- ── 2. 참여자 8 (전사 12명 중) ───────────────────────────────────────
-- ⚠️ VIEWER 여도 결재선에 들어갈 수 있다 — 결재 권한은 approval_line 이 갖는다.
--    다만 ApprovalLineEligibilityPolicy 가 결재자에게 project_member 자격을 요구하므로
--    한지훈·서영광이 여기 없으면 07_approval.sql 이 죽는다.
-- ⛔ 미참여 4명(VW109~112)은 넣지 않는다. NONE 이 아니라 「행이 없음」이 차단이다 (PRJ-010).
INSERT IGNORE INTO project_member (project_member_id, project_id, user_id, permission) VALUES
  (9001, 9001, 'vitawear-VW101', 'EDITOR'),  -- 김서연 주담당
  (9002, 9001, 'vitawear-VW102', 'EDITOR'),  -- 박준호
  (9003, 9001, 'vitawear-VW103', 'EDITOR'),  -- 이현우 팀장 · 1차 결재
  (9004, 9001, 'vitawear-VW104', 'EDITOR'),  -- 정민아 디자인
  (9005, 9001, 'vitawear-VW105', 'EDITOR'),  -- 최동석 물류·CS
  (9006, 9001, 'vitawear-VW106', 'VIEWER'),  -- 한지훈 본부장 · 2차 결재
  (9007, 9001, 'vitawear-VW107', 'VIEWER'),  -- 서영광 대표 · 최종 결재
  (9008, 9001, 'vitawear-VW108', 'VIEWER'),  -- 조은비 재무 · 정산은 /finance 에서
  -- 곁들이 2건에도 최소 1명은 있어야 목록에 뜬다
  (9009, 9002, 'vitawear-VW101', 'EDITOR'),
  (9010, 9003, 'vitawear-VW101', 'EDITOR');


-- ── 2-1. 사업 카테고리 연결 4 ────────────────────────────────────────
-- ⚠️ FK 가 둘 다 복합키다 (V20260814170000):
--      (company_id, project_id)           → project
--      (company_id, business_category_id) → business_category
--    company_id 컬럼을 반드시 넣어야 하고, 값은 project.company_id 와 같아야 한다.
-- ⚠️ PRJ-007 — 카테고리는 복수 선택이다. 메인 프로젝트에 2건을 건다.
--    UNIQUE uk_pbc (project_id, business_category_id) 가 같은 카테고리 중복을 막는다.
INSERT IGNORE INTO project_business_category
  (project_business_category_id, company_id, project_id, business_category_id) VALUES
(9001, 2, 9001, 9010),  -- 무신사 입점 → 유통/이커머스
(9002, 2, 9001, 9011),  -- 무신사 입점 → 제조/의류  ⭐ 복수 선택 시연
(9003, 2, 9002, 9010),  -- 25 F/W     → 유통/이커머스
(9004, 2, 9003, 9010);  -- 29CM 검토  → 유통/이커머스

-- 이걸로 /projects 목록의 **카테고리 필터**가 실제로 동작한다.
-- 안 넣으면 필터 드롭다운에 값은 뜨는데 걸러지는 게 없어 고장난 것처럼 보인다.


-- ── 3. 스테이지 6 ────────────────────────────────────────────────────
INSERT IGNORE INTO stage (stage_id, project_id, name, sort_order) VALUES
  (9001, 9001, 'S1 입점 검토',        1),
  (9002, 9001, 'S2 신청·심사',        2),
  (9003, 9001, 'S3 온보딩',           3),
  (9004, 9001, 'S4 26 S/S 시즌운영',  4),
  (9005, 9001, 'S5 월정산',           5),
  (9006, 9001, 'S6 실적·수익성',      6);


-- ── 4. 스텝 15 ───────────────────────────────────────────────────────
-- ⚠️ sort_order = 프로젝트 전체 통번호 1~15. 스테이지 그룹은 FE 가 stage_id 로 묶어 그린다.
-- ⚠️ owner_user_id 는 책임자이지 작업자가 아니다 (STP-003).
INSERT IGNORE INTO step
  (step_id, stage_id, project_id, name, sort_order,
   started_on, ended_on, owner_user_id, status, completed_at, completed_by) VALUES

  -- S1 입점 검토 ── 완료
  (9001, 9001, 9001, '채널 발굴·조건 등재',      1,
   '2025-11-04', '2025-11-25', 'vitawear-VW101', 'DONE', '2025-11-25 17:40:00', 'vitawear-VW101'),
  (9002, 9001, 9001, '사업성 검토·추진 결재',    2,
   '2025-11-26', '2025-12-05', 'vitawear-VW103', 'DONE', '2025-12-05 16:05:00', 'vitawear-VW103'),

  -- S2 신청·심사 ── 완료
  (9003, 9002, 9001, '제출물 작성',              3,
   '2025-12-08', '2025-12-12', 'vitawear-VW101', 'DONE', '2025-12-12 11:20:00', 'vitawear-VW101'),
  (9004, 9002, 9001, '품질검토·제출',            4,
   '2025-12-10', '2025-12-12', 'vitawear-VW103', 'DONE', '2025-12-12 15:00:00', 'vitawear-VW103'),
  (9005, 9002, 9001, '입점 심사 결과',           5,
   '2025-12-12', '2025-12-16', 'vitawear-VW102', 'DONE', '2025-12-16 14:30:00', 'vitawear-VW102'),

  -- S3 온보딩 ── 완료
  (9006, 9003, 9001, '계약 체결·계정 세팅',      6,
   '2025-12-17', '2025-12-30', 'vitawear-VW103', 'DONE', '2025-12-30 17:10:00', 'vitawear-VW103'),
  (9007, 9003, 9001, '상품 등록·검수·오픈',      7,
   '2026-01-05', '2026-01-30', 'vitawear-VW101', 'DONE', '2026-01-30 18:00:00', 'vitawear-VW101'),

  -- S4 시즌운영 ── 1차 완료 / 2차 절반 / 3차 미시작
  (9008, 9004, 9001, '[1차] 발주·입고·검품',     8,
   '2025-12-22', '2026-02-09', 'vitawear-VW105', 'DONE', '2026-02-09 16:40:00', 'vitawear-VW105'),
  (9009, 9004, 9001, '[1차] 노출·판매·CS',       9,
   '2026-02-10', '2026-03-05', 'vitawear-VW102', 'DONE', '2026-03-05 17:25:00', 'vitawear-VW102'),
  (9010, 9004, 9001, '[2차] 발주·입고·검품',    10,
   '2026-03-16', '2026-04-03', 'vitawear-VW105', 'DONE', '2026-04-03 15:50:00', 'vitawear-VW105'),
  -- ⭐ 기준 시점(2026-04-08)에 진행 중인 스텝
  (9011, 9004, 9001, '[2차] 노출·판매·CS',      11,
   '2026-04-06', '2026-05-08', 'vitawear-VW102', 'IN_PROGRESS', NULL, NULL),
  -- ⛔ 3차는 지우지 마라 — "안 했다 + 사유"를 남길 자리다. 블록은 03 에서 빈 껍데기로 만든다.
  (9012, 9004, 9001, '[3차] 발주·입고·검품',    12,
   '2026-05-18', '2026-06-05', 'vitawear-VW105', 'NOT_STARTED', NULL, NULL),
  (9013, 9004, 9001, '[3차] 노출·판매·CS',      13,
   '2026-06-08', '2026-06-30', 'vitawear-VW102', 'NOT_STARTED', NULL, NULL),

  -- S5 월정산 ── ⭐ 회차마다 스텝을 만들지 않는다. 한 스텝에 SETTLEMENT 블록 3개.
  (9014, 9005, 9001, '월정산 (3회차)',          14,
   '2026-03-01', '2026-05-10', 'vitawear-VW108', 'IN_PROGRESS', NULL, NULL),

  -- S6 실적·수익성 ── 미시작
  (9015, 9006, 9001, '시즌 결산·실적 등재·수익성 분석', 15,
   '2026-07-01', '2026-07-31', 'vitawear-VW101', 'NOT_STARTED', NULL, NULL);


-- ── 5. 곁들이 프로젝트의 스텝 껍데기 16 ──────────────────────────────
-- 블록·파일 없이 스텝만. 목록 화면의 진척률·상태 필터를 채우는 게 전부다.
INSERT IGNORE INTO step (step_id, stage_id, project_id, name, sort_order, owner_user_id, status, completed_at, completed_by)
SELECT 9100 + n, NULL, 9002, CONCAT('25 F/W 스텝 ', n), n, 'vitawear-VW101', 'DONE',
       '2025-10-31 18:00:00', 'vitawear-VW101'
FROM (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
      UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12) t;

INSERT IGNORE INTO step (step_id, stage_id, project_id, name, sort_order, owner_user_id, status)
SELECT 9200 + n, NULL, 9003, CONCAT('29CM 검토 스텝 ', n), n, 'vitawear-VW101', 'NOT_STARTED'
FROM (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) t;

-- ⚠️ stage_id 를 NULL 로 둔 건 의도다 — step.stage_id 는 NULL 허용이 정상 케이스이고(STP-001),
--    스테이지 없는 스텝이 목록에서 어떻게 보이는지도 같이 확인된다.


-- ── 6. 스텝 권한 오버라이드 3 — 차단·강등·승격 ───────────────────────
-- ⚠️ 행이 없으면 프로젝트 권한을 그대로 쓴다 (STP-011). 아래 3건만 예외다.
INSERT IGNORE INTO step_permission (step_permission_id, step_id, user_id, permission) VALUES
  -- 차단: 계약 조건·수수료율은 담당자만
  (9001, 9006, 'vitawear-VW104', 'NONE'),
  (9002, 9006, 'vitawear-VW105', 'NONE'),
  -- 강등: 제출물 작성 스텝에 원가가 들어 있다 (프로젝트는 EDITOR)
  (9003, 9003, 'vitawear-VW104', 'VIEWER'),
  -- 승격: 조은비는 프로젝트 VIEWER 인데 정산 스텝만 EDITOR (05_issues.sql 의 담당 이슈 때문에 필요)
  (9004, 9014, 'vitawear-VW108', 'EDITOR');
