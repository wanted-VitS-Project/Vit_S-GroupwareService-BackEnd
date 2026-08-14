-- 회사 공용 관심 목록. 어느 직원이 등록·해제해도 같은 회사 전원에게 동일하게 보인다
-- (notice_status와 독립적인 별도 플래그 - 제외된 공고도 관심 등록 상태를 유지할 수 있다).
ALTER TABLE company_bid_notice_state
    ADD COLUMN is_favorite TINYINT(1) NOT NULL DEFAULT 0 AFTER dismiss_reason;
