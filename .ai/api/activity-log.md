# ActivityLog API

**상태**: md 명세 기준 계약 (`../API.md` §0·§1)
**최종 업데이트**: 2026-08-11 (Project 상속 멀티테넌시 2차 적용 — Step·Block·목록 조회가 `activity_log.company_id`와 부모 `project.company_id`를 모두 만족할 때만 반환, API 계약 변경 없음) · **담당**: 김용준
**최종 업데이트**: 2026-08-11 (활동 수행자 응답에 `resignedAt` 추가 및 FE 사용 API 명시 — 사원은 삭제하지 않고 퇴사일을 기록하므로, 과거 활동의 수행자 이름을 보존하면서 FE가 퇴사 상태를 표시함) · **담당**: 김용준
**최종 업데이트**: 2026-08-07 (결재선 `lines` 값을 사번→이름으로 변경, "필드별 표시 규칙" FE 가이드 섹션 신설) · **담당**: 김용준
**Domain**: `프로젝트` · SUB-Domain `ActivityLog`

## §0 엔드포인트 요약

| 메서드 | 경로 | 무엇 | 상태 |
|---|---|---|---|
| GET | `/api/v1/steps/{stepId}/activity-logs` | [스텝별 활동 기록 조회](#get-apiv1stepsstepidactivity-logs-스텝별-활동-기록-조회) | — |

> `.ai/api/*.md` 가 단일 계약이다. 경로·필드명·타입·상태코드·에러코드를 임의로 바꾸지 않는다.

---

# GET `/api/v1/steps/{stepId}/activity-logs` — 스텝별 활동 기록 조회

현재 Step에 속한 Block 및 Block 내부 데이터의 활동 기록을 최신순으로 조회한다.

`blockId`를 전달하면 특정 Block에서 발생한 활동 기록만 조회한다. 별도의 Block 활동 기록 API는 만들지 않는다.

Activity Log는 Sprint1에서 BE가 완성된 자연어 문장을 만들지 않고, 화면 조립에 필요한 원자 데이터를 반환한다.

## 기본 정보

| 항목 | 내용 |
| --- | --- |
| API 명 | 스텝별 활동 기록 조회 |
| Method | GET |
| URL | `/api/v1/steps/{stepId}/activity-logs` |
| 인증 필요 여부 | Y |
| 권한 | 프로젝트 참여자 |

## Path Parameter

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `stepId` | Long | Y | 조회할 Step ID |

## Query Parameter

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `blockId` | Long | N | 해당 Block의 활동 기록만 조회 |
| `cursor` | Long | N | 이전 응답의 `nextCursor` |
| `size` | int | N | 조회 개수, 기본값 `20`, 최대값 `100` |

```text
GET /api/v1/steps/10/activity-logs
GET /api/v1/steps/10/activity-logs?blockId=15
GET /api/v1/steps/10/activity-logs?cursor=481&size=20
GET /api/v1/steps/10/activity-logs?blockId=15&cursor=481&size=20
```

## Request Body

없음.

## Response Parameter

### 공통 응답

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `activities` | List | 활동 기록 목록 |
| `nextCursor` | Long | 다음 조회 커서, 없으면 `null` |
| `hasNext` | boolean | 다음 기록 존재 여부 |

### Activity Object

Activity Object 1개는 `activity_log` 1행에 대응한다. 한 수정 이벤트에서 여러 필드가 변경되면 응답에서도 여러 Activity Object로 내려갈 수 있다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `activityLogId` | Long | 활동 기록 ID |
| `action` | String | `CREATE`, `MODIFY`, `DELETE`, `RESTORE`, `PURGE`. `RESTORE`·`PURGE`는 파일 휴지통 복원·영구삭제 전용 |
| `targetType` | String | `BLOCK`, `RESOURCE` |
| `displayName` | String | FE 단순 표시용 이름. `resource.name`이 있으면 그 값, 없으면 `block.title` |
| `fieldName` | String | 수정 필드, 해당하지 않으면 `null` |
| `beforeValue` | String | 변경 전 값 |
| `afterValue` | String | 변경 후 값 |
| `resource` | Object | Block 내부 데이터. Block 자체 활동이면 `resourceId/name` 모두 `null` |
| `actor` | Object | 활동 수행자 |
| `block` | Object | 활동이 발생한 Block |
| `createdAt` | LocalDateTime | 활동 발생 일시 |

### Resource Object

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `resourceId` | Long | Block 내부 데이터 ID. Block 자체 활동이면 `null` |
| `name` | String | Block 내부 데이터 표시명 스냅샷. 없으면 `null` |

### Actor Object

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | String | 사용자 사번 |
| `name` | String | 사용자 이름 |
| `resignedAt` | LocalDate | 퇴사일. 재직 중이면 `null` |

### 퇴사 수행자 표시 — FE 연동 명세

`GET /api/v1/steps/{stepId}/activity-logs`의 `data.activities[].actor.resignedAt`은 JSON에서 `"yyyy-MM-dd"` 문자열 또는 `null`이다.

| 값 | FE 표시 규칙 |
|---|---|
| `null` | 기존처럼 `actor.name`만 표시 |
| 날짜 문자열 | `actor.name`은 그대로 표시하고, 옆에 `퇴사함` 상태를 표시. 과거 활동 기록을 목록에서 제거하지 않음 |

적용 화면은 Step 활동 기록 화면과 Block 활동 로그 팝업이다. Activity Log 조회 API는 이 엔드포인트 하나뿐이다.

### Block Object

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `blockId` | Long | Block ID |
| `title` | String | Block 제목 |
| `type` | String | Block 유형 |

`targetType` 구분 기준은 다음과 같다.

```text
resource.resourceId = null
→ targetType = BLOCK

resource.resourceId != null
→ targetType = RESOURCE
```

`displayName` 계산 기준은 다음과 같다.

```text
resource.name != null
→ displayName = resource.name

resource.name == null
→ displayName = block.title
```

## Success Example

```json
{
  "httpStatus": 200,
  "message": "활동 기록 조회 성공",
  "data": {
    "activities": [
      {
        "activityLogId": 501,
        "action": "MODIFY",
        "targetType": "RESOURCE",
        "displayName": "회사소개서 최신본 첨부",
        "fieldName": "completed",
        "beforeValue": "false",
        "afterValue": "true",
        "resource": {
          "resourceId": 41,
          "name": "회사소개서 최신본 첨부"
        },
        "actor": {
          "userId": "EMP003",
          "name": "이영희",
          "resignedAt": null
        },
        "block": {
          "blockId": 15,
          "title": "제안서 작성 체크리스트",
          "type": "CHECKLIST"
        },
        "createdAt": "2026-08-02T14:32:00"
      },
      {
        "activityLogId": 500,
        "action": "CREATE",
        "targetType": "BLOCK",
        "displayName": "문서 업로드 블록",
        "fieldName": null,
        "beforeValue": null,
        "afterValue": null,
        "resource": {
          "resourceId": null,
          "name": null
        },
        "actor": {
          "userId": "EMP005",
          "name": "최수아",
          "resignedAt": null
        },
        "block": {
          "blockId": 18,
          "title": "문서 업로드 블록",
          "type": "FILE"
        },
        "createdAt": "2026-08-02T13:15:00"
      }
    ],
    "nextCursor": 500,
    "hasNext": true
  }
}
```

조회 결과가 없으면 `200 OK`와 빈 배열을 반환한다.

```json
{
  "httpStatus": 200,
  "message": "활동 기록 조회 성공",
  "data": {
    "activities": [],
    "nextCursor": null,
    "hasNext": false
  }
}
```

## FE 처리 흐름

### Step 활동 기록 화면

```text
화면 진입
→ blockId 없이 최초 조회
→ 응답 순서대로 최신 기록 표시
→ 하단 도달 시 nextCursor로 추가 조회
```

Block 필터 목록은 다음 API에서 조회한다.

```text
GET /api/v1/steps/{stepId}/blocks
```

필터를 변경하면 기존 목록과 커서를 초기화한 뒤 다시 조회한다.

```text
전체 선택
→ blockId 없이 재조회

특정 Block 선택
→ blockId를 포함하여 재조회
```

### Block 활동 로그 팝업

```text
Block 활동 로그 버튼 선택
→ 해당 blockId로 최초 조회
→ 스크롤 시 blockId를 유지한 채 다음 커서 조회
```

화면에서는 다음 값을 FE가 조합한다.

```text
상단: actor.name + block.title + block.type
하단: displayName + actionLabel
```

`actor.resignedAt`이 `null`이 아니면 `actor.name` 옆에 퇴사 상태를 표시한다.

`오늘`, `어제`, 날짜별 그룹과 `14:32`, `2시간 전` 등의 시간 표현은 `createdAt`을 기준으로 FE에서 처리한다.

## BE 처리 흐름

```text
현재 회사 범위의 Step 및 접근 권한 확인
→ blockId 전달 시 현재 회사 범위의 Block과 Step 관계 검증
→ `activity_log.company_id`와 Block → Step → Project.company_id를 모두 만족하는 기록만 조회
→ cursor보다 작은 활동 기록 조회
→ activityLogId 내림차순 정렬
→ nextCursor와 hasNext 계산
→ 수행자·Block·Resource 스냅샷 정보와 함께 반환
```

DB의 `create`, `modify`, `delete`, `restore`, `purge` 값은 API 응답에서 각각 `CREATE`, `MODIFY`, `DELETE`, `RESTORE`, `PURGE`로 매핑한다.

Issue 생성·수정·상태 변경·삭제는 현재 Activity Log 기록 및 조회 대상에 포함하지 않는다.

## Status Code

| 코드 | code | 설명 |
| --- | --- | --- |
| 200 | - | 활동 기록 조회 성공 |
| 400 | `ACTIVITY_LOG_CURSOR_INVALID` | 잘못된 커서 |
| 400 | `ACTIVITY_LOG_SIZE_INVALID` | 잘못된 조회 개수 |
| 400 | `ACTIVITY_LOG_BLOCK_STEP_MISMATCH` | Block이 요청한 Step에 속하지 않음 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `PROJECT_ACCESS_DENIED` | 프로젝트 접근 권한 없음 |
| 404 | `STEP_NOT_FOUND` | Step이 존재하지 않음 |
| 404 | `BLOCK_NOT_FOUND` | Block이 존재하지 않음 |

---

# Activity Log 수집 컨벤션

이 섹션은 Block 관련 도메인 담당자가 Activity Log를 남기기 위해 알아야 하는 팀 내부 계약이다. 외부 API Request·Response가 아니다.

## 전체 책임 범위

각 도메인은 데이터 변경이 실제로 완료되는 Service 계층에서 Spring 동기 이벤트를 발행한다.

```text
각 도메인 Service
→ 변경 전 값과 resourceName 스냅샷 확보
→ 원본 데이터 생성·수정·삭제
→ 실제 변경 필드 비교
→ ActivityOccurredEvent 발행
→ Activity Log Listener 수신
→ field 단위 Activity Log 저장
→ 전체 트랜잭션 커밋
```

## 적용 기준

- `@Async`를 사용하지 않는다.
- 트랜잭션 내부에서 이벤트를 발행한다.
- `@TransactionalEventListener(BEFORE_COMMIT)`으로 처리한다.
- 원본 데이터와 로그는 같은 트랜잭션에서 저장한다.
- 로그 저장 실패 시 원본 데이터 변경도 함께 롤백한다.
- Controller나 FE에서 로그 생성 API를 별도로 호출하지 않는다.
- 각 도메인은 Activity Log Repository, Service, Port를 직접 호출하지 않는다.

### 멀티테넌시 — Activity Log 직접 격리 + Project 상속 검증

- `activity_log`은 회사별 감사 타임라인이므로 `company_id`를 직접 저장한다. `block → step → project` 경로는 부모 소유권 검증에만 쓰고, 로그 행 자체의 격리를 대신하지 않는다.
- `ActivityLogRecordService`가 동기 `BEFORE_COMMIT` 경로에서 `CurrentCompanyIdProvider`로 현재 회사를 읽어 저장한다. 타 Block 도메인은 기존 `ActivityOccurredEvent` 계약을 유지하며 `companyId`를 전달하지 않는다.
- Step·Block 검증과 목록 조회는 부모 `Block → Step → Project.company_id = 현재 회사`를 함께 확인한다. 목록은 여기에 `activity_log.company_id = 현재 회사`까지 더해 이중으로 제한한다.
- 현재 기록 경로는 `ActivityLogEventListener → ActivityLogRecordService → ActivityLogRecordPort` 한 곳이며, `ActivityLogRecordService`가 회사 ID를 명시 스탬핑한다. `V20260811160200__drop_activity_log_company_default.sql` 이후 새 직접 INSERT가 회사 ID를 생략하면 실패해야 한다.

## 공통 이벤트 정보

각 도메인은 다음 정보만 Activity Log 이벤트로 전달한다.

| 항목 | 설명 |
| --- | --- |
| `action` | `CREATE`, `MODIFY`, `DELETE`, `RESTORE`, `PURGE`. 뒤 2개는 파일 휴지통 복원·영구삭제 전용이며 다른 도메인은 발행하지 않는다 |
| `blockId` | 활동이 발생한 Block ID |
| `resourceId` | Block 내부 데이터 ID. Block 자체 활동이면 `null` |
| `resourceName` | Block 내부 데이터 표시명 스냅샷. Block 자체 활동 또는 표시명이 없으면 `null`. DB에는 `TEXT`로 저장 |
| `actorId` | 현재 인증 사용자의 사번 |
| `changes` | 변경 필드와 변경 전·후 값 목록 |

```java
public record ActivityOccurredEvent(
    ActivityLogAction action,
    Long blockId,
    Long resourceId,
    String resourceName,
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

## 공통 기록 규칙

### 생성·삭제

생성·삭제처럼 변경 필드가 특정되지 않으면 null change 1개를 전달한다.

```java
List.of(new ActivityFieldChange(null, null, null))
```

표시명은 `beforeValue`나 `afterValue`가 아니라 `resourceName`에 스냅샷으로 담는다.

### 수정

수정은 실제로 변경된 필드 하나당 로그 한 행을 저장한다.

```text
제목과 상태가 동시에 변경됨
→ title 로그 1행
→ status 로그 1행
```

요청값과 기존 값이 같으면 로그를 남기지 않는다.

### resourceName

`resourceName`은 변경값이 아니라 사용자가 화면에서 대상을 알아볼 수 있는 표시명이다.

| 상황 | 값 |
| --- | --- |
| Block 자체 생성·수정·삭제 | `null` |
| 체크리스트 항목 | 항목 내용 |
| 파일 | 파일명 |
| 이미지 | caption 또는 이미지 표시명 |
| 텍스트 본문 | `null` |

예를 들어 체크리스트 완료 여부를 `false -> true`로 바꿔도 `resourceName`에는 `true`가 아니라 해당 체크리스트 항목 내용이 들어간다.

### 작업자

`actorId`는 FE Request에서 받지 않고 현재 인증 사용자의 사번을 사용한다.

### 화면 문구

각 도메인이 완성된 문장을 직접 전달하지 않는다.

BE는 `actor`, `block`, `resource`, `action`, `fieldName`, `beforeValue`, `afterValue` 같은 원자 데이터를 내려주고, FE가 화면 문구를 조립한다.

### 필드별 표시 규칙 (FE 참고, BE 계약 아님)

`fieldName` 값 기준으로 아래 중 하나로 표시한다. 새 필드가 추가되면 이 표에도 같이 추가한다.

| 방식 | 대상 필드 | 설명 |
| --- | --- | --- |
| 펼치기(전문 표시) | `title`, `content`, `caption` | 자유 텍스트라 길 수 있음 — 화살표로 펼쳐서 `beforeValue`/`afterValue` 전문을 보여준다 |
| 그대로 표시 | `orderIndex` | 1부터 시작하는 위치 번호라 값 변환 없이 "N번째 → M번째"로 그대로 쓴다 |
| 값 사전 매칭 | `isCompleted`, `status` | 아래 사전으로 변환해서 화면 옆에 짧게 표시(펼치기 불필요) |
| BE가 이미 이름으로 변환함 | `lines` | 사번이 아니라 결재자 이름 CSV로 내려오므로 FE가 추가로 변환할 필요 없음 |

**값 사전**

| `fieldName` | 값 | 표시 |
| --- | --- | --- |
| `isCompleted` | `true` | 완료 |
| `isCompleted` | `false` | 미완료 |
| `status` | `DRAFT` | 초안 |
| `status` | `IN_PROGRESS` | 진행중 |
| `status` | `ACTIVE` | 진행중 |
| `status` | `WAITING` | 대기 |
| `status` | `APPROVED` | 승인 |
| `status` | `REJECTED` | 반려 |
| `status` | `COMPLETED` | 완료 |
| `status` | `CANCELED` | 취소 |

> `status`는 결재 회차(`ApprovalStatus`: `DRAFT`/`IN_PROGRESS`/`REJECTED`/`COMPLETED`/`CANCELED`)와 결재선(`ApprovalLineStatus`: `DRAFT`/`WAITING`/`ACTIVE`/`APPROVED`/`REJECTED`/`CANCELED`) 두 enum을 같은 `fieldName`으로 내려보낸다. 값 종류가 거의 겹치지 않고(`IN_PROGRESS`·`COMPLETED`는 회차만, `ACTIVE`·`WAITING`·`APPROVED`는 결재선만 가짐), 겹치는 `REJECTED`·`CANCELED`도 의미가 같아서 어느 쪽인지 구분하지 않고 값 하나로 통일된 사전을 써도 된다.

## 각 도메인이 갖춰야 할 구조

| 위치 | 담당 | 규칙 |
| --- | --- | --- |
| 원본 도메인 Service | 각 블록 도메인 | 실제 DB 변경이 완료되는 `@Transactional` Service에서 이벤트 발행 |
| 이벤트 발행기 | 공통 | `DomainEventPublisher` 주입 |
| 이벤트 객체 | Activity Log 계약 | `ActivityOccurredEvent`, `ActivityFieldChange` 사용 |
| 로그 저장 | Activity Log 도메인 | `ActivityLogEventListener`, `ActivityLogRecordService`, `ActivityLogRecordPort`가 담당 |
| Repository 접근 | Activity Log 도메인 | 타 도메인은 `ActivityLogJpaRepository` 직접 호출 금지 |

```java
domainEventPublisher.publish(ActivityOccurredEvent.of(
    ActivityLogAction.MODIFY,
    blockId,
    checklistItemId,
    checklistContent,
    actorId,
    List.of(new ActivityFieldChange("content", beforeContent, afterContent))
));
```

## 도메인별 수집 범위

### 부모 Block 도메인

모든 Block 유형에 공통으로 적용한다.

| 활동 | Action | `resourceId` | `resourceName` | 기록 정보 |
| --- | --- | --- | --- | --- |
| Block 생성 | `CREATE` | `null` | `null` | null change 1개 |
| Block명 수정 | `MODIFY` | `null` | `null` | `title` 변경 전·후 값 |
| Block 삭제 | `DELETE` | `null` | `null` | null change 1개 |

Block 위치, 크기, 정렬 순서 변경은 현재 로그 수집 대상에서 제외한다.

### 체크리스트

| 활동 | Action | `resourceId` | `resourceName` | 기록 정보 |
| --- | --- | --- | --- | --- |
| 항목 생성 | `CREATE` | 체크리스트 항목 ID | 항목 내용 | null change 1개 |
| 항목 내용 수정 | `MODIFY` | 체크리스트 항목 ID | 항목 내용 | `content` 변경 전·후 값 |
| 체크 상태 변경 | `MODIFY` | 체크리스트 항목 ID | 항목 내용 | `isCompleted` 변경 전·후 값 |
| 항목 삭제 | `DELETE` | 체크리스트 항목 ID | 삭제 전 항목 내용 | null change 1개 |

### 이미지

| 활동 | Action | `resourceId` | `resourceName` | 기록 정보 |
| --- | --- | --- | --- | --- |
| 이미지 항목 생성 | `CREATE` | 이미지 ID | 원본 이미지명 또는 caption | null change 1개 |
| 캡션 수정 | `MODIFY` | 이미지 ID | caption 또는 이미지 표시명 | `caption` 변경 전·후 값 |
| 이미지 순서 수정 | `MODIFY` | 이미지 ID | 이미지 표시명 | `orderIndex` 변경 전·후 값 |
| 이미지 항목 삭제 | `DELETE` | 이미지 ID | 삭제 전 이미지 표시명 | null change 1개 |

이미지 순서 변경은 사용자가 직접 이동한 이미지 한 건만 기록한다.

### 텍스트 Block

| 활동 | Action | `resourceId` | `resourceName` | 기록 정보 |
| --- | --- | --- | --- | --- |
| 본문 수정 | `MODIFY` | 텍스트 데이터 ID | `null` | `content` 변경 전·후 값 |

텍스트 본문은 표시명으로 쓰지 않으므로 `resourceName = null`로 둔다.

민감하거나 과도하게 큰 값은 기록하지 않는다.

### 문서·파일 업로드

파일은 여러 Block에서 공통 사용될 수 있으므로, 파일 정보만 보고 Block을 추론하지 않는다.

파일 기능을 호출한 도메인이 반드시 현재 작업이 발생한 `blockId`를 이벤트에 전달한다.

| 활동 | Action | `resourceId` | `resourceName` | 기록 정보 |
| --- | --- | --- | --- | --- |
| 파일 업로드 | `CREATE` | 파일 또는 파일 버전 ID | 업로드된 파일명 | null change 1개 |
| 파일명 수정 | `MODIFY` | 수정 대상 파일 또는 버전 ID | 파일명 | `fileName` 변경 전·후 값 |
| 파일 삭제(휴지통 이동) | `DELETE` | 파일 또는 파일 버전 ID | 삭제 전 파일명 | null change 1개 |
| 휴지통에서 복원 | `RESTORE` | 파일 또는 파일 버전 ID | 복원된 파일명 | null change 1개 |
| 휴지통에서 영구 삭제 | `PURGE` | 파일 또는 파일 버전 ID | 삭제 전 파일명 | null change 1개 |

파일명 수정 대상이 파일 엔티티인지 파일 버전 엔티티인지는 실제 API 설계 기준을 따른다. 한 기능 안에서는 동일한 `resourceId` 기준을 일관되게 사용한다. `DELETE`(휴지통 이동)·`RESTORE`·`PURGE`도 같은 `resourceId` 기준을 따른다.

⛔ **별도 "휴지통 활동 로그" 화면·API는 만들지 않는다.** `RESTORE`·`PURGE`도 다른 활동과 동일하게 Step 활동 기록(`GET /api/v1/steps/{stepId}/activity-logs`)·Block 활동 로그 팝업에 섞여서 시간순으로 노출된다. FE는 `action` 값으로 문구만 다르게 조립한다 (2026-08-06 결정).

### 결재

결재 Block 자체의 생성·삭제는 부모 Block 활동으로 대표하며 결재 도메인에서 중복 기록하지 않는다.

| 활동 | Action | `resourceId` | `resourceName` | 기록 정보 |
| --- | --- | --- | --- | --- |
| 회차 제목·내용 수정 | `MODIFY` | 결재 회차 ID | 수정 후 결재 제목 | 실제 변경된 `title`, `content`의 변경 전·후 값 |
| 결재 문서 추가 | `CREATE` | 파일 버전 ID | 파일명 | null change 1개 |
| 결재 문서 제거 | `DELETE` | 파일 버전 ID | 제거 전 파일명 | null change 1개 |
| 결재선 등록·수정 | `MODIFY` | 결재 회차 ID | 결재 제목 | `lines` 변경 전·후 값 |
| 결재 상신 | `MODIFY` | 결재 회차 ID | 결재 제목 | `status` 변경 전·후 값 |
| 재상신 회차 생성 | `CREATE` | 새 결재 회차 ID | 결재 제목 | null change 1개 |

결재선 `lines` 값은 결재 순서대로 정렬한 **결재자 이름**을 쉼표로 연결한다(2026-08-07 변경 — 사번 그대로 노출하면 FE가 화면에서 못 알아봐서, `employee` 라이브 조회로 이름으로 바꿔서 기록한다). 결재자별 행을 따로 만들지 않으며, 이름과 순서가 모두 같으면 로그를 남기지 않는다.

⚠️ `employee` 조회에 실패하면(퇴사·탈퇴 등) 이름 대신 사번을 그대로 남긴다 — 로그 저장 자체를 막지 않는다.

상신에 따라 자동으로 바뀌는 결재 전체 상태와 각 결재선 상태는 별도 로그로 중복 기록하지 않는다.
