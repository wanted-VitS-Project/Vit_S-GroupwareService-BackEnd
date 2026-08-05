# 🏢 Department API

**최종 업데이트**: 2026-08-03 · **담당**: 김동현 · Domain `인사` · SUB-Domain `Department`

> 이 파일의 명세가 프론트와의 계약이다. 경로·필드명·타입·상태코드·에러코드를 **한 글자도 바꾸지 않는다** (`../API.md` §0).
> 변경이 필요하면 코드를 먼저 고치지 말고 **이 md 를 먼저 고친 뒤** 팀에 공유한다.

## 엔드포인트

| API명칭 | METHOD | URL | 권한 |
|---|---|---|---|
| 부서 목록 조회 | GET | `/api/v1/departments` | 전체 사용자 |
| 부서 생성 | POST | `/api/v1/departments` | ADMIN |
| 부서명 수정 | PATCH | `/api/v1/departments/{departmentId}` | ADMIN |
| 부서 삭제 | DELETE | `/api/v1/departments/{departmentId}` | ADMIN |

**요구사항** `DEPT-001`~`DEPT-011` · **유스케이스** `USC-DEPT-001`~`USC-DEPT-010`

`USC-DEPT-010`(소속 사원 보기)은 `GET /api/v1/employees?departmentId=` 로 넘어가므로 별도 API 가 없다.

⭐ **계층 최대 2단** — 부서 추가 모달의 상위 부서 드롭다운에 **최상위 부서만** 나오는 것이 근거다. 3단은 만들 수 없으므로 서버도 막는다 (`DEPT-005`).

---

## 1. 부서 목록 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/departments` |
| 인증 필요 | Y · 전체 사용자 |
| 요구사항 | DEPT-001 · DEPT-002 · USC-DEPT-001 · USC-DEPT-002 |

⛔ **페이징이 없다.** 부서는 회사당 수십 개 수준이라 전체를 트리로 반환한다.
⛔ **`children` 안에 또 `children` 이 없다** (최대 2단).
⛔ **정렬은 생성 순서(`departmentId` 오름차순)다.** 화면에 순서 변경 기능이 없어 정렬 컬럼을 두지 않았다.
⛔ **`전체 사용자` 권한인 이유** — 사원 등록·수정의 부서 드롭다운, 사원 목록 필터, 구성원 추가 모달 필터에서 쓰인다. ADMIN 전용으로 좁히면 이 화면들이 막힌다.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.content[]` | List\<Object\> | **최상위 부서** 목록 |
| `data.content[].departmentId` | Long | 부서 번호 (`NOT NULL`) |
| `data.content[].name` | String | 부서명 (`NOT NULL`) |
| `data.content[].directEmployeeCount` | int | **직속** 사원 수 → 삭제 차단 판정 |
| `data.content[].totalEmployeeCount` | int | **하위 포함** 사원 수 → **화면에 표시하는 값** |
| `data.content[].children[]` | List\<Object\> | 하위 부서. 없으면 빈 배열. 같은 필드 구성 |

> 🔑 **인원 수를 두 개 주는 이유** — 화면의 상위 부서 인원이 하위 합계다 (기술본부 18명 = 6+5+5+2).
> 합계만 주면 "직속 사원이 0명인데 삭제가 막히는" 이유를 프론트가 설명할 수 없다.
>
> **시스템 계정과 퇴사자는 인원 수에서 제외한다.**

```json
{ "httpStatus": 200, "message": "부서 목록 조회 성공",
  "data": { "content": [
    { "departmentId": 1, "name": "경영지원본부", "directEmployeeCount": 0, "totalEmployeeCount": 4,
      "children": [
        { "departmentId": 4, "name": "인사팀", "directEmployeeCount": 2, "totalEmployeeCount": 2, "children": [] },
        { "departmentId": 5, "name": "회계팀", "directEmployeeCount": 2, "totalEmployeeCount": 2, "children": [] }
      ] }
  ] } }
```

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |

---

## 2. 부서 생성

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/departments` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | DEPT-003~006 · USC-DEPT-003~006 |

⛔ **`+ 부서 추가` 와 `하위 부서 추가` 가 같은 API 다.** `parentId` 유무로 갈린다. 후자는 프론트가 `parentId` 를 고정해 보낸다.
⛔ **계층은 최대 2단.** 하위 부서를 `parentId` 로 지정하면 `409` (`DEPT-005`).
⛔ **부서명은 전체에서 중복될 수 없다** (`DEPT-006`).

> 🔑 **같은 상위 부서 안에서만 유니크하게 하지 않은 이유** — MySQL 은 `parent_id` 가 `NULL` 인 행끼리 중복을 허용해 **최상위 부서명 중복이 막히지 않는다.** 사원 등록 드롭다운에 같은 이름이 두 개 뜨는 것도 막아야 한다.

**Request Body**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `name` | String | Y | 부서명. 최대 50자 |
| `parentId` | Long | N | 생략하면 최상위 부서 |

**Response** — `departmentId` · `name` · `parentId` · `parentName` · `directEmployeeCount`(0) · `totalEmployeeCount`(0)

| 코드 | code | 설명 |
|---|---|---|
| 201 | – | 생성 성공 |
| 400 | `DEPT_INVALID_REQUEST` | 부서명이 비었거나 50자 초과 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |
| 404 | `DEPT_PARENT_NOT_FOUND` | 상위 부서 없음 |
| 409 | `DEPT_NAME_DUPLICATED` | 이미 존재하는 부서명 |
| 409 | `DEPT_MAX_DEPTH_EXCEEDED` | 하위 부서를 상위로 지정. 계층은 최대 2단 |

