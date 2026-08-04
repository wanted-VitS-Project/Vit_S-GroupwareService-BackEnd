# 체크리스트 블록 API 명세

**노션 원본**: 미확인 — 사용자가 전달한 명세를 그대로 옮김 (확인되는 대로 링크 채워주세요)
**최종 동기화**: 2026-08-04
**도메인 담당**: 정림

> 상태가 `✅ 확정` 이상인 항목은 프론트와의 계약이다. 임의 변경 금지.
> ⚠️ 에러 코드의 **내부 번호는 바뀔 수 있다** (담당자 확인) — 코드 문자열 자체를 구현 기준으로 삼되, 번호 변경 통보 시 함께 갱신할 것.

---

## 개요

블록 생성은 공용 블록 담당자(동훈님)가 처리한다. 블록 삭제는 텍스트 블록과 동일하게 이벤트로 처리되며,
활동 로그 인프라 미비로 리스너는 주석 처리한다 (`ChecklistLifeCycleEventHandler`).
이 문서는 **체크리스트 항목(하위 데이터) CRUD** 3종만 다룬다.

| 상태 | 기능 | METHOD | URL | 권한 |
|------|------|--------|-----|------|
| ✅ 확정 | 체크리스트 항목 생성 | POST | `/api/v1/blocks/checklists/{chkBlockId}/items` | 편집 권한 보유자 |
| ✅ 확정 | 체크리스트 항목 수정 | PATCH | `/api/v1/blocks/checklists/items/{chkId}` | 편집 권한 보유자 |
| ✅ 확정 | 체크리스트 항목 삭제 | DELETE | `/api/v1/blocks/checklists/items/{chkId}` | 편집 권한 보유자 |

---

### 체크리스트 항목 생성 `POST /api/v1/blocks/checklists/{chkBlockId}/items`

**상태**: ✅ 확정
**인증 필요 여부**: Y

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `chkBlockId` | Long | Y | 체크리스트 항목을 생성할 블록의 ID |

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `content` | String | Y | 체크리스트 항목에 담길 내용 |

**Request Example**

```json
{
  "content": "제안서 결재 보고하기"
}
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.chkBlockId` | Long | 항목이 생성된 체크리스트 블록 ID |
| `data.chkId` | Long | 생성된 체크리스트 항목 ID |
| `data.content` | String | 생성된 체크리스트 항목 내용 |
| `data.completedCount` | int | 현재 완료된 항목 개수 |
| `data.totalCount` | int | 전체 항목 개수 |
| `data.createdAt` | LocalDateTime | 체크리스트 항목 생성일 |

**Success Example**

```json
{
  "httpStatus": 201,
  "message": "체크리스트 항목 생성 성공",
  "data": {
    "chkBlockId": 1,
    "chkId": 1,
    "content": "제안서 결재 보고하기",
    "completedCount": 3,
    "totalCount": 5,
    "createdAt": "2026-07-31T15:20:00"
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | — | "체크리스트 항목 생성 성공" |
| 400 | Bad Request | `CHK-004` | "내용을 입력해 주세요." |
| 403 | Forbidden | `CHK-001` | "편집 권한이 없습니다." |
| 403 | Forbidden | `AUTH_PASSWORD_RESET_REQUIRED` | "초기 비밀번호를 먼저 변경해 주세요." (전 도메인 공통 게이트) |
| 404 | Not Found | `CHK-002` | "존재하지 않는 블록입니다." |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | "로그인이 필요합니다." (전 도메인 공통) |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | "서버 내부 오류가 발생했습니다." (전 도메인 공통 폴백) |

---

### 체크리스트 항목 수정 `PATCH /api/v1/blocks/checklists/items/{chkId}`

**상태**: ✅ 확정
**인증 필요 여부**: Y

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `chkId` | Long | Y | 수정할 체크리스트 항목 ID |

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `content` | String | N | 사용자가 수정한 부분을 포함한 모든 내용(nullable) |
| `changeStatusTo` | Boolean | N | 목표 완료 여부 상태(nullable) |

**Request Example**

```json
{
  "content": "제안서 결재 보고하기(매우 중요!)",
  "changeStatusTo": null
}
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.chkId` | Long | 수정된 체크리스트 항목 ID |
| `data.content` | String | 수정된 체크리스트 항목 내용 |
| `data.isCompleted` | Boolean | 수정된 체크리스트 완료 여부 |
| `data.completedCount` | int | 현재 완료된 항목 개수 |
| `data.totalCount` | int | 전체 항목 개수 |
| `data.updatedAt` | LocalDateTime | 체크리스트 항목 수정일 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "체크리스트 항목 수정 성공",
  "data": {
    "chkId": 1,
    "content": "제안서 결재 보고하기(매우 중요!)",
    "isCompleted": false,
    "completedCount": 3,
    "totalCount": 5,
    "updatedAt": "2026-07-31T15:20:00"
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "체크리스트 항목 수정 성공" |
| 400 | Bad Request | `CHK-004` | "내용을 입력해 주세요." (content가 공백뿐일 때) |
| 400 | Bad Request | `CHK-005` | "수정할 내용을 하나 이상 입력해 주세요." (content/changeStatusTo 둘 다 없을 때) |
| 403 | Forbidden | `CHK-001` | "편집 권한이 없습니다." |
| 403 | Forbidden | `AUTH_PASSWORD_RESET_REQUIRED` | "초기 비밀번호를 먼저 변경해 주세요." (전 도메인 공통 게이트) |
| 404 | Not Found | `CHK-003` | "존재하지 않는 항목입니다." |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | "로그인이 필요합니다." (전 도메인 공통) |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | "서버 내부 오류가 발생했습니다." (전 도메인 공통 폴백) |

> 한 요청에서 `content`, `changeStatusTo` 가 동시에 바뀌어도 **필드 단위로 활동 로그를 남긴다** (§5.2 체크리스트, 활동 로그 인프라 도입 전까지는 주석 처리).

---

### 체크리스트 항목 삭제 `DELETE /api/v1/blocks/checklists/items/{chkId}`

**상태**: ✅ 확정
**인증 필요 여부**: Y

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `chkId` | Long | Y | 삭제할 체크리스트 항목 ID |

**Request Body**: 없음

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.completedCount` | int | 현재 완료된 항목 개수 |
| `data.totalCount` | int | 전체 항목 개수 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "체크리스트 항목 삭제 성공",
  "data": {
    "completedCount": 3,
    "totalCount": 5
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "체크리스트 항목 삭제 성공" |
| 403 | Forbidden | `CHK-001` | "편집 권한이 없습니다." |
| 403 | Forbidden | `AUTH_PASSWORD_RESET_REQUIRED` | "초기 비밀번호를 먼저 변경해 주세요." (전 도메인 공통 게이트) |
| 404 | Not Found | `CHK-003` | "존재하지 않는 항목입니다." |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | "로그인이 필요합니다." (전 도메인 공통) |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | "서버 내부 오류가 발생했습니다." (전 도메인 공통 폴백) |

---

## 범위 밖 (블록 자체 CRUD)

- **블록 생성**: 공용 블록 담당자가 `block` + `checklist_block` 행을 함께 만든다. 이 도메인은 다루지 않는다.
- **블록 삭제**: 텍스트 블록과 동일하게 Block 도메인이 이벤트를 발행하고, 이 도메인은 리스너로 받아 소속 항목을 정리한다.
  활동 로그 인프라와 이벤트 타입이 아직 정해지지 않아 `ChecklistLifeCycleEventHandler` 는 주석 처리된 상태로 남겨둔다 (텍스트의 `TextLifeCycleEventHandler` 와 동일한 패턴).
