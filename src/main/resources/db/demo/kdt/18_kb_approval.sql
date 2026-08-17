-- =====================================================================
-- KB 18. 결재 13 · 회차 · 결재선 · 대상문서
-- 🚨 결재 상태는 스텝 상태와 맞춘다. DONE→COMPLETED(전원 APPROVED) · IN_PROGRESS→ACTIVE 한 명.
-- ⭐ 8051 제안서 제출 최종 승인 — rev1 REJECTED(VOC 미달) → rev2 COMPLETED. current_revision_no=2.
-- ⭐ 8049 가격 제안 승인 — 대표(VE111) 명의, acting_drafter=VE109.
-- ⭐ ACTIVE 3건(8053·8055·8056) → 결재대기 탭이 채워진다.
-- ⚠️ 결재선은 approval_revision 에 붙는다. 기안자 본인·중복 금지.
-- 선행: 14_kb_blocks.sql(approval 블록 type_id) · 15_kb_files.sql(approval_document 의 file_version)
-- 되돌리기: DELETE FROM approval_document WHERE approval_document_id BETWEEN 8015 AND 8025;
--           DELETE FROM approval_line WHERE approval_line_id BETWEEN 8087 AND 8140;
--           DELETE FROM approval_revision WHERE approval_revision_id BETWEEN 8045 AND 8060;
--           DELETE FROM approval WHERE approval_id BETWEEN 8044 AND 8056;
-- =====================================================================


INSERT IGNORE INTO approval
  (approval_id, block_id, user_id, acting_drafter_id, status, current_revision_no, completed_at) VALUES
(8044, 8312, 'vitaedu-VE103', NULL, 'COMPLETED', 1, '2025-11-22 16:00:00'),
(8045, 8319, 'vitaedu-VE106', NULL, 'COMPLETED', 1, '2025-11-24 18:00:00'),
(8046, 8332, 'vitaedu-VE108', NULL, 'COMPLETED', 1, '2025-11-25 18:00:00'),
(8047, 8340, 'vitaedu-VE102', NULL, 'COMPLETED', 1, '2025-11-26 19:00:00'),
(8048, 8346, 'vitaedu-VE103', NULL, 'COMPLETED', 1, '2025-11-27 15:00:00'),
(8049, 8354, 'vitaedu-VE111', 'vitaedu-VE109', 'COMPLETED', 1, '2025-11-27 18:00:00'),
(8050, 8362, 'vitaedu-VE102', NULL, 'COMPLETED', 1, '2025-11-28 13:00:00'),
(8051, 8369, 'vitaedu-VE101', NULL, 'COMPLETED', 2, '2025-11-28 15:30:00'),
(8052, 8382, 'vitaedu-VE109', NULL, 'COMPLETED', 1, '2026-02-06 16:00:00'),
(8053, 8389, 'vitaedu-VE104', NULL, 'IN_PROGRESS', 1, NULL),
(8054, 8402, 'vitaedu-VE109', NULL, 'COMPLETED', 1, '2026-02-20 17:00:00'),
(8055, 8412, 'vitaedu-VE109', NULL, 'IN_PROGRESS', 1, NULL),
(8056, 8420, 'vitaedu-VE109', NULL, 'IN_PROGRESS', 1, NULL);


INSERT IGNORE INTO approval_revision
  (approval_revision_id, approval_id, revision_no, title, content, status, submitted_at, finished_at) VALUES
