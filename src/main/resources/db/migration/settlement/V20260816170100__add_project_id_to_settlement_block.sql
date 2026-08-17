-- =====================================================================
-- settlement_block 에 project_id 비정규화
-- =====================================================================
-- 무엇: project_id 컬럼을 추가하고 기존 행을 백필한 뒤 NOT NULL 로 고정한다.
-- 왜:   settlement_block 이 자기 소속 프로젝트를 모른다. 그래서 프로젝트 단위 집계를 하려면
--       그때마다 settlement_block -> block -> step 3단 조인으로 project_id 를 유추해야 했고,
--       SettlementDetailMapper 는 한 쿼리 안에서만 같은 체인을 3번 반복한다
--       (바깥 SELECT · agg 파생 테이블 · agg 범위를 좁히는 target 서브쿼리).
--
--       전임 테이블 block_payment_confirm 에는 이 컬럼이 있었다(V202608031739 init_schema).
--       2026-08-09 정산 재설계(V20260809130000)에서 테이블을 새로 만들며 빠졌다.
--
-- 선례: cash_flow · tax_invoice 도 같은 이유로 company_id 를 비정규화했다
--       (V20260811161200) — 조인으로 스코프를 유추하지 않고 행 자체가 들고 있게 한다.
--
-- ⚠️ 이 값은 불변이라 낡지 않는다. 블록이 다른 프로젝트로 넘어가는 경로가 둘인데 둘 다 이미 막혀 있다:
--    BlockCommandService#moveBlock (from/to 프로젝트 비교) ·
--    StepCommandService#resolveMoveBlockIds (cascade 이동도 같은 프로젝트만 허용).
--
-- ⚠️ 백필이 안 되는 고아 행(block 또는 step 이 없는 행)이 있으면 NOT NULL 고정에서 실패한다.
--    의도한 것이다 — 고아 행이 있다는 건 그 정산 블록의 프로젝트 집계가 이미 틀렸다는 뜻이라,
--    NULL 을 허용해 조용히 넘기면 진행률이 틀린 채로 서비스된다.

-- 1) NULL 허용으로 먼저 추가 (기존 행이 있는 상태에서 NOT NULL 을 바로 걸 수 없다)
ALTER TABLE settlement_block
    ADD COLUMN project_id BIGINT NULL
        COMMENT '소속 프로젝트 (block->step 경유 비정규화 · 블록 생성 시점 스탬핑 · 불변)'
        AFTER block_id;

-- 2) 기존 행 백필 — 삭제된 블록/스텝도 포함해서 채운다(deleted_at 조건 없음).
--    소프트 삭제된 정산 블록도 이력 조회 대상이라 project_id 가 있어야 한다.
UPDATE settlement_block sb
    JOIN block b ON b.block_id = sb.block_id
    JOIN step st ON st.step_id = b.step_id
SET sb.project_id = st.project_id
WHERE sb.project_id IS NULL;

-- 3) NOT NULL 고정 + 프로젝트 FK + 집계 전용 인덱스
--    인덱스 컬럼 순서: project_id(등치) -> type(등치) -> deleted_at(필터).
--    SUM(actual_amount) 집계가 이 인덱스로 범위를 잡고 나면 행 접근이 프로젝트 단위로 좁혀진다.
ALTER TABLE settlement_block
    MODIFY COLUMN project_id BIGINT NOT NULL
        COMMENT '소속 프로젝트 (block->step 경유 비정규화 · 블록 생성 시점 스탬핑 · 불변)',
    ADD CONSTRAINT fk_settlement_block_project
        FOREIGN KEY (project_id) REFERENCES project (project_id),
    ADD INDEX idx_settlement_block_project_type (project_id, type, deleted_at);
