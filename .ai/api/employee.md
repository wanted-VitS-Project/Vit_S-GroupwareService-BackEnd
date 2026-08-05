# 👤 Employee API

**최종 업데이트**: 2026-08-04 · **담당**: 김동현 · Domain `인사` · SUB-Domain `Employee`

> 이 파일의 명세가 프론트와의 계약이다. 경로·필드명·타입·상태코드·에러코드를 **한 글자도 바꾸지 않는다** (`../API.md` §0).
> 변경이 필요하면 코드를 먼저 고치지 말고 **이 md 를 먼저 고친 뒤** 팀에 공유한다.

## 엔드포인트

| API명칭 | METHOD | URL | 권한 |
|---|---|---|---|
| 사원 목록 조회 | GET | `/api/v1/employees` | ADMIN |
| 사원 이름 검색 (결재선 지정용) | GET | `/api/v1/employees/search` | 로그인 사용자 |
| 사원 상세 조회 | GET | `/api/v1/employees/{userId}` | ADMIN |
| 사원 등록 | POST | `/api/v1/employees` | ADMIN |
| 사원 정보 수정 | PATCH | `/api/v1/employees/{userId}` | ADMIN |
| 퇴사 처리 | PATCH | `/api/v1/employees/{userId}/resignation` | ADMIN |
| 엑셀 템플릿 내려받기 | GET | `/api/v1/employees/bulk-template` | ADMIN |
| 엑셀 일괄 등록 **검증** | POST | `/api/v1/employees/bulk/validate` | ADMIN |
| 엑셀 일괄 등록 | POST | `/api/v1/employees/bulk` | ADMIN |

⭐ **상태 필드는 원본 2개다** (2026-08-03 재설계)

화면이 계정 상태(토글)와 비밀번호 상태(배지)를 **분리**해 보여주므로 계산값 하나로 합치지 않는다.

| 필드 | 값 | 화면 |
|---|---|---|
| `accountStatus` | `ACTIVE` · `INACTIVE` | 상세의 계정 상태 **토글** |
| `passwordStatus` | `NORMAL` · `RESET_REQUIRED` | 상세의 비밀번호 상태 **배지** |

목록 배지는 프론트가 조합한다 — `INACTIVE` → 정지 / `RESET_REQUIRED` → 재설정 필요 / 나머지 → 활성.

---

## 1. 사원 목록 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/employees` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | EMP-001 · EMP-003 · EMP-017 · USC-EMP-001 · USC-EMP-014 |

**Request Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `keyword` | String | N | 이름 또는 사번 부분 검색 |
| `departmentId` | Long | N | 부서 필터 |
| `role` | String | N | `MASTER` · `MEMBER` |
| `status` | String | N | `ACTIVE` · `RESET_REQUIRED` · `INACTIVE` |
| `resigned` | Boolean | N | 퇴사 여부. **미지정이면 재직자만** |
| `page` / `size` | int | N | 기본 0 / 20 |

⛔ **시스템 계정은 어떤 조건으로도 조회되지 않는다** (`EMP-003`).
⛔ **기본은 재직자만이다.** 화면에 퇴사 필터가 없다.

> ⭐ **`is_system` 의 범위** (2026-08-03 통합) — **사람이 아닌 계정 전부**다.
> `global/PERMISSION.md` §2-2 는 *"배치·크롤러가 쓰는 시스템 사원"* 으로, 제 설계는 *"ADMIN 공용 계정용 가상 사원"* 으로 썼다.
> 둘 다 같은 성격이라 정의를 **`ADMIN` · 배치 · 크롤러**로 넓혀 하나로 쓴다.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.content[].userId` | String | 사번 (`NOT NULL`) |
| `data.content[].name` | String | 이름 (`NOT NULL`) |
| `data.content[].email` | String | 이메일 **주소** (`null` 허용) |
| `data.content[].emailRegistered` | boolean | `false` 면 로그인 불가 → `⚠ 미등록` 배지 (`EMP-017`) |
| `data.content[].departmentName` | String | 부서명 (`null` 허용) |
| `data.content[].departmentPath` | String | `기술본부 / 개발팀` (`null` 허용) |
| `data.content[].jobPositionName` | String | 직급명 (`null` 허용) |
| `data.content[].role` | String | `MASTER` · `MEMBER` |
| `data.content[].accountStatus` | String | `ACTIVE` · `INACTIVE` |
| `data.content[].passwordStatus` | String | `NORMAL` · `RESET_REQUIRED` |
| `data.content[].resignedAt` | String | 퇴사일 (`null` = 재직중) |
| `data.page` / `size` / `totalElements` / `totalPages` | | 페이징 |

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 |
| 400 | `EMP_INVALID_PARAMETER` | 허용되지 않는 필터 값 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 이 아님 |

---

## 2. 사원 상세 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/employees/{userId}` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | EMP-002 · USC-EMP-002 |

