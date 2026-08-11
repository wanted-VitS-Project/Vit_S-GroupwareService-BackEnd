# 🔐 AUTH API

**최종 업데이트**: 2026-08-11 (프로필 사진 API 추가 — `me` 업로드/삭제·`me` 응답 `profileImageUrl`, 본인만) · 2026-08-04 (약관 동의 API·게이트 추가 — 최초 로그인 전용·ADMIN 스킵·`termsStatus`) · 2026-08-03 (에러코드 3종 확정) · **담당**: 김동현 · Domain `인사` · SUB-Domain `AUTH`

> 이 파일의 명세가 프론트와의 계약이다. 경로·필드명·타입·상태코드·에러코드를 **한 글자도 바꾸지 않는다** (`../API.md` §0).
> 변경이 필요하면 코드를 먼저 고치지 말고 **이 md 를 먼저 고친 뒤** 팀에 공유한다.

## 공통

| 항목 | 값 |
|------|-----|
| 응답 래퍼 | `{ httpStatus, message, data }` · 실패 `{ httpStatus, message, code }` |
| 에러 코드 | `AUTH_{의미}` — 번호식 아님 |
| 인증 | 세션 쿠키 (Redis) |
| 쿠키 | `SESSION` · `HttpOnly` · `SameSite=Lax` · `Secure`(운영) |
| 세션 타임아웃 | **4시간 유휴 · 슬라이딩** (2026-08-03 확정 · §6) |
| 로그인 실패 잠금 | **5회 / 10분 → 10분 잠금** (2026-08-03 확정 · §6) |

> 📌 **프론트가 할 일은 `credentials: 'include'` 하나뿐이다.** 토큰을 저장·갱신하지 않는다.
> 쿠키는 `HttpOnly` 라 JS 가 읽을 수 없고, 읽을 필요도 없다.
> **토큰 재발급 API 는 없다** — 세션은 요청이 있을 때마다 자동 연장된다.

## 엔드포인트

| API명칭 | METHOD | URL | 권한 |
|---|---|---|---|
| 로그인 | POST | `/api/v1/auth/login` | *(비움 · 인증 불필요)* |
| 로그아웃 | POST | `/api/v1/auth/logout` | 전체 사용자 |
| 내 정보 조회 | GET | `/api/v1/auth/me` | 전체 사용자 |
| 비밀번호 변경 | PATCH | `/api/v1/auth/password` | 전체 사용자 (본인만) |
| 약관 동의 | POST | `/api/v1/auth/terms-agreements` | 전체 사용자 (본인만, 최초 로그인 전용) |
| 프로필 사진 등록/변경 | PUT | `/api/v1/auth/me/profile-image` | 전체 사용자 (본인만) |
| 프로필 사진 삭제 | DELETE | `/api/v1/auth/me/profile-image` | 전체 사용자 (본인만) |

> 🖼️ **프로필 사진은 본인만 바꾼다** (2026-08-11 설계). 관리자가 남의 사진을 바꾸는 경로는 없다.
> 업로드/삭제는 여기(마이페이지), **아바타를 뿌리는 서빙 API 는 employee 도메인**(`GET /api/v1/employees/{userId}/profile-image`, `employee.md` §10)에 있다 — 좌상단·프로젝트 멤버 동그라미·결재선 아바타가 전부 그 URL 하나를 공유한다.

---

