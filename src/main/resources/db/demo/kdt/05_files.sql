-- =====================================================================
-- KDT 05. 파일 29 · 버전 40 · 블록 연결 29
-- ---------------------------------------------------------------------
-- 무엇: FILE 블록에 붙는 산출물과 그 버전 이력.
-- 왜:   심사 서류는 한 번에 안 끝난다. 버전이 없으면 「어느 게 최종본인지」가 파일명에만 남는다.
--
-- 선행: 04_blocks.sql (FILE 블록 12개)
-- 후속: 08_approval.sql 이 approval_document 로 특정 file_version 을 결재 대상으로 고정한다.
--
-- 🚨 version_no 는 file 안에서 1부터 연속이어야 한다.
--    끊기거나 겹치면 버전 드롭다운이 비거나 같은 번호가 두 번 뜬다. 이 파일은 생성기로 뽑았다.
--
-- ⭐ 다버전 파일 2건이 시연의 축이다
--    훈련운영계획서 v1→v4 — 반려 뒤 재상신이 버전으로 남는다
--    훈련과정개요서 v1→v3 — J열 합계가 어긋났다가 맞춰지는 과정이 남는다
--
-- ⚠️ 서류 시점 역설
--    결재 대상은 최종안(v1)이고 날인본(v2)은 승인 뒤에 올라온다.
--    처음부터 날인본만 두면 「서명하고 나서 승인받은」 순서가 된다.
--    계약서와 협약서를 v1 최종안 / v2 날인본으로 쪼갠 이유다.
--
-- ⚠️ 실물 오브젝트는 S3 에 없다. storage_key 만 채운다.
--    발표에서 파일을 열지 마라 — 목록과 버전 표시까지만 보여준다.
--
-- 되돌리기:
--   DELETE FROM block_file   WHERE file_id BETWEEN 8001 AND 8029;
--   DELETE FROM file_version WHERE file_version_id BETWEEN 8001 AND 8040;
--   DELETE FROM file         WHERE file_id BETWEEN 8001 AND 8029;
-- =====================================================================


