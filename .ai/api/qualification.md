# 🎓 학력·자격증 마스터 API — Major & Certificate

**최종 업데이트**: 2026-08-18 (이름 규칙 강화 — `,` `;` `:` · 줄바꿈 금지, 생성·수정 시 `*_INVALID_REQUEST` 400. 엑셀 항목 구분자·`전공:학위` 구분자와 충돌하기 때문) · 2026-08-13 (초안→계약 성립 — 전공·자격증 마스터 CRUD. `business_category` 마스터 패턴 복제, 단 **삭제는 hard delete + 참조 차단**)
**담당**: 김동현 · Domain `인사` · SUB-Domain `Qualification`
**요구사항 명세**: [`../docs/domain/인사/HR-V1.md`](../docs/domain/인사/HR-V1.md) §2-G (`MAJ`·`CRT`)
**미러링 기준**: `business_category` (마스터 CRUD·회사스코프·ADMIN)

> 🎓 **전공·자격증을 마스터로 관리한다.** 사원 학력/자격증(→ [`employee.md`](employee.md))이 이 마스터를 참조한다.
> 관리(생성·수정·삭제)는 **ADMIN 전용**, 회사스코프. 목록 조회도 ADMIN(사원 등록 화면의 드롭다운 원본).
> ⛔ **삭제는 hard delete 다**(business_category 의 soft delete 아님). 참조 사원이 있으면 차단한다(부서·직급 선례).

---

## 공통 원칙

| 축 | 규칙 |
|----|------|
| 권한 | 관리·조회 모두 **ADMIN**. 비ADMIN 은 `403`(account `ACC_ADMIN_REQUIRED` 재사용) |
| 테넌시 | 모든 조회/쓰기에 `company_id` 스코프. 이름 UNIQUE 도 회사 내에서만 |
| 삭제 | **hard delete + 참조 차단**. 사원 학력/자격증이 참조하면 409(`MAJOR_IN_USE`·`CERT_IN_USE`) |
| 이름 | 회사 내 UNIQUE. 중복 생성/수정은 409. **최대 100자 · `,` `;` `:` · 줄바꿈 금지**(400 `*_INVALID_REQUEST`) — 사원 엑셀(`employee.md` §6)이 `,` `;` 줄바꿈을 항목 구분자로, `:` 를 `전공:학위` 구분자로 쓰므로 이름에 들어가면 쪼개진다 (2026-08-18). 기존 데이터는 소급 검사하지 않는다 |
| 정렬 | 이름 오름차순(한글 정렬은 프론트 `localeCompare('ko')` — 전역 정책) |
| 사용 수 | 목록에 `employeeCount`(참조 사원 수, 시스템·퇴사 제외) 포함 → 화면 "사원 N" 배지 |

### 에러코드 접두어 `MAJOR_` · `CERT_`

공통 `COMMON_*`(400/403/404/405/500) · 인증 `AUTH_UNAUTHENTICATED`(401) 는 전 엔드포인트 폴백.

---

## §0 엔드포인트 요약

