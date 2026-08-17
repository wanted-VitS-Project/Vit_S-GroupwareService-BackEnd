-- =====================================================================
-- 23. ADMIN 을 프로젝트·결재에서 분리 — 명세 준수
-- ---------------------------------------------------------------------
-- 근거 (.ai/api/approval.md · page-permission.md)
--   · "ADMIN 은 인사 전용이므로 결재자 지정·결재 조회·scope=all 권한이 없다"
--   · "ADMIN 은 모든 범위에서 결재 권한이 없다"
--   · "내 프로젝트·프로젝트 생성은 ADMIN 제외 확정 — project_member 사람 등록 기준"
--
-- 16~22 에서 만든 데이터가 이 명세를 4가지로 어기고 있었다.
--   ① 서영광(ADMIN)이 결재선에 16건    → ADMIN 은 결재자가 될 수 없다
--   ② ADMIN 6명이 결재 24건 기안        → 본인은 403 이라 영원히 못 본다
--   ③ ADMIN 에게 MY_PROJECT 권한 6건    → 명세상 제외 대상
--   ④ ADMIN 이 project_member 21건      → 같은 이유
--
-- ⭐ 조직 논리도 같이 맞춘다. 최종 결재자는 대표여야 하는데 대표(서영광)가 ADMIN 이라
--    결재선에 못 든다. → 대표 자리를 MASTER(배수진)로 옮기고 서영광은 인사 담당으로 내린다.
--
-- 되돌리기 없음(기안자 재배정은 원본을 덮는다). 필요하면 실행 전에 백업:
--   mysqldump ... vitaS approval approval_line project_member page_permission employee > before23.sql
-- =====================================================================

-- ── ① 대표 자리 이동 — MASTER 가 최종 결재자가 된다 ────────────────
UPDATE employee SET job_position_id=9012 WHERE user_id='vitawear-VW112';  -- 배수진 과장 → 대표
UPDATE employee SET job_position_id=9009 WHERE user_id='vitawear-VW107';  -- 서영광 대표 → 과장 (인사)

-- 결재선의 ADMIN 을 걷어낸다.
-- ⚠️ 순서 중요. 9047 은 결재선이 이미 [112, 107] 이라 107을 112로 바꾸면 같은 사람이 두 번 들어간다.
--    이 건만 먼저 106(한지훈)으로 빼고, 나머지를 일괄 치환한다.
UPDATE approval_line SET user_id='vitawear-VW106'
 WHERE approval_revision_id=9048 AND user_id='vitawear-VW107';
UPDATE approval_line SET user_id='vitawear-VW112'
 WHERE user_id='vitawear-VW107';

-- ── ② 기안자 24건 재배정 → MEMBER 6 + MASTER 1 ────────────────────
-- 배정 원칙: 프로젝트 성격에 맞는 담당자에게 준다.
--   재고·물류·검품 → 최동석(물류·CS)  ·  가격·쿠폰·기획전 → 박준호(브랜드)
--   정산·수익성 → 김서연(주담당)      ·  디자인·라인업 → 정민아(디자인)
--   계약·수주 → 배수진(대표)          ·  예산·수주계약 → 한지훈(본부장)
-- ⚠️ 기안자는 그 결재의 결재선에 있으면 안 된다. 9027·9030 은 한지훈이 기안자가 되므로
--    결재선의 한지훈 자리를 배수진으로 밀어낸다 (바로 아래에서 처리).

