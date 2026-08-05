# 🔔 Notification API — 알림

**상태**: `✅ 확정` — 노션 원본 4개 전부 대조 완료(2026-08-05). 이탈 금지 규칙 전면 적용(`../API.md` §0)
**최종 업데이트**: 2026-08-05 · **담당**: 이강욱
**노션**: 확인 필요 — 노션 링크 채워넣기. 검토 중 발견한 수정사항(아래 각 절 참고)을 노션에도 반영했는지 확인
**범위**: 알림 REST API 4개(목록조회·삭제·이동대상조회·전체읽음처리). 생성 이벤트 인프라(`#27`)는 REST 엔드포인트가 없어 여기 없음

> ✅ **노션 원본을 하나씩 받아 대조 완료 — 구현 가능.** 경로·필드명·타입·상태코드·에러코드를 **한 글자도 바꾸지 않는다** (`../API.md` §0).
> 검토 중 잡은 것: `category` 값 형식 확정(영문 접두어), 이동대상조회의 중복 Response Parameter 표 정리, 필드명 오차(`markedCount`) 등 — 노션에도 반영됐는지 재확인 필요.
> 요구사항 근거: [`../docs/domain/결재·알림/NOTI-V1.md`](../docs/domain/결재·알림/NOTI-V1.md)

## 엔드포인트

| # | API명칭 | METHOD | URL | 권한 |
|---|---|---|---|---|
| 1 | 알림 목록 조회 | GET | `/api/v1/notifications` | 인증 사용자(본인 알림만) |
| 2 | 알림 삭제 | DELETE | `/api/v1/notifications/{notificationId}` | 인증 사용자(본인 알림만) |
| 3 | 알림 이동 대상 조회 | GET | `/api/v1/notifications/{notificationId}/target` | 인증 사용자(본인 알림만) |
| 4 | 알림 전체 읽음 처리 | PATCH | `/api/v1/notifications/read-all` | 인증 사용자(본인 알림만) |

⭐ **알림 생성 공개 API는 없다**(`INV-01`) — `#27`(이벤트 인프라)이 내부적으로만 생성한다.
⭐ **`userId`는 `String`이다**(`employee.user_id`, 사번). `notificationId`는 `long`.
⭐ **도메인 무관 구조**(`INV-02`/`INV-04`) — 응답에 `approvalId` 같은 도메인 전용 필드를 두지 않는다. `blockId` 하나와 `type`/`targetId`/`extra`로만 표현한다.

---

## 1. 알림 목록 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/notifications` |
| 인증 필요 | Y · 본인 알림만 |
| 요구사항 | VIW-001~005 |

**Request**

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|:---:|---|
| `category` | Query | String | N | 카테고리 필터(미지정 시 전체). `notification_type` 접두어를 그대로 받는다(예: `category=APPROVAL`) — 한글 라벨 아님. 표시는 프론트가 매핑 |
| `isRead` | Query | Boolean | N | `true`/`false` — 안 읽음만 보려면 `false` |
| `page` | Query | int | N | 페이지 번호(기본 0) |
| `size` | Query | int | N | 페이지 크기(기본 10) |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.content[].notificationId` | long | 알림 구분 번호 |
| `data.content[].blockId` | long, nullable | 연결된 블록 |
| `data.content[].notificationType` | String | 알림 유형(예: `APPROVAL_REQUESTED`/`APPROVAL_REJECTED`/`APPROVAL_COMPLETED`) |
| `data.content[].title` | String | 알림 제목 |
| `data.content[].message` | String | 알림 내용 |
| `data.content[].readAt` | String, nullable | 읽은 시각(`null`이면 안 읽음) |
| `data.content[].createdAt` | String | 생성 일시 |
| `data.totalElements` | int | 해당 필터 기준 전체 개수(탭 숫자로 사용) |
| `data.totalPages` | int | |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "알림 목록 조회 성공",
  "data": {
    "content": [
      {
        "notificationId": 301,
        "blockId": 101,
        "notificationType": "APPROVAL_REQUESTED",
        "title": "결재 요청",
        "message": "출장비 정산 결재 요청이 도착했습니다.",
        "readAt": null,
        "createdAt": "2026-08-01T09:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1
  }
}
```

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 로그인이 필요합니다 |

**비즈니스 규칙**: 로그인 사용자 본인의 알림만 조회(VIW-001) · 항상 최신순(VIW-002) · `deleted_at`이 있는 알림은 제외(VIW-005).

---

## 2. 알림 삭제

| 항목 | 내용 |
|------|------|
| Method · URL | `DELETE /api/v1/notifications/{notificationId}` |
| 인증 필요 | Y · 본인 알림만 |
| 요구사항 | ACT-001~003 |

**Request** — `notificationId`(Path, long, Y)

