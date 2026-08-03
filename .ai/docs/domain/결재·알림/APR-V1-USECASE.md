# 📁 결재(Approval) v1 — 유스케이스 시나리오

**최종 업데이트**: 2026-08-03 (신설)
**담당**: 이강욱

> 테이블 단위로 시나리오를 나눈다. `시스템` 은 사용자 조작 없이 서버가 수행하는 단계다.

---

## **1. approval**

| **유스케이스 시나리오 ID** | **시나리오** | **사용자** |
| --- | --- | --- |
| USC-APR-001 | 결재 블록 생성 시 approval(DRAFT) 동시 생성 | 시스템 |
| USC-APR-002 | 기안자 자동 지정 | 시스템 |
| USC-APR-003 | 전체 상태 전이 (DRAFT → IN_PROGRESS → REJECTED/APPROVED) | 시스템 |
| USC-APR-004 | 현재 회차 번호(`current_revision_no`) 갱신 | 시스템 |
| USC-APR-005 | 최종 완료 일시(`completed_at`) 기록 | 시스템 |
| USC-APR-006 | 결재 목록 조회 — 내가 기안한 것 | 기안자 |
| USC-APR-007 | 결재 목록 조회 — 내가 처리할 것 | ACTIVE 결재자 |
| USC-APR-008 | 결재 목록 조회 — 전체 | MASTER |
| USC-APR-009 | 목록 필터 (상태·기간·제목·프로젝트명) | 전체 사용자 |

---

## **2. approval_revision**

| **유스케이스 시나리오 ID** | **시나리오** | **사용자** |
| --- | --- | --- |
| USC-REV-001 | 최초 회차(1회차, DRAFT) 생성 | 시스템 |
| USC-REV-002 | 제목·내용 작성·수정 | 기안자 |
| USC-REV-003 | DRAFT 상태에서만 수정 가능 검증 | 시스템 |
| USC-REV-004 | 상신 — 상태 전이(DRAFT → IN_PROGRESS) | 기안자 |
| USC-REV-005 | 상신 시점 제목·내용·문서·결재선 확정 | 시스템 |
| USC-REV-006 | 상신 일시(`submitted_at`) 기록 | 시스템 |
| USC-REV-007 | 반려 — 상태 전이(IN_PROGRESS → REJECTED) | 시스템 |
| USC-REV-008 | 종료 일시(`finished_at`) 기록 | 시스템 |
| USC-REV-009 | 재상신 — 새 회차 생성(`revision_no + 1`) | 기안자 |
| USC-REV-010 | 이전 회차 제목·내용·문서 복사 | 시스템 |
| USC-REV-011 | 이전 회차 원본 미수정(이력 보존) | 시스템 |
| USC-REV-012 | 재상신 새 회차 생성 멱등 처리(기존 DRAFT 있으면 반환) | 시스템 |
| USC-REV-013 | 최종 승인 — 상태 전이(IN_PROGRESS → APPROVED) | 시스템 |
| USC-REV-014 | 회차 상세 조회(제목·내용·문서·결재선) | 기안자·참여 결재자·MASTER |
| USC-REV-015 | 회차 목록(이력) 조회, 현재/이전 구분 | 기안자·참여 결재자·MASTER |
| USC-REV-016 | WAITING 결재자 조회 차단 | 시스템 |

---

## **3. approval_line**

| **유스케이스 시나리오 ID** | **시나리오** | **사용자** |
| --- | --- | --- |
| USC-LIN-001 | 결재선 등록(전체 치환) | 기안자 |
| USC-LIN-002 | 결재자 1명 이상 검증 | 시스템 |
| USC-LIN-003 | 순서 중복·누락 검증 | 시스템 |
| USC-LIN-004 | 일반 결재자 project member 검증 | 시스템 |
| USC-LIN-005 | MASTER는 member 검증 제외 | 시스템 |
| USC-LIN-006 | 상신 — 1번 순번 ACTIVE, 나머지 WAITING 전환 | 시스템 |
| USC-LIN-007 | 승인 — 현재 단계 APPROVED, 다음 단계 ACTIVE 전환 | ACTIVE 결재자 |
| USC-LIN-008 | 승인 의견(선택) 기록 | ACTIVE 결재자 |
| USC-LIN-009 | 처리 일시(`processed_at`) 기록 | 시스템 |
| USC-LIN-010 | 반려 — 현재 단계 REJECTED 전환 | ACTIVE 결재자 |
| USC-LIN-011 | 반려 의견(선택) 기록 | ACTIVE 결재자 |
| USC-LIN-012 | 반려 시 이후 WAITING 단계 CANCELED 일괄 전환 | 시스템 |
| USC-LIN-013 | ACTIVE 아닌 단계의 승인·반려 차단 | 시스템 |
| USC-LIN-014 | 이미 처리된 단계의 중복 처리 차단 | 시스템 |
| USC-LIN-015 | 마지막 순번 승인 시 전체 결재 완료 판정 | 시스템 |
| USC-LIN-016 | 재상신 시 반려자부터만 결재선 재구성 | 시스템 |
| USC-LIN-017 | 재상신 시 반려자 이전(기승인) 단계 재생성 제외 | 시스템 |
| USC-LIN-018 | 동시 승인/반려 요청 잠금 처리 (`SELECT ... FOR UPDATE`) | 시스템 |

---

## **4. approval_document**

| **유스케이스 시나리오 ID** | **시나리오** | **사용자** |
| --- | --- | --- |
| USC-DOC-001 | 업로드 완료된 file_version을 결재 문서로 연결 | 기안자 |
| USC-DOC-002 | upload_status COMPLETED 검증 | 시스템 |
| USC-DOC-003 | 동일 회차 내 동일 file_version 중복 연결 차단 | 시스템 |
| USC-DOC-004 | 결재 문서 제거(하드 삭제) | 기안자 |
| USC-DOC-005 | DRAFT 상태에서만 추가·제거 가능 검증 | 시스템 |
| USC-DOC-006 | 상신 후 문서 변경 차단 | 시스템 |
| USC-DOC-007 | 재상신 시 이전 회차 문서 복사 | 시스템 |
| USC-DOC-008 | 문서 목록 조회(파일명·크기·업로드일시) | 기안자·참여 결재자·MASTER |

---

## **5. 알림 이벤트 발행 (결재 → 알림 도메인)**

| **유스케이스 시나리오 ID** | **시나리오** | **사용자** |
| --- | --- | --- |
| USC-EVT-001 | 최초 상신 시 첫 ACTIVE 결재자에게 이벤트 발행 | 시스템 |
| USC-EVT-002 | 중간 승인 시 다음 ACTIVE 결재자에게 이벤트 발행 | 시스템 |
| USC-EVT-003 | 이미 처리한 이전 결재자 제외 | 시스템 |
| USC-EVT-004 | 반려 시 기안자에게 이벤트 발행 | 시스템 |
| USC-EVT-005 | 재상신 시 이전 반려자에게 이벤트 발행 | 시스템 |
| USC-EVT-006 | 최종 승인 시 기안자에게 이벤트 발행 | 시스템 |
