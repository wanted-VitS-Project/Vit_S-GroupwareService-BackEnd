-- approval_line 결재선 치환을 하드 DELETE 에서 논리 삭제로 바꾸기 위해 UNIQUE 를 일반 인덱스로 낮춘다.
-- 근거: DELETE.md D-1(실물은 전부 soft delete) · D-2(하드는 연결 행 7종뿐, approval_line 은 미포함)
--       D-7(soft delete 테이블에 UNIQUE 를 두지 않는다) · TRAP 1 해법 ③(UNIQUE 를 걷고 앱이 활성 행만 검사)
--
-- soft delete 로 바꾸면 치환된 이전 결재선이 행으로 남는데, uk_approval_line 이 살아 있으면
-- 같은 (approval_revision_id, sequence_no) 를 다시 넣을 수 없어 결재선 재등록이 1062 로 실패한다.
-- sequence_no 는 NOT NULL 이라 "삭제 시 NULL 로 비운다"는 D-7 의 대안도 쓸 수 없다.
--
-- 활성 행의 순번 중복은 애플리케이션이 막는다:
--   1) ApprovalLineEligibilityPolicy.assertOrderValid 가 요청 순번의 중복·순서를 검증한다.
--   2) ApprovalCommandService.updateLines 가 회차 행을 PESSIMISTIC_WRITE 로 잠근 뒤 치환하므로
--      같은 회차에 대한 동시 치환이 직렬화된다.
--
-- ⚠️ 순서를 지킨다 — 인덱스를 먼저 추가하고 UNIQUE 를 나중에 지운다.
--    uk_approval_line 의 선두 컬럼이 approval_revision_id 라서 fk_line_revision 이 이 인덱스에
--    의존한다. 먼저 DROP 하면 errno 150 (Cannot drop index needed in a foreign key constraint) 이 난다.
-- 실패 복구: SHOW CREATE TABLE approval_line 으로 인덱스·FK 상태를 확인한다.

ALTER TABLE approval_line
    ADD KEY idx_approval_line_revision_seq (approval_revision_id, sequence_no);

ALTER TABLE approval_line
    DROP INDEX uk_approval_line;
