-- =====================================================================
-- 프로젝트 계층 시드 재적용 — project · stage · step · block · 권한 · 이슈
-- ---------------------------------------------------------------------
-- 무엇: V20260805150000(프로젝트 샘플) · V20260805160000(블록 샘플) ·
--       V20260810090100(권한 테스트셋) 이 넣지 못한 행을 현재 스키마 형식으로 다시 넣는다.
-- 왜:   세 시드 모두 employee FK 가 걸린 테이블에서 전건 스킵됐다. 원본은 이미 적용된
--       파일이라 수정할 수 없으므로(FLYWAY.md §4) 새 파일로 대체 투입한다.
--
-- 원본이 깨진 이유 3가지 — 그대로 복사하면 안 되는 지점이다:
--   ① V20260809110000 이 사번을 'vitas-' 접두사로 바꿨다. 원본은 'EMP001' 로 INSERT →
--      FK 불일치 → INSERT IGNORE 라 에러 없이 전건 스킵. (STATE 등록 버그와 동일 건)
--   ② V20260811100000/100100 이 project·business_category 에 company_id NOT NULL 을 넣고
--      V20260811110000 이 임시 DEFAULT 1 을 뗐다. 원본 컬럼 목록에는 company_id 가 없다.
--   ③ V20260806120000/130000 이 폐기 요구사항 2건(그랜트 · 참여자 NONE)의 행을 지웠다.
--      되살리면 안 되므로 여기서도 제외한다 (아래 §4 · §7 주석 참고).
--
-- 제외한 것 — employee FK 가 없어 원본 시드가 이미 성공한 테이블:
--   business_category(9) · project_business_category 대상 · text(7) · checklist_block(7) · checklist(25)
--   → 재투입하면 UNIQUE 로 스킵될 뿐이라 넣지 않는다. block 1~14 가 들어가면 이 상세 행들이
--      가리키던 고아 상태가 자동으로 해소된다 (text.block_id · checklist_block.block_id 는 FK 가 없다).
--
-- 재적용 안전: 전부 명시적 PK + INSERT IGNORE. dev RDS 는 접두사 전환 **이전**에 원본 시드가
--   들어갔을 수 있어(그 환경은 이미 vitas- 로 리네임됨) 중복이면 조용히 건너뛴다.
--
-- ⚠️ 단일 회사(company_id=1 · company_code='vitas') 전제다. V20260809110000 과 같은 전제이며,
--    회사가 2개 이상이 된 뒤에는 이 파일을 참고 시드로 복사하지 마라.
--
-- ID 대역 (원본과 동일 · 서로 안 겹친다)
--   샘플   : project 1~5    · stage 1~12   · step 1~14   · block 1~14 · issue 1~4
--   테스트셋: project 101~105 · stage 201~210 · step 301~320 · 권한 401~ · 501~ · 601~
-- =====================================================================


-- =====================================================================
-- A. 08-05 프로젝트 샘플 (V20260805150000 재투입)
-- =====================================================================

-- A-1. 프로젝트 5건 — status 5종 전부 커버 · company_id 명시(DEFAULT 제거됨)
INSERT IGNORE INTO project
  (project_id, company_id, name, description, status, client_name, contract_amount,
   started_on, ended_on, closed_at, close_reason_code, close_reason_note, created_by) VALUES
  (1, 1, '공공기관 통합 그룹웨어 구축', '조달청 발주 그룹웨어 구축 사업', 'IN_PROGRESS',
      '조달청', 850000000.00, '2026-03-01', '2026-12-31', NULL, NULL, NULL, 'vitas-EMP001'),
  (2, 1, '사내 전자결재 시스템 고도화', '내부 전자결재 워크플로우 개선', 'NOT_STARTED',
      NULL, NULL, '2026-09-01', '2027-01-31', NULL, NULL, NULL, 'vitas-EMP003'),
  (3, 1, '재무회계 통합 프로젝트', '재무·회계 시스템 통합 및 정산', 'SETTLEMENT',
      '한국재무정보원', 320000000.00, '2025-09-01', '2026-06-30', NULL, NULL, NULL, 'vitas-EMP001'),
  (4, 1, 'AI 챗봇 도입 사업', '사내 업무지원 AI 챗봇 도입', 'COMPLETED',
      '(주)비타테크', 150000000.00, '2025-01-10', '2025-07-31', NULL, NULL, NULL, 'vitas-EMP002'),
  (5, 1, '구 사옥 리모델링 입찰', '구 사옥 리모델링 공사 입찰 참여', 'CLOSED',
      '서울특별시', NULL, NULL, NULL, '2026-02-15', 'NOT_SELECTED', '타사 낙찰로 참여 종료', 'vitas-EMP004');

