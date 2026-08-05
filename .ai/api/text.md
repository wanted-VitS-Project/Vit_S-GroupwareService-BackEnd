# 텍스트 블록 API 명세

**노션 원본**: 미확인 — 프론트가 보고 있는 것으로 추정되나 링크 확인 필요 (확인되는 대로 채워주세요)
**최종 동기화**: 2026-08-05
**도메인 담당**: 정림

> 상태가 `✅ 확정` 이상인 항목은 프론트와의 계약이다. 임의 변경 금지.
> ⚠️ 노션 링크가 아직 미확인 상태입니다 — 확인되는 대로 위 줄에 채워주세요.

---

## 🔴 2026-08-03 기획 변경 (2026-08-05 실제 연동 확인) — 노션 재확인 필요

블록 생성·삭제를 Block 도메인(동훈님)이 전부 처리한다. **텍스트 도메인 API는 PATCH(본문 수정) 하나만 남는다.**

- **생성**: `POST /api/v1/blocks/texts/{stepId}` 폐기. Block 도메인이 블록 생성 트랜잭션 안에서 `TextHandlerService.create(blockId)`를 호출하고, 텍스트 도메인이 JPA로 빈 상세 행을 만들어 PK(txtId)를 돌려주면 그걸 `block.type_id`에 연결한다.
- **조회**: 블록 일괄 조회(`GET /api/v1/steps/{stepId}/blocks`)에서 텍스트 상세(`content` 포함)는 텍스트 도메인이 만든 MyBatis 어댑터(`TextDetailMapper`, `text.infrastructure.blockdetail` 패키지)로 채워진다. 최초 생성 직후엔 `content: null`.
- **삭제**: `DELETE /api/v1/blocks/texts/{txtId}` 폐기. **다만 Block 도메인 쪽 삭제 API 자체가 아직 없다** (2026-08-05 기준) — `BlockDetailPort.deleteDetail()` 인터페이스와 텍스트 쪽 구현(`TextHandlerService.delete`)은 준비돼 있지만 호출하는 진입점이 없어서 실제로 삭제가 되진 않는다. 삭제는 이벤트가 아니라 **블록 삭제와 같은 트랜잭션에서의 동기 호출**로 확정됨 (기존 이벤트 리스너 `TextLifeCycleEventHandler`는 죽은 코드라 삭제함).

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
| 400 | Bad Request | `TXT-003` | "내용을 입력해 주세요." |
| 403 | Forbidden | `TXT-001` | "편집 권한이 없습니다." |
| 403 | Forbidden | `AUTH_PASSWORD_RESET_REQUIRED` | "초기 비밀번호를 먼저 변경해 주세요." (전 도메인 공통 게이트) |
| 404 | Not Found | `TXT-002` | "존재하지 않는 블록입니다." |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | "로그인이 필요합니다." (전 도메인 공통) |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | "서버 내부 오류가 발생했습니다." (전 도메인 공통 폴백) |

---

## 구현 메모 (사람이 확인할 것)

- 응답 포맷은 로그인 기능 병합 이후 팀 공통 `ApiResponse`(`global.presentation.api.common.ApiResponse`: `httpStatus`/`message`/`data`)로 자리잡혔다 — 이 문서 명세와 정확히 일치한다. `success(message, data)` 사용.
- **`text` 테이블엔 `block_id` 컬럼이 있지만 FK는 아니다** (다형성 역방향 — 공용 `block.type_id`가 반대로 `text.txt_id`를 가리킨다).
- **편집 권한(403) 검사, 2026-08-05 실 연동 완료** — `BlockCatalogPort.hasEditPermission("TEXT", txtId, userId, role)`이 `CatalogBlockAdapter`에서 `BlockRepository.findByTypeAndTypeId`로 실제 stepId를 찾은 뒤 Step 도메인의 `StepAccessUseCase.requireEditable`을 재사용해서 판정한다 (더 이상 항상 true인 스텁이 아니다). role은 `TextController`가 `Authentication`에서 `RequesterRole.from(...)`으로 꺼내 Command→Service→Policy까지 실어 나른다.
- **블록명(`getBlockTitle`)도 2026-08-05 실 연동 완료** — 같은 `BlockRepository.findByTypeAndTypeId` 조회로 `Block.getTitle()`을 반환한다.
- 로컬 로그인 테스트는 `account`/`employee` 더미가 Flyway 밖(개인 로컬 스크립트)에서 관리되므로, DB 리셋 후엔 그 스크립트부터 실행해야 한다.

## 활동 로그 (Activity Log)

- 본문 수정(MODIFY) 실 구현 완료 — 실제로 값이 바뀐 경우에만 발행, `resourceId=txtId`, 변경 필드 `content`.
- 블록 자체의 생성/삭제 로그는 텍스트 도메인이 남기지 않는다 — Block 도메인 책임으로 결론 (§5.1, 어댑터 없는 블록 타입까지 커버해야 해서).
- ⚠️ **활동 로그 담당(용준님) 쪽 계약이 다시 정리되는 중** — `ActivityOccurredEvent`에 `resourceName` 필드가 추가되는 등 구조가 바뀔 예정. **그 PR이 머지되면 이 문서와 `TextCommandService`의 발행 코드를 새 계약 기준으로 다시 반영해야 한다.** 지금 적힌 내용은 구 계약 기준.
