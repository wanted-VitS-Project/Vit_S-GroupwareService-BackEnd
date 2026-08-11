# 📝 Approval API — 결재 블록

**상태**: `✅ 확정` — 노션 반영 완료. 이탈 금지 규칙 전면 적용 (`../API.md` §0)
**최종 업데이트**: 2026-08-10 (상세조회 `documents[].fileDeleted` 신설 · `APPROVAL_IN_PROGRESS` 에러코드 폐기 — 노션·프론트 공유 필요)
**최종 업데이트**: 2026-08-06 (§8 신설 — 결재관리 목록조회, `drafterId`/`approverId` 계열 원 명세 `long`→String 정정)
**최종 업데이트**: 2026-08-04 · **담당**: 이강욱
**노션**: 확인 필요 — 노션 링크 채워넣기
**범위**: 결재 블록 관련 API 7개 + 결재관리·처리 API(목록조회 착수, 나머지 4개는 순서대로 추가 예정). 알림 API는 `notification.md` 참고.

> ✅ **노션 반영 완료 — 구현 가능.** 경로·필드명·타입·상태코드·에러코드를 **한 글자도 바꾸지 않는다** (`../API.md` §0).
> 변경이 필요하면 코드를 고치지 말고 **노션을 먼저 고친 뒤** 이 사본을 맞춘다.
> 요구사항 근거: [`../docs/domain/결재·알림/APR-V1.md`](../docs/domain/결재·알림/APR-V1.md)

> ⚠️ **2026-08-04 — "결재 블록 생성" API 삭제됨.** 예전엔 `POST /api/v1/blocks/{blockId}/approval`이 있었는데,
> 실제 블록 생성 흐름을 확인해보니 블록팀이 `BlockCommandService.createBlock()` 안에서 `BlockDetailPort`라는
> 확장점(Java 인터페이스, REST 아님)을 통해 타입별 상세 행을 같은 트랜잭션에 만든다. 결재는 이 인터페이스의
> `ApprovalBlockDetailAdapter` 구현체로 참여할 뿐, 프론트가 별도로 호출하는 API가 아니다. Text·Checklist도
> 동일한 구조(자체 생성 API 없음)라 이 패턴이 맞다. 노션에서도 이 엔드포인트는 제거해야 한다.

> ⚠️ **2026-08-10 — 계약 변경 2건 (노션·프론트 공유 필요).**
>
> | 변경 | 내용 |
> |---|---|
> | 추가 | 회차 상세조회·결재 상세조회의 `data.documents[].fileDeleted`(boolean) — 원본 문서가 **휴지통에 있으면 `true`**. 이름·크기는 그대로 내려간다(증빙 이력이라 감추지 않는다). 팀 삭제 정책 `DELETE.md` D-6·DEL-010 근거. 문서 추가 응답에는 넣지 않았다(방금 올린 파일이라 항상 `false`) |
> | 폐기 | `APPROVAL_IN_PROGRESS`(409) — `BLK-008` 삭제 잠금 폐기로 사용처가 0이 됐다. **진행 중 결재도 블록과 함께 삭제되고 `CANCELED`로 종결된다.** 프론트에 이 409 분기가 있으면 제거해야 한다 |

## 엔드포인트

