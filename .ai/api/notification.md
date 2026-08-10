# 🔔 Notification API — 알림

**상태**: `✅ 확정` — 이 문서가 **단일 계약**이다. 이탈 금지 규칙 전면 적용(`../API.md` §0)
**최종 업데이트**: 2026-08-07 (화면이 개별 처리 방식으로 바뀜 — **개별 삭제 부활 · 개별 읽음 신설 · 전체 읽음 제거**) · 2026-08-07 (이동 대상 판정 방식 변경 — block 경유 → 발행 도메인이 직접 지정, 목록에서 `blockId` 제거) · **담당**: 이강욱
**프론트 공유**: 필요 — 아래 변경은 기존 계약을 바꾸므로 프론트에 알려야 한다
**범위**: 알림 REST API 4개(목록조회·개별삭제·이동대상조회·개별읽음). 생성 이벤트 인프라(`#27`)는 REST 엔드포인트가 없어 여기 없음(맨 아래 "참고" 절)

> ✅ **경로·필드명·타입·상태코드·에러코드를 한 글자도 바꾸지 않는다** (`../API.md` §0).
> ⚠️ **2026-08-07 변경(2차)**: 화면이 일괄 처리에서 **개별 처리**로 바뀌었다.
> - `DELETE /api/v1/notifications/{notificationId}` **부활** (2026-08-06에 폐지했던 것)
> - `PATCH /api/v1/notifications/{notificationId}/read` **신설**
> - `PATCH /api/v1/notifications/read-all` **제거**
> - 3개월 자동 정리 배치(구 `RET-001`)도 함께 **제거** — 개별 삭제가 없던 동안의 대체재였다
>
> 요구사항 근거: [`../docs/domain/결재·알림/NOTI-V1.md`](../docs/domain/결재·알림/NOTI-V1.md)

## 엔드포인트

| # | API명칭 | METHOD | URL | 권한 |
|---|---|---|---|---|
| 1 | 알림 목록 조회 | GET | `/api/v1/notifications` | 인증 사용자(본인 알림만) |
| 2 | 알림 삭제 | DELETE | `/api/v1/notifications/{notificationId}` | 인증 사용자(본인 알림만) |
| 3 | 알림 이동 대상 조회 | GET | `/api/v1/notifications/{notificationId}/target` | 인증 사용자(본인 알림만) |
| 4 | 알림 읽음 처리 | PATCH | `/api/v1/notifications/{notificationId}/read` | 인증 사용자(본인 알림만) |

