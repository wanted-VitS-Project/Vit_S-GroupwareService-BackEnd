-- =====================================================================
-- KDT 03. 프로젝트 10 · 스테이지 12 · 스텝 50 · 참여자 · 스텝 권한
-- ---------------------------------------------------------------------
-- 무엇: 꽉 찬 프로젝트 2건(8001·8002) + 목록용 8건의 계층 골격.
-- 왜:   블록·이슈·정산·결재가 전부 step_id 를 물고 들어오므로 스텝이 먼저다.
--
-- 선행: 01_master.sql · 02_bid.sql (project.bid_notice_id 가 공고를 문다)
--
-- ⚠️ 기준 시점 2026-08-16 — 신청 마감(08-25 18시) 9일 전
--    P8001 완료 8 / 진행 4 / 미시작 6 → 진척률 44%
--
-- 🚨 왜 꽉 찬 프로젝트가 2개인가
--    공고 일정을 그대로 따르면 심사는 9월, 결과는 11-11 이라 기준 시점에 훈련비 수입이
--    물리적으로 존재할 수 없다. 신청 프로젝트 하나만 만들면 재무 화면 절반이 빈다.
--    → 돈이 나가는 프로젝트(8001 · OUTCOME)와 들어오는 프로젝트(8002 · INCOME)를 나눈다.
--    ⛔ 둘 다 IN_PROGRESS 여야 한다. /finance/settlements 는 includeCompleted 생략 시
--       COMPLETED·CLOSED 를 뺀다 — 하나라도 완료로 두면 그 정산이 재무 화면에서 사라진다.
--
-- ⚠️ step.sort_order 는 프로젝트 전체 통번호다 (STP-002).
--    스테이지별 1..n 으로 넣으면 화면은 정상인데 순서가 1,2,3,1,2,3 으로 섞인다.
--
-- 되돌리기: DELETE FROM step_permission WHERE step_permission_id BETWEEN 8001 AND 8004;
--           DELETE FROM project_business_category WHERE project_business_category_id BETWEEN 8001 AND 8012;
--           DELETE FROM step   WHERE project_id BETWEEN 8001 AND 8010;
--           DELETE FROM stage  WHERE project_id BETWEEN 8001 AND 8010;
--           DELETE FROM project_member WHERE project_id BETWEEN 8001 AND 8010;
--           DELETE FROM project WHERE project_id BETWEEN 8001 AND 8010;
-- =====================================================================


-- ── 1. 프로젝트 10 ───────────────────────────────────────────────────
-- ⚠️ company_id 는 NOT NULL 이고 DEFAULT 가 없다. 명시 필수.
-- ⚠️ contract_amount 는 심사 신청에 없다. 훈련비는 인원×단가로 사후 정산되지 계약금액이 아니다.
--    발주액·정산액을 여기 넣지 마라.
INSERT IGNORE INTO project
  (project_id, company_id, bid_notice_id, name, description, status,
   client_name, contract_amount, started_on, ended_on,
   closed_at, close_reason_code, close_reason_note, created_by) VALUES

-- ⭐ 메인 — 공고 8001 에서 전환된 프로젝트
(8001, 3, 8001,
 '2026년 K-디지털 기초역량훈련 심사 신청',
 'Pre-KDT 유형 「KDT 진입을 위한 AI 기초」 40시간 과정 신규 신청. 공고 접수부터 선정 후 개설까지',
 'IN_PROGRESS', '직업능력심사평가원', NULL, '2026-07-31', '2026-11-30',
 NULL, NULL, NULL, 'vitaedu-VE101'),

-- ⭐ 2025년 선정분 운영 — 훈련비가 들어오는 쪽
(8002, 3, NULL,
 'AI 도구 활용 업무 자동화 입문 과정 운영',
 '2025년 심사 선정 과정. 16시간 원격훈련 · 1기 종료 · 2기 운영 중 · 훈련비 4회차 정산',
 'IN_PROGRESS', '한국산업인력공단', NULL, '2025-12-01', '2026-12-31',
 NULL, NULL, NULL, 'vitaedu-VE104'),

