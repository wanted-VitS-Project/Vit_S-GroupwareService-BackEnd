# 🔑 Account API

**상태**: `✅ 확정` — 노션 반영 완료 (2026-08-03). 이탈 금지 규칙 전면 적용 (`../API.md` §0)
**최종 업데이트**: 2026-08-03 · **담당**: 김동현
**노션**: `VitaSAPI` · Domain `인사` · SUB-Domain `Account`

> ✅ **노션 반영 완료 — 구현 가능.** 경로·필드명·타입·상태코드·에러코드를 **한 글자도 바꾸지 않는다** (`../API.md` §0).
> 변경이 필요하면 코드를 고치지 말고 **노션을 먼저 고친 뒤** 이 사본을 맞춘다.

## 엔드포인트

| API명칭 | METHOD | URL | 권한 |
|---|---|---|---|
| 전역 권한 변경 | PATCH | `/api/v1/accounts/{userId}/role` | ADMIN |
| 계정 상태 변경 | PATCH | `/api/v1/accounts/{userId}/status` | ADMIN |
| 비밀번호 재설정 | POST | `/api/v1/accounts/password-resets` | ADMIN |

> ⭐ **"재발송" API 를 만들지 않았다.** BCrypt 해시만 저장하므로 이전 임시 비밀번호의 원문이 없다.
> 재발송은 필연적으로 새 난수 발급 = 재설정이다. 화면의 `재발송` 버튼도 **재설정 API 를 다시 호출**한다 (`ACC-019`).

## 🔑 전역 role — 서열형 3종 (`global/PERMISSION.md` §2)

| role | 성격 | 프로젝트·페이지 |
|---|---|---|
| `ADMIN` | 조직 관리자 = **실권 최상위.** `MASTER` 가 할 수 있는 걸 다 할 수 있다 | **전부 뚫는다** |
| `MASTER` | CEO — 전사 열람·개입. **부여·회수는 못 한다** | **전부 뚫는다** (보고 고칠 수 있다) |
| `MEMBER` | 일반 사용자. 기본값 | 부여받은 것만 |

⭐ **`ADMIN` 은 이 API 로 부여할 수 없다** (2026-08-03 확정). **개발자가 계정을 직접 발급**하며, 필요하면 추가 생성해 제공한다. 즉 `role` 로 줄 수 있는 값은 `MASTER` · `MEMBER` 뿐이다.

⭐ **`ADMIN` 은 겸직하지 않는다 — 시스템 계정이다.** 실제 사원에게 부여하지 않으며 `employee.is_system = 1` 인 가상 사원에 붙는다. 계정 수는 **복수 허용**이다 (회사당 1개 제한 없음).

> 🔴 `global/PERMISSION.md` §2-2 는 *"겸직은 표현하지 않는다. CEO 이면서 조직관리자가 필요하면 `ADMIN` 을 준다"* 고 적혀 있다. **이건 겸직을 ADMIN 부여로 푸는 방식이고 위 확정과 다르다.** 동훈에게 수정 요청 중이다.

⚠️ **`ADMIN`·`MASTER` 가 남의 프로젝트 데이터를 고치면 로그에 `privileged_override = 1` 을 표기한다** (`PERMISSION.md` §2-1).

---

## 1. 전역 권한 변경

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/accounts/{userId}/role` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | ACC-009 · USC-ACC-007 |

**Path Parameter** — `userId` String Y (사번)

⛔ **`ADMIN` 은 이 API 로 부여할 수 없다.** 개발자가 계정을 직접 발급한다 (`ACC-001` · `ACC-023`).
⛔ **자기 자신의 role 행은 수정할 수 없다.** 애플리케이션이 막는다 (`PERMISSION.md` §2-3).

**Request Body**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `role` | String | Y | `MASTER` · `MEMBER` |

**Response** — `data.userId` · `data.role`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 변경 성공 |
| 400 | `ACC_INVALID_ROLE` | 허용되지 않는 값 |
| 400 | `ACC_ADMIN_ROLE_NOT_ALLOWED` | `ADMIN` 을 부여하려 함 |
| 400 | `ACC_SELF_MODIFICATION_NOT_ALLOWED` | 자기 자신의 role 을 바꾸려 함 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 이 아님 |
| 403 | `ACC_SYSTEM_ACCOUNT_NOT_ALLOWED` | 시스템 계정은 대상 불가 (`EMP-003`) |
| 404 | `ACC_NOT_FOUND` | 계정 없음 |

---

## 2. 계정 상태 변경

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/accounts/{userId}/status` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | ACC-011 · ACC-012 · USC-ACC-008 · USC-ACC-009 |

**Path Parameter** — `userId` String Y

