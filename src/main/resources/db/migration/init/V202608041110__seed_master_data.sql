-- =====================================================================
-- 마스터/기본 데이터 시드 — 부서(department) · 직급(job_position)
-- ---------------------------------------------------------------------
-- 계정(account)·사원(employee) 더미는 비밀번호 해시를 포함하므로 여기 넣지 않는다.
--   → FLYWAY.md §3·§4(더미 비밀번호 금지) · §5(개발용 더미는 Flyway 밖 수동 관리).
--     사원·계정 더미는 로컬 수동 seed 스크립트(.ai/local)로만 관리한다.
-- 재적용 안전: 부서·직급 모두 UNIQUE(name) 이 있어 INSERT IGNORE 로 중복 시 건너뛴다 (§3).
-- =====================================================================

-- 부서 — 2단 트리 (departmentPath "본사 / 개발팀" 이 나오도록 parent_id 로 연결)
INSERT IGNORE INTO department (department_id, name, parent_id) VALUES
  (1, '본사',   NULL),
  (2, '개발팀', 1),
  (3, '인사팀', 1),
  (4, '회계팀', 1);

-- 직급 — sort_order 로 서열 표시
INSERT IGNORE INTO job_position (job_position_id, name, sort_order) VALUES
  (1, '사원', 1),
  (2, '팀장', 2),
  (3, '부장', 3),
  (4, '대표', 4);
