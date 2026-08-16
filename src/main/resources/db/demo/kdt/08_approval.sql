-- =====================================================================
-- KDT 08. 결재 43 · 회차 44 · 결재선 86 · 대상문서 14
-- ---------------------------------------------------------------------
-- 무엇: APPROVAL 블록마다 결재 한 건과 그 회차·결재선·대상 문서를 만든다.
-- 왜:   결재 화면이 이 제품의 얼굴인데 데이터가 없으면 빈 목록이다.
--
-- 선행: 04_blocks.sql (APPROVAL 블록 43개) · 05_files.sql (file_version)
--
-- 🚨 목록 쿼리가 approval → block → step → project INNER JOIN 이다.
--    블록 없는 결재는 만들어도 목록에 안 뜬다. 그래서 블록 수가 곧 결재 수다.
--
-- 🚨 목록에 뜨는 조건은 딱 두 가지다. 프로젝트 참여자라는 사실은 아무 상관이 없다.
--      scope=drafted (기본) — approval.user_id 가 나 (또는 acting_drafter_id)
--      scope=pending        — 현재 회차의 내 approval_line.status 가 ACTIVE
--      scope=all            — MASTER 전용. 그 외는 403
--
-- ⭐ COMPLETED 만 잔뜩 만들면 모든 계정의 결재 대기 탭이 0이 된다.
--    그래서 진행 중 스텝의 결재 11건에 서로 다른 11명의 ACTIVE 를 하나씩 뿌렸다.
--    (결재 1건에 ACTIVE 는 1명이다)
--
-- ⚠️ 결재 상태는 스텝 상태와 맞춰야 한다. 생성 시 검사했다.
--      DONE 스텝        → COMPLETED · 전원 APPROVED
--      IN_PROGRESS 스텝 → IN_PROGRESS · 앞 단계 APPROVED, 현재 단계 ACTIVE
--      NOT_STARTED 스텝 → DRAFT · 전원 DRAFT · submitted_at 은 NULL
--
-- ⛔ ADMIN(VE112·VE113)은 결재선에도 기안자에도 없다.
--    `.ai/api/approval.md` 상 ADMIN 은 모든 범위에서 결재 권한이 없다. 넣으면 데이터가 죽는다.
--
-- ⚠️ 대표(VE111) 기안 3건은 acting_drafter_id 를 쓴다.
--    협약과 최종 제출, 프로젝트 종결은 대표 명의로 나가되 작성은 실무자가 한다.
--    대표가 직접 문서를 쓰는 걸로 두면 조직이 이상해 보인다.
--
-- ⭐⭐ approval 8007「훈련운영계획서 제출 승인」이 이 파일의 축이다.
--    rev1 REJECTED (8월 13일 · 실습과제 편성 비율 지적) → rev2 IN_PROGRESS 재상신
--    대상 문서가 v1 에서 v4 로 바뀌어 회차 화면과 버전 고정이 한 자리에서 보인다.
--
-- 되돌리기:
--   DELETE FROM approval_document WHERE approval_document_id BETWEEN 8001 AND 8014;
--   DELETE FROM approval_line     WHERE approval_line_id     BETWEEN 8001 AND 8086;
--   DELETE FROM approval_revision WHERE approval_revision_id BETWEEN 8001 AND 8044;
--   DELETE FROM approval          WHERE approval_id BETWEEN 8001 AND 8043;
-- =====================================================================


-- ── 1. 결재 ─────────────────────────────────────────────────────────
-- ⚠️ block_id 는 UNIQUE 다. 한 블록에 결재 두 건은 못 붙는다.
-- ⚠️ 회사 격리는 기안자 소속으로 판정한다 (drafter.company_id).
INSERT IGNORE INTO approval
  (approval_id, block_id, user_id, acting_drafter_id, status, current_revision_no, completed_at) VALUES
