-- 사내 문서함 (COMPANY-DOC-V1) — 회사 소속 문서 + 버전
-- 무엇: company_document(문서) + company_document_version(버전 append-only) 2 테이블 신설.
-- 왜: 재정·소개·실적·인증 등 회사 기준 자료(AI 공고 검토의 비교자료). 프로젝트 파일(file)과 별개 애그리거트다
--     — file 은 프로젝트 소속(INV-05)이라 재사용 불가. 저장소·업로더 스냅샷·미리보기 인프라만 file 도메인과 공유한다.
-- 결정(COMPANY-DOC-V1 §6): 카테고리=고정 enum(§6-1) · 권한 ADMIN(§6-3) · soft delete(§6-4) · 업로더 스냅샷 nullable(§6-6).
--     업로더 스냅샷을 nullable 로 둔 이유: 사내 문서는 ADMIN 이 올리는데 ADMIN 은 employee 행이 없어 조회가 빈다.
-- 회사 스코프: file 과 달리 company_document 가 직접 company_id 를 가진다(프로젝트를 안 타므로).
-- 번호: develop 최대 V20260813020100 위 030000 으로 재번호(CI 마이그레이션 순서 검사 — 신규는 기준 최대보다 커야 함).

CREATE TABLE company_document (
    company_document_id BIGINT       NOT NULL AUTO_INCREMENT,
    company_id          BIGINT       NOT NULL                            COMMENT '회사(테넌트)',
    category            VARCHAR(30)  NOT NULL                            COMMENT '분류 enum(FINANCE·COMPANY_INTRO·PERFORMANCE·CERTIFICATE·ETC)',
    name                VARCHAR(255) NOT NULL                            COMMENT '표시명',
    created_by          VARCHAR(20)  NOT NULL                            COMMENT '생성자(사번)',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '생성일',
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
    deleted_at          DATETIME     NULL                                COMMENT 'soft delete(§6-4)',
    PRIMARY KEY (company_document_id),
    KEY idx_cd_company_deleted (company_id, deleted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '사내 문서(회사 소속) · COMPANY-DOC-V1';

CREATE TABLE company_document_version (
    company_document_version_id BIGINT       NOT NULL AUTO_INCREMENT,
    company_document_id         BIGINT       NOT NULL                            COMMENT '문서 번호',
    version_no                  INT          NOT NULL                            COMMENT '버전 차수',
    upload_status               VARCHAR(20)  NOT NULL DEFAULT 'UPLOADING'        COMMENT 'UPLOADING·COMPLETED·FAILED',
    storage_key                 VARCHAR(500) NOT NULL                            COMMENT 'S3 키(companies/{companyId}/documents/...)',
    original_file_name          VARCHAR(255) NOT NULL                            COMMENT '원본 파일명',
    extension                   VARCHAR(20)  NOT NULL                            COMMENT '확장자',
    mime_type                   VARCHAR(100) NULL                                COMMENT 'MIME',
    size_bytes                  BIGINT       NOT NULL                            COMMENT '파일 크기',
    checksum                    VARCHAR(128) NULL                                COMMENT '체크섬(선택)',
    page_count                  INT          NULL                                COMMENT 'PDF 페이지 수',
    comment                     VARCHAR(500) NULL                                COMMENT '버전 코멘트',
    uploaded_by                 VARCHAR(20)  NOT NULL                            COMMENT '업로더(사번) · 항상 기록',
    uploader_name               VARCHAR(50)  NULL                                COMMENT '스냅샷 이름(nullable · §6-6)',
    uploader_department         VARCHAR(50)  NULL                                COMMENT '스냅샷 부서',
    uploader_position           VARCHAR(30)  NULL                                COMMENT '스냅샷 직급',
    completed_at                DATETIME     NULL                                COMMENT '완료 시각',
    deleted_at                  DATETIME     NULL                                COMMENT 'soft delete',
    created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '생성일',
    PRIMARY KEY (company_document_version_id),
    UNIQUE KEY uk_cdv_doc_version (company_document_id, version_no),
    -- 목록(§3)의 최신 완료 버전 집계·조인용 복합 인덱스 — company_document_id 로 좁힌 뒤 상태·삭제·차수로 필터.
    KEY idx_cdv_doc_completed (company_document_id, upload_status, deleted_at, version_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '사내 문서 버전(append-only) · COMPANY-DOC-V1';