-- A-2. 프로젝트-카테고리 연결 — business_category_id 를 이름으로 역조회한다.
--      UNIQUE 가 (company_id, name) 로 바뀌었으므로 회사 조건을 함께 건다.
INSERT IGNORE INTO project_business_category (project_id, business_category_id)
SELECT 1, business_category_id FROM business_category WHERE company_id = 1 AND name = 'IT 용역'
UNION ALL
SELECT 2, business_category_id FROM business_category WHERE company_id = 1 AND name = 'IT 용역'
UNION ALL
SELECT 3, business_category_id FROM business_category WHERE company_id = 1 AND name = '재무/회계'
UNION ALL
SELECT 4, business_category_id FROM business_category WHERE company_id = 1 AND name = 'AI·데이터'
UNION ALL
SELECT 5, business_category_id FROM business_category WHERE company_id = 1 AND name = '시설/총무';

-- A-3. 프로젝트 참여자 10건
--   ⚠️ 원본 12건 중 permission='NONE' 2건(id 3 · 8)은 넣지 않는다.
--      V20260806130000 이 PRJ-010 `NONE` 을 폐기하고 같은 행을 지웠다 — 차단은 "행 없음"으로 표현한다.
--      원본 ID 를 그대로 유지하므로 3 · 8 은 비어 있는 게 정상이다.
--   EMP002(MASTER)는 의도적으로 제외 — role 이 항상 EDITOR 로 해석되는지 확인하는 케이스.
INSERT IGNORE INTO project_member (project_member_id, project_id, user_id, permission) VALUES
  (1,  1, 'vitas-EMP001', 'EDITOR'),
  (2,  1, 'vitas-EMP003', 'VIEWER'),
  (4,  2, 'vitas-EMP003', 'EDITOR'),
  (5,  2, 'vitas-EMP001', 'VIEWER'),
  (6,  3, 'vitas-EMP001', 'EDITOR'),
  (7,  3, 'vitas-EMP004', 'EDITOR'),
  (9,  4, 'vitas-EMP001', 'VIEWER'),
  (10, 4, 'vitas-EMP004', 'VIEWER'),
  (11, 5, 'vitas-EMP004', 'EDITOR'),
  (12, 5, 'vitas-EMP001', 'VIEWER');

-- A-4. 스테이지 (sort_order 는 프로젝트 내부 기준)
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

