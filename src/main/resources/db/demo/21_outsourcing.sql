-- =====================================================================
-- 21. 무신사 프로젝트(9001)에 외주 관리 추가 — 출금 연결 + 세금계산서 확대
-- ---------------------------------------------------------------------
-- 지금까지 정산 블록은 전부 INCOME(무신사에서 받는 돈)뿐이었다.
-- 나가는 돈(공장 발주비·촬영비·물류비)은 cash_flow 에만 있고 프로젝트에 안 붙어 있었다.
-- → OUTCOME 정산 블록을 만들어 출금을 연결한다. 프로젝트 손익이 양방향으로 잡힌다.
--
-- ⭐ settlement_block.type 은 enum('INCOME','OUTCOME') 이다. 같은 테이블로 수입/지출을 다 쓴다.
--    화면에서 "받을 돈"과 "줄 돈"을 가르는 건 이 컬럼 하나다.
--
-- ⚠️ 정산 블록을 **회차/용역 단위**로 잡았지 공장 단위로 잡지 않았다.
--    기존 출금이 "1차 발주 선금 / 잔금" 으로 끊겨 있어서, 공장별로 쪼개면
--    기존 cash_flow 금액과 안 맞아 전부 고쳐야 한다. 블록 기준을 데이터에 맞춘다.
--
-- 스테이지 순서: 26 S/S 시즌 운영(4) 다음에 외주 관리(5)를 끼우고 정산·결산을 뒤로 민다.
--
-- 되돌리기: DELETE FROM tax_invoice WHERE tax_id BETWEEN 9005 AND 9020;
--           UPDATE cash_flow SET settle_block_id=NULL, linked_by=NULL, linked_at=NULL
--             WHERE cash_flow_id IN (9003,9020,9021,9022);
--           DELETE FROM cash_flow WHERE cash_flow_id BETWEEN 9025 AND 9030;
--           DELETE FROM settlement_block WHERE settle_id BETWEEN 9007 AND 9013;
--           DELETE FROM block_file WHERE block_id BETWEEN 9330 AND 9348;
--           DELETE FROM file_version WHERE file_version_id BETWEEN 9058 AND 9062;
--           DELETE FROM file WHERE file_id BETWEEN 9029 AND 9032;
--           DELETE FROM checklist WHERE chk_id BETWEEN 9249 AND 9260;
--           DELETE FROM checklist_block WHERE chk_block_id BETWEEN 9113 AND 9115;
--           DELETE FROM `text` WHERE txt_id BETWEEN 9167 AND 9172;
--           DELETE FROM block WHERE block_id BETWEEN 9330 AND 9348;
--           DELETE FROM step WHERE step_id BETWEEN 9401 AND 9403;
--           DELETE FROM stage WHERE stage_id=9017;
--           UPDATE stage SET sort_order=5 WHERE stage_id=9005;
--           UPDATE stage SET sort_order=6 WHERE stage_id=9006;
--           UPDATE step  SET sort_order=14 WHERE step_id=9014;
--           UPDATE step  SET sort_order=15 WHERE step_id=9015;
-- =====================================================================

-- ── 스테이지 · 스텝 순서 조정 ─────────────────────────────────────
UPDATE stage SET sort_order=6 WHERE stage_id=9005;   -- 월 정산
UPDATE stage SET sort_order=7 WHERE stage_id=9006;   -- 시즌 결산
INSERT INTO stage (stage_id, project_id, name, sort_order) VALUES (9017, 9001, '외주 관리', 5);

UPDATE step SET sort_order=17 WHERE step_id=9014;    -- 월 정산
UPDATE step SET sort_order=18 WHERE step_id=9015;    -- 시즌 결산

INSERT INTO step (step_id, project_id, stage_id, name, sort_order, status, started_on, ended_on, owner_user_id) VALUES
(9401, 9001, 9017, '외주 업체 계약',    14, 'DONE',        '2025-12-08','2026-01-09','vitawear-VW111'),
(9402, 9001, 9017, '생산 외주비 지급',  15, 'IN_PROGRESS', '2026-01-10', NULL,       'vitawear-VW108'),
(9403, 9001, 9017, '용역 외주비 지급',  16, 'IN_PROGRESS', '2025-12-18', NULL,       'vitawear-VW108');

