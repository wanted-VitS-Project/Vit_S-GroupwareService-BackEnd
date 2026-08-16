-- =====================================================================
-- 결재관리 목록조회(MGT-001~004) 인덱스
-- =====================================================================
-- 무엇: approval 에 정렬용 복합 인덱스, approval_line 에 pending 스코프 EXISTS 용 인덱스를 추가한다.
-- 왜:   ApprovalListMapper.findApprovals 는 approval 을 드라이빙 테이블로 잡고
--         WHERE a.deleted_at IS NULL ... ORDER BY a.created_at DESC, a.approval_id DESC LIMIT ?
--       로 끝나는데, 이 조합을 받쳐줄 인덱스가 approval 에 하나도 없다. 지금은 approval 을
--       전부 훑고 employee/block/step/project 4단 조인을 마친 뒤에야 정렬하고 자른다.
--       인덱스가 있으면 정렬된 순서로 걸어가며 LIMIT 개수만 채우고 멈춘다.

ALTER TABLE approval
    ADD KEY idx_approval_active_created (deleted_at, created_at, approval_id);

-- scope=pending(MGT-001 "내가 처리할 결재")의 EXISTS 서브쿼리 전용.
--   EXISTS (SELECT 1 FROM approval_line l
--            WHERE l.approval_revision_id = cr.approval_revision_id
--              AND l.user_id = ? AND l.status = 'ACTIVE' AND l.deleted_at IS NULL)
-- 기존 인덱스로는 이걸 못 받는다:
--   · idx_approval_line_revision_seq (approval_revision_id, sequence_no) — user_id 가 없다
--   · fk_line_user (user_id)                                            — 그 사람의 전 결재선을 다 꺼낸다
-- user_id 를 선두로 두어 결재자 본인 기준으로 먼저 좁힌 뒤, status 로 ACTIVE 만 남기고,
-- 꼬리의 approval_revision_id 로 조인 상대와 맞춘다(= 행을 꺼내지 않고 인덱스에서 판정 종료).
--
-- ⚠️ deleted_at 을 status 뒤에 둔 이유 — 결재선은 치환·제외 때 soft delete 되므로
--    (V20260811161100 참고) 삭제분이 실제로 쌓인다. 인덱스에 없으면 ACTIVE 후보를 찾을 때마다
--    행을 꺼내 삭제 여부를 확인해야 한다.
--
-- 🔖 남는 정리 대상: 이 인덱스의 선두가 user_id 라서 FK 자동 인덱스(fk_line_user)가 중복이 된다.
--    notification 쪽과 같은 이유로 이번엔 걷지 않는다 — 실제 인덱스명을 SHOW CREATE TABLE 로
--    확인하지 않고 DROP 하면 마이그레이션이 통째로 멈추는데, 얻는 것은 작은 인덱스 하나의
--    쓰기 비용뿐이다. 두 테이블을 묶어 별도 마이그레이션으로 정리한다.
ALTER TABLE approval_line
    ADD KEY idx_approval_line_user_status (user_id, status, deleted_at, approval_revision_id);
