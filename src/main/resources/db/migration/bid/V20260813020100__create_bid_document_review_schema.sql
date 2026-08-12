-- 입찰 문서 비교 검토
CREATE TABLE bid_review (
                            bid_review_id BIGINT NOT NULL AUTO_INCREMENT,
                            company_id BIGINT NOT NULL,
                            bid_notice_id BIGINT NOT NULL,
                            requested_by VARCHAR(20) NOT NULL,
                            project_id BIGINT NULL
        COMMENT '검토 결과로 생성한 프로젝트',

                            prompt TEXT NOT NULL,
                            review_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                            processing_attempt_id CHAR(36) NOT NULL,
                            retry_count INT NOT NULL DEFAULT 0,

                            result LONGTEXT NULL,
                            error_code VARCHAR(100) NULL,
                            error_message VARCHAR(500) NULL,

                            completed_at DATETIME NULL,
                            expires_at DATETIME NULL
        COMMENT '프로젝트 미귀속 임시파일 정리 예정 시각',
                            abandoned_at DATETIME NULL,
                            cleanup_started_at DATETIME NULL,
                            cleanup_completed_at DATETIME NULL,

                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,

    -- 진행 중인 검토만 동일 사용자·공고 중복을 차단합니다.
                            active_processing_marker TINYINT
                                GENERATED ALWAYS AS (
                                    CASE
                                        WHEN review_status IN ('PENDING', 'PROCESSING') THEN 1
                                        ELSE NULL
                                        END
                                    ) STORED,

                            PRIMARY KEY (bid_review_id),

                            CONSTRAINT fk_bid_review_company
                                FOREIGN KEY (company_id)
                                    REFERENCES company (company_id),

                            CONSTRAINT fk_bid_review_notice
                                FOREIGN KEY (bid_notice_id)
                                    REFERENCES bid_notice (bid_notice_id),

                            CONSTRAINT fk_bid_review_requester
                                FOREIGN KEY (requested_by)
                                    REFERENCES employee (user_id),

                            CONSTRAINT fk_bid_review_project
                                FOREIGN KEY (project_id)
                                    REFERENCES project (project_id),

                            CONSTRAINT uk_bid_review_active_processing
                                UNIQUE (
                                        company_id,
                                        bid_notice_id,
                                        requested_by,
                                        active_processing_marker
                                    ),

                            CONSTRAINT uk_bid_review_project
                                UNIQUE (project_id),

                            CONSTRAINT chk_bid_review_status
                                CHECK (review_status IN (
                                                         'PENDING',
                                                         'PROCESSING',
                                                         'COMPLETED',
                                                         'FAILED',
                                                         'ABANDONED',
                                                         'EXPIRED'
                                    )),

                            CONSTRAINT chk_bid_review_retry_count
                                CHECK (retry_count BETWEEN 0 AND 3),

                            CONSTRAINT chk_bid_review_prompt_length
                                CHECK (CHAR_LENGTH(prompt) BETWEEN 1 AND 3000),

                            CONSTRAINT chk_bid_review_project_state
                                CHECK (
                                    project_id IS NULL
                                        OR review_status = 'COMPLETED'
                                    ),

                            KEY idx_bid_review_company_history (
        company_id,
        bid_notice_id,
        requested_by,
        created_at
    ),

                            KEY idx_bid_review_worker_attempt (
        review_status,
        processing_attempt_id
    ),

                            KEY idx_bid_review_expiration (
        review_status,
        expires_at,
        project_id
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='입찰 공고 첨부와 사내 기준자료 비교 검토';


-- 검토 요청 당시 선택한 문서 스냅샷과 처리 상태
CREATE TABLE bid_review_document (
                                     bid_review_document_id BIGINT NOT NULL AUTO_INCREMENT,
                                     bid_review_id BIGINT NOT NULL,

                                     document_role VARCHAR(30) NOT NULL
                                         COMMENT 'BID_ATTACHMENT 또는 INTERNAL_REFERENCE',
                                     bid_notice_attachment_id BIGINT NULL,
                                     bid_reference_file_id BIGINT NULL,

                                     file_name VARCHAR(500) NOT NULL
                                         COMMENT '검토 요청 당시 파일명 스냅샷',
                                     processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

                                     temporary_storage_key VARCHAR(1000) NULL
        COMMENT '공고 첨부를 검토할 때만 사용하는 임시 S3 객체 키',
                                     file_size BIGINT NULL,
                                     mime_type VARCHAR(100) NULL,
                                     processing_error_message VARCHAR(500) NULL,

                                     promoted_file_id BIGINT NULL
        COMMENT '프로젝트 귀속 후 파일 도메인 file ID',
                                     promoted_file_version_id BIGINT NULL
        COMMENT '프로젝트 귀속 후 파일 버전 ID',
                                     promoted_at DATETIME NULL,
                                     deleted_at DATETIME NULL
        COMMENT '임시 객체와 파생 데이터 정리 완료 시각',

                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP,

                                     PRIMARY KEY (bid_review_document_id),

                                     CONSTRAINT fk_bid_review_document_review
                                         FOREIGN KEY (bid_review_id)
                                             REFERENCES bid_review (bid_review_id)
                                             ON DELETE RESTRICT,

                                     CONSTRAINT fk_bid_review_document_attachment
                                         FOREIGN KEY (bid_notice_attachment_id)
                                             REFERENCES bid_notice_attachment (bid_notice_attachment_id)
                                             ON DELETE RESTRICT,

                                     CONSTRAINT fk_bid_review_document_reference
                                         FOREIGN KEY (bid_reference_file_id)
                                             REFERENCES bid_reference_file (bid_reference_file_id)
                                             ON DELETE RESTRICT,

                                     CONSTRAINT fk_bid_review_document_file
                                         FOREIGN KEY (promoted_file_id)
                                             REFERENCES file (file_id),

                                     CONSTRAINT fk_bid_review_document_file_version
                                         FOREIGN KEY (promoted_file_version_id)
                                             REFERENCES file_version (file_version_id),

                                     CONSTRAINT uk_bid_review_document_attachment
                                         UNIQUE (bid_review_id, bid_notice_attachment_id),

                                     CONSTRAINT uk_bid_review_document_reference
                                         UNIQUE (bid_review_id, bid_reference_file_id),

                                     CONSTRAINT chk_bid_review_document_role
                                         CHECK (document_role IN (
                                                                  'BID_ATTACHMENT',
                                                                  'INTERNAL_REFERENCE'
                                             )),

                                     CONSTRAINT chk_bid_review_document_source
                                         CHECK (
                                             (
                                                 document_role = 'BID_ATTACHMENT'
                                                     AND bid_notice_attachment_id IS NOT NULL
                                                     AND bid_reference_file_id IS NULL
                                                 )
                                                 OR
                                             (
                                                 document_role = 'INTERNAL_REFERENCE'
                                                     AND bid_notice_attachment_id IS NULL
                                                     AND bid_reference_file_id IS NOT NULL
                                                 )
                                             ),

                                     CONSTRAINT chk_bid_review_document_status
                                         CHECK (processing_status IN (
                                                                      'PENDING',
                                                                      'DOWNLOADING',
                                                                      'READY',
                                                                      'FAILED',
                                                                      'PROMOTED',
                                                                      'DELETED'
                                             )),

                                     CONSTRAINT chk_bid_review_document_size
                                         CHECK (
                                             file_size IS NULL
                                                 OR file_size BETWEEN 1 AND 52428800
                                             ),

                                     CONSTRAINT chk_bid_review_document_promotion
                                         CHECK (
                                             (
                                                 processing_status = 'PROMOTED'
                                                     AND document_role = 'BID_ATTACHMENT'
                                                     AND promoted_file_id IS NOT NULL
                                                     AND promoted_file_version_id IS NOT NULL
                                                     AND promoted_at IS NOT NULL
                                                 )
                                                 OR processing_status <> 'PROMOTED'
                                             ),

                                     KEY idx_bid_review_document_review (
        bid_review_id,
        document_role,
        processing_status
    ),

                                     KEY idx_bid_review_document_temp_cleanup (
        processing_status,
        deleted_at
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='입찰 문서 검토의 선택 문서와 임시 저장 상태';


-- AI 검토 결과 근거
CREATE TABLE bid_review_citation (
                                     bid_review_citation_id BIGINT NOT NULL AUTO_INCREMENT,
                                     bid_review_id BIGINT NOT NULL,
                                     bid_review_document_id BIGINT NOT NULL,

                                     rank_order INT NOT NULL,
                                     file_name VARCHAR(500) NOT NULL
                                         COMMENT '검토 당시 파일명 스냅샷',
                                     page_number INT NULL,
                                     sheet_name VARCHAR(255) NULL,
                                     excerpt VARCHAR(1000) NOT NULL,

                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     PRIMARY KEY (bid_review_citation_id),

                                     CONSTRAINT fk_bid_review_citation_review
                                         FOREIGN KEY (bid_review_id)
                                             REFERENCES bid_review (bid_review_id)
                                             ON DELETE RESTRICT,

                                     CONSTRAINT fk_bid_review_citation_document
                                         FOREIGN KEY (bid_review_document_id)
                                             REFERENCES bid_review_document (bid_review_document_id)
                                             ON DELETE RESTRICT,

                                     CONSTRAINT uk_bid_review_citation_rank
                                         UNIQUE (bid_review_id, rank_order),

                                     CONSTRAINT chk_bid_review_citation_rank
                                         CHECK (rank_order >= 1),

                                     KEY idx_bid_review_citation_document (
        bid_review_document_id
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='입찰 문서 비교 검토 결과 근거';


-- 검토 실행과 임시 데이터 정리 작업을 Redis로 전달하는 Outbox
CREATE TABLE bid_review_outbox (
                                   bid_review_outbox_id BIGINT NOT NULL AUTO_INCREMENT,
                                   event_id CHAR(36) NOT NULL,
                                   bid_review_id BIGINT NOT NULL,
                                   attempt_id CHAR(36) NOT NULL,

                                   event_type VARCHAR(50) NOT NULL
                                       COMMENT 'BID_REVIEW_REQUESTED 또는 BID_REVIEW_CLEANUP_REQUESTED',
                                   payload JSON NOT NULL,

                                   publish_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                   publish_attempt_count INT NOT NULL DEFAULT 0,
                                   available_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   lock_owner VARCHAR(100) NULL,
                                   lock_expires_at DATETIME NULL,
                                   published_at DATETIME NULL,
                                   last_error_message VARCHAR(500) NULL,

                                   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,

                                   PRIMARY KEY (bid_review_outbox_id),

                                   CONSTRAINT uk_bid_review_outbox_event
                                       UNIQUE (event_id),

                                   CONSTRAINT uk_bid_review_outbox_attempt
                                       UNIQUE (
                                               bid_review_id,
                                               attempt_id,
                                               event_type
                                           ),

                                   CONSTRAINT fk_bid_review_outbox_review
                                       FOREIGN KEY (bid_review_id)
                                           REFERENCES bid_review (bid_review_id)
                                           ON DELETE RESTRICT,

                                   CONSTRAINT chk_bid_review_outbox_event_type
                                       CHECK (event_type IN (
                                                             'BID_REVIEW_REQUESTED',
                                                             'BID_REVIEW_CLEANUP_REQUESTED'
                                           )),

                                   CONSTRAINT chk_bid_review_outbox_status
                                       CHECK (publish_status IN (
                                                                 'PENDING',
                                                                 'PUBLISHED',
                                                                 'FAILED'
                                           )),

                                   CONSTRAINT chk_bid_review_outbox_attempt_count
                                       CHECK (
                                           publish_attempt_count >= 0
                                               AND publish_attempt_count <= 5
                                           ),

                                   KEY idx_bid_review_outbox_claim (
        publish_status,
        available_at,
        lock_expires_at
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='입찰 문서 검토·임시파일 정리 작업 발행 Outbox';