-- ── 1. 파일 ─────────────────────────────────────────────────────────
INSERT IGNORE INTO file (file_id, project_id, name, created_by, created_at) VALUES
(8001, 8001, '신청자격_수료인원_산정근거.xlsx', 'vitaedu-VE103', '2026-08-04 14:20:00'),
(8002, 8001, '납세증명서_20260803.pdf', 'vitaedu-VE109', '2026-08-03 11:26:00'),
(8003, 8001, '표준재무제표증명_2025.pdf', 'vitaedu-VE109', '2026-08-03 11:20:00'),
(8004, 8001, '자막_검수결과서.xlsx', 'vitaedu-VE107', '2026-08-11 17:10:00'),
(8005, 8001, '훈련운영계획서_비타에듀_AI기초.hwp', 'vitaedu-VE101', '2026-08-12 18:40:00'),
(8006, 8001, '교수학습설계_상세.hwp', 'vitaedu-VE106', '2026-08-13 15:00:00'),
(8007, 8001, '참여인력_운영관리계획.hwp', 'vitaedu-VE104', '2026-08-13 16:30:00'),
(8008, 8001, '훈련과정개요서_AI기초.xlsx', 'vitaedu-VE102', '2026-08-12 14:00:00'),
(8009, 8001, '수료생명단_2025_2026.xlsx', 'vitaedu-VE104', '2026-08-05 11:00:00'),
(8010, 8001, '참여인력_증빙목록.xlsx', 'vitaedu-VE102', '2026-08-14 10:10:00'),
(8011, 8001, '콘텐츠_자체개발확인서.hwp', 'vitaedu-VE102', '2026-08-16 10:00:00'),
(8012, 8001, '서약서.hwp', 'vitaedu-VE102', '2026-08-16 10:05:00'),
(8013, 8001, '개인정보_수집이용동의서.hwp', 'vitaedu-VE102', '2026-08-16 10:08:00'),
(8014, 8001, '협약서_비타웨어.hwp', 'vitaedu-VE101', '2026-08-14 15:20:00'),
(8015, 8001, '콘텐츠개발_계약서_러닝브릿지.pdf', 'vitaedu-VE109', '2026-07-06 14:00:00'),
(8016, 8001, '자막제작_계약서_보이스텍스트.pdf', 'vitaedu-VE109', '2026-07-08 16:20:00'),
(8017, 8001, '촬영용역_계약서_스튜디오원.pdf', 'vitaedu-VE109', '2026-07-08 16:30:00'),
(8018, 8001, '견적비교표_콘텐츠개발_3사.xlsx', 'vitaedu-VE109', '2026-06-29 11:00:00'),
(8019, 8001, '거래명세서_20260710.pdf', 'vitaedu-VE109', '2026-07-10 15:00:00'),
(8020, 8001, '거래명세서_20260810.pdf', 'vitaedu-VE109', '2026-08-10 11:30:00'),
(8021, 8001, '검수결과서_1차_20차시.pdf', 'vitaedu-VE107', '2026-08-07 17:00:00'),
(8022, 8001, '저작권양도확인서_러닝브릿지.pdf', 'vitaedu-VE109', '2026-08-10 11:35:00'),
(8023, 8002, '수료자명단_1기.xlsx', 'vitaedu-VE104', '2026-03-27 16:00:00'),
(8024, 8002, '만족도조사결과_1기.pdf', 'vitaedu-VE104', '2026-03-27 16:10:00'),
(8025, 8002, '훈련비청구서_1차_202604.pdf', 'vitaedu-VE109', '2026-04-05 10:00:00'),
(8026, 8002, '훈련비청구서_2차_202606.pdf', 'vitaedu-VE109', '2026-06-05 10:00:00'),
(8027, 8002, '훈련비청구서_3차_202608.pdf', 'vitaedu-VE109', '2026-08-05 10:00:00'),
(8028, 8002, '정산대조표_1차_2차.xlsx', 'vitaedu-VE109', '2026-06-16 14:00:00'),
(8029, 8002, '환수조정내역_202608.xlsx', 'vitaedu-VE109', '2026-08-13 09:45:00');


-- ── 2. 버전 ─────────────────────────────────────────────────────────
-- ⚠️ comment 는 「수정」·「최종본」 같은 말로 두지 마라. 버전 화면이 죽는다.
--    무엇이 바뀌었는지 적어야 두 버전을 나란히 놓고 판단할 수 있다.
-- ⚠️ uploader_name 은 NOT NULL 이다. 스냅샷이라 사원 정보가 바뀌어도 과거 버전은 옛 값을 유지한다.
INSERT IGNORE INTO file_version
  (file_version_id, file_id, version_no, storage_key,
   original_file_name, extension, mime_type, size_bytes, comment,
   uploaded_by, uploader_name, uploader_department, uploader_position, completed_at) VALUES
