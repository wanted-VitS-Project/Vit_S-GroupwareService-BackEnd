-- =====================================================================
-- KDT 99. 검증 — 전부 0 이어야 한다
-- ---------------------------------------------------------------------
-- 무엇: 01~11 을 다 넣은 뒤 한 번에 돌리는 불변식 검사.
-- 왜:   여기서 걸리는 것들은 **화면에서 안 보인다.** 컴파일도 안 하고 예외도 안 나고
--       숫자나 순서만 조용히 틀린다.
--
-- ⛔ 이 파일은 데이터를 바꾸지 않는다. 읽기만 한다.
-- =====================================================================

SELECT '① 배치 합≠3 (BLK-003)' AS 검사, COUNT(*) AS 위반 FROM (
  SELECT step_id, row_index FROM block
  WHERE block_id BETWEEN 8001 AND 8252 AND deleted_at IS NULL
  GROUP BY step_id, row_index HAVING SUM(col_span) <> 3) t

UNION ALL SELECT '② issue_block 이 다른 스텝 (BLK-009)', COUNT(*)
FROM issue_block ib
JOIN issue i ON i.issue_id = ib.issue_id
JOIN block b ON b.block_id = ib.block_id
WHERE ib.issue_block_id BETWEEN 8001 AND 8067 AND i.step_id <> b.step_id

UNION ALL SELECT '③ 블록 없는 결재', COUNT(*)
FROM approval a LEFT JOIN block b ON b.block_id = a.block_id
WHERE a.approval_id BETWEEN 8001 AND 8043 AND b.block_id IS NULL

UNION ALL SELECT '④ 현재 회차가 없는 결재', COUNT(*)
FROM approval a WHERE a.approval_id BETWEEN 8001 AND 8043
  AND NOT EXISTS (SELECT 1 FROM approval_revision r
                   WHERE r.approval_id = a.approval_id AND r.revision_no = a.current_revision_no)

UNION ALL SELECT '⑤ file_version 번호 불연속', COUNT(*) FROM (
  SELECT file_id FROM file_version WHERE file_id BETWEEN 8001 AND 8029
  GROUP BY file_id HAVING COUNT(*) <> MAX(version_no) OR MIN(version_no) <> 1) t

UNION ALL SELECT '⑥ image order_index 중복', COUNT(*) FROM (
  SELECT img_block_id, order_index FROM image
  WHERE img_block_id BETWEEN 8001 AND 8015 AND deleted_at IS NULL
  GROUP BY 1, 2 HAVING COUNT(*) > 1) t

UNION ALL SELECT '⑦ 고아 TEXT 상세', COUNT(*)
FROM `text` t LEFT JOIN block b ON b.block_id = t.block_id
WHERE t.txt_id BETWEEN 8001 AND 8144 AND b.block_id IS NULL

UNION ALL SELECT '⑧ 담당자 없는 이슈', COUNT(*)
FROM issue i WHERE i.issue_id BETWEEN 8001 AND 8071
  AND NOT EXISTS (SELECT 1 FROM issue_assign a WHERE a.issue_id = i.issue_id)

UNION ALL SELECT '⑨ 이슈 0건인 스텝 (INV-04)', COUNT(*)
FROM step s WHERE s.project_id BETWEEN 8001 AND 8010 AND s.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM issue i WHERE i.step_id = s.step_id)

-- 🚨 여기가 제일 조용히 틀리는 곳이다
UNION ALL SELECT '⑩ 정산 실지급 ≠ 연결된 입출금 합계', COUNT(*)
FROM settlement_block s WHERE s.settle_id BETWEEN 8001 AND 8011
  AND IFNULL(s.actual_amount, 0) <> IFNULL(
      (SELECT SUM(c.amount) FROM cash_flow c
        WHERE c.settle_block_id = s.settle_id AND c.deleted_at IS NULL), 0)

UNION ALL SELECT '⑪ 연결 대상 없는 입출금', COUNT(*)
FROM cash_flow c LEFT JOIN settlement_block s ON s.settle_id = c.settle_block_id
WHERE c.company_id = 3 AND c.settle_block_id IS NOT NULL AND s.settle_id IS NULL

UNION ALL SELECT '⑫ 연결 대상 없는 세금계산서', COUNT(*)
FROM tax_invoice x LEFT JOIN settlement_block s ON s.settle_id = x.settle_block_id
WHERE x.company_id = 3 AND x.settle_block_id IS NOT NULL AND s.settle_id IS NULL

UNION ALL SELECT '⑬ 계산서 공급가+세액 ≠ 총액', COUNT(*)
FROM tax_invoice WHERE company_id = 3 AND supply_amount + tax_amount <> total_amount

