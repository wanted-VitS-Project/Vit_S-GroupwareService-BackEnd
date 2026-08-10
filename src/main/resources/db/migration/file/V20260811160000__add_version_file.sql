-- 멀티 동시수정 방어 · 낙관적 락 (CONCURRENCY.md §2-1 · file)
-- 무엇: file 테이블에 낙관적 락 version 컬럼을 추가한다 (문서명 수정 PATCH /files/{fileId} 대상).
-- 왜: 두 사용자가 같은 문서명을 동시에 고치면 뒤엣것이 앞엣것을 조용히 덮어쓴다(lost-update).
--     조회 때 내려준 version 을 저장 조건(WHERE version = ?)에 걸어 충돌을 409 로 잡는다.
-- 번호 대역: CONCURRENCY.md §7 은 issue·file 을 150000 에 함께 배정했으나, 이미 add_version_issue(issue-only)
--            로 커밋돼(b0f4011) 150000 이 소진됐다. 커밋된 마이그레이션은 체크섬 때문에 수정 불가라 file 은 다음 번호로 분리한다.
-- 기존 행: DEFAULT 1 로 전부 1 부터 시작해야 프론트가 조회에서 받은 값과 맞물린다.
ALTER TABLE file
    ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT '낙관적 락 버전';
