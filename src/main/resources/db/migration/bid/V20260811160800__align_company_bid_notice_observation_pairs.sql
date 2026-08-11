-- 기존 상태 행의 최초·최종 관측 시각과 실행 ID를 같은 원본 행 기준으로 정렬합니다.
UPDATE company_bid_notice_state state_table
JOIN (
    SELECT
        ranked.company_id,
        ranked.bid_notice_id,
        MAX(CASE WHEN ranked.first_rank = 1 THEN ranked.crawl_run_id END) AS first_seen_run_id,
        MAX(CASE WHEN ranked.first_rank = 1 THEN ranked.created_at END) AS first_seen_at,
        MAX(CASE WHEN ranked.last_rank = 1 THEN ranked.crawl_run_id END) AS last_seen_run_id,
        MAX(CASE WHEN ranked.last_rank = 1 THEN ranked.created_at END) AS last_seen_at
    FROM (
        SELECT
            condition_table.company_id,
            raw.bid_notice_id,
            raw.crawl_run_id,
            raw.created_at,
            ROW_NUMBER() OVER (
                PARTITION BY condition_table.company_id, raw.bid_notice_id
                ORDER BY raw.created_at ASC, raw.bid_notice_raw_id ASC
            ) AS first_rank,
            ROW_NUMBER() OVER (
                PARTITION BY condition_table.company_id, raw.bid_notice_id
                ORDER BY raw.created_at DESC, raw.bid_notice_raw_id DESC
            ) AS last_rank
        FROM bid_notice_raw raw
        JOIN crawl_run run_table
          ON run_table.crawl_run_id = raw.crawl_run_id
        JOIN crawl_condition condition_table
          ON condition_table.crawl_condition_id = run_table.crawl_condition_id
        WHERE raw.deleted_at IS NULL
          AND raw.crawl_run_id IS NOT NULL
    ) ranked
    GROUP BY ranked.company_id, ranked.bid_notice_id
) observation
  ON observation.company_id = state_table.company_id
 AND observation.bid_notice_id = state_table.bid_notice_id
SET
    state_table.first_seen_run_id = observation.first_seen_run_id,
    state_table.first_seen_at = observation.first_seen_at,
    state_table.last_seen_run_id = observation.last_seen_run_id,
    state_table.last_seen_at = observation.last_seen_at;
