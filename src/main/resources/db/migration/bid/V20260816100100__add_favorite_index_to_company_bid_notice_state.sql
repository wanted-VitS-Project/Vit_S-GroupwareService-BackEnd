-- 관심 목록(favorite=true) 조회가 기존 idx_company_bid_notice_state_list(company_id, notice_status, bid_notice_id)에
-- 잡히지 않아 회사 범위 내 인덱스 없이 필터링되고 있었다. 실제 조회는 notice_status로도 함께
-- 좁히므로(예: 삭제 제외) notice_status를 포함해야 인덱스만으로 커버된다.
ALTER TABLE company_bid_notice_state
    ADD INDEX idx_company_bid_notice_state_favorite (company_id, is_favorite, notice_status, bid_notice_id);
