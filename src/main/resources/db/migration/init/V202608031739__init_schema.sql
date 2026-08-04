-- =====================================================================
-- VitaminS initial schema
-- =====================================================================
-- Flyway versioned migration: reset statements and FK-check bypass are intentionally omitted.
-- Common employee/account tables are created in V202608031500__init_common_employee_account.sql.

CREATE TABLE crawl_source (
    crawl_source_id BIGINT AUTO_INCREMENT NOT NULL,
    source_code VARCHAR(30) NOT NULL,
    source_name VARCHAR(100) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    deleted_at DATETIME NULL,

    PRIMARY KEY (crawl_source_id),
    UNIQUE (source_code)
);

CREATE TABLE business_category (
    business_category_id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '카테고리 이름',
    code VARCHAR(30) NULL COMMENT '업무코드 (선택 · 표시·검색용)',
    description TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL COMMENT 'soft delete만 · 사용 중이면 삭제 불가',
    PRIMARY KEY (business_category_id),
    CONSTRAINT uk_bc_name UNIQUE (name),
    CONSTRAINT uk_bc_code UNIQUE (code),
    KEY idx_bc_deleted (deleted_at)
) COMMENT '사업 카테고리 (마스터 데이터 · ADMIN 전용 CRUD)';

CREATE TABLE crawl_condition (
    crawl_condition_id BIGINT AUTO_INCREMENT NOT NULL,
    crawl_source_id BIGINT NOT NULL,
    condition_name VARCHAR(100) NOT NULL,
    params JSON NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_success_at DATETIME NULL,
    last_collected_count INT NULL,
    created_by VARCHAR(20) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (crawl_condition_id),
    KEY idx_crawl_condition_creator (created_by),
    CONSTRAINT fk_crawl_condition_source FOREIGN KEY (crawl_source_id) REFERENCES crawl_source (crawl_source_id),
    CONSTRAINT fk_crawl_condition_creator FOREIGN KEY (created_by) REFERENCES employee (user_id)
);

CREATE TABLE bid_notice (
    bid_notice_id BIGINT AUTO_INCREMENT NOT NULL,
    crawl_source_id BIGINT NOT NULL,
    external_id VARCHAR(100) NOT NULL,
    notice_ord VARCHAR(20) NOT NULL,
    notice_name VARCHAR(500) NOT NULL,
    notice_agency VARCHAR(200) NULL,
    demand_agency VARCHAR(200) NULL,
    announced_at DATETIME NULL,
    bid_start_at DATETIME NULL,
    question_deadline_at DATETIME NULL,
    application_deadline_at DATETIME NULL,
    bid_deadline_at DATETIME NULL,
    opening_at DATETIME NULL,
    base_amount DECIMAL(19,2) NULL,
    estimated_amount DECIMAL(19,2) NULL,
    price_range_text VARCHAR(100) NULL,
    minimum_bid_rate_text VARCHAR(100) NULL,
    participation_qualification_text VARCHAR(1000) NULL,
    region_limit_text VARCHAR(500) NULL,
    business_limit_text VARCHAR(500) NULL,
    joint_contract_allowed BOOLEAN NULL,
    joint_contract_text VARCHAR(500) NULL,
    contract_method VARCHAR(100) NULL,
    evaluation_method VARCHAR(100) NULL,
    source_url VARCHAR(1000) NULL,
    has_attachment BOOLEAN NOT NULL DEFAULT FALSE,
    notice_status VARCHAR(20) NOT NULL DEFAULT 'COLLECTED',
    dismiss_reason VARCHAR(500) NULL,
    crawled_at DATETIME NULL,
    business_category_id BIGINT NULL,
    created_by VARCHAR(20) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (bid_notice_id),
    UNIQUE KEY uk_bid_notice_source_external_ord (crawl_source_id, external_id, notice_ord),
    KEY idx_bid_notice_business_category (business_category_id),
    KEY idx_bid_notice_creator (created_by),
    CONSTRAINT fk_bid_notice_source FOREIGN KEY (crawl_source_id) REFERENCES crawl_source (crawl_source_id),
    CONSTRAINT fk_bid_notice_business_category FOREIGN KEY (business_category_id) REFERENCES business_category (business_category_id),
    CONSTRAINT fk_bid_notice_creator FOREIGN KEY (created_by) REFERENCES employee (user_id)
);

