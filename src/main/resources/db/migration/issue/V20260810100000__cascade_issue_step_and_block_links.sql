ALTER TABLE issue
    DROP FOREIGN KEY FK_issue_step;

ALTER TABLE issue
    ADD CONSTRAINT FK_issue_step
        FOREIGN KEY (step_id) REFERENCES step (step_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE;

ALTER TABLE issue_block
    DROP FOREIGN KEY fk_ib_block;

ALTER TABLE issue_block
    ADD CONSTRAINT fk_ib_block
        FOREIGN KEY (block_id) REFERENCES block (block_id)
        ON DELETE CASCADE;
