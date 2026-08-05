-- =====================================================================
-- 프로젝트 계층 샘플 데이터 — business_category · project · stage · step · 권한
-- ---------------------------------------------------------------------
-- 목적: 멤버-카테고리-프로젝트-스테이지-스텝-권한 체계를 실데이터로 검증/데모.
-- 참조 employee: EMP001~EMP004 (V202608031500 은 스키마만 생성 · 사원 더미는
--   .ai/local 수동 seed 로 별도 관리되므로 여기서는 "이미 존재한다"고 가정한다).
--   ⚠️ 이 마이그레이션을 employee/account 더미가 없는 환경에 적용하면 FK 위반으로 실패한다.
-- 재적용 안전: project/stage/step/project_member/step_permission 은 명시적 PK + INSERT IGNORE
--   (§3 · 자연키 UNIQUE 없음 · 두 환경(dev RDS·localtest) 모두 0건 확인 후 1부터 채번).
--   business_category 는 환경마다 이미 다른 더미가 들어있을 수 있어(§3) PK를 고정하지 않고
--   UNIQUE(name) 기준으로만 멱등 처리 — project_business_category 는 이름으로 역조회한다.
-- =====================================================================

-- 사업 카테고리 4종 (PK 미지정 · UNIQUE(name) 로 재적용 안전 확보)
INSERT IGNORE INTO business_category (name, code, description) VALUES
  ('IT 용역',    'IT_SERVICE',          '정보시스템 구축·유지보수 관련 사업'),
  ('재무/회계',   'FINANCE_ACCOUNTING',  '재무·회계·정산 관련 사업'),
  ('AI·데이터',   'AI_DATA',             'AI/데이터 분석 관련 사업'),
  ('시설/총무',   'FACILITY_GA',         '시설·총무·구매 관련 사업');

-- 프로젝트 5건 — status 5종(NOT_STARTED/IN_PROGRESS/SETTLEMENT/COMPLETED/CLOSED) 전부 커버
INSERT IGNORE INTO project
  (project_id, name, description, status, client_name, contract_amount,
   started_on, ended_on, closed_at, close_reason_code, close_reason_note, created_by) VALUES
  (1, '공공기관 통합 그룹웨어 구축', '조달청 발주 그룹웨어 구축 사업', 'IN_PROGRESS',
      '조달청', 850000000.00, '2026-03-01', '2026-12-31', NULL, NULL, NULL, 'EMP001'),
  (2, '사내 전자결재 시스템 고도화', '내부 전자결재 워크플로우 개선', 'NOT_STARTED',
      NULL, NULL, '2026-09-01', '2027-01-31', NULL, NULL, NULL, 'EMP003'),
  (3, '재무회계 통합 프로젝트', '재무·회계 시스템 통합 및 정산', 'SETTLEMENT',
      '한국재무정보원', 320000000.00, '2025-09-01', '2026-06-30', NULL, NULL, NULL, 'EMP001'),
  (4, 'AI 챗봇 도입 사업', '사내 업무지원 AI 챗봇 도입', 'COMPLETED',
      '(주)비타테크', 150000000.00, '2025-01-10', '2025-07-31', NULL, NULL, NULL, 'EMP002'),
  (5, '구 사옥 리모델링 입찰', '구 사옥 리모델링 공사 입찰 참여', 'CLOSED',
      '서울특별시', NULL, NULL, NULL, '2026-02-15', 'NOT_SELECTED', '타사 낙찰로 참여 종료', 'EMP004');

-- 프로젝트-카테고리 연결 (business_category_id 를 이름으로 역조회 · PK 고정 안 함)
INSERT IGNORE INTO project_business_category (project_id, business_category_id)
SELECT 1, business_category_id FROM business_category WHERE name = 'IT 용역'
UNION ALL
SELECT 2, business_category_id FROM business_category WHERE name = 'IT 용역'
UNION ALL
SELECT 3, business_category_id FROM business_category WHERE name = '재무/회계'
UNION ALL
SELECT 4, business_category_id FROM business_category WHERE name = 'AI·데이터'
UNION ALL
SELECT 5, business_category_id FROM business_category WHERE name = '시설/총무';

-- 프로젝트 참여자 권한 — VIEWER/EDITOR/NONE 을 섞어 STP-010(목록 필터링) 데모 가능하게 구성
-- EMP002(MASTER) 는 의도적으로 project_member 에서 제외 — role 이 항상 EDITOR 로 해석되는 것을 확인하는 케이스
INSERT IGNORE INTO project_member (project_member_id, project_id, user_id, permission) VALUES
  (1,  1, 'EMP001', 'EDITOR'),
  (2,  1, 'EMP003', 'VIEWER'),
  (3,  1, 'EMP004', 'NONE'),
  (4,  2, 'EMP003', 'EDITOR'),
  (5,  2, 'EMP001', 'VIEWER'),
  (6,  3, 'EMP001', 'EDITOR'),
  (7,  3, 'EMP004', 'EDITOR'),
  (8,  3, 'EMP003', 'NONE'),
  (9,  4, 'EMP001', 'VIEWER'),
  (10, 4, 'EMP004', 'VIEWER'),
  (11, 5, 'EMP004', 'EDITOR'),
  (12, 5, 'EMP001', 'VIEWER');

