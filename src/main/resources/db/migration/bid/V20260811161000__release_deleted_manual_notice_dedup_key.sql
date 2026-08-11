-- 논리 삭제된 직접 등록 공고가 같은 공고의 재등록을 막지 않도록 중복 키를 해제합니다.
UPDATE bid_notice
SET manual_dedup_key = NULL
WHERE deleted_at IS NOT NULL
  AND owner_company_id IS NOT NULL
  AND manual_dedup_key IS NOT NULL;
