-- =====================================================================
-- 멀티테넌트 Phase 1 · P1-2a — project 에 company_id
-- =====================================================================
-- 대상: project (루트). stage·step·block·재무·이미지·파일은 자식이라 손대지 않는다
-- (project 를 타고 회사가 결정된다).
--
-- ⚠️ DEFAULT 1 은 "임시"다. 회사가 기본회사(1) 하나뿐인 동안 기존 INSERT 가 company_id 를
--    안 넘겨도 1 로 채워져 그대로 동작한다. 생성 스탬핑 코드를 넣은 뒤 별도 마이그레이션으로
--    반드시 제거해야 한다 — 안 그러면 스탬핑을 빠뜨린 INSERT 가 조용히 1번 회사로 찍힌다.
--
-- 백필은 하지 않는다. 회사가 아직 1개뿐이라 기존 전 건의 정답이 1 이고, DEFAULT 1 이 그 값을 채운다.
--
-- 공고 UNIQUE 는 회사 스코프로 교체한다. 공고(bid_notice)는 전 회사 공용이라
-- 같은 공고로 회사 A·B 가 각자 프로젝트를 만들 수 있어야 한다 — 전역 UNIQUE 면 두 번째 회사가 막힌다.
--   · 새 UNIQUE 의 선두 컬럼을 bid_notice_id 로 둔다. fk_project_bid_notice 가 이 인덱스를 쓰므로
--     company_id 를 앞에 두면 FK 용 인덱스가 사라져 DROP INDEX 가 거부된다(1553).

ALTER TABLE project
  ADD COLUMN company_id BIGINT NOT NULL DEFAULT 1 COMMENT '회사(테넌트) · 스탬핑 적용 후 DEFAULT 제거' AFTER project_id,
  ADD KEY idx_project_company_deleted (company_id, deleted_at),
  ADD CONSTRAINT fk_project_company
    FOREIGN KEY (company_id) REFERENCES company (company_id);

ALTER TABLE project
  ADD UNIQUE KEY uk_project_bid_notice_company (bid_notice_id, company_id),
  DROP INDEX uk_project_bid_notice;
