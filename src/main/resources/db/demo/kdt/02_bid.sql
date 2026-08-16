-- =====================================================================
-- KDT 02. 입찰 — 수집 조건·실행 · 공고 7 · 첨부 5 · AI 요약 1 · 회사 상태 7 · 사내 문서 6
-- ---------------------------------------------------------------------
-- 무엇: 「2026년 K-디지털 기초역량훈련 심사 계획 공고」를 직접 등록 공고로 넣고,
--       나라장터 수집분 6건을 곁들여 공고 목록·관심·제외 화면을 채운다.
-- 왜:   이 시나리오는 공고에서 시작한다. 공고가 없으면 프로젝트가 어디서 왔는지 화면에 없다.
--
-- 선행: 01_master.sql
-- 후속: 03_project.sql 이 project.bid_notice_id 로 이 공고를 문다.
--       09_bid_review.sql 이 bid_review.project_id 와 summary.project_id 를 채운다.
--       ⚠️ 세 파일로 나눈 이유는 FK 방향이 서로 반대라 한 파일에 못 넣기 때문이다.
--
-- ⚠️ crawl_source 는 새로 만들지 않는다 — 마이그레이션이 이미 넣었다.
--       1 = NARA(OPEN_API) · 2 = MANUAL(직접 등록)
--
-- ⭐ 회사별 공고 목록은 `company_bid_notice_state` 로 갈린다.
--    운영 DB 에 이미 수집된 공고가 수백 건 있어도 회사 3 의 상태 행이 없으면 목록에 안 뜬다.
--    반대로 상태 행을 안 넣으면 우리 공고도 안 뜬다.
--
-- 되돌리기:
--   DELETE FROM bid_notice_status_history WHERE bid_notice_status_history_id BETWEEN 8001 AND 8006;
--   DELETE FROM company_bid_notice_state  WHERE company_bid_notice_state_id  BETWEEN 8001 AND 8007;
--   DELETE FROM bid_notice_summary        WHERE bid_notice_summary_id = 8001;
--   DELETE FROM bid_notice_attachment     WHERE bid_notice_attachment_id BETWEEN 8001 AND 8005;
--   DELETE FROM bid_notice                WHERE bid_notice_id BETWEEN 8001 AND 8007;
--   DELETE FROM crawl_run                 WHERE crawl_run_id  BETWEEN 8001 AND 8002;
--   DELETE FROM crawl_condition           WHERE crawl_condition_id = 8001;
--   DELETE FROM company_document_version  WHERE company_document_version_id BETWEEN 8001 AND 8008;
--   DELETE FROM company_document          WHERE company_document_id BETWEEN 8001 AND 8006;
-- =====================================================================


-- ── 1. 수집 조건 1 · 실행 2 ──────────────────────────────────────────
-- 직접 등록 공고(8001)는 이 조건과 무관하다. 나라장터 수집분 6건이 여기서 나왔다는 이력이다.
INSERT IGNORE INTO crawl_condition
  (crawl_condition_id, company_id, crawl_source_id, condition_name, params,
   enabled, auto_collection_enabled, schedule_type, scheduled_time, timezone,
   next_run_at, last_success_at, last_collected_count, created_by) VALUES
(8001, 3, 1, '디지털 교육 용역 · 훈련 위탁',
 JSON_OBJECT('keywords', JSON_ARRAY('디지털 교육', '직업훈련', '이러닝', 'LMS'),
             'excludeKeywords', JSON_ARRAY('급식', '시설공사')),
 1, 1, 'DAILY', '07:30:00', 'Asia/Seoul',
 '2026-08-17 07:30:00', '2026-08-16 07:30:00', 3, 'vitaedu-VE101');

INSERT IGNORE INTO crawl_run
  (crawl_run_id, crawl_condition_id, condition_snapshot, trigger_type, run_status,
   started_at, finished_at, collected_count, inserted_count, updated_count, skipped_count,
   requested_by) VALUES
(8001, 8001, JSON_OBJECT('keywords', JSON_ARRAY('디지털 교육', '직업훈련', '이러닝', 'LMS')),
 'MANUAL',   'SUCCEEDED', '2026-08-10 09:12:00', '2026-08-10 09:13:44', 4, 4, 0, 0, 'vitaedu-VE101'),
(8002, 8001, JSON_OBJECT('keywords', JSON_ARRAY('디지털 교육', '직업훈련', '이러닝', 'LMS')),
 'SCHEDULED','SUCCEEDED', '2026-08-16 07:30:00', '2026-08-16 07:31:20', 3, 2, 1, 0, NULL);


