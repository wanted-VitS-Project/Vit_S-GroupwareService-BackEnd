ALTER TABLE crawl_condition
    ADD COLUMN lookback_period VARCHAR(20) NOT NULL DEFAULT 'ONE_WEEK' AFTER enabled;
