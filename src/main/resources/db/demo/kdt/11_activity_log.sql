-- =====================================================================
-- KDT 11. 활동 로그 34 · 알림 3
-- ---------------------------------------------------------------------
-- 무엇: 블록 사건 34건과 이슈 배정 알림 3건.
-- 왜:   250블록짜리 프로젝트인데 활동기록 탭이 비어 있으면
--       「이력 관리가 존재 이유」라고 말해놓고 그 탭을 열었을 때 아무것도 없다.
--
-- ⛔ 반드시 맨 마지막에 실행한다.
--    `activity_log` 에는 `deleted_at` 이 없다. 되돌릴 수단이 없으니 다른 데이터가
--    전부 확정된 뒤 한 번만 돌린다.
--
-- 🚨 이 테이블은 ERD 문서와 실제 스키마가 다르다 (2026-08-16 DESC 로 확인)
--    마이그레이션이 `project_id`·`resource_type`·`target_name`·`privileged_override` 를 DROP 했고
--    `block_id` 를 NOT NULL 로 바꿨다.
--
--    → 기록할 수 있는 건 **블록 사건뿐이다.** 프로젝트·스테이지·스텝·멤버·이슈 로그는
--      넣을 자리가 아예 없다. 넣으려 하지 마라.
--    → `act` 는 ENUM('create','delete','modify','restore','purge') 로 **소문자다.**
--      'CREATE' 를 넣으면 ENUM 위반으로 INSERT 자체가 실패한다.
--    → `target_name` 이 아니라 `resource_name TEXT` 다. 비면 활동기록이 빈 줄로 뜨는데
--      **에러는 안 난다.** 전건 채운다.
--    → ⛔ `privileged_override` 컬럼이 없다. 「상위권한으로 수정」 배지는 화면에 뜰 수가 없다.
--       발표에서 언급하지 마라.
--
-- ⛔ 블록 250개 생성 로그를 다 넣지 마라. 「블록 생성」 250줄이 되어 정작 볼 사건이
--    스크롤 아래로 밀린다. 발표에서 실제로 열 블록만 골랐다.
--
-- 선행: 01~10 전부
--
-- 되돌리기: ⛔ 없다. DELETE FROM activity_log WHERE activity_log_id BETWEEN 8001 AND 8034;
--           로 지우는 수밖에 없고, 운영 DB 에서는 절대 하지 마라.
--           DELETE FROM notification WHERE notification_id BETWEEN 8001 AND 8003;
-- =====================================================================


-- ── 1. 활동 로그 34 ─────────────────────────────────────────────────
-- 시간순이다. field·before_value·after_value 는 modify 에만 채운다.
INSERT IGNORE INTO activity_log
  (activity_log_id, company_id, resource_id, resource_name, block_id, act,
   field, before_value, after_value, user_id, created_at) VALUES

-- 외주 계약과 정산 블록이 만들어진다
(8001, 3, NULL, '외주 계약서', 8107, 'create',
 NULL, NULL, NULL, 'vitaedu-VE109', '2026-07-06 14:10:00'),
(8002, 3, 8001, '심사대응 기능 보완 외주', 8065, 'create',
 NULL, NULL, NULL, 'vitaedu-VE108', '2026-06-29 11:20:00'),
(8003, 3, 8002, '콘텐츠 개발 1차 선금', 8112, 'create',
 NULL, NULL, NULL, 'vitaedu-VE109', '2026-07-08 16:30:00'),
(8004, 3, 8003, '콘텐츠 개발 2차 중도금', 8113, 'create',
 NULL, NULL, NULL, 'vitaedu-VE109', '2026-07-08 16:31:00'),
(8005, 3, 8004, '콘텐츠 개발 3차 잔금', 8114, 'create',
 NULL, NULL, NULL, 'vitaedu-VE109', '2026-07-08 16:32:00'),
(8006, 3, 8005, '자막 제작 선금', 8120, 'create',
 NULL, NULL, NULL, 'vitaedu-VE109', '2026-07-08 16:35:00'),
(8007, 3, 8006, '자막 제작 잔금', 8121, 'create',
 NULL, NULL, NULL, 'vitaedu-VE109', '2026-07-08 16:36:00'),
(8008, 3, 8007, '촬영·스튜디오 용역', 8122, 'create',
 NULL, NULL, NULL, 'vitaedu-VE109', '2026-07-08 16:37:00'),

-- 지급이 일어나면서 상태가 바뀐다
(8009, 3, 8002, '콘텐츠 개발 1차 선금', 8112, 'modify',
 '지급 상태', 'WAITING', 'COMPLETED', 'vitaedu-VE109', '2026-07-10 17:05:00'),
