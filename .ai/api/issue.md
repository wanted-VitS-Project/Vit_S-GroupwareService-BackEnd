# 🧩 Issue API

**상태**: `✅ 확정`
**최종 업데이트**: 2026-08-07 · **담당**: 김용준
**최종 업데이트**: 2026-08-09 (담당자 지정 알림 정책 추가 — 신규 추가 담당자에게만 `ISSUE_ASSIGNED` 발행, 신규 에러코드 없음)
**최종 업데이트**: 2026-08-10 (`ISSUE_ASSIGNED`의 `targetContext`를 `null`에서 `{projectId, stepId}`로 변경 — FE 상세 라우팅에 두 값이 필요하다고 확인됨. **프론트 공유 필요**)
**노션**: 반영 · 예정 Domain `프로젝트` · SUB-Domain `Issue`
**도메인 문서**: `../docs/domain/이슈/ISS-V1.md` · `../docs/domain/이슈/ISS-V1-USECASE.md`

> ✅ **프론트 연동 계약으로 확정된 명세다.** 경로·필드명·타입·상태코드·에러코드를 임의로 바꾸지 않는다.

## 엔드포인트

| API명칭 | METHOD | URL | 권한 |
|---|---|---|---|
| 스텝별 이슈 목록 조회 | GET | `/api/v1/steps/{stepId}/issues` | 프로젝트 참여자 |
| 이슈 생성 | POST | `/api/v1/steps/{stepId}/issues` | 스텝 EDITOR |
| 이슈 상세 조회 | GET | `/api/v1/issues/{issueId}` | 스텝 접근 권한 |
| 이슈 부분 수정 | PATCH | `/api/v1/issues/{issueId}` | 스텝 EDITOR |
| 이슈 상태 변경 | PATCH | `/api/v1/issues/{issueId}/status` | 스텝 EDITOR |
| 이슈 삭제 | DELETE | `/api/v1/issues/{issueId}` | 스텝 EDITOR |
| 담당 이슈 캘린더 조회 | GET | `/api/v1/issues/calendar` | 본인 담당 이슈만 |

## 공통 응답 형식

```json
{
  "httpStatus": 200,
  "message": "요청 성공",
  "data": {}
}
```

실패 응답은 `{ httpStatus, message, code }` 형식을 사용한다.

## 🔑 공통 원칙

| 원칙 | 내용 |
|---|---|
| 소속 | Issue는 Step에 직접 속한다 |
| 상태 | API는 `TODO` · `IN_PROGRESS` · `DONE`을 사용하고 DB의 `TO_DO`와 명시적으로 매핑한다 |
| 담당자 | `issue_assign`을 통해 여러 사원을 연결한다 |
| 관련 Block | `issue_block`을 통해 같은 Step의 여러 Block을 연결한다 |
| 부분 수정 | 일반 필드와 관계 목록 모두 전달된 값만 수정한다 |
| 완료 시각 | `completedAt`은 상태에 따라 BE가 관리하며 사용자가 직접 수정하지 않는다 |
| 삭제 | Issue는 논리 삭제하고 담당자·Block 연결 행은 같은 트랜잭션에서 제거한다 |
| Activity Log | Issue 생성·수정·상태 변경·삭제는 Activity Log에 기록하지 않는다 |
| Notification | 이슈 생성·수정으로 신규 담당자가 추가되면 해당 담당자별로 `ISSUE_ASSIGNED` 알림을 발행한다 |

### 권한 판정

```text
1) 전역 role
     ADMIN / MASTER → 접근 허용
     MEMBER         → 계속

2) Issue → Step → Project 찾기

3) 스텝 권한
     step_permission 행 있음 → 해당 권한 사용
     행 없음                 → project_member 권한 상속
     NONE                    → 접근 차단
     VIEWER                  → 조회만
     EDITOR                  → 조회·생성·수정·상태 변경·삭제
```

### 관계 목록 수정 규칙

| 요청 값 | 처리 |
|---|---|
| 필드 미전달 | 기존 관계 유지 |
| `[]` | 모든 관계 해제 |
| `[IDs]` | 전달 목록과 정확히 일치하도록 동기화 |
| `null` | 허용하지 않음 |

`assigneeIds`와 `blockIds`는 PATCH에서 **추가분이 아니라 최종 전체 목록**이다.

`assigneeIds`는 `GET /api/v1/projects/{projectId}/members` 응답의 `members[].userId`를 사용한다.
`blockIds`는 `GET /api/v1/steps/{stepId}/blocks/options` 응답의 `blocks[].blockId`를 사용한다.

