# 🔒 PagePermission API

**상태**: `✅ 확정` — 노션 반영 완료 (2026-08-03). 이탈 금지 규칙 전면 적용 (`../API.md` §0)
**최종 업데이트**: 2026-08-03 · **담당**: 김동현
**노션**: `VitaSAPI` · Domain `인사` · SUB-Domain `PagePermission`

> ✅ **노션 반영 완료 — 구현 가능.** 경로·필드명·타입·상태코드·에러코드를 **한 글자도 바꾸지 않는다** (`../API.md` §0).
> 변경이 필요하면 코드를 고치지 말고 **노션을 먼저 고친 뒤** 이 사본을 맞춘다.

## 엔드포인트

| API명칭 | METHOD | URL | 권한 |
|---|---|---|---|
| 내 페이지 목록 조회 | GET | `/api/v1/my/pages` | 전체 사용자 |
| 페이지 목록 조회 | GET | `/api/v1/pages` | ADMIN |
| 페이지 접근 가능자 목록 | GET | `/api/v1/pages/{pageCode}/permissions` | ADMIN |
| 페이지 권한 부여 | POST | `/api/v1/pages/{pageCode}/permissions` | ADMIN |
| 페이지 권한 회수 | DELETE | `/api/v1/pages/{pageCode}/permissions/{userId}` | ADMIN |

## 🔑 페이지는 고정 카탈로그다

| 항목 | 내용 |
|------|------|
| 생성 | **개발자가 코드로 제공한다.** 고객사(ADMIN 포함)는 생성·삭제할 수 없다 |
| 저장 | 페이지 자체는 DB 테이블이 없다. **`page_permission`(사람 × 페이지 × 등급)만 저장한다** |
| 하위 단위 | 없음. 페이지 권한 하나가 그 페이지의 모든 메뉴를 연다 |

⭐ **페이지 5개 확정** (2026-08-03 · 페이지 권한 화면 기준)

| pageCode | 페이지명 | 설명 |
|---|---|---|
| `PROJECT_MANAGEMENT` | 프로젝트 관리 | 진행 중인 프로젝트 현황 및 업무 배정 |
| `BUDGET_STATUS` | 예산 현황 | 부서별 예산 집행 내역 및 잔액 조회 |
| `DISCLOSURE_STATUS` | 공시 현황 | 기업 공시 및 입찰 현황 조회 |
| `HR_APPOINTMENT` | 인사 발령 | 인사 발령 내역 및 조직도 조회 |
| `PAYROLL` | 임금 명세서 | 월별 임금 명세서 및 연봉 계약서 |

> `pageCode` 값 자체는 제가 정한 것이다. 화면에는 한글 페이지명만 있어 **코드값 확정이 필요하다.**

🔴 **`global/PERMISSION.md` §3-1 · `global/PAGE.md` §1 과 충돌한다.** 그 문서들은 `page_code` 를 **`BIDDING` · `FINANCE` 2개**로 확정하고 *"늘리지 마라"* 고 적어뒀다.
>
> **2026-08-03 결정 — 위 5개(와이어프레임 기준)로 간다.** 팀 문서 쪽을 수정해야 하며 동훈에게 요청한 상태다.
> 확정 전까지 이 파일과 `global/` 문서가 어긋나 있음을 인지할 것.

## 🚨 ADMIN · MASTER 는 부여 기록 없이도 열람된다

⭐ **전역 role 은 서열형이다** — `ADMIN` > `MASTER` > `MEMBER` (`global/PERMISSION.md` §2). **`ADMIN` 이 `MASTER` 의 열람·수정을 포함한다.**

| source | 의미 | 회수 |
|---|---|---|
| `GRANTED` | ADMIN 이 명시적으로 부여 | 가능 |
| `GLOBAL_ROLE` | **`ADMIN` 또는 `MASTER` 라서** 열람됨. 부여 기록이 없다 (`PAGE-004`) | **불가** |

> **부여 기록만 조회하면 틀린다.** 이걸 빼면 ADMIN 이 "3명에게 줬는데 왜 5명이 보나" 로 혼란을 겪는다 (`PAGE-006`). 화면의 `🔒 전역 권한 (중간관리자)` 표기가 이것이다.

> ⛔ **`ADMIN_ONLY` source 는 폐기했다** (2026-08-03). 페이지 5개가 전부 권한 부여 대상이고, 관리자 화면(`P-60`·`P-61`)은 `page_code` 없이 **role 로 연다** (`PAGE.md` §1).
>
> ⛔ **`ADMIN` 계정은 목록에 나타나지 않는다.** 시스템 계정이라 사람을 고르는 조회에서 제외된다 (`EMP-003`).

---

