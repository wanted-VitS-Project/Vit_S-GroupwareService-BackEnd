-- =====================================================================
-- 06. 재무 — 입금 1 · 세금계산서 2
-- ---------------------------------------------------------------------
-- 무엇: cash_flow / tax_invoice 를 settlement_block 에 연결한다.
-- 왜:   완료 프로젝트도 아닌데 정산 화면이 비어 있으면 「2차 정산까지 왔다」는 말이 화면에 없다.
--       6행이 아니라 3행이면 되는 이유는 기준일(2026-04-08)에 2차 입금이 아직이기 때문이다.
--
-- 선행: 03_blocks.sql (settlement_block 9001~9003)
--
-- ⭐ 기준일 2026-04-08 이 정산 상태 3종을 전부 만든다
--    1차 COMPLETED — 계산서 O 입금 O
--    2차 WAITING   — 계산서 O 입금 X (기한 04-10 미도래라 지연으로도 안 잡힌다)
--    3차 PENDING   — 미도래 · 연결 없음
--    → 화면: 1차 [O O O] / 2차 [O O -] / 3차 [- - -]
--
-- ⚠️ 계산서를 수수료분(10,260,000)이 아니라 정산액 기준 매출로 잡는다.
--    수수료분으로 잡으면 3열에 계산서 10,260,000 옆에 입금 55,950,000 이 떠서
--    같은 회차인데 금액이 5배 어긋나 보인다. 총액 = 입금액으로 맞춘다.
--    (세무 처리 방식은 단정하지 않는다 — 발표에서는 「매출 계산서 1건」까지만 말한다)
--
-- ⭐ 두 원장 모두 3상태를 채운다 — 「연결됨」만 있으면 재무 화면의 절반이 안 보인다
--
--            연결됨   미연결   연결 제외   합계
--   cash_flow   1        2         2         5
--   tax_invoice 2        1         1         4
--
--   미연결   = settle_block_id IS NULL AND is_excluded = 0  → 미연결 건수에 잡힌다
--   연결 제외 = settle_block_id IS NULL AND is_excluded = 1  → 집계에서 빠진다
--   (api/finance.md §86~90 · PATCH /finance/cash-flows/exclude · /tax-invoices/exclude 의 대상)
--
-- 되돌리기: DELETE FROM cash_flow   WHERE cash_flow_id BETWEEN 9001 AND 9005;
--           DELETE FROM tax_invoice WHERE tax_id       BETWEEN 9001 AND 9004;
-- =====================================================================


-- ── 1. cash_flow 1 — 1차 입금만 ──────────────────────────────────────
-- 🚨 balance_after_key · balance_after_present 는 GENERATED STORED 컬럼이다.
--    INSERT 목록에 넣으면 ERROR 3105 로 죽는다. 절대 쓰지 마라.
--    (V20260811161220 · V20260811161230 이 유니크 키 때문에 만든 것이다)
-- ⚠️ 유니크: uk_cash_flow_dedup
--      (company_id, bank_name, type, traded_at, amount, balance_after_present, balance_after_key)
--    같은 은행·같은 시각·같은 금액이면 중복으로 막힌다. traded_at 을 분 단위까지 다르게 둔다.
-- ⚠️ company_id 는 NOT NULL 이고 DEFAULT 가 제거됐다 (V20260811161200). 명시 필수.
INSERT IGNORE INTO cash_flow
  (cash_flow_id, company_id, settle_block_id, bank_name, type, traded_at, amount, balance_after,
   depositor_name, bank_memo, source_type, bank_txn_id, linked_by, linked_at) VALUES
(9001, 2, 9001, 'OO은행', 'INCOME', '2026-03-10 11:04:00', 55950000.00, 168420000.00,
 '(주)무신사', '무신사정산202602', 'CSV', 'BANK20260310-0001',
 'vitawear-VW108', '2026-03-10 13:20:00');

-- ⭐ bank_memo 가 발표의 근거 데이터다.
--    적요에 「무신사정산202602」만 있고 프로젝트명이 어디에도 없다.
--    그래서 자동 매칭은 후보 추천까지만 하고 확정 버튼은 사람이 누른다 — linked_by = 조은비.
--
-- ⛔ 2차 입금(74,100,000 · 2026-04-10)은 넣지 않는다.
--    기준일이 04-08 이라 아직 안 들어온 돈이다. 넣으면 2차가 WAITING 이 아니라 COMPLETED 가 되고
--    상태 3종 그림이 무너진다.


