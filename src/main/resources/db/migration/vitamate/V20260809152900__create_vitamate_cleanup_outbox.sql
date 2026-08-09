CREATE TABLE vitamate_cleanup_job (
    cleanup_job_id BIGINT NOT NULL AUTO_INCREMENT,
    cleanup_key CHAR(36) NOT NULL,
    file_id BIGINT NOT NULL COMMENT '물리 삭제된 파일 ID 스냅샷. FK를 두지 않는다',
    file_version_ids JSON NOT NULL COMMENT 'ChromaDB 정리 대상 파일 버전 ID 배열',
    cleanup_status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    current_attempt_id CHAR(36) NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME NULL,
    deleted_vector_count INT NOT NULL DEFAULT 0,
    last_error_code VARCHAR(100) NULL,
    last_error_message VARCHAR(500) NULL,
    processing_started_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (cleanup_job_id),
    CONSTRAINT uk_vitamate_cleanup_job_key UNIQUE (cleanup_key),
    CONSTRAINT chk_vitamate_cleanup_job_status
        CHECK (cleanup_status IN (
            'WAITING',
            'PUBLISHED',
            'PROCESSING',
            'RETRY_WAIT',
            'COMPLETED',
            'DEAD_LETTER'
        )),
    CONSTRAINT chk_vitamate_cleanup_job_attempt_count
        CHECK (attempt_count >= 0),
    CONSTRAINT chk_vitamate_cleanup_job_deleted_vector_count
        CHECK (deleted_vector_count >= 0),
    KEY idx_vitamate_cleanup_job_file (file_id),
    KEY idx_vitamate_cleanup_job_dispatch (cleanup_status, next_retry_at),
    KEY idx_vitamate_cleanup_job_stale (cleanup_status, processing_started_at)
) COMMENT '파일 영구삭제 후 ChromaDB 파생 데이터 정리 작업';

CREATE TABLE vitamate_cleanup_outbox (
    cleanup_outbox_id BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    cleanup_job_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload JSON NOT NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    publish_attempt_count INT NOT NULL DEFAULT 0,
    available_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_owner VARCHAR(100) NULL,
    lock_expires_at DATETIME NULL,
    published_at DATETIME NULL,
    last_error_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (cleanup_outbox_id),
    CONSTRAINT uk_vitamate_cleanup_outbox_event UNIQUE (event_id),
    CONSTRAINT fk_vitamate_cleanup_outbox_job
        FOREIGN KEY (cleanup_job_id)
        REFERENCES vitamate_cleanup_job (cleanup_job_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_vitamate_cleanup_outbox_status
        CHECK (publish_status IN ('PENDING', 'PUBLISHED')),
    CONSTRAINT chk_vitamate_cleanup_outbox_attempt_count
        CHECK (publish_attempt_count >= 0),
    KEY idx_vitamate_cleanup_outbox_job (cleanup_job_id),
    KEY idx_vitamate_cleanup_outbox_claim (
        publish_status,
        available_at,
        lock_expires_at
    )
) COMMENT '비타메이트 ChromaDB 정리 Redis 발행 outbox';