-- ── 2. 공고 7 ────────────────────────────────────────────────────────
-- ⭐ 8001 은 직접 등록이다. 이 공고는 나라장터가 아니라 직업능력심사평가원 누리집에 뜨므로
--    수집 대상이 아니고 담당자가 손으로 등록하는 게 맞다.
--    crawl_source_id = 2(MANUAL) · owner_company_id = 3 · manual_dedup_key 필요.
--
-- ⚠️ base_amount·estimated_amount 는 NULL 이 정상이다.
--    이건 계약금액 칸이지 단가 칸이 아니다. 훈련비 단가(12,100원/h)와 최대 수강료는
--    AI 요약 amount_summary 로 들어간다. 여기 넣으면 목록에서 발주 금액처럼 보인다.
INSERT IGNORE INTO bid_notice
  (bid_notice_id, crawl_source_id, owner_company_id, external_id, notice_ord, manual_dedup_key,
   notice_type, notice_name, notice_agency, demand_agency,
   announced_at, bid_start_at, application_deadline_at, bid_deadline_at, opening_at,
   base_amount, estimated_amount, participation_qualification_text,
   contract_method, evaluation_method, source_url, has_attachment, notice_status,
   crawled_at, business_category_id, created_by) VALUES
(8001, 2, 3, 'KSQA-2026-53', '00', SHA2('vitaedu|KSQA-2026-53|2026년 K-디지털 기초역량훈련 심사 계획 공고', 256),
 '용역', '2026년 K-디지털 기초역량훈련 심사 계획 공고',
 '직업능력심사평가원', '한국기술교육대학교',
 '2026-07-31 09:00:00', '2026-08-03 10:00:00', '2026-08-25 18:00:00', '2026-08-25 18:00:00',
 '2026-11-11 00:00:00',
 NULL, NULL,
 '인증평가 3년 인증 이상 보유기관, 고등교육법상 대학, 설립 1년 이상이면서 매출 30억 또는 수료 300명 이상인 기관, KDT 선정·개시 실적 보유기관 중 하나',
 '심사 신청', '기본심사 후 과정심사(서면·현장) · 총점 60점 이상 선정',
 'https://www.ksqa.or.kr', 1, 'COLLECTED',
 NULL, 8001, 'vitaedu-VE101');

-- 나라장터 수집분 6 — 목록·필터·제외 화면을 채우는 곁들이
INSERT IGNORE INTO bid_notice
  (bid_notice_id, crawl_source_id, owner_company_id, external_id, notice_ord, manual_dedup_key,
   notice_type, notice_name, notice_agency, demand_agency,
   announced_at, bid_start_at, application_deadline_at, bid_deadline_at, opening_at,
   base_amount, estimated_amount, participation_qualification_text, region_limit_text,
   contract_method, evaluation_method, source_url, has_attachment, notice_status,
   crawled_at, business_category_id, created_by) VALUES

(8002, 1, NULL, 'KDTDEMO-20260806-001', '00', NULL,
 '용역', '2026년 시민 디지털 역량교육 운영 위탁',
 '○○광역시', '○○광역시 평생교육진흥원',
 '2026-08-06 10:00:00', '2026-08-06 10:00:00', '2026-08-20 10:00:00', '2026-08-20 10:00:00',
 '2026-08-21 11:00:00',
 148000000.00, 148000000.00, '직업능력개발훈련시설 또는 평생교육시설 등록 법인', '○○광역시 소재',
 '협상에 의한 계약', '기술평가 80 · 가격평가 20',
 'https://www.g2b.go.kr', 1, 'COLLECTED', '2026-08-10 09:12:30', 8002, NULL),

(8003, 1, NULL, 'KDTDEMO-20260807-002', '00', NULL,
 '용역', '공공기관 임직원 사이버교육 콘텐츠 개발',
 '한국○○공단', '한국○○공단 인재개발원',
 '2026-08-07 14:00:00', '2026-08-07 14:00:00', '2026-08-24 14:00:00', '2026-08-24 14:00:00',
 '2026-08-25 10:00:00',
 92000000.00, 92000000.00, '이러닝 콘텐츠 개발 실적 보유 업체', NULL,
 '협상에 의한 계약', '기술평가 90 · 가격평가 10',
 'https://www.g2b.go.kr', 1, 'COLLECTED', '2026-08-10 09:12:31', 8002, NULL),