-- ── 1-2. ⭐ 미연결 · 연결 제외 입출금 4 ──────────────────────────────
-- 「연결할 수 있는데 아직 안 한 것」과 「애초에 연결 대상이 아닌 것」은 다른 상태다.
--
--   미연결  : settle_block_id IS NULL AND is_excluded = 0  → 미연결 건수에 잡힌다 (매칭 대기)
--   연결 제외: settle_block_id IS NULL AND is_excluded = 1  → 집계에서 빠진다 (정산 대상이 아님)
--   ⚠️ totalCount 는 is_excluded 무관 전체다 (api/finance.md §86~90).
--
-- 이 둘을 구분해 넣지 않으면 /finance/payments 매칭 화면이 빈 목록이 되고,
-- 「미매칭 우선 정렬」·「연결 제외 처리」 기능을 보여줄 대상이 없다.
INSERT IGNORE INTO cash_flow
  (cash_flow_id, company_id, settle_block_id, bank_name, type, traded_at, amount, balance_after,
   depositor_name, bank_memo, source_type, bank_txn_id, linked_by, linked_at, is_excluded) VALUES

-- 🟡 미연결 ① — ⭐ 매칭 화면의 주인공. 돈은 왔는데 어느 회차인지 적요로 알 수 없다.
(9002, 2, NULL, 'OO은행', 'INCOME', '2026-04-08 09:41:00', 1240000.00, 169660000.00,
 '(주)무신사', '무신사정산조정202603', 'CSV', 'BANK20260408-0001',
 NULL, NULL, 0),

-- 🟡 미연결 ② — 지출도 정산 블록에 붙일 수 있다. 이 프로젝트는 OUTCOME 회차를 안 만들어 미연결로 남았다.
(9003, 2, NULL, 'OO은행', 'OUTCOME', '2026-01-10 14:05:00', 36210000.00, 98420000.00,
 '(주)에이패션', '26SS1차발주선금', 'CSV', 'BANK20260110-0002',
 NULL, NULL, 0),

-- ⛔ 연결 제외 ① — 사무실 임대료. 프로젝트와 무관해 미연결 건수에서 뺀다.
(9004, 2, NULL, 'OO은행', 'OUTCOME', '2026-04-01 10:00:00', 3500000.00, 168920000.00,
 '(주)스페이스원', '임대료202604', 'CSV', 'BANK20260401-0001',
 NULL, NULL, 1),

-- ⛔ 연결 제외 ② — 예금 이자.
(9005, 2, NULL, 'OO은행', 'INCOME', '2026-03-31 23:50:00', 41200.00, 168461200.00,
 'OO은행', '이자', 'CSV', 'BANK20260331-0001',
 NULL, NULL, 1);

-- ⭐ 9002 가 §6-E 「자동 매칭은 후보 추천까지만」의 시연 데이터다.
--    적요 '무신사정산조정202603' 은 2차 정산 조정분처럼 보이지만 확신할 수 없다.
--    금액도 회차 금액과 안 맞는다 → 사람이 판단해서 붙이거나 제외한다.
--    ⛔ 발표 리허설에서 이걸 연결하지 마라. 매칭 화면이 빈 목록이 된다.


-- ── 2. tax_invoice 2 — 1·2차 계산서 ──────────────────────────────────
-- ⚠️ 유니크: uk_tax_invoice_approval_no (approval_no) — 승인번호는 전역 유일이다.
-- ⚠️ type 은 settlement_block.type 과 맞춘다 (둘 다 INCOME).
--    매입(OUTCOME)으로 넣으면 입출금 구분이 엇갈린 행이 된다.
-- 🎲 승인번호·사업자번호는 형식만 맞춘 더미다.
INSERT IGNORE INTO tax_invoice
  (tax_id, company_id, settle_block_id, type, approval_no, issued_no,
   supplier_biz_no, supply_amount, tax_amount, total_amount,
   buyer_name, buyer_biz_no, ceo_name, item_name, memo, source_type,
   linked_by, linked_at, is_excluded) VALUES

-- 1차 (2026-02월분) — 55,950,000 = 50,863,636 + 5,086,364
(9001, 2, 9001, 'INCOME', '20260310-00000000-00000001', '2026-03-10',
 '000-00-00000', 50863636.00, 5086364.00, 55950000.00,
 '(주)무신사', '000-00-00001', '서영광',
 '26 S/S 위탁판매 정산 (2026-02월분)', NULL, 'HOMETAX_API',
 'vitawear-VW108', '2026-03-10 13:22:00', 0),

-- 2차 (2026-03월분) — 74,100,000 = 67,363,636 + 6,736,364 · 입금은 아직
(9002, 2, 9002, 'INCOME', '20260405-00000000-00000002', '2026-04-05',
 '000-00-00000', 67363636.00, 6736364.00, 74100000.00,
 '(주)무신사', '000-00-00001', '서영광',
 '26 S/S 위탁판매 정산 (2026-03월분)', '1차 이의분 74,000 가산 반영분 포함', 'HOMETAX_API',
 'vitawear-VW108', '2026-04-05 10:40:00', 0);

