-- =====================================================================
-- KB 15. 파일 21 · 버전 · 블록 연결
-- 무엇: FILE 블록에 붙는 산출물. 다버전은 제안서(v4)·가격제안서(v3)·계약서(v2) 3건.
-- 선행: 14_kb_blocks.sql · 후속: 18_kb_approval.sql(approval_document 가 file_version 을 문다)
-- ⚠️ comment 는 무엇이 바뀌었는지 적는다. uploader_name 은 스냅샷이라 NOT NULL.
-- 되돌리기: DELETE FROM block_file WHERE file_id BETWEEN 8030 AND 8050;
--           DELETE FROM file_version WHERE file_version_id BETWEEN 8041 AND %d;
--           DELETE FROM file WHERE file_id BETWEEN 8030 AND 8050;
-- =====================================================================


INSERT IGNORE INTO file (file_id, project_id, name, created_by, created_at) VALUES
(8030, 8011, '제안요청서_디지털.pdf', 'vitaedu-VE101', '2025-11-20 11:00:00'),
(8031, 8011, '사업자등록증.pdf', 'vitaedu-VE102', '2025-11-21 10:00:00'),
(8032, 8011, '국세지방세_완납증명.pdf', 'vitaedu-VE109', '2025-11-21 10:10:00'),
(8033, 8011, '디지털교육_운영실적_2023_2025.xlsx', 'vitaedu-VE104', '2025-11-21 14:00:00'),
(8034, 8011, 'KB디지털위탁교육_제안서.pdf', 'vitaedu-VE102', '2025-11-26 19:00:00'),
(8035, 8011, '제안서_요약본.pdf', 'vitaedu-VE103', '2025-11-27 15:00:00'),
(8036, 8011, '별지3_가격제안서_KB.xlsx', 'vitaedu-VE109', '2025-11-26 18:00:00'),
(8037, 8011, '별지1_재무상태비교표.hwp', 'vitaedu-VE109', '2025-11-27 16:00:00'),
(8038, 8011, '별지2_청렴계약이행확약서.hwp', 'vitaedu-VE102', '2025-11-27 16:10:00'),
(8039, 8011, '별지4_영업담당자_위임장.hwp', 'vitaedu-VE102', '2025-11-27 16:20:00'),
(8040, 8011, '별지5_개인정보동의서.hwp', 'vitaedu-VE102', '2025-11-27 16:30:00'),
(8041, 8011, '제출확인증.pdf', 'vitaedu-VE101', '2025-11-28 16:40:00'),
(8042, 8011, 'KB_발표자료.pdf', 'vitaedu-VE101', '2025-12-08 18:00:00'),
(8043, 8011, '우선협상대상_선정통보서.pdf', 'vitaedu-VE101', '2025-12-12 14:00:00'),
(8044, 8011, '위탁교육_계약서_KB.pdf', 'vitaedu-VE109', '2026-02-05 15:00:00'),
(8045, 8011, '외주계약서_강사_콘텐츠_LMS.pdf', 'vitaedu-VE109', '2026-02-19 15:00:00'),
(8046, 8011, '외주_단가표.xlsx', 'vitaedu-VE109', '2026-02-18 11:00:00'),
(8047, 8011, 'KB위탁료_청구서_상반기.pdf', 'vitaedu-VE109', '2026-07-05 10:00:00'),
(8048, 8011, '정산대조표_상반기.xlsx', 'vitaedu-VE109', '2026-07-16 14:00:00'),
(8049, 8011, '세금계산서_모음_상반기.pdf', 'vitaedu-VE109', '2026-07-15 11:00:00'),
(8050, 8011, '이체확인증.pdf', 'vitaedu-VE109', '2026-07-10 15:00:00');


INSERT IGNORE INTO file_version
  (file_version_id, file_id, version_no, storage_key,
   original_file_name, extension, mime_type, size_bytes, comment,
   uploaded_by, uploader_name, uploader_department, uploader_position, completed_at) VALUES
