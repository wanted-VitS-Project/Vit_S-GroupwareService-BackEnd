-- =====================================================================
-- 핵심 도메인 스키마
-- 근거: .ai/DOMAIN.md (2026-07-31 확정)
--
-- 규칙
--   · enum 은 VARCHAR 로 둔다. DB ENUM 은 값 추가 때마다 DDL 이 나간다.
--   · 삭제는 전면 soft delete (deleted_at). 하드 삭제 없음 — DOMAIN §11-1
--   · 잠금(입금 연결 블록 · 진행 중 결재 · 결재 대상 파일)은 애플리케이션에서 막는다.
--     FK 는 데이터 깨짐 방지용 안전망일 뿐이다.
--   · DB 기본 charset 이 utf8mb4 여야 한다.
-- =====================================================================


-- =====================================================================
-- 1. 조직 · 계정                                        DOMAIN §14
-- =====================================================================

CREATE TABLE department (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  DATETIME NULL,
    -- ⚠️ (name, deleted_at) 복합 UNIQUE 를 쓰면 안 된다.
    --    MySQL 은 UNIQUE 인덱스에서 NULL 을 서로 다른 값으로 취급하므로
    --    deleted_at IS NULL 인 행이 같은 이름으로 여러 개 들어간다.
    --    소프트 삭제된 이름의 재사용은 포기한다 (부서명 재사용은 드물다).
    UNIQUE KEY uk_department_name (name)
);

-- 계정과 사원은 한 테이블이다 (DOMAIN §14-1).
-- 이벤트스토밍에서 둘로 나뉜 건 행위 종류가 달라서지 엔티티가 둘이라서가 아니다.
CREATE TABLE users (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_id                  VARCHAR(100) NOT NULL,
    password                  VARCHAR(255) NOT NULL,
    name                      VARCHAR(50)  NOT NULL,
    email                     VARCHAR(255) NULL,
    employee_no               VARCHAR(50)  NULL,
    department_id             BIGINT       NULL,
    position                  VARCHAR(50)  NULL,          -- 직급 (팀장 · 대리 …)
    status                    VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE · INACTIVE
    password_change_required  TINYINT(1)   NOT NULL DEFAULT 1,         -- 초기 비밀번호 강제 변경
    hired_on                  DATE         NULL,
    resigned_on               DATE         NULL,          -- 퇴사 처리해도 레코드는 남는다
    created_at                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_login_id (login_id),
    KEY idx_users_department (department_id),
    CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES department (id)
);

-- 다중 부여. DEVELOPER · ADMIN · EXECUTIVE · FINANCE · BIDDING · MEMBER
-- 부서에서 상속하지 않는다 — 인원별 지정만 (DOMAIN §6-1)
CREATE TABLE user_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    role        VARCHAR(30) NOT NULL,
    granted_by  BIGINT      NULL,
    granted_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE business_category (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  DATETIME NULL,
    UNIQUE KEY uk_business_category_name (name)   -- department 와 같은 이유 (NULL 중복 문제)
);


-- =====================================================================
-- 2. 공고 · 입찰  (프로젝트 이전 영역)                    DOMAIN §1
-- =====================================================================

