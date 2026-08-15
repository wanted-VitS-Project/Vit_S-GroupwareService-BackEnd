# 📝 Approval API — 결재 블록

**상태**: `✅ 확정` — 로컬 정본 확정. 2026-08-11 추가분은 노션·프론트 재동기화 필요 (`../API.md` §0)
**최종 업데이트**: 2026-08-15 (🔴 결재 조회 권한 확대 — 스텝 열람 권한자 전원 · 상태 무관 조회 · `WAITING` 제외 폐지)
**최종 업데이트**: 2026-08-12 (`APPROVAL_DELETE_CONFIRM_REQUIRED`(409)·`confirmApprovalCancel` 신설 — 상신 이후 블록 삭제 시 **확인 요구**. 🔴 프론트 공유 필요)
**최종 업데이트**: 2026-08-11 (참여 불가 전환 실제 알림 2종·대행 전 EDITOR 상세조회 추가)
**담당**: 이강욱
**노션**: 확인 필요 — 노션 링크 채워넣기
**범위**: 결재 블록 관련 API 7개 + 결재관리·처리 API(목록조회 착수, 나머지 4개는 순서대로 추가 예정). 알림 API는 `notification.md` 참고.

> ✅ **로컬 명세 확정 — 구현 가능.** 경로·필드명·타입·상태코드·에러코드를 **한 글자도 바꾸지 않는다** (`../API.md` §0).
> 2026-08-11 추가분은 이 로컬 정본을 기준으로 노션·프론트에 동기화한다.
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
>
> ⚠️ **위 「폐기」는 2026-08-12 에 부분 철회됐다 — 아래 항목을 함께 읽어라.**
>
> 🔴 **2026-08-12 — 계약 변경 1건 (프론트 공유 필수).** 위 2026-08-10 의 409 폐기를 **되돌린다** — 단 「금지」가 아니라 **「확인 요구」**다.
>
> | 항목 | 내용 |
> |---|---|
> | 신설 | `APPROVAL_DELETE_CONFIRM_REQUIRED` (409) — 상신 이후 결재 블록의 직접 삭제 시 **확인을 요구한다.** `approval.status` 가 `IN_PROGRESS`·`REJECTED`·`COMPLETED` 일 때 발생 |
> | 🔴 **금지가 아니다** | 이 409 를 받으면 **확인 다이얼로그를 띄우고 `confirmApprovalCancel=true` 로 재요청**한다. 그러면 200 으로 삭제된다. 실패로 끝내면 기능이 성립하지 않는다 |
> | 신설 파라미터 | `confirmApprovalCancel` (Query, boolean, 기본 `false`) — 스텝 삭제의 `moveBlockIds` 와 같은 방식 |
> | 문구 | **코드는 하나, `message` 는 상태별로 다르다** (각자 무엇을 잃는지 알린다) — `IN_PROGRESS` "{제목} 결재가 진행 중입니다. 삭제하면 결재가 취소됩니다." · `REJECTED` "{제목} 결재는 반려된 상태입니다. 삭제하면 재상신할 수 없습니다." · `COMPLETED` "{제목} 결재는 완료된 상태입니다. 삭제하면 승인 이력을 다시 볼 수 없습니다." 분기는 `code` 로 하고 `message` 는 다이얼로그에 그대로 노출한다 |
> | 확인 없이 삭제 | `DRAFT`(아직 요청이 안 갔다) · `CANCELED`(이미 종결됐다) |
> | 발생 지점 | `DELETE /api/v1/blocks/{blockId}` — **블록 도메인 엔드포인트다.** 결재 API 12개는 영향 없다 |
> | 이름 | 폐기된 `APPROVAL_IN_PROGRESS` 를 되살리지 않는다. 세 상태를 덮고 「금지」가 아니라 「확인 요구」라 의미가 다르다 |
> | 스텝·프로젝트 삭제 | **영향 없음.** 스텝 삭제는 지금처럼 결재 상태와 무관하게 성공하고 결재를 `CANCELED` 로 종결한다. 살릴 블록은 스텝 삭제 시 다른 스텝으로 옮긴다(BLK-014) |
> | DB | 마이그레이션 없음 — `approval.status` enum 을 읽기만 한다 |
>
> 근거·판정 기준·경로별 적용: [`../docs/domain/결재·알림/APR-DELETE-DRAFT.md`](../docs/domain/결재·알림/APR-DELETE-DRAFT.md) §11 (`DEL-016`~`DEL-020`)
>
> 📌 **이 409 의 명세 정본은 이 문서다.** `.ai/api/` 에 `block.md` 가 없어 블록 엔드포인트 명세 파일이
> 존재하지 않는다. 위 표가 경로·메서드·상태코드·에러코드·상태별 문구·삭제 허용 상태·cascade 예외를
> **전부 정의하며 Swagger 구현과 일치한다.** 응답 본문은 표준 실패 형식(`{httpStatus, message, code}`)으로
> **확정** — 신규 응답 필드는 없다. 폐기된 `APPROVAL_IN_PROGRESS` 가 같은 자리에서 같은 접두어를 썼던 선례를 따른다.
>
> 남은 결정은 **장기적으로 이 계약을 `block.md` 로 옮길지**뿐이며 블록 담당자 소관이다(별도 이슈).
> `../API.md` §0 에 따라 다른 도메인의 명세 파일을 임의로 만들지 않는다.