### 담당자 지정 알림

| 항목 | 정책 |
|---|---|
| 발행 시점 | 이슈 생성 시 담당자 전체, 이슈 수정 시 기존에 없던 신규 추가 담당자 |
| 미발행 | 담당자 유지, 담당자 해제, 이슈 삭제, 상태 변경 |
| 알림 유형 | `ISSUE_ASSIGNED` |
| 이동 대상 | `targetType=ISSUE`, `targetId=issueId`, `targetContext={"projectId": projectId, "stepId": stepId}` |
| 접근 판정 | 알림 도메인은 이동 대상만 반환하고, 실제 조회 가능 여부는 기존 이슈 상세 API의 Step 접근 권한이 판단 |
| 에러코드 | 신규 추가 없음. 이슈 삭제는 기존 `ISS_NOT_FOUND`, Step 접근 불가는 기존 `ISS_ACCESS_PERMISSION_REQUIRED` |

---

## 1. 스텝별 이슈 목록 조회

| 항목 | 내용 |
|---|---|
| Method · URL | `GET /api/v1/steps/{stepId}/issues` |
| 인증 필요 | Y · 프로젝트 참여자 |
| 요구사항 | ISS-005 · ISS-006 · USC-ISS-001 · USC-ISS-002 |

현재 Step에 등록된 이슈 목록을 조회한다.

`blockId`가 전달되면 해당 Step에서 특정 Block과 연결된 이슈만 반환한다. 이슈 보드의 상태·담당자·Block·우선순위·제목 검색 및 마감일 정렬은 FE에서 처리한다.

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `stepId` | Long | Y | 이슈를 조회할 Step ID |

**Query Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `blockId` | Long | N | 해당 Block과 연결된 이슈만 조회 |

```bash
GET /api/v1/steps/10/issues
GET /api/v1/steps/10/issues?blockId=15
```

⛔ **페이징이 없다.** Step에 등록된 삭제되지 않은 이슈 전체를 반환한다.

⛔ `status`, `assigneeId`, `priority`, `title`, 정렬 조건은 Query Parameter로 전달하지 않는다.

⛔ 상태·담당자·Block·우선순위 필터, 제목 검색, 마감일 정렬은 FE에서 처리한다.

