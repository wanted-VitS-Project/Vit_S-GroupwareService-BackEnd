# 👤 Employee API

**최종 업데이트**: 2026-08-18 (엑셀 §7·§8 `autoCreateMasters` 옵션 — 목록에 없는 전공/자격증을 자동 생성. 검증 응답 `newMasters`·등록 응답 `createdMasters` 추가, 기본값 `false` 면 기존 동작 그대로) · 2026-08-11 (프로필 사진 아바타 서빙 API 추가 §10) · **담당**: 김동현 · Domain `인사` · SUB-Domain `Employee`

> 이 파일의 명세가 프론트와의 계약이다. 경로·필드명·타입·상태코드·에러코드를 **한 글자도 바꾸지 않는다** (`../API.md` §0).
> 변경이 필요하면 코드를 먼저 고치지 말고 **이 md 를 먼저 고친 뒤** 팀에 공유한다.

## §0 엔드포인트 요약

| 메서드 | 경로 | 무엇 | 상태 | 권한 |
|---|---|---|---|---|
| GET | `/api/v1/employees` | [사원 목록 조회](#1-사원-목록-조회) | — | ADMIN |
| GET | `/api/v1/employees/search` | [사원 이름 검색 (결재선 지정용)](#9-사원-이름-검색-결재선-지정용) | — | 로그인 사용자 |
| GET | `/api/v1/employees/{userId}` | [사원 상세 조회](#2-사원-상세-조회) | — | ADMIN |
| POST | `/api/v1/employees` | [사원 등록](#3-사원-등록) | — | ADMIN |
| PATCH | `/api/v1/employees/{userId}` | [사원 정보 수정](#4-사원-정보-수정) | — | ADMIN |
| PATCH | `/api/v1/employees/{userId}/resignation` | [퇴사 처리](#5-퇴사-처리) | — | ADMIN |
| GET | `/api/v1/employees/bulk-template` | [엑셀 템플릿 내려받기](#6-엑셀-템플릿-내려받기) | — | ADMIN |
| POST | `/api/v1/employees/bulk/validate` | [엑셀 일괄 등록 검증](#7-엑셀-일괄-등록-검증) | — | ADMIN |
| POST | `/api/v1/employees/bulk` | [엑셀 일괄 등록](#8-엑셀-일괄-등록) | — | ADMIN |
| GET | `/api/v1/employees/{userId}/profile-image` | [프로필 사진 조회(아바타 서빙)](#10-프로필-사진-조회-아바타-서빙) | — | **로그인 사용자 누구나** |

> ⚠️ **이 목록은 대부분 ADMIN 전용이지만 프로필 사진 조회만 로그인 사용자 전체다** (§9 이름 검색과 같은 예외). 좌상단·프로젝트 멤버·결재선 아바타가 남의 사진을 그려야 하기 때문. `SecurityConfig` 에서 이 경로의 권한을 분리한다. 업로드/삭제(본인만)는 auth 도메인(`auth.md` §5-1·§5-2)에 있다.

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
> `../docs/global/PERMISSION.md` §2-2 는 *"배치·크롤러가 쓰는 시스템 사원"* 으로, 제 설계는 *"ADMIN 공용 계정용 가상 사원"* 으로 썼다.
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
| `data.content[].profileImageUrl` | String | 아바타 서빙 경로(§10). 사진 없으면 `null` — presigned 아님, 프론트가 `null` 이면 이니셜 아바타 |
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
| `data.educations[]` | List\<Object\> | 학력(1:N · HR-V1 `QUAL-003`). 그룹과 같은 조회 패턴 |
| `data.educations[].majorId` | Long | 전공 마스터 ID |
| `data.educations[].majorName` | String | 전공명(스냅샷 아님 — 마스터 조인) |
| `data.educations[].degree` | String | 학위 enum `BACHELOR`·`MASTER`·`DOCTOR` |
| `data.educations[].school` | String | 학교 (`null` 허용) |
| `data.certificates[]` | List\<Object\> | 자격증(1:N) |
| `data.certificates[].certificateId` | Long | 자격증 마스터 ID |
| `data.certificates[].certificateName` | String | 자격증명(마스터 조인) |
| `data.certificates[].acquiredDate` | String | 취득일 `yyyy-MM-dd` (`null` 허용) |

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
⛔ **로그인 아이디는 사번이다.** 별도로 받지 않는다 (`ACC-003`). 초기 비밀번호 메일에 **로그인 아이디(사번)를 임시 비밀번호와 함께** 안내한다 (2026-08-17 추가 — 재설정 메일도 동일). 사용자가 아이디를 몰라 로그인 못 하는 구멍을 막는다.
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
| `educations[]` | List\<Object\> | N | 학력. `{majorId(필수), degree(필수 `BACHELOR`·`MASTER`·`DOCTOR`), school(선택)}`. 전공 마스터에 없으면 `MAJOR_NOT_FOUND` (HR-V1 `QUAL-001`) |
| `certificates[]` | List\<Object\> | N | 자격증. `{certificateId(필수), acquiredDate(선택 `yyyy-MM-dd`)}`. 마스터에 없으면 `CERT_NOT_FOUND` (`QUAL-002`) |

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
| 404 | `MAJOR_NOT_FOUND` / `CERT_NOT_FOUND` | 학력 전공/자격증이 마스터에 없음 |
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
| `educations[]` | List\<Object\> | N | 학력 **전체 교체**(`QUAL-004`). 등록과 같은 항목 구조. **생략(미전송)하면 유지, `[]` 면 전부 삭제** |
| `certificates[]` | List\<Object\> | N | 자격증 **전체 교체**. 생략 유지, `[]` 전부 삭제 |

> ⛔ **학력·자격증은 부분 diff 가 아니라 전체 교체다**(`QUAL-004`). 보낸 배열이 최종 상태이며, 기존 행을 지우고 다시 넣는다. **필드를 아예 보내지 않으면(생략) 기존을 건드리지 않는다** — `null`(미전송)과 `[]`(비우기)를 구분한다.

**Response** — 사원 상세와 같은 구조(`educations[]`·`certificates[]` 포함)

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 수정 성공 |
| 400 | `EMP_INVALID_REQUEST` | 형식 오류 또는 수정할 필드 없음 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` / `ACC_SYSTEM_ACCOUNT_NOT_ALLOWED` | |
| 404 | `EMP_NOT_FOUND` / `EMP_DEPARTMENT_NOT_FOUND` / `EMP_JOB_POSITION_NOT_FOUND` / `MAJOR_NOT_FOUND` / `CERT_NOT_FOUND` | |

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
| 템플릿 컬럼 | 사번 · 이름 · 부서명 · 직급명 · 입사일 · 이메일 · 연락처 · **권한** · **학력** · **자격증** |

> ⭐ **권한 컬럼이 템플릿에 있다** (2026-08-03 수정). 검증 오류에 "엑셀로는 관리자 권한을 부여할 수 없습니다" 가 있으므로 컬럼 자체는 존재하고 **`ADMIN` 값만 거부**한다 (`EMP-010`).
>
> 🆕 **학력·자격증 컬럼** (2026-08-13, HR-V1 `QUAL-005`) — 기존 8열 뒤에 붙는 **선택 컬럼 2개**:
>
> | 컬럼 | 형식 | 예 |
> |---|---|---|
> | 학력 | `전공:학위` · 여러 개는 `;` · `,` · 셀 내 줄바꿈 | `컴퓨터공학:학사, 소프트웨어공학:석사` |
> | 자격증 | `자격증명` · 여러 개는 `;` · `,` · 셀 내 줄바꿈 | `정보처리기사; SQLD` |
>
> - 여러 항목 구분자는 **세미콜론(`;`)·쉼표(`,`)·셀 안 줄바꿈(Alt+Enter)** 을 모두 허용하며, 구분자 뒤 공백은 무시한다 (2026-08-17 확대).
>   ⚠️ 구분자(`,` `;`)·`전공:학위` 의 `:` 가 든 마스터 이름은 쪼개지므로 **마스터 이름에 `,` `;` `:` 를 넣을 수 없다** (2026-08-18 — `qualification.md` 공통 원칙, 생성·수정 시 400).
> - 학위는 엑셀에선 한글(`학사`·`석사`·`박사`)로 쓰고 파서가 enum(`BACHELOR`·`MASTER`·`DOCTOR`)으로 변환한다.
> - 전공·자격증명은 **마스터에 등록된 이름과 정확히 일치**해야 한다(목록 밖은 행 오류). 단 §7·§8 의 `autoCreateMasters=true` 면 목록 밖 이름을 등록 시 자동 생성한다 (2026-08-18).

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

**Request** — `multipart/form-data`

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `file` | File | Y | `.xlsx` · `.xls` · 최대 5MB |
| `autoCreateMasters` | Boolean | N | 기본 `false`. **`true` 면 목록에 없는 전공/자격증을 오류로 보지 않고 등록 시 자동 생성 대상(`newMasters`)으로 분류한다** (2026-08-18 추가). 화면 ① 업로드 단계의 체크박스 "목록에 없는 전공/자격증은 자동 등록" |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.totalRows` | int | 데이터 행 수 |
| `data.validCount` | int | 등록 가능한 행 수 → `오류 제외하고 등록 (N건)` 버튼 |
| `data.errorCount` | int | 오류 행 수 |
| `data.errors[].row` | int | 엑셀 행 번호 |
| `data.errors[].userId` | String | 사번 (`null` 허용 — 사번 누락 행) |
| `data.errors[].name` | String | 이름 (`null` 허용) |
| `data.errors[].validation` | String | `REQUIRED_COLUMN` · `USER_ID_DUPLICATED` · `DEPARTMENT_NOT_FOUND` · `ADMIN_ROLE_NOT_ALLOWED` · `EDU_NOT_FOUND` · `CERT_NOT_FOUND` |
| `data.errors[].message` | String | 사람이 읽는 설명 |
| `data.emailNotRegisteredCount` | int | 이메일 없는 행 수 (`EMP-019`) |
| `data.newMasters.majors[]` | List\<Object\> | **등록 시 새로 생성될 전공** `{name, rowCount}` — `autoCreateMasters=true` 일 때만 채워진다(`false` 면 빈 배열). `rowCount` = 그 이름을 쓰는 **유효 행** 수 |
| `data.newMasters.certificates[]` | List\<Object\> | 등록 시 새로 생성될 자격증 `{name, rowCount}` — 위와 동일 |

> ⭐ **`autoCreateMasters` 가 바꾸는 것은 `EDU_NOT_FOUND`·`CERT_NOT_FOUND` 판정뿐이다.** `true` 면 그 두 오류가 사라지고 대신 `newMasters` 에 이름이 쌓인다.
> 학위 표기 오류·`전공:학위` 형식 오류·이름 100자 초과·**자격증명에 `:` 포함**은 옵션과 무관하게 여전히 `REQUIRED_COLUMN` 행 오류다(마스터 이름 규칙 — `qualification.md` 공통 원칙).
> ⚠️ **화면은 `newMasters` 를 반드시 보여줘야 한다** — 오타("정보 처리기사")가 그대로 마스터로 생성되는 걸 관리자가 등록 전에 잡을 수 있는 유일한 지점이다.
> `newMasters` 는 **오류 없는 행 기준**이다. 다른 오류로 빠지는 행이 유일하게 참조하는 이름은 여기에 안 나오고, 등록 때도 생성되지 않는다.

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
    "emailNotRegisteredCount": 5,
    "newMasters": {
      "majors": [ { "name": "컴퓨터공학", "rowCount": 14 }, { "name": "산업공학", "rowCount": 3 } ],
      "certificates": [ { "name": "정보처리기사", "rowCount": 21 } ] } } }
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

**Request** — `multipart/form-data`

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `file` | File | Y | `.xlsx` · `.xls` · 최대 5MB |
| `skipErrors` | Boolean | N | 기본 `false`. `true` 면 오류 행을 빼고 유효 행만 등록(부분 등록) |
| `autoCreateMasters` | Boolean | N | 기본 `false`. `true` 면 목록에 없는 전공/자격증을 **사원 등록 전에 마스터로 먼저 생성**한 뒤 참조한다 (2026-08-18 추가). ⚠️ **§7 검증 때 보낸 값과 같아야 한다** — 다르면 검증 화면의 오류/`newMasters` 와 등록 결과가 어긋난다 |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.totalRows` | int | 요청 건수 |
| `data.registeredCount` | int | 성공 건수 |
| `data.failedCount` | int | 실패 건수 |
| `data.errors[]` | List\<Object\> | 검증과 같은 구조 |
| `data.emailSentCount` | int | 초기 비밀번호 메일 발송 성공 건수 |
| `data.emailNotRegistered[]` | List\<Object\> | `userId` · `name` (`EMP-019`) |
| `data.createdMasters.majors[]` | List\<Object\> | **이번 등록으로 새로 생성했거나 동명 마스터를 재사용한 전공** `{name, rowCount}`. `autoCreateMasters=false` 면 빈 배열 |
| `data.createdMasters.certificates[]` | List\<Object\> | 새로 생성했거나 동명 재사용한 자격증 `{name, rowCount}` |

> ⭐ **마스터 생성 규칙 (`autoCreateMasters=true`)**
>
> | 항목 | 규칙 |
> |---|---|
> | 시점 | 사원 행 등록 **전에** 한꺼번에 생성한다. `skipErrors=false` 인데 오류 행이 있으면 `EMP_HAS_ERRORS` 로 끝나며 **마스터도 생성되지 않는다** |
> | 대상 | §7 `newMasters` 와 동일 — 오류 없는 행이 참조하는, 목록에 없는 이름. 파일 안 여러 행이 같은 이름을 써도 1건만 |
> | 이름 규칙 | 마스터 이름 규칙(`qualification.md` 공통 원칙 — 100자 · `,` `;` `:` · 줄바꿈 금지)을 그대로 따른다. 위반은 §7 에서 이미 `REQUIRED_COLUMN` 행 오류다 |
> | 동명 처리 | 생성 직전 같은 이름이 이미 있으면(다른 관리자가 먼저 만든 경우·대소문자만 다른 경우) 새로 만들지 않고 **그 마스터를 참조**한다. `createdMasters` 에는 그대로 표시된다 |
> | 사원 행 실패 시 | 마스터는 그대로 남는다(사원과 독립 생명주기 — 필요 없으면 마스터 관리 화면에서 삭제, 참조 없으면 삭제 가능) |

> 화면 ③ 결과의 카드 4개가 `totalRows` · `registeredCount` · `failedCount` · `emailNotRegistered.length` 다.

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 처리 완료 |
| 400 | `EMP_FILE_REQUIRED` / `EMP_FILE_TYPE_INVALID` / `EMP_FILE_SIZE_EXCEEDED` | |
| 400 | `EMP_HAS_ERRORS` | `skipErrors=false` 인데 오류 행이 있음 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |

---

## 9. 사원 후보 검색 (결재선·참여자 지정용)

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/employees/search` |
| 인증 필요 | Y · **로그인 사용자 누구나** (ADMIN 전용 아님) |
| 요구사항 | EMP-020 · USC-EMP-015 |
| 요청 출처 | 결재선·참여자 지정 화면 — 이름으로 검색하거나, **이름을 모를 때 부서로 후보를 펼친다** |

> ⚠️ **인사관리용 목록(`GET /api/v1/employees`, ADMIN)과 권한·용도가 다르다.**
> 이건 후보 조회라 **로그인한 사용자 누구나** 호출한다. `SecurityConfig` 에서 두 경로의 권한을 분리한다.

**Request Parameter** — `name` · `departmentId` 중 **하나 이상 필수** (둘 다 없으면 400). 둘 다 주면 AND.

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `name` | String | N\* | 이름 부분 일치 검색어. 공백뿐이면 없는 것으로 본다 |
| `departmentId` | Long | N\* | 부서 필터. 이름을 모를 때 이 부서 재직자를 후보로 펼친다 |

> \* **name·departmentId 중 최소 하나는 있어야 한다** (2026-08-17, A안 — 이름 없이도 부서로 후보를 펼치기 위해 name 을 선택값으로 열었다).

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data[].userId` | String | 사번 |
| `data[].name` | String | 이름 |
| `data[].department` | String | 부서명 (동명이인 구분용, `null` 허용) |
| `data[].position` | String | 직급명 (동명이인 구분용, `null` 허용) |
| `data[].profileImageUrl` | String | 아바타 서빙 경로(§10). 사진 없으면 `null` — presigned 아님, 결재선 후보 아바타용 |

> 배열(후보 목록)로 내려준다. **급여 등 민감 정보는 포함하지 않는다** — 위 필드는 후보 식별·아바타 표시에 필요한 최소치다.
> ⛔ **시스템 계정(`is_system=1`)과 퇴사자는 후보에 나오지 않는다** — `is_system=0` · 재직자만 (`EMP-003`).

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 (결과 없으면 빈 배열) |
| 400 | `EMP_INVALID_PARAMETER` | `name`·`departmentId` 둘 다 누락 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |

---

## 10. 프로필 사진 조회 (아바타 서빙)

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/employees/{userId}/profile-image` |
| 인증 필요 | Y · **로그인 사용자 누구나** (ADMIN 전용 아님) |
| 요구사항 | 프로필 사진 아바타 — 요구사항 번호 미부여 (2026-08-11 설계) |
| 요청 출처 | 좌상단 프로필·프로젝트 멤버 동그라미·결재선 아바타 등 사진이 보이는 모든 화면 |

> ⭐ **왜 employee 도메인인가** — 사진은 사원 속성(`employee.profile_image_key`)이고, **남의 아바타**를 그려야 하는 화면이 많다. 업로드/삭제는 본인만이라 auth 마이페이지(`auth.md` §5-1·§5-2)에 두지만, **뿌리는 URL 은 사원 단위**라 여기에 둔다. 프론트는 목록/카드 응답의 `profileImageUrl`(값 = 이 경로)을 `<img src>` 에 그대로 박으면 된다.

**Path Parameter** — `userId` String Y (사번)

**Request Body**: 없음

**Response**: JSON 이 아니라 presigned S3 URL 로 **`302` redirect** 한다(트래픽이 S3 로 직행 → 서버 대역폭·부하 최소화). presigned 유효기간이 짧아도 매 요청 새로 발급하므로 이 API 는 항상 살아있는 안정 URL 이다.

```text
HTTP/1.1 302 Found
Location: <presigned S3 URL>
Cache-Control: max-age=300, private
```

> ⚠️ **`profileImageUrl` 은 이 경로이지 presigned URL 이 아니다.** 이미지 블록(`image.md`)은 presigned URL 을 JSON 에 직접 담지만, 아바타는 목록마다 수십 개가 반복 노출되고 거의 안 바뀌므로 그 방식이 맞지 않는다. 대신 **안 만료되는 우리 경로**를 내려주고, 만료·재서명은 이 서빙 API 가 내부에서 책임진다.
> **캐시 전략 (2026-08-17 변경)** — 302 redirect 를 **`max-age=300`(5분)** 로만 캐시한다. 목록에 아바타가 반복 노출돼 매 새로고침마다 왕복이 겹치던 깜빡임을 줄이되, presigned 만료(1시간)보다 짧아 만료된 URL 이 캐시에 남지 않는다. 사진 교체는 **최대 5분 뒤** 반영된다(즉시 아님 — 이전엔 `no-store` 라 즉시였다). private 라 공유 캐시(프록시/CDN)엔 저장되지 않는다.

**Status Code**

| 코드 | code | 설명 |
|---|---|---|
| 302 | – | presigned URL 로 redirect (`Location` 헤더, 성공) |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 404 | `EMP_PROFILE_IMAGE_NOT_FOUND` | 해당 사원의 프로필 사진이 없음 |
| 404 | `EMP_NOT_FOUND` | 사원 없음 |

> 📌 **프론트는 `profileImageUrl` 이 `null` 인 사원에는 이 API 를 부르지 않는다**(이니셜/기본 아바타를 그린다). 따라서 정상 흐름에서 `EMP_PROFILE_IMAGE_NOT_FOUND` 는 사진 삭제와 조회 사이의 경합 정도에서만 난다.

---

## 📌 스키마 요청 (마이그레이션 담당자에게 전달)

- 🔴 **`employee.profile_image_key` 컬럼 추가** (`VARCHAR`, `NULL` 허용) — 프로필 사진의 S3 키를 저장한다. 값이 없으면 사진 없음. Flyway 마이그레이션 필요.