(8045, 8044, 1, '참가자격 검토 승인', '네 요건을 모두 충족한다. 세부 근거를 실적 집계와 재무제표로 붙였다.', 'COMPLETED', '2025-11-22 10:00:00', '2025-11-22 16:00:00'),
(8046, 8045, 1, '제안 전략·커리큘럼 승인', 'DT기획과 DT개발 STEP 구성과 일반 사이버 편성을 확정했다.', 'COMPLETED', '2025-11-24 10:00:00', '2025-11-24 18:00:00'),
(8047, 8046, 1, '기술·운영 역량 정리 승인', '학습이력 API와 학습진단 시스템, 성취도 측정 방안을 정리했다.', 'COMPLETED', '2025-11-25 11:00:00', '2025-11-25 18:00:00'),
(8048, 8047, 1, '제안서 본문 승인', '여섯 항목을 순서대로 작성했다. 요구사항 반영 여부를 항목마다 표기했다.', 'COMPLETED', '2025-11-26 15:00:00', '2025-11-26 19:00:00'),
(8049, 8048, 1, '요약본·품질 검토 승인', 'VOC 인력 현황을 3명으로 보강한 뒤 요약본을 확정했다.', 'COMPLETED', '2025-11-27 11:00:00', '2025-11-27 15:00:00'),
(8050, 8049, 1, '가격 제안 승인', '대표 명의로 가격제안서를 확정한다. 총액은 예산 범위 안이고 환급 비율은 40퍼센트를 넘긴다.', 'COMPLETED', '2025-11-27 14:00:00', '2025-11-27 18:00:00'),
(8051, 8050, 1, '제출서류 준비 확인', '별지 서식과 완납증명, 입찰보증금을 모두 준비했다.', 'COMPLETED', '2025-11-28 10:00:00', '2025-11-28 13:00:00'),
(8052, 8051, 1, '제안서 제출 최종 승인', '제안서와 요약본, 가격제안서, 별지 서식을 최종 제출한다.', 'REJECTED', '2025-11-27 16:00:00', '2025-11-27 17:40:00'),
(8053, 8051, 2, '제안서 제출 최종 승인', 'VOC 인력 현황을 3명으로 보강한 제안서 v4로 다시 올린다.', 'COMPLETED', '2025-11-28 09:30:00', '2025-11-28 15:30:00'),
(8054, 8052, 1, '계약 체결 승인', '위약은 사전 30일 통보로 수정했다. 저작권 공동귀속은 반영되지 않아 원안대로 체결한다.', 'COMPLETED', '2026-02-05 15:00:00', '2026-02-06 16:00:00'),
(8055, 8053, 1, '상반기 운영 결과 승인', 'STEP I 만족도 92퍼센트, STEP II는 미달자 3명 재수강 배정이다. 수료율은 두 과정 모두 90퍼센트를 넘겼다.', 'IN_PROGRESS', '2026-08-14 10:00:00', NULL),
(8056, 8054, 1, '외주 계약 승인', '강사와 콘텐츠, 촬영, LMS 4개 업체와 계약한다. LMS 잔금은 검수 통과 후 지급한다.', 'COMPLETED', '2026-02-19 14:00:00', '2026-02-20 17:00:00'),
(8057, 8055, 1, '3차 위탁료 정산 승인', '상반기 사이버 과정 16,500,000원을 청구한다. 입금 예정은 8월 25일이다.', 'IN_PROGRESS', '2026-08-05 09:40:00', NULL),
(8058, 8056, 1, '콘텐츠 개발 잔금 지급 승인', '교재 콘텐츠 잔금 13,200,000원이다. 검수 통과를 확인한 뒤 지급한다.', 'IN_PROGRESS', '2026-08-20 10:00:00', NULL);


-- 🚨 결재선은 approval_revision 에 붙는다. 회차가 바뀌면 새로 생긴다 (8051 은 두 벌).
INSERT IGNORE INTO approval_line
  (approval_line_id, user_id, approval_revision_id, sequence_no, status, opinion, processed_at) VALUES
