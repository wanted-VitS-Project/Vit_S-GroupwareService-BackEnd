-- =====================================================================
-- 블록 샘플 데이터 — block · text · checklist_block · checklist · issue · issue_block
-- ---------------------------------------------------------------------
-- 목적: 블록 골격 조회·생성(B1~B4) + TEXT/CHECKLIST 상세 어댑터 + 이슈 집계(B5)를
--   실데이터로 검증. V20260805150000(프로젝트 샘플 데이터)의 step 을 그대로 사용한다.
-- 범위: TEXT · CHECKLIST 만 상세 내용까지 채운다 — 다른 8종은 담당 어댑터가
--   아직 없어(BlockDetailRegistry 미등록) 만들어도 type_id 가 NULL로 남을 뿐이라 제외.
-- 참조 employee: EMP001~EMP004 (V20260805150000 과 동일 전제).
-- 재적용 안전: 전 테이블 명시적 PK + INSERT IGNORE (block/text/checklist/issue 계열
--   전부 net-new — dev RDS·localtest 양쪽 0건 확인 후 1부터 채번).
-- =====================================================================

-- 블록 골격 14건 (step 1·2·3·6·8·9·10·13)
INSERT IGNORE INTO block
    (block_id, step_id, title, type, type_id, owner, row_index, col_span, sort_order, created_by) VALUES
    (1,  1,  '요구사항 정의서',        'TEXT',      1, 'EMP001', 0, 1, 0, 'EMP001'),
    (2,  1,  '정의서 작성 체크리스트',  'CHECKLIST', 1, 'EMP001', 0, 1, 1, 'EMP001'),
    (3,  2,  '아키텍처 설계 노트',      'TEXT',      2, 'EMP003', 0, 1, 0, 'EMP003'),
    (4,  2,  '설계 검토 체크리스트',    'CHECKLIST', 2, 'EMP003', 0, 1, 1, 'EMP003'),
    (5,  3,  'API 설계 메모',          'TEXT',      3, 'EMP001', 0, 1, 0, 'EMP001'),
    (6,  3,  '개발 체크리스트',        'CHECKLIST', 3, 'EMP001', 0, 1, 1, 'EMP001'),
    (7,  6,  '요구사항 수집 계획',      'TEXT',      4, 'EMP003', 0, 1, 0, 'EMP003'),
    (8,  6,  '수집 체크리스트',        'CHECKLIST', 4, 'EMP003', 0, 1, 1, 'EMP003'),
    (9,  8,  '구축 결과 요약',         'TEXT',      5, 'EMP001', 0, 1, 0, 'EMP001'),
    (10, 8,  '구축 체크리스트',        'CHECKLIST', 5, 'EMP001', 0, 1, 1, 'EMP001'),
    (11, 9,  '정산 검증 체크리스트',    'CHECKLIST', 6, 'EMP004', 0, 1, 0, 'EMP004'),
    (12, 10, 'AI 챗봇 요구사항 분석 결과', 'TEXT',   6, 'EMP002', 0, 1, 0, 'EMP002'),
    (13, 13, '제안서 초안',            'TEXT',      7, 'EMP004', 0, 1, 0, 'EMP004'),
    (14, 13, '제안서 체크리스트',      'CHECKLIST', 7, 'EMP004', 0, 2, 1, 'EMP004');

-- TEXT 상세 7건
INSERT IGNORE INTO text (txt_id, block_id, content) VALUES
    (1, 1,  '요구사항 정의서\n\n- 대상 기관: 조달청\n- 핵심 기능: 결재/문서관리/일정관리\n- 비기능 요구사항: 동시접속 300명, 응답시간 2초 이내\n\n2026-03-14 검토 완료, 발주처 서면 승인.'),
    (2, 3,  '시스템 아키텍처 설계 노트\n\n- 계층: 헥사고날 아키텍처(domain/application/infrastructure/presentation)\n- DB: MySQL 8.4, 세션 저장소 Redis\n- 인증: 세션 쿠키 방식\n\n검토 중 — API 게이트웨이 도입 여부 논의 필요.'),
    (3, 5,  '백엔드 API 설계 메모\n\n- 프로젝트/스테이지/스텝/블록 CRUD API 우선 개발\n- 헥사고날 서브패키지 구조 적용\n- MyBatis는 조회 전용, JPA는 쓰기 전용.'),
    (4, 7,  '요구사항 수집 계획\n\n- 대상: 결재 담당 부서 인터뷰\n- 기간: 2026-09-01 ~ 2026-09-10\n- 산출물: 요구사항 정의서 초안'),
    (5, 9,  '회계 시스템 구축 결과 요약\n\n- 전표 처리·정산 모듈 구축 완료\n- 기존 회계 시스템 데이터 마이그레이션 완료(2026-02-27)\n- 안정화 기간 1개월 진행'),
    (6, 12, 'AI 챗봇 요구사항 분석 결과\n\n- 대상 업무: 사내 규정 문의, 결재 상태 조회\n- 연동: 비타메이트 분석 엔진\n- 우선순위: 규정 문의 > 결재 조회 > 일정 안내'),
    (7, 13, '제안서 초안\n\n- 사업명: 구 사옥 리모델링 공사\n- 참여 형태: 단독 참여\n- 제출 기한: 2026-01-25');