| # | API명칭 | METHOD | URL | 권한 |
|---|---|---|---|---|
| 1 | 결재 회차 상세조회 | GET | `/api/v1/approvals/{approvalId}/revisions/{revisionId}` | 기안자·ACTIVE 이상 결재자·MASTER |
| 2 | 결재 제목·내용 수정 | PATCH | `/api/v1/approvals/{approvalId}/revisions/{revisionId}` | 기안자 본인 |
| 3 | 결재 문서 추가 | POST | `/api/v1/approvals/{approvalId}/revisions/{revisionId}/documents` | 기안자 본인 |
| 4 | 결재 문서 제거 | DELETE | `/api/v1/approvals/{approvalId}/revisions/{revisionId}/documents/{documentId}` | 기안자 본인 |
| 5 | 결재선 등록·수정 | PUT | `/api/v1/approvals/{approvalId}/revisions/{revisionId}/lines` | 기안자 본인 |
| 6 | 결재 상신 | POST | `/api/v1/approvals/{approvalId}/revisions/{revisionId}/submit` | 기안자 본인 |
| 7 | 재상신 회차 생성 | POST | `/api/v1/approvals/{approvalId}/revisions` | 기안자 본인 |
| 8 | 결재관리 목록조회 | GET | `/api/v1/approvals` | 로그인 사용자(scope=all은 MASTER·ADMIN) |
| 9 | 결재 상세조회 | GET | `/api/v1/approvals/{approvalId}` | 기안자·ACTIVE 이상 결재자·MASTER |
| 10 | 결재 이력조회 | GET | `/api/v1/approvals/{approvalId}/revisions` | 기안자·이력 참여 결재자(ACTIVE 이상)·MASTER |
| 11 | 결재 승인 | POST | `/api/v1/approval-lines/{lineId}/approve` | 해당 결재선의 결재자 본인 |
| 12 | 결재 반려 | POST | `/api/v1/approval-lines/{lineId}/reject` | 해당 결재선의 결재자 본인 |

⭐ **`userId` 계열은 전부 `String`이다.** `employee.user_id`(사번)가 `VARCHAR(20)`이라, `drafterId`/`approverId` 등을 `long`으로 두면 안 된다.
⭐ **결재 상세 생성은 REST API가 아니다.** `ApprovalBlockDetailAdapter`(블록팀 `BlockDetailPort` 구현체)가 블록 생성 트랜잭션 안에서 `approval`+`approval_revision`(1회차)을 만든다.
⭐ **부서·직책은 스냅샷 없이 항상 `employee` 라이브 조회다** (`APR-V1.md` INV-11). `approval_line`에 관련 컬럼 없음.

---

## 1. 결재 회차 상세조회

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
| `data.documents[]` | Array | `documentId`/`fileVersionId`/`fileName`/`fileSize`/`uploadedAt`/`fileDeleted`(boolean) |
| `data.lines[]` | Array | `lineId`/`order`/`approverId`(**String**)/`approverName`/`approverPosition`/`approverDepartment`/`status`/`opinion`/`processedAt` |

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 404 | Not Found | `APPROVAL_NOT_FOUND` | 결재 없음 |
| 404 | Not Found | `APPROVAL_REVISION_NOT_FOUND` | 회차 없음 |
| 403 | Forbidden | `APPROVAL_LINE_NOT_VIEWABLE` | 차례 안 온 결재자(`WAITING`)의 조회 |

**비즈니스 규칙**: `WAITING` 결재자는 조회 불가 · `CANCELED`(반려로 절차 종결돼 건너뛴 결재선)는 조회 가능 · `MASTER`는 차례와 무관하게 전부 조회 가능(`MGT-005`).

---

## 2. 결재 제목·내용 수정

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

## 3. 결재 문서 추가

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

## 4. 결재 문서 제거

| 항목 | 내용 |
|------|------|
| Method · URL | `DELETE /api/v1/approvals/{approvalId}/revisions/{revisionId}/documents/{documentId}` |
| 인증 필요 | Y · 기안자 본인 |
| 요구사항 | APR-007 |

**Response** — `204 No Content` (응답 본문 없음)

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 204 | No Content | – | 제거 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 404 | Not Found | `APPROVAL_NOT_FOUND` / `APPROVAL_REVISION_NOT_FOUND` | |
| 404 | Not Found | `APPROVAL_DOCUMENT_NOT_FOUND` | |
| 403 | Forbidden | `APPROVAL_NOT_DRAFTER` | |
| 409 | Conflict | `APPROVAL_REVISION_NOT_DRAFT` | |

**비즈니스 규칙**: 논리 삭제 — `approval_document.deleted_at` 을 기록한다(`APR-007`).
같은 파일 버전을 다시 연결할 수 있다(중복 검사가 활성 행만 본다).
2026-08-10 하드 삭제에서 전환했다 — 팀 삭제 정책 `DELETE.md` D-1(실물은 전부 soft delete)·
D-2(하드는 「연결 행 7종」뿐, `approval_document` 는 UNIQUE·복합 PK 가 없어 미해당).