-- 스테이지 (프로젝트별 sort_order 는 프로젝트 내부 기준)
INSERT IGNORE INTO stage (stage_id, project_id, name, sort_order) VALUES
  (1,  1, '요구분석', 1),
  (2,  1, '설계',     2),
  (3,  1, '개발',     3),
  (4,  1, '테스트',   4),
  (5,  2, '기획',     1),
  (6,  2, '설계',     2),
  (7,  3, '구축',     1),
  (8,  3, '정산',     2),
  (9,  4, '분석',     1),
  (10, 4, '구축',     2),
  (11, 4, '검수',     3),
  (12, 5, '입찰준비', 1);

-- 스텝 (sort_order 는 스테이지가 아니라 프로젝트 전체 기준 · #66 확정 규칙)
INSERT IGNORE INTO step
  (step_id, stage_id, project_id, name, sort_order, started_on, ended_on,
   owner_user_id, status, completed_at, completed_by) VALUES
  -- 프로젝트 1 (IN_PROGRESS)
  (1,  1,    1, '요구사항 정의서 작성', 1, '2026-03-01', '2026-03-15',
      'EMP001', 'DONE', '2026-03-14 17:00:00', 'EMP001'),
  (2,  2,    1, '시스템 아키텍처 설계', 2, '2026-03-16', '2026-04-10',
      'EMP003', 'IN_PROGRESS', NULL, NULL),
  (3,  3,    1, '백엔드 API 개발',     3, '2026-04-11', '2026-07-31',
      'EMP001', 'IN_PROGRESS', NULL, NULL),
  (4,  4,    1, '통합 테스트',         4, '2026-08-01', '2026-09-30',
      NULL, 'NOT_STARTED', NULL, NULL),
  (5,  NULL, 1, '킥오프 미팅 준비',    5, '2026-02-20', '2026-02-27',
      'EMP001', 'DONE', '2026-02-26 11:00:00', 'EMP001'),
  -- 프로젝트 2 (NOT_STARTED)
  (6,  5, 2, '요구사항 수집', 1, NULL, NULL, 'EMP003', 'NOT_STARTED', NULL, NULL),
  (7,  6, 2, '화면 설계',     2, NULL, NULL, NULL,      'NOT_STARTED', NULL, NULL),
  -- 프로젝트 3 (SETTLEMENT)
  (8,  7, 3, '회계 시스템 구축',   1, '2025-09-01', '2026-02-28',
      'EMP001', 'DONE', '2026-02-27 18:00:00', 'EMP001'),
  (9,  8, 3, '정산 데이터 검증',   2, '2026-03-01', '2026-06-30',
      'EMP004', 'IN_PROGRESS', NULL, NULL),
  -- 프로젝트 4 (COMPLETED)
  (10, 9,  4, '요구사항 분석',   1, '2025-01-10', '2025-02-10',
      'EMP002', 'DONE', '2025-02-09 16:00:00', 'EMP002'),
  (11, 10, 4, '챗봇 엔진 구축',   2, '2025-02-11', '2025-06-15',
      'EMP001', 'DONE', '2025-06-14 15:00:00', 'EMP001'),
  (12, 11, 4, '최종 검수',       3, '2025-06-16', '2025-07-31',
      'EMP004', 'DONE', '2025-07-30 10:00:00', 'EMP004'),
  -- 프로젝트 5 (CLOSED)
  (13, 12, 5, '제안서 작성',     1, '2026-01-10', '2026-01-25',
      'EMP004', 'DONE', '2026-01-24 14:00:00', 'EMP004'),
  (14, 12, 5, '입찰 참가 신청',  2, '2026-01-26', '2026-02-05',
      'EMP001', 'DONE', '2026-02-04 09:00:00', 'EMP001');

-- 스텝 권한 오버라이드 — 상속과 다르게 동작하는 케이스 2건
-- ① 그랜트: 프로젝트에서 NONE(EMP004, project_member#3)인데 이 스텝만 VIEWER 로 허용
-- ② 차단: 프로젝트에서 EDITOR(EMP001, project_member#6)인데 정산 스텝만 NONE 으로 차단
INSERT IGNORE INTO step_permission (step_permission_id, step_id, user_id, permission) VALUES
  (1, 3, 'EMP004', 'VIEWER'),
  (2, 9, 'EMP001', 'NONE');