(8010, 3, 8005, '자막 제작 선금', 8120, 'modify',
 '지급 상태', 'WAITING', 'COMPLETED', 'vitaedu-VE109', '2026-07-27 09:55:00'),
(8011, 3, 8007, '촬영·스튜디오 용역', 8122, 'modify',
 '지급 상태', 'WAITING', 'COMPLETED', 'vitaedu-VE109', '2026-08-05 17:35:00'),
(8012, 3, 8003, '콘텐츠 개발 2차 중도금', 8113, 'modify',
 '지급 상태', 'WAITING', 'COMPLETED', 'vitaedu-VE109', '2026-08-10 15:45:00'),
(8013, 3, 8001, '심사대응 기능 보완 외주', 8065, 'modify',
 '지급 상태', 'WAITING', 'PARTIAL', 'vitaedu-VE109', '2026-06-30 16:15:00'),

-- 훈련비 회차
(8014, 3, 8008, '1차 훈련비 (2026-03 수료분)', 8188, 'create',
 NULL, NULL, NULL, 'vitaedu-VE109', '2026-04-01 09:30:00'),
(8015, 3, 8009, '2차 훈련비 (2026-05 수료분)', 8189, 'create',
 NULL, NULL, NULL, 'vitaedu-VE109', '2026-06-01 09:30:00'),
(8016, 3, 8008, '1차 훈련비 (2026-03 수료분)', 8188, 'modify',
 '지급 상태', 'WAITING', 'COMPLETED', 'vitaedu-VE109', '2026-04-15 14:05:00'),
(8017, 3, 8009, '2차 훈련비 (2026-05 수료분)', 8189, 'modify',
 '지급 상태', 'WAITING', 'COMPLETED', 'vitaedu-VE109', '2026-06-15 13:35:00'),
-- ⭐ 특수 1 — resource_name 은 그 시점의 이름을 스냅샷으로 남긴다
(8018, 3, 8010, '3차 훈련비', 8190, 'create',
 NULL, NULL, NULL, 'vitaedu-VE109', '2026-08-01 09:30:00'),
(8019, 3, 8010, '3차 훈련비', 8190, 'modify',
 '제목', '3차 훈련비', '3차 훈련비 (2026-07 수료분)', 'vitaedu-VE109', '2026-08-05 09:50:00'),
(8020, 3, 8011, '4차 훈련비 (2026-09 수료분)', 8191, 'create',
 NULL, NULL, NULL, 'vitaedu-VE109', '2026-08-01 09:31:00'),

-- ⭐ 특수 2 — 대상은 사라져도 로그는 산다 (INV-05)
--    자막 점검을 세 블록으로 쪼갰다가 하나로 합친 흔적이다
(8021, 3, NULL, '자막 정확성 점검', 8251, 'create',
 NULL, NULL, NULL, 'vitaedu-VE107', '2026-08-10 10:00:00'),
(8022, 3, NULL, '자막 가독성 점검', 8252, 'create',
 NULL, NULL, NULL, 'vitaedu-VE107', '2026-08-10 10:01:00'),
(8023, 3, NULL, '자막 정확성 점검', 8251, 'delete',
 NULL, NULL, NULL, 'vitaedu-VE107', '2026-08-13 11:20:00'),
(8024, 3, NULL, '자막 가독성 점검', 8252, 'delete',
 NULL, NULL, NULL, 'vitaedu-VE107', '2026-08-13 11:21:00'),

-- 신청 서류와 결재
(8025, 3, NULL, '신청자격 판단 결과 승인', 8016, 'create',
 NULL, NULL, NULL, 'vitaedu-VE103', '2026-08-06 10:10:00'),
(8026, 3, NULL, '신청자격 판단 결과 승인', 8016, 'modify',
 '결재 상태', 'IN_PROGRESS', 'COMPLETED', 'vitaedu-VE111', '2026-08-06 15:50:00'),
(8027, 3, NULL, '훈련운영계획서', 8073, 'create',
 NULL, NULL, NULL, 'vitaedu-VE101', '2026-08-08 09:40:00'),
(8028, 3, NULL, '개요서', 8082, 'create',
 NULL, NULL, NULL, 'vitaedu-VE102', '2026-08-10 11:00:00'),
(8029, 3, NULL, '개요서', 8082, 'modify',
 '제목', '개요서', '훈련과정개요서', 'vitaedu-VE102', '2026-08-12 14:10:00'),
(8030, 3, NULL, '자막 검수 결과서', 8058, 'create',
 NULL, NULL, NULL, 'vitaedu-VE107', '2026-08-11 17:00:00'),

