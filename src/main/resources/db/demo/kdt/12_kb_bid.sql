-- =====================================================================
-- KB 12. 입찰 — KB국민은행 디지털 위탁교육 공고 1건(직접 등록) · 첨부 6 · AI 요약 1 · 상태 · 이력
-- ---------------------------------------------------------------------
-- 무엇: 「2026년 디지털 분야 위탁교육 업체 선정」(KB국민은행 인재개발부) 공고를 직접 등록으로 넣는다.
--       비타에듀가 이 공고에 제안서를 낸 프로젝트(P8011)의 출발점이다.
-- 왜:   이 시나리오는 공고에서 시작한다 — 공고가 없으면 프로젝트가 어디서 왔는지 화면에 없다.
--
-- 선행: 01_master.sql(회사3·사원) · 02_bid.sql(company 3 공고 8001~8007 이미 존재)
-- 후속: 13_kb_project.sql 이 project.bid_notice_id = 8011 로 이 공고를 문다.
--       19_kb_bid_review.sql 이 bid_review.project_id 와 summary.project_id 를 8011 로 채운다.
--       ⚠️ 세 파일로 나눈 이유는 FK 방향이 서로 반대라 한 파일에 못 넣기 때문이다.
--
-- ⭐ 8011 은 직접 등록이다. KB 누리집 공고라 나라장터 수집 대상이 아니다 →
--    crawl_source_id = 2(MANUAL) · owner_company_id = 3 · manual_dedup_key 필요.
-- ⚠️ estimated_amount 는 여기선 채운다(800백만원·VAT 포함·2사 예산). KDT 8001(훈련비 단가체계)과 달리
--    KB 는 협상에 의한 계약의 실제 예산이라 발주 금액 칸에 넣는 게 맞다. base_amount 는 NULL.
-- ⚠️ company_document 는 02_bid.sql 의 8001~8006 을 그대로 재사용한다(같은 회사 상시 자료). 새로 안 만든다.
--
-- 되돌리기:
--   DELETE FROM bid_notice_status_history WHERE bid_notice_status_history_id BETWEEN 8011 AND 8013;
--   DELETE FROM company_bid_notice_state  WHERE company_bid_notice_state_id  = 8011;
--   DELETE FROM bid_notice_summary        WHERE bid_notice_summary_id = 8011;
--   DELETE FROM bid_notice_attachment     WHERE bid_notice_attachment_id BETWEEN 8011 AND 8016;
--   DELETE FROM bid_notice                WHERE bid_notice_id = 8011;
-- =====================================================================


-- ── 1. 공고 1 (직접 등록) ────────────────────────────────────────────
INSERT IGNORE INTO bid_notice
  (bid_notice_id, crawl_source_id, owner_company_id, external_id, notice_ord, manual_dedup_key,
   notice_type, notice_name, notice_agency, demand_agency,
   announced_at, bid_start_at, application_deadline_at, bid_deadline_at, opening_at,
   base_amount, estimated_amount, participation_qualification_text,
   contract_method, evaluation_method, source_url, has_attachment, notice_status,
   crawled_at, business_category_id, created_by) VALUES
(8011, 2, 3, 'KB-HRD-2026-DT', '00',
 SHA2('vitaedu|KB-HRD-2026-DT|2026년 디지털 분야 위탁교육 업체 선정', 256),
 '용역', '2026년 디지털 분야 위탁교육 업체 선정',
 '주식회사 KB국민은행', 'KB국민은행 인재개발부',
 '2025-11-20 09:00:00', '2025-11-20 09:00:00', '2025-11-28 18:00:00', '2025-11-28 18:00:00',
 '2025-12-09 10:00:00',
 NULL, 800000000.00,
 '사업자등록증에 교육서비스업 또는 금융·산업교육이 명시된 업체, 설립 후 3년 이상 경과, 국세 및 지방세 체납이 없고 2023년부터 2025년까지 디지털 교육 운영실적을 보유한 업체',
 '협상에 의한 계약',
 '기술능력 70점과 가격 30점 · 기술능력 평가 80퍼센트 이상이며 종합 85점 이상이면 협상적격 · 상위 2개 업체 선정',
 'https://www.kbstar.com', 1, 'COLLECTED',
 NULL, 8001, 'vitaedu-VE101');