> ✅ **2026-08-11 — 사원 참여 불가 보완 정책.** `employee.resigned_at != null`,
> `employee.deleted_at != null`, `account.status=INACTIVE` 중 하나라도 해당하면 결재 참여 불가다.
> 기존 URL·메서드·요청 바디는 유지하고 조회 응답 필드와 `approval.acting_drafter_id`만 추가한다.
> `ADMIN`은 인사 전용이므로 결재자 지정·결재 조회·`scope=all` 권한이 없다.
> 블록 카드의 결재 상세에는 `requiresApproverReplacement`(boolean)를 additive로 추가한다.

> 🔴 **2026-08-15 — 계약 변경 (프론트 공유 필수).** 결재 조회 3종(1·9·10번)의 열람 범위를 넓힌다.
>
> | 항목 | 내용 |
> |---|---|
> | 확대 | 블록이 속한 **스텝 열람 권한자(VIEWER 이상) 전원**이 상태와 무관하게 결재를 조회할 수 있다. 기존 403이 200으로 바뀐다 |
> | 폐기 | `WAITING` 결재자 403 규칙. 결재선에 이름이 있으면 상태 무관 열람 |
> | 포함 | `DRAFT`도 블록 카드와 상세 API에서 동일하게 공개한다. 블록을 볼 수 있는데 내부 내용만 막는 불일치를 두지 않는다 |
> | 변경 없음 | **쓰기 권한은 그대로 기안자/대행 기안자.** `ADMIN` 차단·회사 경계·에러코드(`APPROVAL_LINE_NOT_VIEWABLE`) 모두 유지 |
> | 프론트 작업 | 조회 권한 확대만 반영하면 된다. 응답 구조와 필드는 바뀌지 않는다 |

## 엔드포인트

| # | API명칭 | METHOD | URL | 권한 |
|---|---|---|---|---|
| 1 | 결재 회차 상세조회 | GET | `/api/v1/approvals/{approvalId}/revisions/{revisionId}` | 스텝 열람 권한자(VIEWER 이상)·결재선 참여자·MASTER(상태 무관) |
| 2 | 결재 제목·내용 수정 | PATCH | `/api/v1/approvals/{approvalId}/revisions/{revisionId}` | 기안자/대행 기안자 |
| 3 | 결재 문서 추가 | POST | `/api/v1/approvals/{approvalId}/revisions/{revisionId}/documents` | 기안자/대행 기안자 |
| 4 | 결재 문서 제거 | DELETE | `/api/v1/approvals/{approvalId}/revisions/{revisionId}/documents/{documentId}` | 기안자/대행 기안자 |
| 5 | 결재선 등록·수정 | PUT | `/api/v1/approvals/{approvalId}/revisions/{revisionId}/lines` | 기안자/대행 기안자 |
| 6 | 결재 상신 | POST | `/api/v1/approvals/{approvalId}/revisions/{revisionId}/submit` | 기안자/대행 기안자 |
| 7 | 재상신 회차 생성 | POST | `/api/v1/approvals/{approvalId}/revisions` | 기안자 또는 기안자 참여 불가 시 최초 선점한 스텝 EDITOR |
| 8 | 결재관리 목록조회 | GET | `/api/v1/approvals` | 로그인 사용자(scope=all은 MASTER) |
| 9 | 결재 상세조회 | GET | `/api/v1/approvals/{approvalId}` | 1번과 동일 |
| 10 | 결재 이력조회 | GET | `/api/v1/approvals/{approvalId}/revisions` | 1번과 동일 |
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
| 인증 필요 | Y · 해당 블록 스텝의 열람 권한자(VIEWER 이상)·결재선 참여자·MASTER(상태 무관) |
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
| `data.drafterUnavailable` | boolean | 원 기안자가 퇴사·삭제·계정 비활성으로 참여 불가하면 `true` |
| `data.actingDrafterId` / `actingDrafterName` | String | 대행 기안자 사번·이름. 지정 전이면 `null` |
| `data.status` | String | 회차 상태 |
| `data.submittedAt` / `finishedAt` | String | 상신·종료 일시 |
| `data.documents[]` | Array | `documentId`/`fileVersionId`/`fileName`/`fileSize`/`uploadedAt`/`fileDeleted`(boolean) |
| `data.lines[]` | Array | `lineId`/`order`/`approverId`(**String**)/`approverName`/`approverPosition`/`approverDepartment`/`status`/`opinion`/`processedAt`/`approverUnavailable`(boolean) |

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 404 | Not Found | `APPROVAL_NOT_FOUND` | 결재 없음 |
| 404 | Not Found | `APPROVAL_REVISION_NOT_FOUND` | 회차 없음 |
| 403 | Forbidden | `APPROVAL_LINE_NOT_VIEWABLE` | 스텝 열람 권한 없음 · 결재선 미참여 · `ADMIN` · 타 회사 |

