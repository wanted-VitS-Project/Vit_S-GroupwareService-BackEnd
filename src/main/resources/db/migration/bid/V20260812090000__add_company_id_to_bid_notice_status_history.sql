-- bid_notice_status_history 를 회사 격리(테넌트) 대상으로 확정한다.
-- 도메인 모델(BidNoticeStatusHistory)·엔티티가 companyId 를 필수로 요구하나 스키마에 컬럼이 없어
-- Hibernate ddl-auto=validate 가 기동 시 실패했다. (선행 빈 마이그레이션 190001 은 no-op 로 이미 적용되어
--  수정할 수 없으므로, 컬럼 추가는 상위 버전의 본 마이그레이션에서 수행한다.)
--
-- 기존 행은 임시 DEFAULT 1 로 채운 뒤 작업자(changed_by)의 소속 회사로 귀속하고 DEFAULT 를 제거한다.
-- (crawl_condition.company_id 백필과 동일한 패턴)

ALTER TABLE bid_notice_status_history
    ADD COLUMN company_id BIGINT NOT NULL DEFAULT 1
        COMMENT '상태 변경 이력을 소유한 회사'
        AFTER bid_notice_status_history_id,
    ADD KEY idx_bid_notice_status_history_company (company_id, bid_notice_id),
    ADD CONSTRAINT fk_bid_notice_status_history_company
        FOREIGN KEY (company_id) REFERENCES company (company_id);

UPDATE bid_notice_status_history h
JOIN employee e ON e.user_id = h.changed_by
SET h.company_id = e.company_id
WHERE h.changed_by IS NOT NULL;

ALTER TABLE bid_notice_status_history
    ALTER COLUMN company_id DROP DEFAULT;