---

## 3. 부서명 수정

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/departments/{departmentId}` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | DEPT-007 · USC-DEPT-007 |

⛔ **상위 부서를 바꿀 수 없다 — 부서 이동 기능이 없다.** 부서명 수정 모달에 상위 부서 필드가 없는 것을 근거로 했다. 이동이 필요하면 `parentId` 를 추가해야 하며, 그때는 계층 2단 검사와 순환 참조 검사가 함께 필요하다. → 프론트 확인 대기 (`ASK-03`)
⛔ **부서명을 바꿔도 소속 사원의 배정은 그대로다.** 사원은 `department_id` 를 참조한다.

**Request Body** — `name` String Y (최대 50자)
**Response** — `departmentId` · `name` · `parentId` · `parentName`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 수정 성공 |
| 400 | `DEPT_INVALID_REQUEST` | 부서명이 비었거나 50자 초과 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |
| 404 | `DEPT_NOT_FOUND` | 부서 없음 |
| 409 | `DEPT_NAME_DUPLICATED` | 이미 존재하는 부서명 |

---

## 4. 부서 삭제

| 항목 | 내용 |
|------|------|
| Method · URL | `DELETE /api/v1/departments/{departmentId}` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | DEPT-008~010 · USC-DEPT-008 · USC-DEPT-009 |

⛔ **차단 조건이 두 개다.** 직속 사원이 있거나 하위 부서가 있으면 삭제되지 않는다.
⛔ **하위 부서를 함께 지우지 않는다.** CASCADE 로 두면 팀 하나 지우려다 본부 전체가 사라진다.
⛔ **소프트 삭제가 아니다.** 부서는 권한 판정에 쓰이지 않고 이력에 스냅샷으로 남지도 않으므로 행을 제거한다 (`DEPT-011`).

> ℹ️ **파일 업로더 정보는 영향받지 않는다.** `file_version` 이 업로더의 부서명을 **스냅샷**으로 저장하므로, 부서를 지워도 과거 업로드 이력의 부서 표시는 그대로 남는다.

`data` 는 `null`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 삭제 성공 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |
| 404 | `DEPT_NOT_FOUND` | 부서 없음 |
| 409 | `DEPT_HAS_EMPLOYEES` | 직속 사원 있음. `message` 에 인원 수 |
| 409 | `DEPT_HAS_CHILDREN` | 하위 부서 있음. `message` 에 하위 부서 수 |

## 요구사항 명세

| ID | 대분류 | 중분류 | 소분류 | 설명 |
|---|---|---|---|---|
| DEPT-001 | 부서 관리 | 부서 조회 | 부서 목록 조회 | 사용자는 부서를 계층 구조로 조회할 수 있다. |
| DEPT-002 | 부서 관리 | 부서 조회 | 인원 수 제공 | 시스템은 각 부서의 직속 인원과 하위 부서를 포함한 전체 인원을 제공해야 한다. |
| DEPT-003 | 부서 관리 | 부서 생성 | 최상위 부서 생성 | ADMIN은 상위 부서 없이 부서를 생성할 수 있다. |
| DEPT-004 | 부서 관리 | 부서 생성 | 하위 부서 생성 | ADMIN은 상위 부서를 지정해 하위 부서를 생성할 수 있다. |
| DEPT-005 | 부서 관리 | 부서 생성 | 계층 2단 제한 | 시스템은 부서 계층을 최대 2단으로 제한해야 한다. |
| DEPT-006 | 부서 관리 | 부서 생성 | 부서명 중복 차단 | 시스템은 이미 존재하는 부서명의 생성을 차단해야 한다. |
| DEPT-007 | 부서 관리 | 부서 수정 | 부서명 수정 | ADMIN은 부서명을 수정할 수 있다. |
| DEPT-008 | 부서 관리 | 부서 삭제 | 부서 삭제 | ADMIN은 부서를 삭제할 수 있다. |
| DEPT-009 | 부서 관리 | 부서 삭제 | 소속 사원 삭제 차단 | 시스템은 소속 사원이 있는 부서의 삭제를 차단해야 한다. |
| DEPT-010 | 부서 관리 | 부서 삭제 | 하위 부서 삭제 차단 | 시스템은 하위 부서가 있는 부서의 삭제를 차단해야 한다. |
| DEPT-011 | 부서 관리 | 제약 | 권한 판정 미사용 | 시스템은 부서를 권한 판정에 사용하지 않아야 한다. |

## 유스케이스

| ID | 시나리오 | 사용자 |
|---|---|---|
| USC-DEPT-001 | 부서 목록 조회 | 전체 사용자 |
| USC-DEPT-002 | 부서 인원 수 집계 | 시스템 |
| USC-DEPT-003 | 최상위 부서 생성 | ADMIN |
| USC-DEPT-004 | 하위 부서 생성 | ADMIN |
| USC-DEPT-005 | 계층 2단 초과 차단 | 시스템 |
| USC-DEPT-006 | 부서명 중복 검증 | 시스템 |
| USC-DEPT-007 | 부서명 수정 | ADMIN |
| USC-DEPT-008 | 부서 삭제 | ADMIN |
| USC-DEPT-009 | 소속 사원·하위 부서 삭제 차단 | 시스템 |
| USC-DEPT-010 | 소속 사원 보기 | ADMIN |
