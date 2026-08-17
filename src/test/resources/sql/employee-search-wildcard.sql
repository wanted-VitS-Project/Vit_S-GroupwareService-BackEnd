-- =====================================================================
-- EmployeeSearchQueryMapper.search — LIKE 와일드카드 이스케이프 검증용 픽스처 (H2 · MySQL 모드)
-- =====================================================================
-- ⚠️ 운영 스키마의 미러가 아니다. search 가 실제로 읽는 컬럼만 담았다
--    (approval-employee-position.sql 과 같은 원칙 — 컬럼을 늘려 스키마를 흉내내지 말 것).
--
-- 데이터: 같은 회사에 ① 이름에 '김' 들어간 2명 ② 이름에 리터럴 '%' 든 1명 ③ 리터럴 '_' 든 1명 ④ 무관 1명.
--         name='%' · name='_' 로 검색해도 전 사원이 아니라 그 문자를 실제로 가진 사람만 나와야 한다
--         (이스케이프가 없으면 '%'·'_' 가 와일드카드로 동작해 회사 전 사원이 새어 나온다).

-- @MybatisTest 롤백은 INSERT 만 되돌리고 DDL 은 남는다 — 자식 → 부모 순으로 먼저 지운다.
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS job_position;
DROP TABLE IF EXISTS department;

CREATE TABLE department (
    department_id BIGINT PRIMARY KEY,
    name          VARCHAR(50) NOT NULL,
    company_id    BIGINT      NOT NULL
);

CREATE TABLE job_position (
    job_position_id BIGINT PRIMARY KEY,
    name            VARCHAR(30) NOT NULL,
    company_id      BIGINT      NOT NULL
);

CREATE TABLE employee (
    user_id           VARCHAR(20) PRIMARY KEY,
    company_id        BIGINT      NOT NULL,
    name              VARCHAR(50) NOT NULL,
    is_system         TINYINT     NOT NULL DEFAULT 0,
    department_id     BIGINT,
    job_position_id   BIGINT,
    profile_image_key VARCHAR(512),
    resigned_at       DATE,
    deleted_at        TIMESTAMP
);

INSERT INTO department (department_id, name, company_id) VALUES (1, '개발팀', 1);
INSERT INTO job_position (job_position_id, name, company_id) VALUES (1, '사원', 1);

INSERT INTO employee (user_id, company_id, name, is_system, department_id, job_position_id) VALUES
    ('vitas-EMP001', 1, '김철수',    0, 1, 1),
    ('vitas-EMP002', 1, '김영희',    0, 1, 1),
    ('vitas-EMP003', 1, '100%할인',  0, 1, 1),   -- 이름에 리터럴 '%'
    ('vitas-EMP004', 1, 'a_b테스트', 0, 1, 1),   -- 이름에 리터럴 '_'
    ('vitas-EMP005', 1, '박지성',    0, 1, 1);   -- '김' 무관
