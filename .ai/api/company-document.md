# 📄 사내 문서함 API — Company Document

**최종 업데이트**: 2026-08-13 (초안→계약 성립 — `file` API(`file.md`) 미러링. 블록/스텝 대신 **회사 ADMIN 스코프**, soft delete 만(영구삭제 없음), AI 는 트리거만)
**담당**: 김동현
**요구사항 명세**: [`../docs/domain/파일/COMPANY-DOC-V1.md`](../docs/domain/파일/COMPANY-DOC-V1.md) (✅ 확정)
**미러링 기준**: [`file.md`](file.md) — 업로드 2단계·버전·미리보기·다운로드 인프라를 그대로 공유한다

## §0 엔드포인트 요약

| 메서드 | 경로 | 무엇 | 상태 | 권한 |
|---|---|---|---|---|
| POST | `/api/v1/admin/company-documents/uploads` | [업로드 시작(발급)](#1-업로드-시작-발급) | — | ADMIN (`ACC_ADMIN_REQUIRED`) |
| POST | `/api/v1/admin/company-documents/uploads/{versionId}/complete` | [업로드 완료 통보](#2-업로드-완료-통보) | — | ADMIN (`ACC_ADMIN_REQUIRED`) |
| GET | `/api/v1/admin/company-documents` | [문서 목록(카테고리·검색·페이징)](#3-문서-목록-조회) | — | ADMIN (`ACC_ADMIN_REQUIRED`) |
| PATCH | `/api/v1/admin/company-documents/{documentId}` | [표시명·카테고리 수정](#4-표시명카테고리-수정) | — | ADMIN (`ACC_ADMIN_REQUIRED`) |
| DELETE | `/api/v1/admin/company-documents/{documentId}` | [soft delete](#5-삭제-soft-delete) | — | ADMIN (`ACC_ADMIN_REQUIRED`) |
| POST | `/api/v1/admin/company-documents/{documentId}/restore` | [복구](#6-복구) | — | ADMIN (`ACC_ADMIN_REQUIRED`) |
| GET | `/api/v1/admin/company-documents/{documentId}/versions` | [버전 이력](#7-버전-이력-조회) | — | ADMIN (`ACC_ADMIN_REQUIRED`) |
| GET | `/api/v1/admin/company-document-versions/{versionId}/download` | [다운로드 URL 발급](#8-다운로드-url-발급) | — | ADMIN (`ACC_ADMIN_REQUIRED`) |
| GET | `/api/v1/admin/company-document-versions/{versionId}/preview` | [미리보기(PDF 앞 5p)](#9-미리보기-조회) | — | ADMIN (`ACC_ADMIN_REQUIRED`) |

> 🏢 **사내 문서는 회사(테넌트) 소속 독립 애그리거트다.** 프로젝트·블록·스텝에 붙지 않는다.
> 관리·조회 **모두 ADMIN 전용**(`전사 관리 › 전사 파일 관리 › 사내 문서함` 탭). 권한은 회사 단위 역할로 판정한다.
> 모든 조회는 현재 `company_id` 스코프를 강제한다(`CurrentCompanyIdProvider`). 타 회사 문서가 새지 않는다.

---

## 공통 원칙

| 축 | 규칙 |
|----|------|
| 권한 | **관리·조회 모두 ADMIN.** 비ADMIN 은 `403 ACC_ADMIN_REQUIRED`(FILE-Q 전사 파일과 동일 정책). 스텝 권한 상속 없음 |
| 테넌시 | 모든 조회에 `company_id` 스코프. S3 키 `companies/{companyId}/documents/{documentId}/versions/{versionNo}/{uuid}` |
| 새 문서 vs 새 버전 | **같은 업로드 API.** `companyDocumentId` 주면 새 버전, 안 주면 새 문서(버전 1) |
| 업로드 | **2단계** — 발급(presigned) → 클라이언트 PUT → 완료 통보(서버가 저장소 head 확인). `file` 과 동일 |
| URL 만료 | 업로드 10분 · 다운로드 5분(`file` 과 통일) |
| 삭제 | **단순 soft delete + 복구.** file 식 휴지통·영구삭제 2단은 **미도입**(COMPANY-DOC-V1 §6-4). 저장소 객체는 유지 |
| 낙관락 | **미도입**(단순화). 표시명·카테고리 수정은 낙관락 버전 없이 최종 저장 |
| 업로더 스냅샷 | **업로드 시작(§1) 시점**에 `uploaded_by`(사번)로 조회해 이름·부서·직책을 박는다(file 과 동일). **ADMIN 은 employee 행이 없어 스냅샷이 빌 수 있다** → 이름/부서/직책 `nullable`, `uploaded_by` 는 항상 기록. URL 유효 10분 사이의 인사정보 변경은 수용한다 |
| AI 인덱싱 | 완료 버전을 **인덱싱 대상으로 등록(트리거 발행만)**. 소비(청킹·임베딩)는 AI(vitamate) 도메인 소관. soft delete 시 인덱스 제외 트리거 |
| 미리보기 | **PDF 앞 5페이지만**, 서버가 잘라 바이너리 반환(`file` §10 동일). PDF 아니면 다운로드 안내 |
| 카테고리 | 고정 enum `FINANCE`·`COMPANY_INTRO`·`PERFORMANCE`·`CERTIFICATE`·`ETC`. 한글 표시는 프론트 |

### 에러코드 접두어 `CDOC_`

공통 `COMMON_*`(400/403/404/405/500) 과 인증 `AUTH_UNAUTHENTICATED`(401) 는 전 엔드포인트 공통 폴백이다. 아래 표는 도메인 코드만 나열한다.

---

## 1. 업로드 시작 (발급)

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/admin/company-documents/uploads` |
| 인증 · 권한 | Y · **ADMIN** |
| 요구사항 | CDOC-001~005 |

⛔ **새 문서와 새 버전이 같은 API.** `companyDocumentId` 를 주면 그 문서의 새 버전, 없으면 새 문서(버전 1).
⛔ **파일 자체는 이 API 로 올리지 않는다.** 응답의 `uploadUrl` 로 클라이언트가 저장소에 직접 PUT 후 완료 통보(§2)를 호출한다.

**Request Body**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `category` | String | Y | 새 문서일 때 필수. enum(`FINANCE`·`COMPANY_INTRO`·`PERFORMANCE`·`CERTIFICATE`·`ETC`). 새 버전(`companyDocumentId` 지정)이면 무시 |
| `originalFileName` | String | Y | 원본 파일명(확장자 포함) |
| `sizeBytes` | Long | Y | 50MB 이하 |
| `mimeType` | String | N | MIME 타입 |
| `name` | String | N | 문서 표시명. 생략하면 확장자를 뗀 원본 파일명 |
| `companyDocumentId` | Long | N | 새 버전을 올릴 대상 문서. 생략하면 새 문서 |
| `comment` | String | N | 버전 코멘트 |

**Response** — `companyDocumentId` · `versionId` · `versionNo` · `uploadUrl` · `expiresAt`

| 코드 | code | 설명 |
|---|---|---|
| 201 | – | 발급 성공. 버전이 `업로드중` 으로 생성됨 |
| 400 | `CDOC_INVALID_REQUEST` | 필수값 누락/형식 오류 · 카테고리 enum 불일치 |
| 400 | `CDOC_SIZE_EXCEEDED` | 50MB 초과 |
| 400 | `CDOC_EXTENSION_BLOCKED` | 실행 파일 확장자 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |
| 404 | `CDOC_NOT_FOUND` | `companyDocumentId` 로 지정한 문서 없음(타 회사 포함) |

---

## 2. 업로드 완료 통보

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/admin/company-documents/uploads/{versionId}/complete` |
| 인증 · 권한 | Y · **ADMIN** |
| 요구사항 | CDOC-004·005 · INV-03·04·06·07 |

> 🔑 **서버가 저장소를 직접 확인한다**(head). 객체가 없으면 버전을 `실패` 로 전환한다.
⛔ **업로더 스냅샷은 업로드 시작(§1)에 이미 확정돼 있다**(file 과 동일). 완료 시점에는 조회하지 않는다.
⛔ **PDF 면 총 페이지 수를 추출**한다. 실패해도 완료 처리하고 페이지 수만 비운다.
⛔ **완료 시 인덱싱 트리거를 발행**한다(AI 도메인이 소비).

**Request Body** — `checksum` String N (보내면 **기록**한다. ⚠️ 현재 서버는 크기만 검증하고 checksum 대조는 미구현 — file 과 동일. `CDOC_CHECKSUM_MISMATCH` 는 향후 대조 도입 시 발생한다)

**Response** — `companyDocumentId` · `versionId` · `versionNo` · `name` · `category` · `originalFileName` · `extension` · `sizeBytes` · `pageCount`(nullable) · `comment`(nullable) · `uploaderName`(nullable) · `uploaderDepartment`(nullable) · `uploaderPosition`(nullable) · `completedAt`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 완료 처리 성공 |
| 400 | `CDOC_ALREADY_COMPLETED` | 이미 완료된 버전 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |
| 404 | `CDOC_VERSION_NOT_FOUND` | 버전 없음(타 회사 포함) |
| 409 | `CDOC_OBJECT_NOT_FOUND` | 저장소에 객체 없음. 버전을 `실패` 로 전환 |
| 409 | `CDOC_SIZE_MISMATCH` | 업로드된 크기가 요청과 다름 (`CDOC_CHECKSUM_MISMATCH` 는 향후 대조 도입 시) |

---

## 3. 문서 목록 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/admin/company-documents` |
| 인증 · 권한 | Y · **ADMIN** |
| 요구사항 | CDOC-011 · INV-02 |

⛔ **완료 버전이 하나도 없는 문서는 목록에 없다**(업로드 실패로 버전 0개인 문서 숨김).
⛔ **현재 `company_id` 스코프.** 정렬 최신 완료 버전 시각 내림차순.

**Request Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `category` | String | N | 카테고리 필터(enum). 생략하면 전체 |
| `keyword` | String | N | 표시명·원본 파일명 검색 |
| `page` | int | N | 0-base, 기본 0 |
| `size` | int | N | 기본 20, 최대 100 |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.content[].companyDocumentId` | Long | 문서 번호 |
| `data.content[].category` | String | 카테고리 enum |
| `data.content[].name` | String | 표시명 |
| `data.content[].latestVersionId` | Long | 최신 완료 버전 번호 |
| `data.content[].latestVersionNo` | int | 최신 차수 |
| `data.content[].versionCount` | int | 완료 버전 수 |
| `data.content[].originalFileName` · `extension` · `sizeBytes` | | 최신 버전 파일 정보 |
| `data.content[].previewable` | boolean | PDF 만 `true` |
| `data.content[].uploaderName` | String | 최신 버전 업로더(nullable) |
| `data.content[].uploaderDepartment` · `uploaderPosition` | String | 스냅샷(nullable) |
| `data.content[].updatedAt` | String | 최신 완료 버전 시각 |
| `data.page` · `data.size` · `data.totalElements` · `data.totalPages` | | 페이징(FILE-Q 전사 파일과 동일 구조) |

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공(없으면 빈 배열) |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |

---

## 4. 표시명·카테고리 수정

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/admin/company-documents/{documentId}` |
| 인증 · 권한 | Y · **ADMIN** |
| 요구사항 | CDOC-020 |

⛔ **원본 파일명은 불변.** 표시명·카테고리만 바꾼다. 낙관락 없음(단순화).

**Request Body** — 둘 다 선택(보낸 것만 반영, 최소 1개 필요)

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `name` | String | N | 새 표시명(최대 255자) |
| `category` | String | N | 새 카테고리 enum |

**Response** — `data.companyDocumentId` · `data.name` · `data.category`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 수정 성공 |
| 400 | `CDOC_INVALID_REQUEST` | 이름 255자 초과 · 카테고리 enum 불일치 · 둘 다 비어 변경 없음 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |
| 404 | `CDOC_NOT_FOUND` | 문서 없음 또는 이미 삭제 |

---

## 5. 삭제 (soft delete)

| 항목 | 내용 |
|------|------|
| Method · URL | `DELETE /api/v1/admin/company-documents/{documentId}` |
| 인증 · 권한 | Y · **ADMIN** |
| 요구사항 | CDOC-021 · INV-05 · CDOC-031·032 |

⛔ **저장소 객체는 지우지 않는다**(삭제 시각만 기록, 복구 가능). ⛔ **삭제 시 인덱스 제외 트리거**를 발행한다(AI 참조에서 빠짐).

**Response** — `data.companyDocumentId` · `data.deletedAt`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 삭제 성공 |
| 400 | `CDOC_ALREADY_DELETED` | 이미 삭제 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |
| 404 | `CDOC_NOT_FOUND` | 문서 없음 |

---

## 6. 복구

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/admin/company-documents/{documentId}/restore` |
| 인증 · 권한 | Y · **ADMIN** |
| 요구사항 | CDOC-021 |

⛔ **삭제된 문서만 대상.** 복구 시 인덱싱 재등록 트리거를 발행한다.

**Response** — `data.companyDocumentId` · `data.name` · `data.category`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 복구 성공 |
| 400 | `CDOC_NOT_DELETED` | 삭제 상태가 아님 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |
| 404 | `CDOC_NOT_FOUND` | 문서 없음 |

---

## 7. 버전 이력 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/admin/company-documents/{documentId}/versions` |
| 인증 · 권한 | Y · **ADMIN** |
| 요구사항 | CDOC-012 · INV-04·06 |

⛔ **append-only, 조회 전용.** 업로드 실패 버전은 반환하지 않는다. 정렬 버전 차수 내림차순.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.companyDocumentId` · `data.name` · `data.category` | | 문서 정보 |
| `data.versionCount` | int | 완료 버전 수 |
| `data.content[].versionId` | Long | 버전 번호 |
| `data.content[].versionNo` | int | 버전 차수 |
| `data.content[].latest` | boolean | 최신 버전인지 |
| `data.content[].originalFileName` · `extension` · `sizeBytes` | | 파일 정보 |
| `data.content[].pageCount` | int | PDF 아니면 `null` |
| `data.content[].previewable` | boolean | 미리보기 가능 여부 |
| `data.content[].comment` | String | 버전 코멘트(nullable) |
| `data.content[].uploaderName` · `uploaderDepartment` · `uploaderPosition` | String | 스냅샷(nullable) |
| `data.content[].completedAt` | String | 완료 시각 |

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |
| 404 | `CDOC_NOT_FOUND` | 문서 없음 |

---

## 8. 다운로드 URL 발급

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/admin/company-document-versions/{versionId}/download` |
| 인증 · 권한 | Y · **ADMIN** |
| 요구사항 | CDOC-014 |

⛔ **파일 바이너리를 반환하지 않는다.** 저장소 다운로드 URL(5분)을 발급한다.

**Response** — `versionId` · `originalFileName` · `sizeBytes` · `downloadUrl` · `expiresAt`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 발급 성공 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |
| 404 | `CDOC_VERSION_NOT_FOUND` | 버전 없음 또는 문서 삭제됨 |
| 409 | `CDOC_UPLOAD_NOT_COMPLETED` | 업로드 미완료 버전 |

---

## 9. 미리보기 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/admin/company-document-versions/{versionId}/preview` |
| 인증 · 권한 | Y · **ADMIN** |
| 요구사항 | CDOC-013 |

> 🔑 presigned 를 주면 전체 PDF 에 접근하게 되어 "앞 5페이지" 제한이 무의미해진다. 그래서 **서버가 앞 5페이지를 잘라낸 PDF 를 직접 반환**한다.

⛔ **응답이 JSON 이 아니다.** 잘라낸 PDF 바이너리다. PDF 만 지원.

| 항목 | 값 |
|---|---|
| Content-Type | `application/pdf` |
| Content-Disposition | `inline; filename="preview.pdf"` |
| `X-Preview-Page-Count` | 잘라낸 페이지 수(최대 5) |
| `X-Total-Page-Count` | 원본 전체 페이지 수 |

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | PDF 반환 |
| 403 | `ACC_ADMIN_REQUIRED` | ADMIN 아님 |
| 404 | `CDOC_VERSION_NOT_FOUND` | 버전 없음 또는 문서 삭제됨 |
| 409 | `CDOC_PREVIEW_NOT_SUPPORTED` | PDF 아님 |
| 409 | `CDOC_UPLOAD_NOT_COMPLETED` | 업로드 미완료 버전 |
| 500 | `CDOC_PREVIEW_FAILED` | PDF 처리 실패 |

---

## 미확정 · 후속

- **AI 인덱싱 소비 방식**(§6-2) — AI(vitamate) 도메인 소관. 우리는 완료/삭제 시 **트리거 포트만** 발행하고 스텁으로 격리한다. AI 도메인이 소비 경로를 확정하면 어댑터를 연결한다.
- **활동로그** — v1 에서는 사내 문서 이벤트를 활동로그에 남기지 않는다(관리자 영역, file 과 달리 블록/스텝 컨텍스트가 없음). 필요 시 후속.
