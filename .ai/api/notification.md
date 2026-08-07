# 🔔 Notification API — 알림

**상태**: `✅ 확정` — 노션 원본 4개 전부 대조 완료(2026-08-05). 이탈 금지 규칙 전면 적용(`../API.md` §0)
**최종 업데이트**: 2026-08-07 (이동 대상 판정 방식 변경 — block 경유 → 발행 도메인이 직접 지정. 응답 형태 동일, 404 사유만 축소) · 2026-08-06 (개별 삭제 API 폐지 → `RET-001`) · **담당**: 이강욱
**노션**: 확인 필요 — **삭제 API(구 #2) 제거를 노션에도 반영하고 프론트에 공유할 것.** 나머지는 노션 링크 채워넣기, 검토 중 발견한 수정사항(아래 각 절 참고) 반영 여부 재확인
**범위**: 알림 REST API 3개(목록조회·이동대상조회·전체읽음처리). 생성 이벤트 인프라(`#27`)와 자동 정리 배치(`RET-001`)는 REST 엔드포인트가 없어 여기 없음(맨 아래 "참고" 절 참고)

> ✅ **노션 원본을 하나씩 받아 대조 완료 — 구현 가능.** 경로·필드명·타입·상태코드·에러코드를 **한 글자도 바꾸지 않는다** (`../API.md` §0).
> 검토 중 잡은 것: `category` 값 형식 확정(영문 접두어), 이동대상조회의 중복 Response Parameter 표 정리, 필드명 오차(`markedCount`) 등 — 노션에도 반영됐는지 재확인 필요.
> ⚠️ **2026-08-06 변경**: 개별 알림 삭제 API(`DELETE /api/v1/notifications/{notificationId}`)를 없앴다. 읽음 처리 기능이 이미 있어 개별 삭제 UI/수요가 크지 않다고 판단, 대신 생성된 지 3개월 지난 알림을 서버가 매일 자동으로 논리 삭제한다(`RET-001`, REST API 아님). 화면 쪽에도 삭제 버튼을 넣지 않는 것으로 확인됨(피그마 댓글에 삭제 진입점 자체가 없었음).
> 요구사항 근거: [`../docs/domain/결재·알림/NOTI-V1.md`](../docs/domain/결재·알림/NOTI-V1.md)

## 엔드포인트

| # | API명칭 | METHOD | URL | 권한 |
|---|---|---|---|---|
| 1 | 알림 목록 조회 | GET | `/api/v1/notifications` | 인증 사용자(본인 알림만) |
| 2 | 알림 이동 대상 조회 | GET | `/api/v1/notifications/{notificationId}/target` | 인증 사용자(본인 알림만) |
| 3 | 알림 전체 읽음 처리 | PATCH | `/api/v1/notifications/read-all` | 인증 사용자(본인 알림만) |

⭐ **알림 생성 공개 API는 없다**(`INV-01`) — `#27`(이벤트 인프라)이 내부적으로만 생성한다.
⭐ **알림 삭제 공개 API도 없다**(2026-08-06 변경) — 3개월 자동 정리 배치(`RET-001`)만 있다.
⭐ **`userId`는 `String`이다**(`employee.user_id`, 사번). `notificationId`는 `long`.
⭐ **도메인 무관 구조**(`INV-02`/`INV-04`) — 응답에 `approvalId` 같은 도메인 전용 필드를 두지 않는다. `type`/`targetId`/`extra`로만 표현한다.
⭐ **`blockId` 필드는 제거됐다**(2026-08-07) — 이동 대상 판정이 `target_type`/`target_id`로 바뀌면서 쓰이는 곳이 없어졌다. DB 컬럼·FK도 함께 삭제.

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

## 2. 알림 이동 대상 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/notifications/{notificationId}/target` |
| 인증 필요 | Y · 본인 알림만 |
| 요구사항 | VIW-006~010 · ACT-004 |

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
| 404 | Not Found | `NOTIFICATION_NOT_FOUND` | 알림 자체가 없거나 이미 삭제됨 |

**비즈니스 규칙**: 수신자 본인만 조회 가능 · **도메인 독립성** — 이 API는 알림 도메인 소유이며 특정 도메인에 종속되지 않는다. `type`은 개방형 값이고 결재는 그중 하나일 뿐이다(새 도메인 추가 시 이 API 자체는 수정 불필요, `INV-02`) · 알림에 저장된 `target_type`/`target_id`/`target_context`를 **그대로** 반환하며, 값이 없으면 `type=NONE`(에러 아님, VIW-007) · **대상의 존재 여부·조회 권한은 검증하지 않는다** — 실제 도메인 페이지 API가 자기 정책으로 검증한다(VIW-009). 따라서 대상이 삭제됐어도 이 API는 200으로 이동 정보를 반환한다 · `extra`는 알림 **생성 시점 스냅샷**이라 클릭 시점에 재조회하지 않는다(VIW-010) · 조회 성공 시 **자동으로 읽음 처리**됨(`read_at` 기록, ACT-004) · 원본 업무 페이지로 바로 가지 않고 결재관리 상세로만 이동(VIW-008, 프론트 라우팅 규칙).

> ⚠️ **2026-08-07 변경** — 이전에는 `notification.block_id` → `block.type`을 거쳐 이동 대상을 판정했다. 이 방식은 "블록 자체가 그 도메인인" 결재에만 통해서 이슈·프로젝트 알림은 영구히 `type=NONE`이 되는 구조적 결함이 있었다. 이제 발행 도메인이 이동 대상을 직접 지정한다. **이 응답의 형태는 그대로**이고 404 사유만 좁아졌다(연결 block 삭제는 더 이상 404 사유가 아님). 단 **목록 조회 응답에서는 `blockId` 필드가 제거**됐다 — 이동 정보는 이 API가 전담한다.

---

## 3. 알림 전체 읽음 처리

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
| 이벤트 클래스 | `NotificationRequestedEvent(recipientUserId, notificationType, title, message, targetType, targetId, targetContext)` |
| 패키지 | `notification.domain.event`(자기 도메인 소유, `global` 아님) |
| 처리 방식 | `@TransactionalEventListener(phase = AFTER_COMMIT)` — 발행 트랜잭션 커밋 후에만 `notification` row 생성 |
| 결재 쪽 발행 지점 | 상신(`SUB-003`) · 재상신(`SUB-010`, 상신 재사용) · 승인(`PRC-004`/`PRC-004-1`) · 반려(`PRC-008`) |

이 부분은 REST 엔드포인트가 없어 `.ai/api/README.md`의 "노션 반영 필요" 게이트 대상이 아니다 — `NOTI-V1.md` GEN-001~004만으로 바로 구현 가능하다.

---

## 참고 — `RET-001` 알림 자동 정리 배치 (REST API 아님, 2026-08-06 신설)

| 항목 | 내용 |
|---|---|
| 요구사항 | RET-001 |
| 배경 | 개별 삭제 API(구 #2)를 없애면서, 알림이 무한정 쌓이는 것을 막기 위해 대체한 기능. 화면에도 개별 삭제 진입점이 없어 API·화면 둘 다 없음으로 정리 |
| 구현 | `NotificationRetentionScheduler`(`@Scheduled`) — 매일 정해진 시각(기본 새벽 3시, `notification.retention.cron` 설정값)에 `createdAt`이 3개월(코드 상수 `RETENTION_MONTHS` — 운영 튜닝값이 아니라 `RET-001` 정책값이라 설정으로 안 뺌) 지난 알림을 전 사용자 대상으로 일괄 논리 삭제(`deleted_at` 기록) |
| 사용자 영향 | 별도 액션 없이, 3개월 지난 알림은 `GET /api/v1/notifications` 목록에서 자동으로 사라진다(`deleted_at IS NULL` 필터에 걸림) |

이 부분도 REST 엔드포인트가 없어 노션 반영 게이트 대상이 아니다.
