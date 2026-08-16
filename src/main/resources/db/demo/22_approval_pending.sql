-- =====================================================================
-- 22. 결재관리 "결재 대기(scope=pending)" 채우기
-- ---------------------------------------------------------------------
-- 증상: 블록에서 올린 결재가 결재관리 페이지에 안 보인다.
--
-- 구조 (GET /api/v1/approvals?scope=...) — ApprovalQueryService.listApprovals()
--   scope=drafted (기본) → a.user_id = 나            "내가 올린 결재"
--   scope=pending        → 현재 회차 내 라인이 ACTIVE  "내가 결재할 차례"
--   scope=all            → MASTER 만 (그 외 403)
--
-- 즉 **기안자도 아니고 ACTIVE 결재자도 아니면 목록에 아무것도 안 뜬다.**
-- 프로젝트 멤버라는 사실은 결재 목록과 아무 상관이 없다.
--
-- 문제였던 것: ACTIVE 라인이 한지훈 3건, 배수진 1건뿐이라 나머지 11명은 "결재 대기" 탭이 비었다.
-- → IN_PROGRESS 스텝에 결재 8건을 추가하고 ACTIVE 를 흩뿌린다.
--   MEMBER 6 + MASTER 1 전원이 최소 1건씩 대기를 갖게 된다.
--
-- ⚠️ ACTIVE 는 **현재 회차(approval.current_revision_no)** 의 라인에만 있어야 잡힌다.
--    지난 회차에 ACTIVE 를 남기면 목록에 안 뜬다.
-- ⚠️ 결재를 붙이는 스텝은 반드시 IN_PROGRESS 여야 한다. DONE 스텝에 진행 중 결재는 모순이다.
--    여기서 쓰는 9011·9014·9402·9403 은 전부 IN_PROGRESS 다.
--
-- ⛔ ADMIN 6명은 이 파일로 해결되지 않는다. ApprovalListScopePolicy 가 ADMIN 을
--    페이지 진입 단계에서 403 으로 막는다(MGT-003). 정책 코드 문제라 데이터로 못 고친다.
--
-- 되돌리기: DELETE FROM approval_line WHERE approval_line_id BETWEEN 9100 AND 9115;
--           DELETE FROM approval_revision WHERE approval_revision_id BETWEEN 9043 AND 9050;
--           DELETE FROM approval WHERE approval_id BETWEEN 9042 AND 9049;
--           DELETE FROM `text` WHERE txt_id BETWEEN 9173 AND 9174;
--           DELETE FROM block WHERE block_id BETWEEN 9350 AND 9357;
-- =====================================================================

-- ── 블록 8건 (각 스텝 row3 에 APPROVAL 1 + 2) ─────────────────────
INSERT INTO block (block_id, step_id, title, type, type_id, owner, row_index, col_span, sort_order, created_by) VALUES
(9350,9011,'4월 추가 쿠폰 예산 승인','APPROVAL',9042,'vitawear-VW110',3,1,0,'vitawear-VW110'),
(9351,9011,'봄 아우터 이미지 교체 승인','APPROVAL',9043,'vitawear-VW102',3,2,1,'vitawear-VW102'),
(9352,9014,'3차 정산 예상액 확정 승인','APPROVAL',9044,'vitawear-VW108',3,1,0,'vitawear-VW108'),
(9353,9014,'반품 차감 이의 재제기 승인','APPROVAL',9045,'vitawear-VW109',3,2,1,'vitawear-VW109'),
(9354,9402,'3차 발주 대금 집행 승인','APPROVAL',9046,'vitawear-VW111',3,1,0,'vitawear-VW111'),
(9355,9402,'공장 단가 인상 수용 승인','APPROVAL',9047,'vitawear-VW113',3,2,1,'vitawear-VW113'),
(9356,9403,'사이즈 검수 외주 잔금 승인','APPROVAL',9048,'vitawear-VW107',3,1,0,'vitawear-VW107'),
(9357,9403,'상세페이지 수정비 추가 집행 승인','APPROVAL',9049,'vitawear-VW105',3,2,1,'vitawear-VW105');

-- ── 결재 8건 (전부 IN_PROGRESS — 완료 안 된 상태여야 ACTIVE 가 산다) ──
INSERT INTO approval (approval_id, block_id, user_id, status, current_revision_no, completed_at) VALUES
(9042,9350,'vitawear-VW110','IN_PROGRESS',1,NULL),
(9043,9351,'vitawear-VW102','IN_PROGRESS',1,NULL),
(9044,9352,'vitawear-VW108','IN_PROGRESS',1,NULL),
(9045,9353,'vitawear-VW109','IN_PROGRESS',1,NULL),
(9046,9354,'vitawear-VW111','IN_PROGRESS',1,NULL),
(9047,9355,'vitawear-VW113','IN_PROGRESS',1,NULL),
(9048,9356,'vitawear-VW107','IN_PROGRESS',1,NULL),
(9049,9357,'vitawear-VW105','IN_PROGRESS',1,NULL);

