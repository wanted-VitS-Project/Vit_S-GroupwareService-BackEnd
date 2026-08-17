-- =====================================================================
-- KB 13. 프로젝트 1(P8011) · 스테이지 7 · 스텝 17 · 참여자 11 · 스텝 권한 4
-- ---------------------------------------------------------------------
-- 무엇: KB 공고 8011 에서 전환된 제안·운영 프로젝트의 계층 골격.
-- 왜:   블록·이슈·정산·결재가 전부 step_id 를 물고 들어오므로 스텝이 먼저다.
--
-- 선행: 12_kb_bid.sql (project.bid_notice_id = 8011 이 공고를 문다)
--
-- ⭐ A안 — 제안·선정은 완료(2025-11~12), 계약·운영·정산이 진행 중(2026-02~).
--    기준 2026-08-16 · 완료 13 / 진행 3 / 미시작 1 → 진척률 76%
--    한 프로젝트가 공고→제안→선정→운영→정산 전 구간과 INCOME·OUTCOME 정산을 모두 담는다.
--
-- ⚠️ step.sort_order 는 프로젝트 전체 통번호 1~17 이다 (STP-002).
--    스테이지별 1..n 으로 넣으면 화면은 정상인데 순서가 1,2,3,1,2,3 으로 섞인다.
-- ⛔ ADMIN(VE112·VE113)은 멤버·결재선 어디에도 넣지 않는다.
--
-- 되돌리기:
--   DELETE FROM step_permission WHERE step_permission_id BETWEEN 8005 AND 8008;
--   DELETE FROM step   WHERE step_id  BETWEEN 8051 AND 8067;
--   DELETE FROM stage  WHERE stage_id BETWEEN 8013 AND 8019;
--   DELETE FROM project_business_category WHERE project_business_category_id BETWEEN 8013 AND 8014;
--   DELETE FROM project_member WHERE project_member_id BETWEEN 8052 AND 8062;
--   DELETE FROM project WHERE project_id = 8011;
-- =====================================================================


-- ── 1. 프로젝트 1 ────────────────────────────────────────────────────
-- ⚠️ company_id 는 NOT NULL. contract_amount 는 선정 후 계약분(VAT 포함·1개사 몫) 396,000,000.
INSERT IGNORE INTO project
  (project_id, company_id, bid_notice_id, name, description, status,
   client_name, contract_amount, started_on, ended_on,
   closed_at, close_reason_code, close_reason_note, created_by) VALUES
(8011, 3, 8011,
 '2026년 KB국민은행 디지털 분야 위탁교육 제안·운영',
 'KB국민은행 디지털 분야 위탁교육 업체 선정 공고에 제안해 선정된 사업. DT기획과 DT개발 과정을 2026년 2월부터 1년간 운영하고 위탁료를 정산한다.',
 'IN_PROGRESS', '주식회사 KB국민은행', 396000000.00, '2025-11-20', '2027-01-31',
 NULL, NULL, NULL, 'vitaedu-VE101');


-- ── 2. 사업 카테고리 연결 2 ──────────────────────────────────────────
-- ⚠️ FK 가 둘 다 복합키 — (company_id, project_id) · (company_id, business_category_id). company_id 필수.
INSERT IGNORE INTO project_business_category
  (project_business_category_id, company_id, project_id, business_category_id) VALUES
(8013, 3, 8011, 8001),
(8014, 3, 8011, 8002);