⛔ `blockId`는 Block 상세 화면에서 관련 이슈만 조회할 때 사용한다. 요청 Block은 `stepId`와 같은 Step에 속해야 한다.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.issues` | List | 조회된 이슈 목록 |
| `data.issues[].issueId` | Long | 이슈 ID |
| `data.issues[].title` | String | 이슈 제목 |
| `data.issues[].status` | String | `TODO` · `IN_PROGRESS` · `DONE` |
| `data.issues[].priority` | String | `LOW` · `MEDIUM` · `HIGH` |
| `data.issues[].dueDate` | LocalDate | 마감일. 미지정 시 `null` |
| `data.issues[].assignees` | List | 담당자 목록 |
| `data.issues[].assignees[].userId` | String | 담당자 사번 |
| `data.issues[].assignees[].name` | String | 담당자 이름 |
| `data.issues[].relatedBlocks` | List | 연결된 Block 목록 |
| `data.issues[].relatedBlocks[].blockId` | Long | Block ID |
| `data.issues[].relatedBlocks[].title` | String | Block 제목 |
| `data.issues[].relatedBlocks[].type` | String | Block 유형 |

**Success Response**

```json
{
  "httpStatus": 200,
  "message": "이슈 목록 조회 성공",
  "data": {
    "issues": [
      {
        "issueId": 101,
        "title": "경쟁사 제안서 벤치마킹",
        "status": "TODO",
        "priority": "HIGH",
        "dueDate": "2026-07-25",
        "assignees": [
          {
            "userId": "EMP001",
            "name": "김용준"
          }
        ],
        "relatedBlocks": [
          {
            "blockId": 15,
            "title": "제안서 작성 체크리스트",
            "type": "CHECKLIST"
          }
        ]
      }
    ]
  }
}
```

조회 결과가 없으면 `200 OK`와 빈 배열을 반환한다.

```json
{
  "httpStatus": 200,
  "message": "이슈 목록 조회 성공",
  "data": {
    "issues": []
  }
}
```

### FE 처리 흐름

**이슈 보드**

```text
Step 진입
→ 전체 이슈 조회
→ status 기준으로 칸반 칼럼 분류
→ 담당자·Block·우선순위·제목 필터링
→ dueDate 기준 정렬
```

| 화면 기능 | FE 처리 기준 |
|---|---|
| 상태 필터 | `status` |
| 담당자 필터 | `assignees.userId` |
| Block 필터 | `relatedBlocks.blockId` |
| 우선순위 필터 | `priority` |
| 검색 | `title`만 검색 |
| 정렬 | `dueDate` 기준, `null`은 마지막 |

필터 선택지는 별도 API를 사용한다.

```bash
GET /api/v1/projects/{projectId}/members
GET /api/v1/steps/{stepId}/blocks/options
```

**Block 연결 이슈 팝업**

```text
Block의 연결 이슈 버튼 선택
→ blockId를 포함하여 API 호출
→ 해당 Block과 연결된 이슈 표시
→ 상태별 개수는 반환 목록으로 FE가 계산
```

### BE 처리 흐름

```text
Step 존재 및 접근 권한 확인
→ blockId 전달 시 Block 존재 여부 확인
→ Block이 요청한 Step에 속하는지 검증
→ 삭제되지 않은 이슈 조회
→ 담당자 및 연결 Block과 함께 반환
```

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 이슈 목록 조회 성공 |
| 400 | `ISS_BLOCK_STEP_MISMATCH` | Block이 요청한 Step에 속하지 않음 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `PROJECT_ACCESS_DENIED` | 프로젝트 접근 권한 없음 |
| 404 | `STEP_NOT_FOUND` | Step이 존재하지 않음 |
| 404 | `BLOCK_NOT_FOUND` | Block이 존재하지 않음 |

---

## 2. 이슈 생성

| 항목 | 내용 |
|---|---|
| Method · URL | `POST /api/v1/steps/{stepId}/issues` |
| 인증 필요 | Y · 스텝 EDITOR |
| 요구사항 | ISS-001~004 · ASN-001~005 · IBL-001~004 · STS-001·002·005·006 · USC-ISS-004~006 · USC-ASN-001 · USC-IBL-001 |

⛔ 생성자 사번은 Request로 받지 않는다. 현재 인증 사용자를 `createdBy`로 저장한다.

⛔ `startDate/startDay`와 `completedAt/finishDay`는 Request에 포함하지 않는다.

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `stepId` | Long | Y | 이슈를 생성할 Step 번호 |

**Request Body**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `title` | String | Y | 제목. 공백 제외 필수, 최대 200자 |
| `content` | String | N | 내용. `null` 허용 |
| `dueDate` | String | N | 마감 일시 `yyyy-MM-dd'T'HH:mm:ss` |
| `status` | String | N | `TODO` · `IN_PROGRESS` · `DONE`. 기본 `TODO` |
| `priority` | String | Y | `LOW` · `MEDIUM` · `HIGH` |
| `assigneeIds` | List<String> | N | 담당자 사번 목록. 생략 시 빈 목록 |
| `blockIds` | List<Long> | N | 관련 Block 번호 목록. 생략 시 빈 목록 |

⛔ 담당자는 해당 프로젝트 참여자여야 한다.

⛔ 관련 Block은 Issue와 같은 Step에 속해야 한다.

⛔ `DONE`으로 생성하면 BE가 `completedAt`에 현재 시각을 기록한다.

⛔ 담당자가 있으면 담당자별로 `ISSUE_ASSIGNED` 알림을 발행한다. 이 알림은 트랜잭션 커밋 후 생성되며, 요청이 실패하거나 롤백되면 생성되지 않는다.

**Response** — 상세 조회와 같은 구조

| 코드 | code | 설명 |
|---|---|---|
| 201 | – | 생성 성공 |
| 400 | `ISS_INVALID_REQUEST` | 제목·상태·우선순위·관계 목록 형식 오류 |
| 400 | `ISS_ASSIGNEE_NOT_PROJECT_MEMBER` | 프로젝트 참여자가 아닌 담당자 포함 |
| 400 | `ISS_BLOCK_STEP_MISMATCH` | 다른 Step의 Block 포함 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `ISS_EDIT_PERMISSION_REQUIRED` | Step 편집 권한 없음 |
| 404 | `ISS_STEP_NOT_FOUND` | Step 없음 또는 논리 삭제됨 |
| 404 | `ISS_ASSIGNEE_NOT_FOUND` | 존재하지 않는 사번 포함. 전체 요청 거부 |
| 404 | `ISS_BLOCK_NOT_FOUND` | 존재하지 않거나 삭제된 Block 포함. 전체 요청 거부 |

---

## 3. 이슈 상세 조회

| 항목 | 내용 |
|---|---|
| Method · URL | `GET /api/v1/issues/{issueId}` |
| 인증 필요 | Y · 스텝 접근 권한 |
| 요구사항 | ISS-007 · STS-006 · USC-ISS-003 |

**Path Parameter** — `issueId` Long Y

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.issueId` | Long | 이슈 번호 |
| `data.stepId` | Long | 소속 Step 번호 |
| `data.title` | String | 제목 |
| `data.content` | String | 내용. `null` 허용 |
| `data.status` | String | `TODO` · `IN_PROGRESS` · `DONE` |
| `data.priority` | String | `LOW` · `MEDIUM` · `HIGH` |
| `data.dueDate` | LocalDate | 마감일. 미지정 시 `null` |
| `data.completedAt` | LocalDateTime | `DONE` 완료 시각. 완료 상태가 아니면 `null` |
| `data.assignees[].userId` | String | 담당자 사번 |
| `data.assignees[].name` | String | 담당자 이름 |
| `data.relatedBlocks[].blockId` | Long | 관련 Block 번호 |
| `data.relatedBlocks[].title` | String | 관련 Block명 |
| `data.relatedBlocks[].type` | String | Block 타입 |

