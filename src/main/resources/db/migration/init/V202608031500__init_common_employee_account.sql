-- =====================================================================
-- VitaminS common employee/account schema
-- =====================================================================
-- Scope:
--   department, job_position, employee, account,
--   employee_group, employee_group_member, page_permission
--
-- This migration intentionally excludes cross-domain tables and FK constraints.

CREATE TABLE department (
  department_id BIGINT      NOT NULL AUTO_INCREMENT,
  name          VARCHAR(50) NOT NULL,
  parent_id     BIGINT      NULL,
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (department_id),
  UNIQUE KEY uk_department_name (name),
  KEY idx_department_parent (parent_id),
  CONSTRAINT fk_department_parent
    FOREIGN KEY (parent_id) REFERENCES department (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='department';

CREATE TABLE job_position (
  job_position_id BIGINT      NOT NULL AUTO_INCREMENT,
  name            VARCHAR(30) NOT NULL,
  sort_order      INT         NOT NULL DEFAULT 0,
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (job_position_id),
  UNIQUE KEY uk_job_position_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='job position';

CREATE TABLE employee (
  user_id         VARCHAR(20)  NOT NULL,
  name            VARCHAR(50)  NOT NULL,
  is_system       TINYINT(1)   NOT NULL DEFAULT 0,
  department_id   BIGINT       NULL,
  job_position_id BIGINT       NULL,
  email           VARCHAR(100) NULL,
  phone           VARCHAR(20)  NULL,
  hired_at        DATE         NULL,
  resigned_at     DATE         NULL,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  KEY idx_employee_department (department_id),
  KEY idx_employee_job_position (job_position_id),
  KEY idx_employee_is_system (is_system),
  KEY idx_employee_resigned (resigned_at),
  CONSTRAINT fk_employee_department
    FOREIGN KEY (department_id) REFERENCES department (department_id),
  CONSTRAINT fk_employee_job_position
    FOREIGN KEY (job_position_id) REFERENCES job_position (job_position_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='employee';

CREATE TABLE account (
  account_id           BIGINT       NOT NULL AUTO_INCREMENT,
  user_id              VARCHAR(20)  NOT NULL,
  password             VARCHAR(255) NOT NULL,
  role                 ENUM('ADMIN','MASTER','MEMBER')
                                    NOT NULL DEFAULT 'MEMBER',
  status               ENUM('ACTIVE','INACTIVE')
                                    NOT NULL DEFAULT 'ACTIVE',
  must_change_password TINYINT(1)   NOT NULL DEFAULT 1,
  login_fail_count     INT          NOT NULL DEFAULT 0,
  locked_until         DATETIME     NULL,
  last_login_at        DATETIME     NULL,
  created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                             ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (account_id),
  UNIQUE KEY uk_account_user_id (user_id),
  CONSTRAINT fk_account_employee
    FOREIGN KEY (user_id) REFERENCES employee (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='account';

CREATE TABLE employee_group (
  group_id    BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(50)  NOT NULL,
  description VARCHAR(500) NULL,
  created_by  VARCHAR(20)  NOT NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (group_id),
  UNIQUE KEY uk_employee_group_name (name),
  KEY idx_employee_group_creator (created_by),
  CONSTRAINT fk_employee_group_creator
    FOREIGN KEY (created_by) REFERENCES employee (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='employee group';

CREATE TABLE employee_group_member (
  group_id   BIGINT      NOT NULL,
  user_id    VARCHAR(20) NOT NULL,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (group_id, user_id),
  KEY idx_egm_user (user_id),
  CONSTRAINT fk_egm_group
    FOREIGN KEY (group_id) REFERENCES employee_group (group_id) ON DELETE CASCADE,
  CONSTRAINT fk_egm_employee
    FOREIGN KEY (user_id) REFERENCES employee (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='employee group member';

CREATE TABLE page_permission (
  page_permission_id BIGINT      NOT NULL AUTO_INCREMENT,
  page_code          VARCHAR(50) NOT NULL,
  user_id            VARCHAR(20) NOT NULL,
  permission         ENUM('VIEWER','EDITOR') NOT NULL,
  created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (page_permission_id),
  UNIQUE KEY uk_page_permission (page_code, user_id),
  KEY idx_page_permission_user (user_id),
  CONSTRAINT fk_page_permission_employee
    FOREIGN KEY (user_id) REFERENCES employee (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='page permission';