⭐ **알림 생성 공개 API는 없다**(`INV-01`) — `#27`(이벤트 인프라)이 내부적으로만 생성한다.
⭐ **읽음 처리 경로는 두 가지다** — 이동 대상 조회(#3) 시 자동(`ACT-004`), 또는 명시 호출(#4, `ACT-006`). 이동 없이 읽음만 표시할 때 #4를 쓴다.
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

**비즈니스 규칙**: 수신자 본인만 삭제 가능(ACT-002) · 논리 삭제(`deleted_at`), 하드 삭제 아님(ACT-001 · INV-05) · 삭제 후 목록에서 제외됨(VIW-005) · 이미 삭제된 알림 재삭제는 404(ACT-003) — 없는 알림과 같은 응답이라 존재 여부가 드러나지 않는다.

---

## 3. 알림 이동 대상 조회

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
{ "httpStatus": 200, "message": "알림 이동 대상 조회 성공", "data": { "type": "ISSUE", "targetId": 12, "extra": { "projectId": 3, "stepId": 10 } } }
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

## 4. 알림 읽음 처리

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/notifications/{notificationId}/read` |
| 인증 필요 | Y · 본인 알림만 |
| 요구사항 | ACT-006 |

**Request** — `notificationId`(Path, long, Y) · 바디 없음

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.notificationId` | long | 읽음 처리된 알림 구분 번호 |
| `data.readAt` | String | 읽은 시각. **이미 읽은 알림이면 최초 읽음 시각이 그대로 반환된다**(덮어쓰지 않음) |

**Success Example**

```json
{ "httpStatus": 200, "message": "읽음 처리 성공", "data": { "notificationId": 301, "readAt": "2026-08-07T09:12:00" } }
```

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 처리 성공(이미 읽은 알림도 200 — 멱등) |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 로그인이 필요합니다 |
| 403 | Forbidden | `NOTIFICATION_FORBIDDEN` | 다른 사용자의 알림 읽음 처리 시도 |
| 404 | Not Found | `NOTIFICATION_NOT_FOUND` | 존재하지 않거나 이미 삭제된 알림 |

**비즈니스 규칙**: 수신자 본인만 처리 가능 · **멱등** — 이미 읽은 알림을 다시 호출해도 `read_at`을 덮어쓰지 않고 최초 시각을 유지한다(ACT-006) · 이동 대상 조회(#3)의 자동 읽음(ACT-004)과 **병행**한다. 클릭해서 이동하면 #3이 알아서 처리하고, **이동 없이 읽음만 표시**할 때 이 API를 쓴다.

---

## 📌 타 도메인 연동 가이드 — 알림을 보내려면

> **다른 도메인 담당자가 읽어야 할 부분.** 알림을 보내려면 **이벤트 발행 한 줄**이면 된다.
> 알림 도메인 코드도, 공용 파일도 건드리지 않는다(`GEN-003` · `INV-02`).

### 1. 이동 대상이 있는 알림 (클릭하면 그 화면으로 감)

```java
private final DomainEventPublisher domainEventPublisher;   // 주입

// ⚠️ 반드시 @Transactional 메서드 안에서, 실제 데이터 변경이 끝난 뒤에 발행한다
domainEventPublisher.publish(NotificationRequestedEvent.of(
        수신자사번,                       // "EMP003"
        "PROJECT_INVITED",              // notificationType — 자유 문자열
        "프로젝트 초대",                  // 알림 제목
        project.getName() + "에 초대되었습니다.",   // 알림 내용
        "PROJECT",                      // targetType — 자기 도메인 이름
        project.getProjectId(),         // targetId — 자기 PK
        null));                         // targetContext — 부가 식별값 없으면 null
```

### 2. 이동 대상이 없는 알림 (시스템 공지 등)

```java
domainEventPublisher.publish(NotificationRequestedEvent.of(
        수신자사번, "SYSTEM_NOTICE", "점검 안내", "8월 10일 02:00~04:00 점검 예정입니다."));
```

이 경우 이동 대상 조회는 `type=NONE`을 반환한다(에러 아님). 목록·읽음·삭제는 동일하게 동작한다.

### 지켜야 할 규칙

| 규칙 | 안 지키면 |
|---|---|
| **`@Transactional` 메서드 안에서 발행** | 트랜잭션 밖에서 발행하면 `AFTER_COMMIT` 리스너가 **아예 실행되지 않는다.** 에러도 안 나고 알림만 조용히 사라져서 원인 찾기 어렵다 ⚠️ |
| `targetType`·`targetId`는 **둘 다 넣거나 둘 다 빼기** | `IllegalArgumentException` (DB `CHECK` 제약으로도 막힘) |
| `targetContext`는 **대상이 있을 때만** | 위와 동일하게 거부됨 |
| `targetContext`에는 **이동에 필요한 값만** | 제목 같은 표시용 데이터를 넣으면 이동 데이터와 섞이고 도메인마다 JSON이 비대해진다. 표시 내용은 `title`/`message`가 담당 |

### 알아둘 것

- `notificationType`·`targetType`은 **미리 등록할 필요가 없다.** 처음 보는 값이어도 그대로 저장·응답된다.
- 수신자 **1명당 1건**이다(`GEN-004`). 여러 명에게 보내려면 사람 수만큼 발행한다.
- `targetContext`는 **생성 시점 스냅샷**이다(`VIW-010`). 클릭 시점에 재조회하지 않으므로, 그 사건 당시의 값을 넣어야 알림 문구와 목적지가 일치한다.
- 프론트는 응답의 `type`/`targetId`/`extra`로 이동한다. 백엔드가 URL을 만들어 주지는 않는다.

### 레퍼런스 구현

`approval/application/service/ApprovalCommandService`의 `publishApprovalNotification()` 과 그 호출부 4곳
(상신 · 승인 시 다음 결재자 · 승인 완료 · 반려)을 참고하면 된다.

---

## 참고 — `#27` 생성 이벤트 인프라 (REST API 아님)

| 항목 | 내용 |
|---|---|
| 요구사항 | GEN-001~005 |
| 이벤트 클래스 | `NotificationRequestedEvent(recipientUserId, notificationType, title, message, targetType, targetId, targetContext)` |
| 패키지 | `notification.domain.event`(자기 도메인 소유, `global` 아님) |
| 처리 방식 | `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Transactional(REQUIRES_NEW)` — 발행 트랜잭션 커밋 후에만 `notification` row 생성 |
| 결재 쪽 발행 지점 | 상신(`SUB-003`) · 승인 시 다음 결재자(`PRC-002`) · 승인 완료(`PRC-002`) · 반려(`PRC-008`). **재상신은 별도 발행 없음** — 새 회차를 만든 뒤 사용자가 상신을 호출할 때 발행된다 |

이 부분은 REST 엔드포인트가 없어 프론트 계약과 무관하다 — 백엔드 도메인끼리의 내부 규약이다.

---

## 참고 — 폐기된 기능 (이력)

| 기능 | 폐기 시점 | 사유 |
|---|---|---|
| `PATCH /api/v1/notifications/read-all` (전체 읽음) | 2026-08-07 | 화면이 개별 읽음 방식으로 바뀌어 일괄 처리 진입점이 없어짐 → #4로 대체 |
| `RET-001` 3개월 자동 정리 배치 | 2026-08-07 | 개별 삭제 API가 없던 동안(2026-08-06~07)의 대체재. 삭제(#2)가 부활하면서 근거 소멸 |

> 잠깐 사라졌다 돌아온 것: 개별 삭제(#2)는 2026-08-06에 폐지했다가 2026-08-07에 부활했다.
> ⚠️ 자동 정리를 없애면서 **알림은 사용자가 지우지 않으면 계속 쌓인다.** 인원 규모상 감수하기로 했고,
> 나중에 문제가 되면 보존 정책을 다시 도입한다.