⛔ 화면 표시용 `issueKey`, 시작일, 이슈별 별도 진척도, Issue 활동 이력은 응답하지 않는다.

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `ISS_ACCESS_PERMISSION_REQUIRED` | 소속 Step 열람 권한 없음 |
| 404 | `ISS_NOT_FOUND` | Issue 없음 또는 논리 삭제됨 |

---

## 4. 이슈 부분 수정

| 항목 | 내용 |
|---|---|
| Method · URL | `PATCH /api/v1/issues/{issueId}` |
| 인증 필요 | Y · 스텝 EDITOR |
| 요구사항 | ISS-008·009 · ASN-004 · IBL-003·006 · STS-005 · USC-ISS-007~011 · USC-ASN-002~006 · USC-IBL-002~007 |

⛔ GitHub Issue처럼 항목별 즉시 저장한다. 별도의 최종 수정 버튼을 전제로 하지 않는다.

⛔ 상태는 이 API에서 수정하지 않는다. 상태 변경 전용 API를 호출한다.

**Request Body** — 전달한 필드만 수정

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `title` | String | N | 제목. 전달 시 빈 값 불가, 최대 200자 |
| `content` | String | N | 내용. 명시적 `null`이면 내용 삭제 |
| `dueDate` | LocalDate | N | 마감일. 명시적 `null`이면 마감일 해제 |
| `priority` | String | N | `LOW` · `MEDIUM` · `HIGH` |
| `assigneeIds` | List<String> | N | 최종 담당자 전체 목록 |
| `blockIds` | List<Long> | N | 최종 관련 Block 전체 목록 |

관계 목록은 다음처럼 처리한다.

```text
미전달 → 기존 관계 유지
[]     → 모든 관계 해제
[IDs]  → 해당 목록으로 동기화
null   → 400
```

⛔ `assigneeIds`가 전달되면 기존 담당자와 최종 담당자 목록을 비교해 새로 추가된 담당자에게만 `ISSUE_ASSIGNED` 알림을 발행한다. 기존 담당자 유지나 담당자 해제만으로는 알림을 만들지 않는다.

**Response** — 상세 조회와 같은 최신 구조

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 수정 성공 |
| 400 | `ISS_INVALID_REQUEST` | 수정할 필드 없음, 제목·Enum·목록 형식 오류 |
| 400 | `ISS_ASSIGNEE_NOT_PROJECT_MEMBER` | 프로젝트 참여자가 아닌 담당자 포함 |
| 400 | `ISS_BLOCK_STEP_MISMATCH` | 다른 Step의 Block 포함 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `ISS_EDIT_PERMISSION_REQUIRED` | Step 편집 권한 없음 |
| 404 | `ISS_NOT_FOUND` | Issue 없음 또는 논리 삭제됨 |
| 404 | `ISS_ASSIGNEE_NOT_FOUND` | 존재하지 않는 사번 포함. 전체 요청 거부 |
| 404 | `ISS_BLOCK_NOT_FOUND` | 존재하지 않거나 삭제된 Block 포함. 전체 요청 거부 |

---

## 5. 이슈 상태 변경

