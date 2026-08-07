ALTER TABLE vitamate_analysis_document
    ADD COLUMN document_role VARCHAR(20) NOT NULL DEFAULT 'TARGET' AFTER file_version_id;

ALTER TABLE vitamate_analysis_document
    ADD CONSTRAINT chk_vitamate_analysis_document_role
        CHECK (document_role IN ('REFERENCE', 'TARGET'));
