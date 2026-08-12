# 🔔 Notification API — 알림

**상태**: `✅ 확정` — 이 문서가 **단일 계약**이다. 이탈 금지 규칙 전면 적용(`../API.md` §0)
**최종 업데이트**: 2026-08-12 (**실시간 수신(SSE) 엔드포인트 신설** — 새로고침 없이 알림이 도착한다 · §5)
**최종 업데이트**: 2026-08-11 (결재 참여 불가 알림 2종 추가 — API 구조 변경 없음)
**최종 업데이트**: 2026-08-07 (화면이 개별 처리 방식으로 바뀜 — **개별 삭제 부활 · 개별 읽음 신설 · 전체 읽음 제거**) · 2026-08-07 (이동 대상 판정 방식 변경 — block 경유 → 발행 도메인이 직접 지정, 목록에서 `blockId` 제거) · **담당**: 이강욱
**프론트 공유**: 필요 — 아래 변경은 기존 계약을 바꾸므로 프론트에 알려야 한다

> ⚠️ **2026-08-10 신설 — `APPROVAL_CANCELED`**. 블록·스텝 삭제로 진행 중 결재가 종결되면 기안자와
> 그 시점 결재 차례였던 사람에게 발행된다. **이동 대상이 없다**(`GET /{id}/target` 이 `type=NONE`) —
> 결재가 삭제돼 상세 조회가 404 이므로 이동시키면 에러 화면으로 보내게 된다. 프론트는 이 유형을
> 클릭 시 이동 없이 안내만 하면 된다.

> ✅ **2026-08-11 신설 — 참여 불가 알림 2종.** 기존 목록·이동 대상 API 형식은 그대로다.
>
> | 유형 | 수신자 | 이동 |
> |---|---|---|
> | `APPROVAL_APPROVER_UNAVAILABLE` | 현재 유효한 기안자/대행 기안자 | 결재 상세(`type=APPROVAL`, `extra.revisionId`) |
> | `APPROVAL_DRAFTER_UNAVAILABLE` | 유효 스텝 EDITOR 전원 | 결재 상세(`type=APPROVAL`, `extra.revisionId`) |
> ✅ **2026-08-12 신설 — 실시간 수신(SSE, §5).** 지금까지는 알림이 생겨도 목록을 다시 조회(새로고침)해야 보였다.
> `GET /api/v1/notifications/stream` 을 구독하면 알림이 생기는 즉시 서버가 밀어준다.
> **기존 4개 API 의 경로·필드·응답은 하나도 바뀌지 않았다** — 추가만 됐다.

**범위**: 알림 REST API 4개(목록조회·개별삭제·이동대상조회·개별읽음) + 실시간 수신 SSE 1개(§5). 생성 이벤트 인프라(`#27`)는 REST 엔드포인트가 없어 여기 없음(맨 아래 "참고" 절)

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
| 5 | 실시간 알림 수신(SSE) | GET | `/api/v1/notifications/stream` | 인증 사용자(본인 알림만) |

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
| `data.content[].notificationType` | String | 알림 유형(예: `APPROVAL_REQUESTED`/`APPROVAL_REJECTED`/`APPROVAL_COMPLETED`/`APPROVAL_CANCELED`) |
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

## 5. 실시간 알림 수신 (SSE)

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/notifications/stream` |
| 인증 필요 | Y · 본인 알림만 (세션 쿠키) |
| 응답 Content-Type | `text/event-stream` |
| 요구사항 | RT-001~006 (신설) |

**Request** — 파라미터 없음. 인증은 기존 세션 쿠키(`SESSION`)를 그대로 쓴다.

> ⚠️ 프론트는 `withCredentials` 를 **반드시** 켜야 한다. 안 켜면 쿠키가 실리지 않아 401 이다.
> ```js
> const es = new EventSource('/api/v1/notifications/stream', { withCredentials: true });
> ```

**서버가 보내는 이벤트 3종**

| event 이름 | 시점 | data |
|---|---|---|
| `connected` | 구독 직후 1회 | `{"userId":"EMP001"}` — 연결 확인용. 프론트는 이때 **목록을 한 번 재조회**한다(아래 RT-005) |
| `notification` | 알림이 생성될 때마다 | **§1 목록 항목과 완전히 같은 구조** (`notificationId`/`notificationType`/`title`/`message`/`readAt`/`createdAt`) |
| (주석) | 15초마다 | `:ping` — SSE 주석 줄이라 `EventSource` 이벤트로 올라오지 않는다. 프론트가 처리할 것 없음 |

**Stream Example** (실제 와이어 형식)

```
event: connected
data: {"userId":"EMP001"}

