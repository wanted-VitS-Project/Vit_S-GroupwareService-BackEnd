-- =====================================================================
-- 07. 결재 5건 — 상신 6회차 · 결재선 15 · 대상 문서 7
-- ---------------------------------------------------------------------
-- 무엇: approval / approval_revision / approval_line / approval_document (3층).
-- 왜:   "이 파일 v4 가 그 결재의 대상이었다" 를 적을 자리가 우리에게만 있다는 게
--       발표의 핵심 주장이다. approval_document.file_version_id 가 그 자리다.
--
-- 선행: 03_blocks.sql (block.type_id 가 approval_id 9001~9005 를 미리 가리킨다)
--       04_files.sql  (file_version_id 를 지목한다)
--
-- ⚠️ ApprovalLineEligibilityPolicy 가 결재자에게 project_member 자격을 요구한다.
--    한지훈(VW106)·서영광(VW107)이 02_project.sql 에 VIEWER 로라도 들어 있어야 한다.
--    VIEWER 여도 결재는 된다 — 결재 권한은 approval_line 이 갖지 프로젝트 권한이 아니다.
--
-- ⚠️ approval.block_id 는 UNIQUE 다 (uk_approval_block). 블록 하나에 결재 하나.
-- ⚠️ uk_approval_line 은 2026-08-11 에 제거됐다 (V20260811161100) — 같은 차수 복수 결재자 허용.
--    그래도 이 시드는 차수를 겹치지 않게 넣는다.
--
-- ⭐ 9001 이 이 파일의 핵심이다 — 반려 1회 후 재상신
--    rev 1 : 대상 v4 → 한지훈 반려 (마진 시뮬에 반품율 미반영)
--    rev 2 : 대상 v5 → 3단 전원 승인 · 대표가 「1차 물량 60% 축소」 조건을 달았다
--    그 조건이 S4-1 발주 내역(3,400장)으로 실제로 이어진다.
--
-- 되돌리기: DELETE FROM approval_document WHERE approval_document_id BETWEEN 9001 AND 9007;
--           DELETE FROM approval_line     WHERE approval_line_id     BETWEEN 9001 AND 9015;
--           DELETE FROM approval_revision WHERE approval_revision_id BETWEEN 9001 AND 9006;
--           DELETE FROM approval          WHERE approval_id          BETWEEN 9001 AND 9005;
-- =====================================================================


-- ── 1. approval 5 ────────────────────────────────────────────────────
INSERT IGNORE INTO approval
  (approval_id, block_id, user_id, acting_drafter_id, status, current_revision_no, completed_at) VALUES
(9001, 9013, 'vitawear-VW101', NULL, 'COMPLETED', 2, '2025-12-05 16:05:00'),  -- 입점 추진 승인 (반려 1회)
(9002, 9027, 'vitawear-VW101', NULL, 'COMPLETED', 1, '2025-12-12 13:40:00'),  -- 신청 제출 승인
(9003, 9040, 'vitawear-VW101', NULL, 'COMPLETED', 1, '2025-12-19 15:30:00'),  -- 계약 체결 승인
(9004, 9054, 'vitawear-VW105', NULL, 'COMPLETED', 1, '2025-12-27 14:20:00'),  -- 발주 승인 [1차]
(9005, 9068, 'vitawear-VW105', NULL, 'COMPLETED', 1, '2026-03-19 11:00:00');  -- 발주 승인 [2차]


-- ── 2. approval_revision 6 ───────────────────────────────────────────
INSERT IGNORE INTO approval_revision
  (approval_revision_id, approval_id, revision_no, title, content, status, submitted_at, finished_at) VALUES

-- ⭐ 9001 rev 1 — 반려된 상신. 지우지 않는다. 이 회차가 남아 있는 게 요점이다.
(9001, 9001, 1, '26 S/S 무신사 스토어 입점 추진 승인 요청',
 '자사몰 매출 3개월 연속 하락에 따른 신규 채널 확보 건입니다.\n예상 GMV 2.1억 / 예상 정산 1.7억 / 예상 영업이익률 27%.\n첨부: 입점추진_사업성검토_v4.pdf',
 'REJECTED', '2025-11-28 16:10:00', '2025-11-29 11:20:00'),

-- 9001 rev 2 — 반려 지점부터 재개해 승인
(9002, 9001, 2, '26 S/S 무신사 스토어 입점 추진 승인 요청 (재상신)',
 '반려 사유(반품율 미반영)를 보완했습니다. 반품율 12% 가정을 마진 시뮬레이션에 반영해\n실질 마진율을 47.7% → 44.1% 로 재산출했습니다.\n첨부: 입점추진_사업성검토_v5.pdf',
 'COMPLETED', '2025-12-02 11:40:00', '2025-12-05 16:05:00'),

