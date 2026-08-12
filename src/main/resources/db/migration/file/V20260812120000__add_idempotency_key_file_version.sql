-- 입찰 검토 파일 귀속 · 멱등키 (FILE-V1 §2-G · PROMOTE-007)
-- 무엇: file_version 에 귀속 멱등키(idempotency_key) 컬럼 + 단독 UNIQUE 를 추가한다.
-- 왜: 임시 파일 귀속(복사+INSERT+인덱싱)은 한 트랜잭션이 아니라 재시도 시 중복 file_version 이 생길 수 있다.
--     bidReviewDocumentId 를 멱등키로 저장해 재시도면 기존 버전을 재사용/반환한다(동시 경합 시 UNIQUE 가 최후 방어).
-- 스코프: idempotency_key 단독 UNIQUE. file_version 에 company_id 컬럼이 없고, MySQL UNIQUE 는 다중 NULL 을 허용하므로
--         일반 업로드(멱등키 NULL)는 충돌하지 않는다. bidReviewDocumentId 가 회사별로만 유일하면 그때 company 스코프로 확대.
-- 번호: 전 디렉터리 최대 V20260812110000(tenant/drop_business_category_unique) 위인 120000.
--       ⚠️ develop 이 마이그레이션을 재번호하는 전례가 있다(file/add_version_file 주석 참조) — 머지 직전 최신 번호 재확인 필수.
ALTER TABLE file_version
    ADD COLUMN idempotency_key VARCHAR(100) NULL COMMENT '귀속 멱등키(bid_review_document_id)',
    ADD UNIQUE KEY uk_file_version_idempotency (idempotency_key);