-- A-5. 스텝 (sort_order 는 스테이지가 아니라 프로젝트 전체 기준 · #66 확정 규칙)
INSERT IGNORE INTO step
  (step_id, stage_id, project_id, name, sort_order, started_on, ended_on,
   owner_user_id, status, completed_at, completed_by) VALUES
  -- 프로젝트 1 (IN_PROGRESS)
  (1,  1,    1, '요구사항 정의서 작성', 1, '2026-03-01', '2026-03-15',
      'vitas-EMP001', 'DONE', '2026-03-14 17:00:00', 'vitas-EMP001'),
  (2,  2,    1, '시스템 아키텍처 설계', 2, '2026-03-16', '2026-04-10',
      'vitas-EMP003', 'IN_PROGRESS', NULL, NULL),
  (3,  3,    1, '백엔드 API 개발',     3, '2026-04-11', '2026-07-31',
      'vitas-EMP001', 'IN_PROGRESS', NULL, NULL),
  (4,  4,    1, '통합 테스트',         4, '2026-08-01', '2026-09-30',
      NULL, 'NOT_STARTED', NULL, NULL),
  (5,  NULL, 1, '킥오프 미팅 준비',    5, '2026-02-20', '2026-02-27',
      'vitas-EMP001', 'DONE', '2026-02-26 11:00:00', 'vitas-EMP001'),
  -- 프로젝트 2 (NOT_STARTED)
  (6,  5, 2, '요구사항 수집', 1, NULL, NULL, 'vitas-EMP003', 'NOT_STARTED', NULL, NULL),
  (7,  6, 2, '화면 설계',     2, NULL, NULL, NULL,           'NOT_STARTED', NULL, NULL),
  -- 프로젝트 3 (SETTLEMENT)
  (8,  7, 3, '회계 시스템 구축',   1, '2025-09-01', '2026-02-28',
      'vitas-EMP001', 'DONE', '2026-02-27 18:00:00', 'vitas-EMP001'),
  (9,  8, 3, '정산 데이터 검증',   2, '2026-03-01', '2026-06-30',
      'vitas-EMP004', 'IN_PROGRESS', NULL, NULL),
  -- 프로젝트 4 (COMPLETED)
  (10, 9,  4, '요구사항 분석',   1, '2025-01-10', '2025-02-10',
      'vitas-EMP002', 'DONE', '2025-02-09 16:00:00', 'vitas-EMP002'),
  (11, 10, 4, '챗봇 엔진 구축',   2, '2025-02-11', '2025-06-15',
      'vitas-EMP001', 'DONE', '2025-06-14 15:00:00', 'vitas-EMP001'),
  (12, 11, 4, '최종 검수',       3, '2025-06-16', '2025-07-31',
      'vitas-EMP004', 'DONE', '2025-07-30 10:00:00', 'vitas-EMP004'),
  -- 프로젝트 5 (CLOSED)
  (13, 12, 5, '제안서 작성',     1, '2026-01-10', '2026-01-25',
      'vitas-EMP004', 'DONE', '2026-01-24 14:00:00', 'vitas-EMP004'),
  (14, 12, 5, '입찰 참가 신청',  2, '2026-01-26', '2026-02-05',
      'vitas-EMP001', 'DONE', '2026-02-04 09:00:00', 'vitas-EMP001');

-- A-6. 스텝 권한 오버라이드 1건 (차단)
--   ⚠️ 원본 2건 중 id 1(프로젝트 NONE + 스텝 VIEWER 그랜트)은 넣지 않는다.
--      V20260806120000 이 STP-010 그랜트를 폐기하고 같은 행을 지웠다.
--      남는 것은 "프로젝트 EDITOR → 이 스텝만 NONE" 차단 케이스뿐이다.
INSERT IGNORE INTO step_permission (step_permission_id, step_id, user_id, permission) VALUES
  (2, 9, 'vitas-EMP001', 'NONE');


-- =====================================================================
-- B. 08-05 블록 샘플 (V20260805160000 재투입)
--   상세 행(text 1~7 · checklist_block 1~7 · checklist 1~25)은 이미 들어가 있다 —
--   FK 가 없어 원본 시드가 성공했다. 아래 block 이 들어가면서 참조가 맞춰진다.
--   ⚠️ 그래서 type_id 값은 원본과 반드시 동일해야 한다. 바꾸면 기존 상세 행과 어긋난다.
-- =====================================================================

INSERT IGNORE INTO block
    (block_id, step_id, title, type, type_id, owner, row_index, col_span, sort_order, created_by) VALUES
    (1,  1,  '요구사항 정의서',           'TEXT',      1, 'vitas-EMP001', 0, 1, 0, 'vitas-EMP001'),
    (2,  1,  '정의서 작성 체크리스트',     'CHECKLIST', 1, 'vitas-EMP001', 0, 1, 1, 'vitas-EMP001'),
    (3,  2,  '아키텍처 설계 노트',         'TEXT',      2, 'vitas-EMP003', 0, 1, 0, 'vitas-EMP003'),
    (4,  2,  '설계 검토 체크리스트',       'CHECKLIST', 2, 'vitas-EMP003', 0, 1, 1, 'vitas-EMP003'),
    (5,  3,  'API 설계 메모',             'TEXT',      3, 'vitas-EMP001', 0, 1, 0, 'vitas-EMP001'),
    (6,  3,  '개발 체크리스트',           'CHECKLIST', 3, 'vitas-EMP001', 0, 1, 1, 'vitas-EMP001'),
    (7,  6,  '요구사항 수집 계획',         'TEXT',      4, 'vitas-EMP003', 0, 1, 0, 'vitas-EMP003'),
    (8,  6,  '수집 체크리스트',           'CHECKLIST', 4, 'vitas-EMP003', 0, 1, 1, 'vitas-EMP003'),
    (9,  8,  '구축 결과 요약',            'TEXT',      5, 'vitas-EMP001', 0, 1, 0, 'vitas-EMP001'),
    (10, 8,  '구축 체크리스트',           'CHECKLIST', 5, 'vitas-EMP001', 0, 1, 1, 'vitas-EMP001'),
    (11, 9,  '정산 검증 체크리스트',       'CHECKLIST', 6, 'vitas-EMP004', 0, 1, 0, 'vitas-EMP004'),
    (12, 10, 'AI 챗봇 요구사항 분석 결과', 'TEXT',      6, 'vitas-EMP002', 0, 1, 0, 'vitas-EMP002'),
    (13, 13, '제안서 초안',               'TEXT',      7, 'vitas-EMP004', 0, 1, 0, 'vitas-EMP004'),
    (14, 13, '제안서 체크리스트',          'CHECKLIST', 7, 'vitas-EMP004', 0, 2, 1, 'vitas-EMP004');

-- 이슈 4건 — 블록-이슈 집계(B5, BlockIssueStatLookupPort) 검증용
-- step_id 는 issue_block 으로 연결할 블록의 step 과 반드시 같아야 한다 (BLOCK.md §7 같은 스텝 제약)
INSERT IGNORE INTO issue
    (issue_id, title, content, status, step_id, priority, created_by) VALUES
    (1, '아키텍처 다이어그램 작성', '논리 아키텍처 다이어그램을 작성하고 리뷰를 받는다.', 'DONE',        2, 'MEDIUM', 'vitas-EMP003'),
    (2, '설계 리뷰 진행',          '팀 내 설계 리뷰를 진행하고 피드백을 반영한다.',     'IN_PROGRESS', 2, 'HIGH',   'vitas-EMP003'),
    (3, '정산 승인 요청',          '1차 정산 데이터에 대한 재무팀 승인을 요청한다.',    'DONE',        9, 'HIGH',   'vitas-EMP004'),
    (4, '정산 데이터 오류 확인',    '정산 데이터 중 금액 불일치 항목을 확인한다.',       'TO_DO',       9, 'MEDIUM', 'vitas-EMP004');

-- 블록-이슈 연결 — block 3(step2 TEXT) 2건, block 11(step9 CHECKLIST) 2건
INSERT IGNORE INTO issue_block (issue_block_id, issue_id, block_id) VALUES
    (1, 1, 3),
    (2, 2, 3),
    (3, 3, 11),
    (4, 4, 11);


-- =====================================================================
-- C. 08-10 권한 판정 테스트셋 (V20260810090100 재투입 · EMP001 단독)
--   상속 / 승격 / 강등 / 차단 / 무시(미참여) / 스테이지 기본값 6종을 한 계정으로 확인한다.
--   카테고리 5종과 '폐지-구 분류' 의 deleted_at 은 원본이 이미 넣었다(FK 없음) — 여기서 제외.
-- =====================================================================

-- C-1. 프로젝트 5건 — 104 만 의도적으로 참여자에서 뺀다(C-3 참고)
INSERT IGNORE INTO project
  (project_id, company_id, bid_notice_id, name, description, status, client_name, contract_amount,
   started_on, ended_on, closed_at, close_reason_code, close_reason_note, created_by) VALUES
  (101, 1, NULL, '공공 클라우드 전환 사업', '온프레미스 시스템의 클라우드 전환', 'IN_PROGRESS',
       '조달청', 1200000000.00, '2026-04-01', '2026-12-31', NULL, NULL, NULL, 'vitas-EMP001'),
  (102, 1, NULL, '스마트팩토리 MES 고도화', 'MES 인터페이스 개선 및 기성 정산', 'SETTLEMENT',
       '(주)한빛제조', 480000000.00, '2025-10-01', '2026-08-31', NULL, NULL, NULL, 'vitas-EMP001'),
  (103, 1, NULL, '사내 보안 컨설팅', '정보보호 취약점 진단 및 개선 권고', 'COMPLETED',
       '내부', 90000000.00, '2025-11-01', '2026-03-31', NULL, NULL, NULL, 'vitas-EMP001'),
  (104, 1, NULL, '지하철 역사 리모델링 입찰', '2호선 역사 리모델링 공사 입찰 참여', 'CLOSED',
       '서울교통공사', NULL, NULL, NULL, '2026-05-20 14:00:00', 'FAILED_BID', '예정가격 미달로 유찰', 'vitas-EMP001'),
  (105, 1, NULL, '신규 사업 기획', '스텝·블록이 아직 하나도 없는 신규 프로젝트', 'NOT_STARTED',
       NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'vitas-EMP001');

-- C-2. 프로젝트 ↔ 카테고리 (PRJ-007 복수 선택 · BCT-009 삭제 카테고리 연결 유지)
--   105 는 일부러 연결하지 않는다 — 카테고리 미지정 프로젝트의 필터 동작 확인용.
INSERT IGNORE INTO project_business_category (project_id, business_category_id)
SELECT 101, business_category_id FROM business_category WHERE company_id = 1 AND name = '클라우드/인프라'
UNION ALL
SELECT 101, business_category_id FROM business_category WHERE company_id = 1 AND name = '폐지-구 분류'
UNION ALL
SELECT 102, business_category_id FROM business_category WHERE company_id = 1 AND name = '클라우드/인프라'
UNION ALL
SELECT 102, business_category_id FROM business_category WHERE company_id = 1 AND name = '교육/훈련'
UNION ALL
SELECT 103, business_category_id FROM business_category WHERE company_id = 1 AND name = '보안/컨설팅'
UNION ALL
SELECT 104, business_category_id FROM business_category WHERE company_id = 1 AND name = '건설/시설공사';

-- C-3. 프로젝트 참여자 — EMP001 한 명 (VIEWER/EDITOR 2값만)
--   ⚠️ 104 에는 행이 없다. 생성자(created_by)여도 참여자가 아니면 접근은 차단이다 —
--      created_by 로 권한을 유추하는 코드가 있으면 여기서 403 대신 200 이 나온다.
INSERT IGNORE INTO project_member (project_member_id, project_id, user_id, permission) VALUES
  (401, 101, 'vitas-EMP001', 'EDITOR'),
  (402, 102, 'vitas-EMP001', 'VIEWER'),
  (403, 103, 'vitas-EMP001', 'EDITOR'),
  (404, 105, 'vitas-EMP001', 'EDITOR');

-- C-4. 스테이지 — 101 은 5단(순서 변경용), 나머지는 2단, 105 는 없음
INSERT IGNORE INTO stage (stage_id, project_id, name, sort_order) VALUES
  (201, 101, '착수',     1),
  (202, 101, '설계',     2),
  (203, 101, '구축',     3),
  (204, 101, '이행',     4),
  (205, 101, '안정화',   5),
  (206, 102, '구축',     1),
  (207, 102, '정산',     2),
  (208, 103, '진단',     1),
  (209, 103, '보고',     2),
  (210, 104, '입찰준비', 1);

-- C-5. 스텝 20건 — 프로젝트당 5건 (105 는 0건)
--   305 · 310 은 stage_id 가 NULL 이다 (STP-001 — 스테이지 미배정 스텝).
--   304 는 날짜가 없다 (PRJ-006 — 캘린더 응답에서 빠지는지 확인).
INSERT IGNORE INTO step
  (step_id, stage_id, project_id, name, sort_order, started_on, ended_on,
   owner_user_id, status, completed_at, completed_by) VALUES
  -- 프로젝트 101 (IN_PROGRESS · EMP001 EDITOR) → 진척률 1/5 = 20%
  (301, 201,  101, '전환 대상 자산 조사',    1, '2026-04-01', '2026-04-20',
       'vitas-EMP001', 'DONE',        '2026-04-18 17:30:00', 'vitas-EMP001'),
  (302, 202,  101, '클라우드 아키텍처 설계', 2, '2026-04-21', '2026-05-31',
       'vitas-EMP001', 'IN_PROGRESS', NULL, NULL),
  (303, 203,  101, '인프라 구축',           3, '2026-06-01', '2026-09-30',
       NULL,           'NOT_STARTED', NULL, NULL),
  (304, 204,  101, '데이터 이행',           4, NULL,         NULL,
       'vitas-EMP001', 'NOT_STARTED', NULL, NULL),
  (305, NULL, 101, '주간 보고 정리',        5, '2026-04-01', '2026-12-31',
       'vitas-EMP001', 'IN_PROGRESS', NULL, NULL),
  -- 프로젝트 102 (SETTLEMENT · EMP001 VIEWER) → 진척률 2/5 = 40%
  (306, 206,  102, 'MES 인터페이스 개발',   1, '2025-10-01', '2026-02-28',
       'vitas-EMP001', 'DONE',        '2026-02-26 18:00:00', 'vitas-EMP001'),
  (307, 206,  102, '현장 검증',             2, '2026-03-01', '2026-04-30',
       'vitas-EMP001', 'DONE',        '2026-04-28 15:00:00', 'vitas-EMP001'),
  (308, 207,  102, '기성 정산 자료 작성',    3, '2026-05-01', '2026-07-31',
       'vitas-EMP001', 'IN_PROGRESS', NULL, NULL),
  (309, 207,  102, '최종 정산 검토',        4, '2026-08-01', '2026-08-31',
       NULL,           'NOT_STARTED', NULL, NULL),
  (310, NULL, 102, '변경 요청 대응',        5, '2025-10-01', '2026-08-31',
       'vitas-EMP001', 'IN_PROGRESS', NULL, NULL),
  -- 프로젝트 103 (COMPLETED · EMP001 EDITOR) → 진척률 5/5 = 100%
  (311, 208,  103, '자산 목록 수집',        1, '2025-11-01', '2025-11-20',
       'vitas-EMP001', 'DONE', '2025-11-19 11:00:00', 'vitas-EMP001'),
  (312, 208,  103, '취약점 진단',           2, '2025-11-21', '2026-01-15',
       'vitas-EMP001', 'DONE', '2026-01-14 16:00:00', 'vitas-EMP001'),
  (313, 208,  103, '모의 침투 테스트',       3, '2026-01-16', '2026-02-15',
       'vitas-EMP001', 'DONE', '2026-02-13 17:00:00', 'vitas-EMP001'),
  (314, 209,  103, '개선 권고안 작성',       4, '2026-02-16', '2026-03-15',
       'vitas-EMP001', 'DONE', '2026-03-13 14:00:00', 'vitas-EMP001'),
  (315, 209,  103, '최종 보고',             5, '2026-03-16', '2026-03-31',
       'vitas-EMP001', 'DONE', '2026-03-30 10:00:00', 'vitas-EMP001'),
  -- 프로젝트 104 (CLOSED · EMP001 미참여) → 목록·상세 모두 403 이어야 한다
  (316, 210,  104, '현장 실사',             1, '2026-04-01', '2026-04-15',
       'vitas-EMP001', 'DONE',        '2026-04-14 13:00:00', 'vitas-EMP001'),
  (317, 210,  104, '적산 산출',             2, '2026-04-16', '2026-04-30',
       'vitas-EMP001', 'DONE',        '2026-04-29 18:00:00', 'vitas-EMP001'),
  (318, 210,  104, '입찰 서류 작성',        3, '2026-05-01', '2026-05-10',
       'vitas-EMP001', 'IN_PROGRESS', NULL, NULL),
  (319, 210,  104, '입찰 참가 등록',        4, '2026-05-11', '2026-05-18',
       NULL,           'NOT_STARTED', NULL, NULL),
  (320, 210,  104, '낙찰 결과 확인',        5, '2026-05-19', '2026-05-20',
       NULL,           'NOT_STARTED', NULL, NULL);

-- C-6. 스텝 권한 오버라이드 5건 (STP-010 · STP-011)
--   ⚠️ 조용히 틀리는 지점이다. 행의 유무가 아니라 프로젝트 권한과의 조합으로 최종값이 정해진다.
--
--   501 차단 : 프로젝트 EDITOR → 스텝 NONE      → 스텝 목록에서 제외 · 상세 403
--   502 승격 : 프로젝트 VIEWER → 스텝 EDITOR    → 이 스텝만 편집 가능
--   503 동일 : 프로젝트 EDITOR → 스텝 EDITOR    → 아무것도 안 바뀜(no-op)
--   504 강등 : 프로젝트 EDITOR → 스텝 VIEWER    → 조회만 가능
--   505 무시 : 미참여(104)    → 스텝 EDITOR    → ⛔ 그랜트 폐기(2026-08-06)라 무시되고 403
--   상속     : 행이 없는 스텝(301·304·305·306 …) → 프로젝트 권한 그대로
INSERT IGNORE INTO step_permission (step_permission_id, step_id, user_id, permission) VALUES
  (501, 302, 'vitas-EMP001', 'NONE'),
  (502, 308, 'vitas-EMP001', 'EDITOR'),
  (503, 303, 'vitas-EMP001', 'EDITOR'),
  (504, 311, 'vitas-EMP001', 'VIEWER'),
  (505, 316, 'vitas-EMP001', 'EDITOR');

-- C-7. 스테이지별 새 스텝 권한 기본값 5건 (STG-004)
--   ⚠️ 이 표는 권한 판정에 쓰이지 않는다 (INV-01). 스텝이 **생성될 때** 읽혀 step_permission 행으로
--      복사된다. 여기 SQL 로 넣은 행들은 위 C-5 의 기존 스텝 301~320 에 소급 적용되지 않는다 —
--      "왜 안 먹지?" 로 시간 날리기 쉬운 지점이다. 확인하려면 해당 스테이지에 스텝을 새로 만들어라.
--
--   601 : 착수(201)     EDITOR  → 새 스텝은 편집 가능하게 생성
--   602 : 설계(202)     NONE    → 새 스텝은 만들자마자 자기 눈에서 사라진다
--   603 : 구축(203)     VIEWER  → 프로젝트 EDITOR 인데 새 스텝만 조회 전용
--   604 : 구축(206)     EDITOR  → 프로젝트 VIEWER 인데 새 스텝은 편집 가능(승격 자동 적용)
--   605 : 입찰준비(210) EDITOR  → 미참여 프로젝트라 복사돼도 판정에서는 무시된다
INSERT IGNORE INTO stage_permission_default
  (stage_permission_default_id, stage_id, user_id, permission) VALUES
  (601, 201, 'vitas-EMP001', 'EDITOR'),
  (602, 202, 'vitas-EMP001', 'NONE'),
  (603, 203, 'vitas-EMP001', 'VIEWER'),
  (604, 206, 'vitas-EMP001', 'EDITOR'),
  (605, 210, 'vitas-EMP001', 'EDITOR');


-- =====================================================================
-- D. 투입 검증 가드 — 이 시드가 또 조용히 스킵되는 것을 막는다
-- ---------------------------------------------------------------------
-- 원본 3본이 전건 스킵되고도 Flyway 가 success=1 로 기록한 게 이번 사고의 본질이다.
-- INSERT IGNORE 를 유지하면서 "들어갔는지" 만 사후에 확인한다.
--
-- 동작: 사원 더미(EMP001~004)가 있는 환경에서 기대 건수에 못 미치면 스칼라 서브쿼리가 2행을 반환해
--       오류(1242 Subquery returns more than 1 row)로 마이그레이션이 실패한다.
--       사원 더미가 아예 없는 환경(신규 CI DB 등)은 시드 대상이 아니므로 통과시킨다 —
--       그 환경은 원본 3본도 같은 이유로 비어 있다.
-- =====================================================================
SELECT CASE
  WHEN (SELECT COUNT(*) FROM employee
         WHERE user_id IN ('vitas-EMP001','vitas-EMP002','vitas-EMP003','vitas-EMP004')) < 4
       THEN 0
  WHEN (SELECT COUNT(*) FROM project WHERE project_id BETWEEN 1   AND 5)   < 5
    OR (SELECT COUNT(*) FROM project WHERE project_id BETWEEN 101 AND 105) < 5
    OR (SELECT COUNT(*) FROM stage   WHERE stage_id   BETWEEN 1   AND 12)  < 12
    OR (SELECT COUNT(*) FROM stage   WHERE stage_id   BETWEEN 201 AND 210) < 10
    OR (SELECT COUNT(*) FROM step    WHERE step_id    BETWEEN 1   AND 14)  < 14
    OR (SELECT COUNT(*) FROM step    WHERE step_id    BETWEEN 301 AND 320) < 20
    OR (SELECT COUNT(*) FROM block   WHERE block_id   BETWEEN 1   AND 14)  < 14
    OR (SELECT COUNT(*) FROM issue   WHERE issue_id   BETWEEN 1   AND 4)   < 4
       THEN (SELECT n FROM (SELECT 1 AS n UNION ALL SELECT 2) t)
  ELSE 0 END AS reseed_guard;
