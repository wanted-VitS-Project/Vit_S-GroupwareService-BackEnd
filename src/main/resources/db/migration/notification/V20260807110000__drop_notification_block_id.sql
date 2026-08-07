-- notification.block_id 제거 (NOTI-V1 INV-04 · VIW-007)
--
-- block_id 는 원래 "이동 대상을 판정하기 위한" 컬럼이었다(block_id -> block.type -> 어댑터).
-- V20260807100000 에서 target_type/target_id 가 그 역할을 완전히 가져가면서 이 컬럼을 읽는
-- 코드가 한 곳도 남지 않았다.
--
-- 게다가 block_id 는 특정 도메인(블록)에만 묶인 FK 라, 남겨두면 "알림에 도메인 FK 를 두지
-- 않는다"는 INV-04 취지에 오히려 어긋난다. 블록과 무관한 알림(프로젝트 초대 등)이 늘어날수록
-- 항상 NULL 인 사용처 없는 컬럼이 된다.
--
-- ⚠️ 목록 조회 응답의 blockId 필드도 함께 사라진다(프론트 계약 변경).
ALTER TABLE notification
    DROP FOREIGN KEY fk_notification_block;

ALTER TABLE notification
    DROP COLUMN block_id;