(9003, 9002, 1, '무신사 스토어 입점 신청서 제출 승인 요청',
 '제출 패키지 7건 준비 완료. 품질검토 지적 3건 전건 조치했습니다.\n첨부: 브랜드소개서 v1 / 26SS 룩북 v1',
 'COMPLETED', '2025-12-12 09:30:00', '2025-12-12 13:40:00'),

(9004, 9003, 1, '무신사 입점 계약 체결 승인 요청',
 '수수료 15% / 정산 익월 10일 / 계약기간 1년 자동갱신.\n독소 조항 2건 중 1건(일방적 수수료 변경 → 사전 30일 통보) 반영됐고,\n반품 귀책 구분 요청은 미반영입니다. 검토 부탁드립니다.\n첨부: 입점계약서 최종안 v1',
 'COMPLETED', '2025-12-18 10:00:00', '2025-12-19 15:30:00'),

(9005, 9004, 1, '26 S/S 1차 생산 발주 승인 요청',
 '대표 승인 조건(원안의 60% 축소)을 반영해 3,400장으로 발주합니다.\n발주액 72,420,000원 / 납기 2026-02-05.\n첨부: 발주서_26SS_1차_v1',
 'COMPLETED', '2025-12-26 10:00:00', '2025-12-27 14:20:00'),

(9006, 9005, 1, '26 S/S 2차 리오더 발주 승인 요청',
 '판매율 상위 5스타일 1,800장 리오더. 발주액 39,600,000원 / 납기 2026-04-03.\nC공장은 1차 납기 지연 귀책으로 제외했습니다.\n첨부: 발주서_26SS_2차_v1',
 'COMPLETED', '2026-03-18 10:30:00', '2026-03-19 11:00:00');


-- ── 3. approval_line 15 ──────────────────────────────────────────────
-- ⚠️ 최종 결재자는 sequence_no 최댓값이지 MASTER 가 아니다 (PERMISSION.md §7-2).
--    9001 은 3단(팀장→본부장→대표), 나머지는 2단이다. 건마다 다르다.
INSERT IGNORE INTO approval_line
  (approval_line_id, approval_revision_id, user_id, sequence_no, status, opinion, processed_at) VALUES

-- rev 9001 (입점 추진 1회차) — 2단에서 반려. 3단은 차례가 안 왔다.
(9001, 9001, 'vitawear-VW103', 1, 'APPROVED', '사업성 근거는 충분합니다.', '2025-11-28 17:30:00'),
(9002, 9001, 'vitawear-VW106', 2, 'REJECTED',
 '마진 시뮬레이션에 반품율이 반영되지 않았습니다. 온라인 의류는 반품율이 실질 마진을 좌우합니다. 보완 후 재상신 바랍니다.',
 '2025-11-29 11:20:00'),
(9003, 9001, 'vitawear-VW107', 3, 'CANCELED', NULL, NULL),

-- rev 9002 (재상신) — 3단 전원 승인. 대표가 조건을 달았다.
(9004, 9002, 'vitawear-VW103', 1, 'APPROVED', '반품율 반영 확인했습니다.', '2025-12-02 14:10:00'),
(9005, 9002, 'vitawear-VW106', 2, 'APPROVED', '보완 내용 적절합니다.', '2025-12-03 10:25:00'),
-- ⭐ 이 한 줄이 S4-1 발주 3,400장의 근거다
(9006, 9002, 'vitawear-VW107', 3, 'APPROVED',
 '추진 승인합니다. 다만 1차 발주 물량은 원안의 60% 로 축소하고 나머지는 리오더로 대응하십시오.',
 '2025-12-05 16:05:00'),

-- rev 9003 (신청 제출) — 2단
(9007, 9003, 'vitawear-VW103', 1, 'APPROVED', '제출물 품질 확인 완료.', '2025-12-12 10:20:00'),
(9008, 9003, 'vitawear-VW106', 2, 'APPROVED', NULL, '2025-12-12 13:40:00'),

-- rev 9004 (계약 체결) — 3단
(9009, 9004, 'vitawear-VW103', 1, 'APPROVED', '조항 검토 완료.', '2025-12-18 14:00:00'),
(9010, 9004, 'vitawear-VW106', 2, 'APPROVED',
 '반품 귀책 조항은 아쉽지만 첫 시즌 실적을 보고 갱신 시 재협의합시다.', '2025-12-19 10:40:00'),