(8001, 8016, 'vitaedu-VE103', NULL, 'COMPLETED', 1, '2026-08-06 15:50:00'),
(8002, 8024, 'vitaedu-VE106', NULL, 'COMPLETED', 1, '2026-08-07 17:30:00'),
(8003, 8032, 'vitaedu-VE102', NULL, 'COMPLETED', 1, '2026-08-11 15:10:00'),
(8004, 8040, 'vitaedu-VE107', NULL, 'COMPLETED', 1, '2026-08-12 16:40:00'),
(8005, 8049, 'vitaedu-VE107', NULL, 'IN_PROGRESS', 1, NULL),
(8006, 8066, 'vitaedu-VE108', NULL, 'COMPLETED', 1, '2026-08-15 13:40:00'),
(8007, 8075, 'vitaedu-VE101', NULL, 'IN_PROGRESS', 2, NULL),
(8008, 8084, 'vitaedu-VE102', NULL, 'IN_PROGRESS', 1, NULL),
(8009, 8092, 'vitaedu-VE111', 'vitaedu-VE102', 'DRAFT', 1, NULL),
(8010, 8099, 'vitaedu-VE111', 'vitaedu-VE101', 'DRAFT', 1, NULL),
(8011, 8109, 'vitaedu-VE109', NULL, 'COMPLETED', 1, '2026-07-08 15:20:00'),
(8012, 8117, 'vitaedu-VE107', NULL, 'IN_PROGRESS', 1, NULL),
(8013, 8125, 'vitaedu-VE109', NULL, 'COMPLETED', 1, '2026-08-05 14:50:00'),
(8014, 8146, 'vitaedu-VE104', NULL, 'COMPLETED', 1, '2025-12-18 16:00:00'),
(8015, 8169, 'vitaedu-VE105', NULL, 'COMPLETED', 1, '2026-03-27 15:00:00'),
(8016, 8177, 'vitaedu-VE107', NULL, 'IN_PROGRESS', 1, NULL),
(8017, 8184, 'vitaedu-VE104', NULL, 'IN_PROGRESS', 1, NULL),
(8018, 8194, 'vitaedu-VE109', NULL, 'IN_PROGRESS', 1, NULL),
(8019, 8195, 'vitaedu-VE109', NULL, 'IN_PROGRESS', 1, NULL),
(8020, 8203, 'vitaedu-VE101', NULL, 'COMPLETED', 1, '2025-08-08 16:00:00'),
(8021, 8205, 'vitaedu-VE101', NULL, 'COMPLETED', 1, '2025-08-26 15:00:00'),
(8022, 8207, 'vitaedu-VE110', NULL, 'COMPLETED', 1, '2025-11-28 14:30:00'),
(8023, 8209, 'vitaedu-VE106', NULL, 'COMPLETED', 1, '2026-03-06 17:00:00'),
(8024, 8211, 'vitaedu-VE103', NULL, 'COMPLETED', 1, '2026-03-27 16:00:00'),
(8025, 8213, 'vitaedu-VE111', 'vitaedu-VE103', 'COMPLETED', 1, '2026-05-20 16:30:00'),
(8026, 8215, 'vitaedu-VE104', NULL, 'COMPLETED', 1, '2025-09-26 16:00:00'),
(8027, 8217, 'vitaedu-VE102', NULL, 'COMPLETED', 1, '2025-10-24 17:00:00'),
(8028, 8219, 'vitaedu-VE110', NULL, 'COMPLETED', 1, '2026-01-29 15:00:00'),
(8029, 8221, 'vitaedu-VE101', NULL, 'COMPLETED', 1, '2026-08-14 16:00:00'),
(8030, 8223, 'vitaedu-VE103', NULL, 'IN_PROGRESS', 1, NULL),
(8031, 8225, 'vitaedu-VE109', NULL, 'DRAFT', 1, NULL),
(8032, 8227, 'vitaedu-VE105', NULL, 'COMPLETED', 1, '2025-03-28 16:00:00'),
(8033, 8229, 'vitaedu-VE105', NULL, 'COMPLETED', 1, '2025-11-28 17:00:00'),
(8034, 8231, 'vitaedu-VE109', NULL, 'COMPLETED', 1, '2025-12-19 15:00:00'),
(8035, 8233, 'vitaedu-VE108', NULL, 'COMPLETED', 1, '2026-05-29 17:00:00'),
(8036, 8235, 'vitaedu-VE101', NULL, 'IN_PROGRESS', 1, NULL),
(8037, 8237, 'vitaedu-VE108', NULL, 'DRAFT', 1, NULL),
(8038, 8239, 'vitaedu-VE103', NULL, 'DRAFT', 1, NULL),
(8039, 8241, 'vitaedu-VE108', NULL, 'DRAFT', 1, NULL),
(8040, 8243, 'vitaedu-VE103', NULL, 'DRAFT', 1, NULL),
(8041, 8245, 'vitaedu-VE110', NULL, 'COMPLETED', 1, '2026-06-19 16:00:00'),
(8042, 8247, 'vitaedu-VE106', NULL, 'IN_PROGRESS', 1, NULL),
(8043, 8249, 'vitaedu-VE107', NULL, 'DRAFT', 1, NULL);


