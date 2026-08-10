-- 입찰 공고 수집 조건이 참조할 나라장터 수집처 마스터를 등록한다.
INSERT INTO crawl_source (
    source_code,
    source_name,
    source_type,
    enabled,
    created_at
)
VALUES (
           'NARA',
           '나라장터',
           'OPEN_API',
           TRUE,
           CURRENT_TIMESTAMP
       )
    ON DUPLICATE KEY UPDATE
                         source_name = VALUES(source_name),
                         source_type = VALUES(source_type),
                         enabled = VALUES(enabled),
                         deleted_at = NULL,
                         updated_at = CURRENT_TIMESTAMP;