INSERT INTO approval_revision (approval_revision_id, approval_id, revision_no, title, content, status, submitted_at, finished_at) VALUES
(9043,9042,1,'4월 추가 쿠폰 예산 집행 건',
 '4월 기획전 선정 결과가 04-12 에 나옵니다. 미선정이면 자체 쿠폰으로 대체해야 해서 예산 3,100,000원을 미리 확보해 둡니다.',
 'IN_PROGRESS','2026-04-07 09:30:00',NULL),
(9044,9043,1,'봄 아우터 상세 이미지 교체 건',
 '겨울 배경 컷이라 계절감이 안 맞는다는 문의가 들어옵니다. 야외 재촬영본 3종으로 교체합니다. 추가 비용은 없습니다.',
 'IN_PROGRESS','2026-04-06 14:20:00',NULL),
(9045,9044,1,'3차 정산 예상액 확정 건',
 '4월 구매확정분 기준 잠정 68,200,000원입니다. 반품 유예 기간이 남아 변동 가능성이 있어 확정 전 승인을 받습니다.',
 'IN_PROGRESS','2026-04-08 10:00:00',NULL),
(9046,9045,1,'반품 차감 이의 재제기 건',
 '3월분에서 반품 차감이 우리 집계보다 96,000원 많습니다. 1차 때와 같은 중복 차감으로 보여 다시 이의를 넣습니다.',
 'IN_PROGRESS','2026-04-07 16:00:00',NULL),
(9047,9046,1,'3차 발주 대금 집행 건',
 '3차 발주 수량이 확정되면 선금 50% 를 먼저 집행해야 납기를 맞출 수 있습니다. 한도만 미리 승인받습니다.',
 'IN_PROGRESS','2026-04-08 11:10:00',NULL),
(9048,9047,1,'공장 단가 인상 수용 건',
 '에이패션이 원사 가격 상승을 이유로 니트 공임 8% 인상을 요청했습니다. 4% 까지 협의했고 이 선에서 수용할지 판단이 필요합니다.',
 'IN_PROGRESS','2026-04-06 10:40:00',NULL),
(9049,9048,1,'사이즈 검수 외주 잔금 집행 건',
 '큐씨랩 검수가 12스타일 중 8스타일까지 끝났습니다. 계약상 완료 시 잔금 3,000,000원인데 부분 집행이 가능한지 검토 바랍니다.',
 'IN_PROGRESS','2026-04-07 13:30:00',NULL),
(9050,9049,1,'상세페이지 수정비 추가 집행 건',
 '계약 범위 밖 수정이 발생해 2,900,000원이 이미 나갔습니다. 사후 승인으로 처리하고 정산 블록에 연결합니다.',
 'IN_PROGRESS','2026-04-08 09:00:00',NULL);

-- ── 결재선 16건 ───────────────────────────────────────────────────
-- ACTIVE 를 7명(MEMBER 6 + MASTER 1)에게 흩뿌린다. 각 계정의 "결재 대기" 탭이 여기서 채워진다.
-- 1차가 APPROVED 면 2차가 ACTIVE, 1차가 ACTIVE 면 2차는 WAITING(아직 차례 아님)이다.
INSERT INTO approval_line (approval_line_id, approval_revision_id, user_id, sequence_no, status, opinion, processed_at) VALUES
-- 9042 → 김서연 차례
(9100,9043,'vitawear-VW103',1,'APPROVED','예산 규모는 적정합니다.','2026-04-07 15:00:00'),
(9101,9043,'vitawear-VW101',2,'ACTIVE',NULL,NULL),
-- 9043 → 이현우 차례
(9102,9044,'vitawear-VW103',1,'ACTIVE',NULL,NULL),
(9103,9044,'vitawear-VW106',2,'WAITING',NULL,NULL),
-- 9044 → 정민아 차례
(9104,9045,'vitawear-VW103',1,'APPROVED',NULL,'2026-04-08 13:20:00'),
(9105,9045,'vitawear-VW104',2,'ACTIVE',NULL,NULL),
-- 9045 → 최동석 차례
(9106,9046,'vitawear-VW105',1,'ACTIVE',NULL,NULL),
(9107,9046,'vitawear-VW106',2,'WAITING',NULL,NULL),
-- 9046 → 박준호 차례
(9108,9047,'vitawear-VW103',1,'APPROVED',NULL,'2026-04-08 14:00:00'),
(9109,9047,'vitawear-VW102',2,'ACTIVE',NULL,NULL),
-- 9047 → 배수진 차례
(9110,9048,'vitawear-VW112',1,'ACTIVE',NULL,NULL),
(9111,9048,'vitawear-VW107',2,'WAITING',NULL,NULL),
-- 9048 → 이현우 차례
(9112,9049,'vitawear-VW106',1,'APPROVED','부분 집행 가능한지 계약서 확인 바랍니다.','2026-04-07 17:10:00'),
(9113,9049,'vitawear-VW103',2,'ACTIVE',NULL,NULL),
-- 9049 → 김서연 차례
(9114,9050,'vitawear-VW101',1,'ACTIVE',NULL,NULL),
(9115,9050,'vitawear-VW106',2,'WAITING',NULL,NULL);
