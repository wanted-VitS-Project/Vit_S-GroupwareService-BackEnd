-- =====================================================================
-- 10. 배치 재구성 — 모든 TEXT 를 col_span 1 로
-- ---------------------------------------------------------------------
-- 실측 근거: col_span 1 = 297px · 2 = 610px · 3 = 922px
--   TEXT 는 폭이 넓을수록 한 줄이 길어져 읽기 나빠진다 → 전부 1
--   APPROVAL / IMAGE 는 카드가 풍부하거나 썸네일이 여러 장이라 넓게 둔다
--   행마다 col_span 합 = 3 (BLK-003)
-- =====================================================================

-- 9001 채널 발굴·조건 등재 — IMAGE(9005) 를 아래 전폭으로
UPDATE block SET row_index=0, sort_order=0, col_span=1 WHERE block_id=9001;
UPDATE block SET row_index=0, sort_order=1, col_span=1 WHERE block_id=9002;
UPDATE block SET row_index=0, sort_order=2, col_span=1 WHERE block_id=9003;
UPDATE block SET row_index=1, sort_order=0, col_span=1 WHERE block_id=9006;
UPDATE block SET row_index=1, sort_order=1, col_span=1 WHERE block_id=9007;
UPDATE block SET row_index=1, sort_order=2, col_span=1 WHERE block_id=9004;
UPDATE block SET row_index=2, sort_order=0, col_span=3 WHERE block_id=9005;

-- 9002 사업성 검토·추진 결재 — AI(9008) 2 · APPROVAL(9013) 2
UPDATE block SET row_index=0, sort_order=0, col_span=2 WHERE block_id=9008;
UPDATE block SET row_index=0, sort_order=1, col_span=1 WHERE block_id=9009;
UPDATE block SET row_index=1, sort_order=0, col_span=1 WHERE block_id=9010;
UPDATE block SET row_index=1, sort_order=1, col_span=1 WHERE block_id=9011;
UPDATE block SET row_index=1, sort_order=2, col_span=1 WHERE block_id=9012;
UPDATE block SET row_index=2, sort_order=0, col_span=2 WHERE block_id=9013;
UPDATE block SET row_index=2, sort_order=1, col_span=1 WHERE block_id=9014;

-- 9003 제출물 작성 — IMAGE(9019) 20장이라 전폭
UPDATE block SET row_index=0, sort_order=0, col_span=1 WHERE block_id=9015;
UPDATE block SET row_index=0, sort_order=1, col_span=1 WHERE block_id=9016;
UPDATE block SET row_index=0, sort_order=2, col_span=1 WHERE block_id=9017;
UPDATE block SET row_index=1, sort_order=0, col_span=1 WHERE block_id=9020;
UPDATE block SET row_index=1, sort_order=1, col_span=1 WHERE block_id=9021;
UPDATE block SET row_index=1, sort_order=2, col_span=1 WHERE block_id=9018;
UPDATE block SET row_index=2, sort_order=0, col_span=3 WHERE block_id=9019;

-- 9004 품질검토·제출
UPDATE block SET row_index=0, sort_order=0, col_span=1 WHERE block_id=9022;
UPDATE block SET row_index=0, sort_order=1, col_span=1 WHERE block_id=9023;
UPDATE block SET row_index=0, sort_order=2, col_span=1 WHERE block_id=9024;
UPDATE block SET row_index=1, sort_order=0, col_span=1 WHERE block_id=9025;
UPDATE block SET row_index=1, sort_order=1, col_span=1 WHERE block_id=9028;
UPDATE block SET row_index=1, sort_order=2, col_span=1 WHERE block_id=9026;
UPDATE block SET row_index=2, sort_order=0, col_span=3 WHERE block_id=9027;

