# 📎 File · FileVersion API

**최종 업데이트**: 2026-08-06 (§1 업로드 대상에 결재(APPROVAL) 블록 추가 · §3 목록은 FILE 전용 명시) · **담당**: 김동현 · Domain `프로젝트` · SUB-Domain `File` · `FileVersion`

> 이 파일의 명세가 프론트와의 계약이다. 경로·필드명·타입·상태코드·에러코드를 **한 글자도 바꾸지 않는다** (`../API.md` §0).
> 변경이 필요하면 코드를 먼저 고치지 말고 **이 md 를 먼저 고친 뒤** 팀에 공유한다.
> 파일을 `프로젝트` Domain 에 둔 이유 — 파일은 블록에 붙고 권한도 스텝을 따른다. `Block`·`IssueBlock` 과 같은 계열이다.

## ⭐ 확정 반영 (2026-08-06) — 팀 합의 완료

이전 "미확정"·논의 항목의 확정 결과다. 프론트/AI/결재와 공유 대상.

| 항목 | 확정 |
|---|---|
| presigned 만료 | **업로드 10분 · 다운로드 5분** (§1·§9). 미리보기(§10)는 서버 스트리밍이라 URL·만료 개념 없음 |
| 미완료 업로드 정리 | **12시간** 후 정리 (배치 + S3 Lifecycle) |
| 버전 배지 | **v1 부터 항상 표시** (이전 "versionCount=1 이면 숨김" 규칙 폐기) |
| `previewable` 판정 | **확장자만**(pdf 여부). page_count 추출 성공 여부는 보지 않는다 |
| 미리보기 미지원 파일 | 계약 변경 없음 — §10 이 `FILE_PREVIEW_NOT_SUPPORTED`, FE 가 "미지원 파일" 안내 |
| 영구삭제(§7) 차단 범위 | 결재(`approval_document`) **+ AI 분석(`vitamate_analysis_document`)** 참조까지. `file_index`·`document_chunk` 는 파생이라 함께 정리 |
| `file_index.index_status` | **Spring DB 정본 · Python callback 으로 갱신** · 값 `PENDING·PROCESSING·COMPLETED·FAILED` |
| §11 버전목록 스코프 | **프로젝트 전체** — 경로 `GET /projects/{projectId}/file-versions` (블록 단위 폐기) |
| 업로드 대상 블록(§1) | **`FILE` + `APPROVAL`** — 결재 블록 드롭존도 공용 파일 API 재사용. `block_file`+`approval_document` **이중 링크**. **단 §3 목록은 `FILE` 전용**(결재 파일은 결재 상세에서 조회) |

**블록 생명주기 분리 (A안 확정 · `../docs/global/BLOCK.md` §4-4 근거)** — 블록 삭제는 파일을 건드리지 않는다. 파일은 `file.project_id` 소속으로 살아남고, 조회는 `block.deleted_at IS NULL` 로 거른다. 블록 삭제 후 남은 파일 접근은 §11(프로젝트 전체 보기)로 회수한다. `block_file` 은 hard delete(파일 영구삭제 시 `ON DELETE CASCADE`).

**착수 범위 (2026-08-06 · CRUD 우선)**