**비즈니스 규칙**: 열람 판정은 **블록이 속한 스텝의 유효 권한**을 따른다 — 프로젝트 권한을 스텝 오버라이드로 덮은 최종값이 `NONE`이 아니면 회차 상태와 무관하게 열람 가능(블록 목록조회와 동일 기준). 따라서 `DRAFT`도 블록 카드와 상세 API 모두에서 조회할 수 있다. 결재선에 이름이 오른 사람도 **상태 무관** 열람 가능(`WAITING` 제외 규칙 폐지) — 프로젝트 멤버십 검사가 면제되는 대표 직책 결재자를 위해서다. `ADMIN`은 결재 권한이 없어 계속 차단하고, 회사 경계 검사는 role 검사보다 먼저 수행한다. 기안자 참여 불가 시 스텝 EDITOR 예외 조항은 열람 범위가 넓어져 불필요해졌으나, **대행 선점(7번)의 진입 조건으로는 그대로 유지**한다. 조회 권한 확대는 수정·문서 연결·결재선 설정·상신 권한을 넓히지 않는다.

**참여 불가 실제 알림**: 미처리(`ACTIVE`·`WAITING`) 결재자가 참여 불가로 전환되면 현재 유효한 기안자에게 `APPROVAL_APPROVER_UNAVAILABLE`, 현재 기안자 또는 대행 기안자가 참여 불가로 전환되면 유효 스텝 EDITOR 전원에게 `APPROVAL_DRAFTER_UNAVAILABLE`을 발행한다. 둘 다 `targetType=APPROVAL`, `targetId=approvalId`, `targetContext.revisionId=현재 회차`다. 알림 REST API는 변경하지 않는다.

---

## 2. 결재 제목·내용 수정

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/approvals/{approvalId}/revisions/{revisionId}` |
| 인증 필요 | Y · 기안자/대행 기안자 |
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
| 인증 필요 | Y · 기안자/대행 기안자 |
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
| 인증 필요 | Y · 기안자/대행 기안자 |
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
| 인증 필요 | Y · 기안자/대행 기안자 |
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
| 409 | Conflict | `APPROVAL_REVISION_NOT_DRAFT` | DRAFT 전체 치환이 아니거나 IN_PROGRESS에서 참여 불가한 미처리 결재자 교체·제외 외의 변경 |
| 400 | Bad Request | `APPROVAL_LINE_EMPTY` | 결재자 0명 |
| 400 | Bad Request | `APPROVAL_LINE_ORDER_INVALID` | 순서 중복/누락 |
| 400 | Bad Request | `APPROVAL_LINE_APPROVER_NOT_MEMBER` | 신규 결재자가 참여 불가·ADMIN·project member 아님(`MASTER`만 소속 검증 제외) |

**비즈니스 규칙**: DRAFT에서는 기존처럼 전체 치환한다. IN_PROGRESS에서는 `ACTIVE`/`WAITING` 상태이면서 참여 불가인
결재자만 교체하거나 요청 배열에서 제외할 수 있다. 정상 결재자와 처리 완료 결재자는 수정·제외할 수 없다. 제외할 때는
남은 결재선의 `order`를 1부터 다시 연속 지정하고 뒤 순번을 당긴다. 기존 행은 `CANCELED`+논리 삭제로 이력을 보존한다.
ACTIVE 결재자를 교체하면 새 결재자가 같은 순번·ACTIVE 상태를 이어받는다. ACTIVE 결재자를 제외하면 다음 WAITING을
ACTIVE로 전환해 `APPROVAL_REQUESTED` 알림을 발행한다. 다음 WAITING도 참여 불가라면 함께 제외한 뒤 요청해야 한다.
다음 결재자가 없고 앞선 결재선이 모두 승인됐다면 회차·결재를
`COMPLETED`로 종결하고 기안자에게 완료 알림을 발행한다. 한 요청에서 교체와 제외를 동시에 처리하지 않는다.
`MASTER`만 project member 검증을 면제하고 `ADMIN`은 결재자로 지정할 수 없다. 원 기안자가 참여 불가이고 아직 대행자가
없으면, 같은 회사의 활성 스텝 EDITOR 중 이 교체·제외 요청을 먼저 성공시킨 1인을 대행 기안자로 선점한다.

---

## 6. 결재 상신

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/approvals/{approvalId}/revisions/{revisionId}/submit` |
| 인증 필요 | Y · 기안자/대행 기안자 |
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
| 인증 필요 | Y · 기안자 또는 원 기안자 참여 불가 시 스텝 EDITOR |
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

