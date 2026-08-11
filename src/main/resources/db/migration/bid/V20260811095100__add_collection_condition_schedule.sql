ALTER TABLE crawl_condition
    ADD COLUMN auto_collection_enabled BOOLEAN NOT NULL DEFAULT FALSE
    COMMENT '자동 수집 활성화 여부'
        AFTER enabled,

    ADD COLUMN schedule_type VARCHAR(20) NULL
        COMMENT '자동 수집 주기: DAILY, WEEKDAYS'
        AFTER auto_collection_enabled,

    ADD COLUMN scheduled_time TIME NULL
        COMMENT '자동 수집 실행 기준 시각'
        AFTER schedule_type,

    ADD COLUMN timezone VARCHAR(50) NULL
        COMMENT '자동 수집 기준 시간대'
        AFTER scheduled_time,

    ADD COLUMN next_run_at DATETIME NULL
        COMMENT '다음 자동 수집 실행 예정 시각'
        AFTER timezone,

    ADD COLUMN last_scheduled_at DATETIME NULL
        COMMENT '마지막 자동 수집 요청 생성 시각'
        AFTER next_run_at,

    ADD CONSTRAINT ck_crawl_condition_schedule_type
        CHECK (
            schedule_type IS NULL
            OR schedule_type IN ('DAILY', 'WEEKDAYS')
        ),

    ADD CONSTRAINT ck_crawl_condition_schedule_configuration
        CHECK (
            (
                auto_collection_enabled = FALSE
                AND schedule_type IS NULL
                AND scheduled_time IS NULL
                AND timezone IS NULL
                AND next_run_at IS NULL
            )
            OR
            (
                auto_collection_enabled = TRUE
                AND enabled = TRUE
                AND schedule_type IS NOT NULL
                AND scheduled_time IS NOT NULL
                AND timezone = 'Asia/Seoul'
                AND next_run_at IS NOT NULL
            )
        ),

    ADD INDEX idx_crawl_condition_schedule_due (
        auto_collection_enabled,
        enabled,
        deleted_at,
        next_run_at
    );