:ping

event: notification
data: {"notificationId":301,"notificationType":"APPROVAL_REQUESTED","title":"결재 요청","message":"출장비 정산 결재 요청이 도착했습니다.","readAt":null,"createdAt":"2026-08-12T09:00:00"}
```

**프론트 처리**

```js
es.addEventListener('notification', (e) => {
  const n = JSON.parse(e.data);
  prependToList(n);          // 목록 맨 위에 추가 (항상 최신순 — VIW-002)
  increaseUnreadBadge();     // readAt 이 항상 null 이라 무조건 안 읽음 1건 증가
});
es.addEventListener('connected', () => refetchNotificationList());  // RT-005
```

| 코드 | 상태 | code | 설명 |
|---|---|---|---|
| 200 | OK | – | 구독 성공(스트림 시작) |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 로그인이 필요합니다 |

**비즈니스 규칙**

| # | 규칙 | 상세 |
|---|---|---|
| RT-001 | 본인 알림만 받는다 | 세션의 사번으로만 구독한다. 구독 대상을 파라미터로 지정할 수 없다(타인 알림 도청 불가) |
| RT-002 | **저장이 커밋된 뒤에 보낸다** | 알림 row 가 커밋된 후에만 전송한다. 받는 즉시 목록을 조회해도 그 알림이 반드시 보인다 |
| RT-003 | 탭이 여러 개면 전부 받는다 | 같은 사용자의 연결 N 개에 동일하게 보낸다. 브라우저 탭마다 별도 연결이다 |
| RT-004 | 전송 실패는 알림을 지우지 않는다 | 연결이 끊겨 전송이 실패해도 **알림 row 는 이미 저장돼 있다.** 다음 목록 조회에서 정상적으로 보인다 — 실시간은 **보조 경로**이고 목록 API 가 정본이다 |
| RT-005 | 재연결 공백은 프론트가 메운다 | `EventSource` 는 끊기면 자동 재연결하지만 **끊긴 사이 발행된 알림은 스트림으로 오지 않는다.** 그래서 `connected` 를 받을 때마다 목록을 재조회한다 |
| RT-006 | 30분마다 재연결된다 | 서버가 연결을 30분에 한 번 정상 종료한다(좀비 커넥션 회수). 브라우저가 자동으로 다시 붙고 `connected` 가 다시 오므로 사용자에게 보이는 영향은 없다 |

> 🚨 **실시간은 정본이 아니다.** 읽음 여부·목록·개수의 기준은 항상 §1 목록 API 다.
> SSE 는 "지금 하나 생겼다"는 신호일 뿐이며, 유실돼도 데이터는 DB 에 남아 있다(RT-004).

> ⚠️ **배포 시 필수 — nginx 버퍼링을 꺼야 한다.** 켜져 있으면 이벤트가 nginx 에 고여서
> **로컬에서는 실시간인데 배포하면 여전히 새로고침이 필요한** 증상이 난다.
> 서버가 `X-Accel-Buffering: no` 를 응답에 실어 보내지만, nginx 설정에서 `proxy_buffering off;`
> 를 함께 두는 것을 권장한다(`.ai/INFRA.md` 반영 필요).

> ⚠️ **알림 개수(배지)를 위한 별도 API 는 만들지 않았다.** 안 읽은 개수는 기존 목록 API 로 구한다 —
> `GET /api/v1/notifications?isRead=false&size=1` 의 `data.totalElements`.

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

### 3. 여러 명에게 보낼 때 — **사람 수만큼 발행한다**

이벤트 하나에 수신자 목록을 담는 필드는 **없다.** 알림은 수신자 1명당 1행이라(`GEN-004`),
받는 사람마다 이벤트를 하나씩 발행한다. 각자 따로 읽음·삭제할 수 있어야 하므로 행이 나뉘어야 한다.

```java
// 프로젝트 참여자 전원에게 초대 알림
List<String> memberIds = List.of("EMP001", "EMP002", "EMP003");