-- ⛔ 3차 계산서는 없다 — 2026-04월분이라 발행 시점(05-10)이 미도래다.


-- ── 2-1. ⭐ 미연결 · 연결 제외 세금계산서 2 ──────────────────────────
-- 세금계산서 미연결은 입출금 미연결과 **다른 개념**이라 별도 숫자로 내려간다
-- (`taxInvoiceUnlinkedCount` · api/settlement.md §197). 그래서 원장마다 따로 채운다.
INSERT IGNORE INTO tax_invoice
  (tax_id, company_id, settle_block_id, type, approval_no, issued_no,
   supplier_biz_no, supply_amount, tax_amount, total_amount,
   buyer_name, buyer_biz_no, ceo_name, item_name, memo, source_type,
   linked_by, linked_at, is_excluded) VALUES

-- 🟡 미연결 — A공장 매입 계산서. cash_flow 9003(선금 36,210,000)과 짝이다.
--    연결할 수 있는데 OUTCOME 정산 회차를 안 만들어 아직 안 붙였다.
(9003, 2, NULL, 'OUTCOME', '20260210-00000000-00000003', '2026-02-10',
 '000-00-00002', 32918182.00, 3291818.00, 36210000.00,
 '주식회사 비타웨어', '000-00-00000', '서영광',
 '26 S/S 1차 생산 발주 (작업지시 대금)', NULL, 'HOMETAX_API',
 NULL, NULL, 0),

-- ⛔ 연결 제외 — 사무실 임대료. cash_flow 9004 와 짝이고 둘 다 제외 처리했다.
(9004, 2, NULL, 'OUTCOME', '20260401-00000000-00000004', '2026-04-01',
 '000-00-00003', 3181818.00, 318182.00, 3500000.00,
 '주식회사 비타웨어', '000-00-00000', '서영광',
 '사무실 임대료 (2026-04)', '프로젝트 무관 — 연결 제외', 'HOMETAX_API',
 NULL, NULL, 1);


-- =====================================================================
-- 검증
-- =====================================================================
-- 1) 🚨 회차별 3금액 일치 — 0행이어야 정상
--    틀려도 화면은 정상으로 보인다. 이 쿼리로만 잡힌다.
--    SELECT sb.round_no, sb.planned_amount, ti.total_amount, cf.amount
--    FROM settlement_block sb
--    LEFT JOIN tax_invoice ti ON ti.settle_block_id = sb.settle_id AND ti.deleted_at IS NULL
--    LEFT JOIN cash_flow   cf ON cf.settle_block_id = sb.settle_id AND cf.deleted_at IS NULL
--    WHERE sb.project_id = 9001
--      AND (ti.total_amount <> sb.planned_amount OR cf.amount <> sb.planned_amount);
--
-- 2) 3열 상태 — 1차 [O O] / 2차 [O -] / 3차 [- -]
--    SELECT sb.round_no, sb.status,
--           (ti.tax_id IS NOT NULL) AS tax_linked,
--           (cf.cash_flow_id IS NOT NULL) AS cash_linked
--    FROM settlement_block sb
--    LEFT JOIN tax_invoice ti ON ti.settle_block_id = sb.settle_id
--    LEFT JOIN cash_flow   cf ON cf.settle_block_id = sb.settle_id
--    WHERE sb.project_id = 9001 ORDER BY sb.round_no;
--
-- 3) ⭐ 원장별 3상태 분포 — 아래대로 나와야 재무 화면이 다 산다
--    SELECT '입출금' AS 원장,
--           SUM(settle_block_id IS NOT NULL)                  AS 연결됨,
--           SUM(settle_block_id IS NULL AND is_excluded = 0)  AS 미연결,
--           SUM(is_excluded = 1)                              AS 연결제외,
--           COUNT(*)                                          AS 전체
--    FROM cash_flow WHERE company_id = 2 AND deleted_at IS NULL
--    UNION ALL
--    SELECT '세금계산서',
--           SUM(settle_block_id IS NOT NULL),
--           SUM(settle_block_id IS NULL AND is_excluded = 0),
--           SUM(is_excluded = 1), COUNT(*)
--    FROM tax_invoice WHERE company_id = 2 AND deleted_at IS NULL;
--    기대: 입출금 1/2/2/5 · 세금계산서 2/1/1/4
--
-- 4) ⚠️ 시연 동선 — /finance/settlements 는 includeCompleted 생략 시
--    COMPLETED·CLOSED 프로젝트를 제외한다. 메인(9001)은 IN_PROGRESS 라 정상적으로 뜨고,
--    곁들이 25 F/W(9002 · COMPLETED)·29CM(9003 · CLOSED)는 안 뜬다. 그게 정상 동작이다.
