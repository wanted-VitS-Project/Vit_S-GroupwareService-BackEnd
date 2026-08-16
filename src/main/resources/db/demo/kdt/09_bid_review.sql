-- =====================================================================
-- KDT 09. 입찰 문서 검토 1 · 공고↔프로젝트 연결 마무리
-- ---------------------------------------------------------------------
-- 무엇: 공고 8001 과 사내 기준자료를 비교한 AI 검토 1건을 넣고,
--       그 결과로 만든 프로젝트(8001)를 검토와 확정 요약 양쪽에 연결한다.
-- 왜:   「공고를 보고 프로젝트를 만들었다」가 데이터로 이어져야 시연 동선이 끊기지 않는다.
--
-- 선행: 02_bid.sql · 03_project.sql
--
-- 🚨 이 파일이 02 에서 갈라져 나온 이유
--    project.bid_notice_id → bid_notice   (프로젝트가 공고를 문다)
--    bid_review.project_id → project      (검토가 프로젝트를 문다)
--    FK 방향이 반대라 한 파일에 넣으면 순서를 어떻게 잡아도 죽는다.
--
-- ⚠️ bid_review 는 prompt·processing_attempt_id 가 NOT NULL 이다.
-- ⚠️ project_id 는 UNIQUE 다. 한 프로젝트에 검토 두 건은 못 붙는다.
--
-- 되돌리기:
--   UPDATE bid_notice_summary SET project_id = NULL WHERE bid_notice_summary_id = 8001;
--   DELETE FROM bid_review WHERE bid_review_id = 8001;
-- =====================================================================


-- ── 1. 입찰 문서 검토 ────────────────────────────────────────────────
-- 공고문과 붙임 서식을, 02 에서 넣은 사내 문서 6건과 대조한 결과다.
-- ⚠️ result 는 화면이 그대로 렌더링하는 본문이다. 마크다운 표는 렌더링되지 않으므로
--    굵게와 목록, 빈 줄 문단만 쓴다.
INSERT IGNORE INTO bid_review
  (bid_review_id, company_id, bid_notice_id, requested_by, project_id,
   prompt, review_status, processing_attempt_id, retry_count,
   result, completed_at, expires_at) VALUES
(8001, 3, 8001, 'vitaedu-VE101', 8001,
 '첨부한 사내 자료를 근거로 이 공고의 신청 요건을 우리가 충족하는지, 지금 없는 서류가 무엇인지 정리해 달라.',
 'COMPLETED', '9b71d4c2-08ae-4f36-a5d1-2c6e7f930b45', 0,
 '**신청자격**

세 번째 요건에 해당한다. 사업자등록증상 개업연월일이 2021년이라 설립 1년 기준을 넘겼고, 훈련 운영실적 집계에서 공공지원분을 뺀 수료 인원이 941명이라 300명 기준도 넘는다.

매출 30억 기준은 표준재무제표상 못 넘는다. 인원 기준으로 신청하는 게 맞다.

첫 번째 요건은 인증평가 등급이 없어 쓸 수 없다. 네 번째 요건은 KDT 개시 실적이 없어 해당하지 않는다.

**재정건전성**

납세증명서에 국세와 지방세 체납이 없다. 표준재무제표상 자기자본이 양수이고 기업신용등급이 B라 완전자본잠식과 C등급 이하 어디에도 걸리지 않는다.

다만 납세증명서 유효기간이 신청기간 안에 끝난다. 다시 떼야 한다.

**제출서류**

- 훈련운영계획서와 훈련과정개요서는 전체기관 필수다
- 세 번째 요건으로 신청하므로 수료생 명단이 추가된다
- 자체 LMS 보유 확인서는 신규참여기관용이라 해당하지 않는다
- 협약서는 필수가 아니지만 내면 참여기관 연계 가점 3점을 노릴 수 있다

**지금 없는 것**

- 수료생 명단을 성명 마스킹 형식으로 다시 만들어야 한다
- 참여인력 학위와 경력, 자격증 증빙이 아직 모이지 않았다
- 자막 품질 3기준 자체점검 결과가 없다

**주의할 점**

운영역량이 14점 미만이거나 과정내용이 26점 미만이면 그 자리에서 제외된다. 영역별로 배점의 40퍼센트를 못 넘겨도 마찬가지다. 총점 60점을 넘겨야 심의 대상이 된다.

기관 소개서에 적힌 훈련 실적은 2024년까지만 반영돼 있다. 2025년 이후 실적으로 다시 집계해야 신청자격 근거가 된다.',
 '2026-08-03 15:40:00', '2026-09-02 15:40:00');


-- ── 2. 확정 요약에 프로젝트 연결 ─────────────────────────────────────
-- ⚠️ 02 에서는 project 가 아직 없어 NULL 로 뒀다. 여기서 채운다.
--    `.ai/api/bid.md` — 확정 요약은 bid_notice_summary.project_id 로 생성 프로젝트를 가리킨다.
UPDATE bid_notice_summary
   SET project_id = 8001
 WHERE bid_notice_summary_id = 8001;


-- =====================================================================
-- 검증
-- =====================================================================
-- 1) 공고에서 프로젝트까지 한 줄로 이어지나
--    SELECT n.bid_notice_id, n.notice_name, s.confirmed, r.review_status, p.project_id, p.name
--    FROM bid_notice n
--    LEFT JOIN bid_notice_summary s ON s.bid_notice_id = n.bid_notice_id
--    LEFT JOIN bid_review r         ON r.bid_notice_id = n.bid_notice_id
--    LEFT JOIN project p            ON p.bid_notice_id = n.bid_notice_id
--    WHERE n.bid_notice_id = 8001;
--    기대: 요약 확정 1 · 검토 COMPLETED · 프로젝트 8001
--
-- 2) 검토와 요약이 같은 프로젝트를 가리키나
--    SELECT r.project_id, s.project_id FROM bid_review r, bid_notice_summary s
--    WHERE r.bid_review_id = 8001 AND s.bid_notice_summary_id = 8001;