| 항목 | 내용 |
|---|---|
| Method · URL | `PATCH /api/v1/issues/{issueId}/status` |
| 인증 필요 | Y · 스텝 EDITOR |
| 요구사항 | STS-001~005 · USC-STS-001~007 |

**Request Body**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `status` | String | Y | `TODO` · `IN_PROGRESS` · `DONE` |

### 완료 시각 규칙

| 상태 변경 | `completedAt` 처리 |
|---|---|
| `TODO/IN_PROGRESS → DONE` | 서버 현재 시각 저장 |
| `DONE → TODO/IN_PROGRESS` | `null` 처리 |
| 다시 `DONE` 진입 | 새로운 현재 시각 저장 |
| 동일 상태 요청 | 상태와 완료 시각을 변경하지 않고 현재 값 반환 |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.issueId` | Long | 이슈 번호 |
| `data.status` | String | 변경 후 상태 |
| `data.completedAt` | LocalDateTime | 변경 후 완료 시각. `null` 허용 |
| `data.updatedAt` | LocalDateTime | 최종 수정 일시 |

**Success Response**

```json
{
  "httpStatus": 200,
  "message": "이슈 상태 변경 성공",
  "data": {
    "issueId": 101,
    "status": "DONE",
    "completedAt": "2026-08-02T22:46:00",
    "updatedAt": "2026-08-02T22:46:00"
  }
}
```

> 칸반 Drag & Drop 실패 시 FE가 카드를 기존 칼럼으로 복구한다. 같은 칼럼 안의 카드 순서는 저장하지 않는다.

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 변경 성공 또는 동일 상태 멱등 처리 |
| 400 | `ISS_STATUS_REQUIRED` | 상태가 전달되지 않음 |
| 400 | `ISS_INVALID_STATUS` | 허용하지 않는 상태값 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `ISS_EDIT_PERMISSION_REQUIRED` | Step 편집 권한 없음 |
| 404 | `ISS_NOT_FOUND` | Issue 없음 또는 논리 삭제됨 |

---

## 6. 이슈 삭제

| 항목 | 내용 |
|---|---|
| Method · URL | `DELETE /api/v1/issues/{issueId}` |
| 인증 필요 | Y · 스텝 EDITOR |
| 요구사항 | ISS-010 · IBL-005 · USC-ISS-012·013 · USC-ASN-007 · USC-IBL-009 |

이슈를 논리 삭제하고, 담당자 및 관련 Block 연결 정보를 함께 제거한다.

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `issueId` | Long | Y | 삭제할 이슈 ID |

**Query Parameter** — 없음

**Request Body** — 없음

### 처리 기준

```text
Issue 존재 및 삭제 여부 확인
→ Issue의 stepId 기준 스텝 EDITOR 권한 확인
→ issue.deleted_at = 현재 시각
→ issue_assign 관계 삭제
→ issue_block 관계 삭제
→ 200 OK 반환
```

⛔ `issue.deleted_at`에 현재 시각을 기록하는 논리 삭제다.

⛔ 삭제된 이슈는 목록·상세·집계 조회에서 제외한다.

⛔ 같은 트랜잭션에서 `issue_assign`, `issue_block` 관계 행을 제거한다.

⛔ 담당자 Employee와 관련 Block 원본은 삭제하지 않는다.

⛔ Issue 삭제는 Activity Log에 기록하지 않는다.

⛔ 이미 삭제된 이슈는 존재하지 않는 이슈와 동일하게 처리한다.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | `null` |

**Success Response**

```json
{
  "httpStatus": 200,
  "message": "이슈 삭제 성공",
  "data": null
}
```

### FE 처리 흐름

```text
삭제 버튼 선택
→ 사용자 확인
→ 삭제 API 호출
→ 성공 시 보드에서 해당 이슈 제거
→ 상세 화면 닫기
```

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 이슈 삭제 성공 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `ISS_EDIT_PERMISSION_REQUIRED` | Step 편집 권한 없음 |
| 404 | `ISS_NOT_FOUND` | Issue 없음 또는 이미 논리 삭제됨 |

---

## 7. 담당 이슈 캘린더 조회

| 항목 | 내용 |
|---|---|
| Method · URL | `GET /api/v1/issues/calendar` |
| 인증 필요 | Y · 로그인 사용자 본인 담당 이슈만 조회 (별도 스텝 접근 권한 검사 없음) |
| 요구사항 | 마이페이지 개인 캘린더 — 요구사항 번호 미부여 |

로그인 사용자가 담당자로 지정된, 완료되지 않은 이슈 전체를 한 번에 조회한다. 마이페이지 개인 캘린더에서 날짜별 마킹 용도로 사용하며, 월 이동 등 기간 분기는 FE가 이미 받은 데이터로 처리한다(월 이동마다 재호출하지 않는다).

`issue_assign.user_id`가 본인이고 `status`가 `DONE`이 아닌 이슈만 반환한다. 프로젝트별 색상 매핑은 FE에서 `projectId` 기준으로 처리한다(BE는 색상 값을 내려주지 않는다).

**Query Parameter** — 없음

```bash
GET /api/v1/issues/calendar
```

⛔ 페이징이 없다. 본인 담당 이슈 중 완료되지 않은 것 전체를 반환한다.

⛔ `status`가 `DONE`인 이슈는 조회 대상에서 제외한다.

⛔ `dueDate`가 없는 이슈는 캘린더에 표시할 날짜가 없으므로 조회 대상에서 제외한다.

⛔ 이슈 선택 후 상세 화면 진입은 기존 `GET /api/v1/issues/{issueId}`를 그대로 호출한다. 이 API는 별도 필드를 추가하지 않는다.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.issues` | List | 조회된 이슈 목록 |
| `data.issues[].issueId` | Long | 이슈 번호 |
| `data.issues[].title` | String | 이슈 제목 |
| `data.issues[].status` | String | `TODO` · `IN_PROGRESS` (`DONE`은 반환하지 않음) |
| `data.issues[].priority` | String | `LOW` · `MEDIUM` · `HIGH` |
| `data.issues[].dueDate` | LocalDate | 마감일 |
| `data.issues[].stepId` | Long | 소속 Step 번호 |
| `data.issues[].stepName` | String | 소속 Step명 |
| `data.issues[].projectId` | Long | 소속 Project 번호 |
| `data.issues[].projectName` | String | 소속 Project명 |

