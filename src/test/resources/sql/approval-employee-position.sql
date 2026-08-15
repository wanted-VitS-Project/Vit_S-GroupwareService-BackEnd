-- =====================================================================
-- ApprovalQueryMapper.findEmployee 직급명 매핑 검증용 픽스처 (H2 · MySQL 모드)
-- =====================================================================
-- ⚠️ 운영 스키마의 미러가 아니다. findEmployee 가 실제로 읽는 컬럼만 담았다
--    (approval-list-company-scope.sql 과 같은 원칙 — 컬럼을 늘려 스키마를 흉내내지 말 것).
--
-- 데이터: 같은 회사에 ① 직급이 '대표'인 MEMBER ② 직급 미지정 MEMBER.
--         결재선 자격 판정이 role 이 아니라 직급명으로 대표를 가리기 때문에,
--         그 문자열이 조인을 타고 실제로 실려 오는지가 이 픽스처의 목적이다.

-- @MybatisTest 롤백은 INSERT 만 되돌리고 DDL 은 남는다 — 자식 → 부모 순으로 먼저 지운다.
DROP TABLE IF EXISTS account;
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS job_position;
DROP TABLE IF EXISTS department;

CREATE TABLE department (
    department_id BIGINT PRIMARY KEY,
    name          VARCHAR(50) NOT NULL,
    parent_id     BIGINT
);

CREATE TABLE job_position (
    job_position_id BIGINT PRIMARY KEY,
    name            VARCHAR(30) NOT NULL
);

CREATE TABLE employee (
    user_id         VARCHAR(20) PRIMARY KEY,
    company_id      BIGINT      NOT NULL,
    name            VARCHAR(50) NOT NULL,
    department_id   BIGINT,
    job_position_id BIGINT,
    resigned_at     DATE,
    deleted_at      TIMESTAMP
);

CREATE TABLE account (
    user_id VARCHAR(20) PRIMARY KEY,
    role    VARCHAR(10) NOT NULL,
    status  VARCHAR(10) NOT NULL
);

INSERT INTO department (department_id, name, parent_id) VALUES
    (1, '본사', NULL),
    (2, '개발팀', 1);

-- 운영 시드와 같은 직급명. '대표' 문자열이 정책 상수와 한 글자도 달라선 안 된다.
INSERT INTO job_position (job_position_id, name) VALUES
    (1, '사원'),
    (4, '대표');

INSERT INTO employee (user_id, company_id, name, department_id, job_position_id, resigned_at, deleted_at) VALUES
    ('vitas-EMP100', 1, '대표사원', 2, 4, NULL, NULL),
    ('vitas-EMP101', 1, '직급없음', 2, NULL, NULL, NULL);

INSERT INTO account (user_id, role, status) VALUES
    ('vitas-EMP100', 'MEMBER', 'ACTIVE'),
    ('vitas-EMP101', 'MEMBER', 'ACTIVE');
