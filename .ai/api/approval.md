# 📝 Approval API — 결재 블록

**상태**: `✅ 확정` — 노션 반영 완료. 이탈 금지 규칙 전면 적용 (`../API.md` §0)
**최종 업데이트**: 2026-08-04 · **담당**: 이강욱
**노션**: 확인 필요 — 노션 링크 채워넣기
**범위**: 결재 블록 관련 API 7개(8 엔드포인트)만 담는다. 결재관리·처리 API 5개, 알림 API는 그 차례가 오면 별도로 추가한다.

> ✅ **노션 반영 완료 — 구현 가능.** 경로·필드명·타입·상태코드·에러코드를 **한 글자도 바꾸지 않는다** (`../API.md` §0).
> 변경이 필요하면 코드를 고치지 말고 **노션을 먼저 고친 뒤** 이 사본을 맞춘다.
> 요구사항 근거: [`../docs/domain/결재·알림/APR-V1.md`](../docs/domain/결재·알림/APR-V1.md)

## 엔드포인트

| # | API명칭 | METHOD | URL | 권한 |
|---|---|---|---|---|
| 1 | 결재 블록 생성 | POST | `/api/v1/blocks/{blockId}/approval` | 인증 사용자(프로젝트 member) |
| 2 | 결재 회차 상세조회 | GET | `/api/v1/approvals/{approvalId}/revisions/{revisionId}` | 기안자·ACTIVE 이상 결재자·MASTER |
| 3 | 결재 제목·내용 수정 | PATCH | `/api/v1/approvals/{approvalId}/revisions/{revisionId}` | 기안자 본인 |
| 4 | 결재 문서 추가 | POST | `/api/v1/approvals/{approvalId}/revisions/{revisionId}/documents` | 기안자 본인 |
| 5 | 결재 문서 제거 | DELETE | `/api/v1/approvals/{approvalId}/revisions/{revisionId}/documents/{documentId}` | 기안자 본인 |
| 6 | 결재선 등록·수정 | PUT | `/api/v1/approvals/{approvalId}/revisions/{revisionId}/lines` | 기안자 본인 |
| 7 | 결재 상신 | POST | `/api/v1/approvals/{approvalId}/revisions/{revisionId}/submit` | 기안자 본인 |
| 8 | 재상신 회차 생성 | POST | `/api/v1/approvals/{approvalId}/revisions` | 기안자 본인 |

⭐ **`userId` 계열은 전부 `String`이다.** `employee.user_id`(사번)가 `VARCHAR(20)`이라, `drafterId`/`approverId` 등을 `long`으로 두면 안 된다.
⭐ **`block` 행은 결재가 만들지 않는다.** 블록팀이 먼저 만든 `blockId`를 받아 1번 API가 `approval`+`approval_revision`(1회차)만 붙인다.
⭐ **부서·직책은 스냅샷 없이 항상 `employee` 라이브 조회다** (`APR-V1.md` INV-11). `approval_line`에 관련 컬럼 없음.

---

## 1. 결재 블록 생성

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/blocks/{blockId}/approval` |
| 인증 필요 | Y · 해당 프로젝트 member |
| 요구사항 | APR-001 · APR-001-1 · BND-001 |

⛔ **`block` 행을 생성하지 않는다.** 블록은 이미 존재해야 하며, 없으면 404다.

**Request**

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|:---:|---|
| `blockId` | Path | long | Y | 결재 상세를 붙일 블록 구분 번호 |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.blockId` | long | 요청받은 블록 구분 번호(그대로 반환) |
| `data.approvalId` | long | 생성된 결재 구분 번호(`blockId`와 다른 값) |
| `data.revisionId` | long | 생성된 1회차 상신 구분 번호 |
| `data.revisionNo` | int | 항상 `1` |
| `data.status` | String | `DRAFT` |

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 201 | Created | – | 결재 상세 생성 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 로그인이 필요합니다 |
| 404 | Not Found | `BLOCK_NOT_FOUND` | 블록을 찾을 수 없음 |
| 400 | Bad Request | `BLOCK_TYPE_MISMATCH` | 블록의 `type != APPROVAL` |
| 403 | Forbidden | `APPROVAL_NOT_PROJECT_MEMBER` | 프로젝트 member 아님 |

**비즈니스 규칙**: 요청자가 자동으로 기안자(`approval.user_id`)가 됨 · 생성 직후엔 결재자가 없어 알림 없음(`APR-004`).

---

## 2. 결재 회차 상세조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/approvals/{approvalId}/revisions/{revisionId}` |
| 인증 필요 | Y · 기안자·해당 회차 ACTIVE 이상 결재자(과거 이력 포함)·MASTER |
| 요구사항 | MGT-005 |

