-- =====================================================================
-- ApprovalListMapper 회사 스코프 검증용 픽스처 (H2 · MySQL 모드)
-- =====================================================================
-- ⚠️ 운영 스키마의 미러가 아니다. 목록 쿼리가 실제로 읽는 컬럼만 담았다.
--    실제 스키마는 Flyway(`db/migration`)가 정본이며, MySQL 전용 문법(ENUM ·
--    ON UPDATE CURRENT_TIMESTAMP · 생성 컬럼)이 H2 에서 안 돌아 그대로 쓸 수 없다.
--    여기에 컬럼을 늘려 실제 스키마를 흉내내면 두 벌을 관리하게 된다 — 늘리지 말 것.
--
-- 데이터: 회사 1(vitas)·회사 2(acme) 각각 결재 1건. 두 결재는 사번 base 가 같다
--         (1234567) — 접두사가 없으면 충돌했을 값이라, 회사 격리와 전역 유일 접두사를
--         동시에 확인한다.

-- ⚠️ 테스트마다 다시 실행된다. @MybatisTest 의 트랜잭션 롤백은 INSERT 만 되돌리고 DDL 은 커밋된 채
--    남으므로, 드롭 없이는 두 번째 테스트에서 "테이블 이미 존재"로 깨진다 (H2 는 DB_CLOSE_DELAY=-1 로
--    JVM 내내 살아있다). 자식 → 부모 순서로 지운다.
DROP TABLE IF EXISTS approval_line;
DROP TABLE IF EXISTS approval_revision;
DROP TABLE IF EXISTS approval;
DROP TABLE IF EXISTS block;
DROP TABLE IF EXISTS step;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS employee;

CREATE TABLE employee (
    user_id    VARCHAR(20) PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    company_id BIGINT      NOT NULL
);

CREATE TABLE project (
    project_id BIGINT PRIMARY KEY,
    name       VARCHAR(200) NOT NULL
);

CREATE TABLE step (
    step_id    BIGINT PRIMARY KEY,
    project_id BIGINT       NOT NULL,
    name       VARCHAR(200) NOT NULL
);

CREATE TABLE block (
    block_id BIGINT PRIMARY KEY,
    step_id  BIGINT NOT NULL
);

CREATE TABLE approval (
    approval_id         BIGINT PRIMARY KEY,
    block_id            BIGINT,
    user_id             VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    current_revision_no INT         NOT NULL,
    completed_at        TIMESTAMP,
    created_at          TIMESTAMP   NOT NULL,
    deleted_at          TIMESTAMP
);

CREATE TABLE approval_revision (
    approval_revision_id BIGINT PRIMARY KEY,
    approval_id          BIGINT       NOT NULL,
    revision_no          INT          NOT NULL,
    title                VARCHAR(200) NOT NULL,
    submitted_at         TIMESTAMP,
    deleted_at           TIMESTAMP
);

CREATE TABLE approval_line (
    approval_line_id     BIGINT PRIMARY KEY,
    approval_revision_id BIGINT      NOT NULL,
    user_id              VARCHAR(20) NOT NULL,
    sequence_no          INT         NOT NULL,
    status               VARCHAR(20) NOT NULL,
    deleted_at           TIMESTAMP
);

-- ── 공통 ────────────────────────────────────────────────────────────
INSERT INTO project (project_id, name) VALUES (1, '공용 프로젝트');
INSERT INTO step (step_id, project_id, name) VALUES (1, 1, '착수');
INSERT INTO block (block_id, step_id) VALUES (11, 1), (22, 1);

-- ── 회사 1 (vitas) ──────────────────────────────────────────────────
INSERT INTO employee (user_id, name, company_id) VALUES
  ('vitas-1234567', '김비타', 1),
  ('vitas-7654321', '이결재', 1);

INSERT INTO approval (approval_id, block_id, user_id, status, current_revision_no, completed_at, created_at, deleted_at)
VALUES (100, 11, 'vitas-1234567', 'IN_PROGRESS', 1, NULL, '2026-08-10 10:00:00', NULL);

INSERT INTO approval_revision (approval_revision_id, approval_id, revision_no, title, submitted_at)
VALUES (200, 100, 1, '회사1 품의서', '2026-08-10 10:05:00');

INSERT INTO approval_line (approval_line_id, approval_revision_id, user_id, sequence_no, status, deleted_at)
VALUES (300, 200, 'vitas-7654321', 1, 'ACTIVE', NULL);

-- ── 회사 2 (acme) — base 사번이 회사 1과 동일하다 ────────────────────
INSERT INTO employee (user_id, name, company_id) VALUES
  ('acme-1234567', '박에크', 2),
  ('acme-7654321', '최결재', 2);

INSERT INTO approval (approval_id, block_id, user_id, status, current_revision_no, completed_at, created_at, deleted_at)
VALUES (101, 22, 'acme-1234567', 'IN_PROGRESS', 1, NULL, '2026-08-10 11:00:00', NULL);

INSERT INTO approval_revision (approval_revision_id, approval_id, revision_no, title, submitted_at)
VALUES (201, 101, 1, '회사2 품의서', '2026-08-10 11:05:00');

INSERT INTO approval_line (approval_line_id, approval_revision_id, user_id, sequence_no, status, deleted_at)
VALUES (301, 201, 'acme-7654321', 1, 'ACTIVE', NULL);
