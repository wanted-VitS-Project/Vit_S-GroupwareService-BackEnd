-- =====================================================================
-- 17. 프로젝트 10개 구성 + 팀 배정
-- ---------------------------------------------------------------------
-- 목록에 10개가 뜨고, 그중 2개(9001 · 9002)는 안에 내용이 차 있다.
-- 나머지 8개는 스텝과 결재만 있는 가벼운 프로젝트다.
--
-- ⭐ MEMBER 6명(VW101~VW106)은 **10개 프로젝트 전부**에 들어간다. 누구로 로그인해도
--    같은 목록을 본다 = "6명이 한 팀으로 같이 본다".
--    ADMIN 은 자기가 결재를 기안한 프로젝트에만 붙인다 (기안하려면 EDITOR 여야 한다).
--
-- ⚠️ 9006 만 NOT_STARTED 다. 여기 결재 4건은 전부 DRAFT(상신 전)라 모순이 없다.
--    나머지 프로젝트는 결재가 상신·완료 상태라 스텝이 최소 IN_PROGRESS 여야 한다.
--
-- 되돌리기: DELETE FROM project_member WHERE project_id BETWEEN 9001 AND 9010;
--           DELETE FROM step  WHERE step_id  BETWEEN 9301 AND 9321;
--           DELETE FROM stage WHERE stage_id BETWEEN 9007 AND 9016;
--           DELETE FROM project WHERE project_id BETWEEN 9004 AND 9010;
-- =====================================================================

-- ── 스테이지 이름 잔재 정리 (15 에서 빠졌다) ──────────────────────
UPDATE stage SET name='월 정산' WHERE stage_id=9005;

-- ── 신규 프로젝트 7건 ─────────────────────────────────────────────
INSERT INTO project
  (project_id, company_id, name, description, status, client_name, started_on, ended_on, created_by) VALUES
(9004, 2, 'W컨셉 입점 검토',
 '무신사 다음 채널로 W컨셉을 보고 있다. 수수료가 높은 대신 컨템포러리 고객이 두터워서, 26 F/W 아우터 라인을 여기에 걸어볼지 판단한다.',
 'IN_PROGRESS', '(주)더블유컨셉코리아', '2026-03-16', NULL, 'vitawear-VW103'),

(9005, 2, '자사몰 리뉴얼',
 '자사몰 이탈률이 높아 장바구니와 결제 흐름을 갈아엎는다. 무신사와 가격을 같이 가져가는 만큼 자사몰만의 이유가 필요하다.',
 'IN_PROGRESS', NULL, '2026-02-02', '2026-06-30', 'vitawear-VW101'),

(9006, 2, '26 F/W 시즌 기획',
 '26 S/S 판매 데이터가 나오는 대로 착수한다. 지금은 스텝만 잡아두고 결재는 임시저장 상태다.',
 'NOT_STARTED', NULL, '2026-06-01', NULL, 'vitawear-VW106'),

(9007, 2, 'OEM 수주 (라온어패럴)',
 '라온어패럴 26 F/W 니트 물량을 우리 생산 라인으로 받는 건이다. 자체 브랜드 비수기에 라인을 놀리지 않으려고 잡았다.',
 'IN_PROGRESS', '(주)라온어패럴', '2026-01-05', '2026-08-31', 'vitawear-VW109'),

(9008, 2, 'OEM 수주 (한성텍스타일)',
 '한성텍스타일 우븐 셔츠 12,000장. 납품까지 끝났고 잔금도 들어왔다.',
 'COMPLETED', '(주)한성텍스타일', '2025-08-01', '2026-01-31', 'vitawear-VW110'),

(9009, 2, '물류창고 이전',
 '무신사 물량이 늘면서 기존 창고가 좁아졌다. 김포로 옮기고 재고를 이관한다.',
 'IN_PROGRESS', '(주)한진물류', '2026-03-02', '2026-05-29', 'vitawear-VW111'),

(9010, 2, '25 S/S 재고 처분',
 '25 S/S 잔여 재고 4,100장을 아울렛과 리퍼 채널로 털었다. 원가 이하로 나간 물량이 있어 손실을 별도로 집계했다.',
 'COMPLETED', NULL, '2025-09-01', '2025-12-19', 'vitawear-VW108');

