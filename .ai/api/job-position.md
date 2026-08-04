# 🎖️ JobPosition API

**최종 업데이트**: 2026-08-03 · **담당**: 김동현 · Domain `인사` · SUB-Domain `JobPosition`

> 이 파일의 명세가 프론트와의 계약이다. 경로·필드명·타입·상태코드·에러코드를 **한 글자도 바꾸지 않는다** (`../API.md` §0).
> 변경이 필요하면 코드를 먼저 고치지 말고 **이 md 를 먼저 고친 뒤** 팀에 공유한다.
> 🔴 **화면이 없다.** 직급 관리 화면이 와이어프레임에 없어 프론트에 요청한 상태다 (`ADD-01`). 사원 등록의 직급 드롭다운 값이 여기서 나오므로 **화면이 없으면 사원 등록이 막힌다.**

## 엔드포인트

| API명칭 | METHOD | URL | 권한 |
|---|---|---|---|
| 직급 목록 조회 | GET | `/api/v1/job-positions` | ADMIN |
| 직급 생성 | POST | `/api/v1/job-positions` | ADMIN |
| 직급 수정 | PATCH | `/api/v1/job-positions/{jobPositionId}` | ADMIN |
| 직급 삭제 | DELETE | `/api/v1/job-positions/{jobPositionId}` | ADMIN |

> 목록 조회도 `ADMIN` 이다. 직급 목록이 필요한 화면(사원 등록·수정·직급 관리)이 전부 ADMIN 전용이라 굳이 열어둘 이유가 없다.

⛔ **직급은 권한 판정에 사용하지 않는다** (`POS-009`). 부서와 같은 원칙 — 직급이 생기고 없어져도 권한이 흔들리지 않는다.

---

## 1. 직급 목록 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/job-positions` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | POS-001 · POS-002 · USC-POS-001 · USC-POS-002 |

⛔ **페이징이 없다.** 회사당 수십 개 수준.
⛔ **정렬은 `sortOrder` 오름차순, 같으면 직급명 오름차순.** `sortOrder` 에 UNIQUE 를 걸지 않아 값이 겹칠 수 있다.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.content[].jobPositionId` | Long | 직급 번호 (`NOT NULL`) |
| `data.content[].name` | String | 직급명 (`NOT NULL`) |
| `data.content[].sortOrder` | int | 정렬 순서 |
| `data.content[].employeeCount` | int | 사용 인원. 시스템 계정 제외 (`POS-002`) |

```json
{ "httpStatus": 200, "message": "직급 목록 조회 성공",
  "data": { "content": [
    { "jobPositionId": 1, "name": "사원", "sortOrder": 1, "employeeCount": 14 },
    { "jobPositionId": 2, "name": "대리", "sortOrder": 2, "employeeCount": 6 }
  ] } }
```

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |

---

## 2. 직급 생성

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/job-positions` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | POS-003 · POS-004 · POS-006 · USC-POS-003 · USC-POS-004 |

⛔ **직급명은 중복될 수 없다.** DB 에 UNIQUE 제약 (`POS-004`).

**Request Body**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `name` | String | Y | 직급명. 최대 30자 |
| `sortOrder` | int | N | 생략하면 마지막 순서 + 1 |

**Response** — `jobPositionId` · `name` · `sortOrder` · `employeeCount`(0)

| 코드 | code | 설명 |
|---|---|---|
| 201 | – | 생성 성공 |
| 400 | `POS_INVALID_REQUEST` | 직급명이 비었거나 30자 초과 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |
| 409 | `POS_NAME_DUPLICATED` | 이미 존재하는 직급명 |

---