-- ── 2. 공고 첨부 6 — 공고문 붙임 서식 ────────────────────────────────
-- 🚨 chk_bid_notice_attachment_source — source_url 과 storage_key 중 정확히 하나만.
--    업로드형이므로 storage_key 만 넣고 source_url 은 NULL 이다.
-- ⚠️ 파일 실물은 S3 에 없다. 목록·이름까지만. 열지 마라.
INSERT IGNORE INTO bid_notice_attachment
  (bid_notice_attachment_id, bid_notice_id, attachment_kind, attachment_order,
   file_name, source_url, storage_key, upload_status, size_bytes, mime_type) VALUES
(8011, 8011, 'NOTICE_SPEC', 1, '제안요청서_디지털.pdf',
 NULL, 'bidding/3/notices/8011/attachments/8011-rfp.pdf', 'READY', 662528, 'application/pdf'),
(8012, 8011, 'NOTICE_SPEC', 2, '별지1_연도별_재무상태_비교표.hwp',
 NULL, 'bidding/3/notices/8011/attachments/8012-finance-form.hwp', 'READY', 38912, 'application/x-hwp'),
(8013, 8011, 'NOTICE_SPEC', 3, '별지2_청렴계약이행_확약서.hwp',
 NULL, 'bidding/3/notices/8011/attachments/8013-integrity-form.hwp', 'READY', 33792, 'application/x-hwp'),
(8014, 8011, 'NOTICE_SPEC', 4, '별지3_가격제안서.hwp',
 NULL, 'bidding/3/notices/8011/attachments/8014-price-form.hwp', 'READY', 45056, 'application/x-hwp'),
(8015, 8011, 'NOTICE_SPEC', 5, '별지4_영업담당자_지정_위임장.hwp',
 NULL, 'bidding/3/notices/8011/attachments/8015-delegate-form.hwp', 'READY', 36864, 'application/x-hwp'),
(8016, 8011, 'NOTICE_SPEC', 6, '별지5_개인정보_수집이용_동의서.hwp',
 NULL, 'bidding/3/notices/8011/attachments/8016-privacy-form.hwp', 'READY', 31744, 'application/x-hwp');


-- ── 3. AI 요약 1 — 확정본 ────────────────────────────────────────────
-- ⚠️ notice_snapshot·prompt·processing_attempt_id 는 전부 NOT NULL 이다.
-- ⚠️ project_id 는 여기서 NULL 이다 — project 가 아직 없다. 19_kb_bid_review.sql 이 UPDATE 로 채운다.
INSERT IGNORE INTO bid_notice_summary
  (bid_notice_summary_id, company_id, project_id, bid_notice_id, parent_summary_id, revision_no,
   requested_by, prompt, notice_snapshot,
   overview_summary, amount_summary, schedule_summary, qualification_summary,
   task_summary, risk_summary,
   summary_status, processing_attempt_id, retry_count,
   confirmed, confirmed_by, confirmed_at, completed_at) VALUES
