-- =====================================================================
-- 13. 문서 확장 — 파일 19건/버전 30건 → 28건/버전 57건
-- ---------------------------------------------------------------------
-- 기존에는 v2 이상인 파일이 4건뿐이라 '버전 관리' 기능이 화면에 거의 안 보였다.
-- (1) 기존 파일 7건에 v2/v3 를 얹고 (2) 신규 파일 9건을 붙인다.
--
-- ⚠️ version_no 는 file_id 안에서 1부터 연속이어야 한다. 아래 v2/v3 는
--    현재 DB 의 마지막 버전 번호를 실제로 조회해서 이어 붙인 값이다
--    (9001·9003·9004·9005·9010·9011·9013 은 전부 v1 까지만 있었다).
-- ⚠️ comment 는 화면 버전 목록에 그대로 뜬다. "수정" 같은 말 대신
--    무엇이 바뀌었는지 적어야 데모에서 읽힌다.
--
-- storage_key 는 실제 S3 오브젝트가 아니다 — 다운로드는 404 난다. 목록/버전 UI 용.
--
-- 되돌리기: DELETE FROM block_file WHERE file_id BETWEEN 9020 AND 9028;
--           DELETE FROM file_version WHERE file_version_id BETWEEN 9031 AND 9057;
--           DELETE FROM file WHERE file_id BETWEEN 9020 AND 9028;
-- =====================================================================

-- ── (1) 기존 파일에 후속 버전 추가 ────────────────────────────────
INSERT INTO file_version
  (file_version_id, file_id, version_no, upload_status, storage_key, original_file_name,
   extension, mime_type, size_bytes, page_count, comment,
   uploaded_by, uploader_name, uploader_department, uploader_position, completed_at) VALUES

-- f9001 채널조사_무신사
(9031, 9001, 2, 'COMPLETED', 'demo/file/9001/v2.pdf', '채널조사_무신사_v2.pdf', 'pdf', 'application/pdf',
 2410000, 18, '물류비 산정 기준 확인 결과 반영 — 실질 부담 17.2%',
 'vitawear-VW102', '박준호', '브랜드팀', '사원', '2025-11-12 17:40:00'),

-- f9003 VITAWEAR_브랜드소개서
(9032, 9003, 2, 'COMPLETED', 'demo/file/9003/v2.pdf', 'VITAWEAR_브랜드소개서_v2.pdf', 'pdf', 'application/pdf',
 5820000, 8, '디자인 시안 적용 — 텍스트 위주 초안에서 이미지 레이아웃으로',
 'vitawear-VW104', '정민아', '디자인팀', '대리', '2025-12-16 15:20:00'),