UNION ALL SELECT '⑭ 훈련비 매출에 부가세가 붙었다', COUNT(*)
FROM tax_invoice WHERE company_id = 3 AND type = 'INCOME'
  AND item_name LIKE '%훈련비%' AND tax_amount <> 0

UNION ALL SELECT '⑮ 기안자가 자기 결재선에', COUNT(*)
FROM approval a
JOIN approval_revision r ON r.approval_id = a.approval_id AND r.revision_no = a.current_revision_no
JOIN approval_line l ON l.approval_revision_id = r.approval_revision_id AND l.user_id = a.user_id
WHERE a.approval_id BETWEEN 8001 AND 8043

UNION ALL SELECT '⑯ 결재선 내 동일인 중복', COUNT(*) FROM (
  SELECT approval_revision_id, user_id FROM approval_line
  WHERE approval_revision_id BETWEEN 8001 AND 8044
  GROUP BY 1, 2 HAVING COUNT(*) > 1) t

UNION ALL SELECT '⑰ ACTIVE 없는 IN_PROGRESS 결재', COUNT(*)
FROM approval a
JOIN approval_revision r ON r.approval_id = a.approval_id AND r.revision_no = a.current_revision_no
WHERE a.approval_id BETWEEN 8001 AND 8043 AND a.status = 'IN_PROGRESS'
  AND NOT EXISTS (SELECT 1 FROM approval_line l
                   WHERE l.approval_revision_id = r.approval_revision_id AND l.status = 'ACTIVE')

UNION ALL SELECT '⑱ 결재자가 프로젝트 참여자가 아니다', COUNT(*)
FROM approval_line l
JOIN approval_revision r ON r.approval_revision_id = l.approval_revision_id
JOIN approval a ON a.approval_id = r.approval_id
JOIN block b ON b.block_id = a.block_id
JOIN step s ON s.step_id = b.step_id
LEFT JOIN project_member m ON m.project_id = s.project_id AND m.user_id = l.user_id
WHERE a.approval_id BETWEEN 8001 AND 8043 AND m.project_member_id IS NULL

UNION ALL SELECT '⑲ 기안 3건 미만 계정', COUNT(*)
FROM employee e WHERE e.company_id = 3 AND e.is_system = 0
  AND (SELECT COUNT(*) FROM approval a
        WHERE a.user_id = e.user_id OR a.acting_drafter_id = e.user_id) < 3

UNION ALL SELECT '⑳ 결재 대기 0건인 계정', COUNT(*)
FROM employee e WHERE e.company_id = 3 AND e.is_system = 0
  AND NOT EXISTS (
    SELECT 1 FROM approval_line l
    JOIN approval_revision r ON r.approval_revision_id = l.approval_revision_id
    JOIN approval a ON a.approval_id = r.approval_id AND a.current_revision_no = r.revision_no
    WHERE l.user_id = e.user_id AND l.status = 'ACTIVE')

UNION ALL SELECT '㉑ ADMIN 이 결재선·기안·프로젝트에', COUNT(*) FROM (
  SELECT l.user_id FROM approval_line l WHERE l.user_id IN ('vitaedu-VE112', 'vitaedu-VE113')
  UNION ALL SELECT a.user_id FROM approval a WHERE a.user_id IN ('vitaedu-VE112', 'vitaedu-VE113')
  UNION ALL SELECT m.user_id FROM project_member m WHERE m.user_id IN ('vitaedu-VE112', 'vitaedu-VE113')) t

UNION ALL SELECT '㉒ 부여 대상이 아닌 page_code', COUNT(*)
FROM page_permission p JOIN employee e ON e.user_id = p.user_id
WHERE e.company_id = 3 AND p.page_code NOT IN ('BIDDING', 'FINANCE')

UNION ALL SELECT '㉓ 회사 3 공고 상태 행 누락', COUNT(*)
FROM bid_notice n LEFT JOIN company_bid_notice_state s
  ON s.bid_notice_id = n.bid_notice_id AND s.company_id = 3
WHERE n.bid_notice_id BETWEEN 8001 AND 8007 AND s.company_bid_notice_state_id IS NULL

UNION ALL SELECT '㉔ 공고 첨부의 URL·키가 둘 다이거나 둘 다 없다', COUNT(*)
FROM bid_notice_attachment
WHERE bid_notice_id = 8001 AND NOT ((source_url IS NULL) XOR (storage_key IS NULL))

UNION ALL SELECT '㉕ activity_log resource_name 비었다', COUNT(*)
FROM activity_log WHERE activity_log_id BETWEEN 8001 AND 8034
  AND (resource_name IS NULL OR resource_name = '')