-- ── 스테이지 ──────────────────────────────────────────────────────
-- 9002(25 F/W)는 스텝만 있고 스테이지가 없었다. 3개로 묶는다.
INSERT INTO stage (stage_id, project_id, name, sort_order) VALUES
(9007, 9002, '입점과 세팅',   0),
(9008, 9002, '시즌 운영',     1),
(9009, 9002, '정산과 결산',   2),
(9010, 9004, '검토',          0),
(9011, 9005, '리뉴얼',        0),
(9012, 9006, '기획',          0),
(9013, 9007, '수주 진행',     0),
(9014, 9008, '수주 진행',     0),
(9015, 9009, '이전 진행',     0),
(9016, 9010, '처분 진행',     0);

UPDATE step SET stage_id=9007 WHERE step_id BETWEEN 9101 AND 9104;
UPDATE step SET stage_id=9008 WHERE step_id BETWEEN 9105 AND 9109;
UPDATE step SET stage_id=9009 WHERE step_id BETWEEN 9110 AND 9112;

-- ── 9002 스텝 이름 (지금은 "25 F/W 스텝 1" 같은 자리표시자다) ──────
UPDATE step SET name='입점 신청과 심사'   WHERE step_id=9101;
UPDATE step SET name='계약과 계정 등록'   WHERE step_id=9102;
UPDATE step SET name='상품 등록과 오픈'   WHERE step_id=9103;
UPDATE step SET name='초도 발주와 입고'   WHERE step_id=9104;
UPDATE step SET name='9월 판매 운영'      WHERE step_id=9105;
UPDATE step SET name='2차 발주와 입고'    WHERE step_id=9106;
UPDATE step SET name='10월 판매 운영'     WHERE step_id=9107;
UPDATE step SET name='시즌오프 운영'      WHERE step_id=9108;
UPDATE step SET name='재고 소진'          WHERE step_id=9109;
UPDATE step SET name='월 정산'            WHERE step_id=9110;
UPDATE step SET name='반품 정산과 이의'   WHERE step_id=9111;
UPDATE step SET name='시즌 결산'          WHERE step_id=9112;

-- ── 신규 프로젝트 스텝 21건 ───────────────────────────────────────
INSERT INTO step (step_id, project_id, stage_id, name, sort_order, status, started_on, ended_on, owner_user_id) VALUES
-- 9004 W컨셉 입점 검토 (IN_PROGRESS)
(9301, 9004, 9010, '채널 조사',        0, 'DONE',        '2026-03-16','2026-03-27','vitawear-VW102'),
(9302, 9004, 9010, '입점 조건 협의',   1, 'IN_PROGRESS', '2026-03-30', NULL,       'vitawear-VW103'),
(9303, 9004, 9010, '추진 여부 결정',   2, 'NOT_STARTED', NULL,         NULL,       'vitawear-VW107'),
-- 9005 자사몰 리뉴얼 (IN_PROGRESS)
(9304, 9005, 9011, '요구사항 정리',    0, 'DONE',        '2026-02-02','2026-02-20','vitawear-VW101'),
(9305, 9005, 9011, '업체 선정',        1, 'DONE',        '2026-02-23','2026-03-13','vitawear-VW108'),
(9306, 9005, 9011, '개발과 오픈',      2, 'IN_PROGRESS', '2026-03-16', NULL,       'vitawear-VW104'),
-- 9006 26 F/W 시즌 기획 (NOT_STARTED)
(9307, 9006, 9012, '시즌 컨셉 확정',   0, 'NOT_STARTED', NULL, NULL, 'vitawear-VW106'),
(9308, 9006, 9012, '스타일 라인업',    1, 'NOT_STARTED', NULL, NULL, 'vitawear-VW104'),
(9309, 9006, 9012, '생산 계획',        2, 'NOT_STARTED', NULL, NULL, 'vitawear-VW111'),
-- 9007 OEM 라온어패럴 (IN_PROGRESS)
(9310, 9007, 9013, '견적과 계약',      0, 'DONE',        '2026-01-05','2026-01-23','vitawear-VW109'),
(9311, 9007, 9013, '샘플 승인',        1, 'DONE',        '2026-01-26','2026-02-27','vitawear-VW104'),
(9312, 9007, 9013, '본생산과 납품',    2, 'IN_PROGRESS', '2026-03-02', NULL,       'vitawear-VW111'),
-- 9008 OEM 한성텍스타일 (COMPLETED)
(9313, 9008, 9014, '견적과 계약',      0, 'DONE', '2025-08-01','2025-08-22','vitawear-VW110'),
(9314, 9008, 9014, '샘플 승인',        1, 'DONE', '2025-08-25','2025-09-26','vitawear-VW104'),
(9315, 9008, 9014, '본생산과 납품',    2, 'DONE', '2025-09-29','2026-01-30','vitawear-VW111'),
-- 9009 물류창고 이전 (IN_PROGRESS)
(9316, 9009, 9015, '후보지 실사',      0, 'DONE',        '2026-03-02','2026-03-20','vitawear-VW105'),
(9317, 9009, 9015, '계약과 인테리어',  1, 'DONE',        '2026-03-23','2026-04-24','vitawear-VW107'),
(9318, 9009, 9015, '이전과 재고 이관', 2, 'IN_PROGRESS', '2026-04-27', NULL,       'vitawear-VW105'),
-- 9010 25 S/S 재고 처분 (COMPLETED)
(9319, 9010, 9016, '재고 실사',        0, 'DONE', '2025-09-01','2025-09-19','vitawear-VW105'),
(9320, 9010, 9016, '처분 채널 선정',   1, 'DONE', '2025-09-22','2025-10-17','vitawear-VW108'),
(9321, 9010, 9016, '처분 실행',        2, 'DONE', '2025-10-20','2025-12-19','vitawear-VW110');

