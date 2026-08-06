ALTER TABLE activity_log
    MODIFY act ENUM('create','delete','modify','restore','purge') NOT NULL COMMENT '활동 유형';