**Response** — `204 No Content` (응답 본문 없음 — 204는 RFC 9110상 본문을 가질 수 없다)

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 204 | No Content | – | 삭제 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 로그인이 필요합니다 |
| 403 | Forbidden | `NOTIFICATION_FORBIDDEN` | 다른 사용자의 알림 삭제 시도 |
| 404 | Not Found | `NOTIFICATION_NOT_FOUND` | 존재하지 않거나 이미 삭제된 알림 |

**비즈니스 규칙**: 수신자 본인만 삭제 가능 · 논리 삭제(`deleted_at`), 하드 삭제 아님(ACT-001) · 이미 삭제된 알림 재삭제 요청은 404(멱등 처리, ACT-003).

---

## 3. 알림 이동 대상 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/notifications/{notificationId}/target` |
| 인증 필요 | Y · 본인 알림만 |
| 요구사항 | VIW-006~008 · ACT-004 |

**Request**

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---|---|---|:---:|---|
| `notificationId` | Path | long | Y | 이동 대상을 조회할 알림 구분 번호 |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.type` | String | 이동 대상 도메인 유형(`APPROVAL`/`ISSUE` 등 — 확장 가능한 문자열). `NONE`이면 이동 대상 없음 |
| `data.targetId` | long | 이동 대상 구분 번호(도메인마다 의미 다름 — `APPROVAL`이면 `approvalId`). `type=NONE`이면 `null` |
| `data.extra` | Object, nullable | 도메인별 부가 정보(선택). 결재면 `{revisionId}` |

**Success Example — 도메인별 예시**

```json
{ "httpStatus": 200, "message": "알림 이동 대상 조회 성공", "data": { "type": "APPROVAL", "targetId": 55, "extra": { "revisionId": 56 } } }
```

```json
{ "httpStatus": 200, "message": "알림 이동 대상 조회 성공", "data": { "type": "ISSUE", "targetId": 12, "extra": null } }
```

```json
{ "httpStatus": 200, "message": "알림 이동 대상 조회 성공", "data": { "type": "NONE", "targetId": null, "extra": null } }
```

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 조회 성공(매핑 없어도 `type=NONE`으로 200) |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 로그인이 필요합니다 |
| 403 | Forbidden | `NOTIFICATION_FORBIDDEN` | 다른 사용자의 알림 조회 시도 |
| 404 | Not Found | `NOTIFICATION_NOT_FOUND` | 알림을 찾을 수 없음(연결된 block 삭제 포함) |

**비즈니스 규칙**: 수신자 본인만 조회 가능 · **도메인 독립성** — 이 API는 알림 도메인 소유이며 특정 도메인에 종속되지 않는다. `type`은 개방형 값이고 결재는 그중 하나일 뿐이다(새 도메인 추가 시 이 API 자체는 수정 불필요, `INV-02`) · `notification.block_id` → `block.type`을 거쳐 판정(VIW-007), `block_id`가 없거나 아직 이동 로직이 없는 타입이면 `type=NONE`(에러 아님) · 조회 성공 시 **자동으로 읽음 처리**됨(`read_at` 기록, ACT-004) · 원본 업무 페이지로 바로 가지 않고 결재관리 상세로만 이동(VIW-008, 프론트 라우팅 규칙).

---

## 4. 알림 전체 읽음 처리

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/notifications/read-all` |
| 인증 필요 | Y · 본인 알림만 |
| 요구사항 | ACT-005 |

**Request** — 없음(바디 없음)

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.markedCount` | int | 이번에 읽음 처리된 알림 개수 |

**Success Example**

```json
{ "httpStatus": 200, "message": "전체 읽음 처리 성공", "data": { "markedCount": 5 } }
```

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 처리 성공(대상 0건이어도 200) |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 로그인이 필요합니다 |

**비즈니스 규칙**: 로그인 사용자의 `read_at IS NULL`인 알림 전체가 대상(ACT-005) · 대상 알림들의 `read_at`을 현재 시각으로 일괄 기록.

---

## 참고 — `#27` 생성 이벤트 인프라 (REST API 아님)

| 항목 | 내용 |
|---|---|
| 요구사항 | GEN-001~004 |
| 이벤트 클래스 | `NotificationRequestedEvent(recipientUserId, notificationType, title, message, blockId)` |
| 패키지 | `notification.domain.event`(자기 도메인 소유, `global` 아님) |
| 처리 방식 | `@TransactionalEventListener(phase = AFTER_COMMIT)` — 발행 트랜잭션 커밋 후에만 `notification` row 생성 |
| 결재 쪽 발행 지점 | 상신(`SUB-003`) · 재상신(`SUB-010`, 상신 재사용) · 승인(`PRC-004`/`PRC-004-1`) · 반려(`PRC-008`) |

이 부분은 REST 엔드포인트가 없어 `.ai/api/README.md`의 "노션 반영 필요" 게이트 대상이 아니다 — `NOTI-V1.md` GEN-001~004만으로 바로 구현 가능하다.