## 1. 로그인

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/auth/login` |
| 인증 필요 | N |
| 요구사항 | AUTH-001~004 · AUTH-007 · AUTH-008 · AUTH-010 · ACC-003 · ACC-006 · USC-AUTH-001~009 |

⛔ **비밀번호 찾기 API 가 없다.** 재설정은 ADMIN 이 수행한다 (`ACC-007`).
✅ **로그인 실패 잠금 = 5회 / 10분 → 10분 잠금** (`AUTH-005` · 2026-08-03 확정 · 근거 §6)

**Request Body**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `userId` | String | Y | 사번. 로그인 아이디로 사용 (`ACC-003`) |
| `password` | String | Y | 비밀번호 |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.userId` | String | 사번 (`NOT NULL`) |
| `data.name` | String | 이름 (`NOT NULL`) |
| `data.role` | String | **서열형** `ADMIN` > `MASTER` > `MEMBER` (`global/PERMISSION.md` §2) |
| `data.termsStatus` | String | `AGREED` · `REQUIRED`. `REQUIRED` 면 **약관 동의 전까지 다른 기능 사용 불가**(비밀번호 변경보다 먼저). ADMIN 은 항상 `AGREED`(스킵) |
| `data.passwordStatus` | String | `NORMAL` · `RESET_REQUIRED`. `RESET_REQUIRED` 면 변경 전까지 다른 기능 사용 불가 (`ACC-006`) |
| `data.departmentName` | String | 부서명 (`null` 허용) |
| `data.departmentPath` | String | `기술본부 / 개발팀` (`null` 허용) |
| `data.jobPositionName` | String | 직급명 (`null` 허용) |

> 🔑 **프론트 라우팅 순서** — `termsStatus=REQUIRED` 면 **약관 페이지** → 동의 후 `passwordStatus=RESET_REQUIRED` 면 **비밀번호 변경 페이지** → 둘 다 끝나면 정상. 약관은 **최초 로그인에만**, 비밀번호 변경은 **최초·재설정 모두** 발생한다.

```json
{ "httpStatus": 200, "message": "로그인 성공",
  "data": { "userId": "EMP001", "name": "김민준", "role": "MEMBER",
    "termsStatus": "REQUIRED", "passwordStatus": "RESET_REQUIRED", "departmentName": "개발팀",
    "departmentPath": "기술본부 / 개발팀", "jobPositionName": "대리" } }
```

**Status Code**

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 로그인 성공 |
| 400 | `AUTH_INVALID_REQUEST` | 사번 또는 비밀번호가 비어 있음 |
| 401 | `AUTH_LOGIN_FAILED` | 사번 또는 비밀번호 불일치. **사번 존재 여부를 구분하지 않는다** (`AUTH-003`) |
| 403 | `AUTH_ACCOUNT_INACTIVE` | 계정이 비활성 |
| 423 | `AUTH_ACCOUNT_LOCKED` | 실패 누적 잠금. `message` 에 해제 시각을 담는다 |
| 429 | `AUTH_TOO_MANY_REQUESTS` | 같은 IP 에서 요청 과다. 잠시 후 재시도 |
| 503 | `AUTH_HASHING_BUSY` | 서버 과부하로 처리 못 함. 요청 자체는 정상 — 잠시 후 재시도 |

> **`423` 과 `403` 을 합치지 마라** (2026-08-03 확정). 프론트 처리가 다르다 —
> `403 AUTH_ACCOUNT_INACTIVE` 는 관리자만 풀 수 있고, `423 AUTH_ACCOUNT_LOCKED` 는 시간이 지나면 자동 해제된다.
> 같은 코드로 내리면 화면이 두 상황을 구분하지 못한다.

**`429` · `503` 은 2026-08-03 에 추가·확정한 코드다.**
비밀번호 해시(Argon2id)가 요청당 64MB 를 쓰기 때문에 **동시 실행을 제한하지 않으면 서버가 죽는다.**
막힌 요청을 되돌려보내는 경로가 필요하다. 근거는 §6.

> ⚠️ **`Retry-After` 헤더는 내려주지 않는다.** 재시도 시점을 클라이언트가 헤더로 읽는 시나리오가 없어
> 명세에서 뺐다 (2026-08-03). 프론트는 고정 지연 후 재시도하거나 사용자에게 안내만 한다.

---

## 2. 로그아웃

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/auth/logout` |
| 인증 필요 | Y · 전체 사용자 |
| 요구사항 | AUTH-009 · USC-AUTH-010 |

Request Body 없음 · `data` 는 `null`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 로그아웃 성공 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |

---

## 3. 내 정보 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/auth/me` |
| 인증 필요 | Y · 전체 사용자 |
| 요구사항 | AUTH-008 · ACC-006 · USC-AUTH-008 |

⛔ **연차 정보는 응답에 없다.** 근태는 범위 미정.

