-- 회사(테넌트) FK 무결성 안전망 추가 (멀티테넌시 후속 · A그룹)
-- 무엇: company_id 컬럼만 있고 company FK 가 없던 5개 테이블에 FK(company_id)->company 추가.
-- 왜: 존재하지 않거나 타 회사 company_id 주입을 DB 가 막지 못하던 공백을 닫는다(앱 전제 의존 제거).
-- 위반점검(2026-08-14): company 는 id=1 단일, 대상 5테이블 seed 없음 -> 위반 0. 삭제정책 RESTRICT 기본.
-- 인덱스: major/certificate/company_document 는 company_id 선두 인덱스(uk_*_company_name/idx_cd_company_deleted)로
--         FK 요건을 이미 충족한다. emp_edu/emp_cert 만 company_id 선두 인덱스가 없어 명시적으로 함께 추가한다.

ALTER TABLE major
    ADD CONSTRAINT fk_major_company FOREIGN KEY (company_id) REFERENCES company (company_id);

ALTER TABLE certificate
    ADD CONSTRAINT fk_certificate_company FOREIGN KEY (company_id) REFERENCES company (company_id);

ALTER TABLE employee_education
    ADD KEY idx_emp_edu_company (company_id),
    ADD CONSTRAINT fk_emp_edu_company FOREIGN KEY (company_id) REFERENCES company (company_id);

ALTER TABLE employee_certificate
    ADD KEY idx_emp_cert_company (company_id),
    ADD CONSTRAINT fk_emp_cert_company FOREIGN KEY (company_id) REFERENCES company (company_id);

ALTER TABLE company_document
    ADD CONSTRAINT fk_cd_company FOREIGN KEY (company_id) REFERENCES company (company_id);
