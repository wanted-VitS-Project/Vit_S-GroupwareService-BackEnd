-- 원 기안자 감사 이력을 유지하면서 기존 반려 결재를 이어갈 대행 기안자를 별도 저장한다.
ALTER TABLE approval
    ADD COLUMN acting_drafter_id VARCHAR(20) NULL COMMENT '원 기안자 참여 불가 시 대행 기안자' AFTER user_id,
    ADD KEY idx_approval_acting_drafter (acting_drafter_id),
    ADD CONSTRAINT fk_approval_acting_drafter
        FOREIGN KEY (acting_drafter_id) REFERENCES employee (user_id);
