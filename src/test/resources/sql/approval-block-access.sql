-- =====================================================================
-- ApprovalBlockAccessMapper 검증용 픽스처 (H2 · MySQL 모드)
-- =====================================================================
-- ⚠️ 운영 스키마의 미러가 아니다. 판정 쿼리가 실제로 읽는 컬럼만 담았다.
--    실제 스키마는 Flyway(`db/migration`)가 정본이며, MySQL 전용 문법(ENUM ·
--    ON UPDATE CURRENT_TIMESTAMP)이 H2 에서 안 돌아 그대로 쓸 수 없다. 늘리지 말 것.
--
-- 데이터 구성 — 판정이 갈리는 경우를 한 프로젝트 안에 모아둔다.
--   block 11  정상 블록 (회사 1 · 스텝 1)
--   block 12  블록 자체가 삭제됨
--   block 13  스텝이 삭제됨 (블록은 살아있다 — 두 삭제를 구분해서 확인하려고 나눠 뒀다)
--   block 21  회사 2 프로젝트 소속
--   block 31  프로젝트가 삭제됨

-- ⚠️ 테스트마다 다시 실행된다. @MybatisTest 의 트랜잭션 롤백은 INSERT 만 되돌리고 DDL 은 커밋된 채
--    남으므로, 드롭 없이는 두 번째 테스트에서 "테이블 이미 존재"로 깨진다. 자식 → 부모 순서로 지운다.
DROP TABLE IF EXISTS step_permission;
DROP TABLE IF EXISTS project_member;
DROP TABLE IF EXISTS block;
DROP TABLE IF EXISTS step;
DROP TABLE IF EXISTS project;

CREATE TABLE project (
    project_id BIGINT PRIMARY KEY,
    company_id BIGINT       NOT NULL,
    name       VARCHAR(200) NOT NULL,
    deleted_at TIMESTAMP
);

CREATE TABLE step (
    step_id    BIGINT PRIMARY KEY,
    project_id BIGINT       NOT NULL,
    name       VARCHAR(200) NOT NULL,
    deleted_at TIMESTAMP
);

CREATE TABLE block (
    block_id   BIGINT PRIMARY KEY,
    step_id    BIGINT      NOT NULL,
    type       VARCHAR(30) NOT NULL,
    created_by VARCHAR(20) NOT NULL,
    deleted_at TIMESTAMP
);

CREATE TABLE project_member (
    project_member_id BIGINT PRIMARY KEY,
    project_id        BIGINT      NOT NULL,
    user_id           VARCHAR(20) NOT NULL,
    permission        VARCHAR(10) NOT NULL
);

CREATE TABLE step_permission (
    step_permission_id BIGINT PRIMARY KEY,
    step_id            BIGINT      NOT NULL,
    user_id            VARCHAR(20) NOT NULL,
    permission         VARCHAR(10) NOT NULL
);

-- ── 회사 1 ──────────────────────────────────────────────────────────
INSERT INTO project (project_id, company_id, name, deleted_at) VALUES (1, 1, '회사1 과업', NULL);
INSERT INTO step (step_id, project_id, name, deleted_at) VALUES
  (1, 1, '착수', NULL),
  (2, 1, '삭제된 스텝', '2026-08-15 10:00:00');
INSERT INTO block (block_id, step_id, type, created_by, deleted_at) VALUES
  (11, 1, 'APPROVAL', 'vitas-1111111', NULL),
  (12, 1, 'APPROVAL', 'vitas-1111111', '2026-08-15 11:00:00'),
  (13, 2, 'APPROVAL', 'vitas-1111111', NULL);

-- ── 회사 2 ──────────────────────────────────────────────────────────
INSERT INTO project (project_id, company_id, name, deleted_at) VALUES (2, 2, '회사2 과업', NULL);
INSERT INTO step (step_id, project_id, name, deleted_at) VALUES (3, 2, '착수', NULL);
INSERT INTO block (block_id, step_id, type, created_by, deleted_at) VALUES
  (21, 3, 'APPROVAL', 'acme-1111111', NULL);

-- ── 삭제된 프로젝트 ─────────────────────────────────────────────────
INSERT INTO project (project_id, company_id, name, deleted_at) VALUES (3, 1, '삭제된 과업', '2026-08-15 09:00:00');
INSERT INTO step (step_id, project_id, name, deleted_at) VALUES (4, 3, '착수', NULL);
INSERT INTO block (block_id, step_id, type, created_by, deleted_at) VALUES
  (31, 4, 'APPROVAL', 'vitas-1111111', NULL);

-- ── 권한 (전부 프로젝트 1 · 스텝 1 기준) ────────────────────────────
--   EDITOR_ONLY    프로젝트 EDITOR, 스텝 오버라이드 없음        → EDITOR (상속)
--   VIEWER_ONLY    프로젝트 VIEWER, 스텝 오버라이드 없음        → VIEWER (상속)
--   OVERRIDDEN     프로젝트 VIEWER, 스텝 오버라이드 EDITOR      → EDITOR (오버라이드 우선)
--   DOWNGRADED     프로젝트 EDITOR, 스텝 오버라이드 NONE        → NONE   (오버라이드 우선)
--   PROJECT_NONE   프로젝트 NONE,   스텝 오버라이드 EDITOR      → NONE   (⚠️ 오버라이드를 보지 않는다)
--   OUTSIDER       프로젝트 행 없음, 스텝 오버라이드 EDITOR     → NONE   (⚠️ 미참여도 마찬가지)
INSERT INTO project_member (project_member_id, project_id, user_id, permission) VALUES
  (1, 1, 'vitas-editor01', 'EDITOR'),
  (2, 1, 'vitas-viewer01', 'VIEWER'),
  (3, 1, 'vitas-overrid1', 'VIEWER'),
  (4, 1, 'vitas-downgrd1', 'EDITOR'),
  (5, 1, 'vitas-prjnone1', 'NONE');

INSERT INTO step_permission (step_permission_id, step_id, user_id, permission) VALUES
  (1, 1, 'vitas-overrid1', 'EDITOR'),
  (2, 1, 'vitas-downgrd1', 'NONE'),
  (3, 1, 'vitas-prjnone1', 'EDITOR'),
  (4, 1, 'vitas-outside1', 'EDITOR');
