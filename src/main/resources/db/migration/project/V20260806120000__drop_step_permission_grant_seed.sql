-- STP-010 그랜트(프로젝트 NONE + 스텝 허용) 폐기 (2026-08-06)
-- V20260805150000 의 시드 ① 은 폐기된 요구사항을 표현한 행이라 제거한다.
-- 차단 케이스(step_permission_id = 2) 는 유효하므로 남긴다.
DELETE FROM step_permission WHERE step_permission_id = 1;