UNION ALL SELECT '㉖ modify 인데 변경 내역이 없다', COUNT(*)
FROM activity_log WHERE activity_log_id BETWEEN 8001 AND 8034 AND act = 'modify'
  AND (field IS NULL OR before_value IS NULL OR after_value IS NULL)

UNION ALL SELECT '㉗ file_version 이 업로드 중', COUNT(*)
FROM file_version WHERE file_version_id BETWEEN 8001 AND 8040 AND upload_status <> 'COMPLETED'

UNION ALL SELECT '㉘ 버전 코멘트가 비었거나 줄표', COUNT(*)
FROM file_version WHERE file_version_id BETWEEN 8001 AND 8040
  AND (comment IS NULL OR comment = '' OR comment LIKE '%—%')

UNION ALL SELECT '㉙ 문구 잔재 — 블록 제목', COUNT(*)
FROM block WHERE block_id BETWEEN 8001 AND 8252
  AND (title LIKE '%—%' OR title LIKE '%(가정)%')

UNION ALL SELECT '㉚ 문구 잔재 — 본문이 자기 자신을 설명', COUNT(*)
FROM `text` WHERE txt_id BETWEEN 8001 AND 8144
  AND (content LIKE '%—%' OR content LIKE '%(가정)%' OR content LIKE '%더미%'
       OR content LIKE '%이 블록%' OR content LIKE '%이 스텝%');


-- =====================================================================
-- 눈으로 보는 검사 — 숫자가 나란히 맞는지
-- =====================================================================

-- 1) 정산 3열. COMPLETED 는 세 숫자가 같고, WAITING 은 계산서만, PARTIAL 은 실지급 < 계약
-- SELECT s.settle_id, b.title, s.type, s.status,
--        s.total_amount AS 계약, s.actual_amount AS 실지급,
--        (SELECT SUM(c.amount)       FROM cash_flow c   WHERE c.settle_block_id = s.settle_id) AS 입출금,
--        (SELECT SUM(t.total_amount) FROM tax_invoice t WHERE t.settle_block_id = s.settle_id) AS 계산서
-- FROM settlement_block s JOIN block b ON b.block_id = s.block_id
-- WHERE s.settle_id BETWEEN 8001 AND 8011 ORDER BY s.type DESC, s.settle_id;

-- 2) 원장 3상태 분포 — 기대: 입출금 7/11/4/22 · 세금계산서 10/8/2/20
-- SELECT '입출금' AS 원장, SUM(settle_block_id IS NOT NULL) AS 연결됨,
--        SUM(settle_block_id IS NULL AND is_excluded = 0) AS 미연결,
--        SUM(is_excluded = 1) AS 연결제외, COUNT(*) AS 전체
-- FROM cash_flow WHERE company_id = 3 AND deleted_at IS NULL
-- UNION ALL SELECT '세금계산서', SUM(settle_block_id IS NOT NULL),
--        SUM(settle_block_id IS NULL AND is_excluded = 0), SUM(is_excluded = 1), COUNT(*)
-- FROM tax_invoice WHERE company_id = 3 AND deleted_at IS NULL;

-- 3) 계정별 기안·결재대기 — 11명 전원이 기안 3건 이상, 대기 1건 이상
-- SELECT e.user_id, e.name,
--        (SELECT COUNT(*) FROM approval a
--          WHERE a.user_id = e.user_id OR a.acting_drafter_id = e.user_id) AS 내가올린,
--        (SELECT COUNT(*) FROM approval_line l
--          JOIN approval_revision r ON r.approval_revision_id = l.approval_revision_id
--          JOIN approval a2 ON a2.approval_id = r.approval_id AND a2.current_revision_no = r.revision_no
--         WHERE l.user_id = e.user_id AND l.status = 'ACTIVE') AS 결재대기
-- FROM employee e WHERE e.company_id = 3 AND e.is_system = 0 ORDER BY e.user_id;

-- 4) P8001 진척률 — DONE 8 / IN_PROGRESS 4 / NOT_STARTED 6
-- SELECT status, COUNT(*) FROM step WHERE project_id = 8001 AND deleted_at IS NULL GROUP BY status;

-- 5) ⭐ 완료 스텝의 미완 이슈 — 1행(7차시 자막 동기화 재검수)만 나와야 한다
-- SELECT i.issue_id, i.title, s.name FROM issue i JOIN step s ON s.step_id = i.step_id
-- WHERE i.issue_id BETWEEN 8001 AND 8071 AND s.status = 'DONE' AND i.status <> 'DONE';

