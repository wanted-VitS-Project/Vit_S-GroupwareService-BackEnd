-- =====================================================================
-- 14. 이미지 확장 — 21장 → 47장
-- ---------------------------------------------------------------------
-- 이미지 블록 8개에 평균 2.6장뿐이라 갤러리가 허전했다. 블록을 늘리면 배치(BLK-003)가
-- 깨지므로 블록은 그대로 두고 장수만 늘린다.
--
-- ⚠️ order_index 는 img_block 안에서 0부터 연속이어야 한다. 아래 시작값은
--    현재 DB 의 마지막 order_index 를 조회해 이어 붙인 값이다.
--    (ib9001=0~2, ib9002=0~3, ib9003=0~2, ib9004=0, ib9005=0~1, ib9006=0~2, ib9007=0~1, ib9008=0~2)
--
-- image_url 은 실제 오브젝트가 아니다 — 썸네일은 깨진다. 목록/캡션/순서 UI 용.
--
-- 되돌리기: DELETE FROM image WHERE img_id BETWEEN 9022 AND 9047;
-- =====================================================================

INSERT INTO image (img_id, img_block_id, original_name, image_url, extension, size, caption, order_index) VALUES

-- ── ib9001 / blk9005 (s9001 카테고리 트래픽 캡처) ─────────────────
(9022, 9001, 'traffic_knit_202511.png', 'demo/img/9001/traffic_knit_202511.png', 'png', 480000,
 '니트 카테고리 월간 방문자 추이 — 11월 들어 급증', 3),
(9023, 9001, 'competitor_price_band.png', 'demo/img/9001/competitor_price_band.png', 'png', 395000,
 '경쟁 브랜드 가격 분포 — 우리 가격대(6~12만원) 구간은 상대적으로 얇다', 4),

-- ── ib9002 / blk9019 (s9003 제품컷·룩북 컷) ───────────────────────
(9024, 9002, 'product_outer_01.png', 'demo/img/9002/product_outer_01.png', 'png', 1240000,
 '오버핏 블레이저 — 정면', 4),
(9025, 9002, 'product_outer_02.png', 'demo/img/9002/product_outer_02.png', 'png', 1180000,
 '오버핏 블레이저 — 디테일 (버튼·안감)', 5),
(9026, 9002, 'product_knit_01.png', 'demo/img/9002/product_knit_01.png', 'png', 1090000,
 '크루넥 니트 3컬러 — 컬러칩용', 6),
(9027, 9002, 'lookbook_scene_03.png', 'demo/img/9002/lookbook_scene_03.png', 'png', 2140000,
 '룩북 3번 신 — 재촬영본 (크롭 시 상품 잘림 문제로 다시 찍음)', 7),
(9028, 9002, 'lookbook_scene_04.png', 'demo/img/9002/lookbook_scene_04.png', 'png', 2080000,
 '룩북 4번 신 — 재촬영본', 8),
(9029, 9002, 'lookbook_scene_07.png', 'demo/img/9002/lookbook_scene_07.png', 'png', 1970000,
 '룩북 7번 신 — 재촬영본', 9),

-- ── ib9003 / blk9026 (s9004 반려 Before/After·제출 완료) ──────────
(9030, 9003, 'reject_before_02.png', 'demo/img/9003/reject_before_02.png', 'png', 505000,
 '반려 — 배경 순백(#FFFFFF) 아님', 3),
(9031, 9003, 'reject_after_02.png', 'demo/img/9003/reject_after_02.png', 'png', 870000,
 '수정 — 배경 누끼 후 순백 처리', 4),
(9032, 9003, 'size_chart_fixed.png', 'demo/img/9003/size_chart_fixed.png', 'png', 260000,
 '사이즈표 단위 통일 후 (inch → cm)', 5),

-- ── ib9004 / blk9033 (s9005 승인 통보 화면) ───────────────────────
(9033, 9004, 'md_feedback_mail.png', 'demo/img/9004/md_feedback_mail.png', 'png', 340000,
 'MD 피드백 메일 원문 — 3개 항목', 1),
(9034, 9004, 'partner_dashboard_first.png', 'demo/img/9004/partner_dashboard_first.png', 'png', 590000,
 '파트너센터 첫 진입 화면 — 입점 상태 승인', 2),

-- ── ib9005 / blk9046 (s9007 상세페이지 시안) ──────────────────────
(9035, 9005, 'detail_draft_v2_outer.png', 'demo/img/9005/detail_draft_v2_outer.png', 'png', 1520000,
 '2차 시안 — 모델컷 교체 후 (실제 색상과 일치)', 2),
(9036, 9005, 'color_chip_added.png', 'demo/img/9005/color_chip_added.png', 'png', 420000,
 '컬러칩 이미지 추가 — 색상 오인 CS 예방용', 3),
(9037, 9005, 'detail_size_section.png', 'demo/img/9005/detail_size_section.png', 'png', 610000,
 '상세페이지 사이즈 섹션 — 실측 기준 명시', 4),
(9038, 9005, 'open_day_listing.png', 'demo/img/9005/open_day_listing.png', 'png', 780000,
 '오픈 당일 상품 목록 — 12스타일 노출 확인', 5),

-- ── ib9006 / blk9060 (s9009 랭킹·클레임) ──────────────────────────
(9039, 9006, 'ranking_blazer_202602.png', 'demo/img/9006/ranking_blazer_202602.png', 'png', 620000,
 '오버핏 블레이저 카테고리 랭킹 41위 진입', 3),
(9040, 9006, 'search_keyword_inflow.png', 'demo/img/9006/search_keyword_inflow.png', 'png', 450000,
 '유입 경로 — 검색 62% / 기획전 24% / 기타 14%', 4),
(9041, 9006, 'restock_target_4sku.png', 'demo/img/9006/restock_target_4sku.png', 'png', 310000,
 '재고 20% 미만 4개 SKU — 2차 발주 우선 대상', 5),

-- ── ib9007 / blk9074 (s9011 랭킹 캡처) ────────────────────────────
(9042, 9007, 'review_rating_trend.png', 'demo/img/9007/review_rating_trend.png', 'png', 380000,
 '리뷰 평점 추이 — 4.5 → 4.2 하락 (사이즈 관련 리뷰 영향)', 2),
(9043, 9007, 'return_reason_top3.png', 'demo/img/9007/return_reason_top3.png', 'png', 340000,
 '3월 반품 사유 — 사이즈 52% / 색상 21% / 변심 18%', 3),
(9044, 9007, 'spring_outer_recut.png', 'demo/img/9007/spring_outer_recut.png', 'png', 1310000,
 '봄 아우터 야외 재촬영본 — 교체 작업 진행 중', 4),

-- ── ib9008 / blk9094 (s9014 입금 내역·홈택스) ─────────────────────
(9045, 9008, 'settlement_diff_74000.png', 'demo/img/9008/settlement_diff_74000.png', 'png', 410000,
 '1차 정산 반품 차감 74,000원 차이 — 이의 제기 근거', 3),
(9046, 9008, 'fee_18pct_evidence.png', 'demo/img/9008/fee_18pct_evidence.png', 'png', 395000,
 '기획전 판매분 수수료 18% 적용 화면 — 별도 약관 확인 후 정상 처리', 4),
(9047, 9008, 'hometax_2603_issued.png', 'demo/img/9008/hometax_2603_issued.png', 'png', 520000,
 '홈택스 3월분 세금계산서 발행 완료 (04-10)', 5);
