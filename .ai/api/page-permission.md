# 🔒 PagePermission API

**최종 업데이트**: 2026-08-03 · **담당**: 김동현 · Domain `인사` · SUB-Domain `PagePermission`

> 이 파일의 명세가 프론트와의 계약이다. 경로·필드명·타입·상태코드·에러코드를 **한 글자도 바꾸지 않는다** (`../API.md` §0).
> 변경이 필요하면 코드를 먼저 고치지 말고 **이 md 를 먼저 고친 뒤** 팀에 공유한다.

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
| 노출 vs 접근 | **분리한다.** 메뉴가 보인다고 들어갈 수 있는 것은 아니다 (`permission: NONE`) |

⭐ **카탈로그 11개 확정** (2026-08-10 확장 · **실제 상단바 8탭 + 사이드바 기준**)

> 2026-08-03 의 6개안에서, 상단바·사이드바에 실재하는 메뉴를 빠짐없이 담도록 5개(`HOME`·`NOTIFICATION`·`APPROVAL`·`COMPANY_STATUS`·`ADMIN_CONSOLE`)를 추가했다.
> ⚠️ **추가된 5개는 전부 role 로 열린다** — `page_permission` 행이 생기지 않는다. **부여 대상은 여전히 `BIDDING`·`FINANCE` 2개뿐**이다(§2·DB 규칙 불변).

| pageCode | 메뉴 | 노출 | 접근 |
|---|---|---|---|
| `HOME` | 홈 / 대시보드 | **전원** | 노출되면 항상 가능 |
| `NOTIFICATION` | 알림 | **전원** | 〃 |
| `APPROVAL` | 결재관리 | **전원** | 〃 |
| `BIDDING` | **공고 조회 · 입찰 관리** | **전원** | `ADMIN`·`MASTER` 무조건 · `MEMBER` 는 **부여받아야** |
| `PROJECT_CREATE` | 프로젝트 생성 | `ADMIN` 제외 | 노출되면 항상 가능 |
| `MY_PROJECT` | 내 프로젝트 | `ADMIN` 제외 | 〃 |
| `FINANCE` | 재무 관리 | **전원** | `ADMIN`·`MASTER` 무조건 · `MEMBER` 는 **부여받아야** |
| `COMPANY_STATUS` | 전사현황 | `ADMIN`·`MASTER` 만 | 노출되면 항상 가능 |
| `TEMPLATE` | 템플릿 관리 | `ADMIN` 만 | 〃 |
| `ADMIN_CONSOLE` | 관리자 | `ADMIN` 만 | 〃 |
| `SETTINGS` | 설정 | **전원** | 〃 — 단 **하위 항목이 role 별로 다르다** |

> 🧭 **상단바 "프로젝트" 탭은 별도 코드가 없다** — `PROJECT_CREATE`·`MY_PROJECT` 를 담는 컨테이너라 그 둘의 노출로 표시된다.
> 🧭 **`ADMIN_CONSOLE`(관리자) vs `SETTINGS`(설정) 분리** — 관리자는 ADMIN 전용 콘솔(사원·부서·직급·권한 부여), 설정은 전원의 내 정보·비밀번호. 상단바가 둘을 별도 탭으로 두므로 코드도 나눈다.
> 🧭 **`HOME` = 상단바 홈 = 사이드바 Dashboard** (동일 코드).
> ⭐ **`COMPANY_STATUS`(전사현황)가 MASTER 서열이 여는 전용 화면**이다 — `ADMIN`·`MASTER` 는 `GLOBAL_ROLE` 로 열람, `MEMBER` 는 미반환.

🚨 **노출과 접근은 다르다** (2026-08-03 확정). `MEMBER` 에게 `BIDDING`·`FINANCE` 는 **메뉴가 보이되
클릭하면 "권한 없음"** 이다. 기능의 존재를 알리고 관리자에게 요청할 경로를 만들기 위함이다.
→ `permission: NONE` 으로 내려간다. 아래 §"ADMIN·MASTER…" 참고.

⭐ **`SETTINGS` 는 전원에게 열린다.** 내 정보·비밀번호 변경은 누구나 필요하다.
하위 항목만 role 로 갈린다 — `ADMIN` 은 사원·부서·직급·권한 부여까지, 나머지는 내 정보뿐이다.
`page_code` 를 쪼개는 게 아니라 **role 로 가르므로** "하위 단위 없음" 규칙과 부딪히지 않는다.

