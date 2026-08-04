# 텍스트 블록 API 명세

**노션 원본**: 미확인 — 프론트가 보고 있는 것으로 추정되나 링크 확인 필요 (확인되는 대로 채워주세요)
**최종 동기화**: 2026-08-03
**도메인 담당**: 정림

> 상태가 `✅ 확정` 이상인 항목은 프론트와의 계약이다. 임의 변경 금지.
> ⚠️ 노션 링크가 아직 미확인 상태입니다 — 확인되는 대로 위 줄에 채워주세요.

---

## 🔴 2026-08-03 기획 변경 — 노션 재확인 필요

블록 생성·삭제를 Block 도메인(동훈님)이 전부 처리하기로 바뀌었다. **텍스트 도메인 API는 PATCH(본문 수정) 하나만 남는다.**

- **생성**: `POST /api/v1/blocks/texts/{stepId}` 폐기. Block 도메인이 블록(+텍스트 상세 행)을 직접 만든다.
- **삭제**: `DELETE /api/v1/blocks/texts/{txtId}` 폐기. Block 도메인이 블록 삭제 시 이벤트를 발행하고, 텍스트 도메인은 리스너로 받아 자기 데이터만 정리한다 (`TextHandlerService`, 현재 리스너는 이벤트 타입 미정으로 주석 처리).

**노션 명세와 프론트 계약은 아직 안 맞춰봤다** — 프론트가 실제로 생성/삭제를 어느 경로로 호출하는지(Block 도메인의 통합 API 경로 등) 팀 확인 필요.

| 상태 | 기능 | METHOD | URL | 권한 |
|------|------|--------|-----|------|
| ✅ 확정 (구현됨) | 텍스트 본문 수정 | PATCH | `/api/v1/blocks/texts/{txtId}` | 편집 권한 보유자 |

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
| 200 | OK | — | "텍스트 본문 수정 성공" |
| 403 | Forbidden | `TXT-001` | "편집 권한이 없습니다." |
| 404 | Not Found | `TXT-002` | "존재하지 않는 블록입니다." |
| 401 | Unauthorized | `TXT-003` | "다시 로그인해주세요." |
| 500 | Internal Server Error | `TXT-004` | "서버 내부 오류입니다." |

---

## 구현 메모 (사람이 확인할 것)

- 응답 포맷은 로그인 기능 병합 이후 팀 공통 `ApiResponse`(`global.presentation.api.common.ApiResponse`: `httpStatus`/`message`/`data`)로 자리잡혔다 — 이 문서 명세와 정확히 일치한다. `success(message, data)` 사용.
- **`text` 테이블에 `block_id` 컬럼이 없다** (2026-08-03 기획 변경). 대신 공용 `block` 테이블이 `type`/`blockTypeId` 컬럼으로 상세 테이블(`text.txt_id`)을 가리키는 방향이다.
- 편집 권한(403) 검사는 `BlockCatalogPort.hasEditPermission("TEXT", txtId, userId)` 로 연결되어 있다 (`TextEligibilityPolicy.assertEditPermission`). 다만 구현체(`CatalogBlockAdapter`)는 공용 block 테이블 연동 전까지 항상 `true` 를 반환하는 TODO 스텁이다.
- 403/401 실제 인증 검사는 로그인 기능(김동현) 쪽 `account`/`employee` 테이블 마이그레이션이 아직 없어 로컬에서 테스트가 막혀 있다.