-- 9005 입점 심사 결과 (6블록)
UPDATE block SET row_index=0, sort_order=0, col_span=1 WHERE block_id=9029;
UPDATE block SET row_index=0, sort_order=1, col_span=1 WHERE block_id=9030;
UPDATE block SET row_index=0, sort_order=2, col_span=1 WHERE block_id=9031;
UPDATE block SET row_index=1, sort_order=0, col_span=1 WHERE block_id=9034;
UPDATE block SET row_index=1, sort_order=1, col_span=1 WHERE block_id=9032;
UPDATE block SET row_index=1, sort_order=2, col_span=1 WHERE block_id=9033;

-- 9006 계약 체결·계정 세팅
UPDATE block SET row_index=0, sort_order=0, col_span=1 WHERE block_id=9035;
UPDATE block SET row_index=0, sort_order=1, col_span=1 WHERE block_id=9036;
UPDATE block SET row_index=0, sort_order=2, col_span=1 WHERE block_id=9037;
UPDATE block SET row_index=1, sort_order=0, col_span=1 WHERE block_id=9039;
UPDATE block SET row_index=1, sort_order=1, col_span=1 WHERE block_id=9041;
UPDATE block SET row_index=1, sort_order=2, col_span=1 WHERE block_id=9038;
UPDATE block SET row_index=2, sort_order=0, col_span=3 WHERE block_id=9040;

-- 9007 상품 등록·검수·오픈
UPDATE block SET row_index=0, sort_order=0, col_span=1 WHERE block_id=9042;
UPDATE block SET row_index=0, sort_order=1, col_span=1 WHERE block_id=9043;
UPDATE block SET row_index=0, sort_order=2, col_span=1 WHERE block_id=9044;
UPDATE block SET row_index=1, sort_order=0, col_span=1 WHERE block_id=9045;
UPDATE block SET row_index=1, sort_order=1, col_span=1 WHERE block_id=9047;
UPDATE block SET row_index=1, sort_order=2, col_span=1 WHERE block_id=9048;
UPDATE block SET row_index=2, sort_order=0, col_span=3 WHERE block_id=9046;

-- 9008 [1차] 발주·입고·검품
UPDATE block SET row_index=0, sort_order=0, col_span=1 WHERE block_id=9049;
UPDATE block SET row_index=0, sort_order=1, col_span=1 WHERE block_id=9050;
UPDATE block SET row_index=0, sort_order=2, col_span=1 WHERE block_id=9051;
UPDATE block SET row_index=1, sort_order=0, col_span=1 WHERE block_id=9053;
UPDATE block SET row_index=1, sort_order=1, col_span=1 WHERE block_id=9055;
UPDATE block SET row_index=1, sort_order=2, col_span=1 WHERE block_id=9052;
UPDATE block SET row_index=2, sort_order=0, col_span=3 WHERE block_id=9054;

-- 9009 [1차] 노출·판매·CS
UPDATE block SET row_index=0, sort_order=0, col_span=1 WHERE block_id=9056;
UPDATE block SET row_index=0, sort_order=1, col_span=1 WHERE block_id=9057;
UPDATE block SET row_index=0, sort_order=2, col_span=1 WHERE block_id=9058;
UPDATE block SET row_index=1, sort_order=0, col_span=1 WHERE block_id=9059;
UPDATE block SET row_index=1, sort_order=1, col_span=1 WHERE block_id=9061;
UPDATE block SET row_index=1, sort_order=2, col_span=1 WHERE block_id=9062;
UPDATE block SET row_index=2, sort_order=0, col_span=3 WHERE block_id=9060;

-- 9010 [2차] 발주·입고·검품
UPDATE block SET row_index=0, sort_order=0, col_span=1 WHERE block_id=9063;
UPDATE block SET row_index=0, sort_order=1, col_span=1 WHERE block_id=9064;
UPDATE block SET row_index=0, sort_order=2, col_span=1 WHERE block_id=9065;
UPDATE block SET row_index=1, sort_order=0, col_span=1 WHERE block_id=9067;
UPDATE block SET row_index=1, sort_order=1, col_span=1 WHERE block_id=9069;
UPDATE block SET row_index=1, sort_order=2, col_span=1 WHERE block_id=9066;
UPDATE block SET row_index=2, sort_order=0, col_span=3 WHERE block_id=9068;