**Response** — 마이페이지 화면이 쓰는 필드 전체

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.userId` | String | 사번 (`NOT NULL`) |
| `data.name` | String | 이름 (`NOT NULL`) |
| `data.role` | String | `ADMIN` · `MASTER` · `MEMBER` |
| `data.termsStatus` | String | `AGREED` · `REQUIRED` (ADMIN 은 항상 `AGREED`) |
| `data.passwordStatus` | String | `NORMAL` · `RESET_REQUIRED` |
| `data.email` | String | 이메일 (`null` 허용) |
| `data.phone` | String | 연락처 (`null` 허용) |
| `data.departmentName` | String | 부서명 (`null` 허용) |
| `data.departmentPath` | String | 2단 표시용 (`null` 허용) |
| `data.jobPositionName` | String | 직급명 (`null` 허용) |
| `data.hiredAt` | String | 입사일 `yyyy-MM-dd` (`null` 허용) |
| `data.lastLoginAt` | String | 마지막 로그인 `yyyy-MM-dd HH:mm:ss` (`null` 허용) |
| `data.profileImageUrl` | String | 프로필 사진 URL (`null` 허용). 값은 `/api/v1/employees/{userId}/profile-image`, 사진이 없으면 `null` → 프론트가 이니셜/기본 아바타를 그린다 |

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |

---

## 4. 비밀번호 변경

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/auth/password` |
| 인증 필요 | Y · 전체 사용자 (본인만) |
| 요구사항 | ACC-005 · ACC-006 · ACC-008 · USC-ACC-005 · USC-ACC-006 |

⛔ **다른 사람의 비밀번호는 이 API 로 바꿀 수 없다.** ADMIN 의 재설정은 `POST /api/v1/accounts/password-resets` (`ACC-007`).

⭐ **비밀번호 정책 — 3개 모두 필수** (2026-08-03 확정)

| 조건 | 필수 |
|---|:---:|
| 8자 이상 | ✅ |
| 영문·숫자 포함 | ✅ |
| **특수문자 포함** | ✅ |

⭐ **`currentPassword` 는 상황에 따라 생략된다**

| 상황 | `currentPassword` |
|---|---|
| 최초 변경 (`passwordStatus = RESET_REQUIRED`) | **생략.** 이미 임시 비밀번호로 인증해 세션이 있다 |
| 마이페이지에서 일반 변경 | **필수** |

**Request Body**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `currentPassword` | String | 조건부 | 위 표 참조 |
| `newPassword` | String | Y | 새 비밀번호 |
| `newPasswordConfirm` | String | Y | 확인. 서버에서도 일치를 검증 |

`data` 는 `null`. 성공하면 `passwordStatus` 가 `NORMAL` 로 바뀐다.

**Status Code**

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 변경 성공 |
| 400 | `AUTH_INVALID_REQUEST` | 필수값 누락 |
| 400 | `AUTH_CURRENT_PASSWORD_REQUIRED` | 일반 변경인데 현재 비밀번호를 생략함 |
| 400 | `AUTH_CURRENT_PASSWORD_INVALID` | 현재 비밀번호 불일치 |
| 400 | `AUTH_PASSWORD_CONFIRM_MISMATCH` | 새 비밀번호와 확인이 다름 |
| 400 | `AUTH_PASSWORD_POLICY_VIOLATION` | 정책 3개 중 하나라도 위반 |
| 400 | `AUTH_PASSWORD_UNCHANGED` | 새 비밀번호가 현재와 같음 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |

> 🔑 **현재 비밀번호 오류를 `401` 이 아니라 `400` 으로 둔 이유** — 세션은 유효하다. `401` 을 주면 프론트 공통 인터셉터가 세션 만료로 보고 **로그아웃 처리**를 해버린다.

---

