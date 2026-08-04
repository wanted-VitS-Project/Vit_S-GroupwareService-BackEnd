# 📁 프로젝트 ~ 블록 계층 v1 — API 상세 명세

**최종 업데이트**: 2026-08-05 (⭐ **`inProgressIssueCount` 신설** — 스텝 목록·상세. FE 스텝 진행바 3색(완료/진행 중/진행 전)을 그리려면 필요하다. 🚨 프론트 계약 추가 — 통보 필요)
**최종 업데이트**: 2026-08-05 (⭐ `STEP_NAME_TOO_LONG` 신설(생성·수정) — `step.name VARCHAR(200)` 초과가 500 으로 샜다)
**최종 업데이트**: 2026-08-04 (⭐ 생성 계열 7건 `httpStatus` `200`→`201` 정정(상태코드 표와 모순) · `STAGE_NAME_TOO_LONG` 신설)
**담당**: 동훈
**목록 문서**: [`PRJ-V1-API.md`](PRJ-V1-API.md) · **요구사항**: [`PRJ-V1.md`](PRJ-V1.md) · **흐름도**: [`PRJ-V1-API-FLOW.md`](PRJ-V1-API-FLOW.md)

> ✅ **전 엔드포인트 로컬 기준 `✅ 확정`** (`AGENTS.md` §3). 구현 가능 — 노션 동기화는 사용자가 별도 관리.
> ⭐ **성공 응답은 `httpStatus`·`message`·`data` 봉투를 쓴다** (2026-08-04 정정 — `timestamp`·`status`·`code`(`COMMON-SUCCESS`) 는 실제 구현에 없다). **실패 응답은 `data` 없이 `httpStatus`·`message`·`code`** 다 (`ApiErrorResponse`). 아래 Response Parameter 의 들여쓴 항목은 `data` 하위다.
> 공통 에러(`401 AUTH_UNAUTHENTICATED` · `403 PROJECT_ACCESS_DENIED` · `404 PROJECT_NOT_FOUND`)는 각 Status Code 표에 반복 기재한다.

### ⭐ 신규 ERD 정합 — 전 엔드포인트에 적용된 규칙

| 항목 | 규칙 |
| --- | --- |
| **사람 식별자** | ✅ **확정 — `userId` String (사번 · `VARCHAR(20)`)**. ⛔ `Long` 이 아니다 → [`PRJ-V1-API.md`](PRJ-V1-API.md) §4-A |
| 프로젝트 상태 | `NOT_STARTED` · `IN_PROGRESS` · `SETTLEMENT` · `COMPLETED` · `CLOSED` |
| 스텝 상태 | `NOT_STARTED` · `IN_PROGRESS` · `DONE` — **프로젝트의 `COMPLETED` 와 다른 단어다** |
| 날짜 필드 | `startedOn` / `endedOn` (`started_on` / `ended_on`) |
| 권한 필드 | `permission` (`grade` 아님) — `VIEWER` · `EDITOR` · `NONE` |
| 길이 제약 | 프로젝트명 300 · 스텝명 200 · 블록 제목 200 · 종결사유비고 500 · 사번 20 |
| ✅ `clientName` | **해소 (2026-08-03).** `project.client_name VARCHAR(200) NULL` 로 확정 → [`ERD.md`](ERD.md) §2 |
| ✅ `block.owner` | **신설 (2026-08-03).** 블록 담당자 사번 `VARCHAR(20)` · 선택 입력 (BLK-012) → [`ERD.md`](ERD.md) §5-1 |
| ⭐ `block.type_id` | **부활 (2026-08-03).** 다형성 양방향 ID — 상세 행 PK · FK 없음. **내부 식별자라 응답에 `typeId` 를 내리지 않는다** → [`ERD.md`](ERD.md) §0-12 · §5-2 |
| ⛔ `block.project_id` | **폐기 (2026-08-03).** 프로젝트는 `step` 조인으로 얻는다 → [`ERD.md`](ERD.md) §0-13 |
| ⛔ `step.step_type` | **폐기 (2026-08-03).** 송부 스텝 개념이 없다 (`PRJ-V1.md` STP-007) |
| 블록 삭제 잠금 | **4종** — 입금 연결 입금확인 · 계산서 연결 조회 · 진행 중 결재 · 결재 대상 파일 (`BLOCK.md` §8) |
| ⭐ `businessCategories[]` | **`categoryId` · `name` · `code` 3필드 고정 (2026-08-04 추가).** `code` = `business_category.code VARCHAR(30)` (업무코드 · **NULL 허용**) — FE 프로젝트 상세 화면이 `건축공모 [ACT]` 로 노출한다. 카테고리를 내리는 **4개 엔드포인트 전부** 같은 모양이다 (생성 · 목록 · 상세 · 카테고리 연결) |

⚠️ **ERD Cloud 미반영분이 있다** → [`ERD-CLOUD-DIFF.md`](../ERD-CLOUD-DIFF.md). 코드는 [`../ERD.md`](../ERD.md) §3 정본을 따른다.

---

# POST `/api/v1/projects` — 프로젝트 생성

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 프로젝트 생성 |
| Method | POST |
| URL | `/api/v1/projects` |
| 인증 필요 여부 | Y |
| 권한 | 전체 사용자 |
| 요구사항 | PRJ-001 · PRJ-002 |

## Path Parameter
없음

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `name` | String | Y | 과업명 (**최대 300자** · `project.name`) |
| `description` | String | N | 설명 (`project.description` TEXT) |
| `clientName` | String | N | 발주처 (`project.client_name VARCHAR(200)`) |
| `startedOn` | LocalDate | N | 시작일 (`started_on`) |
| `endedOn` | LocalDate | N | 종료일 (`ended_on`) |
| `contractAmount` | BigDecimal | N | 계약금액 (`DECIMAL(18,2)`) |
| `businessCategoryIds` | List\<Long\> | N | 사업 카테고리 ID 목록 (복수 · `project_business_category` N행) |
| `bidNoticeId` | Long | N | 연결할 공고 ID (`project.bid_notice_id`, **UNIQUE** — 공고 1개당 프로젝트 1개). 생략하면 `NULL`(공고 없이 생성) |

생성 시 상태는 시스템이 `NOT_STARTED` 로 설정한다 (PRJ-001).
`created_by` 에 요청자 **사번**이 들어가고, 생성자는 자동으로 `EDITOR` 참여자가 된다.
⚠️ 화면(FE)의 프로젝트명·설명·사업유형·업무코드 "자동 입력"은 **클라이언트가 공고 상세를 읽어 미리 채우는 것**이다 — 백엔드는 그냥 받은 값을 저장할 뿐, 공고 스냅샷 복사 로직이 없다 (§4-E).