(9011, 9004, 'vitawear-VW107', 3, 'APPROVED', '체결 승인합니다.', '2025-12-19 15:30:00'),

-- rev 9005 (1차 발주) — 2단
(9012, 9005, 'vitawear-VW103', 1, 'APPROVED', '축소 조건 반영 확인.', '2025-12-26 15:40:00'),
(9013, 9005, 'vitawear-VW106', 2, 'APPROVED', NULL, '2025-12-27 14:20:00'),

-- rev 9006 (2차 발주) — 2단
(9014, 9006, 'vitawear-VW103', 1, 'APPROVED', 'C공장 제외 타당합니다.', '2026-03-18 16:00:00'),
(9015, 9006, 'vitawear-VW106', 2, 'APPROVED', '리오더 물량 승인합니다.', '2026-03-19 11:00:00');


-- ── 4. approval_document 7 — ⭐ 파일이 아니라 「버전」을 박는다 ───────
-- 🚨 file_id 가 아니라 file_version_id 다. 이게 이 제품의 한 줄 요약이다.
--    파일을 박으면 나중에 새 버전이 올라왔을 때 "무엇을 승인했는지" 가 사라진다.
INSERT IGNORE INTO approval_document
  (approval_document_id, approval_revision_id, file_version_id) VALUES
(9001, 9001, 9005),  -- 입점추진 1회차 → 사업성검토 v4 (반려된 그 버전이 그대로 남는다)
(9002, 9002, 9006),  -- 입점추진 2회차 → 사업성검토 v5 (승인본)
(9003, 9003, 9007),  -- 신청 제출 → 브랜드소개서 v1
(9004, 9003, 9008),  -- 신청 제출 → 26SS 룩북 v1
(9005, 9004, 9011),  -- ⭐ 계약 체결 → 계약서 v1 최종안. v2 서명본이 아니다.
                     --    승인 뒤 v2 가 올라오므로 결재 화면에 「대상보다 새 버전 있음」 배지가 뜬다.
(9006, 9005, 9020),  -- 1차 발주 → 발주서 1차 v1
(9007, 9006, 9023);  -- 2차 발주 → 발주서 2차 v1


-- =====================================================================
-- 검증
-- =====================================================================
-- 1) 블록 ↔ 결재 다형성이 양방향으로 맞나 (0행이어야 정상)
--    SELECT b.block_id, b.type_id, a.approval_id FROM block b
--    LEFT JOIN approval a ON a.approval_id = b.type_id
--    WHERE b.type = 'APPROVAL' AND b.block_id BETWEEN 9001 AND 9100
--      AND (a.approval_id IS NULL OR a.block_id <> b.block_id);
--
-- 2) ⭐ 반려 → 재상신 — approval 9001 의 회차가 2개이고 1회차가 REJECTED 인가
--    SELECT revision_no, status, submitted_at, finished_at
--    FROM approval_revision WHERE approval_id = 9001 ORDER BY revision_no;
--
-- 3) ⭐ 결재 대상이 「버전」인가 — 회차별로 다른 file_version 을 물어야 한다
--    SELECT ar.approval_id, ar.revision_no, fv.file_id, fv.version_no, fv.original_file_name
--    FROM approval_document ad
--    JOIN approval_revision ar ON ar.approval_revision_id = ad.approval_revision_id
--    JOIN file_version fv ON fv.file_version_id = ad.file_version_id
--    WHERE ad.approval_document_id BETWEEN 9001 AND 9007
--    ORDER BY ar.approval_id, ar.revision_no;
--    기대: 9001 회차1 = v4 / 회차2 = v5  ← 이 두 줄이 발표 §4 의 증거다
--
-- 4) 「대상보다 새 버전 있음」 배지 대상 — 계약서 1건이 나와야 정상
--    SELECT ad.approval_document_id, fv.file_id, fv.version_no AS target_ver, MAX(l.version_no) AS latest_ver
--    FROM approval_document ad
--    JOIN file_version fv ON fv.file_version_id = ad.file_version_id
--    JOIN file_version l  ON l.file_id = fv.file_id
--    WHERE ad.approval_document_id BETWEEN 9001 AND 9007
--    GROUP BY ad.approval_document_id HAVING latest_ver > target_ver;
--
-- 5) 결재자가 전부 project_member 인가 (0행이어야 정상 · ApprovalLineEligibilityPolicy)
--    SELECT DISTINCT al.user_id FROM approval_line al
--    WHERE al.approval_line_id BETWEEN 9001 AND 9015
--      AND al.user_id NOT IN (SELECT user_id FROM project_member WHERE project_id = 9001);
