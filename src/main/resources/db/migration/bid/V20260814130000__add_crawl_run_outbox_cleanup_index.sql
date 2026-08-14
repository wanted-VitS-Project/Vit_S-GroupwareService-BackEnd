-- PUBLISHED 보관 정리 배치(deletePublishedBefore)가 publish_status·published_at으로 걸러낸다.
-- 기존 idx_crawl_run_outbox_claim은 published_at을 포함하지 않아 이 쿼리에 못 쓰인다.
ALTER TABLE crawl_run_outbox
    ADD INDEX idx_crawl_run_outbox_cleanup (publish_status, published_at);
