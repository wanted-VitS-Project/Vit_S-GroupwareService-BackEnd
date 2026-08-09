CREATE TABLE vitamate_review_type (
    review_type VARCHAR(50) NOT NULL,
    review_type_name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (review_type),
    KEY idx_vitamate_review_type_enabled_sort (enabled, sort_order)
) COMMENT '비타메이트 검토 유형 마스터';

CREATE TABLE vitamate_review_template (
    vitamate_review_template_id BIGINT NOT NULL AUTO_INCREMENT,
    review_type VARCHAR(50) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    guide_text TEXT NOT NULL,
    example_text TEXT NULL,
    prompt_template TEXT NOT NULL,
    template_version VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (vitamate_review_template_id),
    CONSTRAINT uk_vitamate_review_template_type_category UNIQUE (review_type, category_code),
    CONSTRAINT fk_vitamate_review_template_type
        FOREIGN KEY (review_type) REFERENCES vitamate_review_type (review_type),
    KEY idx_vitamate_review_template_enabled_sort (review_type, enabled, sort_order)
) COMMENT '비타메이트 검토 카테고리별 템플릿 마스터';

CREATE TABLE vitamate_analysis_template (
    vitamate_analysis_template_id BIGINT NOT NULL AUTO_INCREMENT,
    vitamate_analysis_id BIGINT NOT NULL,
    review_type VARCHAR(50) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    prompt_template TEXT NOT NULL,
    template_version VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    PRIMARY KEY (vitamate_analysis_template_id),
    CONSTRAINT uk_vitamate_analysis_template_analysis_category UNIQUE (vitamate_analysis_id, category_code),
    CONSTRAINT fk_vitamate_analysis_template_analysis
        FOREIGN KEY (vitamate_analysis_id) REFERENCES vitamate_analysis (vitamate_analysis_id),
    KEY idx_vitamate_analysis_template_analysis_sort (vitamate_analysis_id, sort_order)
) COMMENT '분석 요청 당시 사용한 비타메이트 템플릿 스냅샷';