-- ── 3. 참여자 11 — MEMBER 10 + MASTER 1 전원 ────────────────────────
-- ⚠️ 결재자는 project_member 여야 한다 (ApprovalLineEligibilityPolicy).
--    본부장 VE110·대표 VE111 을 VIEWER 로라도 넣어야 18_kb_approval.sql 이 산다.
-- ⭐ VE109(재무)는 프로젝트 VIEWER 지만 정산 스텝만 EDITOR 로 승격한다 (§8 step_permission).
INSERT IGNORE INTO project_member (project_member_id, project_id, user_id, permission) VALUES
(8052, 8011, 'vitaedu-VE101', 'EDITOR'),
(8053, 8011, 'vitaedu-VE102', 'EDITOR'),
(8054, 8011, 'vitaedu-VE103', 'EDITOR'),
(8055, 8011, 'vitaedu-VE104', 'EDITOR'),
(8056, 8011, 'vitaedu-VE105', 'EDITOR'),
(8057, 8011, 'vitaedu-VE106', 'EDITOR'),
(8058, 8011, 'vitaedu-VE107', 'EDITOR'),
(8059, 8011, 'vitaedu-VE108', 'EDITOR'),
(8060, 8011, 'vitaedu-VE109', 'VIEWER'),
(8061, 8011, 'vitaedu-VE110', 'VIEWER'),
(8062, 8011, 'vitaedu-VE111', 'VIEWER');


-- ── 4. 스테이지 7 ────────────────────────────────────────────────────
INSERT IGNORE INTO stage (stage_id, project_id, name, sort_order) VALUES
(8013, 8011, '공고 검토·참가자격',     1),
(8014, 8011, '제안 전략·과정 설계',    2),
(8015, 8011, '제안서 작성',           3),
(8016, 8011, '가격·제출서류',         4),
(8017, 8011, '제출·PT·선정',          5),
(8018, 8011, '계약·과정 운영',        6),
(8019, 8011, '위탁료 정산·외주 관리', 7);


-- ── 5. 스텝 17 (통번호 1~17) ─────────────────────────────────────────
-- 완료 13 / 진행 3 / 미시작 1. owner_user_id 는 책임자이지 작업자가 아니다 (STP-003).
INSERT IGNORE INTO step
  (step_id, stage_id, project_id, name, sort_order,
   started_on, ended_on, owner_user_id, status, completed_at, completed_by) VALUES

-- 공고 검토·참가자격 ── 완료
(8051, 8013, 8011, '공고 접수·요건 분석',              1,
 '2025-11-20', '2025-11-21', 'vitaedu-VE101', 'DONE', '2025-11-21 17:00:00', 'vitaedu-VE101'),
(8052, 8013, 8011, '참가자격 자체점검',                2,
 '2025-11-21', '2025-11-22', 'vitaedu-VE103', 'DONE', '2025-11-22 16:00:00', 'vitaedu-VE103'),

-- 제안 전략·과정 설계 ── 완료
(8053, 8014, 8011, 'DT기획·DT개발 커리큘럼 설계',       3,
 '2025-11-22', '2025-11-24', 'vitaedu-VE106', 'DONE', '2025-11-24 18:00:00', 'vitaedu-VE106'),
(8054, 8014, 8011, '일반 사이버과정·학습로드맵 구성',   4,
 '2025-11-24', '2025-11-25', 'vitaedu-VE107', 'DONE', '2025-11-25 17:00:00', 'vitaedu-VE107'),
(8055, 8014, 8011, '기술·운영 역량 정리',              5,
 '2025-11-24', '2025-11-25', 'vitaedu-VE108', 'DONE', '2025-11-25 18:00:00', 'vitaedu-VE108'),

-- 제안서 작성 ── 완료
(8056, 8015, 8011, '제안서 본문 작성',                 6,
 '2025-11-25', '2025-11-26', 'vitaedu-VE102', 'DONE', '2025-11-26 19:00:00', 'vitaedu-VE102'),
(8057, 8015, 8011, '요약본·품질 검토',                 7,
 '2025-11-26', '2025-11-27', 'vitaedu-VE103', 'DONE', '2025-11-27 15:00:00', 'vitaedu-VE103'),

-- 가격·제출서류 ── 완료
(8058, 8016, 8011, '가격 제안서 산정',                 8,
 '2025-11-26', '2025-11-27', 'vitaedu-VE109', 'DONE', '2025-11-27 18:00:00', 'vitaedu-VE109'),
(8059, 8016, 8011, '제출서류·별지 준비',               9,
 '2025-11-27', '2025-11-28', 'vitaedu-VE102', 'DONE', '2025-11-28 14:00:00', 'vitaedu-VE102'),