(8011, 3, NULL, 8011, NULL, 1,
 'vitaedu-VE101',
 '공고문과 붙임 서식을 근거로 신청 요건, 예산과 단가, 일정, 평가 배점, 탈락 조건을 정리해 달라.',
 JSON_OBJECT('noticeName', '2026년 디지털 분야 위탁교육 업체 선정',
             'noticeAgency', '주식회사 KB국민은행',
             'applicationDeadlineAt', '2025-11-28T18:00:00'),
 'KB국민은행이 임직원 디지털 역량 강화를 위해 2026년 위탁교육 업체 두 곳을 뽑는 협상에 의한 계약이다. 선정되면 2026년 2월부터 1년간 DT기획과 DT개발 과정을 운영한다.',
 '예산은 부가세를 포함해 800백만원 이내이고 두 개 업체를 선정한다. 집합과정은 중식대와 강의장과 노트북 대여료를 포함한 1인당 단가로, 사이버과정은 과정별 1인당 단가로 제안한다. 고용보험 환급과정 비율이 높을수록 가격 배점에서 유리하다.',
 '공고는 11월 20일에 게시됐고 제안서는 11월 28일 18시까지 우편이나 방문으로 접수한다. 업체선정 발표는 12월 9일이다. 위탁기간은 2026년 2월부터 2027년 1월까지다.',
 '사업자등록증에 교육서비스업이 명시돼야 하고 설립 후 3년이 지나야 한다. 국세와 지방세 체납이 없어야 하고 2023년부터 2025년까지 디지털 교육 운영실적이 있어야 한다. 우리는 네 요건을 모두 충족한다.',
 'DT기획과 DT개발 두 분야로 나눠 제안한다. 각 분야는 단계별 핵심과정과 자유수강 일반과정으로 구성한다. 제안서는 업체소개와 사업수행계획과 기술수준과 운영능력과 인력전문성과 가격 여섯 항목으로 쓰고 요약본을 함께 낸다. 별지 서식 다섯 종에 법인인감을 날인한다.',
 '기술능력 평가에서 최고점과 최저점을 뺀 산술평균이 배점의 80퍼센트에 못 미치거나 종합 점수가 85점에 못 미치면 협상적격에서 빠진다. 협상적격자 중 상위 두 곳만 우선협상대상이 된다. 유효한 제안이 두 곳이 안 되면 재공고한다.',
 'COMPLETED', '7b1e9d02-6c34-4a58-8e11-2f9c0a7d4e55', 0,
 1, 'vitaedu-VE103', '2025-11-21 16:40:00', '2025-11-21 11:20:00');


-- ── 4. 회사별 공고 상태 1 ────────────────────────────────────────────
-- ⭐ 이 행이 없으면 회사 3 목록에 KB 공고가 안 뜬다. UNIQUE 는 (company_id, bid_notice_id).
-- ⚠️ 직접 등록이라 수집 실행 이력이 없다 (run_id NULL 이 정상). 프로젝트 전환은 project.bid_notice_id 로 표현된다.
INSERT IGNORE INTO company_bid_notice_state
  (company_bid_notice_state_id, company_id, bid_notice_id, notice_status, dismiss_reason,
   is_favorite, first_seen_run_id, last_seen_run_id, first_seen_at, last_seen_at) VALUES
(8011, 3, 8011, 'IN_REVIEW', NULL, 1, NULL, NULL, '2025-11-20 10:00:00', '2026-08-16 09:00:00');


-- ── 5. 상태 변경 이력 3 ──────────────────────────────────────────────
INSERT IGNORE INTO bid_notice_status_history
  (bid_notice_status_history_id, company_id, bid_notice_id,
   previous_status, changed_status, reason, changed_by, created_at) VALUES
(8011, 3, 8011, 'COLLECTED', 'IN_REVIEW', 'KB 디지털 위탁교육 공고 검토 착수', 'vitaedu-VE101', '2025-11-20 10:00:00'),
(8012, 3, 8011, 'IN_REVIEW', 'IN_REVIEW', '참가자격 네 요건 충족 확인. 제안 준비로 전환한다', 'vitaedu-VE103', '2025-11-22 09:30:00'),
(8013, 3, 8011, 'IN_REVIEW', 'IN_REVIEW', '선정 후 프로젝트로 전환해 운영을 이어간다', 'vitaedu-VE101', '2026-02-06 10:00:00');


-- =====================================================================
-- 검증
-- =====================================================================
-- 1) 회사 3 의 공고 목록이 8건인가 (기존 7 + KB 8011). 운영 DB 의 기존 수집분이 안 섞이나
--    SELECT n.bid_notice_id, s.notice_status, s.is_favorite, n.notice_name
--    FROM company_bid_notice_state s JOIN bid_notice n USING (bid_notice_id)
--    WHERE s.company_id = 3 ORDER BY n.bid_notice_id;
--    기대: 8행 · 8011 이 IN_REVIEW · 관심
--
-- 2) 첨부의 source_url / storage_key 가 정확히 하나만 차 있나 (0행이어야 정상)
--    SELECT bid_notice_attachment_id FROM bid_notice_attachment
--    WHERE bid_notice_id = 8011 AND NOT ((source_url IS NULL) XOR (storage_key IS NULL));
--
-- 3) manual_dedup_key 가 유일한가 (0행이어야 정상)
--    SELECT manual_dedup_key FROM bid_notice WHERE manual_dedup_key IS NOT NULL
--    GROUP BY manual_dedup_key HAVING COUNT(*) > 1;