(8041, 8030, 1, 'projects/8011/files/8030/v1.pdf', '제안요청서_디지털.pdf', 'pdf', 'application/pdf', 662528, 'KB 공고 붙임 원문', 'vitaedu-VE101', '강태현', '사업기획실', '과장', '2025-11-20 11:00:00'),
(8042, 8031, 1, 'projects/8011/files/8031/v1.pdf', '사업자등록증.pdf', 'pdf', 'application/pdf', 96000, '교육서비스업 명시분', 'vitaedu-VE102', '윤하람', '사업기획실', '사원', '2025-11-21 10:00:00'),
(8043, 8032, 1, 'projects/8011/files/8032/v1.pdf', '국세지방세_완납증명.pdf', 'pdf', 'application/pdf', 138000, '유효기간이 제출기간을 덮는 발급본', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2025-11-21 10:10:00'),
(8044, 8033, 1, 'projects/8011/files/8033/v1.xlsx', '디지털교육_운영실적_2023_2025.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 124000, '2023년부터 2025년까지 디지털 과정 운영실적', 'vitaedu-VE104', '배규리', '교육운영팀', '대리', '2025-11-21 14:00:00'),
(8045, 8034, 1, 'projects/8011/files/8034/v1.pdf', 'KB디지털위탁교육_제안서.pdf', 'pdf', 'application/pdf', 4820000, '초안. 여섯 항목 순서로 작성', 'vitaedu-VE102', '윤하람', '사업기획실', '사원', '2025-11-26 19:00:00'),
(8046, 8034, 2, 'projects/8011/files/8034/v2.pdf', 'KB디지털위탁교육_제안서.pdf', 'pdf', 'application/pdf', 4910000, '운영능력 항목에 VOC 대응 인력 현황 3명 추가', 'vitaedu-VE102', '윤하람', '사업기획실', '사원', '2025-11-27 10:30:00'),
(8047, 8034, 3, 'projects/8011/files/8034/v3.pdf', 'KB디지털위탁교육_제안서.pdf', 'pdf', 'application/pdf', 5010000, '기술수준 항목에 학습이력 API 연동 예시 보강', 'vitaedu-VE108', '임채린', '플랫폼팀', '과장', '2025-11-27 13:20:00'),
(8048, 8034, 4, 'projects/8011/files/8034/v4.pdf', 'KB디지털위탁교육_제안서.pdf', 'pdf', 'application/pdf', 5120000, '인력전문성 항목에 강사 풀 현황 보강. 최종 제출본', 'vitaedu-VE102', '윤하람', '사업기획실', '사원', '2025-11-28 09:00:00'),
(8049, 8035, 1, 'projects/8011/files/8035/v1.pdf', '제안서_요약본.pdf', 'pdf', 'application/pdf', 210000, 'A4 한 장 항목별 표', 'vitaedu-VE103', '남기훈', '사업기획실', '팀장', '2025-11-27 15:00:00'),
(8050, 8036, 1, 'projects/8011/files/8036/v1.xlsx', '별지3_가격제안서_KB.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 88000, 'STEP별 인원과 회차에 1인당 단가를 곱한 초안', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2025-11-26 18:00:00'),
(8051, 8036, 2, 'projects/8011/files/8036/v2.xlsx', '별지3_가격제안서_KB.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 94000, '환급과정 비율을 40퍼센트 이상으로 재편성', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2025-11-27 11:00:00'),
(8052, 8036, 3, 'projects/8011/files/8036/v3.xlsx', '별지3_가격제안서_KB.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 97000, '일반 사이버 50회차 평균단가 반영. 총액 예산 내 확정', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2025-11-27 17:30:00'),
(8053, 8037, 1, 'projects/8011/files/8037/v1.hwp', '별지1_재무상태비교표.hwp', 'hwp', 'application/x-hwp', 42000, '최근 3년 재무비율 계산분. 법인인감 날인', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2025-11-27 16:00:00'),
(8054, 8038, 1, 'projects/8011/files/8038/v1.hwp', '별지2_청렴계약이행확약서.hwp', 'hwp', 'application/x-hwp', 34000, '법인인감 날인본', 'vitaedu-VE102', '윤하람', '사업기획실', '사원', '2025-11-27 16:10:00'),
(8055, 8039, 1, 'projects/8011/files/8039/v1.hwp', '별지4_영업담당자_위임장.hwp', 'hwp', 'application/x-hwp', 37000, '정 강태현 부 윤하람 지정', 'vitaedu-VE102', '윤하람', '사업기획실', '사원', '2025-11-27 16:20:00'),
(8056, 8040, 1, 'projects/8011/files/8040/v1.hwp', '별지5_개인정보동의서.hwp', 'hwp', 'application/x-hwp', 32000, '영업담당자 정·부 작성분', 'vitaedu-VE102', '윤하람', '사업기획실', '사원', '2025-11-27 16:30:00'),
(8057, 8041, 1, 'projects/8011/files/8041/v1.pdf', '제출확인증.pdf', 'pdf', 'application/pdf', 88000, '방문접수 확인분', 'vitaedu-VE101', '강태현', '사업기획실', '과장', '2025-11-28 16:40:00'),
(8058, 8042, 1, 'projects/8011/files/8042/v1.pdf', 'KB_발표자료.pdf', 'pdf', 'application/pdf', 3200000, '15분 발표용. 커리큘럼과 API 연동 중심', 'vitaedu-VE101', '강태현', '사업기획실', '과장', '2025-12-08 18:00:00'),
(8059, 8043, 1, 'projects/8011/files/8043/v1.pdf', '우선협상대상_선정통보서.pdf', 'pdf', 'application/pdf', 96000, '상위 2개사 우선협상대상 통보', 'vitaedu-VE101', '강태현', '사업기획실', '과장', '2025-12-12 14:00:00'),
(8060, 8044, 1, 'projects/8011/files/8044/v1.pdf', '위탁교육_계약서_KB.pdf', 'pdf', 'application/pdf', 520000, '최종안. 위약은 사전 30일 통보로 수정. 저작권 공동귀속은 미반영', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-02-05 15:00:00'),
(8061, 8044, 2, 'projects/8011/files/8044/v2.pdf', '위탁교육_계약서_KB.pdf', 'pdf', 'application/pdf', 1840000, '양측 날인본. 승인 뒤 스캔', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-02-06 16:10:00'),
(8062, 8045, 1, 'projects/8011/files/8045/v1.pdf', '외주계약서_강사_콘텐츠_LMS.pdf', 'pdf', 'application/pdf', 1420000, '에듀멘토스·러닝브릿지·에듀플로우 3사 계약 날인본', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-02-19 15:00:00'),
(8063, 8046, 1, 'projects/8011/files/8046/v1.xlsx', '외주_단가표.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 76000, '업체별 견적 비교와 확정 단가', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-02-18 11:00:00'),
(8064, 8047, 1, 'projects/8011/files/8047/v1.pdf', 'KB위탁료_청구서_상반기.pdf', 'pdf', 'application/pdf', 192000, 'STEP I·II·사이버 회차 청구분', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-07-05 10:00:00'),
(8065, 8048, 1, 'projects/8011/files/8048/v1.xlsx', '정산대조표_상반기.xlsx', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 134000, '회차별 청구액과 입금액 대조. 중도 이탈자 조정 반영', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-07-16 14:00:00'),
(8066, 8049, 1, 'projects/8011/files/8049/v1.pdf', '세금계산서_모음_상반기.pdf', 'pdf', 'application/pdf', 420000, '매출 3건과 매입 4건 계산서', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-07-15 11:00:00'),
(8067, 8050, 1, 'projects/8011/files/8050/v1.pdf', '이체확인증.pdf', 'pdf', 'application/pdf', 210000, '강사료·콘텐츠 선금·LMS 선금 이체분', 'vitaedu-VE109', '하성민', '경영지원팀', '과장', '2026-07-10 15:00:00');

UPDATE file_version SET upload_status = 'COMPLETED' WHERE file_version_id BETWEEN 8041 AND 8067;


-- ⚠️ FILE 블록은 어댑터가 없어 block.type_id 가 NULL 이다. 연결은 이 조인 테이블이 전부다.
INSERT IGNORE INTO block_file (block_id, file_id, linked_by, linked_at) VALUES
(8305, 8030, 'vitaedu-VE101', '2025-11-20 11:00:00'),
(8311, 8031, 'vitaedu-VE102', '2025-11-21 10:00:00'),
(8311, 8032, 'vitaedu-VE109', '2025-11-21 10:10:00'),
(8311, 8033, 'vitaedu-VE104', '2025-11-21 14:00:00'),
(8338, 8034, 'vitaedu-VE102', '2025-11-26 19:00:00'),
(8345, 8035, 'vitaedu-VE103', '2025-11-27 15:00:00'),
(8352, 8036, 'vitaedu-VE109', '2025-11-26 18:00:00'),
(8360, 8037, 'vitaedu-VE109', '2025-11-27 16:00:00'),
(8360, 8038, 'vitaedu-VE102', '2025-11-27 16:10:00'),
(8360, 8039, 'vitaedu-VE102', '2025-11-27 16:20:00'),
(8360, 8040, 'vitaedu-VE102', '2025-11-27 16:30:00'),
(8367, 8041, 'vitaedu-VE101', '2025-11-28 16:40:00'),
(8374, 8042, 'vitaedu-VE101', '2025-12-08 18:00:00'),
(8374, 8043, 'vitaedu-VE101', '2025-12-12 14:00:00'),
(8381, 8044, 'vitaedu-VE109', '2026-02-05 15:00:00'),
(8400, 8045, 'vitaedu-VE109', '2026-02-19 15:00:00'),
(8400, 8046, 'vitaedu-VE109', '2026-02-18 11:00:00'),
(8411, 8047, 'vitaedu-VE109', '2026-07-05 10:00:00'),
(8411, 8048, 'vitaedu-VE109', '2026-07-16 14:00:00'),
(8419, 8049, 'vitaedu-VE109', '2026-07-15 11:00:00'),
(8419, 8050, 'vitaedu-VE109', '2026-07-10 15:00:00');

-- 참고: 제안서 file 8034 v4=8048 v1=8045 · 가격제안서 8036 v3=8052 · 계약서 8044 v1=8060