CREATE TABLE crawl_link (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    url         VARCHAR(1000) NOT NULL,
    enabled     TINYINT(1) NOT NULL DEFAULT 1,
    created_by  BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  DATETIME NULL,
    CONSTRAINT fk_crawl_link_user FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE bid_notice (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    crawl_link_id    BIGINT NULL,
    notice_no        VARCHAR(100) NULL,
    title            VARCHAR(500) NOT NULL,
    agency           VARCHAR(200) NULL,          -- 공고기관
    demand_agency    VARCHAR(200) NULL,          -- 수요기관
    base_amount      DECIMAL(18,2) NULL,         -- 기초금액
    estimated_price  DECIMAL(18,2) NULL,         -- 추정가격
    announced_at     DATETIME NULL,
    bid_deadline_at  DATETIME NULL,              -- 투찰마감 — 캘린더 `불가역` 등급
    opening_at       DATETIME NULL,              -- 개찰일  — 캘린더 `불가역` 등급
    region_limit     VARCHAR(200) NULL,
    industry_limit   VARCHAR(200) NULL,
    evaluation_type  VARCHAR(30)  NULL,          -- PROPOSAL · PRICE · QUALIFICATION
    source_url       VARCHAR(1000) NULL,
    crawled_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_bid_notice_deadline (bid_deadline_at),
    CONSTRAINT fk_bid_notice_link FOREIGN KEY (crawl_link_id) REFERENCES crawl_link (id)
);

-- AI 요약. 프로젝트 안의 AI 블록과 다른 기능이다 (USECASE UC-03)
CREATE TABLE bid_notice_summary (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    bid_notice_id  BIGINT NOT NULL,
    content        TEXT NOT NULL,
    confirmed      TINYINT(1) NOT NULL DEFAULT 0,   -- 사람이 검토·수정해 확정했는가
    created_by     BIGINT NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_summary_notice (bid_notice_id),
    CONSTRAINT fk_summary_notice FOREIGN KEY (bid_notice_id) REFERENCES bid_notice (id),
    CONSTRAINT fk_summary_user   FOREIGN KEY (created_by)    REFERENCES users (id)
);


-- =====================================================================
-- 3. 프로젝트 · 계층                                DOMAIN §2 · §5
-- =====================================================================

CREATE TABLE project (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                  VARCHAR(300) NOT NULL,
    description           TEXT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
                          -- NOT_STARTED · IN_PROGRESS · SETTLEMENT · COMPLETED · CLOSED
    business_category_id  BIGINT NULL,
    started_on            DATE NULL,
    ended_on              DATE NULL,

    -- 공고 연결. NULL 이면 B2B 직접 생성 (정상 케이스)
    bid_notice_id         BIGINT NULL,
    notice_snapshot_at    DATETIME NULL,          -- 스냅샷 시점
    notice_changed        TINYINT(1) NOT NULL DEFAULT 0,  -- 재크롤링으로 원본이 바뀜 → 배지

    -- 금액은 프로젝트 속성이다. 블록에 흩지 않는다 (DOMAIN §2)
    base_amount           DECIMAL(18,2) NULL,     -- 기초금액   (공고 스냅샷)
    estimated_price       DECIMAL(18,2) NULL,     -- 추정가격   (공고 스냅샷)
    bid_amount            DECIMAL(18,2) NULL,     -- 투찰금액   UC-10
    award_amount          DECIMAL(18,2) NULL,     -- 낙찰가     UC-11
    award_rank            INT NULL,               -- 순위       UC-11
    contract_amount       DECIMAL(18,2) NULL,     -- 계약금액 — §7-3 잔액 계산이 여기에 의존한다

    -- 개찰 결과 · 종결 사유. 사유 없이 종결할 수 없다 (DOMAIN §2)
    bid_result            VARCHAR(30) NULL,       -- AWARDED · SELECTED · FAILED · NOT_SELECTED · CANCELED
    close_reason_code     VARCHAR(50) NULL,
    close_reason_note     TEXT NULL,
    closed_at             DATETIME NULL,

    -- 이슈 번호 발급용 프로젝트 연번 (DOMAIN §4)
    issue_seq             INT NOT NULL DEFAULT 0,

    created_by            BIGINT NOT NULL,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at            DATETIME NULL,          -- `진행 전` + 내용 없을 때만 허용

    KEY idx_project_status (status, deleted_at),
    CONSTRAINT fk_project_category FOREIGN KEY (business_category_id) REFERENCES business_category (id),
    CONSTRAINT fk_project_notice   FOREIGN KEY (bid_notice_id)        REFERENCES bid_notice (id),
    CONSTRAINT fk_project_user     FOREIGN KEY (created_by)           REFERENCES users (id)
);

-- 참여자는 개인 단위로만 확정한다 (DOMAIN §2)
CREATE TABLE project_member (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    permission  VARCHAR(20) NOT NULL,   -- VIEWER · EDITOR · MANAGER
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_member (project_id, user_id),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_pm_user    FOREIGN KEY (user_id)    REFERENCES users (id)
);

-- 담당팀 = 집계용 라벨 + 인력 선택 후보 필터. 참여자를 자동으로 늘리지 않는다 (DOMAIN §2)
CREATE TABLE project_department (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id     BIGINT NOT NULL,
    department_id  BIGINT NOT NULL,
    UNIQUE KEY uk_project_department (project_id, department_id),
    CONSTRAINT fk_pd_project    FOREIGN KEY (project_id)    REFERENCES project (id),
    CONSTRAINT fk_pd_department FOREIGN KEY (department_id) REFERENCES department (id)
);

-- 스테이지는 묶음 라벨이다. 상태도 권한도 갖지 않는다 (DOMAIN §2)
CREATE TABLE stage (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT NOT NULL,
    name        VARCHAR(100) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  DATETIME NULL,
    KEY idx_stage_project (project_id, sort_order),
    CONSTRAINT fk_stage_project FOREIGN KEY (project_id) REFERENCES project (id)
);

-- 스텝 = 노션의 페이지. 타입이 없다. sort_order 는 정렬일 뿐 선행 완료를 강제하지 않는다
CREATE TABLE step (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id    BIGINT NOT NULL,
    stage_id      BIGINT NULL,
    name          VARCHAR(200) NOT NULL,
    sort_order    INT NOT NULL DEFAULT 0,
    started_on    DATE NULL,          -- 날짜가 있으면 캘린더에 뜬다. 별도 스위치 없음
    ended_on      DATE NULL,
    owner_user_id BIGINT NULL,        -- 그 단계 책임자. 작업자가 아니다
    status        VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',  -- NOT_STARTED · IN_PROGRESS · DONE
    completed_at  DATETIME NULL,      -- 송부 스텝에서는 이 값이 곧 발송일시
    completed_by  BIGINT NULL,        -- 송부 스텝에서는 이 값이 곧 발송자
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    DATETIME NULL,
    KEY idx_step_project (project_id, sort_order),
    KEY idx_step_stage (stage_id),
    CONSTRAINT fk_step_project      FOREIGN KEY (project_id)    REFERENCES project (id),
    CONSTRAINT fk_step_stage        FOREIGN KEY (stage_id)      REFERENCES stage (id),
    CONSTRAINT fk_step_owner        FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_step_completed_by FOREIGN KEY (completed_by)  REFERENCES users (id)
);

-- 권한 저장은 프로젝트 + 스텝 2층뿐이다. 스테이지에는 저장하지 않는다 (DOMAIN §6-2)
CREATE TABLE step_permission (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    step_id     BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    permission  VARCHAR(20) NOT NULL,   -- VIEWER · EDITOR · MANAGER
    UNIQUE KEY uk_step_permission (step_id, user_id),
    CONSTRAINT fk_sp_step FOREIGN KEY (step_id) REFERENCES step (id),
    CONSTRAINT fk_sp_user FOREIGN KEY (user_id) REFERENCES users (id)
);


-- =====================================================================
-- 4. 블록                                              DOMAIN §3
--    공통 테이블 + 타입별 상세 테이블. 확장형 JSON 스키마가 아니다.
-- =====================================================================

CREATE TABLE block (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    step_id     BIGINT NOT NULL,
    project_id  BIGINT NOT NULL,        -- 비정규화. 프로젝트 전역 조회가 스텝 조인 없이 돼야 한다
    type        VARCHAR(30) NOT NULL,
                -- TEXT · IMAGE · FILE · CHECKLIST · MEMO
                -- PAYMENT_CONFIRM · TAX_INVOICE_VIEW · PERFORMANCE_VIEW
                -- APPROVAL · AI
    title       VARCHAR(200) NULL,      -- 입금확인 블록에서는 회차명 (`2차 정산`)
    sort_order  INT NOT NULL DEFAULT 0,
    created_by  BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  DATETIME NULL,
    KEY idx_block_step (step_id, sort_order),
    KEY idx_block_project_type (project_id, type, deleted_at),
    CONSTRAINT fk_block_step    FOREIGN KEY (step_id)    REFERENCES step (id),
    CONSTRAINT fk_block_project FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_block_user    FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE block_text (
    block_id  BIGINT PRIMARY KEY,
    content   MEDIUMTEXT NULL,
    CONSTRAINT fk_block_text FOREIGN KEY (block_id) REFERENCES block (id)
);

CREATE TABLE block_memo (
    block_id  BIGINT PRIMARY KEY,
    content   MEDIUMTEXT NULL,
    CONSTRAINT fk_block_memo FOREIGN KEY (block_id) REFERENCES block (id)
);

-- 체크리스트는 담당자·기한을 갖지 않는다. 필요하면 이슈로 승격한다 (DOMAIN §3-3)
CREATE TABLE checklist_item (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    block_id    BIGINT NOT NULL,
    content     VARCHAR(500) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    checked     TINYINT(1) NOT NULL DEFAULT 0,
    checked_by  BIGINT NULL,
    checked_at  DATETIME NULL,
    issue_id    BIGINT NULL,            -- 이슈로 승격했으면 연결. 이슈 완료 시 자동 체크
    deleted_at  DATETIME NULL,
    KEY idx_checklist_block (block_id, sort_order),
    CONSTRAINT fk_checklist_block FOREIGN KEY (block_id) REFERENCES block (id)
);

-- 실제 저장물. 파일·이미지가 공유한다
CREATE TABLE attachment (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    storage_key    VARCHAR(500) NOT NULL,
    original_name  VARCHAR(300) NOT NULL,
    content_type   VARCHAR(100) NULL,
    size_bytes     BIGINT NOT NULL DEFAULT 0,
    uploaded_by    BIGINT NOT NULL,
    uploaded_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attachment_user FOREIGN KEY (uploaded_by) REFERENCES users (id)
);

-- 파일은 하나고 버전이 붙는다. `보고서_최종2.xlsx` 를 만들지 않는다 (PRODUCT §5-②)
CREATE TABLE block_file_version (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    block_id       BIGINT NOT NULL,
    version_no     INT NOT NULL,
    attachment_id  BIGINT NOT NULL,
    note           VARCHAR(500) NULL,
    created_by     BIGINT NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     DATETIME NULL,
    UNIQUE KEY uk_file_version (block_id, version_no),
    CONSTRAINT fk_fv_block      FOREIGN KEY (block_id)      REFERENCES block (id),
    CONSTRAINT fk_fv_attachment FOREIGN KEY (attachment_id) REFERENCES attachment (id),
    CONSTRAINT fk_fv_user       FOREIGN KEY (created_by)    REFERENCES users (id)
);

CREATE TABLE block_image (
    block_id       BIGINT PRIMARY KEY,
    attachment_id  BIGINT NOT NULL,
    caption        VARCHAR(500) NULL,
    CONSTRAINT fk_bi_block      FOREIGN KEY (block_id)      REFERENCES block (id),
    CONSTRAINT fk_bi_attachment FOREIGN KEY (attachment_id) REFERENCES attachment (id)
);

CREATE TABLE block_ai (
    block_id     BIGINT PRIMARY KEY,
    prompt       TEXT NULL,
    result       MEDIUMTEXT NULL,
    executed_at  DATETIME NULL,
    CONSTRAINT fk_block_ai FOREIGN KEY (block_id) REFERENCES block (id)
);

-- 결재. 파일을 복사하지 않고 파일 블록의 특정 버전을 지목한다 (DOMAIN §8-2 · §8-3)
CREATE TABLE block_approval (
    block_id            BIGINT PRIMARY KEY,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                        -- DRAFT · IN_PROGRESS · APPROVED · REJECTED · WITHDRAWN
    target_block_id     BIGINT NULL,    -- 지목한 파일 블록
    target_version_id   BIGINT NULL,    -- 상신 시점에 고정한 버전
    current_line_order  INT NULL,       -- 지금 활성화된 결재선 순번
    submitted_by        BIGINT NULL,
    submitted_at        DATETIME NULL,
    completed_at        DATETIME NULL,
    CONSTRAINT fk_ba_block   FOREIGN KEY (block_id)          REFERENCES block (id),
    CONSTRAINT fk_ba_target  FOREIGN KEY (target_block_id)   REFERENCES block (id),
    CONSTRAINT fk_ba_version FOREIGN KEY (target_version_id) REFERENCES block_file_version (id)
);

-- 순차 진행. 마지막 순번이 Master 다 — MASTER role 은 존재하지 않는다 (DOMAIN §8-1)
CREATE TABLE approval_line (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    approval_block_id  BIGINT NOT NULL,
    line_order         INT NOT NULL,
    approver_user_id   BIGINT NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'WAITING',
                       -- WAITING · ACTIVE · APPROVED · REJECTED
    comment            TEXT NULL,      -- 반려 사유는 필수 (DOMAIN §8-5)
    acted_at           DATETIME NULL,
    UNIQUE KEY uk_approval_line (approval_block_id, line_order),
    CONSTRAINT fk_al_approval FOREIGN KEY (approval_block_id) REFERENCES block_approval (block_id),
    CONSTRAINT fk_al_user     FOREIGN KEY (approver_user_id)  REFERENCES users (id)
);


-- =====================================================================
-- 5. 재무  (프로젝트 밖 · 병렬 영역)                      DOMAIN §7
-- =====================================================================

-- 입금. project_id 가 1단 매칭, 블록 연결이 2단이다 (DOMAIN §7-4)
-- 돈은 블록보다 먼저 들어올 수 있다.
CREATE TABLE payment (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT NULL,            -- ① 프로젝트 매칭
    paid_at     DATETIME NOT NULL,
    amount      DECIMAL(18,2) NOT NULL,
    payer_name  VARCHAR(200) NULL,
    bank_memo   VARCHAR(500) NULL,      -- 적요. 용역명은 안 온다 — 자동 매칭은 추천까지만
    matched_by  BIGINT NULL,            -- 확정 버튼은 사람이 누른다
    matched_at  DATETIME NULL,
    created_by  BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  DATETIME NULL,
    KEY idx_payment_project (project_id, paid_at),
    CONSTRAINT fk_payment_project FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_payment_user    FOREIGN KEY (created_by) REFERENCES users (id)
);

-- 입금확인 블록 = 정산 회차 그 자체. 회차명은 block.title 이다 (DOMAIN §7-3)
-- payment_id 가 채워진 블록·스텝은 삭제할 수 없다 (DOMAIN §11-2)
CREATE TABLE block_payment_confirm (
    block_id    BIGINT PRIMARY KEY,
    payment_id  BIGINT NULL,            -- ② 블록 연결. 재무만 연결·해제한다
    linked_by   BIGINT NULL,
    linked_at   DATETIME NULL,
    CONSTRAINT fk_bpc_block   FOREIGN KEY (block_id)   REFERENCES block (id),
    CONSTRAINT fk_bpc_payment FOREIGN KEY (payment_id) REFERENCES payment (id)
);

CREATE TABLE tax_invoice (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id      BIGINT NULL,
    invoice_no      VARCHAR(100) NULL,
    issued_on       DATE NOT NULL,
    supply_amount   DECIMAL(18,2) NOT NULL,
    vat_amount      DECIMAL(18,2) NOT NULL DEFAULT 0,
    created_by      BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      DATETIME NULL,
    KEY idx_tax_invoice_project (project_id, issued_on),
    CONSTRAINT fk_ti_project FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_ti_user    FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE performance (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id     BIGINT NOT NULL,
    category       VARCHAR(100) NULL,
    registered_on  DATE NOT NULL,
    amount         DECIMAL(18,2) NULL,
    created_by     BIGINT NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     DATETIME NULL,
    KEY idx_performance_project (project_id),
    CONSTRAINT fk_perf_project FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_perf_user    FOREIGN KEY (created_by) REFERENCES users (id)
);


-- =====================================================================
-- 6. 이슈                                              DOMAIN §4
--    소유는 스텝 하나. 조회 축은 스텝 · 프로젝트 · 담당자 셋.
-- =====================================================================

CREATE TABLE issue (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id        BIGINT NOT NULL,
    step_id           BIGINT NOT NULL,        -- 소유자. step_id 변경으로 이동 가능
    issue_no          INT NOT NULL,           -- 프로젝트 전역 연번 (project.issue_seq 로 발급)
    title             VARCHAR(300) NOT NULL,
    content           MEDIUMTEXT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'TODO',   -- TODO · IN_PROGRESS · DONE
    assignee_user_id  BIGINT NULL,            -- 담당자가 있어야 이슈다 (DOMAIN §3-3)
    due_on            DATE NULL,
    created_by        BIGINT NOT NULL,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    closed_at         DATETIME NULL,
    deleted_at        DATETIME NULL,
    UNIQUE KEY uk_issue_no (project_id, issue_no),
    KEY idx_issue_step (step_id, status),
    KEY idx_issue_assignee (assignee_user_id, status),
    CONSTRAINT fk_issue_project  FOREIGN KEY (project_id)       REFERENCES project (id),
    CONSTRAINT fk_issue_step     FOREIGN KEY (step_id)          REFERENCES step (id),
    CONSTRAINT fk_issue_assignee FOREIGN KEY (assignee_user_id) REFERENCES users (id),
    CONSTRAINT fk_issue_user     FOREIGN KEY (created_by)       REFERENCES users (id)
);

-- 연관 블록 (선택)
CREATE TABLE issue_block (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_id  BIGINT NOT NULL,
    block_id  BIGINT NOT NULL,
    UNIQUE KEY uk_issue_block (issue_id, block_id),
    CONSTRAINT fk_ib_issue FOREIGN KEY (issue_id) REFERENCES issue (id),
    CONSTRAINT fk_ib_block FOREIGN KEY (block_id) REFERENCES block (id)
);


-- =====================================================================
-- 7. 템플릿                                            DOMAIN §13
--    중첩은 스냅샷이다. 하위 템플릿을 참조하지 않으므로 payload 하나면 된다.
-- =====================================================================

CREATE TABLE template (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    scope_type     VARCHAR(20) NOT NULL,   -- PROJECT · STAGE · STEP · BLOCK
    name           VARCHAR(200) NOT NULL,
    description    VARCHAR(500) NULL,
    visibility     VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',  -- PRIVATE · TEAM · COMPANY
    owner_user_id  BIGINT NOT NULL,
    department_id  BIGINT NULL,            -- visibility=TEAM 일 때 공개 범위
    payload        JSON NOT NULL,          -- 구조 + 설정 스냅샷. 파일·실적은 담지 않는다
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     DATETIME NULL,
    KEY idx_template_scope (scope_type, visibility, deleted_at),
    CONSTRAINT fk_template_user       FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_template_department FOREIGN KEY (department_id) REFERENCES department (id)
);


-- =====================================================================
-- 8. 로그 · 알림                                    DOMAIN §11-4 · §9
-- =====================================================================

-- 로그는 어떤 경우에도 삭제하지 않는다.
-- target_name 은 스냅샷이다 — 원본이 지워져도 읽혀야 한다.
CREATE TABLE activity_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    step_id         BIGINT NULL,
    block_id        BIGINT NULL,
    actor_user_id   BIGINT NOT NULL,
    action          VARCHAR(50) NOT NULL,   -- CREATE · UPDATE · DELETE · UPLOAD · APPROVE · REJECT …
    target_type     VARCHAR(30) NOT NULL,   -- BLOCK · STEP · ISSUE · FILE · APPROVAL …
    target_id       BIGINT NULL,
    target_name     VARCHAR(300) NULL,      -- ⚠️ 복사본. FK 로만 두면 표시가 깨진다
    admin_override  TINYINT(1) NOT NULL DEFAULT 0,  -- ADMIN 총관리자 권한으로 수정했는가
    detail          JSON NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_log_project (project_id, created_at),
    KEY idx_log_step (step_id, created_at),
    CONSTRAINT fk_log_project FOREIGN KEY (project_id)     REFERENCES project (id),
    CONSTRAINT fk_log_actor   FOREIGN KEY (actor_user_id)  REFERENCES users (id)
);

-- kind 를 반드시 나눠라. 섞으면 사람들이 알림을 끄고, 그러면 결재 상신도 못 본다 (DOMAIN §9)
CREATE TABLE notification (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id  BIGINT NOT NULL,
    kind               VARCHAR(10) NOT NULL,   -- ACTION (뱃지) · INFO (피드)
    category           VARCHAR(50) NOT NULL,   -- ISSUE_ASSIGNED · APPROVAL_REQUESTED · DUE_SOON …
    title              VARCHAR(300) NOT NULL,
    resource_type      VARCHAR(30) NULL,
    resource_id        BIGINT NULL,
    link_url           VARCHAR(500) NULL,
    read_at            DATETIME NULL,
    resolved_at        DATETIME NULL,          -- ACTION 은 처리될 때까지 안 사라진다
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notification_recipient (recipient_user_id, kind, read_at),
    CONSTRAINT fk_noti_user FOREIGN KEY (recipient_user_id) REFERENCES users (id)
);


-- =====================================================================
-- 뒤늦게 거는 FK  (issue 테이블이 checklist_item 보다 아래에서 생성된다)
-- =====================================================================

ALTER TABLE checklist_item
    ADD CONSTRAINT fk_checklist_issue FOREIGN KEY (issue_id) REFERENCES issue (id);