## 3. 직급 수정

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/job-positions/{jobPositionId}` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | POS-005 · POS-006 · USC-POS-005 · USC-POS-006 |

⛔ **직급명 수정과 순서 변경이 같은 API 다.** 화면 메뉴는 둘로 나뉘어 있지만 바꾸는 대상이 같은 리소스다.
⛔ **순서를 서로 맞바꿀 때 두 번 호출한다.** `sortOrder` 에 UNIQUE 가 없어 중간에 값이 겹쳐도 오류가 나지 않는다.

**Request Body** — 전달한 필드만 수정

| 파라미터 | 타입 | 필수 |
|---|---|:---:|
| `name` | String | N |
| `sortOrder` | int | N |

**Response** — 목록과 같은 구조

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 수정 성공 |
| 400 | `POS_INVALID_REQUEST` | 수정할 필드 없음 또는 형식 오류 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |
| 404 | `POS_NOT_FOUND` | 직급 없음 |
| 409 | `POS_NAME_DUPLICATED` | 이미 존재하는 직급명 |

---

## 4. 직급 삭제

| 항목 | 내용 |
|------|------|
| Method · URL | `DELETE /api/v1/job-positions/{jobPositionId}` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | POS-007 · POS-008 · USC-POS-007 · USC-POS-008 |

⛔ **사용 인원이 있으면 삭제되지 않는다.** 사원의 직급을 먼저 바꾸거나 비워야 한다 (`POS-008`).
⛔ **소프트 삭제가 아니다.** 권한 판정에 쓰이지 않고 이력에 스냅샷으로 남지도 않으므로 행을 제거한다.

> ℹ️ `file_version.uploader_position` 이 **스냅샷**이라 직급을 지워도 과거 업로드 이력의 직책 표시는 남는다.

`data` 는 `null`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 삭제 성공 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |
| 404 | `POS_NOT_FOUND` | 직급 없음 |
| 409 | `POS_IN_USE` | 사용 인원 있음. `message` 에 인원 수 |

## 요구사항 명세

| ID | 대분류 | 중분류 | 소분류 | 설명 |
|---|---|---|---|---|
| POS-001 | 직급 관리 | 직급 조회 | 직급 목록 조회 | ADMIN은 직급 목록을 조회할 수 있다. |
| POS-002 | 직급 관리 | 직급 조회 | 사용 인원 제공 | 시스템은 각 직급을 사용하는 사원 수를 제공해야 한다. |
| POS-003 | 직급 관리 | 직급 생성 | 직급 생성 | ADMIN은 직급을 생성할 수 있다. |
| POS-004 | 직급 관리 | 직급 생성 | 직급명 중복 차단 | 시스템은 이미 존재하는 직급명의 생성을 차단해야 한다. |
| POS-005 | 직급 관리 | 직급 수정 | 직급명 수정 | ADMIN은 직급명을 수정할 수 있다. |
| POS-006 | 직급 관리 | 직급 수정 | 정렬 순서 지정 | ADMIN은 직급의 표시 순서를 지정할 수 있다. |
| POS-007 | 직급 관리 | 직급 삭제 | 직급 삭제 | ADMIN은 직급을 삭제할 수 있다. |
| POS-008 | 직급 관리 | 직급 삭제 | 사용 중 삭제 차단 | 시스템은 사용 인원이 있는 직급의 삭제를 차단해야 한다. |
| POS-009 | 직급 관리 | 제약 | 권한 판정 미사용 | 시스템은 직급을 권한 판정에 사용하지 않아야 한다. |

## 유스케이스

| ID | 시나리오 | 사용자 |
|---|---|---|
| USC-POS-001 | 직급 목록 조회 | ADMIN |
| USC-POS-002 | 직급 사용 인원 집계 | 시스템 |
| USC-POS-003 | 직급 생성 | ADMIN |
| USC-POS-004 | 직급명 중복 검증 | 시스템 |
| USC-POS-005 | 직급명 수정 | ADMIN |
| USC-POS-006 | 정렬 순서 변경 | ADMIN |
| USC-POS-007 | 직급 삭제 | ADMIN |
| USC-POS-008 | 사용 중 직급 삭제 차단 | 시스템 |