⛔ **퇴사 처리와 다른 API 다.** 퇴사는 `PATCH /api/v1/employees/{userId}/resignation` 이며, 퇴사 시 계정이 자동으로 비활성화되므로 이 API 를 따로 호출하지 않는다 (`EMP-016`).

**Request Body**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `status` | String | Y | `ACTIVE` · `INACTIVE` |

> 사원 상세의 **계정 상태 토글**이 이 API 다. 목록의 `재설정 필요` 배지는 `passwordStatus` 이며 이 API 와 무관하다.

**Response** — `data.userId` · `data.status`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 변경 성공 |
| 400 | `ACC_INVALID_STATUS` | 허용되지 않는 값 |
| 400 | `ACC_STATUS_UNCHANGED` | 이미 같은 상태 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 이 아님 |
| 403 | `ACC_SYSTEM_ACCOUNT_NOT_ALLOWED` | 시스템 계정은 대상 불가 |
| 404 | `ACC_NOT_FOUND` | 계정 없음 |

---

## 3. 비밀번호 재설정

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/accounts/password-resets` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | ACC-007 · ACC-013~020 · ACC-025 · USC-ACC-004 · USC-ACC-010~016 |

⛔ **개인 재설정과 다중 재설정이 같은 API 다.** 1명이면 배열 길이 1 (`ACC-007` · `ACC-013`).
⛔ **응답에 비밀번호가 없다.** 해시만 저장하며 화면에도 표시하지 않는다 (`ACC-020`).
⛔ **화면의 `재발송` 버튼도 이 API 를 호출한다** (`ACC-019`).
⛔ **`ADMIN` 계정은 대상이 될 수 없다.** 시스템 계정이라 이메일이 없고, 재발급은 개발자가 한다 (`ACC-023` · `ACC-024`).
⛔ **동기 처리.** 결과를 즉시 반환한다 (`ACC-025`).

> ℹ️ **마지막 ADMIN 보호 규칙(`PERMISSION.md` §2-3)은 별도 처리가 필요 없다.** ADMIN 은 시스템 계정이라 `ACC_SYSTEM_ACCOUNT_NOT_ALLOWED` 로 이미 모든 변경 API 에서 차단된다.

**Request Body**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `userIds` | List\<String\> | Y | 대상 사번 목록. 1개 이상 |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.requestedCount` | int | 요청 건수 (`ACC-017`) |
| `data.successCount` | int | 성공 건수 |
| `data.failedCount` | int | 실패 건수 |
| `data.failures[]` | List\<Object\> | 실패 목록 (`ACC-018`) |
| `data.failures[].userId` | String | 사번 |
| `data.failures[].name` | String | 이름 |
| `data.failures[].reason` | String | `EMAIL_NOT_REGISTERED` · `MAIL_SEND_FAILED` |
| `data.failures[].passwordChanged` | boolean | `EMAIL_NOT_REGISTERED` 는 `false`, `MAIL_SEND_FAILED` 는 `true` (`ACC-016`) |

> 🔑 **`passwordChanged` 가 핵심이다.** `false` 면 이메일을 등록한 뒤 다시 시도하면 되고,
> `true` 면 그 사원은 새 비밀번호를 모르는 상태라 **반드시 다시 호출해야** 한다.
> 이메일 미등록 대상의 비밀번호를 바꾸지 않는 이유가 이것이다 — 전달 못 할 비번을 바꾸면 로그인 불가가 된다.

```json
{ "httpStatus": 200, "message": "비밀번호 재설정 완료",
  "data": { "requestedCount": 3, "successCount": 1, "failedCount": 2,
    "failures": [
      { "userId": "EMP003", "name": "박지훈", "reason": "EMAIL_NOT_REGISTERED", "passwordChanged": false },
      { "userId": "EMP019", "name": "박지원", "reason": "MAIL_SEND_FAILED", "passwordChanged": true }
    ] } }
```

**Status Code**

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 처리 완료. **실패가 섞여 있어도 200** — 프론트가 집계를 보여줘야 하므로 |
| 400 | `ACC_INVALID_REQUEST` | `userIds` 가 비어 있음 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 이 아님 |
| 403 | `ACC_ADMIN_ACCOUNT_NOT_ALLOWED` | 대상에 ADMIN 계정 포함 |
| 404 | `ACC_NOT_FOUND` | 존재하지 않는 사번 포함. **전체를 거부한다** |

> **존재하지 않는 사번은 전체 거부다.** 화면에서 목록을 보고 선택하므로 올 수 없는 값이고, 부분 처리하면 원인을 숨긴다. 부분 실패를 허용하는 건 **메일 발송 단계**뿐이다.
