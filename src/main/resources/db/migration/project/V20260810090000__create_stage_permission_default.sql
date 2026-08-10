-- 스테이지별 「새 스텝 권한 기본값」 (STG-004 · 2026-08-09 개정)
-- ⚠️ 권한 판정에는 쓰지 않는다 (INV-01). 스텝이 생성될 때만 읽혀서 step_permission 행으로 복사된다.
--    그래서 스텝을 다른 스테이지로 옮겨도 권한이 따라 바뀌지 않는다.
CREATE TABLE stage_permission_default (
    stage_permission_default_id BIGINT NOT NULL AUTO_INCREMENT,
    stage_id BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    permission ENUM('VIEWER','EDITOR','NONE') NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (stage_permission_default_id),
    CONSTRAINT uk_spd_stage_user UNIQUE (stage_id, user_id),
    CONSTRAINT fk_spd_stage FOREIGN KEY (stage_id) REFERENCES stage (stage_id),
    CONSTRAINT fk_spd_user FOREIGN KEY (user_id) REFERENCES employee (user_id)
) COMMENT '스테이지 하위 새 스텝의 권한 기본값 (판정에 사용하지 않음)';
