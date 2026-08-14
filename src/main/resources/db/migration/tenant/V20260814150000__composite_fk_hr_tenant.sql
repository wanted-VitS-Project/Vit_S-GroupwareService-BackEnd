-- HR 교차테넌트 참조 방지 복합 FK (멀티테넌시 후속 · B1)
-- 무엇: employee<->department/job_position, department 자기참조를 단일 FK -> 복합 FK(company_id 포함)로 교체.
-- 왜: department_id·job_position_id 는 회사별로 번호가 겹칠 수 있어(사번과 달리 prefix 없음),
--     단일 FK 로는 "회사1 사원이 회사2 부서 참조"를 못 막는다. 복합 FK 로 같은 회사 안으로 강제한다.
-- 위반점검(2026-08-14): 단일 테넌트(company_id=1)라 교차참조 불가 -> 위반 0. throwaway MySQL 로 seed 위 검증.
-- 순서: 부모 UNIQUE(company_id, id) 를 먼저 만든 뒤 자식 FK 를 교체한다(복합 FK 참조 대상 키 선행).
-- nullable(department_id·job_position_id·parent_id) 은 미지정·최상위를 뜻하며, 복합 FK 는 NULL 포함 시
-- 검사하지 않는다(MATCH SIMPLE) -> 정상 동작.
-- ⚠️ MySQL 은 한 ALTER 문에서 같은 이름 FK 를 DROP+ADD 하면 이름 충돌로 거부한다.
--    그래서 DROP 과 ADD 를 반드시 별도 ALTER 문으로 나눈다(이름은 그대로 유지).

-- 1) 부모 유니크키 (복합 FK 참조 대상)
ALTER TABLE department
    ADD UNIQUE KEY uk_department_company_id (company_id, department_id);

ALTER TABLE job_position
    ADD UNIQUE KEY uk_job_position_company_id (company_id, job_position_id);

-- 2) employee.department_id -> department (복합)
ALTER TABLE employee
    DROP FOREIGN KEY fk_employee_department;
ALTER TABLE employee
    ADD KEY idx_employee_company_department (company_id, department_id),
    ADD CONSTRAINT fk_employee_department
        FOREIGN KEY (company_id, department_id) REFERENCES department (company_id, department_id);

-- 3) employee.job_position_id -> job_position (복합)
ALTER TABLE employee
    DROP FOREIGN KEY fk_employee_job_position;
ALTER TABLE employee
    ADD KEY idx_employee_company_job_position (company_id, job_position_id),
    ADD CONSTRAINT fk_employee_job_position
        FOREIGN KEY (company_id, job_position_id) REFERENCES job_position (company_id, job_position_id);

-- 4) department.parent_id -> department 자기참조 (복합)
ALTER TABLE department
    DROP FOREIGN KEY fk_department_parent;
ALTER TABLE department
    ADD KEY idx_department_company_parent (company_id, parent_id),
    ADD CONSTRAINT fk_department_parent
        FOREIGN KEY (company_id, parent_id) REFERENCES department (company_id, department_id);
