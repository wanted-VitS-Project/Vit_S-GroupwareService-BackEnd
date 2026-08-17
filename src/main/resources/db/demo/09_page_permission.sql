-- =====================================================================
-- 09. 화면 접근 권한 15
-- ---------------------------------------------------------------------
-- 무엇: page_permission — 어떤 탭을 열 수 있는지.
-- 왜:   안 주면 탭이 **잠긴다**(숨김이 아니다 · PERMISSION.md).
--       조은비에게 FINANCE 가 없으면 정산 화면 자체를 못 열어 06 데이터가 안 보인다.
--
-- 선행: 01_master.sql
-- ⚠️ 비밀번호가 없는 순수 권한 데이터라 이 파일은 커밋해도 된다.
--    로그인 계정(`account`)은 여전히 이 덤프 밖이다 — README 「계정 부트스트랩」 참고.
--
-- ⭐ 미참여 4명(VW109~112)에게도 MY_PROJECT 를 준다.
--    권한을 빼면 "권한이 없어서 못 봄"이 되는데, 보여주려는 건
--    **"권한이 있어도 참여자가 아니면 안 보인다"**(INV-07 · PRESENTATION.md §7-2)다.
--    권한을 빼면 시연이 다른 걸 증명하게 된다.
--
-- 되돌리기: DELETE FROM page_permission WHERE page_permission_id BETWEEN 9001 AND 9015;
-- =====================================================================

INSERT IGNORE INTO page_permission (page_permission_id, page_code, user_id, permission) VALUES
-- 참여자 8명
(9001, 'MY_PROJECT',     'vitawear-VW101', 'EDITOR'),
(9002, 'PROJECT_CREATE', 'vitawear-VW101', 'EDITOR'),   -- 주담당만 프로젝트 생성
(9003, 'MY_PROJECT',     'vitawear-VW102', 'EDITOR'),
(9004, 'MY_PROJECT',     'vitawear-VW103', 'EDITOR'),
(9005, 'MY_PROJECT',     'vitawear-VW104', 'EDITOR'),
(9006, 'MY_PROJECT',     'vitawear-VW105', 'EDITOR'),
(9007, 'MY_PROJECT',     'vitawear-VW106', 'VIEWER'),   -- 본부장 — 결재만 한다
(9008, 'MY_PROJECT',     'vitawear-VW107', 'VIEWER'),   -- 대표 — 〃
(9009, 'MY_PROJECT',     'vitawear-VW108', 'VIEWER'),
(9010, 'FINANCE',        'vitawear-VW108', 'EDITOR'),   -- ⭐ 조은비만 정산 확정 가능

-- ⭐ 미참여 4명 — 권한은 있는데 프로젝트가 목록에 안 뜬다 (INV-07 시연)
(9011, 'MY_PROJECT',     'vitawear-VW109', 'EDITOR'),
(9012, 'MY_PROJECT',     'vitawear-VW110', 'EDITOR'),
(9013, 'MY_PROJECT',     'vitawear-VW111', 'EDITOR'),
(9014, 'MY_PROJECT',     'vitawear-VW112', 'EDITOR'),
(9015, 'ADMIN_CONSOLE',  'vitawear-VW112', 'EDITOR');   -- 배수진 = 회사 2 관리자

-- ⛔ BIDDING 은 주지 않는다 — 공고 수집은 이 시나리오 밖이고(직접 생성 프로젝트),
--    로컬 bid_notice 가 0건이라 열어도 빈 화면이다.

-- 검증
--   SELECT p.page_code, p.permission, e.name FROM page_permission p
--   JOIN employee e ON e.user_id = p.user_id WHERE e.company_id = 2
--   ORDER BY p.page_code, e.name;
