-- =====================================================================
-- 멀티테넌트 Phase 1 · Activity Log company_id DEFAULT 제거
-- =====================================================================
-- 무엇: activity_log.company_id 의 임시 DEFAULT 1 을 제거한다.
-- 왜: company_id 를 생략하는 구버전/배치/직접 SQL 쓰기는 회사 1로 오귀속되지 않고 실패해야 한다.

ALTER TABLE activity_log
    ALTER COLUMN company_id DROP DEFAULT;
