-- =====================================================================
-- 08. 활동 로그 34 · 알림 3
-- ---------------------------------------------------------------------
-- 무엇: activity_log / notification.
-- 왜:   시드 SQL 은 앱을 안 거치므로 로그가 한 줄도 안 남는다.
--       100블록짜리 프로젝트인데 활동기록 탭이 빈 화면이면
--       "이력 관리가 존재 이유" 라는 발표가 그 자리에서 무너진다.
--
-- 선행: 03_blocks.sql · 05_issues.sql
-- ⛔ 마지막에 실행하라. activity_log 에는 deleted_at 이 없어(D-5) 되돌릴 수단이 없다.
--
-- =====================================================================
-- 🚨 이 테이블은 ERD.md §5-4 확정본과 다르다 (2026-08-15 실측)
-- ---------------------------------------------------------------------
--   V20260804123025__align_activity_log_schema.sql 이 DROP 한 컬럼:
--     project_id · resource_type · target_name · privileged_override
--   그리고 block_id 를 NOT NULL 로 바꿨다. ActivityLogEntity 도 그대로다.
--
--   실제 컬럼:
--     activity_log_id · company_id · act · resource_id · resource_name(TEXT)
--     · field · before_value · after_value · block_id(NOT NULL) · user_id · created_at
--
--   ⚠️ act 는 ENUM('create','delete','modify','restore','purge') — 전부 소문자다.
--      'CREATE' 를 넣으면 ENUM 위반으로 INSERT 자체가 실패한다.
--
--   ⛔ block_id NOT NULL 이라 프로젝트·스테이지·스텝·멤버·이슈 로그를 넣을 자리가 없다.
--      원래 설계 96건 중 62건이 불가능하고 블록 사건 34건만 남는다.
--
--   ⛔ privileged_override 컬럼이 없다 → PRJ-017 「상위권한으로 수정」 배지는
--      화면에 뜰 수가 없다. 발표에서 언급하지 마라.
--
--   → 명세 3건(PRJ-016 · PRJ-017 · INV-09)이 현재 스키마로 성립하지 않는다.
--     요구사항을 접을지 컬럼을 되살릴지는 팀 결정 사항이다.
-- =====================================================================
--
-- ⛔ 블록 100개 생성 로그를 다 넣지 마라. 「블록 생성」 100줄이 되어
--    정작 보여줄 사건이 스크롤 아래로 밀린다. 발표에서 열 블록만 골랐다.
--
-- 되돌리기: DELETE FROM activity_log WHERE activity_log_id BETWEEN 9001 AND 9034;
--           DELETE FROM notification WHERE notification_id BETWEEN 9001 AND 9003;
-- =====================================================================


-- ── 1. act = 'create' 18 ─────────────────────────────────────────────
-- resource_id 는 블록 상세 행 PK, resource_name 은 표시명 스냅샷이다.
-- ⚠️ resource_name 은 전건 채운다. 비면 활동기록이 빈 줄로 뜨는데 에러는 안 난다.
INSERT IGNORE INTO activity_log
  (activity_log_id, company_id, block_id, act, resource_id, resource_name,
   field, before_value, after_value, user_id, created_at) VALUES
