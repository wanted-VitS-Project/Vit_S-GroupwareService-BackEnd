-- project_business_category 교차테넌트 방지 복합 FK (멀티테넌시 후속 · B3)
-- 무엇: 조인 테이블에 company_id 신설·백필 후 project/business_category 참조를 복합 FK 로 교체.
-- 왜: pbc 는 project(회사)·business_category(회사)를 잇는 조인행인데 company_id 가 없어, 회사1 프로젝트에
--     회사2 카테고리를 링크하는 걸 DB 가 못 막는다. 복합 FK 로 같은 회사 안으로 강제한다.
-- 백필 근거: project.company_id 가 실귀속 정본(business_category 는 Phase1 에서 DEFAULT 1 스탬핑이라 근거 약함).
-- 위반점검(2026-08-14): 단일 테넌트(company_id=1)라 교차링크 불가 -> 백필·복합 FK 위반 0. throwaway MySQL 로 검증.
-- ⚠️ MySQL 은 한 ALTER 에서 동명 FK DROP+ADD 불가라 문을 분리한다(B1 교훈). DDL 은 비트랜잭션이라 순서가 중요하다.

-- 1) company_id 신설 (우선 nullable)
ALTER TABLE project_business_category
    ADD COLUMN company_id BIGINT NULL COMMENT '회사(테넌트) · project 기준 백필' AFTER project_business_category_id;

-- 2) project 기준 백필 (조인행의 회사 = 프로젝트의 회사)
UPDATE project_business_category pbc
    JOIN project p ON p.project_id = pbc.project_id
    SET pbc.company_id = p.company_id;

-- 3) NOT NULL 확정
ALTER TABLE project_business_category
    MODIFY COLUMN company_id BIGINT NOT NULL COMMENT '회사(테넌트)';

-- 4) 부모 유니크키 (복합 FK 참조 대상)
ALTER TABLE project
    ADD UNIQUE KEY uk_project_company_id (company_id, project_id);
ALTER TABLE business_category
    ADD UNIQUE KEY uk_business_category_company_id (company_id, business_category_id);

-- 5) project 참조 복합화
ALTER TABLE project_business_category
    DROP FOREIGN KEY fk_pbc_project;
ALTER TABLE project_business_category
    ADD KEY idx_pbc_company_project (company_id, project_id),
    ADD CONSTRAINT fk_pbc_project
        FOREIGN KEY (company_id, project_id) REFERENCES project (company_id, project_id);

-- 6) business_category 참조 복합화
ALTER TABLE project_business_category
    DROP FOREIGN KEY fk_pbc_category;
ALTER TABLE project_business_category
    ADD KEY idx_pbc_company_category (company_id, business_category_id),
    ADD CONSTRAINT fk_pbc_category
        FOREIGN KEY (company_id, business_category_id) REFERENCES business_category (company_id, business_category_id);