> 파일 영구삭제 잠금은 이 삭제를 상위 블록 삭제(`DEL-005`)와 **회차 생존으로 구분**한다 —
> 여기서 뺀 문서는 회차가 살아 있어 파일 잠금을 풀고, 상위 삭제는 회차도 삭제돼 잠금을 유지한다.

---

## 5. 결재선 등록·수정

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
| 400 | Bad Request | `APPROVAL_LINE_APPROVER_NOT_MEMBER` | 일반 결재자가 project member 아님(`MASTER`·`ADMIN`은 제외) |

**비즈니스 규칙**: 전체 치환(기존 삭제 후 재삽입) · `MASTER`·`ADMIN`은 project member 검증 제외.

---

## 6. 결재 상신

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
| 400 | Bad Request | `APPROVAL_CONTENT_REQUIRED` / `APPROVAL_DOCUMENT_REQUIRED` / `APPROVAL_LINE_EMPTY` / `APPROVAL_LINE_ORDER_INVALID` / `APPROVAL_LINE_APPROVER_NOT_MEMBER` | |

**비즈니스 규칙**: 상태 전이 — `revision` DRAFT→IN_PROGRESS, 1번 결재선 ACTIVE·나머지 WAITING, `approval`도 IN_PROGRESS(`SUB-002`) · 최초/재상신 겸용(`revisionNo`로만 구분, 로직 동일) · 첫 ACTIVE 결재자에게만 알림 발행(`SUB-003`) · 동시성은 `SELECT ... FOR UPDATE` + 상태 재확인으로 차단(`INV-07`).

---

## 7. 재상신 회차 생성

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

**비즈니스 규칙**: `approval.status == REJECTED`일 때만 생성 · 이전 회차 원본은 수정 안 함(이력 보존) · 반려자부터의 단계만 order 재부여 · 실제 상신은 별도로 6번 API 호출.

---

## 8. 결재관리 목록조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/approvals` |
| 인증 필요 | Y · `scope=all`은 `MASTER`·`ADMIN`만(`ApprovalViewPolicy.FULL_ACCESS_ROLES`와 동일 기준) |
| 요구사항 | MGT-001~004 |

> ⚠️ **2026-08-06 — 원 명세(노션/이강욱 전달분) 대비 정정.** `drafterId`/`approverId`/`currentApproverId`/`lines[].approverId`가
> 원문엔 `long`이었으나, 결재 도메인 전체(`Approval.drafterId`·`ApprovalLine.approverId`·`employee.user_id VARCHAR(20)`·
> 본 문서 6쪽 위 "userId 계열은 전부 String" 규칙)와 맞지 않아 **String(사번)으로 정정**했다. **노션·프론트에 공유 필요.**