-- ⭐⭐ 반려에서 재상신까지가 로그에 남는다
(8031, 3, NULL, '훈련운영계획서 제출 승인', 8075, 'create',
 NULL, NULL, NULL, 'vitaedu-VE101', '2026-08-12 18:50:00'),
(8032, 3, NULL, '훈련운영계획서 제출 승인', 8075, 'modify',
 '결재 상태', 'DRAFT', 'IN_PROGRESS', 'vitaedu-VE101', '2026-08-13 09:20:00'),
(8033, 3, NULL, '훈련운영계획서 제출 승인', 8075, 'modify',
 '결재 상태', 'IN_PROGRESS', 'REJECTED', 'vitaedu-VE110', '2026-08-13 15:40:00'),
(8034, 3, NULL, '훈련운영계획서 제출 승인', 8075, 'modify',
 '결재 회차', '1', '2', 'vitaedu-VE101', '2026-08-16 10:10:00');


-- ── 2. 알림 3 ───────────────────────────────────────────────────────
-- ⚠️ 의류 데모 시절에는 읽음 컬럼이 없어 전부 넣으면 벨에 영구히 쌓였다.
--    지금은 `read_at` 이 있다 (2026-08-16 DESC 로 확인). 그래도 3건만 넣는다 —
--    시연에서 볼 건 세 개면 충분하고, 벨에 숫자가 크게 뜨면 화면이 지저분하다.
INSERT IGNORE INTO notification
  (notification_id, target_type, target_id, target_context, user_id,
   notification_type, title, message, read_at, created_at) VALUES
(8001, 'ISSUE', 8011,
 JSON_OBJECT('projectId', 8001, 'stepId', 8007, 'projectName', '2026년 K-디지털 기초역량훈련 심사 신청'),
 'vitaedu-VE107', 'ISSUE_ASSIGNED', '이슈가 배정됐습니다',
 '7차시 자막 동기화 재검수 · 기한 2026-08-22', NULL, '2026-08-12 09:10:00'),
(8002, 'ISSUE', 8017,
 JSON_OBJECT('projectId', 8001, 'stepId', 8010, 'projectName', '2026년 K-디지털 기초역량훈련 심사 신청'),
 'vitaedu-VE102', 'ISSUE_ASSIGNED', '이슈가 배정됐습니다',
 '외부 교·강사 증빙 3건 수령 · 기한 2026-08-19', NULL, '2026-08-14 10:20:00'),
(8003, 'ISSUE', 8044,
 JSON_OBJECT('projectId', 8002, 'stepId', 8025, 'projectName', 'AI 도구 활용 업무 자동화 입문 과정 운영'),
 'vitaedu-VE109', 'ISSUE_ASSIGNED', '이슈가 배정됐습니다',
 '3차 훈련비 입금 확인 · 기한 2026-08-25', NULL, '2026-08-05 10:15:00');


-- =====================================================================
-- 검증
-- =====================================================================
-- 1) act 가 전부 소문자인가 (0행이어야 정상 — 아니면 애초에 INSERT 가 실패했을 것이다)
--    SELECT activity_log_id FROM activity_log
--    WHERE activity_log_id BETWEEN 8001 AND 8034 AND BINARY act <> LOWER(act);
--
-- 2) resource_name 이 전건 차 있나 (0행이어야 정상)
--    SELECT activity_log_id FROM activity_log
--    WHERE activity_log_id BETWEEN 8001 AND 8034
--      AND (resource_name IS NULL OR resource_name = '');
--
-- 3) modify 인데 변경 내역이 비었나 (0행이어야 정상)
--    SELECT activity_log_id FROM activity_log
--    WHERE activity_log_id BETWEEN 8001 AND 8034 AND act = 'modify'
--      AND (field IS NULL OR before_value IS NULL OR after_value IS NULL);
--
-- 4) ⭐ 삭제된 블록의 로그가 살아 있나 (4행이어야 정상 — create 2 · delete 2)
--    SELECT l.activity_log_id, l.act, l.resource_name FROM activity_log l
--    JOIN block b ON b.block_id = l.block_id
--    WHERE b.deleted_at IS NOT NULL AND l.company_id = 3;
--
-- 5) ⭐ 이름 스냅샷 — 8019 의 resource_name 이 옛 이름「3차 훈련비」인가
--    SELECT activity_log_id, resource_name, before_value, after_value
--    FROM activity_log WHERE activity_log_id IN (8018, 8019);
--    FK 만 들었으면 과거 로그가 전부 새 이름으로 표시돼 거짓이 된다. 그래서 스냅샷을 남긴다.
--
-- 6) 활동기록이 시간순으로 흐르나 (눈으로 본다)
--    SELECT created_at, act, resource_name, field, before_value, after_value, user_id
--    FROM activity_log WHERE company_id = 3 ORDER BY created_at;