-- 목록용 8건
(8003, 3, NULL, '2025년 K-디지털 기초역량훈련 심사 신청',
 '전년도 심사 대응. 1개 과정 선정',
 'COMPLETED', '직업능력심사평가원', NULL, '2025-08-01', '2025-11-28',
 NULL, NULL, NULL, 'vitaedu-VE101'),

(8004, 3, NULL, 'K-디지털 트레이닝(KDT) 심사 신청',
 '장기과정 진입 시도. 미선정',
 'CLOSED', '직업능력심사평가원', NULL, '2026-02-02', NULL,
 '2026-05-20 17:00:00', 'NOT_SELECTED',
 '운영역량 영역에서 기준점에 미달했다. 2027년 재신청 전에 전담 인력을 늘린다', 'vitaedu-VE103'),

(8005, 3, NULL, '국민내일배움카드 일반 원격과정 인정 신청',
 '자체 개발 원격과정 3건 인정 신청',
 'COMPLETED', '한국산업인력공단', NULL, '2025-09-15', '2026-01-30',
 NULL, NULL, NULL, 'vitaedu-VE104'),

(8006, 3, NULL, '○○광역시 시민 디지털 역량교육 위탁 용역',
 '공고 검토 중. 제안서 작성 착수 전',
 'IN_PROGRESS', '○○광역시 평생교육진흥원', 148000000.00, '2026-08-11', '2026-12-20',
 NULL, NULL, NULL, 'vitaedu-VE101'),

(8007, 3, NULL, '재직자 위탁교육 (AI 사무자동화)',
 '자비부담 비환급 과정. 신청자격 수료 인원 산정의 근거가 되는 실적',
 'COMPLETED', '◇◇테크노파크', 63000000.00, '2025-03-03', '2025-12-19',
 NULL, NULL, NULL, 'vitaedu-VE104'),

(8008, 3, NULL, '자체 LMS 진도·평가 모듈 고도화',
 '과정인정요건 대응. 진도율 산정과 평가 결과 확인 기능 보강',
 'IN_PROGRESS', NULL, 22000000.00, '2026-05-11', '2026-09-30',
 NULL, NULL, NULL, 'vitaedu-VE108'),

(8009, 3, NULL, '원격훈련 인증평가 대응',
 '인증등급 확보 시도. 확보되면 내년 심사에서 신청자격 ①번을 쓸 수 있다',
 'NOT_STARTED', '직업능력심사평가원', NULL, '2026-10-05', '2027-02-27',
 NULL, NULL, NULL, 'vitaedu-VE103'),

(8010, 3, NULL, '2025년 선정과정 콘텐츠 변경심사',
 'AI 도구 버전 변경분 반영. 공고문 §5 변경심사',
 'IN_PROGRESS', '직업능력심사평가원', NULL, '2026-06-01', '2026-09-30',
 NULL, NULL, NULL, 'vitaedu-VE106');


-- ── 2. 사업 카테고리 연결 12 ─────────────────────────────────────────
-- ⚠️ FK 가 둘 다 복합키다 — (company_id, project_id) · (company_id, business_category_id).
--    company_id 를 반드시 넣어야 하고 값은 project.company_id 와 같아야 한다.
INSERT IGNORE INTO project_business_category
  (project_business_category_id, company_id, project_id, business_category_id) VALUES
(8001, 3, 8001, 8001),
(8002, 3, 8002, 8001),
(8003, 3, 8003, 8001),
(8004, 3, 8004, 8001),
(8005, 3, 8005, 8001),
(8006, 3, 8006, 8002),
(8007, 3, 8007, 8002),
(8008, 3, 8008, 8003),
(8009, 3, 8009, 8003),
(8010, 3, 8010, 8001),
-- ⭐ 복수 선택 시연 — 메인 프로젝트에 2건
(8011, 3, 8001, 8003),
(8012, 3, 8002, 8002);


