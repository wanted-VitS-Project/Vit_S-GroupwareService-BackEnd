-- =====================================================================
-- KB 20. 활동 로그 27 · 알림 3 · 삭제 시연 블록 2   ⛔ 반드시 맨 마지막
-- 🚨 act 는 소문자 enum(create·modify·delete). block_id NOT NULL. resource_name 전건 채움.
-- 🚨 activity_log 는 deleted_at 이 없다 — 되돌릴 수단이 없다. 다른 시드 확정 후 마지막에.
-- ⚠️ 삭제 시연 블록 8421·8422 를 먼저 만들고 soft delete 한다 (block_id NOT NULL 을 만족시키려면 행이 있어야 한다).
-- 되돌리기: DELETE FROM notification WHERE notification_id BETWEEN 8004 AND 8006;
--           DELETE FROM activity_log WHERE activity_log_id BETWEEN 8035 AND 8061;
--           DELETE FROM block WHERE block_id BETWEEN 8421 AND 8422;
-- =====================================================================


-- 삭제 시연 블록 2 — 품질검토 스텝(8057)에서 체크리스트를 합친 흔적. 생성 후 soft delete.
INSERT IGNORE INTO block
  (block_id, step_id, title, type, type_id, owner, row_index, col_span, sort_order, created_by, deleted_at) VALUES
(8421, 8057, '제출 전 확인 (구)', 'CHECKLIST', NULL, 'vitaedu-VE103', 3, 1, 8, 'vitaedu-VE103', '2025-11-27 11:20:00'),
(8422, 8057, '본문 대조 확인 (구)', 'CHECKLIST', NULL, 'vitaedu-VE103', 3, 1, 9, 'vitaedu-VE103', '2025-11-27 11:22:00');


INSERT IGNORE INTO activity_log
  (activity_log_id, company_id, resource_id, resource_name, block_id, act,
   field, before_value, after_value, user_id, created_at) VALUES
