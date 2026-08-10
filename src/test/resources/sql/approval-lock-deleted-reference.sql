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