**Request**

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|:---:|---|
| `approvalId` | Path | long | Y | 결재 구분 번호 |
| `revisionId` | Path | long | Y | 상신 회차 구분 번호 |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.revisionId` | long | 상신 회차 구분 번호 |
| `data.revisionNo` | int | 상신 회차 번호 |
| `data.title` / `data.content` | String | 결재 제목·내용 |
| `data.drafterId` | **String** | 기안자 구분 번호(사번) |
| `data.drafterName` / `drafterDepartment` / `drafterPosition` | String | 기안자 이름·부서·직책(라이브 조회) |
| `data.status` | String | 회차 상태 |
| `data.submittedAt` / `finishedAt` | String | 상신·종료 일시 |
| `data.documents[]` | Array | `documentId`/`fileVersionId`/`fileName`/`fileSize`/`uploadedAt` |
| `data.lines[]` | Array | `lineId`/`order`/`approverId`(**String**)/`approverName`/`approverPosition`/`approverDepartment`/`status`/`opinion`/`processedAt` |

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 404 | Not Found | `APPROVAL_NOT_FOUND` | 결재 없음 |
| 404 | Not Found | `APPROVAL_REVISION_NOT_FOUND` | 회차 없음 |
| 403 | Forbidden | `APPROVAL_LINE_NOT_VIEWABLE` | 차례 안 온 결재자(`WAITING`)의 조회 |

**비즈니스 규칙**: `WAITING` 결재자는 조회 불가 · `MASTER`는 차례와 무관하게 전부 조회 가능(`MGT-005`).

---

## 3. 결재 제목·내용 수정

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/approvals/{approvalId}/revisions/{revisionId}` |
| 인증 필요 | Y · 기안자 본인 |
| 요구사항 | APR-002 |

**Request**

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|:---:|---|
| `approvalId` / `revisionId` | Path | long | Y | |
| `title` / `content` | Body | String | N | 하나만 보내도 됨(부분 수정) |

**Response** — `data.revisionId` / `title` / `content` / `updatedAt`

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 수정 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 404 | Not Found | `APPROVAL_NOT_FOUND` / `APPROVAL_REVISION_NOT_FOUND` | |
| 403 | Forbidden | `APPROVAL_NOT_DRAFTER` | 기안자 아님 |
| 409 | Conflict | `APPROVAL_REVISION_NOT_DRAFT` | `DRAFT` 아닌 회차 수정 시도 |

---

## 4. 결재 문서 추가

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/approvals/{approvalId}/revisions/{revisionId}/documents` |
| 인증 필요 | Y · 기안자 본인 |
| 요구사항 | APR-005 · APR-006 |

⛔ **실제 파일 업로드는 공용 파일 API 소관.** 이 API는 업로드 완료된 `fileVersionId`를 연결만 한다.

**Request**

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|:---:|---|
| `approvalId` / `revisionId` | Path | long | Y | |
| `fileVersionId` | Body | long | Y | 업로드 완료된 파일 버전 구분 번호 |

**Response** — `data.documentId` / `fileVersionId` / `fileName` / `fileSize` / `uploadedAt`

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 201 | Created | – | 추가 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 404 | Not Found | `APPROVAL_NOT_FOUND` / `APPROVAL_REVISION_NOT_FOUND` | |
| 404 | Not Found | `FILE_VERSION_NOT_FOUND` | 존재하지 않는 파일 버전 |
| 403 | Forbidden | `APPROVAL_NOT_DRAFTER` | |
| 409 | Conflict | `APPROVAL_REVISION_NOT_DRAFT` | |
| 409 | Conflict | `FILE_VERSION_NOT_READY` | `upload_status != COMPLETED` |
| 409 | Conflict | `DOCUMENT_ALREADY_LINKED` | 동일 회차에 동일 `fileVersionId` 중복 연결 |

---

## 5. 결재 문서 제거

| 항목 | 내용 |
|------|------|
| Method · URL | `DELETE /api/v1/approvals/{approvalId}/revisions/{revisionId}/documents/{documentId}` |
| 인증 필요 | Y · 기안자 본인 |
| 요구사항 | APR-007 |

**Response** — `204 No Content`, `data: null`

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 204 | No Content | – | 제거 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 404 | Not Found | `APPROVAL_DOCUMENT_NOT_FOUND` | |
| 403 | Forbidden | `APPROVAL_NOT_DRAFTER` | |
| 409 | Conflict | `APPROVAL_REVISION_NOT_DRAFT` | |

**비즈니스 규칙**: 하드 삭제(이력 보존 대상 아님, `APR-007`).

---

## 6. 결재선 등록·수정

| 항목 | 내용 |
|------|------|
| Method · URL | `PUT /api/v1/approvals/{approvalId}/revisions/{revisionId}/lines` |
| 인증 필요 | Y · 기안자 본인 |
| 요구사항 | APR-009~014 |

⛔ **요청 바디는 `approverId`(사번)만 받는다.** 이름·직책·부서는 클라이언트가 보내지 않는다 — 이름 검색으로 선택.

**Request**

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|:---:|---|
| `approvalId` / `revisionId` | Path | long | Y | |
| `lines` | Body | Array | Y | 기존 전체를 이 목록으로 치환 |
| `lines[].approverId` | Body | **String** | Y | 결재자 구분 번호(사번) |
| `lines[].order` | Body | int | Y | 결재 순서(1부터 연속) |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.lines[].lineId` | long | |
| `data.lines[].approverId` | **String** | |
| `data.lines[].approverName` / `approverPosition` / `approverDepartment` | String | 라이브 조회(INV-11) |
| `data.lines[].order` | int | |

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 등록·수정 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 404 | Not Found | `APPROVAL_NOT_FOUND` / `APPROVAL_REVISION_NOT_FOUND` | |
| 403 | Forbidden | `APPROVAL_NOT_DRAFTER` | |
| 409 | Conflict | `APPROVAL_REVISION_NOT_DRAFT` | |
| 400 | Bad Request | `APPROVAL_LINE_EMPTY` | 결재자 0명 |
| 400 | Bad Request | `APPROVAL_LINE_ORDER_INVALID` | 순서 중복/누락 |
| 400 | Bad Request | `APPROVAL_LINE_APPROVER_NOT_MEMBER` | 일반 결재자가 project member 아님(`MASTER`는 제외, `APR-012`) |

