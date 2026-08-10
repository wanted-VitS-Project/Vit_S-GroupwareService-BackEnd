DROP TABLE IF EXISTS approval_document;
DROP TABLE IF EXISTS approval_revision;
DROP TABLE IF EXISTS approval;
DROP TABLE IF EXISTS file_version;

CREATE TABLE file_version (
    file_version_id BIGINT PRIMARY KEY,
    file_id BIGINT NOT NULL
);

CREATE TABLE approval (
    approval_id BIGINT PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    deleted_at TIMESTAMP
);

CREATE TABLE approval_revision (
    approval_revision_id BIGINT PRIMARY KEY,
    approval_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    deleted_at TIMESTAMP
);

CREATE TABLE approval_document (
    approval_document_id BIGINT PRIMARY KEY,
    approval_revision_id BIGINT NOT NULL,
    file_version_id BIGINT NOT NULL,
    deleted_at TIMESTAMP
);

INSERT INTO file_version (file_version_id, file_id) VALUES (10, 1);
INSERT INTO approval (approval_id, status, deleted_at)
VALUES (100, 'CANCELED', TIMESTAMP '2026-08-10 17:00:00');
INSERT INTO approval_revision (approval_revision_id, approval_id, title, deleted_at)
VALUES (200, 100, '삭제된 품의', TIMESTAMP '2026-08-10 17:00:00');
INSERT INTO approval_document (approval_document_id, approval_revision_id, file_version_id, deleted_at)
VALUES (300, 200, 10, TIMESTAMP '2026-08-10 17:00:00');

-- file_id = 2 : 기안자가 DRAFT 에서 연결을 스스로 해제한 문서 (APR-007).
-- 문서만 삭제되고 회차는 살아 있어서 파일 잠금을 풀어야 하는 케이스다.
INSERT INTO file_version (file_version_id, file_id) VALUES (11, 2);
INSERT INTO approval (approval_id, status, deleted_at)
VALUES (101, 'DRAFT', NULL);
INSERT INTO approval_revision (approval_revision_id, approval_id, title, deleted_at)
VALUES (201, 101, '작성 중 품의', NULL);
INSERT INTO approval_document (approval_document_id, approval_revision_id, file_version_id, deleted_at)
VALUES (301, 201, 11, TIMESTAMP '2026-08-10 18:00:00');
