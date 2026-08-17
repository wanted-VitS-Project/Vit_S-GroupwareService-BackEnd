# 👥 EmployeeGroup API

**최종 업데이트**: 2026-08-03 · **담당**: 김동현 · Domain `인사` · SUB-Domain `EmployeeGroup`

> 이 파일의 명세가 프론트와의 계약이다. 경로·필드명·타입·상태코드·에러코드를 **한 글자도 바꾸지 않는다** (`../API.md` §0).
> 변경이 필요하면 코드를 먼저 고치지 말고 **이 md 를 먼저 고친 뒤** 팀에 공유한다.

## §0 엔드포인트 요약

| 메서드 | 경로 | 무엇 | 상태 | 권한 |
|---|---|---|---|---|
| GET | `/api/v1/employee-groups` | [그룹 목록 조회](#1-그룹-목록-조회) | — | 전체 사용자 |
| POST | `/api/v1/employee-groups` | [그룹 생성](#2-그룹-생성) | — | ADMIN |
| PATCH | `/api/v1/employee-groups/{groupId}` | [그룹 이름 수정](#3-그룹-이름-수정) | — | ADMIN |
| DELETE | `/api/v1/employee-groups/{groupId}` | [그룹 삭제](#4-그룹-삭제) | — | ADMIN |
| GET | `/api/v1/employee-groups/{groupId}/members` | [구성원 목록 조회](#5-구성원-목록-조회) | — | 전체 사용자 |
| POST | `/api/v1/employee-groups/{groupId}/members` | [구성원 추가](#6-구성원-추가) | — | ADMIN |
| DELETE | `/api/v1/employee-groups/{groupId}/members/{userId}` | [구성원 제거](#7-구성원-제거) | — | ADMIN |

**조회 2개만 `전체 사용자`다.** 그룹은 프로젝트 멤버 선택·페이지 권한 부여에서 "한 번에 고르는 도구"로 쓰이므로 일반 사용자도 목록과 구성원을 봐야 한다 (`GRP-001` · `GRP-008`).

⛔ **그룹 관리 주체가 미확정이다.** 생성·수정·삭제·구성원 변경을 `ADMIN` 전용으로 가정했다. MASTER 도 허용할지 팀 확인 필요.

## 🔑 그룹은 권한이 아니다

| 원칙 | 내용 |
|------|------|
| 정체 | 부서와 별도인 **사람 묶음 = 선택용 인덱스** |
| 저장 | 권한 부여 시 **개인 단위 스냅샷.** 그룹 참조를 남기지 않는다 |
| 결과 | 그룹 구성원이 바뀌거나 그룹이 삭제돼도 **이미 부여된 권한은 변하지 않는다** (`GRP-010`) |
| 그래서 | 삭제를 차단하지 않는다. 부서·직급과 다른 이유가 이것이다 |

---

## 1. 그룹 목록 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/employee-groups` |
| 인증 필요 | Y · 전체 사용자 |
| 요구사항 | GRP-001 · USC-GRP-001 |

**Request Parameter** — `keyword` String N (그룹명 부분 검색)

⛔ **페이징이 없다.**
⛔ **구성원 목록은 포함되지 않는다.** 필요하면 구성원 목록 조회를 호출한다.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.content[].groupId` | Long | 그룹 번호 (`NOT NULL`) |
| `data.content[].name` | String | 그룹명 (`NOT NULL`) |
| `data.content[].description` | String | 설명 (`null` 허용) |
| `data.content[].memberCount` | int | 구성원 수. 시스템 계정·퇴사자 제외 |
| `data.content[].createdByName` | String | 생성자 이름 |
| `data.content[].createdAt` | String | 생성일 `yyyy-MM-dd` |

> 정렬은 이름 오름차순.

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |

---

## 2. 그룹 생성

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/employee-groups` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | GRP-002 · USC-GRP-002 |

⛔ **그룹명은 중복될 수 없다.**
⛔ **생성 시 구성원을 함께 넣지 않는다.** 빈 그룹을 만든 뒤 구성원 추가를 호출한다 (화면 안내 문구와 일치).

**Request Body**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `name` | String | Y | 그룹명. 최대 50자 |
| `description` | String | N | 설명. 최대 500자 |

**Response** — `groupId` · `name` · `description` · `memberCount`(0)

| 코드 | code | 설명 |
|---|---|---|
| 201 | – | 생성 성공 |
| 400 | `GRP_INVALID_REQUEST` | 그룹명이 비었거나 길이 초과 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |
| 409 | `GRP_NAME_DUPLICATED` | 이미 존재하는 그룹명 |

---

## 3. 그룹 이름 수정

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/employee-groups/{groupId}` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | GRP-003 · USC-GRP-003 |

⛔ **그룹명 변경은 기존 권한에 영향을 주지 않는다** (`GRP-010`).

**Request Body** — 전달한 필드만 수정

| 파라미터 | 타입 | 필수 |
|---|---|:---:|
| `name` | String | N |
| `description` | String | N |

**Response** — 목록과 같은 구조

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 수정 성공 |
| 400 | `GRP_INVALID_REQUEST` | 수정할 필드 없음 또는 길이 초과 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |
| 404 | `GRP_NOT_FOUND` | 그룹 없음 |
| 409 | `GRP_NAME_DUPLICATED` | 이미 존재하는 그룹명 |

---

## 4. 그룹 삭제

| 항목 | 내용 |
|------|------|
| Method · URL | `DELETE /api/v1/employee-groups/{groupId}` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | GRP-004 · GRP-010 · USC-GRP-004 |

⛔ **구성원이 있어도 삭제된다.** 구성원 매핑은 `CASCADE` 로 함께 제거된다.
⛔ **그룹을 지워도 권한은 사라지지 않는다.** 개인 단위 스냅샷이므로 그룹과 무관하게 유지된다 (`GRP-010`). **부서·직급과 달리 사용 중 삭제를 차단하지 않는 이유가 이것이다.**

`data` 는 `null`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 삭제 성공 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |
| 404 | `GRP_NOT_FOUND` | 그룹 없음 |

---

## 5. 구성원 목록 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/employee-groups/{groupId}/members` |
| 인증 필요 | Y · 전체 사용자 |
| 요구사항 | GRP-008 · GRP-009 · USC-GRP-005 |

⛔ **시스템 계정과 퇴사자는 포함되지 않는다.**

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.groupId` | Long | 그룹 번호 |
| `data.name` | String | 그룹명 |
| `data.content[].userId` | String | 사번 (`NOT NULL`) |
| `data.content[].name` | String | 이름 (`NOT NULL`) |
| `data.content[].departmentPath` | String | `기술본부 / 개발팀` (`null` 허용) |
| `data.content[].jobPositionName` | String | 직급명 (`null` 허용) |
| `data.content[].addedAt` | String | 추가일 `yyyy-MM-dd` |

> `addedAt` 은 화면의 `추가일` 컬럼이다. 2026-07-31 에 "권한이 아니니 불필요"로 뺐던 `employee_group_member.created_at` 을 **화면 근거로 되살렸다.**
>
> 정렬은 이름 오름차순.

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 404 | `GRP_NOT_FOUND` | 그룹 없음 |

---

## 6. 구성원 추가

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/employee-groups/{groupId}/members` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | GRP-005 · GRP-007 · USC-GRP-005 |

⛔ **한 사원이 여러 그룹에 속할 수 있다** (`GRP-007`).
⛔ **이미 구성원인 사번은 오류가 아니다.** 조용히 건너뛰고 집계에만 반영한다. 여러 ADMIN 이 동시에 편집할 수 있으므로 멱등하게 동작한다.
⛔ **구성원을 추가해도 기존 권한은 늘어나지 않는다** (`GRP-010`).

> 화면의 `이미 소속` 회색 처리는 프론트가 구성원 목록과 대조해 표시한다. 서버는 별도로 알려주지 않는다.

**Request Body** — `userIds` List\<String\> Y (1개 이상)

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.groupId` | Long | 그룹 번호 |
| `data.requestedCount` | int | 요청 건수 |
| `data.addedCount` | int | 새로 추가된 건수 |
| `data.alreadyMemberCount` | int | 이미 구성원이어서 건너뛴 건수 |
| `data.memberCount` | int | 처리 후 전체 구성원 수 |

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 처리 완료 |
| 400 | `GRP_INVALID_REQUEST` | `userIds` 가 비어 있음 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` / `ACC_SYSTEM_ACCOUNT_NOT_ALLOWED` | |
| 404 | `GRP_NOT_FOUND` | 그룹 없음 |
| 404 | `EMP_NOT_FOUND` | 존재하지 않는 사번 포함. **전체를 거부한다** |

---

## 7. 구성원 제거

| 항목 | 내용 |
|------|------|
| Method · URL | `DELETE /api/v1/employee-groups/{groupId}/members/{userId}` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | GRP-006 · USC-GRP-006 |

⛔ **한 명씩 제거한다.** 추가는 다건이지만 제거는 화면에서 개별 행의 `제거` 버튼으로만 일어난다.
⛔ **제거해도 그 사원이 이미 받은 권한은 사라지지 않는다** (`GRP-010`).

**Response** — `data.groupId` · `data.memberCount`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 제거 성공 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |
| 404 | `GRP_NOT_FOUND` | 그룹 없음 |
| 404 | `GRP_MEMBER_NOT_FOUND` | 이 그룹의 구성원이 아님 |