-- 9011 [2차] 노출·판매·CS
UPDATE block SET row_index=0, sort_order=0, col_span=1 WHERE block_id=9070;
UPDATE block SET row_index=0, sort_order=1, col_span=1 WHERE block_id=9071;
UPDATE block SET row_index=0, sort_order=2, col_span=1 WHERE block_id=9072;
UPDATE block SET row_index=1, sort_order=0, col_span=1 WHERE block_id=9073;
UPDATE block SET row_index=1, sort_order=1, col_span=1 WHERE block_id=9075;
UPDATE block SET row_index=1, sort_order=2, col_span=1 WHERE block_id=9076;
UPDATE block SET row_index=2, sort_order=0, col_span=3 WHERE block_id=9074;

-- 9012 [3차] 발주·입고·검품 (껍데기 5) — FILE 만 2
UPDATE block SET row_index=0, sort_order=0, col_span=1 WHERE block_id=9077;
UPDATE block SET row_index=0, sort_order=1, col_span=1 WHERE block_id=9078;
UPDATE block SET row_index=0, sort_order=2, col_span=1 WHERE block_id=9079;
UPDATE block SET row_index=1, sort_order=0, col_span=1 WHERE block_id=9081;
UPDATE block SET row_index=1, sort_order=1, col_span=2 WHERE block_id=9080;

-- 9013 [3차] 노출·판매·CS (껍데기 5) — 내용 없는 칸이라 폭 무관
UPDATE block SET row_index=0, sort_order=0, col_span=1 WHERE block_id=9082;
UPDATE block SET row_index=0, sort_order=1, col_span=1 WHERE block_id=9083;
UPDATE block SET row_index=0, sort_order=2, col_span=1 WHERE block_id=9084;
UPDATE block SET row_index=1, sort_order=0, col_span=1 WHERE block_id=9085;
UPDATE block SET row_index=1, sort_order=1, col_span=2 WHERE block_id=9086;

-- 9014 월정산 — ⭐ 정산 3회차를 맨 윗줄에 나란히 (하이라이트)
UPDATE block SET row_index=0, sort_order=0, col_span=1 WHERE block_id=9088;
UPDATE block SET row_index=0, sort_order=1, col_span=1 WHERE block_id=9089;
UPDATE block SET row_index=0, sort_order=2, col_span=1 WHERE block_id=9090;
UPDATE block SET row_index=1, sort_order=0, col_span=1 WHERE block_id=9087;
UPDATE block SET row_index=1, sort_order=1, col_span=1 WHERE block_id=9091;
UPDATE block SET row_index=1, sort_order=2, col_span=1 WHERE block_id=9092;
UPDATE block SET row_index=2, sort_order=0, col_span=1 WHERE block_id=9093;
UPDATE block SET row_index=2, sort_order=1, col_span=2 WHERE block_id=9094;

-- 9015 시즌 결산 (껍데기 6)
UPDATE block SET row_index=0, sort_order=0, col_span=1 WHERE block_id=9095;
UPDATE block SET row_index=0, sort_order=1, col_span=1 WHERE block_id=9096;
UPDATE block SET row_index=0, sort_order=2, col_span=1 WHERE block_id=9097;
UPDATE block SET row_index=1, sort_order=0, col_span=1 WHERE block_id=9098;
UPDATE block SET row_index=1, sort_order=1, col_span=1 WHERE block_id=9100;
UPDATE block SET row_index=1, sort_order=2, col_span=1 WHERE block_id=9099;

-- 삭제분은 화면에 안 뜨지만 정리
UPDATE block SET row_index=3, sort_order=0, col_span=1 WHERE block_id=9101;
UPDATE block SET row_index=3, sort_order=1, col_span=1 WHERE block_id=9102;