## 5. 약관 동의 (최초 로그인 전용)

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/auth/terms-agreements` |
| 인증 필요 | Y · 전체 사용자 (본인) |
| 요구사항 | 최초 로그인 시 이용약관·개인정보처리방침 동의 (1회성) |

⭐ **최초 로그인에만 뜬다.** 임시 비밀번호로 로그인한 상태에서 **약관 동의 → 그다음 비밀번호 변경** 순으로 진행한다.
⛔ **비밀번호 재설정 후 로그인은 약관을 다시 받지 않는다.** 동의는 사람당 1회이며, 재설정은 비밀번호만 다시 바꾸게 한다.
⛔ **ADMIN 은 대상이 아니다** — 공용 계정이라 약관 동의를 받지 않는다(`termsStatus` 항상 `AGREED`).
⛔ **약관은 하나로 묶는다** — 이용약관·개인정보처리방침을 분리 동의로 받지 않는다(필요 시 상용화 단계에서 분리). 동의 시각만 기록한다(`account.terms_agreed_at`).

**Request Body 없음** (POST 호출 자체가 동의다) · `data` 는 `null`. 성공하면 `termsStatus` 가 `AGREED` 로 바뀐다.

> 재호출해도 무해하다(멱등) — 이미 동의한 사용자가 다시 호출하면 동의 시각만 갱신된다.

**Status Code**

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 동의 완료 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |

> **약관 게이트** — `termsStatus=REQUIRED` 상태에서 이 API·`/auth/logout`·`/auth/me` 를 제외한 모든 요청은 `403 AUTH_TERMS_AGREEMENT_REQUIRED` 로 막힌다. **비밀번호 변경(`/auth/password`)보다 먼저**다. 상세는 §6-7.

---

## 5-1. 프로필 사진 등록/변경

| 항목 | 내용 |
|------|------|
| Method · URL | `PUT /api/v1/auth/me/profile-image` |
| 인증 필요 | Y · 전체 사용자 (본인만) |
| Content-Type | `multipart/form-data` |
| 요구사항 | 마이페이지 프로필 사진 — 요구사항 번호 미부여 (2026-08-11 설계) |

⛔ **본인 사진만 바꾼다.** 남의 사진을 바꾸는 경로는 없다(경로 자체가 `me`).
⛔ **덮어쓰기다(멱등).** 이미 사진이 있으면 교체된다. 신규·교체 모두 `200` 이며, 별도 생성 코드(`201`)를 쓰지 않는다.

**Request Body (multipart)**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `file` | File | Y | 이미지 파일 1장 |

> **검증** — 확장자 화이트리스트(`jpg`·`jpeg`·`png`·`gif`·`webp`) + 매직바이트 + `ImageIO` 디코딩까지 확인한다(이미지 도메인의 업로드 검증과 동일 원칙, `image.md` §S3 저장 정책 / 생성 API 콜아웃 참고 — 확장자만 바꾼 위장 파일 차단). **webp 는 JDK 내장 디코더가 없어 매직바이트까지만** 검사한다(이미지 도메인과 동일한 알려진 제한).
> **용량 상한 5MB.** 초과 시 `400 AUTH_PROFILE_IMAGE_SIZE_EXCEEDED`.
> **저장** — S3 키 `profile-images/{userId}/{UUID}.{ext}`, DB `employee.profile_image_key` 에는 **키만** 저장한다(이미지 도메인과 동일 — 응답·서빙 시점에 처리). 아바타라 정사각 썸네일(예: 512px)로 축소 저장하는 것을 권장한다(원본 보존 불필요).

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.profileImageUrl` | String | 저장된 프로필 사진 URL — `/api/v1/employees/{userId}/profile-image` |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "프로필 사진 등록 성공",
  "data": { "profileImageUrl": "/api/v1/employees/EMP001/profile-image" }
}
```

**Status Code**

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 등록/변경 성공 |
| 400 | `EMP_PROFILE_IMAGE_REQUIRED` | 파일이 없음 |
| 400 | `EMP_PROFILE_IMAGE_TYPE_INVALID` | 지원하지 않는 형식 또는 위장 파일 |
| 400 | `EMP_PROFILE_IMAGE_SIZE_EXCEEDED` | 5MB 초과 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |

> 📌 **에러 접두어가 `EMP_` 인 이유** — 경로는 `/auth/me` 지만 프로필 사진은 **사원 속성**이라 구현·에러코드를 employee 도메인이 소유한다(서빙 API 도 employee). 프론트는 코드 문자열로 분기하므로 접두어가 `EMP_` 여도 무방하다.

> ⚠️ **교체 시 이전 S3 객체는 지우지 않는다**(2026-08-11, 이미지 도메인 소프트 정책과 통일). 아바타는 복구 기능이 없어 남길 이유가 약하지만, 하드 삭제 정책이 팀에서 정해지기 전까지는 도메인 간 정책을 통일해 둔다 — 쌓인 고아 객체 정리는 백로그(`.ai/local/STATE.md`).

---

## 5-2. 프로필 사진 삭제

| 항목 | 내용 |
|------|------|
| Method · URL | `DELETE /api/v1/auth/me/profile-image` |
| 인증 필요 | Y · 전체 사용자 (본인만) |
| 요구사항 | 마이페이지 프로필 사진 — 요구사항 번호 미부여 (2026-08-11 설계) |

`employee.profile_image_key` 를 `null` 로 만든다. 프론트는 이후 `profileImageUrl: null` 을 받아 이니셜/기본 아바타를 그린다.

⛔ **사진이 없어도 `200` 이다(멱등).** "지울 게 없음"을 에러로 취급하지 않는다.
> ⚠️ **S3 객체는 지우지 않는다** — §5-1 교체와 동일 원칙.

**Request Body 없음** · `data` 는 `null`.

**Status Code**

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 삭제 성공 (사진이 없었어도 성공) |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |

---

## 6. 인증 방식 · 보안 파라미터 (2026-08-03 확정)

> 엔드포인트 명세는 아니지만 **프론트 동작과 에러 분기에 영향**을 주므로 여기 남긴다.
> 상세 근거·실측 데이터: `.ai/local/STATE.md` `🔐 인증·보안 확정값`

### 6-1. 왜 JWT 가 아니라 세션인가

| 요구 | 세션 | JWT |
|---|---|---|
| ADMIN 이 role·페이지 권한을 바꾸면 **즉시 반영** | ✅ Redis 세션 값 갱신 | ❌ 클레임이 발급 시점 스냅샷 → 재로그인 강제 |
| 계정 잠금·비활성화 시 **기존 접속 즉시 차단** | ✅ 세션 삭제 | ❌ 만료까지 유효 (블랙리스트 = 결국 세션) |
| 프론트 부담 | 없음 | `401 → 재발급 → 재시도` 인터셉터 |

> 매 요청 서버 상태를 봐야 하는 요구라 JWT 의 무상태 이점이 성립하지 않는다.
> 그래서 **재발급 API 를 만들지 않는다.**

### 6-2. 세션

| 항목 | 값 | 근거 |
|---|---|---|
| 저장소 | Redis (Spring Session · `indexed`) | 사용자별 세션 조회가 필요 (단일 세션 · 즉시 무효화) |
| 타임아웃 | **4시간 유휴 · 슬라이딩** | 점심·회의로 자리를 비워도 재로그인하지 않고, 퇴근 후 방치된 세션은 그날 안에 죽는다 |
| 동시 세션 | **1개** — 새 로그인이 이기고 기존 세션이 끊긴다 | |
| 세션 고정 방어 | 로그인 성공 시 세션 ID 교체 | |

### 6-3. 비밀번호 해시 — Argon2id

| 항목 | 값 |
|---|---|
| 파라미터 | `m=64MB · t=3 · p=1 · salt 16 · hash 32` |
| 동시 실행 | **최대 2건** (초과 시 최대 8초 대기 → `503 AUTH_HASHING_BUSY`) |

**프론트가 알아야 할 것** — 로그인 응답이 **0.3~0.5초** 걸린다. 해시 자체가 느린 게 정상이므로
로딩 표시가 필요하고, 타임아웃을 너무 짧게 잡으면 안 된다.

### 6-4. 레이트리밋

| 층 | 한도 | 초과 시 |
|---|---|---|
| 계정 단위 실패 | **5회 / 10분** → 10분 잠금 | `423 AUTH_ACCOUNT_LOCKED` |
| IP 단위 요청 | **60회 / 분** | `429 AUTH_TOO_MANY_REQUESTS` |

> ⚠️ IP 한도를 인원(30명)의 2배로 잡은 이유 — **사무실이 NAT 하나를 공유**한다.
> 아침에 25명이 동시 로그인하면 한 IP 에서 30회가 넘게 나온다. 한도를 인원과 같게 두면 정상 사용자가 막힌다.

### 6-5. 사번 존재 여부를 숨기는 방법

`AUTH-003` 을 지키려면 **없는 사번에도 더미 해시를 돌려야** 한다. 즉시 401 을 주면
응답 시간 차이(0.4초 vs 1ms)로 사번 존재 여부가 새어 계정 열거가 가능하다.

> 부작용: 미인증 공격자도 서버 메모리를 태울 수 있다 → 6-4 의 IP 레이트리밋이 **선택이 아니라 필수**다.

### 6-6. `RESET_REQUIRED` 게이트 (2026-08-03 구현)

`ACC-006` 의 *"변경 전까지 다른 기능 사용 불가"* 를 **서버에서** 막는다. 프론트가 화면만 가리면 API 를 직접 호출해 뚫린다.

| 항목 | 값 |
|---|---|
| 응답 | `403` · **`AUTH_PASSWORD_RESET_REQUIRED`** (2026-08-03 추가·확정) |
| 예외 경로 | `PATCH /api/v1/auth/password` · `POST /api/v1/auth/logout` · `GET /api/v1/auth/me` · `POST /api/v1/auth/terms-agreements`(약관 게이트와 동시 활성 시 통과 필요, §6-7) |
| 판정 | 세션 속성. 매 요청 DB 를 치지 않는다 |
| 해제 | 비밀번호 변경 성공 시. **세션은 유지**한다 (재로그인시키지 않는다) |

### 6-7. 약관 동의 게이트 (2026-08-04 구현)

최초 로그인 시 **약관 동의를 비밀번호 변경보다 먼저** 강제한다. RESET_REQUIRED 게이트와 같은 방식(세션 속성)이며, **약관 게이트가 비번 게이트보다 앞**에 선다.

| 항목 | 값 |
|---|---|
| 응답 | `403` · **`AUTH_TERMS_AGREEMENT_REQUIRED`** (2026-08-04 추가·확정) |
| 예외 경로 | `POST /api/v1/auth/terms-agreements` · `POST /api/v1/auth/logout` · `GET /api/v1/auth/me` |
| 판정 | 세션 속성 `TERMS_AGREEMENT_REQUIRED`. `account.terms_agreed_at IS NULL` 이고 **ADMIN 이 아닐 때** 로그인 시 세운다 |
| 해제 | 약관 동의(`POST /auth/terms-agreements`) 성공 시. 세션 유지 |
| ADMIN | 게이트 대상 아님 — 로그인 시 애초에 플래그를 세우지 않는다 |

> ⚠️ **두 게이트가 동시에 켜지는 최초 로그인**에서, 약관 동의 엔드포인트는 **비번 게이트의 예외 경로에도 포함**돼야 한다(둘 다 통과해야 하므로). 그래서 `RESET_REQUIRED` 게이트의 예외 경로에도 `/auth/terms-agreements` 를 넣는다. 순서상 약관 게이트가 먼저라 `/auth/password` 는 약관 전엔 막힌다(약관 → 비번 강제).

## 미확정

- [x] `2026-08-03` 추가 코드 3개 확정 — `429 AUTH_TOO_MANY_REQUESTS` · `503 AUTH_HASHING_BUSY` · `403 AUTH_PASSWORD_RESET_REQUIRED`. `Retry-After` 는 명세에서 제외
- [ ] 비밀번호 변경 성공 시 세션을 끊을지 유지할지 (보안 관례는 재로그인 강제 / 현재 구현은 유지)
- [ ] 🔴 **스키마 요청** — `department.parent_id` 가 있어야 `departmentPath` 를 만들 수 있다 (마이그레이션 담당자에게 전달)
