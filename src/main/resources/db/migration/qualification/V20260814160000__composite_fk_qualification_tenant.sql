-- 학력/자격증 교차테넌트 참조 방지 복합 FK (멀티테넌시 후속 · B2)
-- 무엇: employee_education->major, employee_certificate->certificate 를 단일 FK -> 복합 FK(company_id 포함)로 교체.
-- 왜: major_id·certificate_id 는 회사별로 번호가 겹칠 수 있어, 단일 FK 로는 "회사1 사원 학력이 회사2 전공 참조"를
--     못 막는다. major_id·certificate_id 는 NOT NULL 이라 복합 FK 가 항상 강제된다(B1 의 nullable 케이스와 다름).
-- 위반점검(2026-08-14): 단일 테넌트(company_id=1)라 교차참조 불가 -> 위반 0. throwaway MySQL 로 검증.
-- 순서: 부모 UNIQUE(company_id, id) 선행. ⚠️ MySQL 은 한 ALTER 에서 동명 FK DROP+ADD 불가라 문을 분리한다(B1 참고).

-- 1) 부모 유니크키 (복합 FK 참조 대상)
ALTER TABLE major
    ADD UNIQUE KEY uk_major_company_id (company_id, major_id);

ALTER TABLE certificate
    ADD UNIQUE KEY uk_certificate_company_id (company_id, certificate_id);

-- 2) employee_education.major_id -> major (복합)
ALTER TABLE employee_education
    DROP FOREIGN KEY fk_emp_edu_major;
ALTER TABLE employee_education
    ADD KEY idx_emp_edu_company_major (company_id, major_id),
    ADD CONSTRAINT fk_emp_edu_major
        FOREIGN KEY (company_id, major_id) REFERENCES major (company_id, major_id);

-- 3) employee_certificate.certificate_id -> certificate (복합)
ALTER TABLE employee_certificate
    DROP FOREIGN KEY fk_emp_cert_certificate;
ALTER TABLE employee_certificate
    ADD KEY idx_emp_cert_company_certificate (company_id, certificate_id),
    ADD CONSTRAINT fk_emp_cert_certificate
        FOREIGN KEY (company_id, certificate_id) REFERENCES certificate (company_id, certificate_id);