## Request Example
```
{
  "name":"OO시 상수도 관리 용역",
  "description":"상수도 관리 시스템 고도화 용역",
  "clientName":"OO시청",
  "startedOn":"2026-08-01",
  "endedOn":"2026-12-31",
  "contractAmount":120000000,
  "businessCategoryIds": [1,4]
}
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`201` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `projectId` | Long | 생성된 프로젝트 ID |
| `name` | String | 과업명 |
| `clientName` | String | 발주처 (`project.client_name`) |
| `status` | String | 프로젝트 상태 |
| `startedOn` | LocalDate | 시작일 |
| `endedOn` | LocalDate | 종료일 |
| `contractAmount` | BigDecimal | 계약금액 |
| `businessCategories` | List\<Object\> | 연결된 사업 카테고리 (`categoryId`·`name`·`code`) |
| `bidNoticeId` | Long | 연결된 공고 ID (직접 생성이면 `null`) |
| `createdBy` | Object | 생성자 |
| `createdAt` | LocalDateTime | 생성 일시 |

## Success Example
```
{
  "httpStatus":201,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "projectId":12,
    "name":"OO시 상수도 관리 용역",
    "clientName":"OO시청",
    "status":"NOT_STARTED",
    "startedOn":"2026-08-01",
    "endedOn":"2026-12-31",
    "contractAmount":120000000,
    "businessCategories": [
      { "categoryId":1, "name":"환경", "code":"ENV" },
      { "categoryId":4, "name":"상하수도", "code":null }
    ],
    "bidNoticeId":null,
    "createdBy": { "userId":"E2024001", "name":"김용준" },
    "createdAt":"2026-08-01T10:00:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | - | 프로젝트 생성 성공 |
| 400 | Bad Request | `PROJECT_NAME_REQUIRED` | 과업명이 입력되지 않음 |
| 400 | Bad Request | `PROJECT_NAME_TOO_LONG` | 과업명이 300자를 초과함 (2026-08-04 신설 — 명세 §0-2 길이제약에는 있었으나 에러코드가 빠져 있었음, `BUSINESS_CATEGORY_FIELD_TOO_LONG` 선례) |
| 400 | Bad Request | `PROJECT_DATE_RANGE_INVALID` | 시작일이 종료일보다 늦음 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 404 | Not Found | `BUSINESS_CATEGORY_NOT_FOUND` | 사업 카테고리가 존재하지 않음 |
| 409 | Conflict | `PROJECT_BID_NOTICE_ALREADY_LINKED` | 이미 다른 프로젝트가 연결된 공고임 (2026-08-04 신설 — `project.bid_notice_id` UNIQUE 제약을 앱 레벨에서 먼저 막는다, BCT §3-1 선례) |

---

# GET `/api/v1/projects` — 프로젝트 목록 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 프로젝트 목록 조회 |
| Method | GET |
| URL | `/api/v1/projects` |
| 인증 필요 여부 | Y |
| 권한 | 참여자 (`MASTER`·`ADMIN` 은 전 프로젝트) |
| 요구사항 | PRJ-013 · PRJ-015 |

## Path Parameter
없음

## Request Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `status` | String | N | `NOT_STARTED`·`IN_PROGRESS`·`SETTLEMENT`·`COMPLETED`·`CLOSED` |
| `businessCategoryId` | Long | N | 사업 카테고리 필터 |
| `startedOnFrom` | LocalDate | N | 기간 필터 시작 |
| `startedOnTo` | LocalDate | N | 기간 필터 종료 |
| `keyword` | String | N | 과업명·발주처 검색 |
| `page` | int | N | 기본 0 |
| `size` | int | N | 기본 20 |

**보관 기능이 없다.** 종결(`CLOSED`) 건도 `status` 필터로 다시 볼 수 있다 (PRJ-015).

## Request Body
없음

## Request Example
```
GET /api/v1/projects?status=IN_PROGRESS&businessCategoryId=1&page=0&size=20
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `content` | List\<Object\> | 프로젝트 목록 |
| `content[].projectId` | Long | 프로젝트 ID |
| `content[].name` | String | 과업명 |
| `content[].clientName` | String | 발주처 (`project.client_name`) |
| `content[].status` | String | 프로젝트 상태 |
| `content[].startedOn` | LocalDate | 시작일 |
| `content[].endedOn` | LocalDate | 종료일 |
| `content[].contractAmount` | BigDecimal | 계약금액 |
| `content[].progressRate` | Integer | 진척률(%) — **스텝 0개면 응답에 담지 않는다** |
| `content[].businessCategories` | List\<Object\> | 사업 카테고리 (`categoryId`·`name`·`code`) |
| `page` | int | 현재 페이지 |
| `size` | int | 페이지 크기 |
| `totalElements` | long | 전체 건수 |
| `totalPages` | int | 전체 페이지 수 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "content": [
      {
        "projectId":12,
        "name":"OO시 상수도 관리 용역",
        "clientName":"OO시청",
        "status":"IN_PROGRESS",
        "startedOn":"2026-08-01",
        "endedOn":"2026-12-31",
        "contractAmount":120000000,
        "progressRate":40,
        "businessCategories": [ { "categoryId":1, "name":"환경", "code":"ENV" } ]
      }
    ],
    "page":0,
    "size":20,
    "totalElements":1,
    "totalPages":1
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 프로젝트 목록 조회 성공 |
| 400 | Bad Request | `PROJECT_STATUS_INVALID` | 허용되지 않은 상태 값 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |

---

# GET `/api/v1/projects/{projectId}` — 프로젝트 상세 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 프로젝트 상세 조회 |
| Method | GET |
| URL | `/api/v1/projects/{projectId}` |
| 인증 필요 여부 | Y |
| 권한 | 참여자 |
| 요구사항 | USC-PRJ-003 · USC-PRJ-017 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 조회할 프로젝트 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
GET /api/v1/projects/12
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `projectId` | Long | 프로젝트 ID |
| `name` | String | 과업명 |
| `description` | String | 설명 (`project.description` TEXT · `null` 허용) — 2026-08-04 추가 |
| `clientName` | String | 발주처 (`project.client_name`) |
| `status` | String | 프로젝트 상태 |
| `startedOn` | LocalDate | 시작일 |
| `endedOn` | LocalDate | 종료일 |
| `contractAmount` | BigDecimal | 계약금액 |
| `progressRate` | Integer | 진척률(%) — **`doneStepCount / stepCount`**. 스텝 0개면 미포함 |
| `stepCount` | int | 전체 스텝 수 |
| `doneStepCount` | int | 완료 스텝 수 |
| `businessCategories` | List\<Object\> | 사업 카테고리 (`categoryId`·`name`·`code`) |
| `bidNoticeId` | Long | 연결된 공고 ID |
| `closeReasonCode` | String | 종결 사유 코드 (종결 건만) |
| `closeReasonNote` | String | 종결 사유 상세 |
| `myPermission` | String | 요청자의 프로젝트 권한 (`VIEWER`/`EDITOR`) |
| `createdAt` | LocalDateTime | 생성 일시 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "projectId":12,
    "name":"OO시 상수도 관리 용역",
    "description":"상수도 관리 시스템 고도화 용역",
    "clientName":"OO시청",
    "status":"IN_PROGRESS",
    "startedOn":"2026-08-01",
    "endedOn":"2026-12-31",
    "contractAmount":120000000,
    "progressRate":40,
    "stepCount":5,
    "doneStepCount":2,
    "businessCategories": [ { "categoryId":1, "name":"환경", "code":"ENV" } ],
    "bidNoticeId":null,
    "closeReasonCode":null,
    "closeReasonNote":null,
    "myPermission":"EDITOR",
    "createdAt":"2026-08-01T10:00:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 프로젝트 상세 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_ACCESS_DENIED` | 프로젝트 접근 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |

---

# PATCH `/api/v1/projects/{projectId}` — 프로젝트 수정

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 프로젝트 수정 |
| Method | PATCH |
| URL | `/api/v1/projects/{projectId}` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | PRJ-006 · PRJ-008 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 수정할 프로젝트 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `name` | String | N | 과업명 (최대 300자) |
| `description` | String | N | 설명 |
| `clientName` | String | N | 발주처 (`project.client_name VARCHAR(200)`) |
| `startedOn` | LocalDate | N | 시작일 |
| `endedOn` | LocalDate | N | 종료일 |
| `contractAmount` | BigDecimal | N | 계약금액 |

계약금액은 **`project.contract_amount` 한 곳에만** 저장한다 (INV-08). 기간은 자동 계산하지 않는다 (PRJ-006).

## Request Example
```
{
  "endedOn":"2027-01-31",
  "contractAmount":135000000
}
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `projectId` | Long | 프로젝트 ID |
| `name` | String | 과업명 |
| `clientName` | String | 발주처 (`project.client_name`) |
| `startedOn` | LocalDate | 시작일 |
| `endedOn` | LocalDate | 종료일 |
| `contractAmount` | BigDecimal | 계약금액 |
| `updatedAt` | LocalDateTime | 수정 일시 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "projectId":12,
    "name":"OO시 상수도 관리 용역",
    "clientName":"OO시청",
    "startedOn":"2026-08-01",
    "endedOn":"2027-01-31",
    "contractAmount":135000000,
    "updatedAt":"2026-08-01T11:20:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 프로젝트 수정 성공 |
| 400 | Bad Request | `PROJECT_DATE_RANGE_INVALID` | 시작일이 종료일보다 늦음 |
| 400 | Bad Request | `CONTRACT_AMOUNT_INVALID` | 계약금액이 음수 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |

---

# PATCH `/api/v1/projects/{projectId}/status` — 프로젝트 상태 변경

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 프로젝트 상태 변경 |
| Method | PATCH |
| URL | `/api/v1/projects/{projectId}/status` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | PRJ-003 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `status` | String | Y | `NOT_STARTED`·`IN_PROGRESS`·`SETTLEMENT`·`COMPLETED` |

**역방향 전이를 막지 않는다** (PRJ-003). `CLOSED` 는 이 API 로 설정할 수 없고 종결 API 를 쓴다.

## Request Example
```
{ "status":"IN_PROGRESS" }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `projectId` | Long | 프로젝트 ID |
| `status` | String | 변경된 상태 |
| `updatedAt` | LocalDateTime | 변경 일시 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": { "projectId":12, "status":"IN_PROGRESS", "updatedAt":"2026-08-01T11:30:00" }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 상태 변경 성공 |
| 400 | Bad Request | `PROJECT_STATUS_INVALID` | 허용되지 않은 상태 값 (`CLOSED` 포함) |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |

---

# POST `/api/v1/projects/{projectId}/close` — 프로젝트 종결

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 프로젝트 종결 |
| Method | POST |
| URL | `/api/v1/projects/{projectId}/close` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | PRJ-004 · PRJ-005 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `closeReasonCode` | String | Y | `NOT_PARTICIPATED`(미참여)·`FAILED_BID`(유찰)·`NOT_SELECTED`(미선정)·`CANCELED`(취소) |
| `closeReasonNote` | String | N | 사유 상세 |

**어느 상태에서든 종결할 수 있다.** 종결해도 목록·로그에서 사라지지 않는다 (PRJ-004).

## Request Example
```
{
  "closeReasonCode":"NOT_SELECTED",
  "closeReasonNote":"기술평가 2순위로 탈락"
}
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `projectId` | Long | 프로젝트 ID |
| `status` | String | `CLOSED` |
| `closeReasonCode` | String | 종결 사유 코드 |
| `closeReasonNote` | String | 종결 사유 상세 |
| `closedAt` | LocalDateTime | 종결 일시 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "projectId":12,
    "status":"CLOSED",
    "closeReasonCode":"NOT_SELECTED",
    "closeReasonNote":"기술평가 2순위로 탈락",
    "closedAt":"2026-08-01T12:00:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 프로젝트 종결 성공 |
| 400 | Bad Request | `CLOSE_REASON_REQUIRED` | 종결 사유가 입력되지 않음 |
| 400 | Bad Request | `CLOSE_REASON_INVALID` | 허용되지 않은 종결 사유 코드 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |

---

# DELETE `/api/v1/projects/{projectId}` — 프로젝트 삭제

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 프로젝트 삭제 |
| Method | DELETE |
| URL | `/api/v1/projects/{projectId}` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | PRJ-014 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 삭제할 프로젝트 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
DELETE /api/v1/projects/12
```

`진행 전` 이고 **스텝·블록이 0개**일 때만 삭제된다. 삭제는 `deleted_at` 논리 삭제다.

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | `null` |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data":null
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 프로젝트 삭제 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |
| 409 | Conflict | `PROJECT_DELETE_NOT_ALLOWED` | 진행 전이 아니거나 스텝·블록이 남아 있음 — 종결로 처리해야 함 |

---

# GET `/api/v1/projects/{projectId}/progress` — 프로젝트 진척률 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 프로젝트 진척률 조회 |
| Method | GET |
| URL | `/api/v1/projects/{projectId}/progress` |
| 인증 필요 여부 | Y |
| 권한 | 참여자 |
| 요구사항 | PRJ-013 · INV-03 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
GET /api/v1/projects/12/progress
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `projectId` | Long | 프로젝트 ID |
| `totalStepCount` | int | 전체 스텝 수 |
| `doneStepCount` | int | 완료 스텝 수 |
| `progressRate` | Integer | 진척률(%) — **스텝 0개면 응답에 담지 않는다** |

**이슈 수는 계산식에 들어가지 않는다** (INV-03).

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": { "projectId":12, "totalStepCount":5, "doneStepCount":2, "progressRate":40 }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 진척률 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_ACCESS_DENIED` | 프로젝트 접근 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |

---

# POST `/api/v1/projects/{projectId}/business-categories` — 사업 카테고리 연결

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 사업 카테고리 연결 |
| Method | POST |
| URL | `/api/v1/projects/{projectId}/business-categories` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | PRJ-007 · USC-PBC-001·002 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `categoryIds` | List\<Long\> | Y | 연결할 사업 카테고리 ID 목록 |

같은 카테고리 중복은 `UNIQUE` 로 차단된다 (PRJ-007).

## Request Example
```
{ "categoryIds": [1,4] }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`201` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `projectId` | Long | 프로젝트 ID |
| `businessCategories` | List\<Object\> | 연결 후 전체 카테고리 |
| `businessCategories[].categoryId` | Long | 카테고리 ID |
| `businessCategories[].name` | String | 카테고리명 |
| `businessCategories[].code` | String | 업무코드 (`business_category.code` · `null` 허용) |

## Success Example
```
{
  "httpStatus":201,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "projectId":12,
    "businessCategories": [
      { "categoryId":1, "name":"환경", "code":"ENV" },
      { "categoryId":4, "name":"상하수도", "code":null }
    ]
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | - | 사업 카테고리 연결 성공 |
| 400 | Bad Request | `CATEGORY_IDS_REQUIRED` | 카테고리 ID 목록이 비어 있음 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |
| 404 | Not Found | `BUSINESS_CATEGORY_NOT_FOUND` | 사업 카테고리가 존재하지 않음 |
| 409 | Conflict | `BUSINESS_CATEGORY_DUPLICATED` | 이미 연결된 카테고리 |

---

# DELETE `/api/v1/projects/{projectId}/business-categories/{categoryId}` — 사업 카테고리 해제

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 사업 카테고리 해제 |
| Method | DELETE |
| URL | `/api/v1/projects/{projectId}/business-categories/{categoryId}` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | USC-PBC-003 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |
| `categoryId` | Long | Y | 해제할 사업 카테고리 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
DELETE /api/v1/projects/12/business-categories/4
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | `null` |

## Success Example
```
{ "httpStatus":200, "message":"요청이 성공적으로 처리되었습니다.", "data":null }
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 사업 카테고리 해제 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |
| 404 | Not Found | `BUSINESS_CATEGORY_NOT_LINKED` | 연결되지 않은 카테고리 |

---

# GET `/api/v1/projects/{projectId}/members` — 참여자 목록 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 참여자 목록 조회 |
| Method | GET |
| URL | `/api/v1/projects/{projectId}/members` |
| 인증 필요 여부 | Y |
| 권한 | 참여자 |
| 요구사항 | USC-MEM-001 · USC-MEM-008 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
GET /api/v1/projects/12/members
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `members` | List\<Object\> | 참여자 목록 |
| `members[].memberId` | Long | `project_member` 행 ID |
| `members[].userId` | String | 사원 사번 |
| `members[].name` | String | 이름 |
| `members[].department` | String | 부서 |
| `members[].permission` | String | `VIEWER`·`EDITOR`·`NONE` |
| `members[].resigned` | boolean | 퇴사자 여부 (배지 표시용) |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "members": [
      { "memberId":31, "userId":"E2024001", "name":"김용준", "department":"사업1팀", "permission":"EDITOR", "resigned":false },
      { "memberId":32, "userId":"E2024007", "name":"김동훈", "department":"사업1팀", "permission":"VIEWER", "resigned":false }
    ]
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 참여자 목록 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_ACCESS_DENIED` | 프로젝트 접근 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |

---

# POST `/api/v1/projects/{projectId}/members` — 참여자 추가

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 참여자 추가 |
| Method | POST |
| URL | `/api/v1/projects/{projectId}/members` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | PRJ-009 · PRJ-010 · INV-07 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `userId` | String | Y | 추가할 사원 사번 (**한 명씩**) |
| `permission` | String | Y | `VIEWER`·`EDITOR`·`NONE` |

⛔ **팀·부서 일괄 추가 파라미터를 만들지 않는다** (PRJ-009 · INV-07). `MANAGER` 를 보내면 400.

## Request Example
```
{ "userId":"E2024007", "permission":"VIEWER" }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`201` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `memberId` | Long | 생성된 참여자 행 ID |
| `userId` | String | 사원 사번 |
| `name` | String | 이름 |
| `permission` | String | 권한 등급 |

## Success Example
```
{
  "httpStatus":201,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": { "memberId":32, "userId":"E2024007", "name":"김동훈", "permission":"VIEWER" }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | - | 참여자 추가 성공 |
| 400 | Bad Request | `MEMBER_PERMISSION_INVALID` | 허용되지 않은 권한 등급 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |
| 404 | Not Found | `USER_NOT_FOUND` | 지정한 사용자가 존재하지 않음 |
| 409 | Conflict | `MEMBER_ALREADY_EXISTS` | 이미 참여자로 등록됨 |

---

# PATCH `/api/v1/projects/{projectId}/members/{memberId}` — 참여자 권한 변경

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 참여자 권한 변경 |
| Method | PATCH |
| URL | `/api/v1/projects/{projectId}/members/{memberId}` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | PRJ-010 · PRJ-011 · PRJ-012 · INV-10 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |
| `memberId` | Long | Y | 참여자 행 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `permission` | String | Y | `VIEWER`·`EDITOR`·`NONE` |

⛔ **자기 자신의 권한 행은 수정할 수 없다.** `EDITOR` 여도 403 (PRJ-011 · INV-10).

## Request Example
```
{ "permission":"EDITOR" }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `memberId` | Long | 참여자 행 ID |
| `userId` | String | 사원 사번 |
| `permission` | String | 변경된 권한 등급 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": { "memberId":32, "userId":"E2024007", "permission":"EDITOR" }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 참여자 권한 변경 성공 |
| 400 | Bad Request | `MEMBER_PERMISSION_INVALID` | 허용되지 않은 권한 등급 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `MEMBER_SELF_EDIT_DENIED` | 자기 자신의 권한 행은 수정할 수 없음 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `MEMBER_NOT_FOUND` | 참여자가 존재하지 않음 |

---

# DELETE `/api/v1/projects/{projectId}/members/{memberId}` — 참여자 제거

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 참여자 제거 |
| Method | DELETE |
| URL | `/api/v1/projects/{projectId}/members/{memberId}` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | USC-MEM-007 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |
| `memberId` | Long | Y | 참여자 행 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
DELETE /api/v1/projects/12/members/32
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | `null` |

## Success Example
```
{ "httpStatus":200, "message":"요청이 성공적으로 처리되었습니다.", "data":null }
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 참여자 제거 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `MEMBER_SELF_EDIT_DENIED` | 자기 자신은 제거할 수 없음 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `MEMBER_NOT_FOUND` | 참여자가 존재하지 않음 |

---

# GET `/api/v1/projects/{projectId}/stages` — 스테이지 목록 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스테이지 목록 조회 |
| Method | GET |
| URL | `/api/v1/projects/{projectId}/stages` |
| 인증 필요 여부 | Y |
| 권한 | 참여자 |
| 요구사항 | STG-001 · USC-STG-001 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
GET /api/v1/projects/12/stages
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `stages` | List\<Object\> | 스테이지 목록 (`sortOrder` 오름차순) |
| `stages[].stageId` | Long | 스테이지 ID |
| `stages[].name` | String | 스테이지명 |
| `stages[].sortOrder` | int | 정렬 순서 |
| `stages[].stepCount` | int | 소속 스텝 수 |

⚠️ 스테이지는 **권한·상태를 저장하지 않는다** (STG-004 · INV-01). 응답에 그 필드가 없다.

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "stages": [
      { "stageId":7, "name":"제안", "sortOrder":1, "stepCount":3 },
      { "stageId":8, "name":"수행", "sortOrder":2, "stepCount":2 }
    ]
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 스테이지 목록 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_ACCESS_DENIED` | 프로젝트 접근 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |

---

# POST `/api/v1/projects/{projectId}/stages` — 스테이지 생성

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스테이지 생성 |
| Method | POST |
| URL | `/api/v1/projects/{projectId}/stages` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | STG-001 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `name` | String | Y | 스테이지명 |
| `sortOrder` | int | N | 정렬 순서. 미지정 시 `max+1` |

## Request Example
```
{ "name":"제안" }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`201` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `stageId` | Long | 생성된 스테이지 ID |
| `projectId` | Long | 프로젝트 ID |
| `name` | String | 스테이지명 |
| `sortOrder` | int | 정렬 순서 |

## Success Example
```
{
  "httpStatus":201,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": { "stageId":7, "projectId":12, "name":"제안", "sortOrder":1 }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | - | 스테이지 생성 성공 |
| 400 | Bad Request | `STAGE_NAME_REQUIRED` | 스테이지명이 입력되지 않음 |
| 400 | Bad Request | `STAGE_NAME_TOO_LONG` | 스테이지명이 100자를 초과함 (2026-08-04 신설 — `stage.name VARCHAR(100)` 제약에 대한 에러코드가 빠져 있어 초과 입력이 500 으로 샜다, `PROJECT_NAME_TOO_LONG` 선례) |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |

---

# PATCH `/api/v1/stages/{stageId}` — 스테이지 수정

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스테이지 수정 |
| Method | PATCH |
| URL | `/api/v1/stages/{stageId}` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | STG-001 · USC-STG-003 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stageId` | Long | Y | 스테이지 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `name` | String | Y | 스테이지명 |

## Request Example
```
{ "name":"제안·계약" }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `stageId` | Long | 스테이지 ID |
| `name` | String | 스테이지명 |
| `sortOrder` | int | 정렬 순서 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": { "stageId":7, "name":"제안·계약", "sortOrder":1 }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 스테이지 수정 성공 |
| 400 | Bad Request | `STAGE_NAME_REQUIRED` | 스테이지명이 입력되지 않음 |
| 400 | Bad Request | `STAGE_NAME_TOO_LONG` | 스테이지명이 100자를 초과함 (2026-08-04 신설) |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `STAGE_NOT_FOUND` | 스테이지가 존재하지 않음 |

---

# PATCH `/api/v1/projects/{projectId}/stages/order` — 스테이지 순서 변경

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스테이지 순서 변경 |
| Method | PATCH |
| URL | `/api/v1/projects/{projectId}/stages/order` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | STG-002 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `orders` | List\<Object\> | Y | 재정렬 대상 목록 |
| `orders[].stageId` | Long | Y | 스테이지 ID |
| `orders[].sortOrder` | int | Y | 새 정렬 순서 |

`sort_order` 만 갱신한다. **하위 스텝은 건드리지 않는다** (STG-002).

## Request Example
```
{
  "orders": [
    { "stageId":8, "sortOrder":1 },
    { "stageId":7, "sortOrder":2 }
  ]
}
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `stages` | List\<Object\> | 재정렬 결과 |
| `stages[].stageId` | Long | 스테이지 ID |
| `stages[].sortOrder` | int | 정렬 순서 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": { "stages": [ { "stageId":8, "sortOrder":1 }, { "stageId":7, "sortOrder":2 } ] }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 스테이지 순서 변경 성공 |
| 400 | Bad Request | `STAGE_ORDER_INVALID` | 순서 목록이 비었거나 중복된 순서 값 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `STAGE_NOT_FOUND` | 스테이지가 존재하지 않음 |

---

# DELETE `/api/v1/stages/{stageId}` — 스테이지 삭제

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스테이지 삭제 |
| Method | DELETE |
| URL | `/api/v1/stages/{stageId}` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | STG-003 · USC-STG-006·007 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stageId` | Long | Y | 삭제할 스테이지 ID |

## Request Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `moveToStageId` | Long | Y | 하위 스텝을 옮길 스테이지 ID. `0` 이면 미소속(`stage_id = NULL`)으로 이전 |

⛔ **스텝이 함께 삭제되지 않는다.** 이전 대상을 지정하지 않으면 400 (STG-003).

## Request Body
없음

## Request Example
```
DELETE /api/v1/stages/7?moveToStageId=8
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `deletedStageId` | Long | 삭제된 스테이지 ID |
| `movedStepCount` | int | 이전된 스텝 수 |
| `moveToStageId` | Long | 이전 대상 스테이지 ID (`null` 이면 미소속) |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": { "deletedStageId":7, "movedStepCount":3, "moveToStageId":8 }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 스테이지 삭제 성공 |
| 400 | Bad Request | `STAGE_MOVE_TARGET_REQUIRED` | 하위 스텝 이전 대상이 지정되지 않음 |
| 400 | Bad Request | `STAGE_MOVE_TARGET_INVALID` | 이전 대상이 다른 프로젝트이거나 자기 자신 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `STAGE_NOT_FOUND` | 스테이지가 존재하지 않음 |

---

# POST `/api/v1/stages/{stageId}/step-permissions` — 하위 스텝 권한 일괄 적용

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 하위 스텝 권한 일괄 적용 |
| Method | POST |
| URL | `/api/v1/stages/{stageId}/step-permissions` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | STG-004 · USC-STG-008 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stageId` | Long | Y | 스테이지 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `userId` | String | Y | 대상 사원 사번 (VARCHAR(20)) |
| `permission` | String | Y | `VIEWER`·`EDITOR`·`NONE` |

⚠️ **`stage_permission` 테이블은 없다.** 이 요청은 하위 스텝 각각의 `step_permission` 행으로 떨어진다 (STG-004 · INV-01).

## Request Example
```
{ "userId":"E2024007", "permission":"EDITOR" }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`201` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `stageId` | Long | 스테이지 ID |
| `userId` | String | 사원 사번 |
| `permission` | String | 적용된 권한 등급 |
| `appliedStepCount` | int | 권한이 적용된 스텝 수 |

## Success Example
```
{
  "httpStatus":201,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": { "stageId":7, "userId":"E2024007", "permission":"EDITOR", "appliedStepCount":3 }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | - | 권한 일괄 적용 성공 |
| 400 | Bad Request | `STEP_PERMISSION_INVALID` | 허용되지 않은 권한 등급 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `STAGE_NOT_FOUND` | 스테이지가 존재하지 않음 |
| 404 | Not Found | `USER_NOT_FOUND` | 지정한 사용자가 존재하지 않음 |

---

# GET `/api/v1/projects/{projectId}/steps` — 스텝 목록 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스텝 목록 조회 |
| Method | GET |
| URL | `/api/v1/projects/{projectId}/steps` |
| 인증 필요 여부 | Y |
| 권한 | 참여자 |
| 요구사항 | STP-001 · STP-011 · USC-SPM-005 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |

## Request Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stageId` | Long | N | 특정 스테이지의 스텝만 조회. `0` 이면 미소속 스텝 |
| `status` | String | N | `NOT_STARTED`·`IN_PROGRESS`·`DONE` |

**`step_permission` 이 `NONE` 인 스텝은 목록에서 제외된다** (STP-010 · USC-SPM-005).

## Request Body
없음

## Request Example
```
GET /api/v1/projects/12/steps?stageId=7
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `steps` | List\<Object\> | 스텝 목록 |
| `steps[].stepId` | Long | 스텝 ID |
| `steps[].stageId` | Long | 소속 스테이지 ID (`null` 허용) |
| `steps[].name` | String | 스텝명 |
| `steps[].status` | String | 스텝 상태 |
| `steps[].sortOrder` | int | 정렬 순서 |
| `steps[].startedOn` | LocalDate | 시작일 |
| `steps[].endedOn` | LocalDate | 종료일 |
| `steps[].owner` | Object | 책임자 (`userId`·`name`) |
| `steps[].totalIssueCount` | int | 전체 이슈 수 |
| `steps[].doneIssueCount` | int | 완료 이슈 수 (`issue.status = DONE`) |
| ⭐ `steps[].inProgressIssueCount` | int | 진행 중 이슈 수 (`issue.status = IN_PROGRESS`) — **2026-08-05 신설.** FE 스텝 진행바가 완료 🟡 / 진행 중 🔵 / 진행 전 ⬜ **3색**인데 기존 2필드로는 3분할을 못 그렸다. 진행 전 = `total - done - inProgress` |
| `steps[].progressRate` | Integer | 스텝 진척률(%) — **이슈 0개면 미포함**. 분자는 `doneIssueCount` 만 쓴다 (진행 중은 세지 않는다) |
| `steps[].myPermission` | String | 요청자의 스텝 권한 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "steps": [
      {
        "stepId":10,
        "stageId":7,
        "name":"제안서 작성",
        "status":"IN_PROGRESS",
        "sortOrder":1,
        "startedOn":"2026-08-01",
        "endedOn":"2026-08-10",
        "owner": { "userId":"E2024001", "name":"김용준" },
        "totalIssueCount":5,
        "doneIssueCount":2,
        "inProgressIssueCount":2,
        "progressRate":40,
        "myPermission":"EDITOR"
      }
    ]
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 스텝 목록 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_ACCESS_DENIED` | 프로젝트 접근 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |

---

# GET `/api/v1/steps/{stepId}` — 스텝 상세 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스텝 상세 조회 |
| Method | GET |
| URL | `/api/v1/steps/{stepId}` |
| 인증 필요 여부 | Y |
| 권한 | 스텝 접근 권한 |
| 요구사항 | STP-003 · STP-004 · STP-012 · INV-04 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stepId` | Long | Y | 스텝 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
GET /api/v1/steps/10
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `stepId` | Long | 스텝 ID |
| `projectId` | Long | 소속 프로젝트 ID (`NOT NULL`) |
| `stageId` | Long | 소속 스테이지 ID (`null` 허용) |
| `name` | String | 스텝명 |
| `status` | String | `NOT_STARTED`·`IN_PROGRESS`·`DONE` |
| `startedOn` | LocalDate | 시작일 |
| `endedOn` | LocalDate | 종료일 |
| `owner` | Object | 책임자 — `employee_id` 기준. **작업자가 아니다** |
| `totalIssueCount` | int | 전체 이슈 수 |
| `doneIssueCount` | int | 완료 이슈 수 (`issue.status = DONE`) |
| ⭐ `inProgressIssueCount` | int | 진행 중 이슈 수 (`issue.status = IN_PROGRESS`) — **2026-08-05 신설** (목록 조회와 동일) |
| `progressRate` | Integer | 스텝 진척률(%) — **이슈 0개면 응답에 담지 않는다** (INV-04). 분자는 `doneIssueCount` 만 쓴다 |
| `completedBy` | Object | 완료자 |
| `completedAt` | LocalDateTime | 완료 시각 |
| `myPermission` | String | 요청자의 스텝 권한 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "stepId":10,
    "projectId":12,
    "stageId":7,
    "name":"제안서 작성",
    "status":"IN_PROGRESS",
    "startedOn":"2026-08-01",
    "endedOn":"2026-08-10",
    "owner": { "userId":"E2024001", "name":"김용준" },
    "totalIssueCount":5,
    "doneIssueCount":2,
    "inProgressIssueCount":2,
    "progressRate":40,
    "completedBy":null,
    "completedAt":null,
    "myPermission":"EDITOR"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 스텝 상세 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `STEP_ACCESS_DENIED` | 스텝 접근 권한 없음 (`NONE`) |
| 404 | Not Found | `STEP_NOT_FOUND` | 스텝이 존재하지 않음 |

---

# POST `/api/v1/projects/{projectId}/steps` — 스텝 생성

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스텝 생성 |
| Method | POST |
| URL | `/api/v1/projects/{projectId}/steps` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | STP-001 · STP-003 · INV-02 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `name` | String | Y | 스텝명 (**최대 200자** · `step.name`) |
| `stageId` | Long | N | 소속 스테이지 ID. 미지정 시 미소속(`NULL`) |
| `startedOn` | LocalDate | N | 시작일 (`started_on`) |
| `endedOn` | LocalDate | N | 종료일 (`ended_on`) ✅ ERD 로 확정 |
| `ownerUserId` | String | N | 책임자 **사번** (`owner_user_id VARCHAR(20)`). 책임자는 **작업자가 아니다** |

`step.project_id` 는 시스템이 **항상 채운다** (INV-02). 상태는 `NOT_STARTED` 로 시작한다.
⛔ 템플릿 적용 파라미터는 없다 — v1 은 **직접 추가만** (`PRJ-V1.md` §1-2).
⛔ `stepType` 파라미터는 **없다** — 송부 스텝 폐기 (2026-08-03 · `PRJ-V1.md` STP-007).

## Request Example
```
{
  "name":"제안서 작성",
  "stageId":7,
  "startedOn":"2026-08-01",
  "endedOn":"2026-08-10",
  "ownerUserId":"E2024001"
}
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`201` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `stepId` | Long | 생성된 스텝 ID |
| `projectId` | Long | 소속 프로젝트 ID |
| `stageId` | Long | 소속 스테이지 ID |
| `name` | String | 스텝명 |
| `status` | String | `NOT_STARTED` |
| `sortOrder` | int | 정렬 순서 |
| `startedOn` | LocalDate | 시작일 |
| `endedOn` | LocalDate | 종료일 |
| `owner` | Object | 책임자 |
| `createdAt` | LocalDateTime | 생성 일시 |

## Success Example
```
{
  "httpStatus":201,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "stepId":10,
    "projectId":12,
    "stageId":7,
    "name":"제안서 작성",
    "status":"NOT_STARTED",
    "sortOrder":1,
    "startedOn":"2026-08-01",
    "endedOn":"2026-08-10",
    "owner": { "userId":"E2024001", "name":"김용준" },
    "createdAt":"2026-08-01T10:00:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | - | 스텝 생성 성공 |
| 400 | Bad Request | `STEP_NAME_REQUIRED` | 스텝명이 입력되지 않음 |
| 400 | Bad Request | `STEP_NAME_TOO_LONG` | 스텝명이 200자를 초과함 (2026-08-05 신설 — `step.name VARCHAR(200)` 제약에 대한 에러코드가 빠져 있어 초과 입력이 500 으로 샜다, `STAGE_NAME_TOO_LONG` 선례) |
| 400 | Bad Request | `STEP_DATE_RANGE_INVALID` | 시작일이 종료일보다 늦음 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |
| 404 | Not Found | `STAGE_NOT_FOUND` | 스테이지가 존재하지 않음 |
| 404 | Not Found | `USER_NOT_FOUND` | 지정한 책임자가 존재하지 않음 |

---

# PATCH `/api/v1/steps/{stepId}` — 스텝 수정

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스텝 수정 |
| Method | PATCH |
| URL | `/api/v1/steps/{stepId}` |
| 인증 필요 여부 | Y |
| 권한 | 스텝 EDITOR |
| 요구사항 | STP-001 · STP-003 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stepId` | Long | Y | 스텝 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `name` | String | N | 스텝명 (최대 200자) |
| `stageId` | Long | N | 소속 스테이지 변경. `0` 이면 미소속 |
| `startedOn` | LocalDate | N | 시작일 |
| `endedOn` | LocalDate | N | 종료일 |
| `ownerUserId` | String | N | 책임자 **사번** (`VARCHAR(20)` · 작업자 아님) |

## Request Example
```
{ "name":"제안서 작성·검토", "endedOn":"2026-08-12", "ownerUserId":"E2024007" }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `stepId` | Long | 스텝 ID |
| `name` | String | 스텝명 |
| `stageId` | Long | 소속 스테이지 ID |
| `startedOn` | LocalDate | 시작일 |
| `endedOn` | LocalDate | 종료일 |
| `owner` | Object | 책임자 |
| `updatedAt` | LocalDateTime | 수정 일시 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "stepId":10,
    "name":"제안서 작성·검토",
    "stageId":7,
    "startedOn":"2026-08-01",
    "endedOn":"2026-08-12",
    "owner": { "userId":"E2024007", "name":"김동훈" },
    "updatedAt":"2026-08-01T13:00:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 스텝 수정 성공 |
| 400 | Bad Request | `STEP_NAME_TOO_LONG` | 스텝명이 200자를 초과함 (2026-08-05 신설) |
| 400 | Bad Request | `STEP_DATE_RANGE_INVALID` | 시작일이 종료일보다 늦음 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `STEP_EDIT_DENIED` | 스텝 편집 권한 없음 |
| 404 | Not Found | `STEP_NOT_FOUND` | 스텝이 존재하지 않음 |
| 404 | Not Found | `STAGE_NOT_FOUND` | 스테이지가 존재하지 않음 |
| 404 | Not Found | `USER_NOT_FOUND` | 지정한 책임자가 존재하지 않음 |

---

# PATCH `/api/v1/projects/{projectId}/steps/order` — 스텝 순서 변경

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스텝 순서 변경 |
| Method | PATCH |
| URL | `/api/v1/projects/{projectId}/steps/order` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | STP-002 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `orders` | List\<Object\> | Y | 재정렬 대상 목록 |
| `orders[].stepId` | Long | Y | 스텝 ID |
| `orders[].stageId` | Long | N | 이동할 스테이지 ID. `0` 이면 미소속 |
| `orders[].sortOrder` | int | Y | 새 정렬 순서 |

⚠️ **선행 스텝 완료를 검사하지 않는다** (STP-002).

## Request Example
```
{
  "orders": [
    { "stepId":11, "stageId":7, "sortOrder":1 },
    { "stepId":10, "stageId":7, "sortOrder":2 }
  ]
}
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `steps` | List\<Object\> | 재정렬 결과 |
| `steps[].stepId` | Long | 스텝 ID |
| `steps[].stageId` | Long | 소속 스테이지 ID |
| `steps[].sortOrder` | int | 정렬 순서 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "steps": [
      { "stepId":11, "stageId":7, "sortOrder":1 },
      { "stepId":10, "stageId":7, "sortOrder":2 }
    ]
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 스텝 순서 변경 성공 |
| 400 | Bad Request | `STEP_ORDER_INVALID` | 순서 목록이 비었거나 중복된 순서 값 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `STEP_NOT_FOUND` | 스텝이 존재하지 않음 |

---

# PATCH `/api/v1/steps/{stepId}/status` — 스텝 상태 변경

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스텝 상태 변경 |
| Method | PATCH |
| URL | `/api/v1/steps/{stepId}/status` |
| 인증 필요 여부 | Y |
| 권한 | 스텝 EDITOR |
| 요구사항 | STP-004 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stepId` | Long | Y | 스텝 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `status` | String | Y | `NOT_STARTED`·`IN_PROGRESS` |

⚠️ **`DONE` 은 이 API 로 설정하지 않는다.** 미완료 이슈 처리 선택이 필요하므로 완료 처리 API 를 쓴다 (STP-006).
스텝 상태는 **스텝 진척률과 별개 값**이다 (STP-004).

## Request Example
```
{ "status":"IN_PROGRESS" }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `stepId` | Long | 스텝 ID |
| `status` | String | 변경된 상태 |
| `updatedAt` | LocalDateTime | 변경 일시 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": { "stepId":10, "status":"IN_PROGRESS", "updatedAt":"2026-08-01T13:10:00" }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 스텝 상태 변경 성공 |
| 400 | Bad Request | `STEP_STATUS_INVALID` | 허용되지 않은 상태 값 (`DONE` 포함) |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `STEP_EDIT_DENIED` | 스텝 편집 권한 없음 |
| 404 | Not Found | `STEP_NOT_FOUND` | 스텝이 존재하지 않음 |

---

# POST `/api/v1/steps/{stepId}/complete` — 스텝 완료 처리

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스텝 완료 처리 |
| Method | POST |
| URL | `/api/v1/steps/{stepId}/complete` |
| 인증 필요 여부 | Y |
| 권한 | 스텝 EDITOR |
| 요구사항 | STP-005 · STP-006 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stepId` | Long | Y | 스텝 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `openIssueAction` | String | Y | `KEEP`(그대로 두기) · `CLOSE`(함께 종료) |

**이슈가 미완료여도 스텝을 완료할 수 있다** (STP-005). `KEEP` 이면 남은 이슈에 `완료된 스텝` 배지가 붙는다.
완료자·완료시각은 `completed_by`·`completed_at` 에 기록된다 (STP-005).

## Request Example
```
{ "openIssueAction":"KEEP" }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `stepId` | Long | 스텝 ID |
| `status` | String | `DONE` |
| `openIssueCount` | int | 완료 시점의 미완료 이슈 수 |
| `openIssueAction` | String | 적용된 처리 방식 |
| `closedIssueCount` | int | 함께 종료된 이슈 수 (`KEEP` 이면 0) |
| `completedBy` | Object | 완료자 |
| `completedAt` | LocalDateTime | 완료 시각 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "stepId":10,
    "status":"DONE",
    "openIssueCount":3,
    "openIssueAction":"KEEP",
    "closedIssueCount":0,
    "completedBy": { "userId":"E2024001", "name":"김용준" },
    "completedAt":"2026-08-10T17:00:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 스텝 완료 처리 성공 |
| 400 | Bad Request | `OPEN_ISSUE_ACTION_REQUIRED` | 미완료 이슈 처리 방식이 지정되지 않음 |
| 400 | Bad Request | `OPEN_ISSUE_ACTION_INVALID` | 허용되지 않은 처리 방식 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `STEP_EDIT_DENIED` | 스텝 편집 권한 없음 |
| 404 | Not Found | `STEP_NOT_FOUND` | 스텝이 존재하지 않음 |

---

# DELETE `/api/v1/steps/{stepId}` — 스텝 삭제

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스텝 삭제 |
| Method | DELETE |
| URL | `/api/v1/steps/{stepId}` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | STP-008 · STP-009 · PCB-006 · TXL-010 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stepId` | Long | Y | 삭제할 스텝 ID |

## Request Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `issueAction` | String | Y | `MOVE`(이동) · `CLOSE`(함께 종료) |
| `moveToStepId` | Long | N | `issueAction=MOVE` 일 때 필수 — 이슈를 옮길 스텝 ID |

⛔ **조용히 같이 지우지 않는다.** 미선택이면 400 (STP-008).
잠금 블록(입금 연결 입금확인 · 진행 중 결재 · 결재 대상 파일 · 계산서 연결 조회)이 있으면 409 (STP-009 · PCB-006 · TXL-010).

## Request Body
없음

## Request Example
```
DELETE /api/v1/steps/10?issueAction=MOVE&moveToStepId=11
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `deletedStepId` | Long | 삭제된 스텝 ID |
| `issueAction` | String | 적용된 이슈 처리 방식 |
| `movedIssueCount` | int | 이동된 이슈 수 |
| `closedIssueCount` | int | 종료된 이슈 수 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": { "deletedStepId":10, "issueAction":"MOVE", "movedIssueCount":3, "closedIssueCount":0 }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 스텝 삭제 성공 |
| 400 | Bad Request | `ISSUE_ACTION_REQUIRED` | 이슈 처리 방식이 지정되지 않음 |
| 400 | Bad Request | `ISSUE_MOVE_TARGET_REQUIRED` | 이동 대상 스텝이 지정되지 않음 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `STEP_NOT_FOUND` | 스텝이 존재하지 않음 |
| 409 | Conflict | `STEP_DELETE_LOCKED` | 잠금 블록이 있어 삭제할 수 없음 |

---

# GET `/api/v1/steps/{stepId}/permissions` — 스텝 권한 목록 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스텝 권한 목록 조회 |
| Method | GET |
| URL | `/api/v1/steps/{stepId}/permissions` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | STP-010 · STP-011 · USC-SPM-004 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stepId` | Long | Y | 스텝 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
GET /api/v1/steps/10/permissions
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `permissions` | List\<Object\> | 참여자별 판정 결과 |
| `permissions[].userId` | String | 사원 사번 |
| `permissions[].name` | String | 이름 |
| `permissions[].permission` | String | 최종 판정 등급 |
| `permissions[].overridden` | boolean | `step_permission` 행 보유 여부. `false` 면 **프로젝트 권한 상속** (STP-011) |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "permissions": [
      { "userId":"E2024001", "name":"김용준", "permission":"EDITOR", "overridden":false },
      { "userId":"E2024007", "name":"김동훈", "permission":"NONE", "overridden":true }
    ]
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 스텝 권한 목록 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `STEP_NOT_FOUND` | 스텝이 존재하지 않음 |

---

# PUT `/api/v1/steps/{stepId}/permissions/{userId}` — 스텝 권한 부여·변경

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스텝 권한 부여·변경 |
| Method | PUT |
| URL | `/api/v1/steps/{stepId}/permissions/{userId}` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | STP-010 · USC-SPM-001·002 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stepId` | Long | Y | 스텝 ID |
| `userId` | String | Y | 대상 사원 사번 (VARCHAR(20)) |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `permission` | String | Y | `VIEWER`·`EDITOR`·`NONE` |

특정 스텝만 가리려면 **`NONE` 행을 명시적으로** 넣어야 한다 (STP-011). 행이 없으면 차단이 아니라 상속이다.
자기 자신의 권한 행은 수정할 수 없다 (INV-10).

## Request Example
```
{ "permission":"NONE" }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `stepId` | Long | 스텝 ID |
| `userId` | String | 사원 사번 |
| `permission` | String | 적용된 권한 등급 |
| `overridden` | boolean | `true` (행이 생성/갱신됨) |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": { "stepId":10, "userId":"E2024007", "permission":"NONE", "overridden":true }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 스텝 권한 설정 성공 |
| 400 | Bad Request | `STEP_PERMISSION_INVALID` | 허용되지 않은 권한 등급 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `MEMBER_SELF_EDIT_DENIED` | 자기 자신의 권한은 수정할 수 없음 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `STEP_NOT_FOUND` | 스텝이 존재하지 않음 |
| 404 | Not Found | `USER_NOT_FOUND` | 지정한 사용자가 존재하지 않음 |

---

# DELETE `/api/v1/steps/{stepId}/permissions/{userId}` — 스텝 권한 회수

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스텝 권한 회수 |
| Method | DELETE |
| URL | `/api/v1/steps/{stepId}/permissions/{userId}` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 EDITOR |
| 요구사항 | USC-SPM-003 · STP-011 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stepId` | Long | Y | 스텝 ID |
| `userId` | String | Y | 대상 사원 사번 (VARCHAR(20)) |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
DELETE /api/v1/steps/10/permissions/E2024007
```

오버라이드 행을 지우면 **프로젝트 권한 상속으로 되돌아간다** (STP-011). 차단이 아니다.

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `stepId` | Long | 스텝 ID |
| `userId` | String | 사원 사번 |
| `permission` | String | 회수 후 상속된 등급 |
| `overridden` | boolean | `false` |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": { "stepId":10, "userId":"E2024007", "permission":"VIEWER", "overridden":false }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 스텝 권한 회수 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_EDIT_DENIED` | 프로젝트 편집 권한 없음 |
| 404 | Not Found | `STEP_PERMISSION_NOT_FOUND` | 오버라이드 행이 없음 |

---

# GET `/api/v1/steps/{stepId}/blocks` — 스텝 블록 일괄 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 스텝 블록 일괄 조회 |
| Method | GET |
| URL | `/api/v1/steps/{stepId}/blocks` |
| 인증 필요 여부 | Y |
| 권한 | 스텝 접근 권한 |
| 요구사항 | BLK-005 · BLK-006 · BLK-011 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stepId` | Long | Y | 스텝 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
GET /api/v1/steps/10/blocks
```

블록과 타입별 상세가 **한 번에** 내려온다. `LEFT JOIN` 한 방으로 처리하고 타입 수만큼 쿼리를 늘리지 않는다 (BLK-006).

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `blocks` | List\<Object\> | 블록 목록 (`rowIndex`·`sortOrder` 순) |
| `blocks[].blockId` | Long | 블록 ID |
| `blocks[].type` | String | 블록 타입 (ERD enum **10값** · ⛔ `MEMO` 폐기) |
| `blocks[].title` | String | 블록 제목 |
| `blocks[].owner` | Object | 블록 담당자 (`userId` **사번**·`name`). 미지정이면 `null` (BLK-012) |
| `blocks[].rowIndex` | int | 행 인덱스 |
| `blocks[].sortOrder` | int | 행 내 순서 |
| `blocks[].colSpan` | int | 열 병합 수 (1~3) |
| `blocks[].detail` | Object | 타입별 상세 (타입마다 구조가 다르다) |
| `blocks[].linkedIssueTotal` | int | 연결된 이슈 수 |
| `blocks[].linkedIssueDone` | int | 연결된 이슈 중 완료 수 |

⚠️ **`status` 필드가 없다.** 블록은 자체 진행 상태를 갖지 않는다 (BLK-005).
⛔ **`typeId` 를 내리지 않는다.** `block.type_id` 는 존재하지만 **다형성 내부 식별자**라 프론트에 노출하지 않는다 ([`ERD.md`](ERD.md) §5-2).
⛔ **`projectId` 도 내리지 않는다.** `block.project_id` 는 폐기됐다 ([`ERD.md`](ERD.md) §0-13).

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "blocks": [
      {
        "blockId":15,
        "type":"FILE",
        "title":"제안서 문서",
        "owner": { "userId":"E2024001", "name":"김용준" },
        "rowIndex":0,
        "sortOrder":0,
        "colSpan":2,
        "detail": { "fileCount":3 },
        "linkedIssueTotal":5,
        "linkedIssueDone":2
      }
    ]
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 블록 목록 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `STEP_ACCESS_DENIED` | 스텝 접근 권한 없음 |
| 404 | Not Found | `STEP_NOT_FOUND` | 스텝이 존재하지 않음 |

---

# POST `/api/v1/steps/{stepId}/blocks` — 블록 생성

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 블록 생성 |
| Method | POST |
| URL | `/api/v1/steps/{stepId}/blocks` |
| 인증 필요 여부 | Y |
| 권한 | 스텝 EDITOR |
| 요구사항 | BLK-001 · BLK-002 · BLK-003 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stepId` | Long | Y | 블록을 생성할 스텝 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `type` | String | Y | 블록 타입. **ERD `block.type` enum 안에서만** (아래 표) |
| `title` | String | N | 블록 제목 (**최대 200자**. 입금확인 블록에서는 **회차명**) |
| `owner` | String | N | 블록 담당자 **사번** (`block.owner VARCHAR(20)`). **선택 입력** (BLK-012) |
| `rowIndex` | int | N | 행 인덱스. 미지정 시 맨 아래 |
| `sortOrder` | int | N | 행 내 순서 |
| `colSpan` | int | N | 열 병합 수 (1~3). 기본 1 |

**`type` 허용값 — ✅ 10종 (2026-08-03 · `MEMO` 폐기 · `BID_NOTICE` 신설)**

| 값 | 상세 테이블 (`type_id` 대상 PK) | 자식 (1:N) | 담당 |
| --- | --- | --- | --- |
| `TEXT` | `text` (`txt_id`) | — | 정림 |
| `IMAGE` | `image_block` (`img_block_id`) | `image` | 정림 |
| `CHECKLIST` | `checklist_block` (`chk_block_id`) | `checklist` | 정림 |
| `FILE` | `block_file` (**`type_id` NULL** · 복합 PK) | — | 김동현 |
| `PAYMENT_CONFIRM` | `block_payment_confirm` (`payment_block_id`) | `payment` (N:1) | 동훈 |
| `TAX_INVOICE_VIEW` | `tax_invoice_confirm` (`tax_invoice_block_id`) | — | 동훈 |
| `PERFORMANCE_VIEW` | **없음** 🚨 T2 미결 (**`type_id` NULL**) | — | 동훈 |
| `APPROVAL` | `approval` (`approval_id`) | `approval_revision` | 이강욱 |
| `AI` | `vitamate_block` (`vitamate_block_id`) | `vitamate_analysis` | 정현 |
| **`BID_NOTICE`** ⭐ | **`bid_notice_block`** (`bid_notice_block_id`) | — | 정현 |

> 상세 테이블은 **전부 1:1** 이다. 다중 항목은 자식 테이블이 받는다 → [`ERD.md`](ERD.md) §5-2
> ⚠️ **`BID_NOTICE` 는 사용자가 직접 만들 수 없다.** 공고→프로젝트 전환 API 가 자동 생성한다 (`BID-V1` CNV-06).
> 이 엔드포인트의 `type` 선택 목록에서 **빼라** — 보내면 400 이다.

> ⛔ **`MEMO` 는 폐기됐다 (2026-08-03).** enum 에 없고, **FE 타입 선택 목록에서도 빼라.**
> 자유 서술은 `TEXT` 가 담당한다 ([`BLOCK.md`](../../global/BLOCK.md) §4-1 *"본문·목차·회의 메모"*).
> ✅ [`BLOCK.md`](../../global/BLOCK.md) 10종 카탈로그와 [`ERD.md`](ERD.md) enum 이 일치한다 (`PRJ-V1-API.md` §4-B).

블록은 **항상 스텝에 붙는다** (BLK-002). ⛔ **`block.project_id` 는 없다** (2026-08-03 폐기) — 프로젝트는 `step` 을 조인해 얻는다.
⭐ `block.type_id` 는 **있다** (2026-08-03 재확정 · 다형성 양방향 ID). 시스템이 상세 행 INSERT 후 채운다 — **응답에 `typeId` 를 내리지 않는다** (내부 식별자).
⛔ **확장형 JSON 스키마 입력 경로가 없다** (BLK-001).

## Request Example
```
{
  "type":"CHECKLIST",
  "title":"제출 서류 점검",
  "rowIndex":0,
  "sortOrder":1,
  "colSpan":1
}
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`201` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `blockId` | Long | 생성된 블록 ID |
| `stepId` | Long | 소속 스텝 ID |
| `projectId` | Long | 소속 프로젝트 ID |
| `type` | String | 블록 타입 |
| `title` | String | 블록 제목 |
| `owner` | Object | 블록 담당자 (`userId`·`name`). 미지정이면 `null` |
| `rowIndex` | int | 행 인덱스 |
| `sortOrder` | int | 행 내 순서 |
| `colSpan` | int | 열 병합 수 |
| `createdAt` | LocalDateTime | 생성 일시 |

## Success Example
```
{
  "httpStatus":201,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "blockId":21,
    "stepId":10,
    "projectId":12,
    "type":"CHECKLIST",
    "title":"제출 서류 점검",
    "owner":null,
    "rowIndex":0,
    "sortOrder":1,
    "colSpan":1,
    "createdAt":"2026-08-01T14:00:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | - | 블록 생성 성공 |
| 400 | Bad Request | `BLOCK_TYPE_INVALID` | 정의된 **10종** 밖의 타입 (`MEMO` 포함) |
| 400 | Bad Request | `BLOCK_COL_SPAN_INVALID` | `colSpan` 이 1~3 범위 밖 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `STEP_EDIT_DENIED` | 스텝 편집 권한 없음 |
| 404 | Not Found | `STEP_NOT_FOUND` | 스텝이 존재하지 않음 |
| 404 | Not Found | `USER_NOT_FOUND` | 지정한 담당자(`owner`)가 존재하지 않음 |
| 409 | Conflict | `PAYMENT_CONFIRM_BLOCK_DUPLICATED` | 입금확인 블록은 스텝당 1개만 (PCB-001B) |
| 409 | Conflict | `TAX_INVOICE_VIEW_BLOCK_DUPLICATED` | 세금계산서 조회 블록은 스텝당 1개만 (**TXL-001B**) |

> ⚠️ **`TAX_INVOICE_VIEW` 도 스텝당 1개다** (`TAX-V1.md` TXL-001B · `PAY-V1.md` INV-07C).
> 이 API 가 두 타입 모두 스텝당 1개를 검사해야 한다 → [`../재무관리/FIN-V1-API.md`](../재무관리/FIN-V1-API.md) §1-2

---

# PATCH `/api/v1/steps/{stepId}/blocks/layout` — 블록 배치 변경

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 블록 배치 변경 |
| Method | PATCH |
| URL | `/api/v1/steps/{stepId}/blocks/layout` |
| 인증 필요 여부 | Y |
| 권한 | 스텝 EDITOR |
| 요구사항 | BLK-003 · BLK-004 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stepId` | Long | Y | 스텝 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `layouts` | List\<Object\> | Y | 배치 목록 |
| `layouts[].blockId` | Long | Y | 블록 ID |
| `layouts[].rowIndex` | int | Y | 행 인덱스 |
| `layouts[].sortOrder` | int | Y | 행 내 순서 |
| `layouts[].colSpan` | int | Y | 열 병합 수 (1~3) |

**총 열 수는 3 고정**이다 (BLK-003). `UNIQUE(step_id,row_index,sort_order)` 를 걸지 않아 드래그 중간 중복이 허용된다 (BLK-004).

## Request Example
```
{
  "layouts": [
    { "blockId":15, "rowIndex":0, "sortOrder":0, "colSpan":2 },
    { "blockId":21, "rowIndex":0, "sortOrder":1, "colSpan":1 }
  ]
}
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `blocks` | List\<Object\> | 반영된 배치 |
| `blocks[].blockId` | Long | 블록 ID |
| `blocks[].rowIndex` | int | 행 인덱스 |
| `blocks[].sortOrder` | int | 행 내 순서 |
| `blocks[].colSpan` | int | 열 병합 수 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "blocks": [
      { "blockId":15, "rowIndex":0, "sortOrder":0, "colSpan":2 },
      { "blockId":21, "rowIndex":0, "sortOrder":1, "colSpan":1 }
    ]
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 블록 배치 변경 성공 |
| 400 | Bad Request | `BLOCK_COL_SPAN_INVALID` | `colSpan` 이 1~3 범위 밖 |
| 400 | Bad Request | `BLOCK_LAYOUT_INVALID` | 배치 목록이 비었거나 다른 스텝의 블록이 섞임 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `STEP_EDIT_DENIED` | 스텝 편집 권한 없음 |
| 404 | Not Found | `BLOCK_NOT_FOUND` | 블록이 존재하지 않음 |

---

# DELETE `/api/v1/blocks/{blockId}` — 블록 삭제

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 블록 삭제 |
| Method | DELETE |
| URL | `/api/v1/blocks/{blockId}` |
| 인증 필요 여부 | Y |
| 권한 | 스텝 EDITOR |
| 요구사항 | BLK-007 · BLK-008 · INV-05 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `blockId` | Long | Y | 삭제할 블록 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
DELETE /api/v1/blocks/21
```

삭제는 `deleted_at` 플래그다. **하드 삭제 API 가 존재하지 않는다** (BLK-007 · INV-05).

**잠금 대상 4종** (BLK-008 · [`BLOCK.md`](../../global/BLOCK.md) §8): ① 입금 연결 입금확인 ② **계산서 연결 조회**(TXL-009) ③ 진행 중 결재 ④ 결재 대상 파일 → 409.
⚠️ ①②는 **소유 스텝도 함께 잠긴다** (PCB-006 · TXL-010 · `DELETE /api/v1/steps/{stepId}` → 409 `STEP_DELETE_LOCKED`).

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | `null` |

## Success Example
```
{ "httpStatus":200, "message":"요청이 성공적으로 처리되었습니다.", "data":null }
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 블록 삭제 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `STEP_EDIT_DENIED` | 스텝 편집 권한 없음 |
| 404 | Not Found | `BLOCK_NOT_FOUND` | 블록이 존재하지 않음 |
| 409 | Conflict | `BLOCK_DELETE_LOCKED` | 잠금 대상 블록 — 재무가 연결을 해제해야 함 |

---

# POST `/api/v1/blocks/{blockId}/issues` — 이슈-블록 연결

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 이슈-블록 연결 |
| Method | POST |
| URL | `/api/v1/blocks/{blockId}/issues` |
| 인증 필요 여부 | Y |
| 권한 | 스텝 EDITOR |
| 요구사항 | BLK-009 · BLK-010 · INV-06 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `blockId` | Long | Y | 블록 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `issueId` | Long | Y | 연결할 이슈 ID |

**블록과 이슈가 같은 스텝이어야 한다.** DB 제약으로 못 걸어서 애플리케이션이 막는다 (BLK-009 · INV-06).
같은 이슈-블록 쌍은 `UNIQUE(issue_id, block_id)` 로 DB 가 막는다 (BLK-010).

## Request Example
```
{ "issueId":101 }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`201` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `blockId` | Long | 블록 ID |
| `issueId` | Long | 이슈 ID |
| `linkedIssueTotal` | int | 연결 후 전체 이슈 수 |
| `linkedIssueDone` | int | 연결 후 완료 이슈 수 |

## Success Example
```
{
  "httpStatus":201,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": { "blockId":15, "issueId":101, "linkedIssueTotal":6, "linkedIssueDone":2 }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | - | 이슈-블록 연결 성공 |
| 400 | Bad Request | `ISSUE_BLOCK_STEP_MISMATCH` | 이슈와 블록이 서로 다른 스텝에 속함 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `STEP_EDIT_DENIED` | 스텝 편집 권한 없음 |
| 404 | Not Found | `BLOCK_NOT_FOUND` | 블록이 존재하지 않음 |
| 404 | Not Found | `ISSUE_NOT_FOUND` | 이슈가 존재하지 않음 |
| 409 | Conflict | `ISSUE_BLOCK_DUPLICATED` | 이미 연결된 이슈-블록 쌍 |

---

# DELETE `/api/v1/blocks/{blockId}/issues/{issueId}` — 이슈-블록 연결 해제

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 이슈-블록 연결 해제 |
| Method | DELETE |
| URL | `/api/v1/blocks/{blockId}/issues/{issueId}` |
| 인증 필요 여부 | Y |
| 권한 | 스텝 EDITOR |
| 요구사항 | USC-IBL-004 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `blockId` | Long | Y | 블록 ID |
| `issueId` | Long | Y | 이슈 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
DELETE /api/v1/blocks/15/issues/101
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `blockId` | Long | 블록 ID |
| `linkedIssueTotal` | int | 해제 후 전체 이슈 수 |
| `linkedIssueDone` | int | 해제 후 완료 이슈 수 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": { "blockId":15, "linkedIssueTotal":5, "linkedIssueDone":2 }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 이슈-블록 연결 해제 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `STEP_EDIT_DENIED` | 스텝 편집 권한 없음 |
| 404 | Not Found | `ISSUE_BLOCK_NOT_FOUND` | 연결이 존재하지 않음 |

---

# GET `/api/v1/projects/{projectId}/activity-logs` — 활동기록 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 활동기록 조회 |
| Method | GET |
| URL | `/api/v1/projects/{projectId}/activity-logs` |
| 인증 필요 여부 | Y |
| 권한 | 참여자 |
| 요구사항 | PRJ-016 · PRJ-017 · INV-09 · USC-LOG-001~007 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |

## Request Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stepId` | Long | N | 스텝 단위 필터 |
| `page` | int | N | 기본 0 |
| `size` | int | N | 기본 20 |

⛔ **로그 삭제 API 가 존재하지 않는다** (USC-LOG-007).
⚠️ `resourceType` 이 `PAYMENT`·`TAX_INVOICE` 인 **미매칭 재무 로그는 `project_id` 가 `NULL` 이라 이 응답에 나오지 않는다** ([`ERD.md`](ERD.md) §5-4).
🚧 **어느 사건에 로그를 남길지는 아직 미정이다** → [`HANDOFF.md`](../HANDOFF.md) §L-2

## Request Body
없음

## Request Example
```
GET /api/v1/projects/12/activity-logs?stepId=10&page=0&size=20
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 (`200` 고정) |
| `message` | String | 응답 메시지 (`요청이 성공적으로 처리되었습니다.` 고정) |
| `data` | Object | 응답 데이터 |
| `content` | List\<Object\> | 활동기록 (최신순) |
| `content[].logId` | Long | 로그 ID |
| `content[].projectId` | Long | 프로젝트 ID — **이 API 는 `WHERE project_id = ?` 라 항상 채워진다** (컬럼 자체는 NULL 허용 · [`ERD.md`](ERD.md) §5-4) |
| `content[].stepId` | Long | 스텝 ID (`null` 허용) |
| `content[].blockId` | Long | 블록 ID — **블록 밖 사건이면 `null`** (USC-LOG-005) |
| `content[].act` | String | 사건 종류 — ✅ **`CREATE`·`UPDATE`·`DELETE`·`COMPLETE`·`MOVE`** (ERD 확정) |
| `content[].resourceType` | String | 대상 종류 — `PROJECT`·`STAGE`·`STEP`·`BLOCK`·`MEMBER`·`ISSUE`·`PAYMENT`·`TAX_INVOICE` (**8종** · [`ERD.md`](ERD.md) §5-4) |
| `content[].resourceId` | Long | 대상 ID |
| `content[].targetName` | String | **대상 이름 스냅샷** — `NOT NULL`, 반드시 채워진다 (INV-09) |
| `content[].field` | String | 변경 필드명 (`act='UPDATE'` 일 때) |
| `content[].beforeValue` | String | 변경 전 값 |
| `content[].afterValue` | String | 변경 후 값 |
| `content[].actor` | Object | 행위자 (`userId` **사번**·`name`) |
| `content[].privilegedOverride` | boolean | `true` 면 `상위권한으로 수정` 표시 (PRJ-017) |
| `content[].createdAt` | LocalDateTime | 발생 일시 |
| `page` / `size` / `totalElements` / `totalPages` | - | 페이징 정보 |

## Success Example
```
{
  "httpStatus":200,
  "message":"요청이 성공적으로 처리되었습니다.",
  "data": {
    "content": [
      {
        "logId":501,
        "projectId":12,
        "stepId":10,
        "blockId":null,
        "act":"COMPLETE",
        "resourceType":"STEP",
        "resourceId":10,
        "targetName":"제안서 작성",
        "field":"status",
        "beforeValue":"IN_PROGRESS",
        "afterValue":"DONE",
        "actor": { "userId":"E2024001", "name":"김용준" },
        "privilegedOverride":false,
        "createdAt":"2026-08-10T17:00:00"
      }
    ],
    "page":0,
    "size":20,
    "totalElements":1,
    "totalPages":1
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 활동기록 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | Forbidden | `PROJECT_ACCESS_DENIED` | 프로젝트 접근 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |
