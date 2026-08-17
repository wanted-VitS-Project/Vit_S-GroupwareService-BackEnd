# 📁 API 명세 (도메인별)

새 API 는 **여기서 AI 와 함께 설계하고**, 이 md 가 곧 계약이 된다 (2026-08-04 노션 계약 폐지).

> 📖 규칙 전문: [../API.md](../API.md)

---

## 기준은 하나다 — md 에 있으면 계약이다

**`.ai/api/{도메인}.md` 에 적힌 명세가 유일한 계약 기준이다.** 별도 상태 게이트·반영 절차는 없다.

| 상황 | 구현 가능? |
|------|-----------|
| md 에 해당 엔드포인트 명세가 있다 | ✅ 가능. 경로·필드·상태코드·에러코드를 그대로 구현 |
| md 에 없다 | ❌ 먼저 명세를 md 에 작성한다 (설계 요청이면 바로 작성) |

> 개별 파일에 구현 진행상태(미구현/구현중/완료)를 메모로 남기는 것은 자유지만,
> 그것이 **구현 가능 여부를 막지는 않는다.** md 에 명세가 있으면 그게 최신이고 계약이다.

---

## 흐름

1. `.ai/api/{도메인}.md` 에 명세를 작성/갱신한다 (여기서 계약 성립)
2. 명세대로 코드를 구현한다
3. 명세를 바꿔야 하면 **md 를 먼저 고치고** 팀에 공유한 뒤 코드를 맞춘다. 반대 방향 금지

---

## 📂 현재 파일 (2026-08-16)

**21개 파일 · 191개 엔드포인트 · 담당자는 아래 표 참고**

| 파일 | 담당 | SUB-Domain | 개수 | 내용 |
|------|------|----------------|:----:|------|
| [auth.md](auth.md) | 김동현 | `AUTH` | 7 | 로그인 · 로그아웃 · 내 정보 · 비밀번호 변경 |
| [account.md](account.md) | 김동현 | `Account` | 3 | 전역 권한 · 계정 상태 · 비밀번호 재설정 |
| [employee.md](employee.md) | 김동현 | `Employee` | 10 | 목록 · 상세 · 등록 · 수정 · 퇴사 · 엑셀 3종 |
| [department.md](department.md) | 김동현 | `Department` | 4 | 목록(트리) · 생성 · 수정 · 삭제 |
| [job-position.md](job-position.md) | 김동현 | `JobPosition` | 5 | 목록 · 생성 · 수정 · 삭제 |
| [employee-group.md](employee-group.md) | 김동현 | `EmployeeGroup` | 7 | 그룹 4종 + 구성원 3종 |
| [page-permission.md](page-permission.md) | 김동현 | `PagePermission` | 5 | 내 페이지 · 페이지 목록 · 접근자 · 부여 · 회수 |
| [file.md](file.md) | 김동현 | `File` · `FileVersion` | 18 | File 9종 · FileVersion 5종 · 전사 파일 트리 4종(§14) |
| [company-document.md](company-document.md) | 김동현 | `CompanyDocument` | 9 | 사내 문서함 CRUD · 버전 · 업로드 |
| [qualification.md](qualification.md) | 김동현 | `Qualification` | 8 | 전공 · 자격증 마스터 CRUD (참조 차단 삭제) |
| [activity-log.md](activity-log.md) | 김용준 | `ActivityLog` | 1 | 스텝별 활동 기록 조회 (+ 전사 공통 수집 컨벤션 가이드) |
| [issue.md](issue.md) | 김용준 | `Issue` | 8 | 이슈 목록 · 상세 · 생성 · 수정 · 상태변경 · 삭제 · 캘린더 |
| [approval.md](approval.md) | 이강욱 | `Approval` | 12 | 결재 블록 CRUD · 결재선 · 상신 · 승인/반려 |
| [notification.md](notification.md) | 이강욱 | `Notification` | 5 | 알림 목록 · 삭제 · 이동 대상 · 읽음 처리 · SSE 실시간 수신 |
| [bid.md](bid.md) | 정현 | `Bid` | 35 | 입찰 공고 수집·CRUD · AI 요약 · 문서 검토 · 프로젝트 전환 (4개 폐기 포함) |
| [vitamate.md](vitamate.md) | 정현 | `Vitamate` | 13 | AI 문서 분석 요청·결과 조회 · 파이썬 서버 콜백 6종 · 관리자 정리 작업 |
| [checklist.md](checklist.md) | 정림 | `Checklist` | 3 | 체크리스트 항목 생성 · 수정 · 삭제 |
| [text.md](text.md) | 정림 | `Text` | 1 | 텍스트 본문 수정 |
| [image.md](image.md) | 서정림 | `Image` | 10 | 이미지 항목 조회·생성·수정·삭제·복구·다운로드·모아보기 |
| [finance.md](finance.md) | — | `Finance` | 22 | 재무 관리 요약 · 입출금/세금계산서 조회·CSV 업로드·매칭·연결 제외 |
| [settlement.md](settlement.md) | — | `Settlement` | 5 | 정산현황 조회(프로젝트/블록) · 필터 · 정산 항목 조회·수정 |

김동현 담당분은 Domain `인사`(49개 — 인증 7 · 계정 3 · 사원 10 · 부서 4 · 직급 5 · 그룹 7 · 페이지권한 5 · 전공·자격증 8) · `프로젝트`(파일 18개 · 사내 문서함 9개) — **76개**. 나머지 115개는 담당자별 표 참고.
`department.md` · `job-position.md` 에는 요구사항·유스케이스 명세도 함께 들어 있다 (다른 문서에 없어서).
`finance.md` · `settlement.md` 는 담당자 표기가 비어 있다 — 실제 작업자가 채워야 한다.

## 🔴 `.ai/docs/global/` 과의 충돌 (2026-08-03 정리)

`../docs/global/PERMISSION.md` · `PAGE.md` · `BLOCK.md` 를 대조해 **6곳 충돌**을 찾았다. 결론은 아래와 같고, **팀 문서 쪽을 고쳐야 하는 2건은 동훈에게 요청 중**이다.

| # | 항목 | 채택 | 조치 |
|---|------|------|------|
| 1 | `page_code` 개수 | **카탈로그 6개 · 부여 대상 2개**(`BIDDING`·`FINANCE`) — 와이어프레임 5개안은 폐기 (`PERMISSION.md` §3-1 정본) | ✅ 정정 완료 |
| 2 | `ADMIN` 이 프로젝트·페이지를 뚫나 | **뚫는다** (팀 문서) | ✅ 이 폴더 수정 완료 |
| 3 | 전역 role 서열 | **`ADMIN` > `MASTER` > `MEMBER`** (팀 문서) | ✅ 수정 완료 |
| 4 | `ADMIN` 겸직 | **불가 · 시스템 계정** | 🔴 `global/` 문서 수정 요청 |
| 5 | 그룹 · 직급 | **우리 설계 유지** | 🔴 `global/` 문서에 **추가** 요청 |
| 6 | 파일 권한 판정 | **`step_permission` 없으면 `project_member` 상속** (팀 문서) | ✅ 수정 완료 |

**팀 문서에서 새로 발견해 반영한 것**

| 항목 | 반영 위치 |
|------|----------|
| 결재 대상 파일 **삭제 잠금** (`FILE_APPROVAL_IN_PROGRESS`) | `file.md` §5 |
| 결재 참조 파일 **영구 삭제 차단** (`FILE_APPROVAL_REFERENCED`) | `file.md` §7 |
| **버전 단건 조회 API** — 결재가 고정한 `file_version_id` 조회 인터페이스 제공 의무 | `file.md` §11 |
| **블록을 지워도 파일은 산다** → `FILE_BLOCK_DELETED` 폐기 | `file.md` §6 |
| `privileged_override = 1` 로그 | `file.md` 권한 판정 순서 |
| 자기 role 행 수정 차단 (`ACC_SELF_MODIFICATION_NOT_ALLOWED`) | `account.md` §1 |
| `is_system` 정의 통합 (ADMIN · 배치 · 크롤러) | `employee.md` §1 |

> ⚠️ **읽는 순서** — `../docs/global/PERMISSION.md` 를 먼저 읽되, 위 표의 1·4·5번은 **이 폴더가 최신**이다.

## 📐 공통 규칙 (실제 응답 형식)

> ✅ `../API.md` §3-1·§3-3 이 아래와 **일치하도록 갱신됨** (2026-08-04). 이 표와 §3 은 같은 형식을 가리킨다.

| 항목 | 값 |
|------|-----|
| 성공 응답 | `{ httpStatus, message, data }` — **`status` 가 아니다** |
| 실패 응답 | `{ httpStatus, message, code }` |
| 에러 코드 | **`{도메인}_{의미}`** — `AUTH_LOGIN_FAILED` · `FILE_SIZE_EXCEEDED`. 번호식(`AUTH_001`)이 아니다 |
| 페이징 | `data.content[]` · `data.page` · `data.size` · `data.totalElements` · `data.totalPages` |

**에러 코드 접두어 (김동현 선점)** — 에러 코드 접두어(`FILE_`)를 요구사항 ID 접두어(`FILE-`)와 맞춰, `FILE_SIZE_EXCEEDED` 에러가 어느 도메인(`FILE-*` 요구사항) 소속인지 바로 알 수 있게 했다. ⛔ 에러 코드는 의미식이다 — 번호식(`FILE_003`)이 아니다.

`AUTH_` · `ACC_` · `EMP_` · `DEPT_` · `POS_` · `GRP_` · `PAGE_` · `FILE_`

---

## 규칙

- 파일명은 `{도메인}.md` — 예: `auth.md`, `user.md`, `approval.md`
- **작업 중인 도메인만** 둔다. 전체를 복사하지 않는다.
  - 이전 모듈에서 1:1 미러한 명세가 2,025줄 / 72KB 까지 커져 "작업 전 반드시 읽어라" 규칙이 지켜질 수 없게 됐다.
- 명세와 코드가 다르면 → **md 명세가 맞다** (md 자체가 틀렸다면 md 를 먼저 고친다)
- API 를 건드린 PR 은 **"명세(md)를 함께 갱신했다"** 체크 항목을 확인한다

---

## 파일 템플릿

```markdown
# {도메인} API 명세

**최종 수정**: YYYY-MM-DD
**도메인 담당**: {이름}

> 이 파일에 적힌 명세가 프론트와의 계약이다. 임의 변경 금지 — 바꿔야 하면 md 를 먼저 고치고 팀에 공유한다.

---

## 엔드포인트 목록

| 기능 | METHOD | URL | 권한 |
|------|--------|-----|------|
| | | | |

---

### {기능명} `{METHOD} {경로}`

**Request**

| 위치 | 파라미터 | 타입 | 필수 | 설명 |
|------|---------|------|------|------|

**Response** — `{상태코드}`

| 파라미터 | 타입 | 설명 |
|---------|------|------|

**에러**

| 코드 | 상태 | 상황 |
|------|------|------|
```
