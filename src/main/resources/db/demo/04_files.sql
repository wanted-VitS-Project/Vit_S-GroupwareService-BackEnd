-- =====================================================================
-- 04. 파일 19 · 버전 30 · 블록 연결 17 (파일 2건은 블록 미부착)
-- ---------------------------------------------------------------------
-- 무엇: file / file_version / block_file.
-- 왜:   "보고서_최종2.pdf 를 죽인다" 가 이 제품의 핵심 주장이다.
--       버전이 실제로 쌓여 있지 않으면 그 주장을 화면으로 못 보여준다.
--
-- 선행: 03_blocks.sql
--
-- ⚠️ 파일은 프로젝트 소속이다 (file.project_id). 블록은 block_file 로 참조만 한다.
--    블록을 지워도 파일은 프로젝트 문서함에 산다 (BLOCK.md §4-4).
--
-- 🚨 S3 실물이 없다. storage_key 만 채운다 — 다운로드는 404 다.
--    발표에서 파일을 열지 마라. 목록·버전 표시까지만 쓴다.
--
-- ⭐ 다버전은 4개다
--    file 9002 입점추진_사업성검토  v1~v5  ← 반려 후 재상신. 결재 대상은 v5
--    file 9009 상품등록_일괄_26SS   v1~v6  ← 오류 수정이 버전으로 쌓인다
--    file 9007 무신사_입점계약서    v1~v2  ← 결재 대상 v1, 승인 후 서명본 v2 → 「새 버전 있음」 배지
--    file 9017 정산대조_202602      v1~v2  ← 이의 반영본
--    나머지 13개는 v1 단일. 40건 업로드를 흉내내봐야 발표에서 안 연다.
--
-- ⚠️ 07_approval.sql 이 아래 file_version_id 를 지목한다. 여기 ID 를 바꾸면 07 이 깨진다.
--    approval 9001 rev1 → fv 9005 (사업성검토 v4 · 반려된 상신)
--    approval 9001 rev2 → fv 9006 (사업성검토 v5 · 승인)
--    approval 9002      → fv 9007 · 9008 (소개서 v1 · 룩북 v1)
--    approval 9003      → fv 9011 (계약서 v1 최종안) ⭐ v2 서명본이 아니다
--    approval 9004      → fv 9020 (1차 발주서 v1)
--    approval 9005      → fv 9023 (2차 발주서 v1)
--
-- 되돌리기: DELETE FROM block_file   WHERE file_id BETWEEN 9001 AND 9019;
--           DELETE FROM file_version WHERE file_version_id BETWEEN 9001 AND 9030;
--           DELETE FROM file         WHERE file_id BETWEEN 9001 AND 9019;
-- =====================================================================


-- ── 1. file 19 ───────────────────────────────────────────────────────
INSERT IGNORE INTO file (file_id, project_id, name, created_by) VALUES
(9001, 9001, '채널조사_무신사',              'vitawear-VW101'),
(9002, 9001, '입점추진_사업성검토',          'vitawear-VW101'),
(9003, 9001, 'VITAWEAR_브랜드소개서',        'vitawear-VW101'),
(9004, 9001, 'VITAWEAR_26SS_LOOKBOOK',       'vitawear-VW104'),
(9005, 9001, '판매가_수수료산출_26SS',       'vitawear-VW102'),
(9006, 9001, '무신사_입점승인_통보서',       'vitawear-VW102'),
(9007, 9001, '무신사_입점계약서',             'vitawear-VW103'),
(9008, 9001, '파트너세팅_완료확인서',        'vitawear-VW108'),
(9009, 9001, '상품등록_일괄_26SS',           'vitawear-VW102'),
(9010, 9001, '작업지시서_26SS_1차',          'vitawear-VW105'),
(9011, 9001, '발주서_26SS_1차',              'vitawear-VW105'),
(9012, 9001, '거래명세서_A공장_20260207',    'vitawear-VW105'),
(9013, 9001, '작업지시서_26SS_2차',          'vitawear-VW105'),
(9014, 9001, '발주서_26SS_2차',              'vitawear-VW105'),
(9015, 9001, '무신사_정산서_202602',         'vitawear-VW108'),
(9016, 9001, '무신사_정산서_202603',         'vitawear-VW108'),
(9017, 9001, '정산대조_202602',              'vitawear-VW108'),
-- ⭐ 아래 2건은 어떤 블록에도 안 붙인다 — 파일은 **프로젝트 소속**이지 블록 소속이 아니다.
--    `file.project_id` 가 주인이고 `block_file` 은 참조일 뿐이다 (BLOCK.md §4-4).
--    문서함(`/projects/[id]/files`)에만 사는 파일이 정상 케이스인데, 전부 블록에 붙여두면
--    블록 없이는 파일이 존재할 수 없는 것처럼 보인다.
(9018, 9001, '사업자등록증',                 'vitawear-VW101'),
(9019, 9001, '상표권_출원증',                'vitawear-VW101');