**Response** — 목록 필드 + 아래

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.departmentId` | Long | 부서 ID (`null` 허용). 수정 폼 초기값 |
| `data.jobPositionId` | Long | 직급 ID (`null` 허용) |
| `data.phone` | String | 연락처 (`null` 허용) |
| `data.hiredAt` | String | 입사일 `yyyy-MM-dd` (`null` 허용) |
| `data.lastLoginAt` | String | 마지막 로그인 (`null` 허용) |
| `data.groups[]` | List\<Object\> | 소속 그룹 |
| `data.groups[].groupId` | Long | 그룹 번호 |
| `data.groups[].name` | String | 그룹명 |

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `ACC_ADMIN_REQUIRED` / `ACC_SYSTEM_ACCOUNT_NOT_ALLOWED` | 권한 없음 / 시스템 계정 |
| 404 | `EMP_NOT_FOUND` | 사원 없음 |

---

## 3. 사원 등록

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/employees` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | EMP-004 · EMP-018 · ACC-002~004 · ACC-010 · ACC-021 · ACC-022 · USC-EMP-003 · USC-ACC-002 · USC-ACC-017 · USC-ACC-018 |

⛔ **계정이 항상 함께 발급된다** (`ACC-002`). 사원만 등록하는 경로는 없다. **화면의 체크박스는 삭제 대상**이다.
⛔ **로그인 아이디는 사번이다.** 별도로 받지 않는다 (`ACC-003`).
⛔ **`ADMIN` 은 `role` 로 지정할 수 없다** (`ACC-001`).
⛔ **이메일이 없어도 등록된다.** 다만 초기 비밀번호를 전달할 수 없어 로그인하지 못한다 (`EMP-018` · `ACC-022`).

**Request Body**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `userId` | String | Y | 사번. 로그인 아이디로 사용 |
| `name` | String | Y | 이름 |
| `departmentId` | Long | Y | 부서 ID |
| `hiredAt` | String | Y | 입사일 `yyyy-MM-dd` |
| `role` | String | Y | `MASTER` · `MEMBER` |
| `jobPositionId` | Long | N | 직급 ID |
| `email` | String | N | 초기 비밀번호를 이 주소로 발송 |
| `phone` | String | N | 연락처 |

**Response** — `data.userId` · `data.name` · `data.emailRegistered` · `data.emailSent`

> **메일 발송이 실패해도 등록은 성공(`201`)이다.** 사원과 계정은 이미 만들어졌고 비밀번호만 다시 보내면 된다. 프론트는 `emailSent: false` 를 보고 안내 후 재설정을 유도한다.

| 코드 | code | 설명 |
|---|---|---|
| 201 | – | 등록 성공 |
| 400 | `EMP_INVALID_REQUEST` | 필수값 누락/형식 오류 |
| 400 | `EMP_ADMIN_ROLE_NOT_ALLOWED` | `role` 에 `ADMIN` |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 이 아님 |
| 404 | `EMP_DEPARTMENT_NOT_FOUND` / `EMP_JOB_POSITION_NOT_FOUND` | 부서/직급 없음 |
| 409 | `EMP_USER_ID_DUPLICATED` | 이미 등록된 사번 |

---

