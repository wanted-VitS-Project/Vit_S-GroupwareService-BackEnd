-- =====================================================================
-- EmployeeAdminQueryMapper.findPage/count — includeSubDepartments 하위 부서 필터 검증용 픽스처 (H2 · MySQL 모드)
-- =====================================================================
-- ⚠️ 운영 스키마의 미러가 아니다. 목록 조회(findPage·count)가 실제로 읽는 컬럼만 담았다.
--
-- 트리(최대 2단): 기술본부(1) └ 개발팀(2)·인사팀(3) / 재무팀(9, 별도 최상위).
-- 사원: 기술본부 직속 1명 · 개발팀 2명 · 인사팀 1명 · 재무팀 1명.
--   departmentId=1 단독      → 기술본부 직속 1명
--   departmentId=1 + 하위포함 → 기술본부+개발팀+인사팀 4명 (재무팀 제외)
--   departmentId=2 + 하위포함 → 개발팀 2명 (하위 부서 없음 = 직속과 동일)

DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS account;
DROP TABLE IF EXISTS job_position;
DROP TABLE IF EXISTS department;

CREATE TABLE department (
    department_id BIGINT PRIMARY KEY,
    name          VARCHAR(50) NOT NULL,
    parent_id     BIGINT,
    company_id    BIGINT      NOT NULL
);

CREATE TABLE job_position (
    job_position_id BIGINT PRIMARY KEY,
    name            VARCHAR(30) NOT NULL,
    company_id      BIGINT      NOT NULL
);

CREATE TABLE account (
    user_id              VARCHAR(20) PRIMARY KEY,
    role                 VARCHAR(20) NOT NULL,
    status               VARCHAR(20) NOT NULL,
    must_change_password TINYINT     NOT NULL DEFAULT 0
);

CREATE TABLE employee (
    user_id           VARCHAR(20) PRIMARY KEY,
    company_id        BIGINT      NOT NULL,
    name              VARCHAR(50) NOT NULL,
    email             VARCHAR(100),
    is_system         TINYINT     NOT NULL DEFAULT 0,
    department_id     BIGINT,
    job_position_id   BIGINT,
    profile_image_key VARCHAR(512),
    resigned_at       DATE,
    deleted_at        TIMESTAMP
);

INSERT INTO department (department_id, name, parent_id, company_id) VALUES
    (1, '기술본부', NULL, 1),
    (2, '개발팀',   1,    1),
    (3, '인사팀',   1,    1),
    (9, '재무팀',   NULL, 1);

INSERT INTO job_position (job_position_id, name, company_id) VALUES (1, '사원', 1);

INSERT INTO account (user_id, role, status, must_change_password) VALUES
    ('vitas-EMP001', 'MEMBER', 'ACTIVE', 0),
    ('vitas-EMP002', 'MEMBER', 'ACTIVE', 0),
    ('vitas-EMP003', 'MEMBER', 'ACTIVE', 0),
    ('vitas-EMP004', 'MEMBER', 'ACTIVE', 0),
    ('vitas-EMP005', 'MEMBER', 'ACTIVE', 0);

INSERT INTO employee (user_id, company_id, name, is_system, department_id, job_position_id) VALUES
    ('vitas-EMP001', 1, '본부장', 0, 1, 1),   -- 기술본부 직속
    ('vitas-EMP002', 1, '개발가', 0, 2, 1),   -- 개발팀
    ('vitas-EMP003', 1, '개발나', 0, 2, 1),   -- 개발팀
    ('vitas-EMP004', 1, '인사가', 0, 3, 1),   -- 인사팀
    ('vitas-EMP005', 1, '재무가', 0, 9, 1);   -- 재무팀 (하위 아님 → 제외돼야 함)