⭐ **`ADMIN` 이 제외되는 기준은 "내 것이 생기는 화면"** 이다.
`프로젝트 생성`·`내 프로젝트` 는 `project_member` 에 **사람으로 등록**돼야 하는데 `ADMIN` 은 시스템 계정이라 그럴 수 없다.
조회·관리 화면(공고·입찰·재무·템플릿·설정)은 **본다.**

⚠️ **`BIDDING` 하나가 메뉴 2개를 연다.** 공고 조회와 입찰 관리를 따로 부여하지 않는다
(위 "하위 단위 없음 — 페이지 권한 하나가 그 페이지의 모든 메뉴를 연다" 규칙 그대로).

🚨 **DB `page_code` 컬럼에 들어가는 값은 `BIDDING` · `FINANCE` 2개뿐이다.**
나머지 9개는 role 로 열리므로 `page_permission` 행이 아예 생기지 않는다. 카탈로그(11) ≠ 부여 대상(2).

⛔ **폐기된 5개안** — `PROJECT_MANAGEMENT` · `BUDGET_STATUS` · `DISCLOSURE_STATUS` · `HR_APPOINTMENT` · `PAYROLL`.
와이어프레임 기준으로 지었으나 **대응 화면이 존재하지 않는다**(실제 사이드바와 하나도 일치하지 않음).

## 🚨 ADMIN · MASTER 는 부여 기록 없이도 열람된다

⭐ **전역 role 은 기본적으로 서열형이다** — `ADMIN` > `MASTER` > `MEMBER` (`global/PERMISSION.md` §2).
**단 실무 화면은 예외다** — 아래 참고.

| source | 의미 | 회수 |
|---|---|---|
| `GRANTED` | ADMIN 이 명시적으로 부여 (`MEMBER` 한정) | 가능 |
| `GLOBAL_ROLE` | **`ADMIN` 또는 `MASTER` 라서** 열람됨. 부여 기록이 없다 (`PAGE-004`) | **불가** |
| `ADMIN_ONLY` | `ADMIN` 전용 페이지 — `TEMPLATE`·`ADMIN_CONSOLE` (`PAGE-003`) | **불가** |
| `DEFAULT` | 기본 노출 — `PROJECT_CREATE` · `MY_PROJECT` · `SETTINGS`, 그리고 **미부여 상태의** `BIDDING`·`FINANCE` | **불가** |

⭐ **`permission` 은 3값이다** — `NONE` · `VIEWER` · `EDITOR` (2026-08-03 `NONE` 추가).
`NONE` 은 **메뉴는 보이지만 들어갈 수 없는 상태**다. 권한 부여 화면의 3지선다(`X`/뷰어/편집)와 그대로 대응된다.

> ⛔ **이전 규칙 폐기** — *"행이 없으면 그 탭이 아예 안 열린다"* 는 **노출** 기준이었으나,
> 이제 행이 없어도 **보이고 접근만 막힌다.** 행 없음 = `permission: NONE`.

> **부여 기록만 조회하면 틀린다.** 이걸 빼면 ADMIN 이 "3명에게 줬는데 왜 5명이 보나" 로 혼란을 겪는다 (`PAGE-006`). 화면의 `🔒 전역 권한 (중간관리자)` 표기가 이것이다.

> ⭐ **`ADMIN_ONLY` 는 되살렸다** (2026-08-03 재확정). 폐기했던 근거가 *"페이지 5개가 전부 부여 대상"* 이었는데
> **그 5개안 자체가 폐기**됐다. 다만 해당하는 건 **`템플릿 관리` 하나뿐**이다 —
> `설정` 은 전원에게 열리므로 `DEFAULT` 다.

> 🚨 **`ADMIN` 은 `PROJECT_CREATE`·`MY_PROJECT` 만 반환되지 않는다.** 공고·입찰·재무·템플릿·설정은 본다.
> 제외 기준은 "실무 화면" 이 아니라 **"내 것이 생기는 화면"** 이다 — 두 화면은 `project_member` 에
> **사람으로 등록**돼야 하는데 `ADMIN` 은 시스템 계정이라 그럴 수 없다 (`global/PERMISSION.md` §2-4).
> 즉 판정은 "서열 이상" 이 아니라 **허용 role 집합**이다.

