-- 기존 수집 조건은 생성자의 현재 회사로 귀속하고 임시 DEFAULT 1을 제거한다.
UPDATE crawl_condition cc
JOIN employee e ON e.user_id = cc.created_by
SET cc.company_id = e.company_id
WHERE cc.created_by IS NOT NULL;

ALTER TABLE crawl_condition
    ALTER COLUMN company_id DROP DEFAULT;