(9001, 2, 9012, 'create', NULL, '사업성 검토 보고서',       NULL, NULL, NULL, 'vitawear-VW101', '2025-11-26 10:05:00'),
(9002, 2, 9008, 'create', 9001, 'AI 사업성 검토 4항목',     NULL, NULL, NULL, 'vitawear-VW101', '2025-12-01 09:30:00'),
(9003, 2, 9013, 'create', 9001, '입점 추진 승인',           NULL, NULL, NULL, 'vitawear-VW103', '2025-11-28 15:50:00'),
(9004, 2, 9018, 'create', NULL, '소개서·룩북·판매가 시트',  NULL, NULL, NULL, 'vitawear-VW101', '2025-12-10 18:20:00'),
(9005, 2, 9019, 'create', 9002, '제품컷·룩북 컷',           NULL, NULL, NULL, 'vitawear-VW104', '2025-12-11 20:05:00'),
(9006, 2, 9027, 'create', 9002, '신청 제출 승인',           NULL, NULL, NULL, 'vitawear-VW103', '2025-12-12 09:20:00'),
(9007, 2, 9040, 'create', 9003, '계약 체결 승인',           NULL, NULL, NULL, 'vitawear-VW103', '2025-12-18 09:50:00'),
(9008, 2, 9043, 'create', 9008, '스타일별 등록',            NULL, NULL, NULL, 'vitawear-VW102', '2026-01-12 10:50:00'),
(9009, 2, 9044, 'create', NULL, '일괄 업로드 시트',         NULL, NULL, NULL, 'vitawear-VW102', '2026-01-12 11:00:00'),
(9010, 2, 9045, 'create', 9023, '업로드 오류 이력',         NULL, NULL, NULL, 'vitawear-VW102', '2026-01-14 15:35:00'),
(9011, 2, 9054, 'create', 9004, '발주 승인',                NULL, NULL, NULL, 'vitawear-VW105', '2025-12-26 09:50:00'),
(9012, 2, 9055, 'create', 9029, '입고·검품 결과',           NULL, NULL, NULL, 'vitawear-VW105', '2026-02-07 18:05:00'),
(9013, 2, 9060, 'create', 9006, '랭킹·클레임',              NULL, NULL, NULL, 'vitawear-VW105', '2026-02-24 11:10:00'),
(9014, 2, 9062, 'create', 9034, '개선 액션',                NULL, NULL, NULL, 'vitawear-VW105', '2026-03-04 16:20:00'),
(9015, 2, 9068, 'create', 9005, '발주 승인',                NULL, NULL, NULL, 'vitawear-VW105', '2026-03-18 10:20:00'),
-- 정산 회차 블록 3개를 한 스텝에 나란히 만든 시점
(9016, 2, 9088, 'create', 9001, '1차 정산',                 NULL, NULL, NULL, 'vitawear-VW108', '2026-03-01 09:10:00'),
(9017, 2, 9089, 'create', 9002, '2차 정산',                 NULL, NULL, NULL, 'vitawear-VW108', '2026-03-01 09:12:00'),
(9018, 2, 9090, 'create', 9003, '3차 정산',                 NULL, NULL, NULL, 'vitawear-VW108', '2026-03-01 09:14:00');


-- ── 2. act = 'modify' 14 ─────────────────────────────────────────────
INSERT IGNORE INTO activity_log
  (activity_log_id, company_id, block_id, act, resource_id, resource_name,
   field, before_value, after_value, user_id, created_at) VALUES

-- ⭐⭐ 특수 로그 1 — resource_name 스냅샷이 「옛 이름」으로 남는다
--     블록 제목은 지금 '1차 정산 (2026-02월분)' 인데 이 로그에는 '1차 정산' 이 박혀 있다.
--     FK 만 들었으면 과거 로그가 전부 새 이름으로 표시돼 그 로그가 거짓이 된다 (INV-09 의 취지).
(9019, 2, 9088, 'modify', 9001, '1차 정산',
 'title', '1차 정산', '1차 정산 (2026-02월분)', 'vitawear-VW108', '2026-03-02 10:05:00'),

(9020, 2, 9012, 'modify', NULL, '사업성 검토 보고서',
 'fileVersion', 'v4', 'v5', 'vitawear-VW101', '2025-12-02 11:30:00'),
(9021, 2, 9037, 'modify', 9019, '독소 조항 검토 의견',
 'content', '요청 2건 회신 대기', '1건 반영 / 1건 미반영', 'vitawear-VW103', '2025-12-19 16:10:00'),
(9022, 2, 9042, 'modify', 9022, '등록 대상·표준',
 'content', 'SKU 112개', 'SKU 118개', 'vitawear-VW101', '2026-01-20 14:30:00'),
(9023, 2, 9045, 'modify', 9023, '업로드 오류 이력',
 'content', '2차 반려까지', '3차 반려까지', 'vitawear-VW102', '2026-01-19 09:45:00'),
(9024, 2, 9044, 'modify', NULL, '일괄 업로드 시트',
 'fileVersion', 'v5', 'v6', 'vitawear-VW101', '2026-01-22 14:15:00'),
(9025, 2, 9043, 'modify', 9008, '스타일별 등록',
 'checked', '11/12', '12/12', 'vitawear-VW102', '2026-01-22 14:20:00'),
(9026, 2, 9055, 'modify', 9029, '입고·검품 결과',
 'content', '입고 3,400', '입고 3,362 (폐기 22 · 미납 16)', 'vitawear-VW105', '2026-02-07 18:40:00'),
(9027, 2, 9062, 'modify', 9034, '개선 액션',
 'content', '반품 사유 분석만', '개선 액션 2건 추가', 'vitawear-VW105', '2026-03-04 17:00:00'),

-- 정산 진행 — 1차 입금 확정, 2차 예정금액 확정
(9028, 2, 9088, 'modify', 9001, '1차 정산 (2026-02월분)',
 'actualAmount', NULL, '55950000', 'vitawear-VW108', '2026-03-10 13:20:00'),
(9029, 2, 9092, 'modify', 9052, '대조·이의 이력',
 'content', '차이 74,000 발견', '이의 인정 — 2차에 가산 반영', 'vitawear-VW108', '2026-03-09 14:25:00'),