-- ── 3. 참여자 ────────────────────────────────────────────────────────
-- ⚠️ 결재자는 project_member 여야 한다 (ApprovalLineEligibilityPolicy).
--    VE103·VE106(팀장) · VE110(본부장) · VE111(대표)이 없으면 08_approval.sql 이 죽는다.
-- ⛔ ADMIN(VE112·VE113)은 어떤 프로젝트에도 넣지 않는다 —
--    `.ai/api/approval.md` 상 ADMIN 은 결재 권한이 없고 내 프로젝트도 못 본다.
-- ⛔ VE105(문지환)는 P8001 에서 뺀다. NONE 이 아니라 「행이 없음」이 차단이다 (PRJ-010).
INSERT IGNORE INTO project_member (project_member_id, project_id, user_id, permission) VALUES
-- P8001 심사 신청 — 10명 (VE105 제외)
(8001, 8001, 'vitaedu-VE101', 'EDITOR'),
(8002, 8001, 'vitaedu-VE102', 'EDITOR'),
(8003, 8001, 'vitaedu-VE103', 'EDITOR'),
(8004, 8001, 'vitaedu-VE104', 'EDITOR'),
(8005, 8001, 'vitaedu-VE106', 'EDITOR'),
(8006, 8001, 'vitaedu-VE107', 'EDITOR'),
(8007, 8001, 'vitaedu-VE108', 'EDITOR'),
(8008, 8001, 'vitaedu-VE109', 'VIEWER'),
(8009, 8001, 'vitaedu-VE110', 'VIEWER'),
(8010, 8001, 'vitaedu-VE111', 'VIEWER'),

-- P8002 과정 운영 — 11명 전원
(8011, 8002, 'vitaedu-VE101', 'VIEWER'),
(8012, 8002, 'vitaedu-VE102', 'EDITOR'),
(8013, 8002, 'vitaedu-VE103', 'EDITOR'),
(8014, 8002, 'vitaedu-VE104', 'EDITOR'),
(8015, 8002, 'vitaedu-VE105', 'EDITOR'),
(8016, 8002, 'vitaedu-VE106', 'VIEWER'),
(8017, 8002, 'vitaedu-VE107', 'EDITOR'),
(8018, 8002, 'vitaedu-VE108', 'EDITOR'),
(8019, 8002, 'vitaedu-VE109', 'VIEWER'),
(8020, 8002, 'vitaedu-VE110', 'VIEWER'),
(8021, 8002, 'vitaedu-VE111', 'VIEWER'),

-- 목록용 — 결재선에 필요한 인원만
(8022, 8003, 'vitaedu-VE101', 'EDITOR'), (8023, 8003, 'vitaedu-VE103', 'EDITOR'),
(8024, 8003, 'vitaedu-VE110', 'VIEWER'), (8025, 8003, 'vitaedu-VE111', 'VIEWER'),
(8026, 8004, 'vitaedu-VE103', 'EDITOR'), (8027, 8004, 'vitaedu-VE106', 'EDITOR'),
(8028, 8004, 'vitaedu-VE110', 'VIEWER'), (8029, 8004, 'vitaedu-VE111', 'VIEWER'),
(8030, 8005, 'vitaedu-VE104', 'EDITOR'), (8031, 8005, 'vitaedu-VE102', 'EDITOR'),
(8032, 8005, 'vitaedu-VE110', 'VIEWER'), (8033, 8005, 'vitaedu-VE111', 'VIEWER'),
(8034, 8006, 'vitaedu-VE101', 'EDITOR'), (8035, 8006, 'vitaedu-VE103', 'EDITOR'),
(8036, 8006, 'vitaedu-VE109', 'VIEWER'), (8037, 8006, 'vitaedu-VE111', 'VIEWER'),
(8038, 8007, 'vitaedu-VE104', 'EDITOR'), (8039, 8007, 'vitaedu-VE105', 'EDITOR'),
(8040, 8007, 'vitaedu-VE109', 'VIEWER'), (8041, 8007, 'vitaedu-VE111', 'VIEWER'),
-- ⭐ P8008 은 3명뿐이다 — 「권한이 있어도 참여자가 아니면 안 보인다」(INV-07) 시연 대상
(8042, 8008, 'vitaedu-VE108', 'EDITOR'), (8043, 8008, 'vitaedu-VE101', 'VIEWER'),
(8044, 8008, 'vitaedu-VE111', 'VIEWER'),
(8045, 8009, 'vitaedu-VE103', 'EDITOR'), (8046, 8009, 'vitaedu-VE108', 'EDITOR'),
(8047, 8009, 'vitaedu-VE111', 'VIEWER'),
(8048, 8010, 'vitaedu-VE106', 'EDITOR'), (8049, 8010, 'vitaedu-VE107', 'EDITOR'),
(8050, 8010, 'vitaedu-VE110', 'VIEWER'), (8051, 8010, 'vitaedu-VE111', 'VIEWER');