(8001, 8001, 1, 'projects/8001/files/8001/v1.xlsx', '신청자격_수료인원_산정근거.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 142000, '2025년부터 마감일까지 종료 과정 전건 집계', 'vitaedu-VE103', '남기훈', '사업기획실', '팀장', '2026-08-04 14:20:00'),
(8002, 8001, 2, 'projects/8001/files/8001/v2.xlsx', '신청자격_수료인원_산정근거.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 148000, '공공지원분 312명을 빼고 941명으로 재집계', 'vitaedu-VE103', '남기훈', '사업기획실', '팀장', '2026-08-06 10:40:00'),
(8003, 8002, 1, 'projects/8001/files/8002/v1.pdf', '납세증명서_20260803.pdf', 'pdf', 'application/pdf', 138000, '유효기간이 신청기간을 덮는 발급본', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-08-03 11:26:00'),
(8004, 8003, 1, 'projects/8001/files/8003/v1.pdf', '표준재무제표증명_2025.pdf', 'pdf', 'application/pdf', 412000, '자기자본과 신용등급 확인용', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-08-03 11:20:00'),
(8005, 8004, 1, 'projects/8001/files/8004/v1.xlsx', '자막_검수결과서.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 96000, '정확성 오탈자 12건, 동기화 편차 2건 기록', 'vitaedu-VE107', '신재호', '콘텐츠개발팀', '대리', '2026-08-11 17:10:00'),
(8006, 8004, 2, 'projects/8001/files/8004/v2.xlsx', '자막_검수결과서.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 104000, '19차시 동기화 재작업분 반영. 7차시는 미반영', 'vitaedu-VE107', '신재호', '콘텐츠개발팀', '대리', '2026-08-14 18:05:00'),
(8007, 8005, 1, 'projects/8001/files/8005/v1.hwp', '훈련운영계획서_비타에듀_AI기초.hwp', 'hwp', 'application/x-hwp', 1840000, '초안. 실습과제 편성 비율 22퍼센트', 'vitaedu-VE101', '강태현', '사업기획실', '과장', '2026-08-12 18:40:00'),
(8008, 8005, 2, 'projects/8001/files/8005/v2.hwp', '훈련운영계획서_비타에듀_AI기초.hwp', 'hwp', 'application/x-hwp', 1920000, '실습과제 편성 비율을 35퍼센트로 올림', 'vitaedu-VE101', '강태현', '사업기획실', '과장', '2026-08-14 11:20:00'),
(8009, 8005, 3, 'projects/8001/files/8005/v3.hwp', '훈련운영계획서_비타에듀_AI기초.hwp', 'hwp', 'application/x-hwp', 2010000, '참여인력 4명 경력증명을 상세 직무가 보이는 판으로 교체', 'vitaedu-VE102', '윤하람', '사업기획실', '사원', '2026-08-15 16:05:00'),
(8010, 8005, 4, 'projects/8001/files/8005/v4.hwp', '훈련운영계획서_비타에듀_AI기초.hwp', 'hwp', 'application/x-hwp', 2064000, '자막 품질 자체점검 결과를 기본심사 항목에 반영', 'vitaedu-VE101', '강태현', '사업기획실', '과장', '2026-08-16 09:30:00'),
(8011, 8006, 1, 'projects/8001/files/8006/v1.hwp', '교수학습설계_상세.hwp', 'hwp', 'application/x-hwp', 620000, '학습지원과 상호작용 시점을 차시 단위로 정리', 'vitaedu-VE106', '오세아', '콘텐츠개발팀', '팀장', '2026-08-13 15:00:00'),
(8012, 8007, 1, 'projects/8001/files/8007/v1.hwp', '참여인력_운영관리계획.hwp', 'hwp', 'application/x-hwp', 410000, '대체인력 운영 방안 포함', 'vitaedu-VE104', '배규리', '교육운영팀', '대리', '2026-08-13 16:30:00'),
(8013, 8008, 1, 'projects/8001/files/8008/v1.xlsx', '훈련과정개요서_AI기초.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 210000, '40차시 입력. 학습소요시간 산정 사유는 비어 있음', 'vitaedu-VE102', '윤하람', '사업기획실', '사원', '2026-08-12 14:00:00'),
(8014, 8008, 2, 'projects/8001/files/8008/v2.xlsx', '훈련과정개요서_AI기초.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 246000, '차시별 산정 사유 작성. J열 합계 38시간으로 어긋남', 'vitaedu-VE102', '윤하람', '사업기획실', '사원', '2026-08-14 17:40:00'),
(8015, 8008, 3, 'projects/8001/files/8008/v3.xlsx', '훈련과정개요서_AI기초.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 251000, 'J열 합계를 40시간으로 맞춤. 25분 미만 차시 2건 재산정', 'vitaedu-VE102', '윤하람', '사업기획실', '사원', '2026-08-15 18:20:00'),
(8016, 8009, 1, 'projects/8001/files/8009/v1.xlsx', '수료생명단_2025_2026.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 388000, '수료 941명 명단. 성명 마스킹 전', 'vitaedu-VE104', '배규리', '교육운영팀', '대리', '2026-08-05 11:00:00'),
(8017, 8009, 2, 'projects/8001/files/8009/v2.xlsx', '수료생명단_2025_2026.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 388000, '성명을 김** 형식으로 마스킹', 'vitaedu-VE104', '배규리', '교육운영팀', '대리', '2026-08-06 09:20:00'),
(8018, 8010, 1, 'projects/8001/files/8010/v1.xlsx', '참여인력_증빙목록.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 74000, '9명 중 6명 증빙 수령. 외부 교·강사 3명 미수령', 'vitaedu-VE102', '윤하람', '사업기획실', '사원', '2026-08-14 10:10:00'),
(8019, 8011, 1, 'projects/8001/files/8011/v1.hwp', '콘텐츠_자체개발확인서.hwp', 'hwp', 'application/x-hwp', 168000, '최종안. 기여율 100퍼센트로 단독개발 기재', 'vitaedu-VE102', '윤하람', '사업기획실', '사원', '2026-08-16 10:00:00'),
(8020, 8012, 1, 'projects/8001/files/8012/v1.hwp', '서약서.hwp', 'hwp', 'application/x-hwp', 92000, '최종안', 'vitaedu-VE102', '윤하람', '사업기획실', '사원', '2026-08-16 10:05:00'),
(8021, 8013, 1, 'projects/8001/files/8013/v1.hwp', '개인정보_수집이용동의서.hwp', 'hwp', 'application/x-hwp', 118000, '참여인력 9명분 서식', 'vitaedu-VE102', '윤하람', '사업기획실', '사원', '2026-08-16 10:08:00'),
(8022, 8014, 1, 'projects/8001/files/8014/v1.hwp', '협약서_비타웨어.hwp', 'hwp', 'application/x-hwp', 174000, '최종안. 참여기관 역할은 실습과제 검토와 현업 사례 제공', 'vitaedu-VE101', '강태현', '사업기획실', '과장', '2026-08-14 15:20:00'),
(8023, 8014, 2, 'projects/8001/files/8014/v2.hwp', '협약서_비타웨어.hwp', 'hwp', 'application/x-hwp', 2240000, '양측 날인본. 승인 뒤 스캔해 올림', 'vitaedu-VE101', '강태현', '사업기획실', '과장', '2026-08-16 11:40:00'),
(8024, 8015, 1, 'projects/8001/files/8015/v1.pdf', '콘텐츠개발_계약서_러닝브릿지.pdf', 'pdf', 'application/pdf', 480000, '최종안. 저작재산권 전부 양도 조항 포함', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-07-06 14:00:00'),
(8025, 8015, 2, 'projects/8001/files/8015/v2.pdf', '콘텐츠개발_계약서_러닝브릿지.pdf', 'pdf', 'application/pdf', 1820000, '양측 날인본', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-07-08 16:10:00'),
(8026, 8016, 1, 'projects/8001/files/8016/v1.pdf', '자막제작_계약서_보이스텍스트.pdf', 'pdf', 'application/pdf', 1240000, '날인본. 잔금은 검수 통과 후 지급', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-07-08 16:20:00'),
(8027, 8017, 1, 'projects/8001/files/8017/v1.pdf', '촬영용역_계약서_스튜디오원.pdf', 'pdf', 'application/pdf', 980000, '날인본', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-07-08 16:30:00'),
(8028, 8018, 1, 'projects/8001/files/8018/v1.xlsx', '견적비교표_콘텐츠개발_3사.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 88000, '차시 단가와 납기, 저작권 조항을 나란히 비교', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-06-29 11:00:00'),
(8029, 8019, 1, 'projects/8001/files/8019/v1.pdf', '거래명세서_20260710.pdf', 'pdf', 'application/pdf', 210000, '선금 청구분', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-07-10 15:00:00'),
(8030, 8020, 1, 'projects/8001/files/8020/v1.pdf', '거래명세서_20260810.pdf', 'pdf', 'application/pdf', 214000, '중도금 청구분. 20차시 납품 확인 후', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-08-10 11:30:00'),
(8031, 8021, 1, 'projects/8001/files/8021/v1.pdf', '검수결과서_1차_20차시.pdf', 'pdf', 'application/pdf', 640000, '20차시 중 14건 반려 후 재납품분 통과', 'vitaedu-VE107', '신재호', '콘텐츠개발팀', '대리', '2026-08-07 17:00:00'),
(8032, 8022, 1, 'projects/8001/files/8022/v1.pdf', '저작권양도확인서_러닝브릿지.pdf', 'pdf', 'application/pdf', 320000, '자체개발 확인서의 근거 서류', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-08-10 11:35:00'),
(8033, 8023, 1, 'projects/8002/files/8023/v1.xlsx', '수료자명단_1기.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 268000, '수료 312명. 진도율과 평가 점수 병기', 'vitaedu-VE104', '배규리', '교육운영팀', '대리', '2026-03-27 16:00:00'),
(8034, 8024, 1, 'projects/8002/files/8024/v1.pdf', '만족도조사결과_1기.pdf', 'pdf', 'application/pdf', 720000, '만족도 86.4점. 문항별 분포 포함', 'vitaedu-VE104', '배규리', '교육운영팀', '대리', '2026-03-27 16:10:00'),
(8035, 8025, 1, 'projects/8002/files/8025/v1.pdf', '훈련비청구서_1차_202604.pdf', 'pdf', 'application/pdf', 186000, '수료 312명 기준', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-04-05 10:00:00'),
(8036, 8026, 1, 'projects/8002/files/8026/v1.pdf', '훈련비청구서_2차_202606.pdf', 'pdf', 'application/pdf', 184000, '수료 286명 기준', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-06-05 10:00:00'),
(8037, 8027, 1, 'projects/8002/files/8027/v1.pdf', '훈련비청구서_3차_202608.pdf', 'pdf', 'application/pdf', 188000, '수료 341명 기준. 입금 예정 8월 25일', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-08-05 10:00:00'),
(8038, 8028, 1, 'projects/8002/files/8028/v1.xlsx', '정산대조표_1차_2차.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 132000, '1차와 2차 입금액을 청구액과 대조', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-06-16 14:00:00'),
(8039, 8028, 2, 'projects/8002/files/8028/v2.xlsx', '정산대조표_1차_2차.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 141000, '중도이탈 10명 환수 조정분 1,742,400 반영', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-08-13 09:40:00'),
(8040, 8029, 1, 'projects/8002/files/8029/v1.xlsx', '환수조정내역_202608.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 78000, '소급 확인된 중도이탈자 10명 명단과 산출', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-08-13 09:45:00');

-- upload_status 는 기본값 UPLOADING 이다. 전건 COMPLETED 로 올린다.
-- ⚠️ 이걸 빼먹으면 파일이 업로드 중으로 뜨고 목록에서 열리지 않는다.
UPDATE file_version SET upload_status = 'COMPLETED'
 WHERE file_version_id BETWEEN 8001 AND 8040;


-- ── 3. 블록 연결 ────────────────────────────────────────────────────
-- ⚠️ FILE 블록은 어댑터가 없어 block.type_id 가 NULL 이다. 연결은 이 조인 테이블이 전부다.
INSERT IGNORE INTO block_file (block_id, file_id, linked_by, linked_at) VALUES
(8015, 8001, 'vitaedu-VE103', '2026-08-04 14:20:00'),
(8015, 8002, 'vitaedu-VE109', '2026-08-03 11:26:00'),
(8015, 8003, 'vitaedu-VE109', '2026-08-03 11:20:00'),
(8058, 8004, 'vitaedu-VE107', '2026-08-11 17:10:00'),
(8073, 8005, 'vitaedu-VE101', '2026-08-12 18:40:00'),
(8073, 8006, 'vitaedu-VE106', '2026-08-13 15:00:00'),
(8073, 8007, 'vitaedu-VE104', '2026-08-13 16:30:00'),
(8082, 8008, 'vitaedu-VE102', '2026-08-12 14:00:00'),
(8082, 8009, 'vitaedu-VE104', '2026-08-05 11:00:00'),
(8082, 8010, 'vitaedu-VE102', '2026-08-14 10:10:00'),
(8091, 8011, 'vitaedu-VE102', '2026-08-16 10:00:00'),
(8091, 8012, 'vitaedu-VE102', '2026-08-16 10:05:00'),
(8091, 8013, 'vitaedu-VE102', '2026-08-16 10:08:00'),
(8091, 8014, 'vitaedu-VE101', '2026-08-14 15:20:00'),
(8107, 8015, 'vitaedu-VE109', '2026-07-06 14:00:00'),
(8107, 8016, 'vitaedu-VE109', '2026-07-08 16:20:00'),
(8107, 8017, 'vitaedu-VE109', '2026-07-08 16:30:00'),
(8107, 8018, 'vitaedu-VE109', '2026-06-29 11:00:00'),
(8116, 8019, 'vitaedu-VE109', '2026-07-10 15:00:00'),
(8116, 8020, 'vitaedu-VE109', '2026-08-10 11:30:00'),
(8116, 8021, 'vitaedu-VE107', '2026-08-07 17:00:00'),
(8116, 8022, 'vitaedu-VE109', '2026-08-10 11:35:00'),
(8168, 8023, 'vitaedu-VE104', '2026-03-27 16:00:00'),
(8168, 8024, 'vitaedu-VE104', '2026-03-27 16:10:00'),
(8193, 8025, 'vitaedu-VE109', '2026-04-05 10:00:00'),
(8193, 8026, 'vitaedu-VE109', '2026-06-05 10:00:00'),
(8193, 8027, 'vitaedu-VE109', '2026-08-05 10:00:00'),
(8193, 8028, 'vitaedu-VE109', '2026-06-16 14:00:00'),
(8193, 8029, 'vitaedu-VE109', '2026-08-13 09:45:00');


-- =====================================================================
-- 검증 — 전부 0행이어야 한다
-- =====================================================================
-- 1) version_no 가 1부터 연속인가
--    SELECT file_id FROM file_version
--    WHERE file_id BETWEEN 8001 AND 8029
--    GROUP BY file_id HAVING COUNT(*) <> MAX(version_no) OR MIN(version_no) <> 1;
--
-- 2) comment 가 비어 있거나 줄표가 섞였나
--    SELECT file_version_id FROM file_version
--    WHERE file_version_id BETWEEN 8001 AND 8040
--      AND (comment IS NULL OR comment = '' OR comment LIKE '%—%');
--
-- 3) 업로드 상태가 남아 있나
--    SELECT file_version_id FROM file_version
--    WHERE file_version_id BETWEEN 8001 AND 8040 AND upload_status <> 'COMPLETED';
--
-- 4) 파일이 안 붙은 FILE 블록 — 미도래 스텝 3건(8126·8136·8194)만 나와야 정상
--    SELECT b.block_id, b.step_id, b.title FROM block b
--    LEFT JOIN block_file bf ON bf.block_id = b.block_id
--    WHERE b.type = 'FILE' AND b.block_id BETWEEN 8001 AND 8244 AND bf.file_id IS NULL;