(8035, 3, NULL, 'KB 디지털 위탁교육 공고', 8300, 'create', NULL, NULL, NULL, 'vitaedu-VE101', '2025-11-20 10:30:00'),
(8036, 3, NULL, '참가자격 검토 승인', 8312, 'create', NULL, NULL, NULL, 'vitaedu-VE103', '2025-11-22 10:00:00'),
(8037, 3, NULL, '제안 전략·커리큘럼 승인', 8319, 'create', NULL, NULL, NULL, 'vitaedu-VE106', '2025-11-24 10:00:00'),
(8038, 3, NULL, '제안서 본문', 8338, 'create', NULL, NULL, NULL, 'vitaedu-VE102', '2025-11-26 15:00:00'),
(8039, 3, NULL, '가격제안서 별지3', 8352, 'create', NULL, NULL, NULL, 'vitaedu-VE109', '2025-11-26 18:00:00'),
(8040, 3, NULL, '제안서 제출 최종 승인', 8369, 'create', NULL, NULL, NULL, 'vitaedu-VE101', '2025-11-27 16:00:00'),
(8041, 3, NULL, '위탁교육 계약서', 8381, 'create', NULL, NULL, NULL, 'vitaedu-VE109', '2026-02-05 15:00:00'),
(8042, 3, NULL, '1차 위탁료 정산 (상반기 STEP I)', 8405, 'create', NULL, NULL, NULL, 'vitaedu-VE109', '2026-07-05 09:00:00'),
(8043, 3, NULL, '2차 위탁료 정산 (상반기 STEP II)', 8406, 'create', NULL, NULL, NULL, 'vitaedu-VE109', '2026-07-05 09:01:00'),
(8044, 3, NULL, '3차 위탁료 정산 (상반기 사이버)', 8407, 'create', NULL, NULL, NULL, 'vitaedu-VE109', '2026-08-05 09:00:00'),
(8045, 3, NULL, '전문강사료 상반기 집합', 8414, 'create', NULL, NULL, NULL, 'vitaedu-VE109', '2026-02-25 10:00:00'),
(8046, 3, NULL, '교재·콘텐츠 개발 선금', 8415, 'create', NULL, NULL, NULL, 'vitaedu-VE109', '2026-02-25 10:01:00'),
(8047, 3, NULL, 'LMS 운영 지원', 8417, 'create', NULL, NULL, NULL, 'vitaedu-VE109', '2026-02-25 10:03:00'),
(8048, 3, NULL, '제안서 본문', 8338, 'modify', '버전', 'v1', 'v2', 'vitaedu-VE102', '2025-11-27 10:30:00'),
(8049, 3, NULL, '제안서 본문', 8338, 'modify', '버전', 'v3', 'v4', 'vitaedu-VE102', '2025-11-28 09:00:00'),
(8050, 3, NULL, '가격제안서 별지3', 8352, 'modify', '버전', 'v2', 'v3', 'vitaedu-VE109', '2025-11-27 17:30:00'),
(8051, 3, NULL, '1차 위탁료 정산 (상반기 STEP I)', 8405, 'modify', '정산 상태', 'WAITING', 'COMPLETED', 'vitaedu-VE109', '2026-07-15 10:35:00'),
(8052, 3, NULL, '2차 위탁료 정산 (상반기 STEP II)', 8406, 'modify', '정산 상태', 'WAITING', 'COMPLETED', 'vitaedu-VE109', '2026-07-15 10:37:00'),
(8053, 3, NULL, '전문강사료 상반기 집합', 8414, 'modify', '지급 상태', 'WAITING', 'COMPLETED', 'vitaedu-VE109', '2026-07-10 14:10:00'),
(8054, 3, NULL, '교재·콘텐츠 개발 선금', 8415, 'modify', '지급 상태', 'WAITING', 'COMPLETED', 'vitaedu-VE109', '2026-03-05 11:30:00'),
(8055, 3, NULL, 'LMS 운영 지원', 8417, 'modify', '지급 상태', 'WAITING', 'PARTIAL', 'vitaedu-VE109', '2026-04-10 15:20:00'),
(8056, 3, NULL, '상반기 집합과정 운영', 8384, 'modify', '내용', 'STEP I 진행', 'STEP II 만족도 반영', 'vitaedu-VE105', '2026-08-14 17:00:00'),
(8057, 3, NULL, '제안서 제출 최종 승인', 8369, 'modify', '결재 상태', 'ACTIVE', 'REJECTED', 'vitaedu-VE110', '2025-11-27 17:40:00'),
(8058, 3, NULL, '제안서 제출 최종 승인', 8369, 'modify', '회차', '1', '2', 'vitaedu-VE101', '2025-11-28 09:30:00'),
(8059, 3, NULL, '1차 위탁료 정산', 8405, 'modify', '제목', '1차 위탁료 정산', '1차 위탁료 정산 (상반기 STEP I)', 'vitaedu-VE109', '2026-07-06 09:00:00'),
(8060, 3, NULL, '제출 전 확인 (구)', 8421, 'delete', NULL, NULL, NULL, 'vitaedu-VE103', '2025-11-27 11:20:00'),
(8061, 3, NULL, '본문 대조 확인 (구)', 8422, 'delete', NULL, NULL, NULL, 'vitaedu-VE103', '2025-11-27 11:22:00');


INSERT IGNORE INTO notification
  (notification_id, target_type, target_id, target_context, user_id,
   notification_type, title, message, read_at, created_at) VALUES
(8004, 'ISSUE', 8082,
 JSON_OBJECT('projectId', 8011, 'stepId', 8063, 'projectName', '2026년 KB국민은행 디지털 분야 위탁교육 제안·운영'),
 'vitaedu-VE105', 'ISSUE_ASSIGNED', '이슈가 배정됐습니다',
 'STEP II 만족도 미달 3인 재수강 배정 · 기한 2026-09-05', NULL, '2026-08-25 09:10:00'),
(8005, 'ISSUE', 8084,
 JSON_OBJECT('projectId', 8011, 'stepId', 8065, 'projectName', '2026년 KB국민은행 디지털 분야 위탁교육 제안·운영'),
 'vitaedu-VE109', 'ISSUE_ASSIGNED', '이슈가 배정됐습니다',
 'LMS 운영 지원 잔금 지급조건 확정 · 기한 2026-09-30', NULL, '2026-08-01 09:15:00'),
(8006, 'ISSUE', 8085,
 JSON_OBJECT('projectId', 8011, 'stepId', 8066, 'projectName', '2026년 KB국민은행 디지털 분야 위탁교육 제안·운영'),
 'vitaedu-VE109', 'ISSUE_ASSIGNED', '이슈가 배정됐습니다',
 '3차 위탁료 입금 확인 · 기한 2026-08-25', NULL, '2026-08-05 10:15:00');
