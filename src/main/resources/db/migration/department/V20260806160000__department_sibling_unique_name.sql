-- 부서명 중복 검사 범위를 전체 → 같은 상위 부서(형제)로 완화한다 (.ai/api/department.md §2·§3, 2026-08-06).
-- 기술본부>개발팀 과 SI본부>개발팀 처럼 상위가 다르면 같은 이름을 허용한다.
--
-- 전체 유니크(uk_department_name)를 드롭하고 (parent_key, name) 복합 유니크로 교체한다.
-- ⚠️ MySQL 은 parent_id 가 NULL 인 행끼리는 UNIQUE 로 막지 않는다. 그래서 최상위 부서 동명이
--    (parent_id, name) 복합 유니크만으로는 안 걸린다. NULL 을 0 으로 정규화한 생성 열 parent_key
--    (= COALESCE(parent_id, 0)) 에 유니크를 걸어, 최상위(0 을 공유)까지 DB 가 동명을 막게 한다.
--    parent_id 는 AUTO_INCREMENT 로 1 부터 부여되어 0 과 충돌하지 않는다.
--
-- 기존 seed 부서는 전부 전역 유니크였으므로 이 복합 유니크를 위반하지 않는다.

ALTER TABLE department
    DROP INDEX uk_department_name,
    ADD COLUMN parent_key BIGINT AS (COALESCE(parent_id, 0)) STORED COMMENT '유니크용 상위키(최상위=0)';

ALTER TABLE department
    ADD CONSTRAINT uk_department_parent_name UNIQUE (parent_key, name);