## 4. 사원 정보 수정

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/employees/{userId}` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | EMP-013 · EMP-014 · USC-EMP-004 · USC-EMP-005 |

⛔ **사번은 바꿀 수 없다.** 로그인 아이디이자 PK 이며 모든 사람 참조 FK 가 이 값을 가리킨다.
⛔ **전역 권한은 이 API 로 바꾸지 않는다** → `PATCH /api/v1/accounts/{userId}/role`.
⛔ **부서 배정은 별도 API 가 아니다.** `departmentId` 를 보내면 소속이 변경된다 (`EMP-014`).

**Request Body** — 전달한 필드만 수정

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `name` · `phone` · `email` | String | N | |
| `departmentId` · `jobPositionId` | Long | N | `jobPositionId` 에 `null` 을 보내면 직급 미지정 |
| `hiredAt` | String | N | `yyyy-MM-dd` |

**Response** — 사원 상세와 같은 구조

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 수정 성공 |
| 400 | `EMP_INVALID_REQUEST` | 형식 오류 또는 수정할 필드 없음 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` / `ACC_SYSTEM_ACCOUNT_NOT_ALLOWED` | |
| 404 | `EMP_NOT_FOUND` / `EMP_DEPARTMENT_NOT_FOUND` / `EMP_JOB_POSITION_NOT_FOUND` | |

---

## 5. 퇴사 처리

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/employees/{userId}/resignation` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | EMP-015 · EMP-016 · USC-EMP-006 |

⛔ **사원 정보는 삭제되지 않는다.** 퇴사일을 기록하고 계정만 비활성화한다. 과거 프로젝트·파일 이력에 이름이 남아야 한다 (`EMP-016`).
⛔ **계정 상태 변경 API 를 따로 호출하지 않는다.** 이 API 가 계정을 `INACTIVE` 로 함께 바꾼다.
🔴 **화면에 진입점이 없다.** 사원 상세와 `···` 메뉴 어디에도 버튼이 없어 프론트에 요청한 상태다.

**Request Body** — `resignedAt` String Y (`yyyy-MM-dd`)
**Response** — `data.userId` · `data.resignedAt` · `data.accountStatus`(`INACTIVE`)

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 퇴사 처리 성공 |
| 400 | `EMP_INVALID_REQUEST` / `EMP_ALREADY_RESIGNED` | 형식 오류 / 이미 퇴사 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` / `ACC_SYSTEM_ACCOUNT_NOT_ALLOWED` | |
| 404 | `EMP_NOT_FOUND` | 사원 없음 |

---

## 6. 엑셀 템플릿 내려받기

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/employees/bulk-template` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | EMP-005 · EMP-010 · USC-EMP-007 |

⛔ **응답이 JSON 이 아니다.** `.xlsx` 파일 바이너리.

| 항목 | 값 |
|---|---|
| Content-Type | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| Content-Disposition | `attachment; filename="employee_bulk_template.xlsx"` |
| 템플릿 컬럼 | 사번 · 이름 · 부서명 · 직급명 · 입사일 · 이메일 · 연락처 · **권한** |

> ⭐ **권한 컬럼이 템플릿에 있다** (2026-08-03 수정). 검증 오류에 "엑셀로는 관리자 권한을 부여할 수 없습니다" 가 있으므로 컬럼 자체는 존재하고 **`ADMIN` 값만 거부**한다 (`EMP-010`).

| 코드 | code |
|---|---|
| 200 | – |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` |

---

## 7. 엑셀 일괄 등록 검증

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/employees/bulk/validate` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | EMP-006~010 · EMP-019 · USC-EMP-008~012 |

⛔ **등록하지 않는다.** 화면 스텝퍼의 ② 검증 단계 전용이다.
⛔ **검증 실패도 `200` 이다.** 팀 응답 포맷의 에러 응답에 `data` 가 없어 행별 오류를 담을 수 없다.

**Request** — `multipart/form-data` · `file` File Y (`.xlsx` · `.xls` · 최대 5MB)

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.totalRows` | int | 데이터 행 수 |
| `data.validCount` | int | 등록 가능한 행 수 → `오류 제외하고 등록 (N건)` 버튼 |
| `data.errorCount` | int | 오류 행 수 |
| `data.errors[].row` | int | 엑셀 행 번호 |
| `data.errors[].userId` | String | 사번 (`null` 허용 — 사번 누락 행) |
| `data.errors[].name` | String | 이름 (`null` 허용) |
| `data.errors[].validation` | String | `REQUIRED_COLUMN` · `USER_ID_DUPLICATED` · `DEPARTMENT_NOT_FOUND` · `ADMIN_ROLE_NOT_ALLOWED` |
| `data.errors[].message` | String | 사람이 읽는 설명 |
| `data.emailNotRegisteredCount` | int | 이메일 없는 행 수 (`EMP-019`) |

