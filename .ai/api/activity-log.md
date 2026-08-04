# 🧾 ActivityLog API

**상태**: `📝 초안` — 노션 미반영. 구현 금지 (`../API.md` §0·§1)
**최종 업데이트**: 2026-08-04 · **담당**: 김용준
**노션**: 미반영 · 예정 Domain `프로젝트` · SUB-Domain `ActivityLog`
**팀 공유 수집 컨벤션**: 노션 공유 문서 기준

> 📝 **레포 설계 초안이다.** 팀 리뷰와 노션 반영이 끝난 뒤 상태를 `✅ 확정`으로 변경해야 구현할 수 있다.
> 확정 이후에는 경로·필드명·타입·상태코드·에러코드를 임의로 바꾸지 않는다.

## 엔드포인트

| API명칭 | METHOD | URL | 권한 |
|---|---|---|---|
| Step 활동 기록 조회 | GET | `/api/v1/steps/{stepId}/activity-logs` | 스텝 접근 권한 |

⛔ 로그 생성·수정·삭제를 위한 외부 API는 만들지 않는다.

각 도메인의 데이터 변경 Service가 Spring 동기 이벤트를 발행하고 Activity Log 도메인이 같은 트랜잭션에서 자동 저장한다.

## 공통 응답 형식

```json
{
  "httpStatus": 200,
  "message": "요청 성공",
  "data": {}
}
```

실패 응답은 `{ httpStatus, message, code }` 형식을 사용한다.

## 🔑 Activity Log는 Block 단위 활동 이력이다

| 원칙 | 내용 |
|---|---|
| 조회 기준 | Step 활동 기록 조회 API에서 Step 하위 Block 로그를 조회한다 |
| 저장 기준 | 모든 로그는 `blockId`를 필수로 가진다 |
| 수집 범위 | Block 자체와 Block 내부 데이터의 사용자 주요 활동 |
| 제외 범위 | Issue 생성·수정·상태 변경·삭제는 현재 Activity Log 대상이 아니다 |
| 생성 주체 | FE나 Controller가 아니라 각 원본 도메인 Application Service |
| Block 자체 활동 | `resourceId = null` |
| Block 내부 데이터 활동 | `resourceId = 해당 내부 데이터 ID` |
| 수정 저장 | 실제 변경 필드 하나당 `activity_log` 한 행 |
| 작업자 | 현재 인증 사용자의 `employee.user_id` |
| 처리 방식 | Spring 동기 이벤트 + `@TransactionalEventListener(BEFORE_COMMIT)` |

`projectId`는 이벤트와 로그 테이블에 저장하지 않는다. 로그 조회는 Step 기준이며, `blockId`를 통해 Step·Project를 역추적할 수 있다.

### 현재 Action

| API 값 | DB 값 | 의미 |
|---|---|---|
| `CREATE` | `create` | 대상 생성 |
| `MODIFY` | `modify` | 대상 수정 |
| `DELETE` | `delete` | 대상 삭제 |

⛔ 이미지 다운로드는 현재 `activity_log.act`에 대응 값이 없으므로 이 명세에 포함하지 않는다. `DOWNLOAD` 추가가 확정되기 전까지 `MODIFY`로 변조해 저장하지 않는다.

### 현재 저장 필드

| DB 컬럼 | Java 필드 | 필수 | 설명 |
|---|---|:---:|---|
| `activity_log_id` | `activityLogId` | Y | 로그 PK |
| `act` | `act` | Y | `CREATE` · `MODIFY` · `DELETE` |
| `created_at` | `createdAt` | Y | 로그 생성 시각 |
| `resource_id` | `resourceId` | N | Block 자체 활동이면 `null`, 내부 데이터 활동이면 해당 데이터 ID |
| `field` | `field` | N | 변경 필드명. 생성·삭제면 `null` 허용 |
| `before_value` | `beforeValue` | N | 변경 전 값 |
| `after_value` | `afterValue` | N | 변경 후 값 |
| `block_id` | `blockId` | Y | 활동이 발생한 Block ID |
| `user_id` | `userId` | Y | 행위자 사번 |

현재 ERD에는 `project_id`, `resource_type`, `target_name`, `privileged_override` 컬럼이 없다. 해당 정보가 필요해지면 Activity Log DDL·Entity·이벤트·조회 API를 함께 변경한다.

### 도메인별 필수 수집 로그

