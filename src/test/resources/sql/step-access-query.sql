-- =====================================================================
-- StepAccessQueryMapper 판정 재료 조회 검증용 픽스처 (H2 · MySQL 모드)
-- =====================================================================
-- ⚠️ 운영 스키마의 미러가 아니다. 판정 쿼리가 실제로 읽는 컬럼만 담았다.
--    실제 스키마는 Flyway(`db/migration`)가 정본이며, MySQL 전용 문법(ENUM ·
--    ON UPDATE CURRENT_TIMESTAMP)이 H2 에서 안 돌아 그대로 쓸 수 없다.
--    여기에 컬럼을 늘려 실제 스키마를 흉내내면 두 벌을 관리하게 된다 — 늘리지 말 것.
--
-- 데이터 구성 — 4가지 판정 경로를 한 픽스처로 덮는다.
--   step 10 → project 1 (회사 1)  : 참여자 EDITOR · 오버라이드 없음      → 상속
--   step 11 → project 1 (회사 1)  : 참여자 EDITOR · 오버라이드 NONE      → 명시적 차단
--   step 20 → project 2 (회사 2)  : 참여자 EDITOR                        → 회사가 다르면 안 보여야 한다
--   step 30 → project 1           : deleted_at 있음                      → 스텝 자체가 안 나와야 한다
--   step 40 → project 3 (삭제됨)  : 참여자 EDITOR                        → 프로젝트가 죽으면 안 보여야 한다

-- ⚠️ 테스트마다 다시 실행된다. @MybatisTest 의 트랜잭션 롤백은 INSERT 만 되돌리고 DDL 은 커밋된 채
--    남으므로, 드롭 없이는 두 번째 테스트에서 "테이블 이미 존재"로 깨진다.
DROP TABLE IF EXISTS step_permission;
DROP TABLE IF EXISTS project_member;
DROP TABLE IF EXISTS step;
DROP TABLE IF EXISTS project;

CREATE TABLE project (
    project_id BIGINT PRIMARY KEY,
    company_id BIGINT   NOT NULL,
    deleted_at TIMESTAMP NULL
);

CREATE TABLE step (
    step_id    BIGINT PRIMARY KEY,
    project_id BIGINT   NOT NULL,
    deleted_at TIMESTAMP NULL
);

CREATE TABLE project_member (
    project_id BIGINT      NOT NULL,
    user_id    VARCHAR(20) NOT NULL,
    permission VARCHAR(10) NOT NULL,
    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE step_permission (
    step_id    BIGINT      NOT NULL,
    user_id    VARCHAR(20) NOT NULL,
    permission VARCHAR(10) NOT NULL,
    PRIMARY KEY (step_id, user_id)
);

INSERT INTO project (project_id, company_id, deleted_at) VALUES
    (1, 1, NULL),
    (2, 2, NULL),
    (3, 1, '2026-08-01 09:00:00');

INSERT INTO step (step_id, project_id, deleted_at) VALUES
    (10, 1, NULL),
    (11, 1, NULL),
    (20, 2, NULL),
    (30, 1, '2026-08-01 09:00:00'),
    (40, 3, NULL);

INSERT INTO project_member (project_id, user_id, permission) VALUES
    (1, 'vitas-1000001', 'EDITOR'),
    (2, 'vitas-1000001', 'EDITOR'),
    (3, 'vitas-1000001', 'EDITOR');

-- 스텝 11 만 명시적 차단. 다른 스텝에는 오버라이드 행이 없다(= NULL 로 내려와야 한다).
INSERT INTO step_permission (step_id, user_id, permission) VALUES
    (11, 'vitas-1000001', 'NONE');
