-- 멀티 동시수정 방어 · 낙관적 락 (CONCURRENCY.md §2-1 · file)
-- 무엇: file 테이블에 낙관적 락 version 컬럼을 추가한다 (문서명 수정 PATCH /files/{fileId} 대상).
-- 왜: 두 사용자가 같은 문서명을 동시에 고치면 뒤엣것이 앞엣것을 조용히 덮어쓴다(lost-update).
--     조회 때 내려준 version 을 저장 조건(WHERE version = ?)에 걸어 충돌을 409 로 잡는다.
-- 번호 대역: CONCURRENCY.md §7 은 issue·file 을 150000 에 함께 배정했으나, add_version_issue(issue-only)로 소진됐다.
--            처음엔 160000 을 썼으나 develop 이 마이그레이션을 1608xx 대역으로 재배치하면서 issue/cascade... 가
--            160000 을 가져가 충돌했다. develop 최대(160800) 위인 160900 으로 재배치한다.
-- 기존 행: DEFAULT 1 로 전부 1 부터 시작해야 프론트가 조회에서 받은 값과 맞물린다.
ALTER TABLE file
    ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT '낙관적 락 버전';
