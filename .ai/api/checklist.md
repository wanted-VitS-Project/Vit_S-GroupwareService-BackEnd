# 체크리스트 블록 API 명세

**노션 원본**: 미확인 — 사용자가 전달한 명세를 그대로 옮김 (확인되는 대로 링크 채워주세요)
**최종 동기화**: 2026-08-05
**도메인 담당**: 정림

> 상태가 `✅ 확정` 이상인 항목은 프론트와의 계약이다. 임의 변경 금지.
> ⚠️ 에러 코드의 **내부 번호는 바뀔 수 있다** (담당자 확인) — 코드 문자열 자체를 구현 기준으로 삼되, 번호 변경 통보 시 함께 갱신할 것.

---

## 개요

블록 생성은 공용 블록 담당자(동훈님)가 처리한다 — Block 도메인이 생성 트랜잭션 안에서 `ChecklistHandlerService.create(blockId)`를 호출하면 체크리스트 도메인이 JPA로 빈 `checklist_block` 행을 만들어 PK를 돌려준다. 블록 일괄 조회도 체크리스트 도메인이 만든 MyBatis 어댑터(`checklist.infrastructure.blockdetail` 패키지)로 채워진다.

블록 삭제는 **이벤트가 아니라 블록 삭제와 같은 트랜잭션에서의 동기 호출**로 확정됐다 (`BlockDetailPort.deleteDetail` → `ChecklistHandlerService.deleteByBlock`). 이전에 있던 `ChecklistLifeCycleEventHandler`(이벤트 리스너)는 죽은 코드라 삭제함 — 텍스트에서 먼저 정리한 것과 동일한 이유. 다만 **Block 도메인 쪽 삭제 API 자체가 아직 없어서** 실제로 호출되는 시점은 없다.

**편집 권한(403) 검사, 2026-08-05 실 연동 완료** — 더 이상 항상 통과하는 스텁이 아니라, `BlockCatalogPort.hasEditPermission("CHECKLIST", chkBlockId, userId, role)`이 Step 도메인의 `StepAccessUseCase.requireEditable`을 재사용해서 실제로 판정한다. role은 `ChecklistController`가 `Authentication`에서 꺼내 Command→Service→Policy까지 전달한다. 로컬 테스트로 403(프로젝트 미참여 계정)까지 확인함.

⚠️ **`BlockCatalogPort`/`CatalogBlockAdapter`는 텍스트 도메인 패키지에 있는 공용 포트라, 텍스트 브랜치에도 동일한 구현이 별도로 들어가 있다.** 텍스트 PR이 먼저 머지되면 이 브랜치에서 develop을 당겨서 정리(대부분 자동 병합될 것으로 예상, 안 되면 수동 정리)할 것.

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

- **블록 생성**: 공용 블록 담당자가 `block` 행을 만들고, 체크리스트 도메인이 `checklist_block` 행을 만들어준다 (JPA, `ChecklistHandlerService.create`). 이 도메인 API로는 다루지 않는다.
- **블록 삭제**: Block 도메인이 삭제 트랜잭션 안에서 `ChecklistHandlerService.deleteByBlock`을 동기 호출한다(이벤트 아님). Block 도메인 쪽 삭제 API 자체가 아직 없어 현재는 호출되지 않는다.

## 활동 로그 (Activity Log)

- **아직 미반영** — 생성·수정·삭제 전부 주석 처리 상태. 기존 주석은 실제 계약(`ActivityOccurredEvent.of`, `DomainEventPublisher`)과 안 맞는 죽은 코드라 그대로 주석 해제하면 컴파일 에러.
- 활동 로그 담당(용준님) 쪽 계약이 다시 정리되는 중 (`resourceName` 필드 추가 등) — **그 PR 머지되면 텍스트에서 확립한 패턴(MODIFY는 필드 단위, 삭제 로그는 Block 도메인 책임) 그대로 반영 예정.**