(8004, 1, NULL, 'KDTDEMO-20260808-003', '00', NULL,
 '용역', '△△대학교 학습관리시스템(LMS) 고도화',
 '△△대학교', '△△대학교 정보전산원',
 '2026-08-08 09:00:00', '2026-08-08 09:00:00', '2026-08-27 09:00:00', '2026-08-27 09:00:00',
 '2026-08-28 10:00:00',
 310000000.00, 310000000.00, '소프트웨어사업자 신고 업체', NULL,
 '협상에 의한 계약', '기술평가 90 · 가격평가 10',
 'https://www.g2b.go.kr', 1, 'COLLECTED', '2026-08-10 09:12:32', 8003, NULL),

(8005, 1, NULL, 'KDTDEMO-20260809-004', '00', NULL,
 '물품', '초등학교 급식실 조리기구 구매',
 '□□교육지원청', '□□교육지원청',
 '2026-08-09 10:00:00', '2026-08-09 10:00:00', '2026-08-19 10:00:00', '2026-08-19 10:00:00',
 '2026-08-20 11:00:00',
 47000000.00, 47000000.00, '조리기구 제조·판매업 등록 업체', NULL,
 '일반경쟁', '최저가', 'https://www.g2b.go.kr', 0, 'COLLECTED', '2026-08-10 09:12:33', NULL, NULL),

(8006, 1, NULL, 'KDTDEMO-20260814-005', '00', NULL,
 '용역', '재직자 대상 생성형 AI 활용 교육 위탁',
 '◇◇테크노파크', '◇◇테크노파크 기업지원단',
 '2026-08-14 11:00:00', '2026-08-14 11:00:00', '2026-08-28 11:00:00', '2026-08-28 11:00:00',
 '2026-08-31 10:00:00',
 63000000.00, 63000000.00, '직업능력개발훈련시설 신고 법인', NULL,
 '협상에 의한 계약', '기술평가 80 · 가격평가 20',
 'https://www.g2b.go.kr', 1, 'COLLECTED', '2026-08-16 07:30:22', 8002, NULL),

(8007, 1, NULL, 'KDTDEMO-20260815-006', '00', NULL,
 '용역', '노후 전산장비 유지보수 단가계약',
 '◎◎연구원', '◎◎연구원 총무팀',
 '2026-08-15 09:30:00', '2026-08-15 09:30:00', '2026-08-26 09:30:00', '2026-08-26 09:30:00',
 '2026-08-27 10:00:00',
 21000000.00, 21000000.00, '정보통신공사업 등록 업체', NULL,
 '일반경쟁', '적격심사', 'https://www.g2b.go.kr', 0, 'COLLECTED', '2026-08-16 07:30:23', NULL, NULL);


-- ── 3. 공고 첨부 5 — 공고문 붙임 서식 ────────────────────────────────
-- 🚨 chk_bid_notice_attachment_source — source_url 과 storage_key 중 **정확히 하나만** 채워야 한다.
--    업로드형이므로 storage_key 만 넣고 source_url 은 NULL 이다. 둘 다 채우면 CHECK 위반으로 죽는다.
-- ⚠️ 파일 실물은 S3 에 없다. 목록·이름까지만 보여주고 열지 마라.
INSERT IGNORE INTO bid_notice_attachment
  (bid_notice_attachment_id, bid_notice_id, attachment_kind, attachment_order,
   file_name, source_url, storage_key, upload_status, size_bytes, mime_type) VALUES
(8001, 8001, 'NOTICE_SPEC', 1, '1. (공통) 훈련운영계획서.hwp',
 NULL, 'bidding/3/notices/8001/attachments/8001-plan-form.hwp', 'READY', 184320, 'application/x-hwp'),