(8087, 'vitaedu-VE110', 8045, 1, 'APPROVED', NULL, '2025-11-22 15:00:00'),
(8088, 'vitaedu-VE111', 8045, 2, 'APPROVED', NULL, '2025-11-22 16:00:00'),
(8089, 'vitaedu-VE110', 8046, 1, 'APPROVED', NULL, '2025-11-24 17:00:00'),
(8090, 'vitaedu-VE111', 8046, 2, 'APPROVED', NULL, '2025-11-24 18:00:00'),
(8091, 'vitaedu-VE106', 8047, 1, 'APPROVED', NULL, '2025-11-25 16:00:00'),
(8092, 'vitaedu-VE110', 8047, 2, 'APPROVED', NULL, '2025-11-25 18:00:00'),
(8093, 'vitaedu-VE103', 8048, 1, 'APPROVED', NULL, '2025-11-26 18:00:00'),
(8094, 'vitaedu-VE110', 8048, 2, 'APPROVED', NULL, '2025-11-26 19:00:00'),
(8095, 'vitaedu-VE110', 8049, 1, 'APPROVED', NULL, '2025-11-27 14:00:00'),
(8096, 'vitaedu-VE111', 8049, 2, 'APPROVED', NULL, '2025-11-27 15:00:00'),
(8097, 'vitaedu-VE103', 8050, 1, 'APPROVED', NULL, '2025-11-27 16:00:00'),
(8098, 'vitaedu-VE110', 8050, 2, 'APPROVED', NULL, '2025-11-27 18:00:00'),
(8099, 'vitaedu-VE103', 8051, 1, 'APPROVED', NULL, '2025-11-28 12:00:00'),
(8100, 'vitaedu-VE110', 8051, 2, 'APPROVED', NULL, '2025-11-28 13:00:00'),
(8101, 'vitaedu-VE103', 8052, 1, 'APPROVED', NULL, '2025-11-27 17:00:00'),
(8102, 'vitaedu-VE110', 8052, 2, 'REJECTED', '운영능력 항목 VOC 인력 현황이 배점 기준 3명에 미달한다. 보강 후 재상신 바란다', '2025-11-27 17:40:00'),
(8103, 'vitaedu-VE111', 8052, 3, 'WAITING', NULL, NULL),
(8104, 'vitaedu-VE103', 8053, 1, 'APPROVED', NULL, '2025-11-28 11:00:00'),
(8105, 'vitaedu-VE110', 8053, 2, 'APPROVED', NULL, '2025-11-28 14:00:00'),
(8106, 'vitaedu-VE111', 8053, 3, 'APPROVED', NULL, '2025-11-28 15:30:00'),
(8107, 'vitaedu-VE110', 8054, 1, 'APPROVED', NULL, '2026-02-06 15:00:00'),
(8108, 'vitaedu-VE111', 8054, 2, 'APPROVED', NULL, '2026-02-06 16:00:00'),
(8109, 'vitaedu-VE110', 8055, 1, 'ACTIVE', NULL, NULL),
(8110, 'vitaedu-VE111', 8055, 2, 'WAITING', NULL, NULL),
(8111, 'vitaedu-VE110', 8056, 1, 'APPROVED', NULL, '2026-02-20 16:00:00'),
(8112, 'vitaedu-VE111', 8056, 2, 'APPROVED', NULL, '2026-02-20 17:00:00'),
(8113, 'vitaedu-VE110', 8057, 1, 'ACTIVE', NULL, NULL),
(8114, 'vitaedu-VE111', 8057, 2, 'WAITING', NULL, NULL),
(8115, 'vitaedu-VE107', 8058, 1, 'ACTIVE', NULL, NULL),
(8116, 'vitaedu-VE110', 8058, 2, 'WAITING', NULL, NULL);


-- approval_document — 결재 대상 파일 버전 고정 (제안서 v4·v1, 가격제안서 v3, 계약서 v1)
INSERT IGNORE INTO approval_document (approval_document_id, approval_revision_id, file_version_id) VALUES
(8015, 8048, 8048),
(8016, 8050, 8052),
(8017, 8052, 8045),
(8018, 8053, 8048),
(8019, 8054, 8060),
(8020, 8056, 8060);
