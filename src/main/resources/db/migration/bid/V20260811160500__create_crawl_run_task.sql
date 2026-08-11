CREATE TABLE crawl_run_task (
                                crawl_run_task_id BIGINT NOT NULL AUTO_INCREMENT,
                                crawl_run_id BIGINT NOT NULL,
                                notice_type VARCHAR(30) NOT NULL,
                                keyword VARCHAR(255) NOT NULL DEFAULT '',
                                region_code VARCHAR(50) NOT NULL DEFAULT '',
                                industry_code VARCHAR(50) NOT NULL DEFAULT '',
                                page_number INT NOT NULL,
                                task_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                attempt_id CHAR(36) NULL,
                                retry_count INT NOT NULL DEFAULT 0,
                                processing_started_at DATETIME NULL,
                                lease_expires_at DATETIME NULL,
                                collected_count INT NOT NULL DEFAULT 0,
                                inserted_count INT NOT NULL DEFAULT 0,
                                updated_count INT NOT NULL DEFAULT 0,
                                skipped_count INT NOT NULL DEFAULT 0,
                                error_code VARCHAR(100) NULL,
                                error_message VARCHAR(500) NULL,
                                created_at DATETIME NOT NULL,
                                updated_at DATETIME NOT NULL,
                                finished_at DATETIME NULL,
                                PRIMARY KEY (crawl_run_task_id),
                                CONSTRAINT fk_crawl_run_task_run
                                    FOREIGN KEY (crawl_run_id) REFERENCES crawl_run (crawl_run_id),
                                CONSTRAINT uk_crawl_run_task_combination UNIQUE (
                                                                                 crawl_run_id,
                                                                                 notice_type,
                                                                                 keyword,
                                                                                 region_code,
                                                                                 industry_code,
                                                                                 page_number
                                    )
);