-- ── 4. 스테이지 12 ───────────────────────────────────────────────────
INSERT IGNORE INTO stage (stage_id, project_id, name, sort_order) VALUES
-- P8001
(8001, 8001, 'S1 공고 검토·신청자격',   1),
(8002, 8001, 'S2 훈련과정 설계',        2),
(8003, 8001, 'S3 콘텐츠·LMS 준비',      3),
(8004, 8001, 'S4 신청서류 작성·제출',   4),
(8005, 8001, 'S5 외주 관리',            5),
(8006, 8001, 'S6 심사 대응',            6),
(8007, 8001, 'S7 선정 후 개설',         7),
-- P8002
(8008, 8002, 'S1 과정 개설·모집',       1),
(8009, 8002, 'S2 1기 운영',             2),
(8010, 8002, 'S3 2기 운영',             3),
(8011, 8002, 'S4 훈련비 정산',          4),
(8012, 8002, 'S5 성과관리',             5);


-- ── 5. P8001 스텝 18 ─────────────────────────────────────────────────
-- 완료 8 / 진행 4 / 미시작 6
-- ⚠️ owner_user_id 는 책임자이지 작업자가 아니다 (STP-003).
INSERT IGNORE INTO step
  (step_id, stage_id, project_id, name, sort_order,
   started_on, ended_on, owner_user_id, status, completed_at, completed_by) VALUES

-- S1 공고 검토·신청자격 ── 완료
(8001, 8001, 8001, '공고 접수·요건 분석',            1,
 '2026-07-31', '2026-08-04', 'vitaedu-VE101', 'DONE', '2026-08-04 17:30:00', 'vitaedu-VE101'),
(8002, 8001, 8001, '신청자격·기본심사 자체점검',      2,
 '2026-08-04', '2026-08-06', 'vitaedu-VE103', 'DONE', '2026-08-06 16:10:00', 'vitaedu-VE103'),

-- S2 훈련과정 설계 ── 완료
(8003, 8002, 8001, '훈련유형 선정·과정 기획',        3,
 '2026-08-05', '2026-08-07', 'vitaedu-VE106', 'DONE', '2026-08-07 18:00:00', 'vitaedu-VE106'),
(8004, 8002, 8001, '커리큘럼·훈련시간 산정',          4,
 '2026-08-07', '2026-08-11', 'vitaedu-VE106', 'DONE', '2026-08-11 15:40:00', 'vitaedu-VE106'),
(8005, 8002, 8001, '실습과제·프로젝트 설계',          5,
 '2026-08-08', '2026-08-12', 'vitaedu-VE107', 'DONE', '2026-08-12 17:20:00', 'vitaedu-VE107'),

-- S3 콘텐츠·LMS 준비 ── 1건 진행 중
(8006, 8003, 8001, '콘텐츠 제작·검수',                6,
 '2026-07-13', '2026-08-22', 'vitaedu-VE107', 'IN_PROGRESS', NULL, NULL),
-- ⭐ 스텝은 완료지만 자막 이슈 1건을 미완으로 남긴다 (06_issues.sql · 「완료된 스텝」 배지)
(8007, 8003, 8001, '자막 제작·품질 점검',             7,
 '2026-07-27', '2026-08-14', 'vitaedu-VE107', 'DONE', '2026-08-14 18:10:00', 'vitaedu-VE107'),
(8008, 8003, 8001, 'LMS 심사계정·확인경로 세팅',      8,
 '2026-08-10', '2026-08-15', 'vitaedu-VE108', 'DONE', '2026-08-15 14:00:00', 'vitaedu-VE108'),