| 도메인 | 필수 수집 활동 | `resourceId` 기준 | 필드명 규칙 |
|---|---|---|---|
| Block 공통 | Block 생성 · Block명 수정 · Block 삭제 | Block 자체 활동은 `null` | Block 엔티티 필드명 기준 |
| 체크리스트 | 항목 생성 · 내용 수정 · 체크 상태 수정 · 항목 삭제 | 체크리스트 항목 ID | 체크리스트 도메인 필드명 기준 |
| 이미지 | 이미지 생성 · 캡션 수정 · 순서 수정 · 이미지 삭제 | 이미지 ID | 이미지 도메인 필드명 기준 |
| 텍스트 | 본문 수정 | 텍스트 블록 내부 데이터 ID. 별도 내부 ID가 없으면 `null` | 텍스트 도메인 필드명 기준 |
| 파일·파일 버전 | 업로드 완료 · 파일명 수정 · 파일 삭제 | 파일 또는 파일 버전 ID | 파일 도메인 필드명 기준 |

파일은 여러 Block에서 공통 사용될 수 있으므로 이벤트 발행자가 **사용자 작업이 발생한 `blockId`**를 반드시 전달한다.

위 표는 최소 수집 기준이다. 각 블록 도메인은 자기 요구사항에 맞춰 `field` 이름과 `beforeValue`·`afterValue` 표현을 정하되, 프론트 표시와 장애 추적이 가능하도록 도메인 문서 또는 API 문서에 필드명 의미를 남긴다.

---

## 1. Step 활동 기록 조회

| 항목 | 내용 |
|---|---|
| Method · URL | `GET /api/v1/steps/{stepId}/activity-logs` |
| 인증 필요 | Y · 스텝 접근 권한 |
| 요구사항 | QRY-001~007 · USC-QRY-001~010 |

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `stepId` | Long | Y | 활동 기록을 조회할 Step 번호 |

**Request Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `blockId` | Long | N | 전달 시 해당 Block의 활동만 조회 |
| `cursor` | Long | N | 이전 응답의 `nextCursor`. 최초 요청은 생략 |
| `size` | int | N | 조회 개수. 기본 20 |

⛔ `activityType`, `action`, 날짜 범위 필터는 현재 제공하지 않는다.

⛔ 별도 `/blocks/{blockId}/activity-logs` API를 만들지 않는다. Block 상세 필터도 동일 API에 `blockId`를 전달한다.

### 커서 규칙

```text
최초 요청
GET /api/v1/steps/10/activity-logs?size=20

다음 요청
GET /api/v1/steps/10/activity-logs?cursor={nextCursor}&size=20

Block 필터 다음 요청
GET /api/v1/steps/10/activity-logs?blockId=15&cursor={nextCursor}&size=20
```

