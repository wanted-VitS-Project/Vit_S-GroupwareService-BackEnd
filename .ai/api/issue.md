# 🧩 Issue API

**상태**: `📝 초안` — 노션 미반영. 구현 금지 (`../API.md` §0·§1)
**최종 업데이트**: 2026-08-04 · **담당**: 김용준
**노션**: 미반영 · 예정 Domain `프로젝트` · SUB-Domain `Issue`
**도메인 문서**: `../docs/domain/이슈/ISS-V1.md` · `../docs/domain/이슈/ISS-V1-USECASE.md`

> 📝 **레포 설계 초안이다.** 팀 리뷰와 노션 반영이 끝난 뒤 상태를 `✅ 확정`으로 변경해야 구현할 수 있다.
> 확정 이후에는 경로·필드명·타입·상태코드·에러코드를 임의로 바꾸지 않는다.

## 엔드포인트

| API명칭 | METHOD | URL | 권한 |
|---|---|---|---|
| Step 이슈 목록 조회 | GET | `/api/v1/steps/{stepId}/issues` | 스텝 접근 권한 |
| 이슈 생성 | POST | `/api/v1/steps/{stepId}/issues` | 스텝 EDITOR |
| 이슈 상세 조회 | GET | `/api/v1/issues/{issueId}` | 스텝 접근 권한 |
| 이슈 부분 수정 | PATCH | `/api/v1/issues/{issueId}` | 스텝 EDITOR |
| 이슈 상태 변경 | PATCH | `/api/v1/issues/{issueId}/status` | 스텝 EDITOR |
| 이슈 삭제 | DELETE | `/api/v1/issues/{issueId}` | 스텝 EDITOR |

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

---

## 1. Step 이슈 목록 조회

| 항목 | 내용 |
|---|---|
| Method · URL | `GET /api/v1/steps/{stepId}/issues` |
| 인증 필요 | Y · 스텝 접근 권한 |
| 요구사항 | ISS-005 · ISS-006 · USC-ISS-001 · USC-ISS-002 |

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `stepId` | Long | Y | 이슈를 조회할 Step 번호 |

**Request Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `blockId` | Long | N | 전달 시 해당 Block과 연결된 이슈만 조회 |

⛔ **페이징이 없다.** Step에 등록된 삭제되지 않은 이슈 전체를 반환한다.

⛔ 상태·담당자·관련 Block·우선순위 필터, 제목 검색, 마감일 정렬은 FE에서 처리한다.

⛔ `blockId`는 Block 상세 화면에서 관련 이슈만 조회할 때 사용한다. 요청 Block은 `stepId`와 같은 Step에 속해야 한다.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.content[].issueId` | Long | 이슈 번호 |
| `data.content[].title` | String | 이슈 제목 |
| `data.content[].status` | String | `TODO` · `IN_PROGRESS` · `DONE` |
| `data.content[].priority` | String | `LOW` · `MEDIUM` · `HIGH` |
| `data.content[].dueDate` | String | 마감 일시. `null` 허용 |
| `data.content[].assignees[].userId` | String | 담당자 사번 |
| `data.content[].assignees[].name` | String | 담당자 이름 |
| `data.content[].relatedBlocks[].blockId` | Long | 관련 Block 번호 |
| `data.content[].relatedBlocks[].title` | String | 관련 Block명 |
| `data.content[].relatedBlocks[].type` | String | Block 타입 |

> 조회 결과가 없으면 `200`과 빈 `data.content`를 반환한다.

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 |
| 400 | `ISS_BLOCK_STEP_MISMATCH` | `blockId`가 요청 Step에 속하지 않음 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `ISS_ACCESS_PERMISSION_REQUIRED` | Step 열람 권한 없음 |
| 404 | `ISS_STEP_NOT_FOUND` | Step 없음 또는 논리 삭제됨 |
| 404 | `ISS_BLOCK_NOT_FOUND` | 필터 Block 없음 또는 논리 삭제됨 |

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
| `data.dueDate` | String | 사용자 지정 마감 일시. `null` 허용 |
| `data.completedAt` | String | `DONE` 완료 시각. 완료 상태가 아니면 `null` |
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
| `dueDate` | String | N | 마감 일시. 명시적 `null`이면 마감일 해제 |
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
| `data.completedAt` | String | 변경 후 완료 시각. `null` 허용 |

> 칸반 Drag & Drop 실패 시 FE가 카드를 기존 칼럼으로 복구한다. 같은 칼럼 안의 카드 순서는 저장하지 않는다.

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 변경 성공 또는 동일 상태 멱등 처리 |
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

⛔ `issue.deleted_at`에 현재 시각을 기록하는 논리 삭제다.

⛔ 같은 트랜잭션에서 `issue_assign`, `issue_block` 관계 행을 제거한다.

⛔ 담당자 Employee와 관련 Block 원본은 삭제하지 않는다.

⛔ Issue 삭제는 Activity Log에 기록하지 않는다.

`data`는 `null`이다.

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 삭제 성공 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `ISS_EDIT_PERMISSION_REQUIRED` | Step 편집 권한 없음 |
| 404 | `ISS_NOT_FOUND` | Issue 없음 또는 이미 논리 삭제됨 |

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