## 1. 내 페이지 목록 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/my/pages` |
| 인증 필요 | Y · 전체 사용자 |
| 요구사항 | PAGE-001~004 · USC-PAGE-001~003 |

⛔ **사이드바 버튼 노출의 근거가 되는 API 다.** 여기 없는 페이지는 버튼이 뜨지 않는다.

⭐ **role 별 반환 범위** (2026-08-03 수정)

| role | 반환 | permission |
|---|---|---|
| `ADMIN` | **전체 페이지** | `EDITOR` · `source: GLOBAL_ROLE` |
| `MASTER` | **전체 페이지** | 부여받았으면 그 등급, 아니면 `EDITOR` · `source: GLOBAL_ROLE` |
| `MEMBER` | 부여받은 것만 | 부여된 등급 · `source: GRANTED` |

> `MASTER` 는 *"모든 프로젝트·페이지에 들어가 **보고 고칠 수 있다**"* (`PERMISSION.md` §2). 열람만이 아니라 편집까지다.
> 이전 명세의 "열람 하한선(`VIEWER`)" 은 **폐기**했다.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.content[].pageCode` | String | 페이지 코드 (`NOT NULL`) |
| `data.content[].name` | String | 페이지 표시명 (`NOT NULL`) |
| `data.content[].permission` | String | `VIEWER` · `EDITOR` |
| `data.content[].source` | String | `GRANTED` · `GLOBAL_ROLE` |

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공. 없으면 빈 배열 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |

---

## 2. 페이지 목록 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/pages` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | PAGE-005 · USC-PAGE-004 |

⛔ **페이지 권한 화면의 목록이다.** 고정 카탈로그이므로 페이지 자체를 추가·삭제하는 API 는 없다.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.content[].pageCode` | String | 페이지 코드 |
| `data.content[].name` | String | 페이지명 |
| `data.content[].description` | String | 설명 |
| `data.content[].accessCount` | int | 접근 인원 = `grantedCount` + `globalRoleCount` |
| `data.content[].grantedCount` | int | 명시적 부여 인원 |
| `data.content[].globalRoleCount` | int | 전역 권한 열람 인원 |
| `data.content[].lastModifiedAt` | String | 마지막 수정일 `yyyy-MM-dd` (`null` 허용 — 부여 기록 없음) |

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |

---

## 3. 페이지 접근 가능자 목록

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/pages/{pageCode}/permissions` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | PAGE-005 · PAGE-006 · USC-PAGE-004 · USC-PAGE-005 |

⛔ **`revocable: false` 인 사용자는 회수할 수 없다.** 전역 권한에서 나온 접근이라 회수 대상이 아니다.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.pageCode` · `data.name` | String | 페이지 정보 |
| `data.content[].userId` | String | 사번 (`NOT NULL`) |
| `data.content[].name` | String | 이름 (`NOT NULL`) |
| `data.content[].departmentPath` | String | `기술본부 / 개발팀` (`null` 허용) |
| `data.content[].jobPositionName` | String | 직급명 (`null` 허용) |
| `data.content[].role` | String | 전역 권한. `전역 권한 (중간관리자)` 문구에 쓴다 |
| `data.content[].permission` | String | `VIEWER` · `EDITOR` |
| `data.content[].source` | String | `GRANTED` · `GLOBAL_ROLE` |
| `data.content[].revocable` | boolean | `GLOBAL_ROLE` 은 `false` |
| `data.grantedCount` · `data.globalRoleCount` | int | 집계 |

> 정렬 — `source=GRANTED` 가 먼저, 그다음 이름 오름차순.

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |
| 404 | `PAGE_NOT_FOUND` | 존재하지 않는 페이지 코드 |

---

## 4. 페이지 권한 부여

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/pages/{pageCode}/permissions` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | PAGE-005 · PAGE-007 · PAGE-008 · PAGE-010 · GRP-009 · GRP-010 · USC-PAGE-006 · USC-PAGE-007 · USC-GRP-008 |

> 🔑 **`PUT` 이 아니라 `POST` 인 이유** — `PUT` 은 전체 교체 의미다. 그러면 요청 목록에 없는 사용자의 권한이 회수되는데, 이 화면은 사람을 하나씩 추가·변경·회수하는 구조다. 게다가 접근 가능자 목록에 **회수할 수 없는 `MASTER` 가 섞여 있어** 전체 교체가 위험하다.