- `activityLogId` 내림차순으로 조회한다.
- `cursor`가 있으면 `activity_log_id < cursor` 조건을 적용한다.
- `size + 1`건을 조회해 `hasNext`를 계산할 수 있다.
- Block 필터를 사용한 다음 요청에도 같은 `blockId`를 유지한다.
- 전체 개수와 페이지 번호는 반환하지 않는다.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.activities[].activityLogId` | Long | 활동 로그 번호 |
| `data.activities[].action` | String | `CREATE` · `MODIFY` · `DELETE` |
| `data.activities[].description` | String | Block 유형·Action·필드를 기준으로 BE가 만든 활동 문구. 행위자 이름은 제외 |
| `data.activities[].fieldName` | String | 변경 필드명. 생성·삭제면 `null` |
| `data.activities[].beforeValue` | String | 변경 전 값. `null` 허용 |
| `data.activities[].afterValue` | String | 변경 후 값. `null` 허용 |
| `data.activities[].resourceId` | Long | Block 자체 활동이면 `null`, 내부 데이터면 해당 ID |
| `data.activities[].actor.userId` | String | 행위자 사번 |
| `data.activities[].actor.name` | String | 행위자 이름 |
| `data.activities[].block.blockId` | Long | 활동이 발생한 Block 번호 |
| `data.activities[].block.title` | String | 현재 Block명 |
| `data.activities[].block.type` | String | Block 타입 |
| `data.activities[].createdAt` | String | 활동 발생 일시 `yyyy-MM-dd'T'HH:mm:ss` |
| `data.nextCursor` | Long | 다음 조회 cursor. 다음 데이터가 없으면 `null` |
| `data.hasNext` | boolean | 다음 데이터 존재 여부 |

> Activity Log에는 Block명 스냅샷 컬럼이 없다. 현재 명세는 조회 시 현재 Block명을 조합한다. 과거 시점의 Block명을 보존해야 한다면 DDL과 API를 함께 변경해야 한다.

> FE는 `actor.name + description`으로 문장을 표시하고, `createdAt`을 기준으로 오늘·어제·날짜 그룹과 상대 시간을 렌더링한다.

> 조회 결과가 없으면 `200`, 빈 `data.activities`, `nextCursor: null`, `hasNext: false`를 반환한다.

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 |
| 400 | `LOG_INVALID_REQUEST` | cursor 또는 size 형식 오류 |
| 400 | `LOG_BLOCK_STEP_MISMATCH` | 필터 Block이 요청 Step에 속하지 않음 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `LOG_ACCESS_PERMISSION_REQUIRED` | Step 열람 권한 없음 |
| 404 | `LOG_STEP_NOT_FOUND` | Step 없음 또는 논리 삭제됨 |
| 404 | `LOG_BLOCK_NOT_FOUND` | 필터 Block 없음 또는 논리 삭제됨 |

---

## 내부 수집 컨벤션 — 외부 API 아님

이 섹션은 Block 관련 도메인 담당자가 Activity Log를 남기기 위해 알아야 하는 팀 내부 계약이다. 외부 API Request·Response가 아니다.

### 이벤트 구조

```java
public record ActivityOccurredEvent(
    ActivityLogAction action,
    Long blockId,
    Long resourceId,
    String actorId,
    List<ActivityFieldChange> changes
) {}
```

```java
public record ActivityFieldChange(
    String field,
    String beforeValue,
    String afterValue
) {}
```

### 발행·저장 흐름

```text
각 도메인 @Transactional Service
→ 변경 전 값 확보
→ 원본 데이터 변경
→ 실제 변경 필드 비교
→ ActivityOccurredEvent 발행
→ @TransactionalEventListener(BEFORE_COMMIT)
→ Activity Log 필드별 저장
→ 전체 트랜잭션 커밋
```

- Controller에서 이벤트를 발행하지 않는다.
- JPA Entity를 이벤트에 직접 넣지 않는다.
- `actorId`를 FE Request에서 받지 않는다.
- `projectId`를 이벤트에 넣지 않는다.
- `blockId`는 항상 필수다. 부모 엔티티가 추가되어도 Activity Log의 조회 기준은 흔들지 않는다.
- 동일 값 수정은 이벤트와 로그를 생성하지 않는다.
- 한 API에서 여러 필드가 바뀌면 `changes`에 여러 항목을 전달하고 로그 도메인이 여러 행으로 저장한다.
- 각 원본 도메인은 `ActivityLogJpaRepository`를 직접 사용하지 않는다.

### 각 도메인이 갖춰야 할 구조

| 위치 | 담당 | 규칙 |
|---|---|---|
| 원본 도메인 Service | 각 블록 도메인 | 실제 DB 변경이 완료되는 `@Transactional` Service에서 이벤트를 발행한다 |
| 이벤트 발행기 | 공통 | `DomainEventPublisher`를 주입받아 발행한다 |
| 이벤트 객체 | Activity Log 계약 | `ActivityOccurredEvent`와 `ActivityFieldChange`만 사용한다 |
| 로그 저장 | Activity Log 도메인 | `ActivityLogEventListener`와 `ActivityLogWriter`가 담당한다 |
| Repository 접근 | Activity Log 도메인 | 타 도메인은 `ActivityLogJpaRepository`를 직접 호출하지 않는다 |

### 발행 예시

```java
domainEventPublisher.publish(ActivityOccurredEvent.of(
        ActivityLogAction.MODIFY,
        blockId,
        checklistItemId,
        actorId,
        List.of(new ActivityFieldChange("content", beforeContent, afterContent))
));
```

생성·삭제처럼 변경 필드가 하나로 특정되지 않는 활동도 `changes`는 비울 수 없다. 이 경우 `field`, `beforeValue`, `afterValue`를 모두 `null`로 둔 `ActivityFieldChange` 하나를 전달한다.

```java
domainEventPublisher.publish(ActivityOccurredEvent.of(
        ActivityLogAction.CREATE,
        blockId,
        checklistItemId,
        actorId,
        List.of(new ActivityFieldChange(null, null, null))
));
```

### 부모 엔티티 추가 시 수정 방향

부모 엔티티가 나중에 추가되어도 타 도메인이 따라야 할 이벤트 계약은 최대한 유지한다.

| 변화 | 수정 위치 | 타 도메인 영향 |
|---|---|---|
| 조회에서 Step·Project 조인이 필요 | Activity Log 조회 Repository/Service | 없음. 기존 이벤트 발행 구조 유지 |
| `blockId`만으로 부모 추적이 불가능 | Activity Log DDL·Entity·Event 계약 변경 필요 | 있음. 팀 합의 후 전체 도메인 이벤트 발행부 수정 |
| Block 내부 데이터 타입 구분 필요 | 우선 각 도메인의 `field`/설명 조합으로 처리 | 없음 |
| `resource_type` 재도입 필요 | DDL·Entity·Event·API Response 변경 | 있음. 노션 명세 확정 후 진행 |

현재 구조에서는 `blockId`가 부모 추적의 기준이다. 따라서 부모 엔티티가 추가되더라도 `blockId → Step → Project` 관계가 유지되면 타 블록 도메인은 수정하지 않는다.