CREATE TABLE file (
  file_id     BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '파일 번호',
  project_id  BIGINT       NOT NULL                             COMMENT '프로젝트',
  name        VARCHAR(255) NOT NULL                             COMMENT '문서명',
  created_by  VARCHAR(20)  NOT NULL                             COMMENT '생성자',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '생성일',
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
  deleted_at  DATETIME     NULL                                 COMMENT '삭제일',
  PRIMARY KEY (file_id),
  KEY idx_file_project (project_id),
  KEY idx_file_deleted (deleted_at),
  KEY idx_file_creator (created_by),
  CONSTRAINT fk_file_creator
    FOREIGN KEY (created_by) REFERENCES employee (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='파일';

CREATE TABLE crawl_run (
    crawl_run_id BIGINT AUTO_INCREMENT NOT NULL,
    crawl_condition_id BIGINT NOT NULL,
    trigger_type VARCHAR(20) NOT NULL,
    run_status VARCHAR(20) NOT NULL,
    started_at DATETIME NOT NULL,
    finished_at DATETIME NULL,
    collected_count INT NOT NULL DEFAULT 0,
    inserted_count INT NOT NULL DEFAULT 0,
    updated_count INT NOT NULL DEFAULT 0,
    skipped_count INT NOT NULL DEFAULT 0,
    error_message TEXT NULL,
    requested_by VARCHAR(20) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    PRIMARY KEY (crawl_run_id),
    KEY idx_crawl_run_requester (requested_by),
    CONSTRAINT fk_crawl_run_condition FOREIGN KEY (crawl_condition_id) REFERENCES crawl_condition (crawl_condition_id),
    CONSTRAINT fk_crawl_run_requester FOREIGN KEY (requested_by) REFERENCES employee (user_id)
);

CREATE TABLE project (
    project_id        BIGINT        NOT NULL AUTO_INCREMENT,
    bid_notice_id     BIGINT        NULL     COMMENT '입찰 공고 ID (NULL = 직접 생성)',
    name              VARCHAR(300)  NOT NULL COMMENT '프로젝트명',
    description       TEXT          NULL,
    status            ENUM('NOT_STARTED','IN_PROGRESS','SETTLEMENT','COMPLETED','CLOSED')
                                    NOT NULL DEFAULT 'NOT_STARTED',
    client_name       VARCHAR(200)  NULL     COMMENT '발주처',
    contract_amount   DECIMAL(18,2) NULL     COMMENT '계약금액 (금액의 유일한 저장 지점)',
    started_on        DATE          NULL,
    ended_on          DATE          NULL,
    closed_at         DATETIME      NULL     COMMENT '종결 일시',
    close_reason_code ENUM('NOT_PARTICIPATED','FAILED_BID','NOT_SELECTED','CANCELED') NULL,
    close_reason_note VARCHAR(500)  NULL,
    created_by        VARCHAR(20)   NOT NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        DATETIME      NULL     COMMENT '진행 전 + 스텝·블록 0개일 때만',
    PRIMARY KEY (project_id),
    UNIQUE KEY uk_project_bid_notice (bid_notice_id),
    KEY idx_project_status (status, deleted_at),
    KEY idx_project_client (client_name),
    CONSTRAINT fk_project_bid_notice FOREIGN KEY (bid_notice_id) REFERENCES bid_notice (bid_notice_id),
    CONSTRAINT fk_project_created_by FOREIGN KEY (created_by) REFERENCES employee (user_id)
) COMMENT '프로젝트';

CREATE TABLE approval (
    approval_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    block_id            BIGINT NULL COMMENT '⭐ 결재 블록 (다형성 역방향 · FK 없음 · NULL = 블록 없는 독립 결재)',
    user_id             VARCHAR(20) NOT NULL COMMENT '기안자 id',
    status              ENUM('DRAFT','IN_PROGRESS','REJECTED','COMPLETED','CANCELED') NOT NULL COMMENT '결재 상태',
    current_revision_no INT NOT NULL COMMENT '현재 상신 회차',
    completed_at        DATETIME NULL COMMENT '결재 완료 일시',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at          DATETIME NULL,
    UNIQUE KEY uk_approval_block (block_id),
    CONSTRAINT fk_approval_user FOREIGN KEY (user_id) REFERENCES employee (user_id)
) COMMENT '결재';

CREATE TABLE bid_notice_raw (
    bid_notice_raw_id BIGINT AUTO_INCREMENT NOT NULL,
    bid_notice_id BIGINT NOT NULL,
    crawl_run_id BIGINT NULL,
    source_code VARCHAR(30) NOT NULL,
    payload_type VARCHAR(20) NOT NULL,
    raw_payload JSON NOT NULL,
    raw_payload_hash CHAR(64) NOT NULL,
    parsed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    PRIMARY KEY (bid_notice_raw_id),
    UNIQUE (bid_notice_id, raw_payload_hash),

    CONSTRAINT fk_bid_notice_raw_notice
        FOREIGN KEY (bid_notice_id)
        REFERENCES bid_notice (bid_notice_id),

    CONSTRAINT fk_bid_notice_raw_run
        FOREIGN KEY (crawl_run_id)
        REFERENCES crawl_run (crawl_run_id)
);

CREATE TABLE bid_notice_summary (
    bid_notice_summary_id BIGINT AUTO_INCREMENT NOT NULL,
    bid_notice_id BIGINT NOT NULL,
    requested_by VARCHAR(20) NULL,
    prompt TEXT NULL,
    amount_summary TEXT NULL,
    schedule_summary TEXT NULL,
    task_summary TEXT NULL,
    risk_summary TEXT NULL,
    summary_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_by VARCHAR(20) NULL,
    confirmed_at DATETIME NULL,
    error_message TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (bid_notice_summary_id),
    KEY idx_bid_notice_summary_requester (requested_by),
    KEY idx_bid_notice_summary_confirmer (confirmed_by),
    CONSTRAINT fk_bid_notice_summary_notice FOREIGN KEY (bid_notice_id) REFERENCES bid_notice (bid_notice_id),
    CONSTRAINT fk_bid_notice_summary_requester FOREIGN KEY (requested_by) REFERENCES employee (user_id),
    CONSTRAINT fk_bid_notice_summary_confirmer FOREIGN KEY (confirmed_by) REFERENCES employee (user_id)
);

CREATE TABLE bid_notice_participant (
    bid_notice_participant_id BIGINT AUTO_INCREMENT NOT NULL,
    bid_notice_id BIGINT NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    business_number VARCHAR(20) NULL,
    is_representative BOOLEAN NULL,
    registration_type VARCHAR(20) NULL,
    created_by VARCHAR(20) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (bid_notice_participant_id),
    KEY idx_bid_notice_participant_creator (created_by),
    CONSTRAINT fk_bid_notice_participant_notice FOREIGN KEY (bid_notice_id) REFERENCES bid_notice (bid_notice_id),
    CONSTRAINT fk_bid_notice_participant_creator FOREIGN KEY (created_by) REFERENCES employee (user_id)
);

CREATE TABLE bid_notice_status_history (
    bid_notice_status_history_id BIGINT AUTO_INCREMENT NOT NULL,
    bid_notice_id BIGINT NOT NULL,
    previous_status VARCHAR(20) NULL,
    changed_status VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NULL,
    changed_by VARCHAR(20) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    PRIMARY KEY (bid_notice_status_history_id),
    KEY idx_bid_notice_status_history_changer (changed_by),
    CONSTRAINT fk_bid_notice_status_history_notice FOREIGN KEY (bid_notice_id) REFERENCES bid_notice (bid_notice_id),
    CONSTRAINT fk_bid_notice_status_history_changer FOREIGN KEY (changed_by) REFERENCES employee (user_id)
);

CREATE TABLE file_version (
  file_version_id     BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '파일 버전 번호',
  file_id             BIGINT       NOT NULL                          COMMENT '파일 번호',
  version_no          INT          NOT NULL                          COMMENT '버전 차수',
  upload_status       VARCHAR(20)  NOT NULL DEFAULT 'UPLOADING'      COMMENT '업로드 상태',
  storage_key         VARCHAR(500) NOT NULL                          COMMENT '파일 저장 경로',
  original_file_name  VARCHAR(255) NOT NULL                          COMMENT '원본 파일명',
  extension           VARCHAR(20)  NOT NULL                          COMMENT '확장자',
  mime_type           VARCHAR(100) NULL                              COMMENT '파일 형식',
  size_bytes          BIGINT       NOT NULL                          COMMENT '파일 크기',
  checksum            VARCHAR(128) NULL                              COMMENT '체크섬',
  page_count          INT          NULL                              COMMENT '총 페이지 수',
  comment             VARCHAR(500) NULL                              COMMENT '버전 코멘트',
  uploaded_by         VARCHAR(20)  NOT NULL                          COMMENT '업로더',
  uploader_name       VARCHAR(50)  NOT NULL                          COMMENT '업로더 이름',
  uploader_department VARCHAR(50)  NULL                              COMMENT '업로더 부서',
  uploader_position   VARCHAR(30)  NULL                              COMMENT '업로더 직책',
  created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
  completed_at        DATETIME     NULL                              COMMENT '업로드 완료 시각',
  deleted_at          DATETIME     NULL                              COMMENT '삭제일',
  PRIMARY KEY (file_version_id),
  UNIQUE KEY uk_file_version (file_id, version_no),
  KEY idx_file_version_status (upload_status, created_at),
  KEY idx_file_version_uploader (uploaded_by),
  CONSTRAINT fk_file_version_file
    FOREIGN KEY (file_id) REFERENCES file (file_id),
  CONSTRAINT fk_file_version_uploader
    FOREIGN KEY (uploaded_by) REFERENCES employee (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='파일 버전';

CREATE TABLE project_member (
    project_member_id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    permission ENUM('VIEWER','EDITOR','NONE') NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_member_id),
    CONSTRAINT uk_pm_project_user UNIQUE (project_id, user_id),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES project (project_id),
    CONSTRAINT fk_pm_user FOREIGN KEY (user_id) REFERENCES employee (user_id)
) COMMENT '프로젝트 참여자·권한';

CREATE TABLE project_business_category (
    project_business_category_id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    business_category_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_business_category_id),
    CONSTRAINT uk_pbc UNIQUE (project_id, business_category_id),
    CONSTRAINT fk_pbc_project FOREIGN KEY (project_id) REFERENCES project (project_id),
    CONSTRAINT fk_pbc_category FOREIGN KEY (business_category_id) REFERENCES business_category (business_category_id)
) COMMENT '프로젝트 사업 분류 (복수)';

CREATE TABLE stage (
    stage_id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    PRIMARY KEY (stage_id),
    CONSTRAINT fk_stage_project FOREIGN KEY (project_id) REFERENCES project (project_id),
    KEY idx_stage_project (project_id, sort_order)
) COMMENT '스테이지 (분류만 · 권한·상태 저장 안 함)';

CREATE TABLE approval_revision (
    approval_revision_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    approval_id          BIGINT NOT NULL,
    revision_no          INT NOT NULL COMMENT '상신 회차',
    title                VARCHAR(200) NOT NULL COMMENT '결재 제목',
    content              TEXT NULL COMMENT '결재 내용',
    status               ENUM('DRAFT','IN_PROGRESS','REJECTED','COMPLETED','CANCELED') NOT NULL COMMENT '결재 상태',
    submitted_at         DATETIME NULL COMMENT '상신 일시',
    finished_at          DATETIME NULL COMMENT '상신 종료일',
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '상신 생성 일시',
    updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    deleted_at           DATETIME NULL COMMENT '삭제 일시',
    UNIQUE KEY uk_approval_revision (approval_id, revision_no),
    CONSTRAINT fk_revision_approval FOREIGN KEY (approval_id) REFERENCES approval (approval_id)
) COMMENT '결재 상신';

CREATE TABLE step (
    step_id BIGINT NOT NULL AUTO_INCREMENT,
    stage_id BIGINT NULL COMMENT '분류만 · NULL 허용',
    project_id BIGINT NOT NULL COMMENT 'INV-02 · NOT NULL',
    name VARCHAR(200) NOT NULL,
    sort_order INT NOT NULL COMMENT '선행 완료 강제 없음',
    started_on DATE NULL,
    ended_on DATE NULL,
    owner_user_id VARCHAR(20) NULL COMMENT '책임자 (작업자 아님)',
    status ENUM('NOT_STARTED','IN_PROGRESS','DONE') NOT NULL DEFAULT 'NOT_STARTED',
    completed_at DATETIME NULL,
    completed_by VARCHAR(20) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    PRIMARY KEY (step_id),
    CONSTRAINT fk_step_stage FOREIGN KEY (stage_id) REFERENCES stage (stage_id),
    CONSTRAINT fk_step_project FOREIGN KEY (project_id) REFERENCES project (project_id),
    CONSTRAINT fk_step_owner FOREIGN KEY (owner_user_id) REFERENCES employee (user_id),
    CONSTRAINT fk_step_complete FOREIGN KEY (completed_by) REFERENCES employee (user_id),
    KEY idx_step_project (project_id, sort_order),
    KEY idx_step_stage (stage_id, sort_order)
) COMMENT '스텝';

CREATE TABLE approval_line (
    approval_line_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id              VARCHAR(20) NOT NULL COMMENT '결재자 사번',
    approval_revision_id BIGINT NOT NULL,
    sequence_no          INT NOT NULL COMMENT '결재 차수',
    status               ENUM('DRAFT','WAITING','ACTIVE','APPROVED','REJECTED','CANCELED') NOT NULL COMMENT '결재 회차 상태',
    opinion              TEXT NULL COMMENT '결재 의견',
    processed_at         DATETIME NULL COMMENT '결재 처리 일시',
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    deleted_at           DATETIME NULL COMMENT '삭제 일시',
    UNIQUE KEY uk_approval_line (approval_revision_id, sequence_no),
    CONSTRAINT fk_line_revision FOREIGN KEY (approval_revision_id) REFERENCES approval_revision (approval_revision_id),
    CONSTRAINT fk_line_user     FOREIGN KEY (user_id) REFERENCES employee (user_id)
) COMMENT '결재선';

CREATE TABLE step_permission (
    step_permission_id BIGINT NOT NULL AUTO_INCREMENT,
    step_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    permission ENUM('VIEWER','EDITOR','NONE') NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (step_permission_id),
    CONSTRAINT uk_sp_step_user UNIQUE (step_id, user_id),
    CONSTRAINT fk_sp_step FOREIGN KEY (step_id) REFERENCES step (step_id),
    CONSTRAINT fk_sp_user FOREIGN KEY (user_id) REFERENCES employee (user_id)
) COMMENT '스텝 권한 오버라이드 (행 없으면 프로젝트 권한 상속)';

CREATE TABLE issue (
    `issue_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '이슈 아이디',
    `title` VARCHAR(200) NULL COMMENT '이슈 제목',
    `content` TEXT NULL COMMENT '이슈 내용',
    `due_date` DATETIME NULL COMMENT '마감 일시',
    `status` ENUM('TO_DO','IN_PROGRESS','DONE') NULL COMMENT '이슈 상태',
    `step_id` BIGINT NOT NULL COMMENT '스텝 아이디',
    `start_day` DATETIME NULL COMMENT '시작 일시',
    `finish_day` DATETIME NULL COMMENT '완료 일시',
    `priority` ENUM('LOW','MEDIUM','HIGH') NULL COMMENT '우선순위',
    `created_by` VARCHAR(20) NOT NULL COMMENT '생성자 사번',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    `deleted_at` DATETIME NULL COMMENT '삭제 일시',
    CONSTRAINT `PK_ISSUE` PRIMARY KEY (`issue_id`),
    CONSTRAINT `FK_issue_step` FOREIGN KEY (`step_id`) REFERENCES `step` (`step_id`) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT `FK_issue_creator` FOREIGN KEY (`created_by`) REFERENCES `employee` (`user_id`) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE approval_document (
    approval_document_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    approval_revision_id BIGINT NOT NULL,
    file_version_id      BIGINT NOT NULL,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at           DATETIME NULL,
    CONSTRAINT fk_document_revision FOREIGN KEY (approval_revision_id) REFERENCES approval_revision (approval_revision_id),
    CONSTRAINT fk_document_file_version FOREIGN KEY (file_version_id) REFERENCES file_version (file_version_id)
) COMMENT '결재 문서';

CREATE TABLE block (
    block_id BIGINT NOT NULL AUTO_INCREMENT,
    step_id BIGINT NOT NULL COMMENT '소유 스텝',
    title VARCHAR(200) NULL COMMENT '입금확인 블록에서는 회차명',
    type ENUM('TEXT','IMAGE','FILE','CHECKLIST','PAYMENT_CONFIRM',
              'TAX_INVOICE_VIEW','PERFORMANCE_VIEW','APPROVAL','AI') NOT NULL
              COMMENT '9종 · MEMO 폐기 (2026-08-03) · 다형성 판별자',
    type_id BIGINT NULL COMMENT '⭐ 상세 행 PK (다형성 · FK 없음 · FILE/PERFORMANCE_VIEW 는 NULL)',
    owner VARCHAR(20) NULL COMMENT '블록 담당자',
    row_index INT NOT NULL,
    col_span INT NOT NULL DEFAULT 1 COMMENT '1~3',
    sort_order INT NOT NULL,
    created_by VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL COMMENT 'soft delete만 · 하드 삭제 API 없음',
    PRIMARY KEY (block_id),
    CONSTRAINT ck_block_col_span CHECK (col_span BETWEEN 1 AND 3),
    CONSTRAINT fk_block_step FOREIGN KEY (step_id) REFERENCES step (step_id),
    CONSTRAINT fk_block_owner FOREIGN KEY (owner) REFERENCES employee (user_id),
    CONSTRAINT fk_block_created_by FOREIGN KEY (created_by) REFERENCES employee (user_id),
    KEY idx_block_step (step_id, row_index, sort_order),
    KEY idx_block_type_deleted (type, deleted_at),
    KEY idx_block_type_id (type, type_id)
) COMMENT '블록 골격';
-- block.type_id 매핑:
--   TEXT             -> text.txt_id
--   IMAGE            -> image_block.img_block_id
--   CHECKLIST        -> checklist_block.chk_block_id
--   PAYMENT_CONFIRM  -> block_payment_confirm.payment_block_id
--   TAX_INVOICE_VIEW -> tax_invoice_confirm.tax_invoice_block_id
--   APPROVAL         -> approval.approval_id
--   AI               -> vitamate_block.vitamate_block_id
--   FILE/PERFORMANCE_VIEW는 별도 상세 PK 없이 관계 테이블 또는 type만 사용한다.

CREATE TABLE issue_assign (
    `issue_assign_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(20) NOT NULL COMMENT '사번',
    `issue_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `PK_ISSUE_ASSIGN` PRIMARY KEY (`issue_assign_id`),
    CONSTRAINT `UK_issue_assign` UNIQUE (`issue_id`, `user_id`),
    CONSTRAINT `FK_issue_assign_issue` FOREIGN KEY (`issue_id`) REFERENCES `issue` (`issue_id`) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT `FK_issue_assign_employee` FOREIGN KEY (`user_id`) REFERENCES `employee` (`user_id`) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE issue_block (
    issue_block_id BIGINT NOT NULL AUTO_INCREMENT,
    issue_id       BIGINT NOT NULL,
    block_id       BIGINT NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (issue_block_id),
    CONSTRAINT uk_ib UNIQUE (issue_id, block_id),
    CONSTRAINT fk_ib_issue FOREIGN KEY (issue_id) REFERENCES issue (issue_id) ON DELETE CASCADE,
    CONSTRAINT fk_ib_block FOREIGN KEY (block_id) REFERENCES block (block_id)
) COMMENT '이슈-블록 연결 (같은 스텝만 · 앱 검증)';

CREATE TABLE activity_log (
    activity_log_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '활동 로그 아이디',
    project_id          BIGINT       NULL     COMMENT '프로젝트 사건이면 필수 · 재무 탭 로그는 NULL',
    resource_type       VARCHAR(30)  NOT NULL COMMENT 'PROJECT/STAGE/STEP/BLOCK/MEMBER/ISSUE/PAYMENT/TAX_INVOICE',
    resource_id         BIGINT       NULL     COMMENT '대상 PK',
    block_id            BIGINT       NULL     COMMENT '블록 사건일 때만',
    target_name         VARCHAR(300) NOT NULL COMMENT '대상 이름 스냅샷 (INV-09)',
    act                 ENUM('CREATE','UPDATE','DELETE','COMPLETE','MOVE') NOT NULL COMMENT '활동 유형',
    field               VARCHAR(100) NULL     COMMENT '변경 필드',
    before_value        TEXT         NULL     COMMENT '변경 전 값',
    after_value         TEXT         NULL     COMMENT '변경 후 값',
    privileged_override TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '상위권한 수정 (PRJ-017)',
    user_id             VARCHAR(20)  NOT NULL COMMENT '행위자 사번',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    PRIMARY KEY (activity_log_id),
    CONSTRAINT fk_al_project FOREIGN KEY (project_id) REFERENCES project (project_id),
    CONSTRAINT fk_al_block   FOREIGN KEY (block_id)   REFERENCES block (block_id),
    CONSTRAINT fk_al_user    FOREIGN KEY (user_id)    REFERENCES employee (user_id),
    KEY idx_al_project  (project_id, created_at),
    KEY idx_al_resource (resource_type, resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='활동 로그';

CREATE TABLE notification (
    notification_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    block_id          BIGINT NULL COMMENT '블록 id (block과 무관한 알림도 있어 NULL 허용)',
    user_id           VARCHAR(20) NOT NULL COMMENT '알림 수령자 id',
    notification_type VARCHAR(50) NOT NULL COMMENT '알림 유형',
    title             VARCHAR(200) NOT NULL COMMENT '알림 제목',
    message           TEXT NULL COMMENT '알림 내용',
    read_at           DATETIME NULL COMMENT '알림 읽음 처리 일시',
    deleted_at        DATETIME NULL COMMENT '알람삭제일',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '알람생성일',
    CONSTRAINT fk_notification_block FOREIGN KEY (block_id) REFERENCES block (block_id),
    CONSTRAINT fk_notification_user  FOREIGN KEY (user_id) REFERENCES employee (user_id)
) COMMENT '알림';

CREATE TABLE `image_block` (
    `img_block_id` BIGINT NOT NULL AUTO_INCREMENT,
    `block_id` BIGINT NOT NULL COMMENT '다형성 역방향 · FK 없음',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`img_block_id`),
    UNIQUE KEY `uk_image_block_block` (`block_id`)
);

CREATE TABLE `text` (
    `txt_id` BIGINT NOT NULL AUTO_INCREMENT,
    `block_id` BIGINT NOT NULL COMMENT '다형성 역방향 · FK 없음',
    `content` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`txt_id`),
    UNIQUE KEY `uk_text_block` (`block_id`)
);

CREATE TABLE `checklist_block` (
    `chk_block_id` BIGINT NOT NULL AUTO_INCREMENT,
    `block_id` BIGINT NOT NULL COMMENT '다형성 역방향 · FK 없음',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`chk_block_id`),
    UNIQUE KEY `uk_checklist_block_block` (`block_id`)
);

CREATE TABLE vitamate_block (
    vitamate_block_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    block_id BIGINT NOT NULL COMMENT '다형성 역방향 · FK 없음',
    welcome_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    UNIQUE KEY uk_vitamate_block_block (block_id)
);

CREATE TABLE block_file (
  block_id  BIGINT      NOT NULL                            COMMENT '블록 번호 (다형성 역방향 · FK 없음)',
  file_id   BIGINT      NOT NULL                            COMMENT '파일 번호',
  linked_by VARCHAR(20) NOT NULL                            COMMENT '연결한 사람',
  linked_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '연결일',
  PRIMARY KEY (block_id, file_id),
  KEY idx_block_file_file (file_id),
  KEY idx_block_file_linker (linked_by),
  CONSTRAINT fk_block_file_file
    FOREIGN KEY (file_id) REFERENCES file (file_id) ON DELETE CASCADE,
  CONSTRAINT fk_block_file_linker
    FOREIGN KEY (linked_by) REFERENCES employee (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='블록 파일';

CREATE TABLE file_index (
    file_version_id BIGINT PRIMARY KEY,
    index_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    index_error_message TEXT NULL,
    indexed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    CONSTRAINT fk_file_index_file_version
        FOREIGN KEY (file_version_id)
        REFERENCES file_version (file_version_id)
);

CREATE TABLE document_chunk (
    document_chunk_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_version_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    page_number INT NULL,
    section_title VARCHAR(255) NULL,
    start_offset INT NULL,
    end_offset INT NULL,
    token_count INT NULL,
    chroma_id VARCHAR(150) NULL,
    embedding_model VARCHAR(100) NULL,
    embedding_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    UNIQUE KEY uk_document_chunk_version_index (file_version_id, chunk_index),
    KEY idx_document_chunk_file_version (file_version_id),
    KEY idx_document_chunk_chroma_id (chroma_id),

    CONSTRAINT fk_document_chunk_file_version
        FOREIGN KEY (file_version_id)
        REFERENCES file_version (file_version_id)
);

CREATE TABLE payment (
    payment_id    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '입금 아이디',
    project_id    BIGINT         NULL     COMMENT '프로젝트 아이디 (NULL = 미매칭)',
    block_id      BIGINT         NULL     COMMENT '연결된 입금확인 블록 (NULL = 미연결)',
    paid_at       DATE           NOT NULL COMMENT '입금일',
    amount        DECIMAL(18,2)  NOT NULL COMMENT '입금액',
    payer_name    VARCHAR(200)   NULL     COMMENT '입금자명',
    bank_memo     VARCHAR(500)   NULL     COMMENT '적요 (용역명은 안 온다)',
    source_type   VARCHAR(20)    NOT NULL COMMENT '수집 출처 MANUAL/CSV',
    bank_txn_id   VARCHAR(100)   NULL     COMMENT '은행 거래 고유번호 (중복 판정 키)',
    matched_by    VARCHAR(20)    NULL     COMMENT '프로젝트 매칭자',
    matched_at    DATETIME       NULL     COMMENT '프로젝트 매칭 일시',
    confirmed_by  VARCHAR(20)    NULL     COMMENT '입금 확정자',
    confirmed_at  DATETIME       NULL     COMMENT '입금 확정 일시',
    linked_by     VARCHAR(20)    NULL     COMMENT '블록 연결자',
    linked_at     DATETIME       NULL     COMMENT '블록 연결 일시',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    DATETIME       NULL,
    PRIMARY KEY (payment_id),
    CONSTRAINT uk_payment_bank_txn      UNIQUE (bank_txn_id),
    CONSTRAINT ck_payment_amount        CHECK  (amount > 0),
    CONSTRAINT fk_payment_project       FOREIGN KEY (project_id)   REFERENCES project (project_id),
    CONSTRAINT fk_payment_block         FOREIGN KEY (block_id)     REFERENCES block (block_id),
    CONSTRAINT fk_payment_matched_by    FOREIGN KEY (matched_by)   REFERENCES employee (user_id),
    CONSTRAINT fk_payment_confirmed_by  FOREIGN KEY (confirmed_by) REFERENCES employee (user_id),
    CONSTRAINT fk_payment_linked_by     FOREIGN KEY (linked_by)    REFERENCES employee (user_id),
    KEY idx_payment_project (project_id, deleted_at),
    KEY idx_payment_block   (block_id)
) COMMENT '입금';

CREATE TABLE tax_invoice (
    tax_invoice_id  BIGINT         NOT NULL AUTO_INCREMENT COMMENT '세금계산서 아이디',
    project_id      BIGINT         NULL     COMMENT '프로젝트 아이디 (NULL = 미매칭)',
    approval_no     VARCHAR(100)   NOT NULL COMMENT '승인번호 (중복 판정 키)',
    issued_on       DATE           NOT NULL COMMENT '발행일',
    supply_amount   DECIMAL(18,2)  NOT NULL COMMENT '공급가액',
    vat_amount      DECIMAL(18,2)  NOT NULL COMMENT '부가세액',
    total_amount    DECIMAL(18,2)  NOT NULL COMMENT '합계 (홈택스 원본값)',
    buyer_name      VARCHAR(200)   NOT NULL COMMENT '공급받는자',
    buyer_biz_no    VARCHAR(20)    NULL     COMMENT '공급받는자 사업자번호',
    source_type     VARCHAR(20)    NOT NULL COMMENT '수집 출처 CSV/HOMETAX_API',
    matched_by      VARCHAR(20)    NULL     COMMENT '프로젝트 매칭자',
    matched_at      DATETIME       NULL     COMMENT '프로젝트 매칭 일시',
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수집 일시',
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME       NULL,
    PRIMARY KEY (tax_invoice_id),
    CONSTRAINT uk_tax_invoice_approval_no UNIQUE (approval_no),
    CONSTRAINT fk_tax_invoice_project     FOREIGN KEY (project_id) REFERENCES project (project_id),
    CONSTRAINT fk_tax_invoice_matched_by  FOREIGN KEY (matched_by) REFERENCES employee (user_id),
    KEY idx_tax_invoice_project (project_id, deleted_at),
    KEY idx_tax_invoice_issued  (issued_on)
) COMMENT '세금계산서 (홈택스 사본)';

CREATE TABLE `image` (
    `img_id` BIGINT NOT NULL AUTO_INCREMENT,
    `img_block_id` BIGINT NOT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `image_url` TEXT NOT NULL,
    `extension` VARCHAR(10) NOT NULL,
    `size` BIGINT NOT NULL,
    `caption` TEXT NULL,
    `order_index` INT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`img_id`),
    CONSTRAINT `fk_image_img_block` FOREIGN KEY (`img_block_id`) REFERENCES `image_block` (`img_block_id`)
);

CREATE TABLE `checklist` (
    `chk_id` BIGINT NOT NULL AUTO_INCREMENT,
    `chk_block_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `is_completed` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`chk_id`),
    CONSTRAINT `fk_checklist_chk_block` FOREIGN KEY (`chk_block_id`) REFERENCES `checklist_block` (`chk_block_id`)
);

CREATE TABLE block_payment_confirm (
    payment_block_id BIGINT        NOT NULL AUTO_INCREMENT,
    block_id         BIGINT        NOT NULL COMMENT '입금확인 블록 (다형성 역방향 · FK 없음)',
    project_id       BIGINT        NOT NULL COMMENT 'round_no UNIQUE 스코프 · MTC-004 검증 근거',
    round_no         INT           NOT NULL COMMENT '프로젝트 내 정산 회차 번호',
    planned_date     DATE          NULL     COMMENT '입금 예정일 (미입력이 정상)',
    planned_amount   DECIMAL(18,2) NULL     COMMENT '예정금액 (미계획이 정상)',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at       DATETIME      NULL     COMMENT '⚠️ 삭제 판정은 block.deleted_at 이 우선 (BLK-007)',
    PRIMARY KEY (payment_block_id),
    CONSTRAINT uk_bpc_block    UNIQUE (block_id),
    CONSTRAINT uk_bpc_round    UNIQUE (project_id, round_no),
    CONSTRAINT fk_bpc_project  FOREIGN KEY (project_id) REFERENCES project (project_id),
    KEY idx_bpc_planned_date (planned_date)
) COMMENT '정산 회차 (입금확인 블록 상세)';

CREATE TABLE tax_invoice_confirm (
    tax_invoice_block_id BIGINT      NOT NULL AUTO_INCREMENT,
    block_id             BIGINT      NOT NULL COMMENT '세금계산서 조회 블록 (다형성 역방향 · FK 없음)',
    tax_invoice_id       BIGINT      NOT NULL COMMENT '연결된 계산서',
    linked_by            VARCHAR(20) NOT NULL COMMENT '연결 처리자 (재무만)',
    linked_at            DATETIME    NOT NULL COMMENT '연결 일시',
    PRIMARY KEY (tax_invoice_block_id),
    CONSTRAINT uk_tic_block       UNIQUE (block_id),
    CONSTRAINT uk_tic_tax_invoice UNIQUE (tax_invoice_id),
    CONSTRAINT fk_tic_tax_invoice FOREIGN KEY (tax_invoice_id) REFERENCES tax_invoice (tax_invoice_id),
    CONSTRAINT fk_tic_linked_by   FOREIGN KEY (linked_by)      REFERENCES employee (user_id)
) COMMENT '세금계산서 조회 블록 상세';

CREATE TABLE vitamate_analysis (
    vitamate_analysis_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vitamate_block_id BIGINT NOT NULL,
    requested_by VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    prompt TEXT NOT NULL,
    result LONGTEXT NULL,
    analysis_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processing_attempt_id VARCHAR(36) NULL,
    processing_started_at DATETIME NULL,
    lease_expires_at DATETIME NULL,
    error_message TEXT NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    UNIQUE KEY uk_vitamate_analysis_idempotency (vitamate_block_id, requested_by, idempotency_key),
    KEY idx_vitamate_analysis_block_status (vitamate_block_id, analysis_status, created_at),
    KEY idx_vitamate_analysis_requested_by (requested_by),
    CONSTRAINT fk_vitamate_analysis_block FOREIGN KEY (vitamate_block_id) REFERENCES vitamate_block (vitamate_block_id),
    CONSTRAINT fk_vitamate_analysis_requested_by FOREIGN KEY (requested_by) REFERENCES employee (user_id)
);

CREATE TABLE vitamate_analysis_document (
    vitamate_analysis_document_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vitamate_analysis_id BIGINT NOT NULL,
    file_version_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    UNIQUE KEY uk_vitamate_analysis_document (
        vitamate_analysis_id,
        file_version_id
    ),
    KEY idx_vitamate_analysis_document_file_version (file_version_id),

    CONSTRAINT fk_vitamate_analysis_document_analysis
        FOREIGN KEY (vitamate_analysis_id)
        REFERENCES vitamate_analysis (vitamate_analysis_id),

    CONSTRAINT fk_vitamate_analysis_document_file_version
        FOREIGN KEY (file_version_id)
        REFERENCES file_version (file_version_id)
);

CREATE TABLE vitamate_analysis_citation (
    vitamate_analysis_citation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vitamate_analysis_id BIGINT NOT NULL,
    vitamate_analysis_document_id BIGINT NOT NULL,
    document_chunk_id BIGINT NOT NULL,
    rank_order INT NOT NULL,
    distance_score DECIMAL(10, 6) NULL,
    excerpt TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    UNIQUE KEY uk_vitamate_analysis_citation_rank (
        vitamate_analysis_id,
        rank_order
    ),
    KEY idx_vitamate_analysis_citation_document (
        vitamate_analysis_document_id
    ),
    KEY idx_vitamate_analysis_citation_chunk (document_chunk_id),

    CONSTRAINT fk_vitamate_analysis_citation_analysis
        FOREIGN KEY (vitamate_analysis_id)
        REFERENCES vitamate_analysis (vitamate_analysis_id),

    CONSTRAINT fk_vitamate_analysis_citation_document
        FOREIGN KEY (vitamate_analysis_document_id)
        REFERENCES vitamate_analysis_document (vitamate_analysis_document_id),

    CONSTRAINT fk_vitamate_analysis_citation_chunk
        FOREIGN KEY (document_chunk_id)
        REFERENCES document_chunk (document_chunk_id)
);