| 메서드 | 경로 | 무엇 | 상태 |
|---|---|---|---|
| GET | `/api/v1/majors` | [전공 마스터 목록](#1-전공-마스터-목록) | — |
| POST | `/api/v1/majors` | [전공 생성](#2-전공-생성) | — |
| PATCH | `/api/v1/majors/{majorId}` | [전공 수정](#3-전공-수정) | — |
| DELETE | `/api/v1/majors/{majorId}` | [전공 삭제(참조 차단)](#4-전공-삭제) | — |
| GET | `/api/v1/certificates` | [자격증 마스터 목록](#5-자격증-마스터-목록) | — |
| POST | `/api/v1/certificates` | [자격증 생성](#6-자격증-생성) | — |
| PATCH | `/api/v1/certificates/{certificateId}` | [자격증 수정](#7-자격증-수정) | — |
| DELETE | `/api/v1/certificates/{certificateId}` | [자격증 삭제(참조 차단)](#8-자격증-삭제) | — |

---

## 1. 전공 마스터 목록

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/majors` |
| 인증 · 권한 | Y · **ADMIN** |

**Request Parameter** — `keyword` String N (이름 검색)

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.majors[].majorId` | Long | 전공 번호 |
| `data.majors[].name` | String | 전공명 |
| `data.majors[].employeeCount` | int | **활성** 참조 사원 수(시스템·퇴사 제외) — 화면 배지용 |
| `data.majors[].deletable` | boolean | **전체 참조 수**(활성+퇴사+시스템)가 0이면 true. ⚠️ `employeeCount`(활성) 아님 — FK(RESTRICT)는 퇴사 사원 학력도 삭제를 막으므로 활성만 보면 삭제 버튼이 켜졌는데 실제 삭제가 실패한다 |

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공(없으면 빈 배열) |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |

---

## 2. 전공 생성

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/majors` |
| 인증 · 권한 | Y · **ADMIN** |

**Request Body** — `name` String Y (최대 100자)

**Response** — `data.majorId` · `data.name`

| 코드 | code | 설명 |
|---|---|---|
| 201 | – | 생성 성공 |
| 400 | `MAJOR_INVALID_REQUEST` | 이름 누락·100자 초과·금지 문자(`,` `;` `:` 줄바꿈) 포함 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |
| 409 | `MAJOR_NAME_DUPLICATED` | 회사 내 동명 전공 존재 |

---

## 3. 전공 수정

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/majors/{majorId}` |
| 인증 · 권한 | Y · **ADMIN** |

**Request Body** — `name` String Y (최대 100자)

**Response** — `data.majorId` · `data.name`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 수정 성공 |
| 400 | `MAJOR_INVALID_REQUEST` | 이름 누락·100자 초과·금지 문자(`,` `;` `:` 줄바꿈) 포함 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |
| 404 | `MAJOR_NOT_FOUND` | 전공 없음(타 회사 포함) |
| 409 | `MAJOR_NAME_DUPLICATED` | 동명 전공 존재 |

---

## 4. 전공 삭제

| 항목 | 내용 |
|------|------|
| Method · URL | `DELETE /api/v1/majors/{majorId}` |
| 인증 · 권한 | Y · **ADMIN** |

⛔ **hard delete.** 참조하는 사원 학력이 있으면 삭제하지 않는다.

**Response** — `data.majorId`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 삭제 성공 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |
| 404 | `MAJOR_NOT_FOUND` | 전공 없음 |
| 409 | `MAJOR_IN_USE` | 참조하는 사원 학력이 있음(message 에 사원 수) |

---

## 5. 자격증 마스터 목록

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/certificates` |
| 인증 · 권한 | Y · **ADMIN** |

**Request Parameter** — `keyword` String N

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.certificates[].certificateId` | Long | 자격증 번호 |
| `data.certificates[].name` | String | 자격증명 |
| `data.certificates[].employeeCount` | int | **활성** 참조 사원 수(시스템·퇴사 제외) — 화면 배지용 |
| `data.certificates[].deletable` | boolean | **전체 참조 수**(활성+퇴사+시스템)가 0이면 true (전공과 동일 규칙) |

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공(없으면 빈 배열) |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |

---

## 6. 자격증 생성

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/certificates` |
| 인증 · 권한 | Y · **ADMIN** |

**Request Body** — `name` String Y (최대 100자)

**Response** — `data.certificateId` · `data.name`

| 코드 | code | 설명 |
|---|---|---|
| 201 | – | 생성 성공 |
| 400 | `CERT_INVALID_REQUEST` | 이름 누락·100자 초과·금지 문자(`,` `;` `:` 줄바꿈) 포함 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |
| 409 | `CERT_NAME_DUPLICATED` | 동명 자격증 존재 |

---

## 7. 자격증 수정

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/certificates/{certificateId}` |
| 인증 · 권한 | Y · **ADMIN** |

**Request Body** — `name` String Y (최대 100자)

**Response** — `data.certificateId` · `data.name`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 수정 성공 |
| 400 | `CERT_INVALID_REQUEST` | 이름 누락·100자 초과·금지 문자(`,` `;` `:` 줄바꿈) 포함 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |
| 404 | `CERT_NOT_FOUND` | 자격증 없음 |
| 409 | `CERT_NAME_DUPLICATED` | 동명 자격증 존재 |

---

## 8. 자격증 삭제

| 항목 | 내용 |
|------|------|
| Method · URL | `DELETE /api/v1/certificates/{certificateId}` |
| 인증 · 권한 | Y · **ADMIN** |

⛔ **hard delete.** 참조하는 사원 자격증이 있으면 삭제하지 않는다.

**Response** — `data.certificateId`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 삭제 성공 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |
| 404 | `CERT_NOT_FOUND` | 자격증 없음 |
| 409 | `CERT_IN_USE` | 참조하는 사원 자격증이 있음(message 에 사원 수) |

---

## 사원 학력/자격증과의 관계

사원에 붙는 학력/자격증은 **employee 도메인**이 소유한다([`employee.md`](employee.md) 등록·상세·수정·엑셀 확장). 이 마스터는 그 **참조 원본**이다:

| 개념 | 소유 | 참조 |
|------|------|------|
| 전공 마스터(`major`) | 이 문서 | `employee_education.major_id` |
| 자격증 마스터(`certificate`) | 이 문서 | `employee_certificate.certificate_id` |
| 학위(degree) | enum(`BACHELOR·MASTER·DOCTOR`) — 마스터 아님 | `employee_education.degree` |

> ⭐ **엑셀 자동 생성 경로** (2026-08-18) — `employee.md` §7·§8 의 `autoCreateMasters=true` 는 이 마스터를 **사원 등록 전에 이름만으로 생성**한다.
> 생성 규칙(이름 검증·회사 내 UNIQUE)은 위 §2·§6 과 동일하며, 이미 같은 이름이 있으면 새로 만들지 않고 그 마스터를 참조한다. 이 경로도 ADMIN 전용이다.
