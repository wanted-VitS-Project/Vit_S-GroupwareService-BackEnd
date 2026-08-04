# 📁 프로젝트 ~ 블록 계층 v1 — API 목록

**최종 업데이트**: 2026-08-04 (⭐ #44 착수 중 FE 화면 확인 — 공고 있음/없음 분기 제거(`bidNoticeId` 선택 필드로 통합), 담당부서·담당자·워크플로우 자동생성은 화면에 있으나 **범위 밖 재확인**(§4-E))
**최종 업데이트**: 2026-08-04 (⭐ 전 엔드포인트 로컬 기준 `✅ 확정` 처리 — `AGENTS.md` §3 완화, 노션 동기화는 게이트 아님. 노션 401 코드는 아직 구버전 `AUTH_TOKEN_EXPIRED` — 팀에 정정 요청 필요)
**최종 업데이트**: 2026-08-04 (401 정정 — `AUTH_TOKEN_EXPIRED` → `AUTH_UNAUTHENTICATED` · 인증 방식 **세션 쿠키** 확정)
**담당**: 동훈
**근거**: [`PRJ-V1.md`](PRJ-V1.md) · [`PRJ-V1-USECASE.md`](PRJ-V1-USECASE.md) · **[`ERD.md`](ERD.md) (확정본)**
**상세 명세**: [`PRJ-V1-API-DETAIL.md`](PRJ-V1-API-DETAIL.md) · **흐름도**: [`PRJ-V1-API-FLOW.md`](PRJ-V1-API-FLOW.md)

> ✅ **전 엔드포인트 로컬 기준 `✅ 확정` (2026-08-04).** 구현 가능 — 노션 동기화는 구현 게이트가 아니다 (`AGENTS.md` §3).
> ⚠️ 노션 페이지 일부가 구버전이다(예: 401 코드). 발견되는 대로 노션도 맞춰 갱신 요청할 것 — 방치하면 프론트가 구버전을 본다.

---

## 0. 공통 규약

### 0-0. ⭐ ERD ↔ API 필드 매핑 (신규 ERD 기준)

| 테이블 | 컬럼 | 타입 | API 필드 | API 타입 |
| --- | --- | --- | --- | --- |
| `project` | `project_id` | BIGINT | `projectId` | Long |
| | `bid_notice_id` | BIGINT **NULL** | `bidNoticeId` | Long |
| | `name` | VARCHAR(300) | `name` | String |
| | `description` | TEXT | `description` | String |
| | `status` | ENUM 5값 | `status` | String |
| | `client_name` | VARCHAR(200) | `clientName` | String |
| | `contract_amount` | DECIMAL(18,2) | `contractAmount` | BigDecimal |
| | `close_reason_code` | ENUM 4값 | `closeReasonCode` | String |
| | `close_reason_note` | VARCHAR(500) | `closeReasonNote` | String |
| | `started_on` / `ended_on` | DATE | `startedOn` / `endedOn` | LocalDate |
| | `closed_at` | DATETIME | `closedAt` | LocalDateTime |
| | `created_by` | **VARCHAR(20)** | `createdBy.userId` | **String (사번)** |
| `project_member` | `project_member_id` | BIGINT | `memberId` | Long |
| | `user_id` | **VARCHAR(20)** | `userId` | **String (사번)** |
| | `permission` | ENUM 3값 | `permission` | String |
| `step` | `project_id` | BIGINT **NOT NULL** | `projectId` | Long |
| | `stage_id` | BIGINT NULL | `stageId` | Long |
| | `owner_user_id` | **VARCHAR(20)** | `ownerUserId` | **String (사번)** |
| | `started_on` / `ended_on` | DATE | `startedOn` / `endedOn` | LocalDate |
| `block` | `type` | ENUM **10값** | `type` | String |
| | `owner` | **VARCHAR(20)** | `owner` | **String (사번)** |
| | `row_index`/`col_span`/`sort_order` | INT | `rowIndex`/`colSpan`/`sortOrder` | int |
| `activity_log` | `act` | ENUM 5값 | `act` | String |
| | `resource_type` / `resource_id` | VARCHAR(30) / BIGINT | `resourceType` / `resourceId` | String / Long |
| | `target_name` | VARCHAR(300) **NOT NULL** | `targetName` | String |
| | `privileged_override` | TINYINT(1) | `privilegedOverride` | boolean |

> ⛔ **`step.step_type` 은 폐기됐다 (2026-08-03).** 송부 스텝 개념을 만들지 않는다 → `PRJ-V1.md` STP-007
> ⭐ **`block.type_id` 는 부활했다 (2026-08-03 재확정 · 다형성 양방향 ID).** 단 **내부 식별자라 블록 응답에 `typeId` 를 내리지 않는다** → [`ERD.md`](ERD.md) §0-12 · §5-2
> ⛔ **`block.project_id` 는 폐기됐다 (2026-08-03).** 응답에 `projectId` 를 내리지 않고, 프로젝트는 `step` 조인으로 얻는다 → [`ERD.md`](ERD.md) §0-13
> ✅ **`activity_log` 는 스키마 확정** → [`ERD.md`](ERD.md) §5-4. `project_id` 는 **NULL 허용**이다 (미매칭 입금 로그). ✅ ERD 최종본에도 반영 완료 ([`../ERD.md`](../ERD.md) §3)

> ✅ **사람 식별자는 사번 `String` 으로 확정됐다 (2026-08-03)** — `user_id VARCHAR(20)`.
> ⚠️ 노션 이슈 명세(`assigneeIds: List<Long>`)가 아직 숫자다 — **이슈 쪽 정정 요청 필요** → §4-A

### 0-1. enum 값 (ERD 확정)

| 대상 | 값 |
| --- | --- |
| `project.status` | `NOT_STARTED` · `IN_PROGRESS` · `SETTLEMENT` · `COMPLETED` · `CLOSED` |
| `project.close_reason_code` | `NOT_PARTICIPATED` · `FAILED_BID` · `NOT_SELECTED` · `CANCELED` |
| `project_member.permission` · `step_permission.permission` | `VIEWER` · `EDITOR` · `NONE` |
| `step.status` | `NOT_STARTED` · `IN_PROGRESS` · `DONE` |
| `block.type` (**10종**) | `TEXT` · `IMAGE` · `FILE` · `CHECKLIST` · `PAYMENT_CONFIRM` · `TAX_INVOICE_VIEW` · `PERFORMANCE_VIEW` · `APPROVAL` · `AI` · **`BID_NOTICE`** — ⛔ `MEMO` 폐기 |
| `activity_log.act` | `CREATE` · `UPDATE` · `DELETE` · `COMPLETE` · `MOVE` |

⚠️ **프로젝트 `COMPLETED` 와 스텝 `DONE` 은 다른 단어다.** 섞어 쓰면 FE 분기가 깨진다.

### 0-2. 길이 제약 (400 검증 대상)

| 필드 | 최대 | 컬럼 |
| --- | --- | --- |
| `name` (프로젝트) | 300 | `project.name` |
| `name` (스텝) | 200 | `step.name` |
| `title` (블록) | 200 | `block.title` |
| `closeReasonNote` | 500 | `project.close_reason_note` |
| `userId` | 20 | `*.user_id` |

### 응답 공통 봉투

**성공**만 이 3필드다. `timestamp`·`status`·`code`(`COMMON-SUCCESS`) 는 실제 구현(`ApiResponse`)에 없다.

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |

⚠️ **실패는 봉투가 다르다** — `httpStatus`·`message`·`code` 뿐이고 **`data` 가 없다** (`ApiErrorResponse`). 확장 필드(건수 등)를 따로 못 내리므로 필요하면 `message` 문구에 담는다.

### 공통 에러

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_ACCESS_DENIED` | 프로젝트 접근 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |

### 권한 표기

| 표기 | 의미 |
| --- | --- |
| `참여자` | `project_member` 행 보유 (`VIEWER` 이상) + `MASTER`·`ADMIN` |
| `프로젝트 EDITOR` | `project_member.permission='EDITOR'` + `MASTER`·`ADMIN` |
| `스텝 EDITOR` | `step_permission` 판정 결과 `EDITOR` (행 없으면 프로젝트 권한 상속 — STP-011) |

---

## 1. 전체 엔드포인트 (38)

| 개발상태 | 명세상태 | 도메인 | 기능 | Method | URL | 권한 | 설명 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 개발 전 | ✅ 확정 | Project | 프로젝트 목록 조회 | GET | `/api/v1/projects` | 참여자 | 상태·카테고리·기간으로 필터한 프로젝트 목록을 조회한다. |
| 개발 전 | ✅ 확정 | Project | 프로젝트 상세 조회 | GET | `/api/v1/projects/{projectId}` | 참여자 | 프로젝트 기본 정보·진척률·카테고리를 조회한다. |
| 개발 전 | ✅ 확정 | Project | 프로젝트 생성 | POST | `/api/v1/projects` | 전체 사용자 | 프로젝트를 생성한다. `bidNoticeId`(선택)를 보내면 공고와 연결된 채로 생성한다. 상태는 `NOT_STARTED`. |
| 개발 전 | ✅ 확정 | Project | 프로젝트 수정 | PATCH | `/api/v1/projects/{projectId}` | 프로젝트 EDITOR | 과업명·설명·기간·**발주처**·계약금액을 수정한다. |
| 개발 전 | ✅ 확정 | Project | 프로젝트 상태 변경 | PATCH | `/api/v1/projects/{projectId}/status` | 프로젝트 EDITOR | `NOT_STARTED` → `IN_PROGRESS` → `SETTLEMENT` → `COMPLETED` 로 상태를 바꾼다. |
| 개발 전 | ✅ 확정 | Project | 프로젝트 종결 | POST | `/api/v1/projects/{projectId}/close` | 프로젝트 EDITOR | 사유를 붙여 프로젝트를 `CLOSED` 로 만든다. |
| 개발 전 | ✅ 확정 | Project | 프로젝트 삭제 | DELETE | `/api/v1/projects/{projectId}` | 프로젝트 EDITOR | 진행 전 · 스텝/블록 0개일 때만 논리 삭제한다. |
| 개발 전 | ✅ 확정 | Project | 프로젝트 진척률 조회 | GET | `/api/v1/projects/{projectId}/progress` | 참여자 | 완료 스텝 / 전체 스텝 진척률을 조회한다. |
| 개발 전 | ✅ 확정 | Project | 사업 카테고리 연결 | POST | `/api/v1/projects/{projectId}/business-categories` | 프로젝트 EDITOR | 사업 카테고리를 복수 연결한다. |
| 개발 전 | ✅ 확정 | Project | 사업 카테고리 해제 | DELETE | `/api/v1/projects/{projectId}/business-categories/{categoryId}` | 프로젝트 EDITOR | 연결된 사업 카테고리를 해제한다. |
| 개발 전 | ✅ 확정 | Member | 참여자 목록 조회 | GET | `/api/v1/projects/{projectId}/members` | 참여자 | 참여자와 `permission`·퇴사 여부를 조회한다. |
| 개발 전 | ✅ 확정 | Member | 참여자 추가 | POST | `/api/v1/projects/{projectId}/members` | 프로젝트 EDITOR | 참여자를 **한 명씩** 추가한다. |
| 개발 전 | ✅ 확정 | Member | 참여자 권한 변경 | PATCH | `/api/v1/projects/{projectId}/members/{memberId}` | 프로젝트 EDITOR | 참여자 권한을 `VIEWER`/`EDITOR`/`NONE` 으로 바꾼다. |
| 개발 전 | ✅ 확정 | Member | 참여자 제거 | DELETE | `/api/v1/projects/{projectId}/members/{memberId}` | 프로젝트 EDITOR | 참여자를 프로젝트에서 제거한다. |
| 개발 전 | ✅ 확정 | Stage | 스테이지 목록 조회 | GET | `/api/v1/projects/{projectId}/stages` | 참여자 | 스테이지를 순서대로 조회한다. |
| 개발 전 | ✅ 확정 | Stage | 스테이지 생성 | POST | `/api/v1/projects/{projectId}/stages` | 프로젝트 EDITOR | 스테이지를 생성한다. |
| 개발 전 | ✅ 확정 | Stage | 스테이지 수정 | PATCH | `/api/v1/stages/{stageId}` | 프로젝트 EDITOR | 스테이지 이름을 수정한다. |
| 개발 전 | ✅ 확정 | Stage | 스테이지 순서 변경 | PATCH | `/api/v1/projects/{projectId}/stages/order` | 프로젝트 EDITOR | 스테이지 순서를 일괄 재정렬한다. |
| 개발 전 | ✅ 확정 | Stage | 스테이지 삭제 | DELETE | `/api/v1/stages/{stageId}` | 프로젝트 EDITOR | 하위 스텝 이전 대상을 지정해 스테이지를 삭제한다. |
| 개발 전 | ✅ 확정 | Stage | 하위 스텝 권한 일괄 적용 | POST | `/api/v1/stages/{stageId}/step-permissions` | 프로젝트 EDITOR | 스테이지 하위 스텝에 권한을 일괄 적용한다. |
| 개발 전 | ✅ 확정 | Step | 스텝 목록 조회 | GET | `/api/v1/projects/{projectId}/steps` | 참여자 | 스텝을 스테이지·순서 기준으로 조회한다. |
| 개발 전 | ✅ 확정 | Step | 스텝 상세 조회 | GET | `/api/v1/steps/{stepId}` | 스텝 접근 권한 | 스텝 속성·상태·진척률을 조회한다. |
| 개발 전 | ✅ 확정 | Step | 스텝 생성 | POST | `/api/v1/projects/{projectId}/steps` | 프로젝트 EDITOR | 스텝을 생성한다. `stage_id` 는 `NULL` 허용, `project_id` 는 항상 채운다. |
| 개발 전 | ✅ 확정 | Step | 스텝 수정 | PATCH | `/api/v1/steps/{stepId}` | 스텝 EDITOR | 이름·기간·책임자·소속 스테이지를 수정한다. |
| 개발 전 | ✅ 확정 | Step | 스텝 순서 변경 | PATCH | `/api/v1/projects/{projectId}/steps/order` | 프로젝트 EDITOR | 스텝 순서를 일괄 재정렬한다. |
| 개발 전 | ✅ 확정 | Step | 스텝 상태 변경 | PATCH | `/api/v1/steps/{stepId}/status` | 스텝 EDITOR | `NOT_STARTED` / `IN_PROGRESS` 로 바꾼다. `DONE` 은 완료 처리 API 소관. |
| 개발 전 | ✅ 확정 | Step | 스텝 완료 처리 | POST | `/api/v1/steps/{stepId}/complete` | 스텝 EDITOR | 미완료 이슈 처리 방식을 지정해 스텝을 완료한다. |
| 개발 전 | ✅ 확정 | Step | 스텝 삭제 | DELETE | `/api/v1/steps/{stepId}` | 프로젝트 EDITOR | 이슈 처리 방식을 지정해 스텝을 삭제한다. |
| 개발 전 | ✅ 확정 | StepPermission | 스텝 권한 목록 조회 | GET | `/api/v1/steps/{stepId}/permissions` | 프로젝트 EDITOR | 스텝 권한 오버라이드 행을 조회한다. |
| 개발 전 | ✅ 확정 | StepPermission | 스텝 권한 부여·변경 | PUT | `/api/v1/steps/{stepId}/permissions/{userId}` | 프로젝트 EDITOR | 스텝별 `VIEWER`/`EDITOR`/`NONE` 을 지정한다. |
| 개발 전 | ✅ 확정 | StepPermission | 스텝 권한 회수 | DELETE | `/api/v1/steps/{stepId}/permissions/{userId}` | 프로젝트 EDITOR | 오버라이드 행을 지워 프로젝트 권한 상속으로 되돌린다. |
| 개발 전 | ✅ 확정 | Block | 스텝 블록 일괄 조회 | GET | `/api/v1/steps/{stepId}/blocks` | 스텝 접근 권한 | 블록과 타입별 상세를 한 번에 조회한다. |
| 개발 전 | ✅ 확정 | Block | 블록 생성 | POST | `/api/v1/steps/{stepId}/blocks` | 스텝 EDITOR | **10종** 타입 안에서 블록을 생성한다. `PAYMENT_CONFIRM`·`TAX_INVOICE_VIEW` 는 **스텝당 1개** 검사 (PCB-001B · TXL-001B). |
| 개발 전 | ✅ 확정 | Block | 블록 배치 변경 | PATCH | `/api/v1/steps/{stepId}/blocks/layout` | 스텝 EDITOR | 드래그 결과를 일괄 반영한다. |
| 개발 전 | ✅ 확정 | Block | 블록 삭제 | DELETE | `/api/v1/blocks/{blockId}` | 스텝 EDITOR | 잠금 4종을 검사한 뒤 논리 삭제한다. |
| 개발 전 | ✅ 확정 | IssueBlock | 이슈-블록 연결 | POST | `/api/v1/blocks/{blockId}/issues` | 스텝 EDITOR | 같은 스텝의 이슈를 블록에 연결한다. |
| 개발 전 | ✅ 확정 | IssueBlock | 이슈-블록 연결 해제 | DELETE | `/api/v1/blocks/{blockId}/issues/{issueId}` | 스텝 EDITOR | 이슈-블록 연결을 해제한다. |
| 개발 전 | ✅ 확정 | ActivityLog | 활동기록 조회 | GET | `/api/v1/projects/{projectId}/activity-logs` | 참여자 | 프로젝트 활동기록을 최신순으로 조회한다. |

---

## 2. 요구사항 ↔ 엔드포인트 대응

| 요구사항 | 엔드포인트 |
| --- | --- |
| PRJ-001 · 002 | `POST /api/v1/projects` (공고 전환은 입찰 도메인 `POST /api/bid-notices/{id}/project`) |
| PRJ-003 · 004 · 005 | `PATCH .../status` · `POST .../close` |
| PRJ-006 · 008 | `PATCH /api/v1/projects/{projectId}` |
| PRJ-007 | `POST/DELETE .../business-categories` |
| PRJ-009~012 | `.../members/*` |
| PRJ-013 | `GET .../progress` · 목록/상세 응답 |
| PRJ-014 | `DELETE /api/v1/projects/{projectId}` |
| PRJ-015 | `GET /api/v1/projects` |
| PRJ-016 · 017 | `GET .../activity-logs` |
| STG-001~003 | `.../stages/*` |
| STG-004 | `POST /api/v1/stages/{stageId}/step-permissions` (저장은 `step_permission`) |
| STP-001~009 | `.../steps/*` |
| STP-010 · 011 | `.../steps/{stepId}/permissions/*` |
| STP-012 | `GET /api/v1/steps/{stepId}` |
| BLK-001~008 · 012 | `.../blocks/*` (`owner` 는 블록 생성·조회 응답에 포함 — BLK-012) |
| BLK-009~011 | `.../blocks/{blockId}/issues/*` |
| **TXL-001B** (재무) | `POST /api/v1/steps/{stepId}/blocks` — `TAX_INVOICE_VIEW` 스텝당 1개 검사 → [`../재무관리/TAX-V1.md`](../재무관리/TAX-V1.md) |

---

## 3. ✅ 신규 ERD 로 해소된 항목

| 이전 미결 | 해소 |
| --- | --- |
| 스텝 종료일 컬럼명 미정 (`PRJ-V1.md` §5-3) | ✅ `step.ended_on DATE` 로 확정 |
| `activity_log.act` 값 목록 미확정 (§5-4) | ✅ **5값 확정.** 스키마 전체도 확정 → [`ERD.md`](ERD.md) §5-4 (⚠️ ERD Cloud 미반영) |
| `step.project_id` 존재 여부 (INV-02) | ✅ `NOT NULL` 로 확정 |
| 송부 스텝 식별 방법 (STP-007) | ⛔ **폐기 (2026-08-03).** 송부 스텝을 만들지 않으므로 `step_type` 도 없다 |
| 프로젝트 종결 사유 저장 (PRJ-005) | ✅ `close_reason_code` ENUM + `close_reason_note` 확정 |
| 3열 그리드 배치 (BLK-003) | ✅ `row_index` · `col_span` · `sort_order` 확정 |
| `project_business_category` 복수 선택 (PRJ-007) | ✅ `project_id` 추가 + UNIQUE 로 해소 |
| **`project` 발주처 (§4-C)** | ✅ **`project.client_name VARCHAR(200)` 추가로 해소** (2026-08-03) |
| **`project` 계약금액 (PRJ-008)** | ✅ **`project.contract_amount DECIMAL(18,2)` 추가로 해소** (2026-08-03) |
| **`project.bid_notice_id` NOT NULL (PRJ-001)** | ✅ **NULL 허용으로 변경** — 직접 생성이 정상 경로다 |

---

## 4. 🚨 신규 ERD 와 기존 문서의 충돌 — 팀 결정 필요

### 4-A. 사람 식별자 타입 → ✅ **확정 (2026-08-03)**

> **`user_id VARCHAR(20)` — 사번 `String` 으로 간다.** API 필드는 `userId` 다.

| 출처 | 표기 | 판정 |
| --- | --- | --- |
| **확정 ERD** | `user_id VARCHAR(20)` (사번) | ✅ **이게 정본** |
| `PERMISSION.md` §3 | `page_permission(page_code, employee_id, permission)` | ✅ 같은 값 (사번) — 컬럼명만 다름 |
| `PRJ-V1.md` STP-003 | "`owner_user_id` 는 `employee_id` 다(계정 id 아님)" | ✅ 같은 값 |
| **노션 이슈 명세** | `assigneeIds: List<Long>` · `"userId": 3` → 숫자 | ⛔ **이슈 쪽이 `String` 으로 맞춰야 한다** |

⚠️ **노션 이슈 명세는 아직 `Long` 이다.** 프론트와의 계약이므로 **이슈 담당자에게 정정 요청**이 필요하다
(`AGENTS.md` §3 — 명세 변경은 팀 합의 사항). 내 도메인 문서 전체는 이미 `String` 사번으로 통일돼 있다.

### 4-B. 블록 타입 → ✅ **10종 확정** (`MEMO` 폐기 · `BID_NOTICE` 신설 · 2026-08-03)

> **`MEMO` 를 `block.type` enum 에서 뺀다.** 상세 테이블·담당이 없고 담을 내용도 없다 —
> 자유 서술은 `TEXT` 가 이미 담당한다 ([`BLOCK.md`](../../global/BLOCK.md) §4-1 *"본문·목차·회의 메모"*).

| 출처 | 값 | 판정 |
| --- | --- | --- |
| [`BLOCK.md`](../../global/BLOCK.md) (제목·§3·§4 카탈로그·§5 요약표) | **10종** | ✅ **이제 양쪽이 일치한다** |
| [`ERD.md`](ERD.md) `block.type` | **10종** (`MEMO` 제거) | ✅ |

⚠️ **ERD Cloud 의 `block.type` enum 에서 `MEMO` 를 빼야 한다** → [`ERD-CLOUD-DIFF.md`](../ERD-CLOUD-DIFF.md) §3-2
⛔ FE 블록 타입 선택 목록에서도 `MEMO` 를 **빼라.**

### 4-C. ~~`project` 에 발주처 컬럼이 없다~~ → ✅ **해소 (2026-08-03)**

`project.client_name VARCHAR(200) NULL` 로 확정했다 → [`ERD.md`](ERD.md) §2

`bid_notice.demand_agency` 조인으로 대체하지 않은 이유:
① 공고 기관명과 실제 계약 발주처가 다를 수 있다 ② 공고 없이 생긴 프로젝트(`PRJ-001`)에서 깨진다.

이걸로 아래가 열렸다 — 계산서 매칭 후보(`TAX-V1` TXM-002) · 입금 매칭 후보(`PAY-V1` MTC-005) · 정산현황 발주처 열·필터(`STL-V1` STL-004·007).

### 4-D. `activity_log` → ✅ **스키마 확정 (2026-08-03)**

`project_id`(**NULL 허용**) · `resource_type` · `target_name` · `privileged_override` 를 확정하고 `block_id` 를 NULL 허용으로 풀었다 → [`ERD.md`](ERD.md) §5-4

`project_id` 를 NOT NULL 로 두면 **미매칭 입금 로그(`PAY-007`)를 남길 수 없다.** 그래서 `PRJ-016` 의 `NOT NULL` 은 **기록 규칙**으로 읽는다 — 프로젝트 계층 6종은 앱이 반드시 채우고 `PAYMENT`·`TAX_INVOICE` 만 NULL 을 허용한다.

⚠️ **ERD Cloud 에는 아직 없다.** 남은 미결은 **로그를 남길 사건 목록**뿐 → [`HANDOFF.md`](../HANDOFF.md) §L-2

### 4-E. FE "프로젝트 등록" 화면 vs 현재 스펙 (2026-08-04, #44 착수 중 발견)

FE 목업이 공고 있음/없음을 **분기하지 않는 단일 폼**으로 나왔다. 검토 결과:

| 화면 요소 | 처리 |
| --- | --- |
| 공고 있음/없음 분기 없음 | ✅ **반영.** `POST /api/v1/projects` 가 `bidNoticeId`(선택)를 그대로 받는다. 화면의 "자동" 배지(프로젝트명·설명·사업유형·업무코드)는 **FE 가 공고 상세를 미리 읽어 클라이언트에서 채우는 것**이라 백엔드 로직 추가가 없다 — PRJ-002(공고 전환)와의 경계는 별개 문제로 남는다(§5-2) |
| 담당 부서(필수) | ⛔ **범위 밖 유지.** `project` 테이블에 컬럼 없음. ERD 에 프로젝트-부서 연결이 없다 |
| 담당자(필수, 참여자와 별개) | ⛔ **범위 밖 유지.** `created_by`(생성자)만 있고 별도 "담당자" 역할이 없다 |
| 적용 워크플로우 → 스테이지 자동 생성 | ⛔ **범위 밖 유지.** `PRJ-V1.md:40`·`ERD.md:548` 에 `template` 테이블 부재로 "스텝/스테이지는 직접 추가만" 이 **이미 결정돼 있다.** 화면은 그 반대를 전제하므로 이 화면 그대로 만들려면 스키마 설계부터 다시 해야 한다 |

⚠️ **화면(디자인)이 백엔드 스펙보다 앞서 있다.** 담당부서·담당자·워크플로우 3개는 별도 스키마·요구사항 확정이 있어야 다시 논의 가능 — 지금은 화면에서 빼거나 FE 쪽에서 임시 처리해야 한다. 백로그에 등록.

---

## 5. 🚧 아직 남은 확인 필요

| # | 항목 | 영향 |
| --- | --- | --- |
| 1 | `issue` 스키마 미확정 | 스텝 완료·삭제 시 이슈 처리 응답 필드 → [`HANDOFF.md`](../HANDOFF.md) §3 |
| 2 | 공고 전환 **스냅샷 복사 vs 링크만** 문서 충돌 (`PRJ-V1.md` §5-7) | `POST /api/bid-notices/{id}/project` 응답 범위. **ERD 는 링크만(`bid_notice_id`)이라 PRJ-002 쪽이 맞다** |
| 3 | `business_category` 마스터 | ✅ 테이블 존재 확인. `categoryId` 조회 API 필요 여부만 미정 |
| 4 | ~~`stage` 테이블 컬럼 미제시~~ | ✅ **해소.** `stage_id`·`project_id`·`name`·`sort_order`·`created_at`·`deleted_at` → [`ERD.md`](ERD.md) §4-1 |
| 5 | `project_member` 에 `deleted_at` 없음 | 참여자 제거가 **하드 DELETE** 다. `activity_log` 가 참조하면 INV-05(하드삭제 금지)와 충돌 |
| 6 | ~~`block.type_id` 운용 방식~~ | ⭐ **재확정 (2026-08-03).** 폐기안을 **철회**하고 **다형성 양방향 ID**(`block.type_id` ↔ `{상세}.block_id` · 양쪽 FK 없음)로 간다. 블록 응답에서 `typeId` 는 여전히 **안 내린다** (내부 식별자) → [`ERD.md`](ERD.md) §5-2 |
| 7 | 인증 방식 (세션 쿠키 / 토큰) | 전 API 401 처리 |
| 8 | ~~`block.owner` 운용 규칙~~ | ✅ **확정 (2026-08-03).** 블록 응답에 `owner`(사번) 를 내린다. 정산현황 담당자 = **다음 예정일 회차 블록의 owner** |
