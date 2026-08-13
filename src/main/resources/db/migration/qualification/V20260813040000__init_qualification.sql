-- 학력·자격증 (HR-V1 §2-G) — 전공/자격증 마스터 + 사원 학력/자격증(1:N)
-- 무엇: major·certificate(마스터) + employee_education·employee_certificate(사원 1:N) 4테이블 신설.
-- 왜: 사원 학력/자격증을 마스터 기반으로 관리(목업·QUAL). 전공/자격증은 회사별 마스터, 사원에 1:N.
-- 결정(HR-V1 §2-G·INV-18~21): 마스터 이름 회사 UNIQUE · 삭제는 hard delete + 참조 차단(앱) · 학위는 enum · 모두 company_id 스코프.
-- FK: 무결성 안전망(RESTRICT 기본) — 실제 삭제 차단은 앱이 employeeCount 로 먼저 판정한다(부서·직급 선례).
-- 번호: develop 최대 V20260813020100 위 040000(사내문서 030000 과도 안 겹침). ⚠️ 머지 직전 최신 번호 재확인.

CREATE TABLE major (
    major_id   BIGINT       NOT NULL AUTO_INCREMENT,
    company_id BIGINT       NOT NULL                            COMMENT '회사(테넌트)',
    name       VARCHAR(100) NOT NULL                            COMMENT '전공명',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '생성일',
    PRIMARY KEY (major_id),
    UNIQUE KEY uk_major_company_name (company_id, name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '전공 마스터 · HR-V1 MAJ';

CREATE TABLE certificate (
    certificate_id BIGINT       NOT NULL AUTO_INCREMENT,
    company_id     BIGINT       NOT NULL                            COMMENT '회사(테넌트)',
    name           VARCHAR(100) NOT NULL                            COMMENT '자격증명',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '생성일',
    PRIMARY KEY (certificate_id),
    UNIQUE KEY uk_certificate_company_name (company_id, name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '자격증 마스터 · HR-V1 CRT';

CREATE TABLE employee_education (
    employee_education_id BIGINT       NOT NULL AUTO_INCREMENT,
    company_id            BIGINT       NOT NULL                            COMMENT '회사(테넌트)',
    user_id               VARCHAR(20)  NOT NULL                            COMMENT '사원(사번) · employee.user_id',
    major_id              BIGINT       NOT NULL                            COMMENT '전공 마스터',
    degree                VARCHAR(20)  NOT NULL
        CHECK (degree IN ('BACHELOR', 'MASTER', 'DOCTOR'))                  COMMENT '학위 enum(BACHELOR·MASTER·DOCTOR)',
    school                VARCHAR(100) NULL                                COMMENT '학교(선택)',
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '생성일',
    PRIMARY KEY (employee_education_id),
    KEY idx_emp_edu_user (user_id),
    KEY idx_emp_edu_major (major_id),
    CONSTRAINT fk_emp_edu_employee FOREIGN KEY (user_id)  REFERENCES employee (user_id),
    CONSTRAINT fk_emp_edu_major    FOREIGN KEY (major_id) REFERENCES major (major_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '사원 학력(1:N) · HR-V1 QUAL';

CREATE TABLE employee_certificate (
    employee_certificate_id BIGINT      NOT NULL AUTO_INCREMENT,
    company_id              BIGINT      NOT NULL                            COMMENT '회사(테넌트)',
    user_id                 VARCHAR(20) NOT NULL                            COMMENT '사원(사번) · employee.user_id',
    certificate_id          BIGINT      NOT NULL                            COMMENT '자격증 마스터',
    acquired_date           DATE        NULL                                COMMENT '취득일(선택)',
    created_at              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '생성일',
    PRIMARY KEY (employee_certificate_id),
    KEY idx_emp_cert_user (user_id),
    KEY idx_emp_cert_certificate (certificate_id),
    CONSTRAINT fk_emp_cert_employee    FOREIGN KEY (user_id)        REFERENCES employee (user_id),
    CONSTRAINT fk_emp_cert_certificate FOREIGN KEY (certificate_id) REFERENCES certificate (certificate_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '사원 자격증(1:N) · HR-V1 QUAL';
