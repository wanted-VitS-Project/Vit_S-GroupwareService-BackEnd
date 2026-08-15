-- 관심 목록(favorite=true) 조회가 기존 idx_company_bid_notice_state_list(company_id, notice_status, bid_notice_id)에
-- 잡히지 않아 회사 범위 내 인덱스 없이 필터링되고 있었다. notice_status와 별개로 조회되는 조건이라
-- 기존 인덱스를 건드리지 않고 전용 인덱스를 추가한다.
ALTER TABLE company_bid_notice_state
    ADD INDEX idx_company_bid_notice_state_favorite (company_id, is_favorite, bid_notice_id);
