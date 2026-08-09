-- =====================================================================
-- 멀티테넌트 · 사번 접두사 — 회사코드를 user_id 앞에 부착
-- =====================================================================
-- 무엇: 모든 사번(employee.user_id)과 이를 참조하는 33개 FK 칼럼 값 앞에 회사코드 접두사를 붙인다.
--       회사 1의 코드를 'DEFAULT' → 'vitas' 로 확정하고, 값은 'vitas-{기존사번}' 이 된다.
-- 왜:   회사마다 같은 base 사번(예: 1234567)을 쓸 수 있게, user_id 를 전역 유일로 만든다.
--       PK·FK 는 단일 칼럼 그대로 유지(복합 안 됨). 회사 판별은 접두사가 아니라 company_id 칼럼이 담당한다.
--
-- ⚠️ 일회성·단일회사 전제 마이그레이션이다. 지금 모든 데이터가 회사 1(vitas) 소속이라
--    전 사번에 동일 접두사 'vitas-' 를 붙이는 게 정확하다. 회사가 2개 이상인 상태에서는 절대 재실행 금지.
-- ⚠️ FOREIGN_KEY_CHECKS=0 사용 — FLYWAY.md §4 는 이를 기본 금지하나, "부모+자식을 동일 접두사로 한 번에
--    리네임" 하는 일회성 벌크 작업이라 예외로 허용한다. 부모·자식을 모두 같은 규칙으로 갱신하므로
--    종료 시 참조 무결성이 보존된다(체크 재활성 후에도 orphan 없음).
-- 참고: activity_log.before_value/after_value 같은 감사 자유텍스트에 박힌 옛 사번은 이력 스냅샷이라 건드리지 않는다.

-- 1) 회사 1 코드 확정 (DEFAULT 폐기)
UPDATE company SET company_code = 'vitas' WHERE company_id = 1 AND company_code = 'DEFAULT';

-- 2) 사번 접두사 일괄 부착 (부모 + 33개 자식 FK 칼럼)
SET FOREIGN_KEY_CHECKS = 0;

-- 부모
UPDATE employee                   SET user_id      = CONCAT('vitas-', user_id)      WHERE user_id IS NOT NULL;

-- 인사 · 계정
UPDATE account                    SET user_id      = CONCAT('vitas-', user_id)      WHERE user_id IS NOT NULL;
UPDATE page_permission            SET user_id      = CONCAT('vitas-', user_id)      WHERE user_id IS NOT NULL;
UPDATE employee_group             SET created_by   = CONCAT('vitas-', created_by)   WHERE created_by IS NOT NULL;
UPDATE employee_group_member      SET user_id      = CONCAT('vitas-', user_id)      WHERE user_id IS NOT NULL;
UPDATE activity_log               SET user_id      = CONCAT('vitas-', user_id)      WHERE user_id IS NOT NULL;
UPDATE notification               SET user_id      = CONCAT('vitas-', user_id)      WHERE user_id IS NOT NULL;

-- 프로젝트 계열
UPDATE project                    SET created_by   = CONCAT('vitas-', created_by)   WHERE created_by IS NOT NULL;
UPDATE project_member             SET user_id      = CONCAT('vitas-', user_id)      WHERE user_id IS NOT NULL;
UPDATE step                       SET owner_user_id = CONCAT('vitas-', owner_user_id) WHERE owner_user_id IS NOT NULL;
UPDATE step                       SET completed_by = CONCAT('vitas-', completed_by) WHERE completed_by IS NOT NULL;
UPDATE step_permission            SET user_id      = CONCAT('vitas-', user_id)      WHERE user_id IS NOT NULL;
UPDATE block                      SET created_by   = CONCAT('vitas-', created_by)   WHERE created_by IS NOT NULL;
UPDATE block                      SET owner        = CONCAT('vitas-', owner)        WHERE owner IS NOT NULL;
UPDATE issue                      SET created_by   = CONCAT('vitas-', created_by)   WHERE created_by IS NOT NULL;
UPDATE issue_assign               SET user_id      = CONCAT('vitas-', user_id)      WHERE user_id IS NOT NULL;

-- 파일
UPDATE file                       SET created_by   = CONCAT('vitas-', created_by)   WHERE created_by IS NOT NULL;
UPDATE file_version               SET uploaded_by  = CONCAT('vitas-', uploaded_by)  WHERE uploaded_by IS NOT NULL;
UPDATE block_file                 SET linked_by    = CONCAT('vitas-', linked_by)    WHERE linked_by IS NOT NULL;

-- 결재
UPDATE approval                   SET user_id      = CONCAT('vitas-', user_id)      WHERE user_id IS NOT NULL;
UPDATE approval_line              SET user_id      = CONCAT('vitas-', user_id)      WHERE user_id IS NOT NULL;

-- 재무
UPDATE payment                    SET matched_by   = CONCAT('vitas-', matched_by)   WHERE matched_by IS NOT NULL;
UPDATE payment                    SET confirmed_by = CONCAT('vitas-', confirmed_by) WHERE confirmed_by IS NOT NULL;
UPDATE payment                    SET linked_by    = CONCAT('vitas-', linked_by)    WHERE linked_by IS NOT NULL;
UPDATE tax_invoice                SET matched_by   = CONCAT('vitas-', matched_by)   WHERE matched_by IS NOT NULL;
UPDATE tax_invoice_confirm        SET linked_by    = CONCAT('vitas-', linked_by)    WHERE linked_by IS NOT NULL;

-- 공고 · 크롤링
UPDATE bid_notice                 SET created_by   = CONCAT('vitas-', created_by)   WHERE created_by IS NOT NULL;
UPDATE bid_notice_summary         SET requested_by = CONCAT('vitas-', requested_by) WHERE requested_by IS NOT NULL;
UPDATE bid_notice_summary         SET confirmed_by = CONCAT('vitas-', confirmed_by) WHERE confirmed_by IS NOT NULL;
UPDATE bid_notice_participant     SET created_by   = CONCAT('vitas-', created_by)   WHERE created_by IS NOT NULL;
UPDATE bid_notice_status_history  SET changed_by   = CONCAT('vitas-', changed_by)   WHERE changed_by IS NOT NULL;
UPDATE crawl_condition            SET created_by   = CONCAT('vitas-', created_by)   WHERE created_by IS NOT NULL;
UPDATE crawl_run                  SET requested_by = CONCAT('vitas-', requested_by) WHERE requested_by IS NOT NULL;

-- AI
UPDATE vitamate_analysis          SET requested_by = CONCAT('vitas-', requested_by) WHERE requested_by IS NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;