-- ── 팀 배정 ───────────────────────────────────────────────────────
DELETE FROM project_member WHERE project_id BETWEEN 9001 AND 9010;

-- MEMBER 6명 → 10개 프로젝트 전부. 한지훈(본부장)만 VIEWER 다.
INSERT INTO project_member (project_id, user_id, permission)
SELECT p.project_id, u.user_id, u.perm
FROM project p
CROSS JOIN (
  SELECT 'vitawear-VW101' user_id, 'EDITOR' perm UNION ALL
  SELECT 'vitawear-VW102', 'EDITOR' UNION ALL
  SELECT 'vitawear-VW103', 'EDITOR' UNION ALL
  SELECT 'vitawear-VW104', 'EDITOR' UNION ALL
  SELECT 'vitawear-VW105', 'EDITOR' UNION ALL
  SELECT 'vitawear-VW106', 'VIEWER'
) u
WHERE p.project_id BETWEEN 9001 AND 9010;

-- ADMIN·MASTER → 자기가 결재를 기안하는 프로젝트에만. 기안하려면 EDITOR 여야 한다.
INSERT INTO project_member (project_id, user_id, permission) VALUES
-- 9001 무신사 26 S/S — 재무·대표는 보기만
(9001, 'vitawear-VW107', 'VIEWER'),
(9001, 'vitawear-VW108', 'VIEWER'),
-- 9002 25 F/W — 결재 12건이 전부 여기 있어 전원 참여
(9002, 'vitawear-VW107', 'EDITOR'),
(9002, 'vitawear-VW108', 'EDITOR'),
(9002, 'vitawear-VW109', 'EDITOR'),
(9002, 'vitawear-VW110', 'EDITOR'),
(9002, 'vitawear-VW111', 'EDITOR'),
(9002, 'vitawear-VW112', 'EDITOR'),
(9002, 'vitawear-VW113', 'EDITOR'),
-- 9004 W컨셉
(9004, 'vitawear-VW107', 'EDITOR'),
(9004, 'vitawear-VW112', 'EDITOR'),
-- 9005 자사몰 리뉴얼
(9005, 'vitawear-VW108', 'EDITOR'),
(9005, 'vitawear-VW113', 'EDITOR'),
-- 9006 26 F/W 기획
(9006, 'vitawear-VW109', 'EDITOR'),
(9006, 'vitawear-VW110', 'EDITOR'),
(9006, 'vitawear-VW111', 'EDITOR'),
-- 9007 OEM 라온
(9007, 'vitawear-VW109', 'EDITOR'),
(9007, 'vitawear-VW111', 'VIEWER'),
-- 9008 OEM 한성
(9008, 'vitawear-VW110', 'EDITOR'),
-- 9009 물류창고 이전
(9009, 'vitawear-VW107', 'EDITOR'),
(9009, 'vitawear-VW111', 'EDITOR'),
-- 9010 재고 처분
(9010, 'vitawear-VW108', 'EDITOR'),
(9010, 'vitawear-VW112', 'EDITOR'),
(9010, 'vitawear-VW113', 'EDITOR');