> ⛔ **`ADMIN` 계정은 목록에 나타나지 않는다.** 시스템 계정이라 사람을 고르는 조회에서 제외된다 (`EMP-003`).

---

## 1. 내 페이지 목록 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/my/pages` |
| 인증 필요 | Y · 전체 사용자 |
| 요구사항 | PAGE-001~004 · USC-PAGE-001~003 |

⛔ **사이드바 버튼 노출의 유일한 근거가 되는 API 다.** 여기 없는 페이지는 버튼이 뜨지 않는다.
**프론트는 메뉴 표시 규칙을 갖지 않는다** — 이 응답에 있으면 그리고, 없으면 안 그린다 (2026-08-03 변경).
판정 규칙이 서버 한 곳에만 있어야 게이트를 바꿔도 프론트가 어긋나지 않는다.

⛔ **응답에 있다고 들어갈 수 있는 것은 아니다.** `permission: NONE` 은 **버튼은 그리되 접근은 막으라**는 뜻이다.
`MEMBER` 가 부여받기 전의 `BIDDING`·`FINANCE` 가 이 상태다.

⭐ **role 별 반환 범위** (2026-08-10 · 11코드)

| pageCode | 메뉴 | `ADMIN` | `MASTER` | `MEMBER` |
|---|---|---|---|---|
| `HOME` | 홈 / 대시보드 | `EDITOR`·`DEFAULT` | `EDITOR`·`DEFAULT` | `EDITOR`·`DEFAULT` |
| `NOTIFICATION` | 알림 | `EDITOR`·`DEFAULT` | `EDITOR`·`DEFAULT` | `EDITOR`·`DEFAULT` |
| `APPROVAL` | 결재관리 | `EDITOR`·`DEFAULT` | `EDITOR`·`DEFAULT` | `EDITOR`·`DEFAULT` |
| `BIDDING` | 공고 조회 · 입찰 관리 | `EDITOR`·`GLOBAL_ROLE` | `EDITOR`·`GLOBAL_ROLE` | **부여 시** 그 등급·`GRANTED` / **미부여 시 `NONE`·`DEFAULT`** |
| `PROJECT_CREATE` | 프로젝트 생성 | ❌ 미반환 | `EDITOR`·`DEFAULT` | `EDITOR`·`DEFAULT` |
| `MY_PROJECT` | 내 프로젝트 | ❌ 미반환 | `EDITOR`·`DEFAULT` | `EDITOR`·`DEFAULT` |
| `FINANCE` | 재무 관리 | `EDITOR`·`GLOBAL_ROLE` | `EDITOR`·`GLOBAL_ROLE` | **부여 시** 그 등급·`GRANTED` / **미부여 시 `NONE`·`DEFAULT`** |
| `COMPANY_STATUS` | 전사현황 | `EDITOR`·`GLOBAL_ROLE` | `EDITOR`·`GLOBAL_ROLE` | ❌ 미반환 |
| `TEMPLATE` | 템플릿 관리 | `EDITOR`·`ADMIN_ONLY` | ❌ 미반환 | ❌ 미반환 |
| `ADMIN_CONSOLE` | 관리자 | `EDITOR`·`ADMIN_ONLY` | ❌ 미반환 | ❌ 미반환 |
| `SETTINGS` | 설정 | `EDITOR`·`DEFAULT` | `EDITOR`·`DEFAULT` | `EDITOR`·`DEFAULT` |

**`MEMBER` · 아무 부여 없음 — 8개가 내려간다** (미반환: `COMPANY_STATUS`·`TEMPLATE`·`ADMIN_CONSOLE`)

| pageCode | permission | source |
|---|---|---|
| `BIDDING` · `FINANCE` | **`NONE`** | `DEFAULT` |
| `HOME` · `NOTIFICATION` · `APPROVAL` · `PROJECT_CREATE` · `MY_PROJECT` · `SETTINGS` | `EDITOR` | `DEFAULT` |

프론트는 `NONE` 을 회색 처리하거나 클릭 시 "권한이 없습니다" 를 띄운다.