(9030, 2, 9089, 'modify', 9002, '2차 정산 (2026-03월분)',
 'plannedAmount', NULL, '74100000', 'vitawear-VW108', '2026-04-03 09:55:00'),

-- 기준일 직전 — 진행 중 스텝의 움직임
(9031, 2, 9071, 'modify', 9012, '노출·운영 점검',
 'checked', '3/8', '5/8', 'vitawear-VW102', '2026-04-06 17:40:00'),
(9032, 2, 9070, 'modify', 9039, '판매 실적',
 'content', '2026-03 마감까지', '04-08 진행분 포함', 'vitawear-VW102', '2026-04-08 09:15:00');


-- ── 3. act = 'delete' 2 ──────────────────────────────────────────────
-- ⭐ 특수 로그 2 — 대상은 사라졌는데 로그는 산다 (INV-05).
--    S2-2 에서 체크리스트 4장을 2장으로 합친 흔적이다.
--    블록 9101·9102 는 deleted_at 이 찍혀 있어 조회에 안 잡히지만 이 로그는 남는다.
INSERT IGNORE INTO activity_log
  (activity_log_id, company_id, block_id, act, resource_id, resource_name,
   field, before_value, after_value, user_id, created_at) VALUES
(9033, 2, 9101, 'delete', 9017, '이미지 규격 검토', NULL, NULL, NULL, 'vitawear-VW103', '2025-12-11 09:20:00'),
(9034, 2, 9102, 'delete', 9018, '사이즈표 검토',   NULL, NULL, NULL, 'vitawear-VW103', '2025-12-11 09:20:00');


-- ── 4. notification 3 ────────────────────────────────────────────────
-- ⚠️ block_id 컬럼은 DROP 됐다 (V20260807110000). 넣으면 실패한다.
-- ⚠️ CHECK ck_notification_target — target_type·target_id 는 함께 있거나 함께 없어야 한다.
-- ⛔ 읽음 컬럼이 없다 (deleted_at 만). 많이 넣으면 벨에 영구히 쌓인다.
--    미완료 이슈 3건 배정만 남긴다.
INSERT IGNORE INTO notification
  (notification_id, user_id, notification_type, title, message,
   target_type, target_id, target_context, created_at) VALUES
(9001, 'vitawear-VW104', 'ISSUE_ASSIGNED', '새 이슈가 배정되었습니다',
 '사이즈표 실측 재기입 (12스타일) — 기한 2026-03-20',
 'ISSUE', 9009, NULL, '2026-03-06 09:05:00'),
(9002, 'vitawear-VW102', 'ISSUE_ASSIGNED', '새 이슈가 배정되었습니다',
 '4월 기획전 선정 결과 확인 — 기한 2026-04-12',
 'ISSUE', 9010, NULL, '2026-04-07 09:02:00'),
(9003, 'vitawear-VW108', 'ISSUE_ASSIGNED', '새 이슈가 배정되었습니다',
 '2차 정산 입금 확인 (04-10) — 기한 2026-04-10',
 'ISSUE', 9013, NULL, '2026-04-08 09:03:00');


-- =====================================================================
-- 검증
-- =====================================================================
-- 1) act 분포 — create 18 / modify 14 / delete 2
--    SELECT act, COUNT(*) FROM activity_log
--    WHERE activity_log_id BETWEEN 9001 AND 9034 GROUP BY act;
--
-- 2) resource_name 이 전건 차 있나 (0행이어야 정상)
--    SELECT activity_log_id FROM activity_log
--    WHERE activity_log_id BETWEEN 9001 AND 9034
--      AND (resource_name IS NULL OR resource_name = '');
--
-- 3) ⭐ 옛 이름 스냅샷 — 로그의 이름과 현재 블록 제목이 달라야 정상 (1행)
--    SELECT al.activity_log_id, al.resource_name AS logged, b.title AS current_title
--    FROM activity_log al JOIN block b ON b.block_id = al.block_id
--    WHERE al.activity_log_id = 9019;
--    기대: logged = '1차 정산' / current_title = '1차 정산 (2026-02월분)'
--
-- 4) ⭐ 삭제된 블록의 로그가 남아 있나 (2행)
--    SELECT al.activity_log_id, al.resource_name, b.deleted_at
--    FROM activity_log al JOIN block b ON b.block_id = al.block_id
--    WHERE al.act = 'delete' AND al.activity_log_id BETWEEN 9001 AND 9034;
--
-- 5) 시간순으로 흐르나 — 2025-11-26 ~ 2026-04-08
--    SELECT MIN(created_at), MAX(created_at) FROM activity_log
--    WHERE activity_log_id BETWEEN 9001 AND 9034;
