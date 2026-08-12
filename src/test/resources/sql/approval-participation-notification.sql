DROP TABLE IF EXISTS step_permission;
DROP TABLE IF EXISTS project_member;
DROP TABLE IF EXISTS account;
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS block;
DROP TABLE IF EXISTS step;
DROP TABLE IF EXISTS project;

CREATE TABLE project (
    project_id BIGINT PRIMARY KEY,
    deleted_at TIMESTAMP
);

CREATE TABLE step (
    step_id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    deleted_at TIMESTAMP
);

CREATE TABLE block (
    block_id BIGINT PRIMARY KEY,
    step_id BIGINT NOT NULL,
    deleted_at TIMESTAMP
);

CREATE TABLE employee (
    user_id VARCHAR(30) PRIMARY KEY,
    company_id BIGINT NOT NULL,
    resigned_at DATE,
    deleted_at TIMESTAMP
);

CREATE TABLE account (
    user_id VARCHAR(30) PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL,
    deleted_at TIMESTAMP
);

CREATE TABLE project_member (
    project_id BIGINT NOT NULL,
    user_id VARCHAR(30) NOT NULL,
    permission VARCHAR(20) NOT NULL,
    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE step_permission (
    step_id BIGINT NOT NULL,
    user_id VARCHAR(30) NOT NULL,
    permission VARCHAR(20) NOT NULL,
    PRIMARY KEY (step_id, user_id)
);

INSERT INTO project (project_id, deleted_at) VALUES (1, NULL);
INSERT INTO step (step_id, project_id, deleted_at) VALUES (5, 1, NULL);
INSERT INTO block (block_id, step_id, deleted_at) VALUES (10, 5, NULL);

INSERT INTO employee (user_id, company_id, resigned_at, deleted_at) VALUES
    ('EMP_ELIGIBLE', 1, NULL, NULL),
    ('EMP_STEP_EDITOR', 1, NULL, NULL),
    ('EMP_PROJECT_NONE', 1, NULL, NULL),
    ('EMP_STEP_NONE', 1, NULL, NULL),
    ('EMP_VIEWER', 1, NULL, NULL),
    ('EMP_ADMIN', 1, NULL, NULL),
    ('EMP_INACTIVE', 1, NULL, NULL),
    ('EMP_ACCOUNT_DELETED', 1, NULL, NULL),
    ('EMP_RESIGNED', 1, '2026-08-11', NULL),
    ('EMP_DELETED', 1, NULL, '2026-08-11 12:00:00'),
    ('EMP_OTHER_COMPANY', 2, NULL, NULL);

INSERT INTO account (user_id, status, role, deleted_at) VALUES
    ('EMP_ELIGIBLE', 'ACTIVE', 'MEMBER', NULL),
    ('EMP_STEP_EDITOR', 'ACTIVE', 'MEMBER', NULL),
    ('EMP_PROJECT_NONE', 'ACTIVE', 'MEMBER', NULL),
    ('EMP_STEP_NONE', 'ACTIVE', 'MEMBER', NULL),
    ('EMP_VIEWER', 'ACTIVE', 'MEMBER', NULL),
    ('EMP_ADMIN', 'ACTIVE', 'ADMIN', NULL),
    ('EMP_INACTIVE', 'INACTIVE', 'MEMBER', NULL),
    ('EMP_ACCOUNT_DELETED', 'ACTIVE', 'MEMBER', '2026-08-11 12:00:00'),
    ('EMP_RESIGNED', 'ACTIVE', 'MEMBER', NULL),
    ('EMP_DELETED', 'ACTIVE', 'MEMBER', NULL),
    ('EMP_OTHER_COMPANY', 'ACTIVE', 'MEMBER', NULL);

INSERT INTO project_member (project_id, user_id, permission) VALUES
    (1, 'EMP_ELIGIBLE', 'EDITOR'),
    (1, 'EMP_STEP_EDITOR', 'VIEWER'),
    (1, 'EMP_PROJECT_NONE', 'NONE'),
    (1, 'EMP_STEP_NONE', 'EDITOR'),
    (1, 'EMP_VIEWER', 'VIEWER'),
    (1, 'EMP_ADMIN', 'EDITOR'),
    (1, 'EMP_INACTIVE', 'EDITOR'),
    (1, 'EMP_ACCOUNT_DELETED', 'EDITOR'),
    (1, 'EMP_RESIGNED', 'EDITOR'),
    (1, 'EMP_DELETED', 'EDITOR'),
    (1, 'EMP_OTHER_COMPANY', 'EDITOR');

INSERT INTO step_permission (step_id, user_id, permission) VALUES
    (5, 'EMP_STEP_EDITOR', 'EDITOR'),
    (5, 'EMP_PROJECT_NONE', 'EDITOR'),
    (5, 'EMP_STEP_NONE', 'NONE');
