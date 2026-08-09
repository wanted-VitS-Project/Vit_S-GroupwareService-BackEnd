-- PRJ-010 `NONE` 폐기 (2026-08-06)
-- 프로젝트 참여자 권한은 VIEWER · EDITOR 2값만 남는다. 차단은 참여자 제거(DELETE)로 표현한다.
-- `step_permission` 의 NONE(스텝 차단 오버라이드)은 STP-011 이 요구하므로 값 자체는 유지한다.

-- 제거될 참여자의 스텝 오버라이드를 먼저 지운다 (참여자 제거 API 와 같은 순서).
DELETE sp
  FROM step_permission sp
  JOIN step s
    ON s.step_id = sp.step_id
  JOIN project_member pm
    ON pm.project_id = s.project_id
   AND pm.user_id = sp.user_id
 WHERE pm.permission = 'NONE';

DELETE FROM project_member WHERE permission = 'NONE';