(9033, 9003, 3, 'COMPLETED', 'demo/file/9003/v3.pdf', 'VITAWEAR_브랜드소개서_v3.pdf', 'pdf', 'application/pdf',
 6110000, 10, '브랜드 히스토리 2p 추가 — MD 가 스토리 부족하다고 피드백',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2025-12-23 11:05:00'),

-- f9004 VITAWEAR_26SS_LOOKBOOK
(9034, 9004, 2, 'COMPLETED', 'demo/file/9004/v2.pdf', 'VITAWEAR_26SS_LOOKBOOK_v2.pdf', 'pdf', 'application/pdf',
 14200000, 24, '정방형 크롭 재편집 + 재촬영 3컷 교체',
 'vitawear-VW104', '정민아', '디자인팀', '대리', '2025-12-22 18:10:00'),

-- f9005 판매가_수수료산출_26SS
(9035, 9005, 2, 'COMPLETED', 'demo/file/9005/v2.xlsx', '판매가_수수료산출_26SS_v2.xlsx', 'xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
 128000, NULL, '수수료 12% → 15% 정정 (계약 조건 확인)',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2025-12-12 14:30:00'),
(9036, 9005, 3, 'COMPLETED', 'demo/file/9005/v3.xlsx', '판매가_수수료산출_26SS_v3.xlsx', 'xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
 141000, NULL, '스타일별 차등 마진 확정본 — 아우터 상향/기본티 하향',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2025-12-16 13:50:00'),

-- f9010 작업지시서_26SS_1차
(9037, 9010, 2, 'COMPLETED', 'demo/file/9010/v2.pdf', '작업지시서_26SS_1차_v2.pdf', 'pdf', 'application/pdf',
 2260000, 14, '니트 4종 원단 혼용률 표기 추가',
 'vitawear-VW111', '노현주', '생산관리팀', '대리', '2026-01-02 11:20:00'),

-- f9011 발주서_26SS_1차
(9038, 9011, 2, 'COMPLETED', 'demo/file/9011/v2.pdf', '발주서_26SS_1차_v2.pdf', 'pdf', 'application/pdf',
 980000, 3, '스타일별 수량 재배분 반영 — 총 수량 동일',
 'vitawear-VW109', '윤태경', '영업팀', '과장', '2026-02-03 17:10:00'),

-- f9013 작업지시서_26SS_2차
(9039, 9013, 2, 'COMPLETED', 'demo/file/9013/v2.pdf', '작업지시서_26SS_2차_v2.pdf', 'pdf', 'application/pdf',
 2340000, 15, '니트 3종 원단 단종 → 대체 원단 스펙으로 교체',
 'vitawear-VW111', '노현주', '생산관리팀', '대리', '2026-03-12 16:40:00'),
(9040, 9013, 3, 'COMPLETED', 'demo/file/9013/v3.pdf', '작업지시서_26SS_2차_v3.pdf', 'pdf', 'application/pdf',
 2380000, 16, '대체 원단 색차 허용 범위 명시 (ΔE 1.5 이내)',
 'vitawear-VW111', '노현주', '생산관리팀', '대리', '2026-03-13 10:15:00');


-- ── (2) 신규 파일 9건 ─────────────────────────────────────────────
INSERT INTO file (file_id, project_id, name, created_by) VALUES
(9020, 9001, '사이즈스펙_26SS_12스타일',   'vitawear-VW104'),
(9021, 9001, '무신사_MD_피드백_정리',      'vitawear-VW101'),
(9022, 9001, '파트너센터_권한신청서',      'vitawear-VW101'),
(9023, 9001, '상세페이지_카피_원고',       'vitawear-VW102'),
(9024, 9001, '검품리포트_26SS_1차',        'vitawear-VW105'),
(9025, 9001, '거래명세서_B공장_20260316',  'vitawear-VW108'),
(9026, 9001, '검품리포트_26SS_2차',        'vitawear-VW105'),
(9027, 9001, '정산대조_202603',            'vitawear-VW108'),
(9028, 9001, '세금계산서_발행내역_2603',   'vitawear-VW108');

INSERT INTO file_version
  (file_version_id, file_id, version_no, upload_status, storage_key, original_file_name,
   extension, mime_type, size_bytes, page_count, comment,
   uploaded_by, uploader_name, uploader_department, uploader_position, completed_at) VALUES

-- 9020 사이즈스펙 (v3) — 오탈자 이슈 #9022 와 이어진다
(9041, 9020, 1, 'COMPLETED', 'demo/file/9020/v1.xlsx', '사이즈스펙_26SS_12스타일_v1.xlsx', 'xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
 76000, NULL, '패턴실 스펙 그대로 옮긴 초안',
 'vitawear-VW104', '정민아', '디자인팀', '대리', '2025-12-12 10:00:00'),
(9042, 9020, 2, 'COMPLETED', 'demo/file/9020/v2.xlsx', '사이즈스펙_26SS_12스타일_v2.xlsx', 'xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
 79000, NULL, 'inch 표기 5건 cm 로 통일',
 'vitawear-VW104', '정민아', '디자인팀', '대리', '2025-12-31 16:00:00'),
(9043, 9020, 3, 'COMPLETED', 'demo/file/9020/v3.xlsx', '사이즈스펙_26SS_12스타일_v3.xlsx', 'xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
 83000, NULL, '샘플 실측값으로 재기입 — 패턴 스펙과 최대 1.5cm 차이 있었음',
 'vitawear-VW104', '정민아', '디자인팀', '대리', '2026-01-02 09:40:00'),

-- 9021 MD 피드백 정리 (v2)
(9044, 9021, 1, 'COMPLETED', 'demo/file/9021/v1.docx', '무신사_MD_피드백_정리_v1.docx', 'docx',
 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
 62000, 4, '통화 내용 받아적은 초안',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2026-01-09 14:00:00'),
(9045, 9021, 2, 'COMPLETED', 'demo/file/9021/v2.docx', '무신사_MD_피드백_정리_v2.docx', 'docx',
 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
 71000, 6, '항목별 대응 방침·담당자·기한 추가',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2026-01-13 14:50:00'),

-- 9022 권한신청서 (v1)
(9046, 9022, 1, 'COMPLETED', 'demo/file/9022/v1.pdf', '파트너센터_권한신청서_v1.pdf', 'pdf', 'application/pdf',
 410000, 2, '3명분 일괄 신청',
 'vitawear-VW101', '김서연', '브랜드팀', '대리', '2026-01-16 14:20:00'),

-- 9023 상세페이지 카피 (v2)
(9047, 9023, 1, 'COMPLETED', 'demo/file/9023/v1.docx', '상세페이지_카피_원고_v1.docx', 'docx',
 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
 88000, 12, '12스타일 카피 초안',
 'vitawear-VW102', '박준호', '브랜드팀', '사원', '2026-01-18 17:00:00'),
(9048, 9023, 2, 'COMPLETED', 'demo/file/9023/v2.docx', '상세페이지_카피_원고_v2.docx', 'docx',
 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
 94000, 13, '소재·세탁 표기 문구 표준화, 과장 표현 3건 삭제',
 'vitawear-VW102', '박준호', '브랜드팀', '사원', '2026-01-23 11:30:00'),

-- 9024 검품리포트 1차 (v2)
(9049, 9024, 1, 'COMPLETED', 'demo/file/9024/v1.xlsx', '검품리포트_26SS_1차_v1.xlsx', 'xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
 112000, NULL, '전수 검품 결과 — 불량 22장',
 'vitawear-VW105', '최동석', '물류·CS팀', '대리', '2026-02-09 15:20:00'),
(9050, 9024, 2, 'COMPLETED', 'demo/file/9024/v2.xlsx', '검품리포트_26SS_1차_v2.xlsx', 'xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
 118000, NULL, '불량 사유 분류 추가 (봉제 14 / 오염 5 / 사이즈 3)',
 'vitawear-VW105', '최동석', '물류·CS팀', '대리', '2026-02-11 10:40:00'),

-- 9025 거래명세서 B공장 (v1)
(9051, 9025, 1, 'COMPLETED', 'demo/file/9025/v1.pdf', '거래명세서_B공장_20260316.pdf', 'pdf', 'application/pdf',
 640000, 2, '2차 입고분',
 'vitawear-VW108', '조은비', '재무팀', '과장', '2026-03-16 13:00:00'),

-- 9026 검품리포트 2차 (v3) — 불량률 상승 이슈 #9037 과 이어진다
(9052, 9026, 1, 'COMPLETED', 'demo/file/9026/v1.xlsx', '검품리포트_26SS_2차_v1.xlsx', 'xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
 124000, NULL, '전수 검품 결과 — 불량률 1.8%',
 'vitawear-VW105', '최동석', '물류·CS팀', '대리', '2026-03-17 16:10:00'),
(9053, 9026, 2, 'COMPLETED', 'demo/file/9026/v2.xlsx', '검품리포트_26SS_2차_v2.xlsx', 'xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
 131000, NULL, '라인별 집계 추가 — 신규 라인에 불량 68% 집중',
 'vitawear-VW105', '최동석', '물류·CS팀', '대리', '2026-03-18 14:00:00'),
(9054, 9026, 3, 'COMPLETED', 'demo/file/9026/v3.xlsx', '검품리포트_26SS_2차_v3.xlsx', 'xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
 136000, NULL, '공장 회신·재발방지 조치 첨부 (최종)',
 'vitawear-VW105', '최동석', '물류·CS팀', '대리', '2026-03-19 15:30:00'),

-- 9027 정산대조 3월분 (v2)
(9055, 9027, 1, 'COMPLETED', 'demo/file/9027/v1.xlsx', '정산대조_202603_v1.xlsx', 'xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
 101000, NULL, 'SKU 단위 대조 — 기획전 판매분 수수료 18% 항목 발견',
 'vitawear-VW108', '조은비', '재무팀', '과장', '2026-04-07 11:00:00'),
(9056, 9027, 2, 'COMPLETED', 'demo/file/9027/v2.xlsx', '정산대조_202603_v2.xlsx', 'xlsx',
 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
 104000, NULL, '기획전 약관 확인 후 정상 처리 — 차이 0원으로 종결',
 'vitawear-VW108', '조은비', '재무팀', '과장', '2026-04-08 09:30:00'),

-- 9028 세금계산서 발행내역 (v1)
(9057, 9028, 1, 'COMPLETED', 'demo/file/9028/v1.pdf', '세금계산서_발행내역_2603.pdf', 'pdf', 'application/pdf',
 220000, 1, '홈택스 발행 완료분',
 'vitawear-VW108', '조은비', '재무팀', '과장', '2026-04-08 11:10:00');


-- ── (3) 블록 연결 ─────────────────────────────────────────────────
-- 신규 파일은 전부 해당 스텝의 FILE 블록에 붙인다.
INSERT INTO block_file (block_id, file_id, linked_by) VALUES
(9018, 9020, 'vitawear-VW104'),  -- s9003 소개서·룩북·판매가 시트
(9032, 9021, 'vitawear-VW101'),  -- s9005 입점 승인 통보서
(9038, 9022, 'vitawear-VW101'),  -- s9006 계약서·세팅 확인서
(9044, 9023, 'vitawear-VW102'),  -- s9007 일괄 업로드 시트
(9052, 9024, 'vitawear-VW105'),  -- s9008 작업지시서·발주서·거래명세서
(9066, 9025, 'vitawear-VW108'),  -- s9010 작업지시서·발주서
(9066, 9026, 'vitawear-VW105'),  -- s9010
(9093, 9027, 'vitawear-VW108'),  -- s9014 정산서·대조 시트
(9093, 9028, 'vitawear-VW108');  -- s9014