-- ── 블록 19건 ─────────────────────────────────────────────────────
INSERT INTO block (block_id, step_id, title, type, type_id, owner, row_index, col_span, sort_order, created_by) VALUES
-- 9401 외주 업체 계약
(9330,9401,'외주 범위','TEXT',9167,'vitawear-VW111',0,1,0,'vitawear-VW111'),
(9331,9401,'업체별 계약 조건','TEXT',9168,'vitawear-VW111',0,1,1,'vitawear-VW111'),
(9332,9401,'계약 확인','CHECKLIST',9113,'vitawear-VW111',0,1,2,'vitawear-VW111'),
(9333,9401,'외주 계약서','FILE',NULL,'vitawear-VW111',1,2,0,'vitawear-VW111'),
-- 9402 생산 외주비 지급
(9334,9402,'지급 기준','TEXT',9169,'vitawear-VW108',0,1,0,'vitawear-VW108'),
(9335,9402,'지급 현황','TEXT',9170,'vitawear-VW108',0,1,1,'vitawear-VW108'),
(9336,9402,'지급 전 확인','CHECKLIST',9114,'vitawear-VW108',0,1,2,'vitawear-VW108'),
(9337,9402,'1차 발주 대금','SETTLEMENT',9007,'vitawear-VW108',1,1,0,'vitawear-VW108'),
(9338,9402,'2차 발주 대금','SETTLEMENT',9008,'vitawear-VW108',1,1,1,'vitawear-VW108'),
(9339,9402,'3차 발주 대금','SETTLEMENT',9009,'vitawear-VW108',1,1,2,'vitawear-VW108'),
(9340,9402,'발주 계산서·이체 확인','FILE',NULL,'vitawear-VW108',2,2,0,'vitawear-VW108'),
-- 9403 용역 외주비 지급
(9341,9403,'용역 외주 목록','TEXT',9171,'vitawear-VW108',0,1,0,'vitawear-VW108'),
(9342,9403,'단가와 정산 방식','TEXT',9172,'vitawear-VW108',0,1,1,'vitawear-VW108'),
(9343,9403,'지급 전 확인','CHECKLIST',9115,'vitawear-VW108',0,1,2,'vitawear-VW108'),
(9344,9403,'룩북 촬영 외주','SETTLEMENT',9010,'vitawear-VW108',1,1,0,'vitawear-VW108'),
(9345,9403,'상세페이지 제작 외주','SETTLEMENT',9011,'vitawear-VW108',1,1,1,'vitawear-VW108'),
(9346,9403,'물류 대행(3PL)','SETTLEMENT',9012,'vitawear-VW108',1,1,2,'vitawear-VW108'),
(9347,9403,'사이즈 검수 외주','SETTLEMENT',9013,'vitawear-VW108',2,1,0,'vitawear-VW108'),
(9348,9403,'용역 계약서·계산서','FILE',NULL,'vitawear-VW108',2,2,1,'vitawear-VW108');