**Request**

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|:---:|---|
| `scope` | Query | String | N | `drafted`(기본) / `pending` / `all` |
| `status` | Query | String | N | 결재 상태 필터(`DRAFT`/`IN_PROGRESS`/`REJECTED`/`COMPLETED`/`CANCELED`) |
| `drafterId` | Query | **String** | N | 기안자 필터(사번). `scope=drafted`에선 무시되고 요청자 본인으로 강제됨 |
| `approverId` | Query | **String** | N | 결재자 필터(사번) — 현재 회차 결재선 기준 |
| `fromDate` / `toDate` | Query | String | N | 조회 기간(`yyyy-MM-dd`, `created_at` 기준, inclusive) |
| `keyword` | Query | String | N | 결재 제목 또는 프로젝트명 검색어(둘 다 대상) |
| `revisionNo` | Query | int | N | 현재 회차 번호(`current_revision_no`) 필터 |
| `page` | Query | int | N | 페이지 번호(기본 0) |
| `size` | Query | int | N | 페이지 크기(기본 10) |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.content[].approvalId` | long | 결재 구분 번호 |
| `data.content[].title` | String | 현재 회차 제목 |
| `data.content[].status` | String | 결재 상태 |
| `data.content[].currentRevisionNo` | int | 현재 회차 번호 |
| `data.content[].drafterId` | **String** | 기안자 구분 번호(사번) |
| `data.content[].drafterName` | String | 기안자 이름 |
| `data.content[].currentApproverId` | **String** | 현재 `ACTIVE` 결재자 구분 번호(사번). 없으면 `null` |
| `data.content[].currentApproverName` | String | 현재 `ACTIVE` 결재자 이름 |
| `data.content[].projectId` / `projectName` | long / String | 소속 프로젝트 |
| `data.content[].stepId` / `stepName` | long / String | 소속 스텝 |
| `data.content[].lines[]` | Array | 현재 회차의 결재선 전체 미리보기(아바타 표시용) |
| `data.content[].lines[].approverId` | **String** | 결재자 구분 번호(사번) |
| `data.content[].lines[].approverName` | String | 결재자 이름 |
| `data.content[].lines[].order` | int | 결재 순서 |
| `data.content[].lines[].status` | String | 결재 단계 상태(`APPROVED`/`ACTIVE`/`WAITING`/`REJECTED`/`CANCELED`) |
| `data.content[].createdAt` / `submittedAt` / `completedAt` | String | 생성·상신·완료 일시(`submittedAt`/`completedAt`은 미도달 시 `null`) |
| `data.totalElements` / `totalPages` | int | 페이징 정보 |

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 403 | Forbidden | `APPROVAL_SCOPE_ALL_FORBIDDEN` | `MASTER`·`ADMIN`이 아닌 사용자의 `scope=all` 요청 |

**비즈니스 규칙**: `scope=drafted`(기본) — `drafterId`를 요청자 본인으로 강제 · `scope=pending` — 요청자가 현재 회차에서 `ACTIVE`인 결재만 조회 · `scope=all` — `MASTER`·`ADMIN`만 허용, 나머지 필터 자유 조합.

---

## 9. 결재 상세조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/approvals/{approvalId}` |
| 인증 필요 | Y · 기안자·현재 회차 ACTIVE 이상 결재자(과거 이력 포함)·MASTER(`ApprovalViewPolicy`, 1번과 동일 규칙) |
| 요구사항 | MGT-005~006 |

> ⚠️ **2026-08-06 정정** — `drafterId`는 원 명세(전달분) `long` → **String(사번)으로 정정**(8번과 동일 사유).
> **문서·결재선 세부 필드(`documents[]`/`lines[]` 내부 항목)가 원 명세에 명시되지 않아**, 1번(회차 상세조회)과
> 동일한 구조(`documentId`/`fileVersionId`/`fileName`/`fileSize`/`uploadedAt`/`fileDeleted`, `lineId`/`approverId`/`approverName`/
> `approverPosition`/`approverDepartment`/`order`/`status`/`opinion`/`processedAt`)를 그대로 재사용하는 것으로 판단해
> 반영했다(전달자 확인: "조회 권한은 1번과 동일" + "data = 1번 조회 응답 + drafter 정보 + blockOrigin"). **실제 프론트 요구와
> 다르면 알려달라 — 세부 필드는 추정치다.**
> ⚠️ **2026-08-06 추가 정정** — Figma 화면 검토 중 발견: "data = 1번 조회 응답 + drafter 정보"라고 해놓고
> `drafterDepartment`/`drafterPosition`을 Response 표에 안 옮겼었음(1번엔 이미 있는 필드). 화면에 기안자
> 부서·직책이 실제로 표시돼 있어 아래 표에 추가함.