- ✅ **이번**: §1·2 업로드 · §3·8·9·10 조회 · **버전 단건 조회(결재용)** · §4 수정 · §5 휴지통 이동 · **파일 버전 목록(비타메이트, #138) 읽기 구현 완료** — 프로젝트 스코프(`/projects/{projectId}/file-versions`) · `file_index` LEFT JOIN
- ⏸️ **나중**: §6 복구 · §7 영구삭제 (휴지통 화면 대기). **#138 의 `index_status` 쓰기(갱신)는 AI 도메인 별도 이슈**(읽기만 file 도메인 소관 · 배정현 확인)

## 엔드포인트

### File (7)

| API명칭 | METHOD | URL | 권한 |
|---|---|---|---|
| 파일 업로드 시작 | POST | `/api/v1/files/uploads` | 스텝 EDITOR |
| 파일 업로드 완료 통보 | POST | `/api/v1/files/uploads/{fileVersionId}/complete` | 스텝 EDITOR |
| 블록 파일 목록 조회 | GET | `/api/v1/blocks/{blockId}/files` | 스텝 접근 권한 |
| 문서명 수정 | PATCH | `/api/v1/files/{fileId}` | 스텝 EDITOR |
| 휴지통으로 이동 | DELETE | `/api/v1/files/{fileId}` | 스텝 EDITOR |
| 휴지통에서 복구 | POST | `/api/v1/files/{fileId}/restore` | 스텝 EDITOR |
| 영구 삭제 | POST | `/api/v1/files/{fileId}/permanent-deletion` | 스텝 EDITOR |

### FileVersion (5)

| API명칭 | METHOD | URL | 권한 |
|---|---|---|---|
| 버전 이력 조회 | GET | `/api/v1/files/{fileId}/versions` | 스텝 접근 권한 |
| **버전 단건 조회** | GET | `/api/v1/file-versions/{fileVersionId}` | 스텝 접근 권한 |
| 다운로드 URL 발급 | GET | `/api/v1/file-versions/{fileVersionId}/download` | 스텝 접근 권한 |
| 미리보기 조회 | GET | `/api/v1/file-versions/{fileVersionId}/preview` | 스텝 접근 권한 |
| **파일 버전 목록 조회** (비타메이트 분석 선택용) | GET | `/api/v1/projects/{projectId}/file-versions` | 프로젝트 접근 권한 |

> **버전 단건 조회**는 2026-08-03 추가. 결재 블록이 고정한 `file_version_id` 로 그 버전을 조회하는 인터페이스다 (`BLOCK.md` §4-4).

## 🔑 공통 원칙

⛔ **파일 단위 권한이 없다.** 스텝의 편집/열람 권한을 그대로 따른다. 열람이면 미리보기와 다운로드 둘 다 된다 (`FILE-014`).

### 권한 판정 순서 ⭐ (2026-08-03 · `global/PERMISSION.md` §4·§6 반영)

```
1) 전역 role
     ADMIN  → 통과 (수정 시 privileged_override 기록)
     MASTER → 통과 (수정 시 privileged_override 기록)
     MEMBER → 계속

2) 파일 → 스텝 찾기
     file → block_file → block → step

3) 스텝 권한
     step_permission 행 있음 → 그 값        (오버라이드)
     행 없음                 → project_member 값  (상속)
     project_member 에도 없음 → 참여자가 아니다 → 차단
     결과가 NONE             → 차단
     결과가 VIEWER           → 조회만 (미리보기·다운로드·버전이력)
     결과가 EDITOR           → 업로드·수정·삭제까지
```

> 🔴 **3층 상속을 빼먹으면 안 된다.** `step_permission` 만 보면 **프로젝트 권한만 가진 참여자가 전부 차단된다.**
> 스텝에 행이 없는 게 정상이고, 그때는 `project_member` 값을 그대로 쓴다 (`PERMISSION.md` §4-1).

⚠️ **`ADMIN`·`MASTER` 가 남의 프로젝트 파일을 수정·삭제하면 `privileged_override = 1` 을 로그에 표기한다** (`PERMISSION.md` §2-1).

### 블록과 파일의 소유 관계 ⚠️

> `BLOCK.md` §4-4: **파일은 프로젝트 소속**(`file.project_id`)이고 블록은 그걸 **참조**한다. **블록을 지워도 파일은 산다.**

| 상황 | 결과 |
|---|---|
| 블록 삭제 | `block_file` 연결이 무효가 된다. **`file` 은 그대로 살아 있다** |
| 파일 휴지통 이동 | 블록 연결은 유지된다 (복구하면 그 자리로 돌아온다) |

### 🔴 soft delete — `CASCADE` 가 발동하지 않는다 (2026-08-03 추가)

팀은 **삭제를 전면 soft delete** 로 한다 (`docs/README.md` §3). 그래서 `block_file.block_id` 의 `ON DELETE CASCADE` 는 **실제로 걸리지 않는다.**

```
블록 삭제 → block.deleted_at 만 채워진다 (물리 DELETE 없음)
          → CASCADE 미발동
          → block_file 행이 그대로 남는다   ← 여기가 함정
```

⛔ **모든 파일 조회에서 `block.deleted_at IS NULL` 을 명시적으로 확인한다.** FK 에 맡기면 삭제된 블록의 파일이 계속 조회된다.

| 상황 | 처리 |
|---|---|
| 삭제된 블록의 파일 목록 조회 | `404 FILE_BLOCK_NOT_FOUND` — 삭제된 블록은 없는 블록으로 취급 |
| 삭제된 블록에 업로드 시도 | `404 FILE_BLOCK_NOT_FOUND` |
| 복구 대상 파일의 블록이 삭제됨 | 복구는 **성공**하고 `blockId: null` · `blockDeleted: true` 로 알린다 (§6) |

> `CASCADE` 자체는 하드 삭제(데이터 정리 배치 등)를 대비해 남겨둔다. 실행되지 않는 것과 없는 것은 다르다.

### 결재와의 인터페이스 ⚠️

> `BLOCK.md` §4-4: 결재 블록이 파일 블록을 지목하고 `approval_document.file_version_id` 로 **그 시점 버전을 고정**한다.
> `file_id` 가 아니라 **`file_version_id`** 를 박는다. **파일 블록 담당(나)이 이 조회 인터페이스를 제공한다.**

그래서 **버전 단건 조회 API** 를 제공한다 (아래 11번). 그리고 결재 대상 파일은 **결재 진행 중 삭제가 잠긴다** (`BLOCK.md` §8).

⭐ **확정 사항**

| 항목 | 값 |
|---|---|
| 저장소 | S3 · **presigned URL** (클라이언트가 직접 PUT) |
| 최대 크기 | **50MB** |
| 확장자 | **블랙리스트** — 실행파일만 차단 (`.exe` `.bat` `.sh` `.jar` 등) |
| 버전 | `⬆` 업로드마다 증가 · 코멘트 선택 입력 · **되돌리기·버전 삭제 없음** |
| 미리보기 | **PDF 만** · 최대 5페이지 |
| 삭제 | 소프트=휴지통 이동·S3 유지 / 영구=`"영구 삭제"` 입력 후 **전 버전 S3 제거** |
| 휴지통 | 보관 기간 **무제한** |

⭐ **`영구 삭제` 를 `DELETE` 가 아니라 `POST` 로 둔 이유** — 확인 문자를 서버가 검증해야 하는데(`FILE-023`), `DELETE` 에 본문을 담으면 일부 프록시와 클라이언트가 본문을 버린다.

---

## 1. 파일 업로드 시작

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/files/uploads` |
| 인증 필요 | Y · 스텝 EDITOR |
| 요구사항 | FILE-004~010 · FILE-014 · VER-002~005 · USC-FILE-003~006 · USC-VER-002 |

⛔ **새 문서와 새 버전이 같은 API 다.** `fileId` 를 주면 그 문서의 새 버전, 주지 않으면 새 문서(버전 1)다. 검증·presigned 발급·완료 통보 흐름이 완전히 같다.
⛔ **파일 자체는 이 API 로 올리지 않는다.** 응답의 `uploadUrl` 로 클라이언트가 저장소에 직접 PUT 한 뒤 완료 통보를 호출한다.
⛔ **동명 문서가 있으면 `409` 로 거부한다.** 사용자가 확인하면 `allowDuplicateName: true` 로 다시 호출한다 (`FILE-009`).
⭐ **`uploadUrl` 만료 = 10분** (2026-08-06 확정).
⭐ **업로드 대상 블록 = `FILE` 또는 `APPROVAL`** (2026-08-06 확정). 결재 블록의 드롭존에 올린 파일도 이 API 로 받는다 — 결재 도메인은 자체 업로드 API 를 두지 않고 공용 파일 API 를 재사용한다. 결재 블록에 올리면 파일이 `block_file` 로 그 블록에 매달리고(FILE 블록과 동일), 이후 프론트가 결재 첨부 API(`POST /approvals/{id}/revisions/{revId}/documents`)로 `fileVersionId` 를 넘겨 `approval_document` 링크를 추가한다 → **`block_file` + `approval_document` 이중 링크**(팀 합의). 권한·삭제잠금(§5)·버전 조회는 FILE 블록과 완전히 같은 `블록→스텝` 경로를 탄다. 그 외 타입 블록은 `FILE_BLOCK_NOT_FOUND`.

**Request Body**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `blockId` | Long | Y | 파일을 붙일 블록 (`FILE` 또는 `APPROVAL` 타입) |
| `originalFileName` | String | Y | 원본 파일명 (확장자 포함) |
| `sizeBytes` | Long | Y | 50MB 이하 |
| `mimeType` | String | N | MIME 타입 |
| `name` | String | N | 문서 표시명. 생략하면 확장자를 뗀 원본 파일명 |
| `fileId` | Long | N | 새 버전을 올릴 대상 문서. 생략하면 새 문서 |
| `comment` | String | N | 버전 코멘트 (`VER-005`) |
| `allowDuplicateName` | Boolean | N | 기본 `false` |

**Response** — `fileId` · `fileVersionId` · `versionNo` · `uploadUrl` · `expiresAt`

| 코드 | code | 설명 |
|---|---|---|
| 201 | – | 발급 성공. 버전이 `업로드중` 으로 생성됨 |
| 400 | `FILE_INVALID_REQUEST` | 필수값 누락/형식 오류 |
| 400 | `FILE_SIZE_EXCEEDED` | 50MB 초과 (`FILE-007`) |
| 400 | `FILE_EXTENSION_BLOCKED` | 실행 파일 확장자 (`FILE-008`) |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `FILE_EDIT_PERMISSION_REQUIRED` | 스텝 편집 권한 없음 |
| 404 | `FILE_BLOCK_NOT_FOUND` | 블록이 없거나 **soft delete** 됨 (`block.deleted_at IS NOT NULL`) 또는 **`FILE`·`APPROVAL` 이 아닌 타입** |
| 404 | `FILE_NOT_FOUND` | `fileId` 로 지정한 문서 없음 |
| 409 | `FILE_NAME_DUPLICATED` | 동명 문서 존재. `allowDuplicateName: true` 로 재요청 |

---

## 2. 파일 업로드 완료 통보

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/files/uploads/{fileVersionId}/complete` |
| 인증 필요 | Y · 스텝 EDITOR |
| 요구사항 | FILE-013 · VER-006 · VER-008 · USC-FILE-008 · USC-FILE-009 · USC-VER-005 · USC-VER-006 |

> 🔑 **이 단계가 핵심이다.** 서버가 저장소에 직접 확인한다. 클라이언트의 통보만 믿으면 **올리지 않고 "완료"만 보내도 깨진 링크가 생긴다** (`FILE-013`).

⛔ **업로더 정보가 이 시점에 스냅샷으로 확정된다.** 이름·부서·직책을 버전에 박아 이후 소속이 바뀌어도 이력이 변하지 않는다 (`VER-006`).
⛔ **PDF 면 총 페이지 수를 추출한다.** 실패해도 업로드는 성공 처리하고 페이지 수만 비운다 (`VER-008`).

**Request Body** — `checksum` String N (보내면 서버가 대조)

**Response** — `fileId` · `fileVersionId` · `versionNo` · `name` · `originalFileName` · `extension` · `sizeBytes` · `pageCount` · `comment` · `uploaderName` · `uploaderDepartment` · `uploaderPosition` · `completedAt`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 완료 처리 성공 |
| 400 | `FILE_ALREADY_COMPLETED` | 이미 완료된 버전 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `FILE_EDIT_PERMISSION_REQUIRED` | |
| 404 | `FILE_VERSION_NOT_FOUND` | 버전 없음 |
| 409 | `FILE_OBJECT_NOT_FOUND` | 저장소에 객체 없음. 버전을 `실패` 로 전환 |
| 409 | `FILE_SIZE_MISMATCH` / `FILE_CHECKSUM_MISMATCH` | 크기/체크섬 불일치 |

---

## 3. 블록 파일 목록 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/blocks/{blockId}/files` |
| 인증 필요 | Y · 스텝 접근 권한 |
| 요구사항 | FILE-001~003 · FILE-005 · FILE-020 · FILE-022 · USC-FILE-001 · USC-FILE-014 |

**Request Parameter** — `deleted` Boolean N (`true` 면 휴지통, 기본 `false`)

⛔ **휴지통도 이 API 로 조회한다.** 같은 데이터를 같은 권한으로 보므로 API 를 나누지 않았다 (`FILE-020`).
⛔ **완료된 버전이 하나도 없는 문서는 목록에 없다.** 업로드 실패로 버전이 0개인 문서가 빈 항목으로 뜨는 것을 막는다 (`FILE-002`).
⛔ **정렬은 블록 연결일 오름차순이다.** 파일 순서 변경 기능이 없다.
⛔ **블록이 soft delete 됐으면 `404` 다.** `block.deleted_at IS NULL` 을 확인한다 — FK `CASCADE` 는 발동하지 않는다.
⛔ **이 목록은 `FILE` 블록 전용이다.** 결재 블록에 매달린 파일(§1)은 여기서 조회하지 않는다 — 결재 파일은 결재 상세 화면(`approval_document`)에서 본다. `APPROVAL` 블록으로 호출하면 `FILE_BLOCK_NOT_FOUND`. (업로드·다운로드·버전조회는 결재 블록도 받지만 이 목록만 FILE 로 좁힌다.)

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.blockId` | Long | 블록 번호 |
| `data.canEdit` | boolean | 요청자가 편집할 수 있는지. 프론트가 버튼 노출에 쓴다 |
| `data.content[].fileId` | Long | 문서 번호 (`NOT NULL`) |
| `data.content[].name` | String | 문서 표시명 (`NOT NULL`) |
| `data.content[].latestVersionId` | Long | 최신 버전 번호 |
| `data.content[].latestVersionNo` | int | 최신 버전 차수 |
| `data.content[].versionCount` | int | 전체 버전 수. **버전 배지는 `v1` 부터 항상 표시** (2026-08-06 변경) |
| `data.content[].originalFileName` | String | 최신 버전 원본 파일명 |
| `data.content[].extension` | String | 확장자. 아이콘·배지에 쓴다 |
| `data.content[].sizeBytes` | Long | 최신 버전 크기 |
| `data.content[].previewable` | boolean | PDF 만 `true` |
| `data.content[].uploaderName` | String | 최신 버전 업로더 (스냅샷) |
| `data.content[].uploaderDepartment` · `uploaderPosition` | String | 스냅샷 (`null` 허용) |
| `data.content[].updatedAt` | String | 최신 버전 업로드 시각 |
| `data.content[].deletedAt` | String | 휴지통 진입 시각. `deleted=false` 면 항상 `null` |

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공. 없으면 빈 배열 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `FILE_ACCESS_PERMISSION_REQUIRED` | 스텝 열람 권한 없음 |
| 404 | `FILE_BLOCK_NOT_FOUND` | 블록 없음 |

---

## 4. 문서명 수정

| 항목 | 내용 |
|------|------|
| Method · URL | `PATCH /api/v1/files/{fileId}` |
| 인증 필요 | Y · 스텝 EDITOR |
| 요구사항 | FILE-016 · USC-FILE-012 |

⛔ **원본 파일명은 바뀌지 않는다.** 표시명만 바꾸며 버전마다 저장된 원본 파일명은 그대로다.

**Request Body** — `name` String Y (최대 255자)
**Response** — `data.fileId` · `data.name`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 수정 성공 |
| 400 | `FILE_INVALID_REQUEST` | 이름이 비었거나 255자 초과 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `FILE_EDIT_PERMISSION_REQUIRED` | |
| 404 | `FILE_NOT_FOUND` | 문서 없음 또는 이미 휴지통 |

---

## 5. 휴지통으로 이동

| 항목 | 내용 |
|------|------|
| Method · URL | `DELETE /api/v1/files/{fileId}` |
| 인증 필요 | Y · 스텝 EDITOR |
| 요구사항 | FILE-018 · FILE-019 · FILE-022 · USC-FILE-013 |

⛔ **저장소 객체는 지우지 않는다.** 복구할 수 있어야 하므로 삭제 시각만 기록한다 (`FILE-019`).
⛔ **보관 기간 제한이 없다** (`FILE-022`).
⛔ **편집 권한이 있으면 누구나 지울 수 있다.** 업로더 본인으로 제한하지 않는다.

🔴 **결재 대상으로 지목된 파일은 결재 진행 중 삭제할 수 없다** (`BLOCK.md` §4-4 · §8). 결재를 회수하거나 완료해야 한다.

**Response** — `data.fileId` · `data.deletedAt`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 이동 성공 |
| 400 | `FILE_ALREADY_DELETED` | 이미 휴지통 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `FILE_EDIT_PERMISSION_REQUIRED` | |
| 404 | `FILE_NOT_FOUND` | 문서 없음 |
| 409 | `FILE_APPROVAL_IN_PROGRESS` | 진행 중인 결재의 대상이라 삭제할 수 없음. `message` 에 결재 정보를 담는다 |

---

## 6. 휴지통에서 복구

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/files/{fileId}/restore` |
| 인증 필요 | Y · 스텝 EDITOR |
| 요구사항 | FILE-021 · USC-FILE-015 |

⛔ **원래 블록으로 복구된다.** 블록 연결은 휴지통에 있는 동안에도 유지된다.

⭐ **블록이 삭제돼도 복구된다** (2026-08-03 수정). `BLOCK.md` §4-4 의 *"블록을 지워도 파일은 산다"* 를 따른다. 이 경우 파일은 **블록에 붙지 않은 상태**로 살아난다.

> 이전 명세의 `FILE_BLOCK_DELETED`(블록 없으면 복구 불가)는 **폐기**했다. 파일이 프로젝트 소속이라는 원칙과 어긋났다.

⛔ **블록이 soft delete 된 경우도 여기에 해당한다.** `block_file` 행은 남아 있지만 블록이 죽었으므로 `blockId: null` · `blockDeleted: true` 로 응답한다. 프론트는 *"원래 블록이 삭제되어 프로젝트 문서함으로 복구했습니다"* 같은 안내를 띄운다.

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.fileId` | Long | 문서 번호 |
| `data.name` | String | 문서 표시명 |
| `data.blockId` | Long | 복구된 블록 번호. 블록이 삭제됐으면 **`null`** |
| `data.blockDeleted` | boolean | 원래 블록이 삭제된 상태인지 |

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 복구 성공 |
| 400 | `FILE_NOT_DELETED` | 휴지통에 없음 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `FILE_EDIT_PERMISSION_REQUIRED` | |
| 404 | `FILE_NOT_FOUND` | 문서 없음 |

---

## 7. 영구 삭제

| 항목 | 내용 |
|------|------|
| Method · URL | `POST /api/v1/files/{fileId}/permanent-deletion` |
| 인증 필요 | Y · 스텝 EDITOR |
| 요구사항 | FILE-023~025 · USC-FILE-016 · USC-FILE-017 |

⛔ **휴지통에 있는 문서만 대상이다.** 목록에서 바로 영구 삭제할 수 없다.
⛔ **모든 버전의 저장소 객체를 제거한다.** 되돌릴 수 없다 (`FILE-025`).
⛔ **확인 문자를 서버가 검증한다.** 정확히 `영구 삭제` 여야 한다 (`FILE-023`).
⛔ **저장소 삭제가 일부 실패해도 DB 는 지운다.** 사용자를 기다리게 하지 않기 위해서이며, 실패한 키는 정리 대상으로 남긴다.

**Request Body** — `confirmText` String Y (`영구 삭제` 와 정확히 일치)
**Response** — `data.fileId` · `data.deletedVersionCount` · `data.storageDeletedCount`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 영구 삭제 성공 |
| 400 | `FILE_CONFIRM_TEXT_MISMATCH` | 확인 문자 불일치 |
| 400 | `FILE_NOT_DELETED` | 휴지통에 없음 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `FILE_EDIT_PERMISSION_REQUIRED` | |
| 404 | `FILE_NOT_FOUND` | 문서 없음 |
| 409 | `FILE_APPROVAL_REFERENCED` | 결재가 이 파일의 버전을 참조하고 있어 영구 삭제할 수 없음 |

> 🔴 **영구 삭제는 결재 이력을 깨뜨릴 수 있다.** `approval_document.file_version_id` 가 이 파일의 버전을 가리키고 있으면, 지운 뒤 결재 이력에서 문서를 열 수 없게 된다. **완료된 결재까지 포함해 참조가 있으면 거부한다.**
>
> 휴지통 이동(`FILE_APPROVAL_IN_PROGRESS`)은 **진행 중** 결재만 막지만, 영구 삭제는 **모든** 참조를 막는다. 되돌릴 수 없기 때문이다.

---

## 8. 버전 이력 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/files/{fileId}/versions` |
| 인증 필요 | Y · 스텝 접근 권한 |
| 요구사항 | VER-001 · VER-002 · VER-007 · VER-010 · USC-VER-001 |

⛔ **버전 삭제와 되돌리기가 없다.** append-only 이며 조회 전용이다 (`VER-007`).
⛔ **업로드에 실패한 버전은 반환하지 않는다.**
⛔ **업로더 정보는 스냅샷이다.** 부서를 옮기거나 퇴사해도 당시 값이 나온다 (`VER-006`).

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.fileId` · `data.name` | | 문서 정보 |
| `data.versionCount` | int | 전체 버전 수 |
| `data.content[].fileVersionId` | Long | 버전 번호 (`NOT NULL`) |
| `data.content[].versionNo` | int | 버전 차수 (`NOT NULL`) |
| `data.content[].latest` | boolean | 최신 버전인지 |
| `data.content[].originalFileName` · `extension` · `sizeBytes` | | 파일 정보 |
| `data.content[].pageCount` | int | PDF 가 아니면 `null` |
| `data.content[].previewable` | boolean | 미리보기 가능 여부 |
| `data.content[].comment` | String | 버전 코멘트 (`null` 허용) |
| `data.content[].uploaderName` | String | 업로더 이름 (`NOT NULL`) |
| `data.content[].uploaderDepartment` · `uploaderPosition` | String | 스냅샷 (`null` 허용) |
| `data.content[].completedAt` | String | 업로드 완료 시각 |

> 정렬 — 버전 차수 내림차순.

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `FILE_ACCESS_PERMISSION_REQUIRED` | |
| 404 | `FILE_NOT_FOUND` | 문서 없음 |

---

## 11. 버전 단건 조회 ⭐ (결재용 인터페이스)

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/file-versions/{fileVersionId}` |
| 인증 필요 | Y · 스텝 접근 권한 |
| 근거 | `BLOCK.md` §4-4 — *"파일 블록 담당이 이 조회 인터페이스를 제공한다"* |

**왜 필요한가** — 결재 블록은 상신 시점의 `file_version_id` 를 `approval_document` 에 박아 **버전을 고정**한다. 이후 결재 화면에서 그 버전 하나를 열어야 하는데, 버전 이력 조회(`/files/{fileId}/versions`)는 `fileId` 를 알아야 하고 목록 전체를 받는다. 결재 쪽은 `fileVersionId` 만 갖고 있다.

⛔ **`latest` 필드로 `대상보다 새 버전 있음` 경고를 만든다.** `false` 면 결재가 고정한 버전 이후에 새 버전이 올라온 것이다 (`BLOCK.md` §4-4).

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data.fileVersionId` | Long | 버전 번호 (`NOT NULL`) |
| `data.fileId` | Long | 문서 번호 |
| `data.fileName` | String | 문서 표시명 |
| `data.versionNo` | int | 버전 차수 |
| `data.latest` | boolean | **최신 버전인지.** `false` 면 경고 배지를 띄운다 |
| `data.latestVersionNo` | int | 현재 최신 차수. 경고 문구에 쓴다 |
| `data.originalFileName` · `extension` · `sizeBytes` | | 파일 정보 |
| `data.pageCount` | int | PDF 가 아니면 `null` |
| `data.previewable` | boolean | 미리보기 가능 여부 |
| `data.comment` | String | 버전 코멘트 (`null` 허용) |
| `data.uploaderName` · `uploaderDepartment` · `uploaderPosition` | String | 스냅샷 |
| `data.completedAt` | String | 업로드 완료 시각 |
| `data.fileDeleted` | boolean | 문서가 휴지통에 있는지. 결재 이력에서 상태를 보여주기 위해 |

```json
{ "httpStatus": 200, "message": "버전 조회 성공",
  "data": { "fileVersionId": 74, "fileId": 31, "fileName": "제안서",
    "versionNo": 1, "latest": false, "latestVersionNo": 2,
    "originalFileName": "제안서_v1.pdf", "extension": "pdf", "sizeBytes": 4404019,
    "pageCount": 38, "previewable": true, "comment": "초안",
    "uploaderName": "김철수", "uploaderDepartment": "사업기획팀", "uploaderPosition": "팀장",
    "completedAt": "2026-07-15 17:44:02", "fileDeleted": false } }
```

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공. **문서가 휴지통에 있어도 반환한다** (`fileDeleted: true`) |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `FILE_ACCESS_PERMISSION_REQUIRED` | |
| 404 | `FILE_VERSION_NOT_FOUND` | 버전 없음 |

> 다운로드·미리보기와 달리 **휴지통에 있어도 `404` 가 아니다.** 결재 이력은 파일이 지워진 뒤에도 무엇을 결재했는지 보여줘야 한다.

---

## 9. 다운로드 URL 발급

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/file-versions/{fileVersionId}/download` |
| 인증 필요 | Y · 스텝 접근 권한 |
| 요구사항 | FILE-014 · VER-011 · VER-012 · USC-VER-008 · USC-VER-009 |

⛔ **파일 바이너리를 반환하지 않는다.** 저장소 다운로드 URL 을 발급하고 클라이언트가 직접 받는다. 서버를 거치지 않아 대역폭과 메모리를 쓰지 않는다.
⛔ **최신 버전과 과거 버전이 같은 API 다** (`VER-011` · `VER-012`).
⛔ **열람 권한이면 다운로드까지 된다.** 미리보기만 허용하고 다운로드를 막는 안은 백로그 (`FILE-014`).
⭐ **다운로드 URL 만료 = 5분** (2026-08-06 확정).

**Response** — `fileVersionId` · `originalFileName` · `sizeBytes` · `downloadUrl` · `expiresAt`

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 발급 성공 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `FILE_ACCESS_PERMISSION_REQUIRED` | |
| 404 | `FILE_VERSION_NOT_FOUND` | 버전 없음 또는 문서가 휴지통 |
| 409 | `FILE_UPLOAD_NOT_COMPLETED` | 업로드 미완료 버전 |

---

## 10. 미리보기 조회

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/file-versions/{fileVersionId}/preview` |
| 인증 필요 | Y · 스텝 접근 권한 |
| 요구사항 | VER-008~010 · FILE-013 · USC-VER-007 |

> 🔑 **저장소 URL 을 발급하지 않는 이유** — presigned URL 을 주면 클라이언트가 **전체 PDF 에 접근**하게 되어 "최대 5페이지" 제한이 무의미해진다. 그래서 미리보기만 **서버가 앞 5페이지를 잘라낸 PDF 를 직접 반환**한다. 다운로드는 어차피 전체를 주는 것이므로 presigned 를 쓴다.

⛔ **응답이 JSON 이 아니다.** 잘라낸 PDF 바이너리다.
⛔ **PDF 만 지원한다.** 다른 형식은 `409` 이며 프론트는 다운로드를 안내한다 (`VER-010`).

| 항목 | 값 |
|---|---|
| Content-Type | `application/pdf` |
| Content-Disposition | `inline; filename="preview.pdf"` |
| `X-Preview-Page-Count` | 잘라낸 페이지 수 (최대 5) |
| `X-Total-Page-Count` | 원본 전체 페이지 수 |

> 두 헤더로 화면 문구를 만든다 — *"전체 문서는 다운로드 후 확인하세요 (총 42페이지)"*.

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | PDF 반환 |
| 401 · 403 | `AUTH_UNAUTHENTICATED` / `FILE_ACCESS_PERMISSION_REQUIRED` | |
| 404 | `FILE_VERSION_NOT_FOUND` | 버전 없음 또는 문서가 휴지통 |
| 409 | `FILE_PREVIEW_NOT_SUPPORTED` | PDF 가 아님 |
| 409 | `FILE_UPLOAD_NOT_COMPLETED` | 업로드 미완료 버전 |
| 500 | `FILE_PREVIEW_FAILED` | PDF 처리 실패 |

## 미확정

- [x] ~~presigned 업로드·다운로드 URL 만료 시간~~ → 확정: 업로드 10분 · 다운로드 5분 (2026-08-06)
- [x] ~~미완료 업로드 정리 주기~~ → 확정: **12시간** (2026-08-06)
- [x] ~~`activity_log` 기록 방식~~ → 확정: 공용 이벤트 계약(`ActivityOccurredEvent`) 발행 · `@TransactionalEventListener(BEFORE_COMMIT)` 같은 트랜잭션 (`ARCHITECTURE.md` §2-3 · `activitylog` 선례). 업로드=CREATE·수정=MODIFY·삭제=DELETE, `blockId` 필수 전달

## 화면 미확보

휴지통 화면과 업로드 진행 화면 목업을 아직 못 받았다. 받은 것은 파일 블록 목록 · 뷰어 · 버전 이력 3개뿐이다.

---

## 11. 파일 버전 목록 조회 (비타메이트 분석 선택용)

| 항목 | 내용 |
|------|------|
| Method · URL | `GET /api/v1/projects/{projectId}/file-versions` |
| 인증 필요 | Y · 프로젝트 접근 권한 (열람 이상) |
| 요구사항 | VER-013 · USC-VER (비타메이트 결합) |
| 요청 출처 | AI/비타메이트 — 분석 요청 화면에서 **분석 대상 파일 버전을 선택**. 분석은 `fileVersionId` 목록 기준으로 저장·수행 |

> 버전 이력 조회(§9)와 필드가 겹치지만, **AI 선택용 별도 read model**이다 — `previewable`·`indexStatus` 가 추가된다.

**Request Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|:---:|---|
| `projectId` (path) | Long | Y | 분석 선택 대상 프로젝트 |

**Response**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `data[].fileId` | Long | 논리 파일 ID |
| `data[].name` | String | 문서 표시명 (`fileName`) |
| `data[].fileVersionId` | Long | **분석 저장 기준 키** |
| `data[].versionNo` | int | 버전 차수 |
| `data[].latest` | boolean | 최신 버전 여부 (과거 버전도 목록에 포함) |
| `data[].originalFileName` | String | 원본 파일명 |
| `data[].extension` | String | 확장자 |
| `data[].sizeBytes` | long | 바이트 크기 |
| `data[].pageCount` | int | 페이지 수 (`null` 허용) |
| `data[].previewable` | boolean | 미리보기 가능 여부 (**확장자 PDF 기준**) |
| `data[].completedAt` | String | 업로드 완료 시각 |
| `data[].indexStatus` | String | 인덱싱 상태 (`embeddingStatus`) — `file_index` 출처 |

**정책**
- ⛔ **업로드 완료된 버전만**(`upload_status = COMPLETED`) 반환한다.
- ⛔ **휴지통 파일은 기본 제외**(`file.deleted_at IS NULL`).
- ✅ **프로젝트 전체 범위** — 특정 블록이 아니라 프로젝트에 속한 모든 문서(`file.project_id`)의 버전을 본다. **블록이 삭제돼 고아가 된 파일도 포함**된다(파일은 프로젝트 소속).
- ✅ **과거 버전도 목록에 포함**한다 (같은 파일의 이전 버전도 선택 가능).
- 인덱싱 상태가 `COMPLETED` 인 버전만 프론트에서 **선택 가능**하게 처리한다 (목록에는 다 내려주되 프론트가 비활성화).

| 코드 | code | 설명 |
|---|---|---|
| 200 | – | 조회 성공 (없으면 빈 배열) |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음/만료 |
| 403 | `FILE_ACCESS_PERMISSION_REQUIRED` | 프로젝트 접근(열람) 권한 없음 |
| 404 | `PROJECT_NOT_FOUND` | 프로젝트 없음 (공용 `ProjectAccessUseCase` 판정) |

> 🟢 **경로 스코프 = 프로젝트 확정 · 읽기 구현 완료** (2026-08-06 · #138). 권한은 파일 단위가 아니라 프로젝트 단위 —
> 공용 **`ProjectAccessUseCase.requireAccess(projectId, userId, role)`** 를 재사용한다(스텝 리소스가 `StepAccessUseCase` 를 쓰는 것과 동형).
> `indexStatus`(`embeddingStatus`)는 `file_index`(**AI 담당 테이블**)에서 오며, **file 도메인이 `file_index` 를 LEFT JOIN 해 내려준다** — 인덱스 행이 없으면 `COALESCE` 로 `PENDING`.
> ⛔ **쓰기(`index_status` 갱신)는 file 도메인이 하지 않는다 — AI 도메인 별도 이슈**(배정현 확인). 읽기만 여기 소관.
> `file_index.index_status` enum 은 `PENDING·PROCESSING·COMPLETED·FAILED` (Spring DB 정본, 확정). `previewable` 은 **확장자 기준(PDF 여부)으로 확정** 판정한다 — §1 상단 규칙과 동일(page_count 추출 성공 여부는 무관).