-- p9001 무신사 26 S/S
UPDATE approval SET user_id='vitawear-VW102' WHERE approval_id=9042;  -- 4월 추가 쿠폰 예산
UPDATE approval SET user_id='vitawear-VW101' WHERE approval_id=9044;  -- 3차 정산 예상액
UPDATE approval SET user_id='vitawear-VW102' WHERE approval_id=9045;  -- 반품 차감 이의 재제기
UPDATE approval SET user_id='vitawear-VW105' WHERE approval_id=9046;  -- 3차 발주 대금 집행
UPDATE approval SET user_id='vitawear-VW104' WHERE approval_id=9047;  -- 공장 단가 인상 수용
UPDATE approval SET user_id='vitawear-VW112' WHERE approval_id=9048;  -- 사이즈 검수 잔금
-- p9002 25 F/W
UPDATE approval SET user_id='vitawear-VW101' WHERE approval_id=9011;  -- 2차 발주
UPDATE approval SET user_id='vitawear-VW104' WHERE approval_id=9012;  -- 10월 쿠폰 예산
UPDATE approval SET user_id='vitawear-VW105' WHERE approval_id=9013;  -- 시즌오프 할인율
UPDATE approval SET user_id='vitawear-VW102' WHERE approval_id=9014;  -- 잔여 재고 처분
UPDATE approval SET user_id='vitawear-VW105' WHERE approval_id=9015;  -- 정산 확정
UPDATE approval SET user_id='vitawear-VW102' WHERE approval_id=9017;  -- 시즌 결산 보고
-- 그 외 프로젝트
UPDATE approval SET user_id='vitawear-VW101' WHERE approval_id=9020;  -- W컨셉 수수료 협의안
UPDATE approval SET user_id='vitawear-VW104' WHERE approval_id=9024;  -- 자사몰 업체 선정
UPDATE approval SET user_id='vitawear-VW101' WHERE approval_id=9025;  -- 자사몰 오픈 일정
UPDATE approval SET user_id='vitawear-VW106' WHERE approval_id=9027;  -- 26 F/W 예산 편성
UPDATE approval SET user_id='vitawear-VW104' WHERE approval_id=9028;  -- 26 F/W 라인업
UPDATE approval SET user_id='vitawear-VW105' WHERE approval_id=9029;  -- 26 F/W 생산 계획
UPDATE approval SET user_id='vitawear-VW106' WHERE approval_id=9030;  -- 라온 OEM 계약
UPDATE approval SET user_id='vitawear-VW112' WHERE approval_id=9033;  -- 한성 OEM 계약
UPDATE approval SET user_id='vitawear-VW105' WHERE approval_id=9036;  -- 물류창고 후보지
UPDATE approval SET user_id='vitawear-VW112' WHERE approval_id=9037;  -- 임대차 계약
UPDATE approval SET user_id='vitawear-VW102' WHERE approval_id=9039;  -- 재고 실사 결과
UPDATE approval SET user_id='vitawear-VW112' WHERE approval_id=9041;  -- 처분 손실 확정

-- 기안자가 자기 결재선에 들어간 2건을 푼다 (9027·9030 → 한지훈이 기안자가 됐다).
UPDATE approval_line SET user_id='vitawear-VW112'
 WHERE approval_revision_id IN (9028, 9031) AND user_id='vitawear-VW106';

-- ── ③ 화면 권한에서 ADMIN 제외 ────────────────────────────────────
DELETE p FROM page_permission p JOIN account a ON a.user_id=p.user_id
 WHERE a.role='ADMIN' AND p.page_code IN ('MY_PROJECT','PROJECT_CREATE');

-- ── ④ 프로젝트 멤버에서 ADMIN 제외 ────────────────────────────────
DELETE m FROM project_member m JOIN account a ON a.user_id=m.user_id
 WHERE a.role='ADMIN';

-- =====================================================================
-- ⑤ 일괄 치환 부작용 정리
-- ---------------------------------------------------------------------
-- 18_p9002.sql 은 기안자가 기본 결재선(103·106·107)에 들어가면 그 자리를 112로 밀어냈다.
-- 그 결재선에 107 도 함께 있었으므로, 위에서 107→112 를 일괄 적용하자 112 가 두 번 들어갔다.
--   rev9008 [112,106,112] · rev9011 [103,112,112]
-- 그리고 기안자가 배수진인 9016 은 최종 결재자(107→112)가 기안자 본인이 돼버렸다.
-- ⚠️ 일괄 UPDATE 로 사람을 바꿀 때는 **치환 대상이 이미 그 결재선에 있는지**를 먼저 봐야 한다.
-- =====================================================================
UPDATE approval_line SET user_id='vitawear-VW101' WHERE approval_line_id=9019;  -- rev9008 1차
UPDATE approval_line SET user_id='vitawear-VW101' WHERE approval_line_id=9029;  -- rev9011 2차

-- 대표가 기안한 건이라 최종 결재 단계를 없애고 2단계로 줄인다 (팀장 → 본부장).
DELETE FROM approval_line WHERE approval_line_id=9048;