**비즈니스 규칙**: `approval.status == REJECTED`일 때만 생성 · 이전 회차 원본은 수정 안 함(이력 보존) · 반려자부터의 단계만 order 재부여 · 실제 상신은 별도로 6번 API 호출. 원 기안자가 참여 불가이고 대행 기안자가 없으면, 같은 스텝의 유효 EDITOR 중 이 API를 먼저 성공시킨 1인을 `acting_drafter_id`로 원자적으로 지정한다. 이후 편집·상신은 그 대행자만 가능하다. 대행자도 참여 불가가 되면 다른 유효 EDITOR가 같은 방식으로 재선점한다. 원 기안자 `user_id`는 감사 이력을 위해 바꾸지 않는다.

---

## 8. 결재관리 목록조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/approvals` |
| 인증 필요 | Y · `scope=all`은 `MASTER`만(`ADMIN`은 결재 권한 없음) |
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
| 403 | Forbidden | `APPROVAL_SCOPE_ALL_FORBIDDEN` | `MASTER`가 아닌 사용자의 `scope=all` 요청 |

**비즈니스 규칙**: `scope=drafted`(기본) — 원 기안자 또는 대행 기안자가 요청자인 결재 · `scope=pending` — 요청자가 현재 회차에서 `ACTIVE`인 결재만 조회 · `scope=all` — `MASTER`만 허용, 나머지 필터 자유 조합. `ADMIN`은 모든 범위에서 결재 권한이 없다.

---

## 9. 결재 상세조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/approvals/{approvalId}` |
| 인증 필요 | Y · 1번과 동일 규칙(`ApprovalViewPolicy`) |
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
| `data.drafterUnavailable` | boolean | 원 기안자 참여 불가 여부 |
| `data.actingDrafterId` / `actingDrafterName` | String | 대행 기안자 사번·이름. 없으면 `null` |
| `data.status` | String | 회차 상태 |
| `data.documents[]` | Array | 1번과 동일 구조(추정, 위 참고) |
| `data.lines[]` | Array | 1번과 동일 구조. 각 항목에 `approverUnavailable` 포함 |
| `data.blockOrigin.blockId` / `stepId` / `projectId` | long | 원본 블록·스텝·프로젝트 구분 번호 |

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | |
| 404 | Not Found | `APPROVAL_NOT_FOUND` | 결재 없음 |
| 403 | Forbidden | `APPROVAL_LINE_NOT_VIEWABLE` | 스텝 열람 권한 없음 · 결재선 미참여 · `ADMIN` · 타 회사 |

**비즈니스 규칙**: 조회 권한은 1번(회차 상세조회)과 동일 · 열람 권한자는 상태와 무관하게 항상 **현재 회차**(`approval.current_revision_no`)를 본다 · 회차 지정 불가(지정 조회는 1번 API) · `blockOrigin`으로 원본 블록·스텝·프로젝트 이동 정보 제공(MGT-006).

---

## 10. 결재 이력조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/approvals/{approvalId}/revisions` |
| 인증 필요 | Y · 1번과 동일 규칙(`ApprovalViewPolicy`) |
| 요구사항 | MGT-007 |

> ✅ **2026-08-15 해소** — `WAITING` 제외 여부가 쟁점이었으나, 열람 범위를 스텝 권한 기준으로 넓히면서
> 제외 규칙 자체가 폐지됐다. 1번과 동일 규칙을 그대로 따른다.

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

**비즈니스 규칙**: 전체 회차를 상태와 무관하게 `revisionNo` 오름차순으로 반환 · `isCurrent`로 진행 중/종료된 회차 구분 · 페이징 없음(회차 수가 적어 전량 반환).

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
