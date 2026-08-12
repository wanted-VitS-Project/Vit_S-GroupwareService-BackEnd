-- 회사별 입찰 기준자료 파일 메타데이터
CREATE TABLE bid_reference_file (
                                    bid_reference_file_id BIGINT NOT NULL AUTO_INCREMENT,
                                    company_id BIGINT NOT NULL,
                                    file_name VARCHAR(255) NOT NULL
                                        COMMENT '사용자에게 표시할 원본 파일명',
                                    extension VARCHAR(20) NOT NULL
                                        COMMENT '소문자 파일 확장자',
                                    mime_type VARCHAR(100) NOT NULL,
                                    size_bytes BIGINT NOT NULL,
                                    storage_key VARCHAR(1000) NOT NULL
                                        COMMENT '회사별 입찰 기준자료 S3 객체 키',
                                    storage_key_hash BINARY(32)
                                        GENERATED ALWAYS AS (UNHEX(SHA2(storage_key, 256))) STORED
                                        COMMENT '긴 S3 객체 키의 유일성 검증용 SHA-256',

                                    upload_status VARCHAR(20) NOT NULL DEFAULT 'UPLOADING',
                                    index_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                    index_attempt_id CHAR(36) NULL
        COMMENT '현재 인덱싱 시도 UUID',
                                    index_retry_count INT NOT NULL DEFAULT 0,
                                    index_error_message VARCHAR(500) NULL,

                                    upload_expires_at DATETIME NULL
        COMMENT 'Presigned upload URL 만료 시각',
                                    completed_at DATETIME NULL
        COMMENT '업로드 완료 시각',
                                    indexed_at DATETIME NULL
        COMMENT '인덱싱 완료 시각',

                                    created_by VARCHAR(20) NOT NULL,
                                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
                                    deleted_at DATETIME NULL,

                                    PRIMARY KEY (bid_reference_file_id),

                                    CONSTRAINT uk_bid_reference_file_storage_key
                                        UNIQUE (storage_key_hash),

                                    CONSTRAINT fk_bid_reference_file_company
                                        FOREIGN KEY (company_id)
                                            REFERENCES company (company_id),

                                    CONSTRAINT fk_bid_reference_file_creator
                                        FOREIGN KEY (created_by)
                                            REFERENCES employee (user_id),

                                    CONSTRAINT chk_bid_reference_file_size
                                        CHECK (size_bytes BETWEEN 1 AND 52428800),

                                    CONSTRAINT chk_bid_reference_file_upload_status
                                        CHECK (upload_status IN (
                                                                 'UPLOADING',
                                                                 'COMPLETED',
                                                                 'FAILED'
                                            )),

                                    CONSTRAINT chk_bid_reference_file_index_status
                                        CHECK (index_status IN (
                                                                'PENDING',
                                                                'PROCESSING',
                                                                'COMPLETED',
                                                                'FAILED'
                                            )),

                                    CONSTRAINT chk_bid_reference_file_retry_count
                                        CHECK (
                                            index_retry_count >= 0
                                                AND index_retry_count <= 3
                                            ),

                                    KEY idx_bid_reference_file_company_active (
        company_id,
        deleted_at,
        created_at
    ),

                                    KEY idx_bid_reference_file_index_target (
        index_status,
        index_attempt_id
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='회사별 입찰 AI 검토 기준자료';


-- 기준자료에서 추출하고 임베딩한 문서 청크
CREATE TABLE bid_reference_chunk (
                                     bid_reference_chunk_id BIGINT NOT NULL AUTO_INCREMENT,
                                     bid_reference_file_id BIGINT NOT NULL,
                                     index_attempt_id CHAR(36) NOT NULL
                                         COMMENT '이 청크를 생성한 인덱싱 시도 UUID',
                                     chunk_index INT NOT NULL,
                                     page_number INT NULL,
                                     sheet_name VARCHAR(255) NULL,
                                     section_title VARCHAR(255) NULL,
                                     start_offset INT NULL,
                                     end_offset INT NULL,
                                     token_count INT NULL,

                                     chroma_id VARCHAR(150) NULL,
                                     excerpt VARCHAR(1000) NULL
        COMMENT '근거 표시용 청크 미리보기',
                                     embedding_model VARCHAR(100) NULL,
                                     embedding_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP,
                                     deleted_at DATETIME NULL,

                                     PRIMARY KEY (bid_reference_chunk_id),

                                     CONSTRAINT uk_bid_reference_chunk_attempt_index
                                         UNIQUE (
                                                 bid_reference_file_id,
                                                 index_attempt_id,
                                                 chunk_index
                                             ),

                                     CONSTRAINT uk_bid_reference_chunk_chroma
                                         UNIQUE (chroma_id),

                                     CONSTRAINT fk_bid_reference_chunk_file
                                         FOREIGN KEY (bid_reference_file_id)
                                             REFERENCES bid_reference_file (bid_reference_file_id)
                                             ON DELETE RESTRICT,

                                     CONSTRAINT chk_bid_reference_chunk_index
                                         CHECK (chunk_index >= 0),

                                     CONSTRAINT chk_bid_reference_chunk_embedding_status
                                         CHECK (embedding_status IN (
                                                                     'PENDING',
                                                                     'COMPLETED',
                                                                     'FAILED'
                                             )),

                                     KEY idx_bid_reference_chunk_active (
        bid_reference_file_id,
        deleted_at,
        chunk_index
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='입찰 기준자료 문서 청크와 임베딩 메타데이터';


-- 업로드 완료 후 인덱싱 및 삭제 정리를 Redis로 전달하는 Outbox
CREATE TABLE bid_reference_file_outbox (
                                           bid_reference_file_outbox_id BIGINT NOT NULL AUTO_INCREMENT,
                                           event_id CHAR(36) NOT NULL
                                               COMMENT 'Outbox 이벤트 UUID',
                                           bid_reference_file_id BIGINT NOT NULL,
                                           attempt_id CHAR(36) NOT NULL
                                               COMMENT '인덱싱 또는 삭제 작업 시도 UUID',
                                           event_type VARCHAR(50) NOT NULL
                                               COMMENT 'REFERENCE_FILE_INDEX_REQUESTED 또는 REFERENCE_FILE_DELETE_REQUESTED',
                                           payload JSON NOT NULL
                                               COMMENT '민감한 URL이나 저장소 키를 포함하지 않는 Worker 메시지',

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

                                           PRIMARY KEY (bid_reference_file_outbox_id),

                                           CONSTRAINT uk_bid_reference_file_outbox_event
                                               UNIQUE (event_id),

                                           CONSTRAINT uk_bid_reference_file_outbox_attempt
                                               UNIQUE (
                                                       bid_reference_file_id,
                                                       attempt_id,
                                                       event_type
                                                   ),

                                           CONSTRAINT fk_bid_reference_file_outbox_file
                                               FOREIGN KEY (bid_reference_file_id)
                                                   REFERENCES bid_reference_file (bid_reference_file_id)
                                                   ON DELETE RESTRICT,

                                           CONSTRAINT chk_bid_reference_file_outbox_status
                                               CHECK (publish_status IN (
                                                                         'PENDING',
                                                                         'PUBLISHED',
                                                                         'FAILED'
                                                   )),

                                           CONSTRAINT chk_bid_reference_file_outbox_attempt_count
                                               CHECK (
                                                   publish_attempt_count >= 0
                                                       AND publish_attempt_count <= 5
                                                   ),

                                           KEY idx_bid_reference_file_outbox_claim (
        publish_status,
        available_at,
        lock_expires_at
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='입찰 기준자료 인덱싱·삭제 작업 발행 Outbox';