-- CHECKLIST 상세 껍데기 7건
INSERT IGNORE INTO checklist_block (chk_block_id, block_id) VALUES
    (1, 2), (2, 4), (3, 6), (4, 8), (5, 10), (6, 11), (7, 14);

-- CHECKLIST 항목 — 스텝 진행 상태에 맞춰 완료율을 다르게 구성
INSERT IGNORE INTO checklist (chk_id, chk_block_id, content, is_completed) VALUES
    -- chk_block 1 (step1 DONE) — 3/3 완료
    (1, 1, '기능 요구사항 수집', TRUE),
    (2, 1, '비기능 요구사항 정의', TRUE),
    (3, 1, '발주처 검토 및 승인', TRUE),
    -- chk_block 2 (step2 IN_PROGRESS) — 2/4 완료
    (4, 2, '논리 아키텍처 다이어그램 작성', TRUE),
    (5, 2, '인프라 구성도 작성', TRUE),
    (6, 2, '보안 설계 검토', FALSE),
    (7, 2, '성능 요구사항 반영', FALSE),
    -- chk_block 3 (step3 IN_PROGRESS) — 2/5 완료
    (8,  3, '프로젝트 API 개발', TRUE),
    (9,  3, '스테이지 API 개발', TRUE),
    (10, 3, '스텝 API 개발', FALSE),
    (11, 3, '블록 API 개발', FALSE),
    (12, 3, '통합 테스트 작성', FALSE),
    -- chk_block 4 (step6 NOT_STARTED) — 0/3 완료
    (13, 4, '이해관계자 인터뷰 일정 수립', FALSE),
    (14, 4, '현행 프로세스 분석', FALSE),
    (15, 4, '요구사항 초안 작성', FALSE),
    -- chk_block 5 (step8 DONE) — 4/4 완료
    (16, 5, '전표 처리 모듈 개발', TRUE),
    (17, 5, '정산 모듈 개발', TRUE),
    (18, 5, '데이터 마이그레이션', TRUE),
    (19, 5, '안정화 테스트', TRUE),
    -- chk_block 6 (step9 IN_PROGRESS) — 1/3 완료
    (20, 6, '정산 데이터 검증 항목 정의', TRUE),
    (21, 6, '1차 검증 수행', FALSE),
    (22, 6, '재무팀 승인 요청', FALSE),
    -- chk_block 7 (step13 DONE) — 3/3 완료
    (23, 7, '제안서 목차 작성', TRUE),
    (24, 7, '견적서 작성', TRUE),
    (25, 7, '제출 서류 취합', TRUE);

-- 이슈 4건 — 블록-이슈 집계(B5, BlockIssueStatLookupPort) 검증용
-- step_id 는 issue_block 으로 연결할 블록의 step 과 반드시 같아야 한다 (BLOCK.md §7 같은 스텝 제약)
INSERT IGNORE INTO issue
    (issue_id, title, content, status, step_id, priority, created_by) VALUES
    (1, '아키텍처 다이어그램 작성', '논리 아키텍처 다이어그램을 작성하고 리뷰를 받는다.', 'DONE', 2, 'MEDIUM', 'EMP003'),
    (2, '설계 리뷰 진행', '팀 내 설계 리뷰를 진행하고 피드백을 반영한다.', 'IN_PROGRESS', 2, 'HIGH', 'EMP003'),
    (3, '정산 승인 요청', '1차 정산 데이터에 대한 재무팀 승인을 요청한다.', 'DONE', 9, 'HIGH', 'EMP004'),
    (4, '정산 데이터 오류 확인', '정산 데이터 중 금액 불일치 항목을 확인한다.', 'TO_DO', 9, 'MEDIUM', 'EMP004');

-- 블록-이슈 연결 — block 3(step2 TEXT) 2건, block 11(step9 CHECKLIST) 2건
INSERT IGNORE INTO issue_block (issue_block_id, issue_id, block_id) VALUES
    (1, 1, 3),
    (2, 2, 3),
    (3, 3, 11),
    (4, 4, 11);