(8002, 8001, 'NOTICE_SPEC', 2, '2. (공통) 훈련과정개요서 양식.xlsx',
 NULL, 'bidding/3/notices/8001/attachments/8002-course-form.xlsx', 'READY', 96256,
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'),
(8003, 8001, 'NOTICE_SPEC', 3, '3. (신청자격 3유형) 수료생 명단.xlsx',
 NULL, 'bidding/3/notices/8001/attachments/8003-graduates-form.xlsx', 'READY', 51200,
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'),
(8004, 8001, 'NOTICE_SPEC', 4, '4. (신규참여기관) 자체LMS보유 확인서.hwp',
 NULL, 'bidding/3/notices/8001/attachments/8004-lms-form.hwp', 'READY', 41984, 'application/x-hwp'),
(8005, 8001, 'NOTICE_SPEC', 5, '5. (협약체결기관) 협약서.hwp',
 NULL, 'bidding/3/notices/8001/attachments/8005-agreement-form.hwp', 'READY', 47104, 'application/x-hwp');

-- ⛔ 4번은 신규참여기관용이다. 비타에듀는 2025년 선정 실적이 있어 제출 대상이 아니다.
--    그래도 공고에 붙은 서식이므로 첨부에는 5건 다 있다.
--    제출 대상 여부는 04_blocks.sql 의 체크리스트에서 「해당 없음」으로 갈린다.


-- ── 4. AI 요약 1 — 확정본 ────────────────────────────────────────────
-- ⚠️ notice_snapshot·prompt·processing_attempt_id 는 전부 NOT NULL 이다.
-- ⚠️ project_id 는 여기서 NULL 이다 — project 가 아직 없다. 09_bid_review.sql 이 UPDATE 로 채운다.
INSERT IGNORE INTO bid_notice_summary
  (bid_notice_summary_id, company_id, project_id, bid_notice_id, parent_summary_id, revision_no,
   requested_by, prompt, notice_snapshot,
   overview_summary, amount_summary, schedule_summary, qualification_summary,
   task_summary, risk_summary,
   summary_status, processing_attempt_id, retry_count,
   confirmed, confirmed_by, confirmed_at, completed_at) VALUES
(8001, 3, NULL, 8001, NULL, 1,
 'vitaedu-VE101',
 '공고문과 붙임 서식을 근거로 신청 요건, 훈련비 상한, 일정, 심사 배점, 탈락 조건을 정리해 달라.',
 JSON_OBJECT('noticeName', '2026년 K-디지털 기초역량훈련 심사 계획 공고',
             'noticeAgency', '직업능력심사평가원',
             'applicationDeadlineAt', '2026-08-25T18:00:00'),
 '자체 개발한 원격훈련과정을 대상으로 IT 기초역량, AI 도구 활용, Pre-KDT 세 유형 중 하나로 신청하는 심사다. 선정되면 인정일로부터 1년간 운영할 수 있다.',
 '시간당 훈련비 단가는 12,100원이 상한이다. 최대 수강료는 IT 기초역량 25만원, AI 도구 활용 20만원, Pre-KDT 50만원이고 각각 자부담이 10퍼센트다. 단가를 넘겨 신청하면 조건부 적합 판정 대상이 된다.',
 '신청은 8월 3일 10시부터 8월 25일 18시까지다. 마감 뒤 8월 26일부터 28일까지만 수정과 보완이 된다. 심사는 9월부터 11월 초까지이고 결과는 11월 11일에 나온다. 선정 뒤 3개월 안에 개설하지 않으면 페널티 대상이다.',
 '우리는 세 번째 요건에 해당한다. 설립 1년이 넘었고 자비부담 비환급 과정 수료 인원이 300명을 넘는다. 이 요건으로 신청하면 수료생 명단을 함께 내야 한다. 인증등급이 없어 첫 번째 요건은 못 쓴다.',
 '전체 기관은 훈련운영계획서와 훈련과정개요서를 낸다. 세 번째 요건 기관은 수료생 명단이 추가된다. 협약서를 내면 참여기관 연계 가점 3점을 받을 수 있다. 서면심사는 운영역량 35점과 과정내용 65점이고 가점이 최대 8점이다.',
 '운영역량이 14점 미만이거나 과정내용이 26점 미만이면 그 자리에서 탈락한다. 영역별로 배점의 40퍼센트를 못 넘겨도 제외된다. 총점 60점을 넘어야 심의 대상이 된다. 자막은 미제공이거나 품질이 모자라면 조건부 적합으로 넘어간다.',
 'COMPLETED', '3f2c8a41-5d9e-4b17-9c02-7ae4d1b60f83', 0,
 1, 'vitaedu-VE103', '2026-08-04 16:20:00', '2026-08-04 11:42:00');


-- ── 5. 회사별 공고 상태 7 — 관심 2 · 제외 2 ──────────────────────────
-- ⭐ 이 행이 없으면 회사 3 의 공고 목록이 빈다. UNIQUE 는 (company_id, bid_notice_id).
-- ⚠️ is_favorite 는 notice_status 와 독립이다. 제외한 공고도 관심 상태를 유지할 수 있다.
INSERT IGNORE INTO company_bid_notice_state
  (company_bid_notice_state_id, company_id, bid_notice_id, notice_status, dismiss_reason,
   is_favorite, first_seen_run_id, last_seen_run_id, first_seen_at, last_seen_at) VALUES
-- ⭐ 메인 공고 — 직접 등록이라 수집 실행 이력이 없다 (run_id NULL 이 정상)
(8001, 3, 8001, 'IN_REVIEW', NULL, 1, NULL, NULL, '2026-08-03 10:40:00', '2026-08-16 09:05:00'),
(8002, 3, 8002, 'IN_REVIEW', NULL, 1, 8001, 8002, '2026-08-10 09:13:00', '2026-08-16 07:31:00'),
(8003, 3, 8003, 'COLLECTED', NULL, 0, 8001, 8002, '2026-08-10 09:13:00', '2026-08-16 07:31:00'),
(8004, 3, 8004, 'COLLECTED', NULL, 0, 8001, 8001, '2026-08-10 09:13:00', '2026-08-10 09:13:00'),
-- ⛔ 제외 2 — 사유가 남아 있어야 목록에서 왜 뺐는지 보인다
(8005, 3, 8005, 'DISMISSED', '교육 훈련과 무관한 물품 구매다', 0,
 8001, 8001, '2026-08-10 09:13:00', '2026-08-10 09:13:00'),
(8006, 3, 8006, 'COLLECTED', NULL, 0, 8002, 8002, '2026-08-16 07:31:00', '2026-08-16 07:31:00'),
(8007, 3, 8007, 'DISMISSED', '전산장비 유지보수라 훈련 사업과 관련이 없다', 0,
 8002, 8002, '2026-08-16 07:31:00', '2026-08-16 07:31:00');


-- ── 6. 상태 변경 이력 6 ──────────────────────────────────────────────
INSERT IGNORE INTO bid_notice_status_history
  (bid_notice_status_history_id, company_id, bid_notice_id,
   previous_status, changed_status, reason, changed_by, created_at) VALUES
(8001, 3, 8001, 'COLLECTED', 'IN_REVIEW', '신청 요건 검토 착수', 'vitaedu-VE101', '2026-08-03 10:40:00'),
(8002, 3, 8002, 'COLLECTED', 'IN_REVIEW', '기존 위탁 실적이 있어 검토한다', 'vitaedu-VE101', '2026-08-11 14:10:00'),
(8003, 3, 8005, 'COLLECTED', 'DISMISSED', '교육 훈련과 무관한 물품 구매다', 'vitaedu-VE102', '2026-08-10 15:22:00'),
(8004, 3, 8007, 'COLLECTED', 'DISMISSED', '전산장비 유지보수라 훈련 사업과 관련이 없다', 'vitaedu-VE102', '2026-08-16 09:40:00'),
(8005, 3, 8003, 'COLLECTED', 'COLLECTED', '콘텐츠 개발 인력이 8월에 심사 신청에 묶여 있어 보류한다', 'vitaedu-VE106', '2026-08-12 10:05:00'),
(8006, 3, 8004, 'COLLECTED', 'COLLECTED', '규모가 커 단독 수행이 어렵다. 공동수급 여부를 확인 중이다', 'vitaedu-VE103', '2026-08-12 10:12:00');


-- ── 7. 사내 문서 6 — AI 검토의 비교자료 ──────────────────────────────
-- ⚠️ company_document 는 file 과 다른 애그리거트다. file 은 프로젝트 소속이라 재사용이 안 된다.
--    회사 기준 자료(재정·소개·실적·인증)는 여기 둔다.
INSERT IGNORE INTO company_document
  (company_document_id, company_id, category, name, created_by) VALUES
(8001, 3, 'FINANCE',       '2025년 표준재무제표증명',        'vitaedu-VE112'),
(8002, 3, 'FINANCE',       '국세·지방세 납세증명서',          'vitaedu-VE112'),
(8003, 3, 'COMPANY_INTRO', '비타에듀 기관 소개서',            'vitaedu-VE112'),
(8004, 3, 'PERFORMANCE',   '2024~2025 훈련 운영실적 집계',    'vitaedu-VE112'),
(8005, 3, 'CERTIFICATE',   '사업자등록증',                    'vitaedu-VE112'),
(8006, 3, 'ETC',           '자체 LMS 기능 구성 자료',         'vitaedu-VE112');

-- ⚠️ version_no 는 문서 안에서 1부터 연속이어야 한다. 8004 만 3버전이다.
INSERT IGNORE INTO company_document_version
  (company_document_version_id, company_document_id, version_no, upload_status, storage_key,
   original_file_name, extension, mime_type, size_bytes, comment,
   uploaded_by, uploader_name, uploader_department, uploader_position, completed_at) VALUES
(8001, 8001, 1, 'COMPLETED', 'companies/3/documents/8001/v1.pdf',
 '표준재무제표증명_2025.pdf', 'pdf', 'application/pdf', 412000,
 '2025 귀속분 발급본', 'vitaedu-VE112', '운영관리자', '경영지원팀', '과장', '2026-08-03 11:20:00'),
(8002, 8002, 1, 'COMPLETED', 'companies/3/documents/8002/v1.pdf',
 '납세증명서_20260803.pdf', 'pdf', 'application/pdf', 138000,
 '유효기간이 신청기간을 덮는 발급본', 'vitaedu-VE112', '운영관리자', '경영지원팀', '과장', '2026-08-03 11:26:00'),
(8003, 8003, 1, 'COMPLETED', 'companies/3/documents/8003/v1.pdf',
 '비타에듀_기관소개서.pdf', 'pdf', 'application/pdf', 2870000,
 '조직도와 LMS 화면을 포함한 개정본', 'vitaedu-VE112', '운영관리자', '경영지원팀', '과장', '2026-07-20 09:10:00'),
(8004, 8004, 1, 'COMPLETED', 'companies/3/documents/8004/v1.xlsx',
 '훈련운영실적_2024_2025.xlsx', 'xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 88000,
 '2024년분까지 집계', 'vitaedu-VE112', '운영관리자', '경영지원팀', '과장', '2026-02-11 15:00:00'),
(8005, 8004, 2, 'COMPLETED', 'companies/3/documents/8004/v2.xlsx',
 '훈련운영실적_2024_2025.xlsx', 'xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 104000,
 '2025년 상반기 종료 과정 12건 추가', 'vitaedu-VE112', '운영관리자', '경영지원팀', '과장', '2026-07-08 17:30:00'),
(8006, 8004, 3, 'COMPLETED', 'companies/3/documents/8004/v3.xlsx',
 '훈련운영실적_2024_2025.xlsx', 'xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 121000,
 '공공지원분을 제외한 수료 941명 기준으로 재집계', 'vitaedu-VE112', '운영관리자', '경영지원팀', '과장', '2026-08-03 10:05:00'),
(8007, 8005, 1, 'COMPLETED', 'companies/3/documents/8005/v1.pdf',
 '사업자등록증.pdf', 'pdf', 'application/pdf', 96000,
 '개업연월일 확인용', 'vitaedu-VE112', '운영관리자', '경영지원팀', '과장', '2026-08-03 11:30:00'),
(8008, 8006, 1, 'COMPLETED', 'companies/3/documents/8006/v1.pdf',
 'LMS_기능구성.pdf', 'pdf', 'application/pdf', 3420000,
 '훈련생 모듈과 관리자 모듈 화면 캡처', 'vitaedu-VE108', '임채린', '플랫폼팀', '과장', '2026-08-05 13:40:00');


-- =====================================================================
-- 검증
-- =====================================================================
-- 1) 회사 3 의 공고 목록이 7건인가 (운영 DB 의 기존 수집분이 안 섞이나)
--    SELECT n.bid_notice_id, s.notice_status, s.is_favorite, n.notice_name
--    FROM company_bid_notice_state s JOIN bid_notice n USING (bid_notice_id)
--    WHERE s.company_id = 3 ORDER BY n.bid_notice_id;
--    기대: 7행 · IN_REVIEW 2 · DISMISSED 2 · 관심 2
--
-- 2) 첨부의 source_url / storage_key 가 정확히 하나만 차 있나 (0행이어야 정상)
--    SELECT bid_notice_attachment_id FROM bid_notice_attachment
--    WHERE bid_notice_id = 8001
--      AND NOT ((source_url IS NULL) XOR (storage_key IS NULL));
--
-- 3) 사내 문서 version_no 가 1부터 연속인가 (0행이어야 정상)
--    SELECT company_document_id FROM company_document_version
--    WHERE company_document_id BETWEEN 8001 AND 8006
--    GROUP BY company_document_id
--    HAVING COUNT(*) <> MAX(version_no) OR MIN(version_no) <> 1;