-- 6) 공고에서 프로젝트까지 이어지나
-- SELECT n.bid_notice_id, n.notice_name, s.confirmed, r.review_status, p.project_id
-- FROM bid_notice n
-- LEFT JOIN bid_notice_summary s ON s.bid_notice_id = n.bid_notice_id
-- LEFT JOIN bid_review r         ON r.bid_notice_id = n.bid_notice_id
-- LEFT JOIN project p            ON p.bid_notice_id = n.bid_notice_id
-- WHERE n.bid_notice_id = 8001;

-- 7) 🚨 화면에서 실제로 보이는지는 API 로 확인한다.
--    DB 카운트만 봐서는 역할 게이트에 걸리는 걸 못 잡는다.
--    GET /api/v1/approvals?scope=drafted&size=1
--    GET /api/v1/approvals?scope=pending&size=1
--    GET /api/v1/bidding/notices
--    GET /api/v1/finance/settlements


-- =====================================================================
-- KB 프로젝트(P8011) 검증 — 전부 0이어야 정상 (2026-08-17 throwaway MySQL 8 에서 통과 확인)
-- =====================================================================
SELECT 'KB col_span 규칙위반' k, COUNT(*) v FROM block
  WHERE block_id BETWEEN 8300 AND 8422
    AND ((type IN ('FILE','BID_NOTICE') AND col_span<>2) OR (type NOT IN ('FILE','BID_NOTICE') AND col_span<>1))
UNION ALL SELECT 'KB 정산 실지급≠연결합계', COUNT(*) FROM settlement_block s WHERE s.settle_id BETWEEN 8012 AND 8019
  AND IFNULL(s.actual_amount,0) <> IFNULL((SELECT SUM(c.amount) FROM cash_flow c WHERE c.settle_block_id=s.settle_id),0)
UNION ALL SELECT 'KB INCOME 과세 tax≠공급가10%', COUNT(*) FROM tax_invoice
  WHERE tax_id BETWEEN 8021 AND 8032 AND total_amount>0 AND tax_amount <> ROUND(supply_amount*0.1)
UNION ALL SELECT 'KB version_no 불연속', COUNT(*) FROM (
  SELECT file_id FROM file_version WHERE file_version_id BETWEEN 8041 AND 8067
  GROUP BY file_id HAVING COUNT(*)<>MAX(version_no) OR MIN(version_no)<>1) t
UNION ALL SELECT 'KB order_index 중복', COUNT(*) FROM (
  SELECT img_block_id,order_index FROM image WHERE img_id BETWEEN 8300 AND 8320 GROUP BY 1,2 HAVING COUNT(*)>1) t
UNION ALL SELECT 'KB issue_block 다른스텝', COUNT(*) FROM issue_block ib
  JOIN issue i USING(issue_id) JOIN block b ON b.block_id=ib.block_id
  WHERE ib.issue_block_id BETWEEN 8068 AND 8090 AND i.step_id<>b.step_id
UNION ALL SELECT 'KB 블록없는 결재', COUNT(*) FROM approval a
  LEFT JOIN block b ON b.block_id=a.block_id WHERE a.approval_id BETWEEN 8044 AND 8056 AND b.block_id IS NULL
UNION ALL SELECT 'KB 기안자가 자기결재선에', COUNT(*) FROM approval ap
  JOIN approval_revision r ON r.approval_id=ap.approval_id AND r.revision_no=ap.current_revision_no
  JOIN approval_line l ON l.approval_revision_id=r.approval_revision_id AND l.user_id=ap.user_id
  WHERE ap.approval_id BETWEEN 8044 AND 8056
UNION ALL SELECT 'KB ACTIVE없는 IN_PROGRESS결재', COUNT(*) FROM approval ap
  JOIN approval_revision r ON r.approval_id=ap.approval_id AND r.revision_no=ap.current_revision_no
  WHERE ap.approval_id BETWEEN 8044 AND 8056 AND ap.status='IN_PROGRESS'
    AND NOT EXISTS(SELECT 1 FROM approval_line l WHERE l.approval_revision_id=r.approval_revision_id AND l.status='ACTIVE')
UNION ALL SELECT 'KB activity resource_name 빈값', COUNT(*) FROM activity_log
  WHERE activity_log_id BETWEEN 8035 AND 8061 AND (resource_name IS NULL OR resource_name='')
UNION ALL SELECT 'KB 활성 FILE블록 연결없음(껍데기 8394 제외)', COUNT(*) FROM block b JOIN step s ON s.step_id=b.step_id
  WHERE b.type='FILE' AND b.step_id BETWEEN 8051 AND 8067 AND b.deleted_at IS NULL AND s.status<>'NOT_STARTED'
    AND NOT EXISTS(SELECT 1 FROM block_file bf WHERE bf.block_id=b.block_id);