-- ── 2. file_version 30 ───────────────────────────────────────────────
-- ⚠️ uploader_name/department/position 은 비정규화 스냅샷이다.
--    사원이 부서를 옮겨도 과거 버전의 표기는 안 바뀐다 — 그게 의도다.
-- ⚠️ upload_status 는 'COMPLETED' 다 ('DONE' 아님 · UploadStatus enum 확인).
INSERT IGNORE INTO file_version
  (file_version_id, file_id, version_no, upload_status, storage_key, original_file_name,
   extension, mime_type, size_bytes, comment,
   uploaded_by, uploader_name, uploader_department, uploader_position, created_at, completed_at) VALUES

-- f9001 채널조사 v1
(9001, 9001, 1, 'COMPLETED', 'demo/file/9001/v1.pdf', '채널조사_무신사_v1.pdf',
 'pdf', 'application/pdf', 2841000, '초안',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2025-11-13 17:20:00', '2025-11-13 17:20:12'),

-- ⭐ f9002 사업성 검토 v1~v5 — v4 반려 → v5 재상신 → 승인
(9002, 9002, 1, 'COMPLETED', 'demo/file/9002/v1.pdf', '입점추진_사업성검토_v1.pdf',
 'pdf', 'application/pdf', 3120000, '초안',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2025-11-26 10:05:00', '2025-11-26 10:05:18'),
(9003, 9002, 2, 'COMPLETED', 'demo/file/9002/v2.pdf', '입점추진_사업성검토_v2.pdf',
 'pdf', 'application/pdf', 3244000, 'AI 검토 결과 반영',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2025-11-27 14:40:00', '2025-11-27 14:40:21'),
(9004, 9002, 3, 'COMPLETED', 'demo/file/9002/v3.pdf', '입점추진_사업성검토_v3.pdf',
 'pdf', 'application/pdf', 3298000, '마진 시뮬레이션 3종 추가',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2025-11-28 09:12:00', '2025-11-28 09:12:30'),
(9005, 9002, 4, 'COMPLETED', 'demo/file/9002/v4.pdf', '입점추진_사업성검토_v4.pdf',
 'pdf', 'application/pdf', 3310000, '상신본 (2025-11-28) — 11-29 반려',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2025-11-28 16:00:00', '2025-11-28 16:00:14'),
(9006, 9002, 5, 'COMPLETED', 'demo/file/9002/v5.pdf', '입점추진_사업성검토_v5.pdf',
 'pdf', 'application/pdf', 3402000, '반품율 12% 가정 반영 후 재상신 — 최종 승인본',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2025-12-02 11:30:00', '2025-12-02 11:30:26'),

-- f9003~9005 제출물
(9007, 9003, 1, 'COMPLETED', 'demo/file/9003/v1.pdf', 'VITAWEAR_브랜드소개서_v1.pdf',
 'pdf', 'application/pdf', 8420000, '8p 최종',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2025-12-10 18:20:00', '2025-12-10 18:20:44'),
(9008, 9004, 1, 'COMPLETED', 'demo/file/9004/v1.pdf', 'VITAWEAR_26SS_LOOKBOOK_v1.pdf',
 'pdf', 'application/pdf', 24800000, '24p 최종',
 'vitawear-VW104', '정민아', '디자인팀', '대리', '2025-12-11 20:05:00', '2025-12-11 20:07:02'),
(9009, 9005, 1, 'COMPLETED', 'demo/file/9005/v1.xlsx', '판매가_수수료산출_26SS_v1.xlsx',
 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 186000, '12종 전체',
 'vitawear-VW102', '박준호', '브랜드팀', '사원', '2025-12-11 15:10:00', '2025-12-11 15:10:05'),

-- f9006 승인 통보서
(9010, 9006, 1, 'COMPLETED', 'demo/file/9006/v1.pdf', '무신사_입점승인_20251216.pdf',
 'pdf', 'application/pdf', 640000, '수신본',
 'vitawear-VW102', '박준호', '브랜드팀', '사원', '2025-12-16 14:35:00', '2025-12-16 14:35:08'),

-- f9007~9008 계약·세팅
-- ⭐ f9007 계약서 v1 최종안(결재 대상) → v2 서명본
--    결재는 v1 을 고정한다. v2 는 승인 뒤에 올라온 새 버전이라
--    결재 화면에 「대상보다 새 버전 있음」 경고 배지가 뜬다 (BLOCK.md §4-7).
--    ⚠️ 서명본을 결재 대상으로 박으면 「승인 전에 이미 서명된 문서」가 되어 시간이 거꾸로 간다.
(9011, 9007, 1, 'COMPLETED', 'demo/file/9007/v1.pdf', '무신사_입점계약서_최종안_v1.pdf',
 'pdf', 'application/pdf', 1880000, '결재 대상 최종안',
 'vitawear-VW103', '이현우', '브랜드팀', '팀장', '2025-12-18 09:20:00', '2025-12-18 09:20:17'),
(9028, 9007, 2, 'COMPLETED', 'demo/file/9007/v2.pdf', '무신사_입점계약서_서명본_v2.pdf',
 'pdf', 'application/pdf', 1920000, '양측 서명 완료본 — 승인 후 업로드',
 'vitawear-VW103', '이현우', '브랜드팀', '팀장', '2025-12-19 17:00:00', '2025-12-19 17:00:19'),
(9012, 9008, 1, 'COMPLETED', 'demo/file/9008/v1.pdf', '파트너세팅_완료확인_v1.pdf',
 'pdf', 'application/pdf', 410000, NULL,
 'vitawear-VW108', '조은비', '재무팀', '과장', '2025-12-30 16:50:00', '2025-12-30 16:50:07'),

-- ⭐ f9009 상품등록 일괄 시트 v1~v6 — 오류 수정이 버전으로 쌓인다
(9013, 9009, 1, 'COMPLETED', 'demo/file/9009/v1.xlsx', '상품등록_일괄_26SS_v1.xlsx',
 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 322000, '초안 118 SKU',
 'vitawear-VW102', '박준호', '브랜드팀', '사원', '2026-01-12 11:00:00', '2026-01-12 11:00:09'),
(9014, 9009, 2, 'COMPLETED', 'demo/file/9009/v2.xlsx', '상품등록_일괄_26SS_v2.xlsx',
 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 324000, '원산지 코드 오류 수정 (VNM to VN)',
 'vitawear-VW102', '박준호', '브랜드팀', '사원', '2026-01-14 15:30:00', '2026-01-14 15:30:11'),
(9015, 9009, 3, 'COMPLETED', 'demo/file/9009/v3.xlsx', '상품등록_일괄_26SS_v3.xlsx',
 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 323000, '사이즈 옵션 중복 제거',
 'vitawear-VW102', '박준호', '브랜드팀', '사원', '2026-01-16 10:20:00', '2026-01-16 10:20:08'),
(9016, 9009, 4, 'COMPLETED', 'demo/file/9009/v4.xlsx', '상품등록_일괄_26SS_v4.xlsx',
 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 325000, '소재 혼용률 합계 정정 (5스타일)',
 'vitawear-VW102', '박준호', '브랜드팀', '사원', '2026-01-19 09:40:00', '2026-01-19 09:40:13'),
(9017, 9009, 5, 'COMPLETED', 'demo/file/9009/v5.xlsx', '상품등록_일괄_26SS_v5.xlsx',
 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 326000, '부분 통과 103/118',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2026-01-20 17:05:00', '2026-01-20 17:05:10'),
(9018, 9009, 6, 'COMPLETED', 'demo/file/9009/v6.xlsx', '상품등록_일괄_26SS_v6.xlsx',
 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 327000, '전건 통과 118/118 — 최종',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2026-01-22 14:15:00', '2026-01-22 14:15:16'),

-- f9010~9012 1차 생산
(9019, 9010, 1, 'COMPLETED', 'demo/file/9010/v1.pdf', '작업지시서_26SS_1차_v1.pdf',
 'pdf', 'application/pdf', 4210000, '12스타일',
 'vitawear-VW105', '최동석', '물류·CS팀', '대리', '2025-12-24 13:00:00', '2025-12-24 13:00:22'),
(9020, 9011, 1, 'COMPLETED', 'demo/file/9011/v1.pdf', '발주서_26SS_1차_v1.pdf',
 'pdf', 'application/pdf', 720000, '3,400장 / 72,420,000원',
 'vitawear-VW105', '최동석', '물류·CS팀', '대리', '2025-12-26 09:30:00', '2025-12-26 09:30:07'),
(9021, 9012, 1, 'COMPLETED', 'demo/file/9012/v1.pdf', '거래명세서_A공장_20260207.pdf',
 'pdf', 'application/pdf', 380000, NULL,
 'vitawear-VW105', '최동석', '물류·CS팀', '대리', '2026-02-07 18:10:00', '2026-02-07 18:10:04'),

-- f9013~9014 2차 생산
(9022, 9013, 1, 'COMPLETED', 'demo/file/9013/v1.pdf', '작업지시서_26SS_2차_v1.pdf',
 'pdf', 'application/pdf', 2140000, '5스타일',
 'vitawear-VW105', '최동석', '물류·CS팀', '대리', '2026-03-16 11:20:00', '2026-03-16 11:20:15'),
(9023, 9014, 1, 'COMPLETED', 'demo/file/9014/v1.pdf', '발주서_26SS_2차_v1.pdf',
 'pdf', 'application/pdf', 640000, '1,800장 / 39,600,000원',
 'vitawear-VW105', '최동석', '물류·CS팀', '대리', '2026-03-18 10:00:00', '2026-03-18 10:00:06'),

-- f9015~9017 정산
(9024, 9015, 1, 'COMPLETED', 'demo/file/9015/v1.pdf', '무신사_정산서_202602.pdf',
 'pdf', 'application/pdf', 540000, '2026-02월분',
 'vitawear-VW108', '조은비', '재무팀', '과장', '2026-03-05 10:15:00', '2026-03-05 10:15:05'),
(9025, 9016, 1, 'COMPLETED', 'demo/file/9016/v1.pdf', '무신사_정산서_202603.pdf',
 'pdf', 'application/pdf', 552000, '2026-03월분 — 1차 이의분 74,000 가산 반영',
 'vitawear-VW108', '조은비', '재무팀', '과장', '2026-04-03 09:50:00', '2026-04-03 09:50:06'),
-- ⭐ f9017 대조 시트 v1 → v2 (이의 반영본)
(9026, 9017, 1, 'COMPLETED', 'demo/file/9017/v1.xlsx', '정산대조_202602_v1.xlsx',
 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 94000, '대조 결과 — 차이 74,000 발견',
 'vitawear-VW108', '조은비', '재무팀', '과장', '2026-03-06 16:30:00', '2026-03-06 16:30:03'),
(9027, 9017, 2, 'COMPLETED', 'demo/file/9017/v2.xlsx', '정산대조_202602_v2.xlsx',
 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 96000, '이의 인정 반영 (2026-03-09)',
 'vitawear-VW108', '조은비', '재무팀', '과장', '2026-03-09 14:20:00', '2026-03-09 14:20:04'),

-- ⭐ f9018~9019 블록 미부착 — 문서함에만 사는 파일
(9029, 9018, 1, 'COMPLETED', 'demo/file/9018/v1.pdf', '사업자등록증.pdf',
 'pdf', 'application/pdf', 180000, '제출용 상시 보관',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2025-12-08 10:00:00', '2025-12-08 10:00:03'),
(9030, 9019, 1, 'COMPLETED', 'demo/file/9019/v1.pdf', '상표권_출원증.pdf',
 'pdf', 'application/pdf', 210000, '등록증은 2026-03 예정',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2025-12-08 10:02:00', '2025-12-08 10:02:04');


-- ── 3. block_file 17 ─────────────────────────────────────────────────
-- ⚠️ 복합 PK (block_id, file_id) 라 block.type_id 는 NULL 로 남는다. 그게 정상이다.
-- ⚠️ 3차 껍데기(9080)와 S6 껍데기(9099)에는 파일을 붙이지 않는다 — 미도래 구간이다.
INSERT IGNORE INTO block_file (block_id, file_id, linked_by, linked_at) VALUES
(9004, 9001, 'vitawear-VW101', '2025-11-13 17:22:00'),
(9012, 9002, 'vitawear-VW101', '2025-11-26 10:07:00'),
(9018, 9003, 'vitawear-VW101', '2025-12-10 18:22:00'),
(9018, 9004, 'vitawear-VW104', '2025-12-11 20:09:00'),
(9018, 9005, 'vitawear-VW102', '2025-12-11 15:12:00'),
(9032, 9006, 'vitawear-VW102', '2025-12-16 14:37:00'),
(9038, 9007, 'vitawear-VW103', '2025-12-19 17:03:00'),
(9038, 9008, 'vitawear-VW108', '2025-12-30 16:52:00'),
(9044, 9009, 'vitawear-VW102', '2026-01-12 11:02:00'),
(9052, 9010, 'vitawear-VW105', '2025-12-24 13:03:00'),
(9052, 9011, 'vitawear-VW105', '2025-12-26 09:32:00'),
(9052, 9012, 'vitawear-VW105', '2026-02-07 18:12:00'),
(9066, 9013, 'vitawear-VW105', '2026-03-16 11:22:00'),
(9066, 9014, 'vitawear-VW105', '2026-03-18 10:02:00'),
(9093, 9015, 'vitawear-VW108', '2026-03-05 10:17:00'),
(9093, 9016, 'vitawear-VW108', '2026-04-03 09:52:00'),
(9093, 9017, 'vitawear-VW108', '2026-03-06 16:32:00');


-- 검증
--  파일별 최신 버전이 의도대로인가 (9002=5, 9009=6, 9017=2, 나머지 1)
--    SELECT file_id, MAX(version_no) FROM file_version
--    WHERE file_version_id BETWEEN 9001 AND 9027 GROUP BY file_id ORDER BY file_id;
--  FILE 블록 11개 중 파일이 붙은 건 9개다 (껍데기 2개 제외)
--    SELECT b.block_id, b.title, COUNT(bf.file_id) FROM block b
--    LEFT JOIN block_file bf ON bf.block_id = b.block_id
--    WHERE b.type = 'FILE' AND b.block_id BETWEEN 9001 AND 9100 GROUP BY b.block_id;