-- ── 2. 회차 ─────────────────────────────────────────────────────────
-- ⚠️ approval.current_revision_no 가 가리키는 회차가 반드시 있어야 한다.
--    없으면 상세 화면이 빈다. 8007 만 2회차이고 나머지는 1회차다.
INSERT IGNORE INTO approval_revision
  (approval_revision_id, approval_id, revision_no, title, content, status, submitted_at, finished_at) VALUES
(8001, 8001, 1, '신청자격 판단 결과 승인', '세 번째 신청자격으로 간다. 수료 941명으로 300명 기준을 넘겼고 준법성과 재정건전성도 걸리는 게 없다.', 'COMPLETED', '2026-08-06 10:20:00', '2026-08-06 15:50:00'),
(8002, 8002, 1, '훈련유형·과정 기획 승인', '기존 과정과 유형이 겹치지 않도록 Pre-KDT 로 간다. 21개 분야 중 인공지능에 해당한다.', 'COMPLETED', '2026-08-07 11:00:00', '2026-08-07 17:30:00'),
(8003, 8003, 1, '훈련시간·수강료 산정 승인', '40차시 40시간으로 수강료 484,000원이다. Pre-KDT 상한 50만원 안에 든다.', 'COMPLETED', '2026-08-11 10:30:00', '2026-08-11 15:10:00'),
(8004, 8004, 1, '실습과제·프로젝트 편성 승인', '실습과제 6개와 프로젝트 1개로 편성시간 14시간이다. 총 훈련시간의 35퍼센트다.', 'COMPLETED', '2026-08-12 11:20:00', '2026-08-12 16:40:00'),
(8005, 8005, 1, '9차시 납기 연장 승인', 'RAG 와 오픈 API 9차시 납기를 8월 22일로 미룬다. 계약상 지연배상 대상은 아니다.', 'IN_PROGRESS', '2026-08-14 09:40:00', NULL),
(8006, 8006, 1, 'LMS 심사계정 개방 승인', '심사기간에 중복 로그인과 순차 학습 제한을 푼다. 심사용 계정 3개를 개요서에 적는다.', 'COMPLETED', '2026-08-15 10:00:00', '2026-08-15 13:40:00'),
(8007, 8007, 1, '훈련운영계획서 제출 승인', '실습과제 편성 비율을 35퍼센트로 올리고 참여인력 증빙을 보강했다. 자막 자체점검 결과도 반영했다.', 'REJECTED', '2026-08-13 09:20:00', '2026-08-13 15:40:00'),
(8008, 8007, 2, '훈련운영계획서 제출 승인', '실습과제 편성 비율을 35퍼센트로 올리고 참여인력 증빙을 보강했다. 자막 자체점검 결과도 반영했다.', 'IN_PROGRESS', '2026-08-16 10:10:00', NULL),
(8009, 8008, 1, '참여인력 목록 확정 승인', '참여인력 9명 중 6명 증빙을 받았다. 외부 교·강사 3명은 19일까지 못 받으면 목록에서 뺀다.', 'IN_PROGRESS', '2026-08-15 18:30:00', NULL),
(8010, 8009, 1, '협약 체결 승인', '참여기관 연계 가점을 받기 위한 협약이다. 참여기관 역할은 실습과제 검토와 현업 사례 제공이다.', 'DRAFT', NULL, NULL),
(8011, 8010, 1, 'HRD-Net 최종 제출 승인', 'HRD-Net 2026년 30회차로 제출한다. 24일에 올리고 25일에는 확인만 한다.', 'DRAFT', NULL, NULL),
(8012, 8011, 1, '외주 계약 체결 승인', '콘텐츠 개발 66,000,000원, 자막 8,800,000원, 촬영 5,500,000원이다. 저작재산권은 전부 우리에게 넘어온다.', 'COMPLETED', '2026-07-07 14:00:00', '2026-07-08 15:20:00'),
(8013, 8012, 1, '콘텐츠 개발 중도금 지급 승인', '20차시 납품과 검수를 확인했다. 계약상 중도금 30퍼센트인 19,800,000원을 지급한다.', 'IN_PROGRESS', '2026-08-10 09:30:00', NULL),
(8014, 8013, 1, '자막·촬영 용역비 지급 승인', '자막 선금과 촬영 용역비를 지급한다. 자막 잔금은 7차시 검수가 통과해야 나간다.', 'COMPLETED', '2026-08-05 11:00:00', '2026-08-05 14:50:00'),
(8015, 8014, 1, '과정 개설 신고 승인', '선정 후 3개월 안에 열어야 한다. 12월 19일 신고하고 1월 12일 개강한다.', 'COMPLETED', '2025-12-17 10:00:00', '2025-12-18 16:00:00'),
(8016, 8015, 1, '1기 수료 판정 승인', '358명 중 312명 수료다. 수료율 87.2퍼센트, 만족도 86.4점으로 추가 배정 기준을 넘겼다.', 'COMPLETED', '2026-03-26 11:00:00', '2026-03-27 15:00:00'),
(8017, 8016, 1, '2기 중간 점검 결과 승인', '11주차 평균 진도율 68퍼센트다. 같은 시점 1기 61퍼센트보다 높다.', 'IN_PROGRESS', '2026-08-13 14:00:00', NULL),
(8018, 8017, 1, '중도이탈 대응 방안 승인', '난이도는 보충 영상으로, 업무 과중은 학습 기한 연장으로 대응한다. 설치 문제는 원격 지원을 붙인다.', 'IN_PROGRESS', '2026-08-12 10:20:00', NULL),
(8019, 8018, 1, '3차 훈련비 청구 승인', '2026년 7월 수료 341명분 59,415,840원을 청구한다. 입금 예정은 8월 25일이다.', 'IN_PROGRESS', '2026-08-05 09:40:00', NULL),
(8020, 8019, 1, '환수 조정분 회차 배정 승인', '소급 확인된 중도이탈자 10명분 1,742,400원이 들어왔다. 어느 회차에 붙일지 판단이 필요하다.', 'IN_PROGRESS', '2026-08-13 10:10:00', NULL),
(8021, 8020, 1, '신청 요건 검토 승인', '작년에도 세 번째 요건으로 신청했다.', 'COMPLETED', '2025-08-07 10:00:00', '2025-08-08 16:00:00'),
(8022, 8021, 1, '운영계획서 제출 승인', '운영계획서와 개요서를 제출한다.', 'COMPLETED', '2025-08-25 11:00:00', '2025-08-26 15:00:00'),
(8023, 8022, 1, '심사 결과 보고 승인', '총점 71점으로 선정됐다. 조건부 항목은 없다.', 'COMPLETED', '2025-11-27 14:00:00', '2025-11-28 14:30:00'),
(8024, 8023, 1, 'KDT 과정 설계 승인', '6개월 장기과정으로 프로젝트 비중을 40퍼센트로 잡았다.', 'COMPLETED', '2026-03-05 10:00:00', '2026-03-06 17:00:00'),
(8025, 8024, 1, 'KDT 신청서류 제출 승인', '기업 참여 확약서 2건을 함께 낸다.', 'COMPLETED', '2026-03-26 10:00:00', '2026-03-27 16:00:00'),
(8026, 8025, 1, '프로젝트 종결 승인', '운영역량 영역에서 기준점에 미달했다. 전담 인력을 늘린 뒤 2027년에 다시 낸다.', 'COMPLETED', '2026-05-19 15:00:00', '2026-05-20 16:30:00'),
(8027, 8026, 1, '대상 과정 선정 승인', '자체 개발 원격과정 3건을 인정 신청 대상으로 고른다.', 'COMPLETED', '2025-09-25 10:00:00', '2025-09-26 16:00:00'),
(8028, 8027, 1, '인정 신청서 제출 승인', '과정별 훈련시간과 수강료를 각각 산정했다.', 'COMPLETED', '2025-10-23 11:00:00', '2025-10-24 17:00:00'),
(8029, 8028, 1, '보완 제출본 승인', '차시 구성 보완 요구를 받은 1건을 고쳐 다시 낸다.', 'COMPLETED', '2026-01-28 10:00:00', '2026-01-29 15:00:00'),
(8030, 8029, 1, '참여 여부 결정 승인', '기술평가 80점 가격평가 20점이다. 지역 제한이 있어 지사 주소로 참여한다.', 'COMPLETED', '2026-08-13 14:00:00', '2026-08-14 16:00:00'),
(8031, 8030, 1, '제안서 상신', '기술 부문 초안이 나왔다. 8월 19일까지 마무리한다.', 'IN_PROGRESS', '2026-08-16 11:00:00', NULL),
(8032, 8031, 1, '제안서 최종 제출 승인', '8월 20일 10시 마감이다. 19일에 올려둔다.', 'DRAFT', NULL, NULL),
(8033, 8032, 1, '교육 과정 설계 승인', '재직자 대상 8주 과정으로 짰다.', 'COMPLETED', '2025-03-27 10:00:00', '2025-03-28 16:00:00'),
(8034, 8033, 1, '기수별 운영 결과 보고 승인', '3개 기수 합쳐 실시 684명, 수료 612명이다.', 'COMPLETED', '2025-11-27 11:00:00', '2025-11-28 17:00:00'),
(8035, 8034, 1, '정산 보고 승인', '회차 기준 계약이라 계약금액 전액을 청구한다.', 'COMPLETED', '2025-12-18 10:00:00', '2025-12-19 15:00:00'),
(8036, 8035, 1, '요구사항 확정 승인', '진도율 산정 로직과 평가 결과 확인 화면 두 가지다.', 'COMPLETED', '2026-05-28 10:00:00', '2026-05-29 17:00:00'),
(8037, 8036, 1, '개발 중간 검수 승인', '진도율 로직은 검수가 끝났다. 평가 결과 화면은 9월 11일 예정이다.', 'IN_PROGRESS', '2026-08-14 10:00:00', NULL),
(8038, 8037, 1, '검수 계획 승인', '심사용 계정으로 실제 수강 흐름을 돌려 확인한다.', 'DRAFT', NULL, NULL),
(8039, 8038, 1, '인증평가 신청 계획 승인', '인증평가 지표를 받아 현재 상태와 대조한다.', 'DRAFT', NULL, NULL),
(8040, 8039, 1, '증빙 정비 계획 승인', '훈련 운영 기록과 시설 장비 대장이 주 대상이다.', 'DRAFT', NULL, NULL),
(8041, 8040, 1, '현장평가 준비 계획 승인', '현장평가는 2027년 1월 이후다.', 'DRAFT', NULL, NULL),
(8042, 8041, 1, '변경 대상 차시 확정 승인', '16차시 중 5차시다. 다루는 도구의 화면이 바뀌어 실습 절차가 안 맞는다.', 'COMPLETED', '2026-06-18 10:00:00', '2026-06-19 16:00:00'),
(8043, 8042, 1, '재제작분 중간 검수 승인', '5차시 중 3차시 촬영이 끝났다. 자막은 재제작분만 다시 뽑는다.', 'IN_PROGRESS', '2026-08-14 15:00:00', NULL),
(8044, 8043, 1, '변경심사 신청 승인', '승인받은 훈련내용을 바꾸려면 변경심사를 내야 한다. 9월 안에 낸다.', 'DRAFT', NULL, NULL);