-- S4 신청서류 작성·제출 ── 진행 2 / 미시작 2
(8009, 8004, 8001, '훈련운영계획서 작성',             9,
 '2026-08-08', '2026-08-21', 'vitaedu-VE101', 'IN_PROGRESS', NULL, NULL),
(8010, 8004, 8001, '훈련과정개요서·참여인력 목록',   10,
 '2026-08-10', '2026-08-21', 'vitaedu-VE102', 'IN_PROGRESS', NULL, NULL),
(8011, 8004, 8001, '확인서·서약서·동의서 날인',      11,
 '2026-08-19', '2026-08-22', 'vitaedu-VE102', 'NOT_STARTED', NULL, NULL),
(8012, 8004, 8001, 'HRD-Net 제출',                   12,
 '2026-08-24', '2026-08-25', 'vitaedu-VE101', 'NOT_STARTED', NULL, NULL),

-- S5 외주 관리 ── ⭐ OUTCOME 정산이 여기 붙는다
(8013, 8005, 8001, '외주 업체 계약',                 13,
 '2026-06-22', '2026-07-08', 'vitaedu-VE109', 'DONE', '2026-07-08 16:00:00', 'vitaedu-VE109'),
(8014, 8005, 8001, '콘텐츠 개발 외주비 지급',        14,
 '2026-07-10', '2026-09-30', 'vitaedu-VE109', 'IN_PROGRESS', NULL, NULL),
(8015, 8005, 8001, '자막·촬영 용역비 지급',          15,
 '2026-07-25', '2026-08-22', 'vitaedu-VE109', 'DONE', '2026-08-05 15:30:00', 'vitaedu-VE109'),

-- S6·S7 ── 미도래. 블록은 만들되 내용을 비운다 (BLOCK.md §1 규약 5)
(8016, 8006, 8001, '서면심사 대응',                  16,
 '2026-09-07', '2026-10-16', 'vitaedu-VE101', 'NOT_STARTED', NULL, NULL),
(8017, 8006, 8001, '현장심사 인터뷰·과정인정요건',   17,
 '2026-10-19', '2026-11-06', 'vitaedu-VE108', 'NOT_STARTED', NULL, NULL),
(8018, 8007, 8001, '결과 확인·개설 준비',            18,
 '2026-11-11', '2026-11-30', 'vitaedu-VE104', 'NOT_STARTED', NULL, NULL);


-- ── 6. P8002 스텝 8 ──────────────────────────────────────────────────
INSERT IGNORE INTO step
  (step_id, stage_id, project_id, name, sort_order,
   started_on, ended_on, owner_user_id, status, completed_at, completed_by) VALUES
(8019, 8008, 8002, '과정 개설 신고·홍보',            1,
 '2025-12-01', '2025-12-19', 'vitaedu-VE104', 'DONE', '2025-12-19 17:00:00', 'vitaedu-VE104'),
(8020, 8008, 8002, '1기 모집·선발',                  2,
 '2025-12-22', '2026-01-09', 'vitaedu-VE105', 'DONE', '2026-01-09 18:00:00', 'vitaedu-VE105'),
(8021, 8009, 8002, '1기 학습 진도·독려',             3,
 '2026-01-12', '2026-03-13', 'vitaedu-VE105', 'DONE', '2026-03-13 17:30:00', 'vitaedu-VE105'),
(8022, 8009, 8002, '1기 수료 판정',                  4,
 '2026-03-16', '2026-03-27', 'vitaedu-VE104', 'DONE', '2026-03-27 16:20:00', 'vitaedu-VE104'),
(8023, 8010, 8002, '2기 학습 진도·독려',             5,
 '2026-06-01', '2026-09-11', 'vitaedu-VE105', 'IN_PROGRESS', NULL, NULL),
(8024, 8010, 8002, '2기 중도이탈 관리',              6,
 '2026-06-15', '2026-09-11', 'vitaedu-VE104', 'IN_PROGRESS', NULL, NULL),
-- ⭐ 회차마다 스텝을 만들지 않는다. settlement_block 의 UNIQUE 는 block_id 하나뿐이다.
(8025, 8011, 8002, '훈련비 청구·수납 (4회차)',       7,
 '2026-04-01', '2026-11-25', 'vitaedu-VE109', 'IN_PROGRESS', NULL, NULL),
