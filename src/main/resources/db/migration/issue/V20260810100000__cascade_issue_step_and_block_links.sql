-- ⚠️ DROP과 ADD를 한 ALTER TABLE 문으로 합치지 않는다 — 같은 이름의 FK를 한 문장에서
-- DROP+ADD 하면 MySQL/InnoDB가 ADD의 이름 중복 검사를 DROP 반영 전에 수행해
-- "Error 1826: Duplicate foreign key constraint name"으로 실패한다(실제 재현됨).
-- FK 이름을 그대로 유지해야 하므로(추적성, CLEANUP.md §3-4) 두 문장으로 분리한다.
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
