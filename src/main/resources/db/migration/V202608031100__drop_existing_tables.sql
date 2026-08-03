-- ============================================================
-- 기존 테이블 전체 삭제
-- 주의: 테이블과 내부 데이터가 모두 영구 삭제됨
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS activity_log;

DROP TABLE IF EXISTS template;

DROP TABLE IF EXISTS issue_block;
DROP TABLE IF EXISTS checklist_item;
DROP TABLE IF EXISTS issue;

DROP TABLE IF EXISTS block_payment_confirm;
DROP TABLE IF EXISTS performance;
DROP TABLE IF EXISTS tax_invoice;
DROP TABLE IF EXISTS payment;

DROP TABLE IF EXISTS approval_line;
DROP TABLE IF EXISTS block_approval;
DROP TABLE IF EXISTS block_ai;
DROP TABLE IF EXISTS block_image;
DROP TABLE IF EXISTS block_file_version;
DROP TABLE IF EXISTS attachment;
DROP TABLE IF EXISTS block_memo;
DROP TABLE IF EXISTS block_text;
DROP TABLE IF EXISTS block;

DROP TABLE IF EXISTS step_permission;
DROP TABLE IF EXISTS step;
DROP TABLE IF EXISTS stage;
DROP TABLE IF EXISTS project_department;
DROP TABLE IF EXISTS project_member;
DROP TABLE IF EXISTS project;

DROP TABLE IF EXISTS bid_notice_summary;
DROP TABLE IF EXISTS bid_notice;
DROP TABLE IF EXISTS crawl_link;

DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS business_category;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS department;

-- 테스트용 task 테이블
DROP TABLE IF EXISTS task;

SET FOREIGN_KEY_CHECKS = 1;