-- ── 3. 결재선 ───────────────────────────────────────────────────────
-- 🚨 결재선은 approval 이 아니라 approval_revision 에 붙는다.
--    회차가 바뀌면 결재선도 새로 생긴다 — 8007 은 결재선이 두 벌이다.
-- ⚠️ 기안자는 자기 결재선에 들어가지 않는다. 결재선 안에 같은 사람이 두 번 나와서도 안 된다.
--    생성 시 둘 다 검사했다.
INSERT IGNORE INTO approval_line
  (approval_line_id, user_id, approval_revision_id, sequence_no, status, opinion, processed_at) VALUES
(8001, 'vitaedu-VE110', 8001, 1, 'APPROVED', NULL, '2026-08-06 15:50:00'),
(8002, 'vitaedu-VE111', 8001, 2, 'APPROVED', NULL, '2026-08-06 15:50:00'),
(8003, 'vitaedu-VE103', 8002, 1, 'APPROVED', NULL, '2026-08-07 17:30:00'),
(8004, 'vitaedu-VE110', 8002, 2, 'APPROVED', NULL, '2026-08-07 17:30:00'),
(8005, 'vitaedu-VE106', 8003, 1, 'APPROVED', NULL, '2026-08-11 15:10:00'),
(8006, 'vitaedu-VE110', 8003, 2, 'APPROVED', NULL, '2026-08-11 15:10:00'),
(8007, 'vitaedu-VE106', 8004, 1, 'APPROVED', NULL, '2026-08-12 16:40:00'),
(8008, 'vitaedu-VE103', 8004, 2, 'APPROVED', NULL, '2026-08-12 16:40:00'),
(8009, 'vitaedu-VE106', 8005, 1, 'ACTIVE', NULL, NULL),
(8010, 'vitaedu-VE110', 8005, 2, 'WAITING', NULL, NULL),
(8011, 'vitaedu-VE103', 8006, 1, 'APPROVED', NULL, '2026-08-15 13:40:00'),
(8012, 'vitaedu-VE110', 8006, 2, 'APPROVED', NULL, '2026-08-15 13:40:00'),
(8013, 'vitaedu-VE103', 8007, 1, 'APPROVED', '실습과제 편성은 콘텐츠개발팀 확인을 거쳤다', '2026-08-13 11:10:00'),
(8014, 'vitaedu-VE110', 8007, 2, 'REJECTED', '실습과제 편성 비율이 배점 대비 낮다. 35퍼센트 수준으로 올려 다시 올려 달라', '2026-08-13 15:40:00'),
(8015, 'vitaedu-VE111', 8007, 3, 'WAITING', NULL, NULL),
(8016, 'vitaedu-VE103', 8008, 1, 'APPROVED', '지적사항 반영을 확인했다', '2026-08-16 11:30:00'),
(8017, 'vitaedu-VE110', 8008, 2, 'ACTIVE', NULL, NULL),
(8018, 'vitaedu-VE111', 8008, 3, 'WAITING', NULL, NULL),
(8019, 'vitaedu-VE104', 8009, 1, 'ACTIVE', NULL, NULL),
(8020, 'vitaedu-VE110', 8009, 2, 'WAITING', NULL, NULL),
(8021, 'vitaedu-VE103', 8010, 1, 'DRAFT', NULL, NULL),
(8022, 'vitaedu-VE110', 8010, 2, 'DRAFT', NULL, NULL),
(8023, 'vitaedu-VE103', 8011, 1, 'DRAFT', NULL, NULL),
(8024, 'vitaedu-VE110', 8011, 2, 'DRAFT', NULL, NULL),
(8025, 'vitaedu-VE110', 8012, 1, 'APPROVED', NULL, '2026-07-08 15:20:00'),
(8026, 'vitaedu-VE111', 8012, 2, 'APPROVED', NULL, '2026-07-08 15:20:00'),
(8027, 'vitaedu-VE109', 8013, 1, 'ACTIVE', NULL, NULL),
(8028, 'vitaedu-VE110', 8013, 2, 'WAITING', NULL, NULL),
(8029, 'vitaedu-VE110', 8014, 1, 'APPROVED', NULL, '2026-08-05 14:50:00'),
(8030, 'vitaedu-VE111', 8014, 2, 'APPROVED', NULL, '2026-08-05 14:50:00'),
(8031, 'vitaedu-VE103', 8015, 1, 'APPROVED', NULL, '2025-12-18 16:00:00'),
(8032, 'vitaedu-VE110', 8015, 2, 'APPROVED', NULL, '2025-12-18 16:00:00'),
(8033, 'vitaedu-VE104', 8016, 1, 'APPROVED', NULL, '2026-03-27 15:00:00'),
(8034, 'vitaedu-VE110', 8016, 2, 'APPROVED', NULL, '2026-03-27 15:00:00'),
(8035, 'vitaedu-VE105', 8017, 1, 'ACTIVE', NULL, NULL),
(8036, 'vitaedu-VE110', 8017, 2, 'WAITING', NULL, NULL),
(8037, 'vitaedu-VE102', 8018, 1, 'ACTIVE', NULL, NULL),
(8038, 'vitaedu-VE110', 8018, 2, 'WAITING', NULL, NULL),
(8039, 'vitaedu-VE110', 8019, 1, 'APPROVED', '내용 확인했다', '2026-08-05 09:40:00'),
(8040, 'vitaedu-VE111', 8019, 2, 'ACTIVE', NULL, NULL),
(8041, 'vitaedu-VE103', 8020, 1, 'ACTIVE', NULL, NULL),
(8042, 'vitaedu-VE110', 8020, 2, 'WAITING', NULL, NULL),
(8043, 'vitaedu-VE103', 8021, 1, 'APPROVED', NULL, '2025-08-08 16:00:00'),
(8044, 'vitaedu-VE110', 8021, 2, 'APPROVED', NULL, '2025-08-08 16:00:00'),
(8045, 'vitaedu-VE103', 8022, 1, 'APPROVED', NULL, '2025-08-26 15:00:00'),
(8046, 'vitaedu-VE111', 8022, 2, 'APPROVED', NULL, '2025-08-26 15:00:00'),
(8047, 'vitaedu-VE111', 8023, 1, 'APPROVED', NULL, '2025-11-28 14:30:00'),
(8048, 'vitaedu-VE103', 8024, 1, 'APPROVED', NULL, '2026-03-06 17:00:00'),
(8049, 'vitaedu-VE110', 8024, 2, 'APPROVED', NULL, '2026-03-06 17:00:00'),
(8050, 'vitaedu-VE110', 8025, 1, 'APPROVED', NULL, '2026-03-27 16:00:00'),
(8051, 'vitaedu-VE111', 8025, 2, 'APPROVED', NULL, '2026-03-27 16:00:00'),
(8052, 'vitaedu-VE110', 8026, 1, 'APPROVED', NULL, '2026-05-20 16:30:00'),
(8053, 'vitaedu-VE102', 8027, 1, 'APPROVED', NULL, '2025-09-26 16:00:00'),
(8054, 'vitaedu-VE110', 8027, 2, 'APPROVED', NULL, '2025-09-26 16:00:00'),
(8055, 'vitaedu-VE104', 8028, 1, 'APPROVED', NULL, '2025-10-24 17:00:00'),
(8056, 'vitaedu-VE110', 8028, 2, 'APPROVED', NULL, '2025-10-24 17:00:00'),
(8057, 'vitaedu-VE111', 8029, 1, 'APPROVED', NULL, '2026-01-29 15:00:00'),
(8058, 'vitaedu-VE103', 8030, 1, 'APPROVED', NULL, '2026-08-14 16:00:00'),
(8059, 'vitaedu-VE111', 8030, 2, 'APPROVED', NULL, '2026-08-14 16:00:00'),
(8060, 'vitaedu-VE101', 8031, 1, 'ACTIVE', NULL, NULL),
(8061, 'vitaedu-VE111', 8031, 2, 'WAITING', NULL, NULL),
(8062, 'vitaedu-VE103', 8032, 1, 'DRAFT', NULL, NULL),
(8063, 'vitaedu-VE111', 8032, 2, 'DRAFT', NULL, NULL),
(8064, 'vitaedu-VE109', 8033, 1, 'APPROVED', NULL, '2025-03-28 16:00:00'),
(8065, 'vitaedu-VE111', 8033, 2, 'APPROVED', NULL, '2025-03-28 16:00:00'),
(8066, 'vitaedu-VE104', 8034, 1, 'APPROVED', NULL, '2025-11-28 17:00:00'),
(8067, 'vitaedu-VE109', 8034, 2, 'APPROVED', NULL, '2025-11-28 17:00:00'),
(8068, 'vitaedu-VE104', 8035, 1, 'APPROVED', NULL, '2025-12-19 15:00:00'),
(8069, 'vitaedu-VE111', 8035, 2, 'APPROVED', NULL, '2025-12-19 15:00:00'),
(8070, 'vitaedu-VE101', 8036, 1, 'APPROVED', NULL, '2026-05-29 17:00:00'),
(8071, 'vitaedu-VE111', 8036, 2, 'APPROVED', NULL, '2026-05-29 17:00:00'),
(8072, 'vitaedu-VE108', 8037, 1, 'ACTIVE', NULL, NULL),
(8073, 'vitaedu-VE111', 8037, 2, 'WAITING', NULL, NULL),
(8074, 'vitaedu-VE101', 8038, 1, 'DRAFT', NULL, NULL),
(8075, 'vitaedu-VE111', 8038, 2, 'DRAFT', NULL, NULL),
(8076, 'vitaedu-VE108', 8039, 1, 'DRAFT', NULL, NULL),
(8077, 'vitaedu-VE111', 8039, 2, 'DRAFT', NULL, NULL),
(8078, 'vitaedu-VE103', 8040, 1, 'DRAFT', NULL, NULL),
(8079, 'vitaedu-VE111', 8040, 2, 'DRAFT', NULL, NULL),
(8080, 'vitaedu-VE108', 8041, 1, 'DRAFT', NULL, NULL),
(8081, 'vitaedu-VE111', 8041, 2, 'DRAFT', NULL, NULL),
(8082, 'vitaedu-VE111', 8042, 1, 'APPROVED', NULL, '2026-06-19 16:00:00'),
(8083, 'vitaedu-VE107', 8043, 1, 'ACTIVE', NULL, NULL),
(8084, 'vitaedu-VE110', 8043, 2, 'WAITING', NULL, NULL),
(8085, 'vitaedu-VE106', 8044, 1, 'DRAFT', NULL, NULL),
(8086, 'vitaedu-VE110', 8044, 2, 'DRAFT', NULL, NULL);