**비즈니스 규칙**: 전체 치환(기존 삭제 후 재삽입) · `MASTER`는 순번 위치·member 여부 무관하게 지정 가능.

---

## 7. 결재 상신

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/approvals/{approvalId}/revisions/{revisionId}/submit` |
| 인증 필요 | Y · 기안자 본인 |
| 요구사항 | SUB-001~004 |

**Request** — `approvalId` / `revisionId` (Path, long, Y)

**Response** — `data.approvalId` / `revisionId` / `revisionNo` / `status`(`IN_PROGRESS`) / `submittedAt` / `firstActiveLineId`

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 상신 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 404 | Not Found | `APPROVAL_NOT_FOUND` / `APPROVAL_REVISION_NOT_FOUND` | |
| 403 | Forbidden | `APPROVAL_NOT_DRAFTER` | |
| 409 | Conflict | `APPROVAL_REVISION_NOT_DRAFT` | 이미 상신됐거나 `DRAFT` 아님(중복 상신 포함) |
| 400 | Bad Request | `APPROVAL_CONTENT_REQUIRED` | 제목/내용 누락 |
| 400 | Bad Request | `APPROVAL_DOCUMENT_REQUIRED` | 문서 0건 |
| 400 | Bad Request | `APPROVAL_LINE_EMPTY` | 결재자 0명 |
| 400 | Bad Request | `APPROVAL_LINE_ORDER_INVALID` | |
| 400 | Bad Request | `APPROVAL_LINE_APPROVER_NOT_MEMBER` | |

**비즈니스 규칙**: 상태 전이 — `revision` DRAFT→IN_PROGRESS, 1번 결재선 ACTIVE·나머지 WAITING, `approval`도 IN_PROGRESS(`SUB-002`) · 최초/재상신 겸용(`revisionNo`로만 구분, 로직 동일) · 첫 ACTIVE 결재자에게만 알림 발행(`SUB-003`) · 동시성은 `SELECT ... FOR UPDATE` + 상태 재확인으로 차단(`INV-07`).

---

## 8. 재상신 회차 생성

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/approvals/{approvalId}/revisions` |
| 인증 필요 | Y · 기안자 본인 |
| 요구사항 | SUB-005~009 |

**Request** — `approvalId` (Path, long, Y)

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.revisionId` / `revisionNo` | long/int | 새(또는 기존) DRAFT 회차 |
| `data.status` | String | `DRAFT` |
| `data.copiedFromRevisionNo` | int | 복사 원본 회차 번호 |
| `data.title` / `content` | String | 이전 회차에서 복사 |
| `data.documents[]` | Array | 이전 회차에서 복사 |
| `data.lines[]` | Array | 반려자부터 재구성, 전부 `DRAFT`. `approverId`는 **String** |

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 201 | Created | – | 새 회차 생성 |
| 200 | OK | – | 이미 있는 DRAFT 회차 그대로 반환(멱등) |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 404 | Not Found | `APPROVAL_NOT_FOUND` | |
| 403 | Forbidden | `APPROVAL_NOT_DRAFTER` | |
| 409 | Conflict | `APPROVAL_NOT_REJECTED` | `approval.status != REJECTED` |

**비즈니스 규칙**: `approval.status == REJECTED`일 때만 생성 · 이전 회차 원본은 수정 안 함(이력 보존) · 반려자부터의 단계만 order 재부여 · 실제 상신은 별도로 7번 API 호출.