**Request**

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|:---:|---|
| `approvalId` | Path | long | Y | 결재 구분 번호 |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.revisionId` | long | 현재 회차 구분 번호 |
| `data.revisionNo` | int | 현재 회차 번호 |
| `data.title` / `content` | String | 결재 제목·내용 |
| `data.drafterId` | **String** | 기안자 구분 번호(사번) |
| `data.drafterName` / `drafterDepartment` / `drafterPosition` | String | 기안자 이름·부서·직책(라이브 조회) |
| `data.status` | String | 회차 상태 |
| `data.documents[]` | Array | 1번과 동일 구조(추정, 위 참고) |
| `data.lines[]` | Array | 1번과 동일 구조(추정, 위 참고) |
| `data.blockOrigin.blockId` / `stepId` / `projectId` | long | 원본 블록·스텝·프로젝트 구분 번호 |

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 404 | Not Found | `APPROVAL_NOT_FOUND` | 결재 없음 |
| 403 | Forbidden | `APPROVAL_LINE_NOT_VIEWABLE` | 차례 안 온 결재자(`WAITING`)의 조회 |

**비즈니스 규칙**: 조회 권한은 1번(회차 상세조회)과 동일 · 항상 **현재 회차**(`approval.current_revision_no`)를 보여준다(회차 지정 불가 — 지정 조회는 1번 API) · `blockOrigin`으로 원본 블록·스텝·프로젝트 이동 정보 제공(MGT-006).

---

## 10. 결재 이력조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/approvals/{approvalId}/revisions` |
| 인증 필요 | Y · 기안자·이력에 참여한 결재자·MASTER(아래 참고) |
| 요구사항 | MGT-007 |

> ⚠️ **2026-08-06 판단** — 전달받은 문구는 "기안자, 해당 결재 이력에 참여한 결재자, master"로 `WAITING` 제외 여부가
> 명시돼 있지 않았다. `APR-V1.md`엔 MGT-007 전용 권한 규칙이 따로 없고 MGT-005("`WAITING` 결재자는 403")만 있어,
> **1번(회차 상세조회)과 동일한 기준(전체 회차 통틀어 `ACTIVE`/`APPROVED`/`REJECTED`/`CANCELED`로 한 번이라도 도달한
> 결재자만 허용, 순수 `WAITING`만 있는 사람은 403)을 그대로 적용**했다. 실제 의도와 다르면 알려달라.

**Request**

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|:---:|---|
| `approvalId` | Path | long | Y | 결재 구분 번호 |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.content[].revisionId` | long | 회차 구분 번호 |
| `data.content[].revisionNo` | int | 회차 번호 |
| `data.content[].status` | String | 회차 상태 |
| `data.content[].submittedAt` / `finishedAt` | String | 상신·종료 일시 |
| `data.content[].isCurrent` | boolean | 현재 진행 중인 회차 여부(`approval.current_revision_no`와 일치) |

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 404 | Not Found | `APPROVAL_NOT_FOUND` | 결재 없음 |
| 403 | Forbidden | `APPROVAL_LINE_NOT_VIEWABLE` | 이력 조회 권한 없음 |

**비즈니스 규칙**: 전체 회차를 `revisionNo` 오름차순으로 반환 · `isCurrent`로 진행 중/종료된 회차 구분 · 페이징 없음(회차 수가 적어 전량 반환).

---

## 11. 결재 승인

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/approval-lines/{lineId}/approve` |
| 인증 필요 | Y · 해당 결재선의 결재자 본인 |
| 요구사항 | PRC-001~004 |

> ⚠️ **2026-08-06 정정 3건(전달분 대비)**
> 1. **완료 상태값**: "회차·결재 전체가 `APPROVED`로 종료"라고 돼 있었으나 `ApprovalStatus` enum(DB `ENUM`)에
>    `APPROVED`가 없다(`DRAFT`/`IN_PROGRESS`/`REJECTED`/`COMPLETED`/`CANCELED`만 존재) — STATE.md에 이미 같은
>    혼동을 겪고 `COMPLETED`로 확정한 기록이 있다. **`COMPLETED`로 정정**(라인 자체의 `status`는 원문 그대로 `APPROVED` 맞음 — `ApprovalLineStatus`엔 `APPROVED`가 실제로 있음).
> 2. **`APPROVAL_LINE_NOT_ACTIVE` vs `APPROVAL_LINE_ALREADY_PROCESSED` 구분 기준 추가**: 원문엔 두 코드 다 "ACTIVE
>    아닐 때"로만 적혀 있어 겹쳐 보임 — `line.status=WAITING`(아직 차례 안 옴)이면 `NOT_ACTIVE`, `APPROVED`/`REJECTED`/
>    `CANCELED`(이미 종결)면 `ALREADY_PROCESSED`로 매핑했다.
> 3. **`lineId` 자체가 없는 경우 404가 명세에 없어서** `APPROVAL_LINE_FORBIDDEN`(403)으로 흡수 처리한다(리소스
>    존재 여부를 노출하지 않는 패턴). 실제 프론트 기대와 다르면 알려달라.

