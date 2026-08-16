-- =====================================================================
-- KDT 10. 화면 접근 권한 4
-- ---------------------------------------------------------------------
-- 무엇: page_permission — 부여 대상 화면을 누가 열 수 있는지.
-- 왜:   안 주면 탭이 **잠긴다**(숨김이 아니다).
--       BIDDING 이 없으면 02 에서 넣은 공고 7건이 화면에 아예 안 뜬다.
--
-- 선행: 01_master.sql
-- ⚠️ 비밀번호가 없는 순수 권한 데이터라 이 파일은 커밋해도 된다.
--    로그인 계정(`account`)은 여전히 이 덤프 밖이다 — README 「계정 만들기」 참고.
--
-- 🚨🚨 여기가 의류 데모(company 2)와 결정적으로 다르다
--
--    `page_permission` 행이 생기는 코드는 **`BIDDING`·`FINANCE` 둘뿐이다.**
--    PageCode 카탈로그에서 이 둘만 Category.GRANTABLE 이고, 나머지 9개는 role 로 열린다.
--
--      COMMON       HOME · NOTIFICATION · APPROVAL · SETTINGS   전원 열림
--      PROJECT      PROJECT_CREATE · MY_PROJECT                 ADMIN 만 제외
--      GRANTABLE    BIDDING · FINANCE                           ← 행이 생기는 유일한 둘
--      MASTER_GATED COMPANY_STATUS                              ADMIN·MASTER 만
--      ADMIN_ONLY   TEMPLATE · ADMIN_CONSOLE                    ADMIN 만
--
--    ⛔ `MY_PROJECT`·`PROJECT_CREATE`·`ADMIN_CONSOLE` 행을 만들지 마라.
--       INSERT 는 성공하지만 화면 판정에 아무 영향이 없다. 만들어놓고 「권한을 줬는데 왜 안 되지」로
--       한참 헤매게 된다. 의류 데모의 09_page_permission.sql 을 복사하면 정확히 이 함정에 빠진다.
--
-- ⚠️ ADMIN·MASTER 는 role 로 통과한다 (BiddingPageAccessAdapter).
--    VE111(MASTER)·VE112·VE113(ADMIN)에게는 행이 필요 없다.
--
-- 되돌리기: DELETE FROM page_permission WHERE page_permission_id BETWEEN 8001 AND 8004;
-- =====================================================================

INSERT IGNORE INTO page_permission (page_permission_id, page_code, user_id, permission) VALUES
-- ⭐ 공고 조회·입찰 관리 — 이게 없으면 02 의 공고 7건이 화면에 안 뜬다
(8001, 'BIDDING', 'vitaedu-VE101', 'EDITOR'),   -- 강태현 · 심사신청 주담당. 공고 직접 등록과 검토 요청
(8002, 'BIDDING', 'vitaedu-VE103', 'EDITOR'),   -- 남기훈 · 팀장. AI 요약 확정과 공고 제외 판단
(8003, 'BIDDING', 'vitaedu-VE102', 'VIEWER'),   -- 윤하람 · 조회만. 등록·제외는 못 한다

-- ⭐ 재무 관리 — 정산 현황과 입출금 매칭
(8004, 'FINANCE', 'vitaedu-VE109', 'EDITOR');   -- 하성민 · 재무. 입출금 연결 확정은 이 사람만


-- =====================================================================
-- 검증
-- =====================================================================
-- 1) 회사 3 의 권한 부여 현황
--    SELECT p.page_code, p.permission, e.name, e.user_id
--    FROM page_permission p JOIN employee e ON e.user_id = p.user_id
--    WHERE e.company_id = 3 ORDER BY p.page_code, e.user_id;
--    기대: BIDDING 3행 · FINANCE 1행
--
-- 2) ⛔ 부여 대상이 아닌 코드로 행이 생겼나 (0행이어야 정상)
--    SELECT p.page_permission_id, p.page_code FROM page_permission p
--    JOIN employee e ON e.user_id = p.user_id
--    WHERE e.company_id = 3 AND p.page_code NOT IN ('BIDDING', 'FINANCE');
--
-- 3) 화면에서 실제로 열리는지는 API 로 확인한다
--    GET /api/v1/bidding/notices            ← VE104 로 로그인하면 막혀야 정상이다
--    GET /api/v1/finance/settlements
--
-- ⭐ 시연 포인트 — 권한이 있어도 참여자가 아니면 안 보인다
--    VE104(배규리)는 BIDDING 이 없어 공고 화면 자체가 막힌다. 이건 「권한이 없어서」다.
--    그와 별개로 P8008(LMS 고도화)은 참여자가 3명뿐이라, 권한이 멀쩡한 사람도
--    참여자가 아니면 목록에 안 뜬다. 두 가지가 다른 이유라는 걸 나눠서 보여줘라.
