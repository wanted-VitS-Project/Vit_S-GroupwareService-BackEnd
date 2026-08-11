-- =====================================================================
-- VitaminS common employee/account schema
-- =====================================================================
-- Extracted from ERD final schema. Keep this migration before init_schema.

CREATE TABLE department (
  department_id BIGINT      NOT NULL AUTO_INCREMENT              COMMENT '부서 번호',
  name          VARCHAR(50) NOT NULL                             COMMENT '부서명',
  parent_id     BIGINT      NULL                                 COMMENT '상위 부서',
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '생성일',
  updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
  PRIMARY KEY (department_id),
  UNIQUE KEY uk_department_name (name),
  KEY idx_department_parent (parent_id),
  CONSTRAINT fk_department_parent
    FOREIGN KEY (parent_id) REFERENCES department (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='부서';

CREATE TABLE job_position (
  job_position_id BIGINT      NOT NULL AUTO_INCREMENT              COMMENT '직급 번호',
  name            VARCHAR(30) NOT NULL                             COMMENT '직급명',
  sort_order      INT         NOT NULL DEFAULT 0                   COMMENT '정렬 순서',
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '생성일',
  updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
  PRIMARY KEY (job_position_id),
  UNIQUE KEY uk_job_position_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='직급';

CREATE TABLE employee (
  user_id         VARCHAR(20)  NOT NULL                             COMMENT '사번',
  name            VARCHAR(50)  NOT NULL                             COMMENT '이름',
  is_system       TINYINT(1)   NOT NULL DEFAULT 0                   COMMENT '시스템 계정 여부',
  department_id   BIGINT       NULL                                 COMMENT '부서',
  job_position_id BIGINT       NULL                                 COMMENT '직급',
  email           VARCHAR(100) NULL                                 COMMENT '이메일',
  phone           VARCHAR(20)  NULL                                 COMMENT '연락처',
  hired_at        DATE         NULL                                 COMMENT '입사일',
  resigned_at     DATE         NULL                                 COMMENT '퇴사일',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '생성일',
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
  deleted_at      DATETIME     NULL                                 COMMENT '삭제일 (퇴사일 resigned_at 과 다르다)',
  PRIMARY KEY (user_id),
  KEY idx_employee_department (department_id),
  KEY idx_employee_job_position (job_position_id),
  KEY idx_employee_is_system (is_system),
  KEY idx_employee_resigned (resigned_at),
  CONSTRAINT fk_employee_department
    FOREIGN KEY (department_id) REFERENCES department (department_id),
  CONSTRAINT fk_employee_job_position
    FOREIGN KEY (job_position_id) REFERENCES job_position (job_position_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사원';

CREATE TABLE account (
  account_id           BIGINT       NOT NULL AUTO_INCREMENT        COMMENT '계정 번호',
  user_id              VARCHAR(20)  NOT NULL                       COMMENT '사번',
  password             VARCHAR(255) NOT NULL                       COMMENT '비밀번호',
  role                 ENUM('ADMIN','MASTER','MEMBER')
                                    NOT NULL DEFAULT 'MEMBER'      COMMENT '전역 권한',
  status               ENUM('ACTIVE','INACTIVE')
                                    NOT NULL DEFAULT 'ACTIVE'      COMMENT '계정 상태',
  must_change_password TINYINT(1)   NOT NULL DEFAULT 1             COMMENT '초기 비밀번호 변경 여부',
  login_fail_count     INT          NOT NULL DEFAULT 0             COMMENT '로그인 실패 횟수',
  locked_until         DATETIME     NULL                           COMMENT '잠금 해제 시각',
  last_login_at        DATETIME     NULL                           COMMENT '마지막 로그인 일',
  created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
  updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                             ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
  deleted_at           DATETIME     NULL                           COMMENT '삭제일',
  PRIMARY KEY (account_id),
  UNIQUE KEY uk_account_user_id (user_id),
  CONSTRAINT fk_account_employee
    FOREIGN KEY (user_id) REFERENCES employee (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='계정';

CREATE TABLE employee_group (
  group_id    BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '그룹 번호',
  name        VARCHAR(50)  NOT NULL                             COMMENT '그룹명',
  description VARCHAR(500) NULL                                 COMMENT '설명',
  created_by  VARCHAR(20)  NOT NULL                             COMMENT '생성자',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '생성일',
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
  PRIMARY KEY (group_id),
  UNIQUE KEY uk_employee_group_name (name),
  KEY idx_employee_group_creator (created_by),
  CONSTRAINT fk_employee_group_creator
    FOREIGN KEY (created_by) REFERENCES employee (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='그룹';

CREATE TABLE page_permission (
  page_permission_id BIGINT      NOT NULL AUTO_INCREMENT          COMMENT '페이지 권한 번호',
  page_code          VARCHAR(50) NOT NULL                         COMMENT '페이지 코드 (BIDDING · FINANCE)',
  user_id            VARCHAR(20) NOT NULL                         COMMENT '사번',
  permission         ENUM('VIEWER','EDITOR') NOT NULL             COMMENT '권한 등급',
  created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
  updated_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
  PRIMARY KEY (page_permission_id),
  UNIQUE KEY uk_page_permission (page_code, user_id),
  KEY idx_page_permission_user (user_id),
  CONSTRAINT fk_page_permission_employee
    FOREIGN KEY (user_id) REFERENCES employee (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='페이지 권한';

CREATE TABLE employee_group_member (
  group_id   BIGINT      NOT NULL                            COMMENT '그룹 번호',
  user_id    VARCHAR(20) NOT NULL                            COMMENT '사번',
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '추가일',
  PRIMARY KEY (group_id, user_id),
  KEY idx_egm_user (user_id),
  CONSTRAINT fk_egm_group
    FOREIGN KEY (group_id) REFERENCES employee_group (group_id) ON DELETE CASCADE,
  CONSTRAINT fk_egm_employee
    FOREIGN KEY (user_id) REFERENCES employee (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='그룹 구성원';
