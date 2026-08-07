-- 알림 이동 대상을 발행 도메인이 직접 지정하도록 컬럼 추가 (NOTI-V1 GEN-005 · VIW-007)
--
-- 이전에는 notification.block_id -> block.type 을 거쳐 이동 대상을 판정했는데,
-- 이 방식은 "블록 자체가 그 도메인인" 결재에만 성립해서 이슈(블록과 다대다)·
-- 프로젝트(블록 무관)는 영구히 이동 불가였다. 이제 알림을 발행하는 도메인이
-- 대상을 직접 담아 보내고, 알림 도메인은 해석 없이 그대로 저장·반환한다.
--
-- target_id 에 FK 를 걸지 않는 이유: target_type 값에 따라 가리키는 테이블이 달라지는
-- 다형성 참조라 DB 로 강제할 수 없다. block.type + block.type_id 와 동일한 패턴이다.

ALTER TABLE notification
    ADD COLUMN target_type    VARCHAR(50) NULL
        COMMENT '이동 대상 도메인 유형 (APPROVAL/PROJECT/ISSUE 등 · 열린 문자열, ENUM 금지 — INV-03)'
        AFTER block_id,
    ADD COLUMN target_id      BIGINT      NULL
        COMMENT '이동 대상 구분 번호 (target_type 이 가리키는 도메인의 PK · 다형성이라 FK 없음 — INV-04)'
        AFTER target_type,
    ADD COLUMN target_context JSON        NULL
        COMMENT '이동에 필요한 부가 식별값 스냅샷 (예: {"revisionId": 56}) · 표시용 데이터는 넣지 않는다'
        AFTER target_id;

-- 부분 입력(타입만 있고 대상이 없는 등) 차단. MySQL 8.0.16+ 부터 실제로 강제된다.
ALTER TABLE notification
    ADD CONSTRAINT ck_notification_target
        CHECK (
            (target_type IS NULL AND target_id IS NULL)
            OR (target_type IS NOT NULL AND target_id IS NOT NULL)
        );
