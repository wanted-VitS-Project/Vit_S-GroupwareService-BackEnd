-- 부서명 중복 검사 범위를 전체 → 같은 상위 부서(형제)로 완화한다 (.ai/api/department.md §2·§3, 2026-08-06).
-- 기술본부>개발팀 과 SI본부>개발팀 처럼 상위가 다르면 같은 이름을 허용한다.
--
-- 전체 유니크(uk_department_name)를 드롭하고 (parent_id, name) 복합 유니크로 교체한다.
-- ⚠️ MySQL 은 parent_id 가 NULL 인 행끼리는 UNIQUE 로 막지 않으므로, 이 복합 유니크는
--    "같은 부모 아래 자식 부서 동명" 만 DB 에서 막는다. 최상위(부모 없음) 동명은 애플리케이션이
--    parent_id IS NULL 검사로 막는다 (DepartmentCommandService).
--
-- 기존 seed 부서는 전부 전역 유니크였으므로 이 복합 유니크를 위반하지 않는다.

ALTER TABLE department
    DROP INDEX uk_department_name,
    ADD CONSTRAINT uk_department_parent_name UNIQUE (parent_id, name);
