ALTER TABLE vitamate_analysis
    ADD COLUMN review_type VARCHAR(50) NULL AFTER prompt,
    ADD COLUMN review_category_codes VARCHAR(500) NULL AFTER review_type,
    ADD COLUMN additional_instruction TEXT NULL AFTER review_category_codes,
    ADD COLUMN prompt_template_version VARCHAR(50) NULL AFTER additional_instruction;
