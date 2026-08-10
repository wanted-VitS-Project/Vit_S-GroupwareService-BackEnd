-- ⚠️ FK 공백을 만들지 않기 위해 임시 FK를 먼저 추가한다.
-- DROP과 ADD를 한 ALTER TABLE 문으로 합치면 같은 이름의 FK 추가가 실패할 수 있으므로,
-- 임시 FK로 참조 무결성을 유지한 뒤 기존 이름의 FK를 교체한다.
ALTER TABLE issue
    ADD CONSTRAINT fk_issue_step_cascade_tmp
        FOREIGN KEY (step_id) REFERENCES step (step_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE;

ALTER TABLE issue
    DROP FOREIGN KEY FK_issue_step;

ALTER TABLE issue
    ADD CONSTRAINT FK_issue_step
        FOREIGN KEY (step_id) REFERENCES step (step_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE;

ALTER TABLE issue
    DROP FOREIGN KEY fk_issue_step_cascade_tmp;

ALTER TABLE issue_block
    ADD CONSTRAINT fk_ib_block_cascade_tmp
        FOREIGN KEY (block_id) REFERENCES block (block_id)
        ON DELETE CASCADE;

ALTER TABLE issue_block
    DROP FOREIGN KEY fk_ib_block;

ALTER TABLE issue_block
    ADD CONSTRAINT fk_ib_block
        FOREIGN KEY (block_id) REFERENCES block (block_id)
        ON DELETE CASCADE;

ALTER TABLE issue_block
    DROP FOREIGN KEY fk_ib_block_cascade_tmp;