(8026, 8012, 8002, '만족도·수료율 점검',             8,
 '2026-11-02', '2026-11-27', 'vitaedu-VE104', 'NOT_STARTED', NULL, NULL);


-- ── 7. 목록용 8건의 스텝 24 ──────────────────────────────────────────
-- ⚠️ stage_id 를 NULL 로 둔 건 의도다 — step.stage_id 는 NULL 허용이 정상 케이스이고(STP-001),
--    스테이지 없는 스텝이 목록에서 어떻게 보이는지도 같이 확인된다.
INSERT IGNORE INTO step
  (step_id, stage_id, project_id, name, sort_order, started_on, ended_on,
   owner_user_id, status, completed_at, completed_by) VALUES
-- 8003 2025년 심사 신청 (완료)
(8027, NULL, 8003, '신청 요건 검토',        1, '2025-08-01', '2025-08-08', 'vitaedu-VE101', 'DONE', '2025-08-08 17:00:00', 'vitaedu-VE101'),
(8028, NULL, 8003, '운영계획서 작성·제출',  2, '2025-08-08', '2025-08-26', 'vitaedu-VE101', 'DONE', '2025-08-26 17:40:00', 'vitaedu-VE101'),
(8029, NULL, 8003, '심사 대응·결과 확인',   3, '2025-09-08', '2025-11-28', 'vitaedu-VE103', 'DONE', '2025-11-28 15:00:00', 'vitaedu-VE103'),
-- 8004 KDT 심사 신청 (종결)
(8030, NULL, 8004, '과정 설계',             1, '2026-02-02', '2026-03-06', 'vitaedu-VE106', 'DONE', '2026-03-06 18:00:00', 'vitaedu-VE106'),
(8031, NULL, 8004, '신청서류 제출',         2, '2026-03-09', '2026-03-27', 'vitaedu-VE103', 'DONE', '2026-03-27 17:00:00', 'vitaedu-VE103'),
(8032, NULL, 8004, '심사 결과 확인',        3, '2026-04-13', '2026-05-20', 'vitaedu-VE103', 'DONE', '2026-05-20 16:40:00', 'vitaedu-VE103'),
-- 8005 내일배움카드 일반과정 (완료)
(8033, NULL, 8005, '대상 과정 선정',        1, '2025-09-15', '2025-09-26', 'vitaedu-VE104', 'DONE', '2025-09-26 17:00:00', 'vitaedu-VE104'),
(8034, NULL, 8005, '인정 신청서 작성',      2, '2025-09-29', '2025-10-24', 'vitaedu-VE102', 'DONE', '2025-10-24 18:00:00', 'vitaedu-VE102'),
(8035, NULL, 8005, '보완 대응·인정 확인',   3, '2025-11-10', '2026-01-30', 'vitaedu-VE104', 'DONE', '2026-01-30 15:20:00', 'vitaedu-VE104'),
-- 8006 지자체 위탁 용역 (진행)
(8036, NULL, 8006, '공고 요건 분석',        1, '2026-08-11', '2026-08-14', 'vitaedu-VE101', 'DONE', '2026-08-14 17:00:00', 'vitaedu-VE101'),
(8037, NULL, 8006, '제안서 작성',           2, '2026-08-17', '2026-08-19', 'vitaedu-VE103', 'IN_PROGRESS', NULL, NULL),
(8038, NULL, 8006, '제출·평가 대응',        3, '2026-08-20', '2026-08-21', 'vitaedu-VE101', 'NOT_STARTED', NULL, NULL),
-- 8007 재직자 위탁교육 (완료)
(8039, NULL, 8007, '교육 과정 설계',        1, '2025-03-03', '2025-03-28', 'vitaedu-VE104', 'DONE', '2025-03-28 17:00:00', 'vitaedu-VE104'),
(8040, NULL, 8007, '교육 운영',             2, '2025-04-07', '2025-11-28', 'vitaedu-VE105', 'DONE', '2025-11-28 18:00:00', 'vitaedu-VE105'),
(8041, NULL, 8007, '정산·결과 보고',        3, '2025-12-01', '2025-12-19', 'vitaedu-VE109', 'DONE', '2025-12-19 16:00:00', 'vitaedu-VE109'),
-- 8008 LMS 고도화 (진행)
(8042, NULL, 8008, '요구사항 정리',         1, '2026-05-11', '2026-05-29', 'vitaedu-VE108', 'DONE', '2026-05-29 18:00:00', 'vitaedu-VE108'),
(8043, NULL, 8008, '개발 외주 관리',        2, '2026-06-01', '2026-09-11', 'vitaedu-VE108', 'IN_PROGRESS', NULL, NULL),
(8044, NULL, 8008, '검수·이관',             3, '2026-09-14', '2026-09-30', 'vitaedu-VE108', 'NOT_STARTED', NULL, NULL),
-- 8009 인증평가 대응 (미시작)
(8045, NULL, 8009, '평가 지표 분석',        1, '2026-10-05', '2026-10-30', 'vitaedu-VE103', 'NOT_STARTED', NULL, NULL),
(8046, NULL, 8009, '증빙 정비',             2, '2026-11-02', '2026-12-24', 'vitaedu-VE108', 'NOT_STARTED', NULL, NULL),
(8047, NULL, 8009, '현장평가 대응',         3, '2027-01-04', '2027-02-26', 'vitaedu-VE103', 'NOT_STARTED', NULL, NULL),
-- 8010 콘텐츠 변경심사 (진행)
(8048, NULL, 8010, '변경 대상 차시 선별',   1, '2026-06-01', '2026-06-19', 'vitaedu-VE106', 'DONE', '2026-06-19 17:00:00', 'vitaedu-VE106'),
(8049, NULL, 8010, '콘텐츠 재제작',         2, '2026-06-22', '2026-09-04', 'vitaedu-VE107', 'IN_PROGRESS', NULL, NULL),
(8050, NULL, 8010, '변경심사 신청',         3, '2026-09-07', '2026-09-30', 'vitaedu-VE106', 'NOT_STARTED', NULL, NULL);