-- 제출·PT·선정 ── 완료
(8060, 8017, 8011, '제안서 제출',                     10,
 '2025-11-28', '2025-11-28', 'vitaedu-VE101', 'DONE', '2025-11-28 16:30:00', 'vitaedu-VE101'),
(8061, 8017, 8011, 'PT·선정 결과 확인',               11,
 '2025-12-09', '2025-12-12', 'vitaedu-VE101', 'DONE', '2025-12-12 15:00:00', 'vitaedu-VE101'),

-- 계약·과정 운영 ── 계약 완료 · 상반기 진행 · 하반기 껍데기
(8062, 8018, 8011, '계약 체결',                       12,
 '2026-02-02', '2026-02-06', 'vitaedu-VE109', 'DONE', '2026-02-06 16:00:00', 'vitaedu-VE109'),
(8063, 8018, 8011, '상반기 집합과정 운영',             13,
 '2026-03-07', '2026-08-29', 'vitaedu-VE105', 'IN_PROGRESS', NULL, NULL),
-- ⚠️ 하반기 STEP Ⅲ/Ⅳ(주니어PO·금융플랫폼전문가)는 연간 1회라 아직 개설 전. 블록만 만들고 내용을 비운다.
(8064, 8018, 8011, '하반기·사이버과정 운영',           14,
 '2026-09-05', '2026-12-19', 'vitaedu-VE104', 'NOT_STARTED', NULL, NULL),

-- 위탁료 정산·외주 관리 ── ⭐ INCOME·OUTCOME 정산이 여기 붙는다
(8065, 8019, 8011, '외주 업체 계약',                   15,
 '2026-02-09', '2026-02-20', 'vitaedu-VE109', 'DONE', '2026-02-20 17:00:00', 'vitaedu-VE109'),
(8066, 8019, 8011, '위탁료 청구·수납',                 16,
 '2026-07-01', '2026-11-30', 'vitaedu-VE109', 'IN_PROGRESS', NULL, NULL),
(8067, 8019, 8011, '강사·콘텐츠 외주비 지급',          17,
 '2026-02-25', '2026-09-30', 'vitaedu-VE109', 'IN_PROGRESS', NULL, NULL);


-- ── 6. 스텝 권한 오버라이드 4 — 승격·강등·차단 ──────────────────────
-- ⚠️ 행이 없으면 프로젝트 권한을 그대로 쓴다 (STP-011). 아래 4건만 예외다.
INSERT IGNORE INTO step_permission (step_permission_id, step_id, user_id, permission) VALUES
-- 승격: VE109 는 프로젝트 VIEWER 인데 위탁료 청구 스텝만 EDITOR
(8005, 8066, 'vitaedu-VE109', 'EDITOR'),
-- 강등: 가격 제안 스텝에 1인당 단가·원가 산출이 있다
(8006, 8058, 'vitaedu-VE105', 'VIEWER'),
-- 차단: 외주 계약 스텝에 업체 견적·단가가 있다
(8007, 8065, 'vitaedu-VE102', 'NONE'),
(8008, 8065, 'vitaedu-VE105', 'NONE');


-- =====================================================================
-- 검증
-- =====================================================================
-- 1) 진척률 — DONE 13 / IN_PROGRESS 3 / NOT_STARTED 1
--    SELECT status, COUNT(*) FROM step WHERE project_id = 8011 AND deleted_at IS NULL GROUP BY status;
--
-- 2) sort_order 가 프로젝트 통번호인가 (0행이어야 정상)
--    SELECT project_id FROM step WHERE project_id = 8011
--    GROUP BY project_id HAVING COUNT(*) <> MAX(sort_order) OR MIN(sort_order) <> 1;
--
-- 3) ⛔ ADMIN 이 멤버에 섞이지 않았나 (0행이어야 정상)
--    SELECT * FROM project_member WHERE project_id = 8011 AND user_id IN ('vitaedu-VE112','vitaedu-VE113');
--
-- 4) 공고 → 프로젝트 연결
--    SELECT p.project_id, p.name, n.notice_name FROM project p
--    JOIN bid_notice n ON n.bid_notice_id = p.bid_notice_id WHERE p.project_id = 8011;
