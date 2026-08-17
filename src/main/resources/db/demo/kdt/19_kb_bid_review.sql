-- =====================================================================
-- KB 19. 입찰 문서 검토 1 · 공고↔프로젝트 연결 마무리
-- 🚨 project.bid_notice_id→bid_notice 와 bid_review.project_id→project 는 FK 방향이 반대다.
--    그래서 12(공고)·13(프로젝트)·19(검토연결) 세 조각으로 나눴다.
-- ⚠️ project_id 는 UNIQUE. bid_review 는 prompt·processing_attempt_id 가 NOT NULL.
-- 되돌리기: UPDATE bid_notice_summary SET project_id=NULL WHERE bid_notice_summary_id=8011;
--           DELETE FROM bid_review WHERE bid_review_id=8011;
-- =====================================================================


INSERT IGNORE INTO bid_review
  (bid_review_id, company_id, bid_notice_id, requested_by, project_id,
   prompt, review_status, processing_attempt_id, retry_count,
   result, completed_at, expires_at) VALUES
(8011, 3, 8011, 'vitaedu-VE101', 8011,
 '첨부한 사내 자료를 근거로 이 공고의 참가자격을 우리가 충족하는지, 지금 없는 서류가 무엇인지 정리해 달라.',
 'COMPLETED', 'c4a8f5e1-3b27-4d90-8f6a-1e5d2c7b904a', 0,
 '**신청자격**

네 요건을 모두 충족한다. 사업자등록증에 교육서비스업이 명시돼 있고 개업연월일이 2021년이라 설립 3년 기준을 넘겼다.

국세와 지방세 완납증명에 체납이 없다. 2023년부터 2025년까지 디지털 교육 운영실적이 실적 집계표에 정리돼 있다.

**제출서류**

- 제안서 6부와 요약본은 필수다
- 최근 3년 재무상태비교표와 감사보고서를 별지1 형식으로 낸다
- 청렴계약이행 확약서와 가격제안서, 위임장, 개인정보동의서를 별지 서식으로 낸다
- 입찰보증금은 입찰금액의 5퍼센트 이상으로 낸다

**지금 없는 것**

- 최근 3년 재무비율을 별지1 서식으로 다시 계산해야 한다
- 4대보험 완납증명을 4개 항목이 한 장에 보이게 다시 떼야 한다
- 영업담당자 정과 부의 개인정보동의서와 신분증 사본이 아직 모이지 않았다

**주의할 점**

기술능력 평가에서 최고점과 최저점을 뺀 산술평균이 배점의 80퍼센트에 못 미치거나 종합 점수가 85점에 못 미치면 협상적격에서 빠진다. 협상적격자 중 상위 두 곳만 우선협상대상이 된다.

가격 배점은 고용보험 환급과정 비율이 40퍼센트를 넘어야 만점이다. 비환급 과정은 환급금에 상당하는 할인율을 제시해야 환급과정으로 인정된다.',
 '2025-11-21 15:40:00', '2025-12-21 15:40:00');


-- 확정 요약에 프로젝트 연결 (12 에서는 project 가 없어 NULL 이었다)
UPDATE bid_notice_summary SET project_id = 8011 WHERE bid_notice_summary_id = 8011;