-- ── 8. 스텝 권한 오버라이드 4 — 차단·강등·승격 ───────────────────────
-- ⚠️ 행이 없으면 프로젝트 권한을 그대로 쓴다 (STP-011). 아래 4건만 예외다.
INSERT IGNORE INTO step_permission (step_permission_id, step_id, user_id, permission) VALUES
-- 차단: 외주 계약 단가와 업체 견적이 들어 있다
(8001, 8013, 'vitaedu-VE102', 'NONE'),
(8002, 8013, 'vitaedu-VE105', 'NONE'),
-- 강등: 훈련운영계획서 스텝에 인건비 산출 근거가 있다 (프로젝트는 EDITOR)
(8003, 8009, 'vitaedu-VE107', 'VIEWER'),
-- 승격: 하성민은 P8001 에서 VIEWER 인데 정산 스텝만 EDITOR
(8004, 8014, 'vitaedu-VE109', 'EDITOR');


-- =====================================================================
-- 검증
-- =====================================================================
-- 1) P8001 진척률 — 완료 8 / 15 유효 스텝? 아니다. 전체 18 중 DONE 8 → 44%
--    SELECT status, COUNT(*) FROM step WHERE project_id = 8001 AND deleted_at IS NULL GROUP BY status;
--    기대: DONE 8 · IN_PROGRESS 4 · NOT_STARTED 6
--
-- 2) sort_order 가 프로젝트 통번호인가 (0행이어야 정상)
--    SELECT project_id FROM step WHERE project_id IN (8001, 8002)
--    GROUP BY project_id HAVING COUNT(*) <> MAX(sort_order) OR MIN(sort_order) <> 1;
--
-- 3) ⛔ ADMIN 이 프로젝트 멤버에 섞이지 않았나 (0행이어야 정상)
--    SELECT * FROM project_member WHERE user_id IN ('vitaedu-VE112', 'vitaedu-VE113');
--
-- 4) 공고 → 프로젝트 연결
--    SELECT p.project_id, p.name, n.notice_name FROM project p
--    JOIN bid_notice n ON n.bid_notice_id = p.bid_notice_id WHERE p.company_id = 3;