INSERT INTO `text` (txt_id, block_id, content) VALUES
(9167,9330,'26 S/S 에서 외부에 맡기는 건 네 갈래다.

- 봉제 생산 (공장 3사)
- 룩북·제품컷 촬영
- 상세페이지 제작
- 물류 대행

사이즈 검수는 반품 문제가 커져서 이번 시즌 중에 추가로 붙였다.'),
(9168,9331,'- **에이패션** 니트, 선금 50% 잔금 50%
- **비제이텍스** 우븐, 선금 50% 잔금 50%
- **씨엠어패럴** 아우터, 납품 후 30일
- **프레임스튜디오** 촬영, 건당
- **디자인노트** 상세페이지, 스타일당
- **케이로지스** 3PL, 월 정산'),
(9169,9334,'선금은 발주 확정일에, 잔금은 입고 검품 합격 후 5영업일 안에 준다.

불량 폐기분은 잔금에서 빼고 지급한다.'),
(9170,9335,'1차 72,420,000원 **완납**. 선금 01-10, 잔금 02-09.

2차 39,600,000원은 선금 19,800,000원만 나갔고 잔금은 04-06 지급 예정이다.

3차는 발주 자체가 미정이라 금액이 안 잡혀 있다.'),
(9171,9341,'촬영과 상세페이지는 시즌 초에 한 번, 물류는 매달 나간다.

사이즈 검수는 3월에 계약했고 첫 청구가 아직 안 왔다.'),
(9172,9342,'- 촬영 8,800,000원, 12스타일 일괄
- 상세페이지 4,200,000원, 스타일당 350,000원
- 물류 월 정산, 출고 건당 1,850원
- 사이즈 검수 스타일당 125,000원, 계약금 1,500,000원 선지급');

INSERT INTO checklist_block (chk_block_id, block_id) VALUES (9113,9332),(9114,9336),(9115,9343);

INSERT INTO checklist (chk_id, chk_block_id, content, is_completed) VALUES
(9249,9113,'사업자등록증 사본 수취',1),
(9250,9113,'단가표 확정',1),
(9251,9113,'하자 책임 조항 확인',1),
(9252,9113,'대금 지급 조건 확인',1),
(9253,9114,'입고 검품 합격 확인',1),
(9254,9114,'불량 폐기분 차감 반영',1),
(9255,9114,'매입 세금계산서 수취',1),
(9256,9114,'이체 내역 대조',0),
(9257,9115,'용역 완료 확인',1),
(9258,9115,'산출물 인수',1),
(9259,9115,'매입 세금계산서 수취',1),
(9260,9115,'이체 내역 대조',0);

-- ── 외주 계약서 파일 ──────────────────────────────────────────────
INSERT INTO file (file_id, project_id, name, created_by) VALUES
(9029, 9001, '외주계약서_봉제3사',        'vitawear-VW111'),
(9030, 9001, '외주단가표_26SS',           'vitawear-VW111'),
(9031, 9001, '용역계약서_촬영_상세페이지', 'vitawear-VW108'),
(9032, 9001, '매입계산서_외주_26SS',      'vitawear-VW108');

INSERT INTO file_version
  (file_version_id, file_id, version_no, upload_status, storage_key, original_file_name,
   extension, mime_type, size_bytes, page_count, comment,
   uploaded_by, uploader_name, uploader_department, uploader_position, completed_at) VALUES
(9058,9029,1,'COMPLETED','demo/file/9029/v1.pdf','외주계약서_봉제3사_v1.pdf','pdf','application/pdf',
 3240000,26,'에이패션·비제이텍스·씨엠어패럴 3사 일괄',
 'vitawear-VW111','노현주','생산관리팀','대리','2026-01-09 15:00:00'),
(9059,9030,1,'COMPLETED','demo/file/9030/v1.xlsx','외주단가표_26SS_v1.xlsx','xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
 88000,NULL,'스타일별 공임 단가',
 'vitawear-VW111','노현주','생산관리팀','대리','2025-12-22 11:30:00'),
(9060,9030,2,'COMPLETED','demo/file/9030/v2.xlsx','외주단가표_26SS_v2.xlsx','xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
 94000,NULL,'씨엠어패럴 아우터 단가 협의 반영 (건당 1,200원 인하)',
 'vitawear-VW111','노현주','생산관리팀','대리','2026-01-08 16:20:00'),
(9061,9031,1,'COMPLETED','demo/file/9031/v1.pdf','용역계약서_촬영_상세페이지_v1.pdf','pdf','application/pdf',
 1420000,12,'프레임스튜디오·디자인노트 2건',
 'vitawear-VW108','조은비','재무팀','과장','2025-12-15 10:40:00'),
(9062,9032,1,'COMPLETED','demo/file/9032/v1.pdf','매입계산서_외주_26SS_v1.pdf','pdf','application/pdf',
 980000,8,'홈택스 수취분 일괄 출력',
 'vitawear-VW108','조은비','재무팀','과장','2026-04-08 09:20:00');

INSERT INTO block_file (block_id, file_id, linked_by) VALUES
(9333,9029,'vitawear-VW111'),
(9333,9030,'vitawear-VW111'),
(9340,9032,'vitawear-VW108'),
(9348,9031,'vitawear-VW108');

-- ── OUTCOME 정산 블록 7건 ─────────────────────────────────────────
INSERT INTO settlement_block
  (settle_id, block_id, project_id, round_no, type, status,
   total_amount, planned_amount, planned_tax_amount, planned_date, tax_invoice_due_date,
   actual_amount, actual_date, trader_name, bank_name, account_number, account_holder) VALUES
(9007,9337,9001,1,'OUTCOME','COMPLETED',
 72420000, 72420000, 7242000, '2026-02-09','2026-02-10',
 72420000,'2026-02-09 14:50:00','(주)에이패션','OO은행','***-**-****33','주식회사 에이패션'),
(9008,9338,9001,2,'OUTCOME','PARTIAL',
 39600000, 39600000, 3960000, '2026-04-06','2026-04-10',
 19800000,'2026-03-19 15:10:00','(주)비제이텍스','OO은행','***-**-****57','주식회사 비제이텍스'),
(9009,9339,9001,3,'OUTCOME','PENDING',
 NULL, NULL, NULL, NULL, NULL,
 NULL, NULL,'(주)에이패션','OO은행','***-**-****33','주식회사 에이패션'),
(9010,9344,9001,1,'OUTCOME','COMPLETED',
 8800000, 8800000, 880000, '2025-12-18','2025-12-31',
 8800000,'2025-12-18 16:00:00','(주)프레임스튜디오','OO은행','***-**-****81','주식회사 프레임스튜디오'),
(9011,9345,9001,1,'OUTCOME','COMPLETED',
 4200000, 4200000, 420000, '2026-01-08','2026-01-10',
 4200000,'2026-01-08 11:20:00','(주)디자인노트','OO은행','***-**-****04','주식회사 디자인노트'),
(9012,9346,9001,1,'OUTCOME','PARTIAL',
 11500000, 11500000, 1150000, '2026-04-30','2026-05-10',
 11500000,'2026-04-08 10:00:00','(주)케이로지스','OO은행','***-**-****29','주식회사 케이로지스'),
(9013,9347,9001,1,'OUTCOME','WAITING',
 1500000, 1500000, 150000, '2026-04-30','2026-05-10',
 NULL, NULL,'(주)큐씨랩','OO은행','***-**-****66','주식회사 큐씨랩');

-- ── 출금 6건 추가 ─────────────────────────────────────────────────
-- balance_after 를 비운 건은 은행 CSV 에 잔액이 안 실려 온 경우다 (실제로 흔하다).
INSERT INTO cash_flow
  (cash_flow_id, company_id, settle_block_id, bank_name, type, traded_at, amount, balance_after,
   depositor_name, bank_memo, source_type, bank_txn_id, linked_by, linked_at, is_excluded) VALUES
(9025,2,9008,'OO은행','OUTCOME','2026-04-06 15:30:00',19800000,NULL,
 '(주)비제이텍스','26SS2차발주잔금','CSV','BANK20260406-0002','vitawear-VW108','2026-04-06 17:00:00',0),
(9026,2,9010,'OO은행','OUTCOME','2025-12-18 16:00:00', 8800000,NULL,
 '(주)프레임스튜디오','룩북촬영비','CSV','BANK20251218-0001','vitawear-VW108','2025-12-19 09:40:00',0),
(9027,2,9011,'OO은행','OUTCOME','2026-01-08 11:20:00', 4200000,NULL,
 '(주)디자인노트','상세페이지제작비','CSV','BANK20260108-0001','vitawear-VW108','2026-01-08 14:10:00',0),
(9028,2,9012,'OO은행','OUTCOME','2026-04-08 10:00:00', 6100000,NULL,
 '(주)케이로지스','물류비202604','CSV','BANK20260408-0002','vitawear-VW108','2026-04-08 11:30:00',0),
-- 미연결 — 계약 범위 밖에서 추가로 나간 건. 화면에서 "연결하기" 시연 대상.
(9029,2,NULL,'OO은행','OUTCOME','2026-04-28 14:00:00', 2900000,NULL,
 '(주)디자인노트','상세페이지수정비','CSV','BANK20260428-0001',NULL,NULL,0),
(9030,2,NULL,'OO은행','OUTCOME','2026-03-27 09:50:00', 1500000,NULL,
 '(주)큐씨랩','사이즈검수계약금','CSV','BANK20260327-0001',NULL,NULL,0);

-- ── 기존 출금 4건을 외주 블록에 연결 ──────────────────────────────
UPDATE cash_flow SET settle_block_id=9007, linked_by='vitawear-VW108', linked_at='2026-01-10 16:00:00'
 WHERE cash_flow_id=9003;   -- 1차 발주 선금
UPDATE cash_flow SET settle_block_id=9007, linked_by='vitawear-VW108', linked_at='2026-02-09 16:30:00'
 WHERE cash_flow_id=9020;   -- 1차 발주 잔금
UPDATE cash_flow SET settle_block_id=9008, linked_by='vitawear-VW108', linked_at='2026-03-19 17:00:00'
 WHERE cash_flow_id=9021;   -- 2차 발주 선금
UPDATE cash_flow SET settle_block_id=9012, linked_by='vitawear-VW108', linked_at='2026-03-30 17:20:00'
 WHERE cash_flow_id=9022;   -- 3월 물류비

-- ── 세금계산서 4건 → 20건 ─────────────────────────────────────────
-- INCOME  = 우리가 발행한 매출 계산서 (buyer 가 상대방)
-- OUTCOME = 우리가 수취한 매입 계산서 (buyer 가 비타웨어)
INSERT INTO tax_invoice
  (tax_id, company_id, settle_block_id, type, approval_no, issued_no, supplier_biz_no,
   supply_amount, tax_amount, total_amount, buyer_name, buyer_biz_no, ceo_name,
   item_name, memo, source_type, linked_by, linked_at, is_excluded) VALUES

-- 외주 매입 계산서 — 정산 블록에 연결
(9005,2,9007,'OUTCOME','20260210-00000000-00000005','2026-02-10','000-00-00001',
 65836364, 6583636, 72420000,'주식회사 비타웨어','000-00-00000','김대현',
 '26SS 1차 봉제 임가공','1차 발주 완납분','HOMETAX_API','vitawear-VW108','2026-02-10 10:20:00',0),
(9006,2,9008,'OUTCOME','20260410-00000000-00000006','2026-04-10','000-00-00002',
 36000000, 3600000, 39600000,'주식회사 비타웨어','000-00-00000','박세훈',
 '26SS 2차 봉제 임가공','선금·잔금 합산 발행','HOMETAX_API','vitawear-VW108','2026-04-10 09:50:00',0),
(9007,2,9010,'OUTCOME','20251231-00000000-00000007','2025-12-31','000-00-00003',
  8000000,  800000,  8800000,'주식회사 비타웨어','000-00-00000','이가람',
 '26SS 룩북 촬영 용역','12스타일 일괄','HOMETAX_API','vitawear-VW108','2025-12-31 14:00:00',0),
(9008,2,9011,'OUTCOME','20260110-00000000-00000008','2026-01-10','000-00-00004',
  3818182,  381818,  4200000,'주식회사 비타웨어','000-00-00000','최윤서',
 '상세페이지 제작 용역','12스타일','HOMETAX_API','vitawear-VW108','2026-01-10 11:00:00',0),
(9009,2,9012,'OUTCOME','20260410-00000000-00000009','2026-04-10','000-00-00005',
 10454545, 1045455, 11500000,'주식회사 비타웨어','000-00-00000','정한별',
 '3PL 물류 대행 (2026-03~04)','월 정산 2개월분','HOMETAX_API','vitawear-VW108','2026-04-10 10:30:00',0),

-- 25 F/W 매출 계산서 — 20_cashflow.sql 이 만든 정산 블록에 연결
(9010,2,9004,'INCOME','20251010-00000000-00000010','2025-10-10','000-00-00000',
 40181818, 4018182, 44200000,'주식회사 무신사','000-00-11111','조만호',
 '25FW 위탁판매 정산 (2025-09)',NULL,'HOMETAX_API','vitawear-VW108','2025-10-10 15:00:00',0),
(9011,2,9005,'INCOME','20251110-00000000-00000011','2025-11-10','000-00-00000',
 57545455, 5754545, 63300000,'주식회사 무신사','000-00-11111','조만호',
 '25FW 위탁판매 정산 (2025-10)',NULL,'HOMETAX_API','vitawear-VW108','2025-11-10 14:20:00',0),
(9012,2,9006,'INCOME','20260112-00000000-00000012','2026-01-12','000-00-00000',
 70363636, 7036364, 77400000,'주식회사 무신사','000-00-11111','조만호',
 '25FW 위탁판매 정산 (2025-11~12)','2차 이의분 132,000 가산','HOMETAX_API','vitawear-VW108','2026-01-12 16:00:00',0),

-- OEM 매출 계산서 — 정산 블록이 없어 미연결
(9013,2,NULL,'INCOME','20250825-00000000-00000013','2025-08-25','000-00-00000',
 29127273, 2912727, 32040000,'주식회사 한성텍스타일','000-00-22222','한동규',
 'OEM 우븐 셔츠 선금 (30%)',NULL,'HOMETAX_API',NULL,NULL,0),
(9014,2,NULL,'INCOME','20260130-00000000-00000014','2026-01-30','000-00-00000',
 67963636, 6796364, 74760000,'주식회사 한성텍스타일','000-00-22222','한동규',
 'OEM 우븐 셔츠 잔금','불량 84장 차감 후','HOMETAX_API',NULL,NULL,0),
(9015,2,NULL,'INCOME','20260123-00000000-00000015','2026-01-23','000-00-00000',
 27054545, 2705455, 29760000,'주식회사 라온어패럴','000-00-33333','서지웅',
 'OEM 니트 선금 (30%)',NULL,'HOMETAX_API',NULL,NULL,0),
(9016,2,NULL,'INCOME','20260406-00000000-00000016','2026-04-06','000-00-00000',
 22545455, 2254545, 24800000,'주식회사 라온어패럴','000-00-33333','서지웅',
 'OEM 니트 1차 납품분',NULL,'HOMETAX_API',NULL,NULL,0),

-- 재고 처분 매출
(9017,2,NULL,'INCOME','20251114-00000000-00000017','2025-11-14','000-00-00000',
 24363636, 2436364, 26800000,'주식회사 아울렛플러스','000-00-44444','문재훈',
 '25SS 재고 위탁 정산',NULL,'HOMETAX_API',NULL,NULL,0),

-- 미연결 매입 — 아직 정산 블록에 안 붙인 외주비
(9018,2,NULL,'OUTCOME','20260430-00000000-00000018','2026-04-30','000-00-00004',
  2636364,  263636,  2900000,'주식회사 비타웨어','000-00-00000','최윤서',
 '상세페이지 수정 용역','계약 범위 밖 추가분','HOMETAX_API',NULL,NULL,0),
(9019,2,NULL,'OUTCOME','20260331-00000000-00000019','2026-03-31','000-00-00006',
  1363636,  136364,  1500000,'주식회사 비타웨어','000-00-00000','권나연',
 '사이즈 검수 용역 계약금',NULL,'HOMETAX_API',NULL,NULL,0),

-- 정산과 무관 — 연결 제외
(9020,2,NULL,'OUTCOME','20260401-00000000-00000020','2026-04-01','000-00-00007',
  4909091,  490909,  5400000,'주식회사 비타웨어','000-00-00000','오상혁',
 '사무실 관리비 (2026-04)','정산 대상 아님','HOMETAX_API',NULL,NULL,1);

-- =====================================================================
-- 정합성 보정 — 적용 후 대조에서 걸린 것
-- ---------------------------------------------------------------------
-- status 와 "연결된 출금 합계"가 어긋나 있었다. 이건 컴파일도 안 되고 예외도 안 나는데
-- 화면 숫자만 틀리는 종류라 반드시 대조 쿼리로 잡아야 한다 (플레이북 §7).
--   9008 2차 발주  PARTIAL 인데 선금+잔금이 다 연결됨 → COMPLETED
--   9012 물류 대행 PARTIAL 인데 3월+4월이 다 연결됨   → COMPLETED
--   → PARTIAL 사례가 사라지므로 9013(사이즈 검수)을 계약금만 지급한 PARTIAL 로 바꾼다.
-- =====================================================================
UPDATE settlement_block SET status='COMPLETED', actual_amount=39600000, actual_date='2026-04-06 15:30:00'
 WHERE settle_id=9008;
UPDATE settlement_block SET status='COMPLETED' WHERE settle_id=9012;
UPDATE settlement_block SET status='PARTIAL',
       total_amount=4500000, planned_amount=4500000, planned_tax_amount=450000,
       actual_amount=1500000, actual_date='2026-03-27 09:50:00'
 WHERE settle_id=9013;

UPDATE cash_flow SET settle_block_id=9013, linked_by='vitawear-VW108', linked_at='2026-03-27 11:00:00'
 WHERE cash_flow_id=9030;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='1차 72,420,000원 **완납**. 선금 01-10, 잔금 02-09.

2차 39,600,000원도 **완납**. 선금 03-19, 잔금 04-06.

3차는 발주 자체가 미정이라 금액이 안 잡혀 있다.'
WHERE b.block_id=9335;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='- 촬영 8,800,000원, 12스타일 일괄
- 상세페이지 4,200,000원, 스타일당 350,000원
- 물류 월 정산, 출고 건당 1,850원
- 사이즈 검수 4,500,000원, 계약금 1,500,000원 선지급 후 완료 시 잔금'
WHERE b.block_id=9342;