**Request**

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|:---:|---|
| `lineId` | Path | long | Y | 결재선 구분 번호 |
| `opinion` | Body | String | N | 승인 의견 |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.lineId` | long | 결재선 구분 번호 |
| `data.status` | String | 결재 단계 상태(`APPROVED`) |
| `data.processedAt` | String | 처리 일시 |
| `data.nextActiveLineId` | long | 다음 활성화된 결재선 구분 번호(없으면 `null`) |
| `data.approvalCompleted` | boolean | 이 승인으로 전체 결재가 완료됐는지 여부 |

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 승인 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 403 | Forbidden | `APPROVAL_LINE_FORBIDDEN` | 해당 결재선의 결재자가 아님(존재하지 않는 `lineId` 포함) |
| 409 | Conflict | `APPROVAL_LINE_NOT_ACTIVE` | `WAITING` 등 아직 차례가 안 온 결재선 |
| 409 | Conflict | `APPROVAL_LINE_ALREADY_PROCESSED` | 이미 처리된(`APPROVED`/`REJECTED`/`CANCELED`) 결재선의 중복 처리 시도 |

**비즈니스 규칙**: 결재선 row를 잠근 뒤 `ACTIVE` 상태를 재확인하고 처리(동시 요청·이중 클릭은 두 번째부터 409) · 다음 결재선(다음 `sequenceNo`) 있으면 `ACTIVE` 전환 + 그 결재자에게 `APPROVAL_REQUESTED` 알림 · 없으면(마지막 순번) 회차·결재 모두 `COMPLETED` + `finished_at`/`completed_at` 기록 + 기안자에게 `APPROVAL_COMPLETED` 알림 · 활동 로그는 라인 자체의 `ACTIVE→APPROVED` 전환만 남기고 다음 라인 활성화·회차/결재 완료 전환은 파생 효과로 보고 기록하지 않는다(상신#7과 동일 원칙).

---

## 12. 결재 반려

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/approval-lines/{lineId}/reject` |
| 인증 필요 | Y · 해당 결재선의 결재자 본인 |
| 요구사항 | PRC-005~009 |

> 승인(11번)과 거의 동일 구조 — `REJECTED`는 `ApprovalStatus`·`ApprovalLineStatus` 둘 다 실제 값이라 승인 때 겪은
> enum 불일치는 없음. 에러코드 3종도 11번에서 이미 만든 것을 그대로 재사용(신규 추가 없음).

**Request**

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|:---:|---|
| `lineId` | Path | long | Y | 결재선 구분 번호 |
| `opinion` | Body | String | N | 반려 의견 |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.lineId` | long | 결재선 구분 번호 |
| `data.status` | String | 결재 단계 상태(`REJECTED`) |
| `data.processedAt` | String | 처리 일시 |

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 반려 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 403 | Forbidden | `APPROVAL_LINE_FORBIDDEN` | 해당 결재선의 결재자가 아님(존재하지 않는 `lineId` 포함) |
| 409 | Conflict | `APPROVAL_LINE_NOT_ACTIVE` | `WAITING` 등 아직 차례가 안 온 결재선 |
| 409 | Conflict | `APPROVAL_LINE_ALREADY_PROCESSED` | 이미 처리된 결재선의 중복 반려 시도 |

**비즈니스 규칙**: 결재선 row를 잠근 뒤 `ACTIVE` 상태를 재확인하고 처리(11번과 동일 동시성 처리) · 이후 `WAITING` 단계는 전부 `CANCELED`, 회차·결재 전체는 `REJECTED`로 종료(`finished_at`/`completed_at` 기록) · 기안자에게 `APPROVAL_REJECTED` 알림 · 활동 로그는 라인 자체의 `ACTIVE→REJECTED` 전환만 남기고 다운스트림 `CANCELED`·회차/결재 `REJECTED` 전환은 파생 효과로 보고 기록하지 않는다(11번과 동일 원칙).