> `MASTER` 는 *"모든 프로젝트·페이지에 들어가 **보고 고칠 수 있다**"* (`PERMISSION.md` §2). 열람만이 아니라 편집까지다.
> 이전 명세의 "열람 하한선(`VIEWER`)" 은 **폐기**했다.
> ⚠️ role 게이트로 열리는 페이지는 `VIEWER`/`EDITOR` 구분이 없어 **항상 `EDITOR`** 로 내려간다.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.content[].pageCode` | String | 페이지 코드 (`NOT NULL`) |
| `data.content[].name` | String | 페이지 표시명 (`NOT NULL`) |
| `data.content[].permission` | String | **`NONE`** · `VIEWER` · `EDITOR` — `NONE` 은 노출되나 접근 불가 |
| `data.content[].source` | String | `GRANTED` · `GLOBAL_ROLE` · `ADMIN_ONLY` · `DEFAULT` |

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

⛔ **`my/pages` 와 반환 집합이 다르다.** 여기는 **부여 가능한 페이지만** 나온다 — `BIDDING` · `FINANCE` **2개**.
role 로 여는 페이지(`SETTINGS`·`TEMPLATE`·`PROJECT_CREATE`·`MY_PROJECT`)는 **부여 대상이 아니므로 목록에 없다.**
부여할 수 없는 것을 부여 화면에 띄우면 안 된다.

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
| `data.content[].permission` | String | `VIEWER` · `EDITOR` — 접근 가능자 목록이므로 `NONE` 은 나오지 않는다 |
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
  "data": { "pageCode": "FINANCE", "userId": "EMP002",
    "stillAccessible": true, "accessSource": "GLOBAL_ROLE" } }
```

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 회수 성공 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `ACC_ADMIN_REQUIRED` | |
| 404 | `PAGE_NOT_FOUND` | 페이지 코드 없음 |
| 404 | `PAGE_PERMISSION_NOT_FOUND` | 부여된 권한이 없음 |

## 미확정

- [x] `2026-08-03` ~~`global/` 문서의 `page_code` 를 5개로 수정~~ → **5개안 폐기.** 실제 사이드바 기준 카탈로그 6개로 재확정하고 `global/PERMISSION.md`·`PAGE.md` 도 함께 갱신 완료
- [x] `2026-08-03` `pageCode` 코드값 확정 — 실제 메뉴에 대응하는 6개
- [x] `2026-08-03` ~~`PAGE-003` 폐기 처리~~ → **유지.** `ADMIN_ONLY` 가 되살아났다 (`SETTINGS`·`TEMPLATE`)
- [x] `2026-08-10` ~~🔴 `홈`·`알림`·`결재관리`·`전사현황` 이 사이드바에 없다~~ → **해소.** 상단바 8탭 확인(스크린샷) 후 카탈로그를 **11개로 확장** — `HOME`·`NOTIFICATION`·`APPROVAL`·`COMPANY_STATUS`·`ADMIN_CONSOLE` 추가. `COMPANY_STATUS`(전사현황)=MASTER+ADMIN 전용으로 **MASTER 전용 화면 확보**.
- [x] `2026-08-10` `내 프로젝트`·`프로젝트 생성` 은 `ADMIN` 제외 확정 — "내 것이 생기는 화면"(project_member 사람 등록) 기준. 나머지(공고·재무·전사현황·템플릿·관리자·설정·홈·알림·결재)는 ADMIN 도 본다.

## 팀 문서와의 관계

| 항목 | `global/PERMISSION.md` | 이 파일 | 결론 |
|---|---|---|---|
| `page_code` 개수 | `BIDDING`·`FINANCE` | **동일** (부여 대상 기준) | ✅ **합의 완료** — 카탈로그는 6, 부여 대상은 2 |
| role 서열 | `ADMIN` > `MASTER` > `MEMBER` | **기본은 동일, 실무 화면은 예외** | ⚠️ **팀 문서 수정 완료** (`ADMIN` 제외 규칙) |
| ADMIN 이 페이지를 뚫나 | 뚫는다 | **실무 화면은 안 뚫는다 — 반환 자체가 안 된다** | 우리 것 → 팀 문서 수정 완료 |
| ADMIN 겸직 | 겸직 가능 (사원에게 부여) | **불가 · 시스템 계정** | 우리 것 → 팀 문서 수정 |
| 부여 주체 | ADMIN | **동일** | ✅ |
| `NONE` 값 | 두지 않았다 (행 없음이 차단) | **`NONE` 도입** — 노출/접근 분리 | 우리 것 → 팀 문서 수정 완료 |
| 3지선다 `X`/뷰어/편집 | `X` 는 행 삭제 | **동일** (회수 API) | ✅ |