memberIds.forEach(userId ->
        domainEventPublisher.publish(NotificationRequestedEvent.of(
                userId, "PROJECT_INVITED", "프로젝트 초대",
                project.getName() + "에 초대되었습니다.",
                "PROJECT", project.getProjectId(), null)));
```

**주의**

- **본인은 제외해야 하는 경우가 많다.** 초대한 사람에게 "당신이 초대됐습니다"가 가면 어색하다.
  ```java
  memberIds.stream()
          .filter(userId -> !userId.equals(actorId))   // 행위자 제외
          .forEach(...);
  ```
- 같은 사람에게 **중복 발행하지 않도록** 목록에 중복이 없는지 확인한다(알림 쪽에서 걸러주지 않는다).
- 수신자가 많아도 반복 발행이면 충분하다 — 리스너가 커밋 후 각각 저장한다.

### 4. `targetContext`에 뭘 넣어야 하나 ⭐

**판단 기준은 "우리 도메인 데이터"가 아니라 "프론트가 그 화면을 여는 데 필요한 것"이다.**

`targetId`(PK) 하나만으로 화면이 열리면 `null`을 넣고, 부족하면 부족한 값을 채운다.

| 도메인 | 화면을 열려면 | `targetContext` |
|---|---|---|
| 프로젝트 | 프로젝트 PK 하나면 충분 | `null` |
| 결재 | 어느 **회차**인지 필요 | `Map.of("revisionId", revisionId)` |
| 이슈 | 프로젝트·스텝 **경로**가 필요 | `Map.of("projectId", ..., "stepId", ...)` |

```java
// 이슈 — 계층 경로가 필요한 경우
domainEventPublisher.publish(NotificationRequestedEvent.of(
        userId, "ISSUE_ASSIGNED", "이슈 배정",
        issue.getTitle() + " 이슈 담당자로 지정되었습니다.",
        "ISSUE", issueId,
        Map.of("projectId", step.projectId(), "stepId", step.stepId())));
```

> 🚨 **모르겠으면 프론트에 "이 화면 여는 데 뭐가 필요해?"라고 물어보는 게 가장 빠르다.**
> `null`로 두면 알림은 정상 생성되고 클릭도 되는데 **화면 이동만 조용히 실패**한다 —
> 에러가 안 나서 늦게 발견된다(실제로 이슈에서 한 번 겪었다).

**넣지 말 것** — 제목·작성자명 같은 **표시용** 데이터. 그건 `title`/`message`가 담당한다.
이동 데이터와 표시 데이터가 섞이면 도메인마다 JSON 구조가 제각각 커진다.

### 지켜야 할 규칙

| 규칙 | 안 지키면 |
|---|---|
| **`@Transactional` 메서드 안에서 발행** | 트랜잭션 밖에서 발행하면 `AFTER_COMMIT` 리스너가 **아예 실행되지 않는다.** 에러도 안 나고 알림만 조용히 사라져서 원인 찾기 어렵다 ⚠️ |
| `targetType`·`targetId`는 **둘 다 넣거나 둘 다 빼기** | `IllegalArgumentException` (DB `CHECK` 제약으로도 막힘) |
| `targetContext`는 **대상이 있을 때만** | 위와 동일하게 거부됨 |
| `targetContext`에는 **이동에 필요한 값만** | 제목 같은 표시용 데이터를 넣으면 이동 데이터와 섞이고 도메인마다 JSON이 비대해진다. 표시 내용은 `title`/`message`가 담당 |

### 알아둘 것

- **실시간 전송은 자동이다.** 이벤트를 발행하면 저장 후 SSE(§5)로 자동 전송된다 — 발행하는 도메인이 추가로 할 일은 **없다.**
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