```json
{ "httpStatus": 200, "message": "120건 중 8건 오류",
  "data": { "totalRows": 120, "validCount": 112, "errorCount": 8,
    "errors": [
      { "row": 3, "userId": "EMP099", "name": "홍길동",
        "validation": "REQUIRED_COLUMN", "message": "필수 컬럼 누락: 부서명" },
      { "row": 12, "userId": "EMP102", "name": "김철수",
        "validation": "USER_ID_DUPLICATED", "message": "파일 내 사번 중복 (7행)" },
      { "row": 24, "userId": "EMP115", "name": "정대현",
        "validation": "ADMIN_ROLE_NOT_ALLOWED", "message": "엑셀로는 관리자 권한을 부여할 수 없습니다" }
    ],
    "emailNotRegisteredCount": 5 } }
```

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 검증 완료 (오류가 있어도 200) |
| 400 | `EMP_FILE_REQUIRED` / `EMP_FILE_TYPE_INVALID` / `EMP_FILE_SIZE_EXCEEDED` | 파일 없음 / 형식 아님 / 5MB 초과 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |

> 파일을 열기 **전에** 알 수 있는 오류(없음·형식·크기)만 `400` 이다. 파일을 연 뒤의 오류는 모두 `200` + `errors` 로 간다.

---

## 8. 엑셀 일괄 등록

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/employees/bulk` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | EMP-005~012 · EMP-019 · USC-EMP-007~013 |

⭐ **부분 등록을 허용한다** (2026-08-03 변경). 07-30 의 "전체 롤백 확정"은 **폐기**됐다.
화면에 `오류 제외하고 등록 (112건)` 버튼이 있고, 롤백 근거였던 "재업로드 시 중복"은 부분 등록이면 해소된다.

**Request** — `multipart/form-data` · `file` File Y · `skipErrors` Boolean N (기본 `false`)

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.totalRows` | int | 요청 건수 |
| `data.registeredCount` | int | 성공 건수 |
| `data.failedCount` | int | 실패 건수 |
| `data.errors[]` | List\<Object\> | 검증과 같은 구조 |
| `data.emailSentCount` | int | 초기 비밀번호 메일 발송 성공 건수 |
| `data.emailNotRegistered[]` | List\<Object\> | `userId` · `name` (`EMP-019`) |

> 화면 ③ 결과의 카드 4개가 `totalRows` · `registeredCount` · `failedCount` · `emailNotRegistered.length` 다.

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 처리 완료 |
| 400 | `EMP_FILE_REQUIRED` / `EMP_FILE_TYPE_INVALID` / `EMP_FILE_SIZE_EXCEEDED` | |
| 400 | `EMP_HAS_ERRORS` | `skipErrors=false` 인데 오류 행이 있음 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |

---

## 9. 사원 이름 검색 (결재선 지정용)

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/employees/search` |
| 인증 필요 | Y · **로그인 사용자 누구나** (ADMIN 전용 아님) |
| 요구사항 | EMP-020 · USC-EMP-015 |
| 요청 출처 | 결재 도메인 — 결재선 등록 화면에서 기안자가 결재자를 이름으로 검색 → 후보에서 선택 |

> ⚠️ **인사관리용 목록(`GET /api/v1/employees`, ADMIN)과 권한·용도가 다르다.**
> 이건 결재자 자동완성이라 **로그인한 사용자 누구나** 호출한다. `SecurityConfig` 에서 두 경로의 권한을 분리한다.

**Request Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `name` | String | Y | 이름 부분 일치 검색어 |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data[].userId` | String | 사번 |
| `data[].name` | String | 이름 |
| `data[].department` | String | 부서명 (동명이인 구분용, `null` 허용) |
| `data[].position` | String | 직급명 (동명이인 구분용, `null` 허용) |

> 배열(후보 목록)로 내려준다. **급여 등 민감 정보는 포함하지 않는다** — 위 4개 필드만.
> ⛔ **시스템 계정(`is_system=1`)과 퇴사자는 결재자 후보에 나오지 않는다** — `is_system=0` · 재직자만 (`EMP-003`).

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 (결과 없으면 빈 배열) |
| 400 | `EMP_INVALID_PARAMETER` | `name` 누락 등 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
