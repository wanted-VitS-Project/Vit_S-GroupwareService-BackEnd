# 텍스트 블록 API 명세

**노션 원본**: 미확인 — 프론트가 보고 있는 것으로 추정되나 링크 확인 필요 (확인되는 대로 채워주세요)
**최종 동기화**: 2026-08-03
**도메인 담당**: 정림

> 상태가 `✅ 확정` 이상인 항목은 프론트와의 계약이다. 임의 변경 금지.
> ⚠️ 노션 링크가 아직 미확인 상태입니다 — 확인되는 대로 위 줄에 채워주세요.

---

## 🔴 2026-08-03 기획 변경 — 노션 재확인 필요

블록 생성·삭제를 Block 도메인(동훈님)이 전부 처리하기로 바뀌었다. **아래 표는 구현 현황이고, 노션 명세와 프론트 계약은 아직 안 맞춰봤다** — 프론트가 실제로 어느 경로로 생성/삭제를 호출하는지 팀 확인 필요.

| 상태 | 기능 | METHOD | URL | 권한 | 비고 |
|------|------|--------|-----|------|------|
| 🔴 재확인 필요 | 텍스트 블록 생성 | ~~POST `/api/v1/blocks/texts/{stepId}`~~ | | | Block 도메인이 처리 — 이 경로 자체가 없어질 수 있음 |
| ✅ 확정 (구현됨) | 텍스트 본문 수정 | PATCH | `/api/v1/blocks/texts/{txtId}` | 편집 권한 보유자 | 권한 검사는 아직 TODO (하단 구현 메모 참고) |
| 🔴 재확인 필요 | 텍스트 블록 삭제 | ~~DELETE `/api/v1/blocks/texts/{txtId}`~~ | | | 이 API 대신, Block 도메인이 블록 삭제 시 이벤트를 발행하고 이 도메인이 리스너로 받아 처리 |

---

### 텍스트 블록 생성 `POST /api/v1/blocks/texts/{stepId}`

**상태**: ✅ 확정
**인증 필요 여부**: Y

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stepId` | Long | Y | 생성할 블록의 스텝(페이지) ID |

**Request Body**: 없음

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.txtId` | Long | 생성된 텍스트 블록 ID |
| `data.createdAt` | LocalDateTime | 텍스트 블록 생성일 |

**Success Example**

```json
{
  "httpStatus": 201,
  "message": "텍스트 블록 생성 성공",
  "data": {
    "txtId": 1,
    "createdAt": "2026-07-31T15:20:00"
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | `TXT-001` | "텍스트 블록 생성 성공" |
| 403 | Forbidden | `TXT-004` | "편집 권한이 없습니다." |
| 404 | Not Found | `TXT-005` | "존재하지 않는 step입니다." |
| 401 | Unauthorized | `TXT-007` | "다시 로그인해주세요." |
| 500 | Internal Server Error | `TXT-008` | "서버 내부 오류입니다." |

---

### 텍스트 본문 수정 `PATCH /api/v1/blocks/texts/{txtId}`

**상태**: ✅ 확정
**인증 필요 여부**: Y

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `txtId` | Long | Y | 수정할 텍스트 항목 ID |

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `content` | String | Y | 사용자가 수정한 부분을 포함한 모든 내용 |

**Request Example**

```json
{
  "content": "**오전 회의록** \n주제: 제안서 분담하기 \n기한: 오늘 오후"
}
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.txtId` | Long | 수정된 텍스트 블록 ID |
| `data.content` | String | 수정된 텍스트 블록 내용 |
| `data.updatedAt` | LocalDateTime | 텍스트 블록 수정일 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "텍스트 본문 수정 성공",
  "data": {
    "txtId": 1,
    "content": "**오전 회의록** \n주제: 제안서 분담하기 \n기한: 오늘 오후",
    "updatedAt": "2026-07-31T15:20:00"
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | `TXT-002` | "텍스트 본문 수정 성공" |
| 403 | Forbidden | `TXT-004` | "편집 권한이 없습니다." |
| 404 | Not Found | `TXT-006` | "존재하지 않는 블록입니다." |
| 401 | Unauthorized | `TXT-007` | "다시 로그인해주세요." |
| 500 | Internal Server Error | `TXT-008` | "서버 내부 오류입니다." |

---

### 텍스트 블록 삭제 `DELETE /api/v1/blocks/texts/{txtId}`

**상태**: ✅ 확정
**인증 필요 여부**: Y

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `txtId` | Long | Y | 삭제할 텍스트 블록 ID |

**Request Body**: 없음

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 (`null`) |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "텍스트 블록 삭제 성공",
  "data": null
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | `TXT-003` | "텍스트 블록 삭제 성공" |
| 403 | Forbidden | `TXT-004` | "편집 권한이 없습니다." |
| 404 | Not Found | `TXT-006` | "존재하지 않는 블록입니다." |
| 401 | Unauthorized | `TXT-007` | "다시 로그인해주세요." |
| 500 | Internal Server Error | `TXT-008` | "서버 내부 오류입니다." |

---

## 구현 메모 (사람이 확인할 것)

- 응답 포맷은 로그인 기능 병합 이후 팀 공통 `ApiResponse`(`global.presentation.api.common.ApiResponse`: `httpStatus`/`message`/`data`)로 자리잡혔다 — 이 문서 원래 명세와 정확히 일치한다. `success(message, data)` / `created(message, data)` 사용.
- **`text` 테이블에 `block_id` 컬럼이 없다** (2026-08-03 기획 변경). 블록 생성·삭제는 Block 도메인이 전부 처리한다.
- 그 결과 **PATCH의 편집 권한(403) 검사를 어디서 하는지가 열린 문제다** — txtId 로 stepId 를 되짚어갈 방법이 없어졌다. `TextEligibilityPolicy` 에 TODO로 남겨둠.
- `BlockCatalogPort`/`CatalogBlockAdapter` (existsStep·hasEditPermission·hasViewPermission·getBlockTitle) 는 create API 제거로 **현재 호출하는 곳이 없다.** 나중에 PATCH 권한 검사에 다시 쓸지, 아예 지울지 결정 필요.
- 403/401 실제 인증 검사는 로그인 기능(김동현) 쪽 `account` 테이블 마이그레이션이 아직 없어 로컬에서 테스트 불가 상태.