**Success Response**

```json
{
  "httpStatus": 200,
  "message": "담당 이슈 캘린더 조회 성공",
  "data": {
    "issues": [
      {
        "issueId": 101,
        "title": "제안서 1차 초안 작성",
        "status": "IN_PROGRESS",
        "priority": "HIGH",
        "dueDate": "2026-08-11",
        "stepId": 10,
        "stepName": "입찰 진행",
        "projectId": 3,
        "projectName": "OO시 스마트도로 구축"
      }
    ]
  }
}
```

조회 결과가 없으면 `200 OK`와 빈 배열을 반환한다.

### FE 처리 흐름

```text
마이페이지 캘린더 진입
→ 최초 1회 캘린더 조회 API 호출 (본인 담당 · 미완료 이슈 전체)
→ dueDate 기준으로 날짜별 이슈 마킹
→ 월 이동 시 API 재호출 없이, 이미 받은 데이터에서 해당 월만 필터링해 표시
→ projectId 기준으로 프로젝트별 색상 매핑, 하단에 색상 범례 표시
→ 날짜 선택 시 해당 날짜의 이슈 목록 표시
→ 이슈 선택 시 기존 상세 조회 API 호출
```

```bash
GET /api/v1/issues/{issueId}
```

### BE 처리 흐름

```text
로그인 사용자(userId) 확인
→ issue_assign.user_id = 본인 AND status != DONE 인 이슈 조회 (issue ⋈ issue_assign ⋈ step ⋈ project)
→ 삭제되지 않은 데이터만 응답
```

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 담당 이슈 캘린더 조회 성공 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |

> 확정된 결정: 조회 범위는 "본인 담당(`issue_assign`) + 미완료(`status != DONE`)"이다. 이미 종료된 프로젝트라도 미완료 이슈가 남아있으면 캘린더에 계속 노출된다(프로젝트 상태 기준 필터는 도입하지 않음).

---

## 내부 연동 — Step별 Issue 진척도

별도 FE 호출 API를 만들지 않는다.

이슈 도메인은 프로젝트 사이드바 조립 서비스가 호출할 수 있는 내부 조회 서비스를 제공한다.

```text
List<stepId>
→ Step별 TODO / IN_PROGRESS / DONE / 전체 수 일괄 집계
→ 프로젝트 도메인이 Stage·Step 정보와 조립
```

- Step마다 반복 Count 쿼리를 실행하지 않는다.
- `step_id IN (...) GROUP BY step_id, status` 형태로 일괄 조회한다.
- 삭제된 Issue는 집계에서 제외한다.
- 이슈가 0개인 Step의 퍼센트 표현은 프로젝트 도메인 문서의 최종 정책을 따른다.