⛔ **부여와 등급 변경이 같은 API 다.** 이미 권한이 있으면 등급을 갱신한다 (`PAGE-008` · `PAGE-010`).
⛔ **전체 교체가 아니다.** 요청 목록에 없는 사용자의 권한은 건드리지 않는다.
⛔ **그룹으로 부여하면 개인 단위로 저장된다.** 이후 그룹 구성원이 바뀌어도 이 권한은 변하지 않는다 (`GRP-010`).
⛔ **`ADMIN` 은 대상이 될 수 없다.** 시스템 계정이며 이미 전체를 뚫는다.
⛔ **`MASTER` 에게 부여하는 것은 무의미하지만 막지 않는다.** 이미 전체를 편집할 수 있으므로 행이 있어도 결과가 같다. 화면에서 후보로 뜨면 ADMIN 이 실수로 넣을 수 있으니 프론트가 안내하는 편이 낫다.

**Request Body**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `permissions` | List\<Object\> | Y | 1개 이상 |
| `permissions[].userId` | String | Y | 사번 |
| `permissions[].permission` | String | Y | `VIEWER` · `EDITOR` |

> 그룹 일괄 적용은 프론트가 구성원 목록을 받아 같은 등급으로 배열을 채워 보낸다. 개인별로 다른 등급을 한 번에 설정하는 것도 같은 요청으로 된다.

**Response** — `pageCode` · `requestedCount` · `grantedCount`(신규) · `updatedCount`(등급 변경) · `unchangedCount`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 처리 완료 |
| 400 | `PAGE_INVALID_REQUEST` | `permissions` 가 비었거나 사번 중복 |
| 400 | `PAGE_INVALID_PERMISSION` | 허용되지 않는 등급 값 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` / `ACC_SYSTEM_ACCOUNT_NOT_ALLOWED` | |
| 404 | `PAGE_NOT_FOUND` | 페이지 코드 없음 |
| 404 | `EMP_NOT_FOUND` | 존재하지 않는 사번 포함. **전체를 거부한다** |

---

## 5. 페이지 권한 회수

| 항목 | 내용 |
|------|------|
| Method · URL | `DELETE /api/v1/pages/{pageCode}/permissions/{userId}` |
| 인증 필요 | Y · ADMIN |
| 요구사항 | PAGE-009 · USC-PAGE-008 |

⛔ **`MASTER` 의 권한을 회수해도 페이지는 계속 보인다.** 회수되는 것은 명시적 부여 기록뿐이고 열람은 전역 권한에서 나온다 (`PAGE-004`).
⛔ **부여 기록이 없으면 회수할 것이 없다.** 전역 권한만으로 보고 있던 사용자를 회수하려 하면 `404` 다.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.pageCode` · `data.userId` | String | |
| `data.stillAccessible` | boolean | 회수 후에도 접근 가능한지. `MASTER` 면 `true` |
| `data.accessSource` | String | `true` 일 때 `GLOBAL_ROLE`, 아니면 `null` |

```json
{ "httpStatus": 200, "message": "페이지 권한을 회수했습니다. 전역 권한으로 열람은 계속 가능합니다",
  "data": { "pageCode": "PROJECT_MANAGEMENT", "userId": "EMP002",
    "stillAccessible": true, "accessSource": "GLOBAL_ROLE" } }
```

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 회수 성공 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |
| 404 | `PAGE_NOT_FOUND` | 페이지 코드 없음 |
| 404 | `PAGE_PERMISSION_NOT_FOUND` | 부여된 권한이 없음 |

## 미확정

- [ ] 🔴 **`global/PERMISSION.md` · `global/PAGE.md` 의 `page_code` 를 2개 → 5개로 수정** (동훈 요청 중)
- [ ] `pageCode` 코드값 확정 — 위 5개는 제가 지은 값이다. 화면에는 한글 페이지명만 있다
- [ ] `PAGE-003`(인사관리 ADMIN 전용) 요구사항 폐기 처리 — `ADMIN_ONLY` 가 없어졌으므로 요구사항도 정리 필요

## 팀 문서와의 관계

| 항목 | `global/PERMISSION.md` | 이 파일 | 결론 |
|---|---|---|---|
| `page_code` 개수 | 2개 (`BIDDING`·`FINANCE`) | **5개** | 우리 것 → 팀 문서 수정 |
| role 서열 | `ADMIN` > `MASTER` > `MEMBER` | **동일** | ✅ 팀 문서 수용 |
| ADMIN 이 페이지를 뚫나 | 뚫는다 | **동일** | ✅ 팀 문서 수용 |
| ADMIN 겸직 | 겸직 가능 (사원에게 부여) | **불가 · 시스템 계정** | 우리 것 → 팀 문서 수정 |
| 부여 주체 | ADMIN | **동일** | ✅ |
| `NONE` 값 | 두지 않는다 (행 없음이 차단) | **동일** | ✅ |
| 3지선다 `X`/뷰어/편집 | `X` 는 행 삭제 | **동일** (회수 API) | ✅ |