-- ── 4. 결재 대상 문서 ───────────────────────────────────────────────
-- ⚠️ 대상은 파일이 아니라 **파일의 특정 버전**이다.
--    그래서 결재 뒤에 새 버전이 올라오면 「대상보다 새 버전 있음」이 뜬다.
--    8007 rev1 은 운영계획서 v1, rev2 는 v4 를 가리킨다.
--    협약서와 계약서는 v1(최종안)을 가리키고 v2(날인본)는 승인 뒤에 올라온 것이다.
INSERT IGNORE INTO approval_document (approval_document_id, approval_revision_id, file_version_id) VALUES
(8001, 8005, 8025),
(8002, 8007, 8007),
(8003, 8008, 8010),
(8004, 8009, 8015),
(8005, 8009, 8018),
(8006, 8010, 8022),
(8007, 8012, 8024),
(8008, 8012, 8028),
(8009, 8013, 8030),
(8010, 8013, 8031),
(8011, 8016, 8033),
(8012, 8016, 8034),
(8013, 8019, 8037),
(8014, 8020, 8040);


-- =====================================================================
-- 검증 — 전부 0행이어야 한다
-- =====================================================================
-- 1) 블록 없는 결재
--    SELECT a.approval_id FROM approval a LEFT JOIN block b ON b.block_id = a.block_id
--    WHERE a.approval_id BETWEEN 8001 AND 8043 AND b.block_id IS NULL;
--
-- 2) 현재 회차가 없는 결재
--    SELECT a.approval_id FROM approval a WHERE a.approval_id BETWEEN 8001 AND 8043
--      AND NOT EXISTS (SELECT 1 FROM approval_revision r
--                       WHERE r.approval_id = a.approval_id AND r.revision_no = a.current_revision_no);
--
-- 3) 기안자가 자기 결재선에
--    SELECT a.approval_id FROM approval a
--    JOIN approval_revision r ON r.approval_id = a.approval_id AND r.revision_no = a.current_revision_no
--    JOIN approval_line l ON l.approval_revision_id = r.approval_revision_id AND l.user_id = a.user_id
--    WHERE a.approval_id BETWEEN 8001 AND 8043;
--
-- 4) 결재선 내 동일인 중복
--    SELECT approval_revision_id, user_id FROM approval_line
--    WHERE approval_revision_id BETWEEN 8001 AND 8044 GROUP BY 1,2 HAVING COUNT(*) > 1;
--
-- 5) ACTIVE 없는 IN_PROGRESS 결재
--    SELECT a.approval_id FROM approval a
--    JOIN approval_revision r ON r.approval_id = a.approval_id AND r.revision_no = a.current_revision_no
--    WHERE a.approval_id BETWEEN 8001 AND 8043 AND a.status = 'IN_PROGRESS'
--      AND NOT EXISTS (SELECT 1 FROM approval_line l
--                       WHERE l.approval_revision_id = r.approval_revision_id AND l.status = 'ACTIVE');
--
-- 6) ⛔ ADMIN 이 결재선에
--    SELECT l.approval_line_id FROM approval_line l JOIN account ac ON ac.user_id = l.user_id
--    WHERE ac.role = 'ADMIN';
--
-- 7) ⭐ 계정별 기안 건수와 결재 대기 건수 — 11명 전원이 기안 3건 이상, 대기 1건 이상
--    SELECT e.user_id, e.name,
--           (SELECT COUNT(*) FROM approval a
--             WHERE a.user_id = e.user_id OR a.acting_drafter_id = e.user_id) AS 내가올린,
--           (SELECT COUNT(*) FROM approval_line l
--             JOIN approval_revision r ON r.approval_revision_id = l.approval_revision_id
--             JOIN approval a2 ON a2.approval_id = r.approval_id
--                             AND a2.current_revision_no = r.revision_no
--            WHERE l.user_id = e.user_id AND l.status = 'ACTIVE') AS 결재대기
--    FROM employee e WHERE e.company_id = 3 AND e.is_system = 0 ORDER BY e.user_id;
--
-- 8) 화면에서 실제로 보이는지는 API 로 확인한다. DB 카운트만으로는 역할 게이트를 못 잡는다.
--    GET /api/v1/approvals?scope=drafted&size=1
--    GET /api/v1/approvals?scope=pending&size=1
