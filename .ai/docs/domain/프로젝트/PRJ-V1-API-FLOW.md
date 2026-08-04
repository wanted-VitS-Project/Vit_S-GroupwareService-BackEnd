# 📁 프로젝트 ~ 블록 계층 v1 — API 흐름 · FE/BE 책임 분담

**최종 업데이트**: 2026-08-05 (⭐ §6-1 블록 조회 — *"LEFT JOIN 한 방"* → **타입별 배치 쿼리**로 정정. `checklist`·`image` 가 1:N 이라 한 방이 물리적으로 불가능했다)
**최종 업데이트**: 2026-08-04 (⭐ 전 엔드포인트 로컬 기준 `✅ 확정` — `AGENTS.md` §3 완화, 노션 동기화는 게이트 아님)
**최종 업데이트**: 2026-08-04 (401 정정 — `AUTH_TOKEN_EXPIRED` → `AUTH_UNAUTHENTICATED` · 인증 방식 **세션 쿠키** 확정)
**담당**: 동훈
**목록**: [`PRJ-V1-API.md`](PRJ-V1-API.md) · **상세**: [`PRJ-V1-API-DETAIL.md`](PRJ-V1-API-DETAIL.md) · **요구사항**: [`PRJ-V1.md`](PRJ-V1.md) · **스키마**: [`ERD.md`](ERD.md)

> 엔드포인트 **38개** 전부에 대해 프론트가 하는 일과 백엔드가 하는 일을 갈랐다.
> ✅ 전 엔드포인트 로컬 기준 `✅ 확정` (`AGENTS.md` §3). 노션 동기화는 게이트 아님.
> ✅ **ERD 확정본 정합** — 사번 `String` · `SETTLEMENT`/`COMPLETED` · `startedOn`/`endedOn` · `permission` · `project.client_name` · `project.contract_amount` · `block.owner`.
> ⛔ **`stepType` 폐기 (2026-08-03)** — 송부 스텝을 만들지 않는다 (`PRJ-V1.md` STP-007).
> ⭐ **`block.type_id` 는 살아 있다** (다형성 양방향 ID · [`ERD.md`](ERD.md) §0-12). 다만 **응답에는 내리지 않는다** — FE 는 `detail` 객체를 받으므로 상세 PK 가 필요 없다. 서버 내부 매핑용 컬럼이다.
> ⚠️ **ERD Cloud 미반영분이 있다** → [`ERD-CLOUD-DIFF.md`](../ERD-CLOUD-DIFF.md)
> 🚨 사람 식별자 타입이 노션 이슈 명세와 충돌한다 → [`PRJ-V1-API.md`](PRJ-V1-API.md) §4-A

---

## 0. 공통 규약

### 0-1. 책임 원칙

| 구분 | FE | BE |
|------|----|----|
| **입력 검증** | 즉시 피드백용 1차 검증 (필수값·날짜 역전·숫자 범위) | **최종 판정.** FE 검증을 신뢰하지 않고 전부 다시 본다 |
| **권한** | 권한 없는 **버튼을 숨긴다** (UX) | **차단은 여기서.** 숨겨진 버튼을 직접 호출해도 403 |
| **상태 전이** | 낙관적 업데이트 금지 — 응답 받고 반영 | 전이 가능 여부 판정 |
| **파괴적 동작** | **확인 모달 필수** (삭제·종결·완료) | 조건 검사 후 실행 |
| **에러 문구** | 사용자용 한국어 문구는 **FE 가 매핑** | `code` 만 정확히 내려준다 |

⚠️ **에러 문구를 BE `message` 그대로 띄우지 마라.** `code` 로 분기하고 문구는 FE 가 갖는다.

### 0-2. 전 API 공통 분기

```
401 AUTH_UNAUTHENTICATED  → 세션 정리 후 로그인 페이지로 이동 (현재 URL 을 redirect 파라미터로)
403 *_DENIED              → 토스트 + 해당 버튼 영구 비활성화 (권한은 새로고침해도 안 바뀐다)
404 *_NOT_FOUND           → 토스트 + 목록에서 제거 / 상위 화면으로 이동
409 *                     → 토스트로 **원인과 다음 행동**을 함께 안내
5xx                       → "잠시 후 다시 시도해주세요" + 재시도 버튼
```

✅ **인증 방식 확정 (2026-08-04)**: **HttpOnly 세션 쿠키**다 (Spring Session · 구현 완료 #95). FE 는 요청에 `credentials: 'include'` 만 켜면 된다. 토큰 재발급 API 는 없다. 공통 401 코드는 `AUTH_UNAUTHENTICATED` — `AUTH_TOKEN_EXPIRED` 는 폐기했다.

---

## 1. Project

### 1-1. 프로젝트 목록 조회 — `GET /api/v1/projects`

```
FE (P-20 프로젝트 목록)                         BE
──────────────────────────────────────────────────────────────
화면 진입 / 필터 변경 / 페이지 이동
    │
    ├─ ① 필터 상태를 URL 쿼리에 동기화 (뒤로가기 복원용)
    │
    ├─ ② 스켈레톤 표시
    │
    ├─ GET /api/v1/projects?status=&page= ──→ ① 인증 쿠키 검증 → 401
    │                                         ② status 값 검증 → 400 PROJECT_STATUS_INVALID
    │                                         ③ 접근 가능 프로젝트 산출
    │                                            · 일반: project_member 보유 건
    │                                            · MASTER/ADMIN: 전 건
    │                                         ④ 필터 적용 (상태·카테고리·기간·키워드)
    │                                         ⑤ 진척률 계산 (완료스텝/전체스텝)
    │                                            └ 스텝 0개면 progressRate **미포함**
    │                                    ←── 200 {content[], page, totalElements}
    │
    └─ ③ 응답 분기
          200 → 카드/테이블 렌더
                progressRate 없으면 **막대 대신 `-` 표시** (0% 로 그리지 마라 · PRJ-013)
                종결 건은 회색 처리 (숨기지 않는다 · PRJ-015)
          400 → 필터 초기화 + 토스트
          401 → 로그인
```

### 1-2. 프로젝트 상세 조회 — `GET /api/v1/projects/{projectId}`

```
FE (P-22 워크스페이스 진입)                     BE
──────────────────────────────────────────────────────────────
/projects/12 진입
    │
    ├─ ① 스켈레톤 + 사이드바 자리 확보
    │
    ├─ GET /api/v1/projects/12 ────────────→ ① 인증 검증 → 401
    │                                        ② 프로젝트 조회 (deleted_at IS NULL) → 404
    │                                        ③ 접근 권한 판정 → 403
    │                                           · project_member 또는 MASTER/ADMIN
    │                                        ④ 진척률·카테고리·종결사유 조립
    │                                        ⑤ myPermission 계산해서 함께 내림
    │                                   ←── 200 {projectId, ..., myPermission}
    │
    └─ ② 응답 분기
          200 → **myPermission 로 UI 게이팅**
                VIEWER → 편집·삭제·설정 버튼 렌더 안 함
                EDITOR → 전체 노출
          403 → "접근 권한이 없습니다" + /projects 로 이동
          404 → "삭제된 프로젝트입니다" + /projects 로 이동
```

### 1-3. 프로젝트 직접 생성 — `POST /api/v1/projects`

```
FE (P-21 프로젝트 직접 생성)                    BE
──────────────────────────────────────────────────────────────
[생성] 클릭
    │
    ├─ ① 클라이언트 검증
    │     과업명 공백 → 인라인 에러, 요청 안 보냄
    │     시작일 > 종료일 → 인라인 에러
    │     계약금액 음수 → 인라인 에러
    │
    ├─ ② 버튼 비활성화 + 로딩 (중복 제출 차단)
    │
    ├─ POST /api/v1/projects ─────────────→ ① 인증 검증 → 401
    │   {name, description, startedOn,      ② name 공백/300자 초과 → 400 PROJECT_NAME_REQUIRED
    │    endedOn, contractAmount,           ③ 날짜 역전 → 400 PROJECT_DATE_RANGE_INVALID
    │    businessCategoryIds[]}             ④ 카테고리 존재 검증 → 404 BUSINESS_CATEGORY_NOT_FOUND
    │                                       ⑤ project INSERT
    │                                          status='NOT_STARTED' · bid_notice_id=NULL
    │                                       ⑥ 생성자를 EDITOR 참여자로 INSERT
    │                                       ⑦ project_business_category N행 INSERT
    │                                       ⑧ activity_log 기록 (target_name 스냅샷)
    │                                  ←── 201 {projectId, ...}
    │
    └─ ③ 응답 분기
          201 → navigate(`/projects/${projectId}`)
                **경고 없이 정상 경로다** (PRJ-001)
          400 → 해당 필드 인라인 에러 + 버튼 복구
          401 → 로그인
```

### 1-4. 프로젝트 수정 — `PATCH /api/v1/projects/{projectId}`

```
FE (P-27 설정)                                  BE
──────────────────────────────────────────────────────────────
필드 편집 후 [저장]
    │
    ├─ ① 변경된 필드만 body 에 담는다 (PATCH 의미 유지)
    │     변경 없으면 요청 안 보냄
    │
    ├─ ② 날짜 역전 · 금액 음수 1차 검증
    │
    ├─ PATCH /api/v1/projects/12 ─────────→ ① 인증 검증 → 401
    │   {endedOn, contractAmount}           ② 프로젝트 조회 → 404
    │                                       ③ 프로젝트 EDITOR 판정 → 403 PROJECT_EDIT_DENIED
    │                                       ④ 날짜 역전 → 400 · 금액 음수 → 400
    │                                       ⑤ **contract_amount 는 project 한 곳에만** UPDATE (INV-08)
    │                                       ⑥ MASTER/ADMIN 이 미참여 프로젝트를 고쳤으면
    │                                          activity_log.privileged_override = 1
    │                                  ←── 200 {projectId, ...}
    │
    └─ ③ 응답 분기
          200 → 폼 값 갱신 + "저장했습니다" 토스트
                계약금액이 바뀌었으면 **정산 화면 캐시 무효화**
          400 → 인라인 에러
          403 → 편집 UI 를 읽기 전용으로 전환
```

### 1-5. 프로젝트 상태 변경 — `PATCH /api/v1/projects/{projectId}/status`

```
FE (P-27 설정 · 상태 드롭다운)                  BE
──────────────────────────────────────────────────────────────
상태 선택
    │
    ├─ ① 드롭다운에 CLOSED 를 **넣지 않는다** (종결은 별도 버튼)
    │
    ├─ ② 역방향 전이도 **막지 않는다** (PRJ-003 — 되돌릴 일이 실제로 있다)
    │     단, 되돌리는 경우만 확인 모달
    │
    ├─ PATCH .../12/status ───────────────→ ① 인증 검증 → 401
    │   {status:"IN_PROGRESS"}              ② 프로젝트 조회 → 404
    │                                       ③ EDITOR 판정 → 403
    │                                       ④ 값 검증 (CLOSED 오면 거부) → 400 PROJECT_STATUS_INVALID
    │                                       ⑤ status UPDATE · activity_log 기록
    │                                  ←── 200 {projectId, status}
    │
    └─ ③ 응답 분기
          200 → 뱃지 갱신 + 목록 캐시 무효화
          400 → 드롭다운 원복 + 토스트
```

### 1-6. 프로젝트 종결 — `POST /api/v1/projects/{projectId}/close`

```
FE (P-27 설정 · [종결] 버튼)                    BE
──────────────────────────────────────────────────────────────
[종결] 클릭
    │
    ├─ ① 종결 모달 표시 — **사유 선택이 필수 UI 다**
    │     미참여 / 유찰 / 미선정 / 취소 + 상세 메모(선택)
    │     사유 미선택 시 [확인] 비활성 (PRJ-005)
    │     취소 → 종료
    │
    ├─ ② 버튼 비활성화 + 로딩
    │
    ├─ POST .../12/close ─────────────────→ ① 인증 검증 → 401
    │   {closeReasonCode, closeReasonNote}  ② 프로젝트 조회 → 404
    │                                       ③ EDITOR 판정 → 403
    │                                       ④ 사유 누락 → 400 CLOSE_REASON_REQUIRED
    │                                          허용 밖 코드 → 400 CLOSE_REASON_INVALID
    │                                       ⑤ **어느 상태에서든 허용** (PRJ-004)
    │                                       ⑥ status='CLOSED' + 사유 저장 · activity_log 기록
    │                                  ←── 200 {status:"CLOSED", closedAt}
    │
    └─ ③ 응답 분기
          200 → 종결 뱃지 표시. **목록·로그에서 지우지 않는다** (PRJ-004)
                편집 UI 는 읽기 전용으로 전환
          400 → 모달 유지 + 인라인 에러
```

### 1-7. 프로젝트 삭제 — `DELETE /api/v1/projects/{projectId}`

```
FE (P-27 설정 · [삭제] 버튼)                    BE
──────────────────────────────────────────────────────────────
[삭제] 클릭
    │
    ├─ ① 삭제 조건을 **FE 가 먼저 안내한다**
    │     상태≠진행전 또는 스텝>0 → 버튼 비활성 + 툴팁
    │        "진행 전이고 스텝이 없을 때만 삭제할 수 있습니다"
    │
    ├─ ② 확인 모달 ("이 프로젝트를 삭제할까요?")
    │     취소 → 종료
    │
    ├─ DELETE /api/v1/projects/12 ────────→ ① 인증 검증 → 401
    │   Body 없음                           ② 프로젝트 조회 → 404
    │                                       ③ EDITOR 판정 → 403
    │                                       ④ **삭제 조건 재검사** (진행 전 · 스텝/블록 0개)
    │                                          위반 → 409 PROJECT_DELETE_NOT_ALLOWED
    │                                       ⑤ deleted_at SET (하드 삭제 없음 · INV-05)
    │                                  ←── 200 {data:null}
    │
    └─ ③ 응답 분기
          200 → 목록에서 제거 + navigate('/projects')
          409 → "스텝이 있어 삭제할 수 없습니다. **종결로 처리하세요**" 토스트
                + [종결하기] 버튼을 토스트에 함께 노출
          404 → "이미 삭제된 프로젝트입니다" + 목록에서 제거
```

### 1-8. 프로젝트 진척률 조회 — `GET /api/v1/projects/{projectId}/progress`

```
FE (P-22 사이드바 · 개요)                       BE
──────────────────────────────────────────────────────────────
스텝 완료/삭제 후 재조회 (또는 상세와 함께 1회)
    │
    ├─ GET .../12/progress ───────────────→ ① 인증 검증 → 401
    │                                       ② 프로젝트 조회 → 404 · 접근 권한 → 403
    │                                       ③ 완료 스텝 / 전체 스텝 집계
    │                                          ⚠️ **이슈 수를 섞지 않는다** (INV-03)
    │                                       ④ 스텝 0개면 progressRate 를 **응답에서 뺀다**
    │                                  ←── 200 {totalStepCount, doneStepCount, progressRate?}
    │
    └─ ② 응답 분기
          200 → progressRate 있으면 막대 + %
                **없으면 "스텝 없음" 문구** (0% 로 그리면 거짓말 · INV-04 와 같은 이유)
```

### 1-9. 사업 카테고리 연결 — `POST /api/v1/projects/{projectId}/business-categories`

```
FE (P-27 설정 · 카테고리 멀티셀렉트)            BE
──────────────────────────────────────────────────────────────
카테고리 선택 후 [추가]
    │
    ├─ ① 이미 연결된 항목은 **선택 목록에서 제외** (중복 요청 예방)
    │
    ├─ POST .../12/business-categories ───→ ① 인증 검증 → 401
    │   {categoryIds:[1,4]}                 ② 프로젝트 조회 → 404 · EDITOR 판정 → 403
    │                                       ③ 빈 배열 → 400 CATEGORY_IDS_REQUIRED
    │                                       ④ 카테고리 존재 검증 → 404
    │                                       ⑤ INSERT (UNIQUE 위반 → 409)
    │                                  ←── 201 {businessCategories[]}
    │
    └─ ② 응답 분기
          201 → 응답의 **전체 목록으로 교체** (부분 병합 금지)
          409 → "이미 연결된 카테고리입니다" + 목록 재조회
```

### 1-10. 사업 카테고리 해제 — `DELETE .../business-categories/{categoryId}`

```
FE (P-27 설정 · 칩 [x])                         BE
──────────────────────────────────────────────────────────────
칩의 [x] 클릭
    │
    ├─ ① 모달 없이 즉시 요청 (되돌리기 쉬운 동작)
    │     단, 낙관적 제거는 하지 않는다
    │
    ├─ DELETE .../business-categories/4 ──→ ① 인증 검증 → 401
    │                                       ② EDITOR 판정 → 403
    │                                       ③ 연결 행 조회 → 404 BUSINESS_CATEGORY_NOT_LINKED
    │                                       ④ 행 DELETE
    │                                  ←── 200 {data:null}
    │
    └─ ② 응답 분기
          200 → 칩 제거
          404 → 칩 제거 + 조용히 무시 (이미 없는 상태 = 원하는 상태)
```

---

## 2. Member

### 2-1. 참여자 목록 조회 — `GET /api/v1/projects/{projectId}/members`

```
FE (P-27 설정 · 참여자 탭)                      BE
──────────────────────────────────────────────────────────────
탭 진입
    │
    ├─ GET .../12/members ────────────────→ ① 인증 검증 → 401
    │                                       ② 접근 권한 판정 → 403 · 404
    │                                       ③ project_member + employee 조인
    │                                       ④ 퇴사 여부(resigned) 계산
    │                                  ←── 200 {members[]}
    │
    └─ ② 응답 분기
          200 → 표 렌더
                · 퇴사자 → **뱃지 표시** (행을 지우지 않는다)
                · **본인 행은 권한 셀렉트를 비활성**  (PRJ-011)
                · VIEWER 로 열었으면 편집 컬럼 자체를 렌더하지 않음
```

### 2-2. 참여자 추가 — `POST /api/v1/projects/{projectId}/members`

```
FE (P-27 설정 · [참여자 추가])                  BE
──────────────────────────────────────────────────────────────
사원 검색 → 선택 → 권한 선택 → [추가]
    │
    ├─ ① **한 명씩만** 선택 가능한 UI (PRJ-009 · INV-07)
    │     ⛔ 팀·부서 일괄 추가 UI 를 만들지 마라
    │
    ├─ ② 권한 셀렉트는 VIEWER/EDITOR/NONE **3값만** 노출
    │
    ├─ POST .../12/members ───────────────→ ① 인증 검증 → 401
    │   {userId:5, grade:"VIEWER"}      ② EDITOR 판정 → 403 · 프로젝트 → 404
    │                                       ③ grade 값 검증 → 400 MEMBER_PERMISSION_INVALID
    │                                          (MANAGER 등은 여기서 막힌다)
    │                                       ④ 사원 존재 검증 → 404 USER_NOT_FOUND
    │                                       ⑤ 중복 검사 → 409 MEMBER_ALREADY_EXISTS
    │                                       ⑥ project_member 1행 INSERT · 로그 기록
    │                                  ←── 201 {memberId, ...}
    │
    └─ ③ 응답 분기
          201 → 표에 행 추가 + 검색창 초기화
          409 → "이미 참여 중인 사원입니다" 토스트
```

### 2-3. 참여자 권한 변경 — `PATCH .../members/{memberId}`

```
FE (P-27 설정 · 권한 셀렉트)                    BE
──────────────────────────────────────────────────────────────
셀렉트 변경
    │
    ├─ ① **본인 행이면 셀렉트 자체가 비활성** (PRJ-011 · INV-10)
    │
    ├─ ② NONE 선택 시 확인 모달
    │     "이 사용자는 프로젝트를 볼 수 없게 됩니다"
    │
    ├─ PATCH .../members/32 ──────────────→ ① 인증 검증 → 401
    │   {grade:"EDITOR"}                    ② EDITOR 판정 → 403
    │                                       ③ **요청자 == 대상자 검사** → 403 MEMBER_SELF_EDIT_DENIED
    │                                          ⚠️ EDITOR 여도 막는다
    │                                       ④ grade 값 검증 → 400
    │                                       ⑤ 행 조회 → 404 MEMBER_NOT_FOUND
    │                                       ⑥ UPDATE · 로그 기록
    │                                  ←── 200 {memberId, grade}
    │
    └─ ③ 응답 분기
          200 → 셀렉트 확정 + 토스트
          403 MEMBER_SELF_EDIT_DENIED → 셀렉트 원복 + "본인 권한은 변경할 수 없습니다"
```

### 2-4. 참여자 제거 — `DELETE .../members/{memberId}`

```
FE (P-27 설정 · 행 [제거])                      BE
──────────────────────────────────────────────────────────────
[제거] 클릭
    │
    ├─ ① 본인 행이면 버튼 미노출
    │
    ├─ ② 확인 모달 ("참여자를 제거할까요?")
    │
    ├─ DELETE .../members/32 ─────────────→ ① 인증 검증 → 401
    │                                       ② EDITOR 판정 → 403
    │                                       ③ 자기 자신 검사 → 403 MEMBER_SELF_EDIT_DENIED
    │                                       ④ 행 조회 → 404
    │                                       ⑤ DELETE · 로그 기록
    │                                          ⚠️ step_permission 잔여 행 처리 정책 확인 필요
    │                                  ←── 200 {data:null}
    │
    └─ ③ 응답 분기
          200 → 행 제거
          404 → 행 제거 (이미 없음)
```

---

## 3. Stage

### 3-1. 스테이지 목록 조회 — `GET /api/v1/projects/{projectId}/stages`

```
FE (P-22 사이드바)                              BE
──────────────────────────────────────────────────────────────
워크스페이스 진입
    │
    ├─ GET .../12/stages ─────────────────→ ① 인증 검증 → 401 · 접근 권한 → 403 · 404
    │                                       ② sort_order 오름차순 조회
    │                                       ③ 스텝 수 집계
    │                                          ⚠️ 권한·상태 필드는 **없다** (INV-01)
    │                                  ←── 200 {stages[]}
    │
    └─ ② 응답 분기
          200 → 사이드바 트리 렌더
                스테이지에 상태 뱃지·권한 아이콘을 **그리지 마라** (저장하지 않는 값이다)
```

### 3-2. 스테이지 생성 — `POST /api/v1/projects/{projectId}/stages`

```
FE (P-22 사이드바 · [+ 스테이지])               BE
──────────────────────────────────────────────────────────────
이름 입력 → Enter
    │
    ├─ ① 공백 검사 → 인라인 에러
    │
    ├─ POST .../12/stages ────────────────→ ① 인증 검증 → 401 · EDITOR → 403 · 404
    │   {name:"제안"}                       ② name 공백 → 400 STAGE_NAME_REQUIRED
    │                                       ③ sortOrder 미지정 시 max+1 계산
    │                                       ④ INSERT · 로그 기록
    │                                  ←── 201 {stageId, sortOrder}
    │
    └─ ② 응답 분기
          201 → 트리 맨 아래에 추가 + 이름 편집 상태 해제
          400 → 인라인 에러
```

### 3-3. 스테이지 수정 — `PATCH /api/v1/stages/{stageId}`

```
FE (P-22 사이드바 · 더블클릭 인라인 편집)       BE
──────────────────────────────────────────────────────────────
이름 수정 → blur / Enter
    │
    ├─ ① 값이 그대로면 요청 안 보냄
    │
    ├─ PATCH /api/v1/stages/7 ────────────→ ① 인증 검증 → 401 · EDITOR → 403
    │   {name:"제안·계약"}                  ② 스테이지 조회 → 404 STAGE_NOT_FOUND
    │                                       ③ name 공백 → 400
    │                                       ④ UPDATE · 로그 기록 (target_name 은 **변경 후** 이름)
    │                                  ←── 200 {stageId, name}
    │
    └─ ② 응답 분기
          200 → 라벨 확정
          404 → 트리에서 제거 + "삭제된 스테이지입니다"
```

### 3-4. 스테이지 순서 변경 — `PATCH .../stages/order`

```
FE (P-22 사이드바 · 드래그)                     BE
──────────────────────────────────────────────────────────────
드래그 종료(drop)
    │
    ├─ ① 드래그 중에는 **로컬 상태만** 갱신 (부드러운 UX)
    │
    ├─ ② drop 시점에 **전체 순서를 다시 계산해 일괄 전송**
    │     (한 건씩 보내면 중간 상태가 서버에 남는다)
    │
    ├─ PATCH .../12/stages/order ─────────→ ① 인증 검증 → 401 · EDITOR → 403
    │   {orders:[{stageId,sortOrder},...]}  ② 목록 비었/중복 순서 → 400 STAGE_ORDER_INVALID
    │                                       ③ 전부 같은 프로젝트인지 검증
    │                                       ④ **sort_order 만** UPDATE
    │                                          ⛔ 하위 스텝은 건드리지 않는다 (STG-002)
    │                                  ←── 200 {stages[]}
    │
    └─ ③ 응답 분기
          200 → 서버 순서로 확정
          400 → **드래그 이전 순서로 롤백** + 토스트
```

### 3-5. 스테이지 삭제 — `DELETE /api/v1/stages/{stageId}`

```
FE (P-22 사이드바 · 스테이지 [삭제])            BE
──────────────────────────────────────────────────────────────
[삭제] 클릭
    │
    ├─ ① 하위 스텝이 있으면 **이전 대상 선택 모달** (STG-003)
    │     "하위 스텝 3개를 어디로 옮길까요?"
    │     [다른 스테이지 선택 ▾] / [미소속으로 두기]
    │     ⛔ "함께 삭제" 선택지를 **만들지 마라**
    │     미선택 시 [확인] 비활성
    │
    ├─ DELETE /api/v1/stages/7            → ① 인증 검증 → 401 · EDITOR → 403
    │        ?moveToStageId=8               ② 스테이지 조회 → 404
    │                                       ③ 파라미터 누락 → 400 STAGE_MOVE_TARGET_REQUIRED
    │                                       ④ 대상이 타 프로젝트/자기자신 → 400 ..._INVALID
    │                                       ⑤ 하위 스텝 stage_id 일괄 이전
    │                                       ⑥ 스테이지 삭제 · 로그 기록
    │                                  ←── 200 {movedStepCount}
    │
    └─ ② 응답 분기
          200 → 트리 재조회 (스텝이 이동했으므로 부분 갱신 대신 전체 재조회)
                "스텝 3개를 '수행'으로 옮겼습니다" 토스트
          400 → 모달 유지
```

### 3-6. 하위 스텝 권한 일괄 적용 — `POST /api/v1/stages/{stageId}/step-permissions`

```
FE (P-27 설정 · 스테이지 행 [하위 일괄 적용])   BE
──────────────────────────────────────────────────────────────
사원 + 권한 선택 → [적용]
    │
    ├─ ① **"스테이지에 권한을 준다"고 쓰지 마라.**
    │     문구: "이 스테이지의 스텝 3개에 권한을 적용합니다" (INV-01)
    │
    ├─ ② 적용 대상 스텝 수를 미리 보여준다
    │
    ├─ POST .../stages/7/step-permissions → ① 인증 검증 → 401 · EDITOR → 403
    │   {userId:5, grade:"EDITOR"}      ② 스테이지 → 404 · 사원 → 404
    │                                       ③ grade 검증 → 400 STEP_PERMISSION_INVALID
    │                                       ④ **하위 스텝마다 step_permission UPSERT**
    │                                          ⚠️ stage_permission 테이블은 없다
    │                                  ←── 201 {appliedStepCount:3}
    │
    └─ ③ 응답 분기
          201 → "스텝 3개에 적용했습니다" + 스텝 권한 표 재조회
```

---

## 4. Step

### 4-1. 스텝 목록 조회 — `GET /api/v1/projects/{projectId}/steps`

```
FE (P-22 워크스페이스 보드)                     BE
──────────────────────────────────────────────────────────────
워크스페이스 진입 / 스테이지 필터
    │
    ├─ GET .../12/steps?stageId=7 ────────→ ① 인증 검증 → 401 · 접근 권한 → 403 · 404
    │                                       ② step_permission 판정
    │                                          · 행 없음 → 프로젝트 권한 상속 (STP-011)
    │                                          · NONE → **목록에서 제외** (STP-010)
    │                                       ③ 스텝별 이슈 집계 (완료/전체)
    │                                          └ 이슈 0개면 progressRate **미포함**
    │                                       ④ myPermission 계산
    │                                  ←── 200 {steps[]}
    │
    └─ ② 응답 분기
          200 → 카드 렌더
                · progressRate 없으면 **막대 자체를 그리지 않는다** (INV-04)
                · myPermission=VIEWER 인 카드는 편집 핸들 미노출
                ⚠️ "권한 없는 스텝"을 회색으로도 표시하지 마라 — 응답에 아예 없다
```

### 4-2. 스텝 상세 조회 — `GET /api/v1/steps/{stepId}`

```
FE (P-22A 스텝 상세)                            BE
──────────────────────────────────────────────────────────────
스텝 카드 클릭
    │
    ├─ ① 3탭 레이아웃 준비 [블록 | 이슈 | 활동기록]
    │
    ├─ GET /api/v1/steps/10 ──────────────→ ① 인증 검증 → 401
    │                                       ② 스텝 조회 → 404 STEP_NOT_FOUND
    │                                       ③ step_permission 판정 → NONE 이면 403
    │                                       ④ 이슈 집계 · 책임자(employee) 조인
    │                                       ⑤ completedBy/At 포함
    │                                  ←── 200 {stepId, ..., myPermission}
    │
    └─ ② 응답 분기
          200 → 헤더 + myPermission 로 버튼 게이팅
                status=DONE 이면 완료자·완료시각 표시
          403 → "이 스텝에 접근할 수 없습니다" + 보드로 이동
```

### 4-3. 스텝 생성 — `POST /api/v1/projects/{projectId}/steps`

```
FE (P-28 스텝 추가 모달)                        BE
──────────────────────────────────────────────────────────────
[+ 스텝] → 모달 → [추가]
    │
    ├─ ① **템플릿 선택 UI 를 만들지 마라** — v1 은 직접 추가만 (PRJ-V1 §1-2)
    │
    ├─ ② 이름 공백 · 날짜 역전 1차 검증
    │
    ├─ POST .../12/steps ─────────────────→ ① 인증 검증 → 401 · EDITOR → 403 · 404
    │   {name, stageId?, startedOn?,        ② name 공백 → 400 STEP_NAME_REQUIRED
    │    endedOn, ownerUserId}          ③ 날짜 역전 → 400 STEP_DATE_RANGE_INVALID
    │                                       ④ stage/사원 존재 검증 → 404
    │                                       ⑤ **project_id 를 항상 채워서** INSERT (INV-02)
    │                                          status='NOT_STARTED' · sort_order=max+1
    │                                       ⑥ 로그 기록
    │                                  ←── 201 {stepId, ...}
    │
    └─ ③ 응답 분기
          201 → 보드에 카드 추가 + 모달 닫기
                진척률이 바뀌므로 **프로젝트 진척률 재조회**
          400 → 인라인 에러
```

### 4-4. 스텝 수정 — `PATCH /api/v1/steps/{stepId}`

```
FE (P-22A 스텝 헤더 · 속성 편집)                BE
──────────────────────────────────────────────────────────────
속성 편집 → [저장]
    │
    ├─ ① 책임자 셀렉트는 **참여자 목록에서만** 고르게 한다
    │     ⚠️ 책임자 ≠ 작업자 — 라벨을 "책임자"로 정확히 쓴다 (STP-003)
    │
    ├─ PATCH /api/v1/steps/10 ────────────→ ① 인증 검증 → 401
    │   {name, endedOn, ownerUserId}    ② 스텝 조회 → 404
    │                                       ③ **스텝 EDITOR** 판정 → 403 STEP_EDIT_DENIED
    │                                       ④ 날짜 역전 → 400
    │                                       ⑤ stage/사원 존재 검증 → 404
    │                                       ⑥ UPDATE · 로그 기록
    │                                  ←── 200 {stepId, ...}
    │
    └─ ② 응답 분기
          200 → 헤더 갱신
          403 → 편집 UI 를 읽기 전용으로 전환
```

### 4-5. 스텝 순서 변경 — `PATCH .../steps/order`

```
FE (P-22 보드 · 드래그)                         BE
──────────────────────────────────────────────────────────────
스텝 카드 drop (스테이지 간 이동 포함)
    │
    ├─ ① 로컬 상태 먼저 갱신 → drop 시 일괄 전송
    │
    ├─ ② ⚠️ **선행 스텝 미완료를 이유로 막지 마라** (STP-002)
    │     경고 문구도 띄우지 않는다
    │
    ├─ PATCH .../12/steps/order ──────────→ ① 인증 검증 → 401 · EDITOR → 403
    │   {orders:[{stepId,stageId,           ② 목록 비었/중복 순서 → 400 STEP_ORDER_INVALID
    │             sortOrder},...]}          ③ 스텝 존재 검증 → 404
    │                                       ④ stage_id · sort_order UPDATE
    │                                          ⛔ 선행 완료 검사 없음
    │                                  ←── 200 {steps[]}
    │
    └─ ③ 응답 분기
          200 → 서버 순서로 확정
          400 → 드래그 이전으로 롤백
```

### 4-6. 스텝 상태 변경 — `PATCH /api/v1/steps/{stepId}/status`

```
FE (P-22A 상태 셀렉트)                          BE
──────────────────────────────────────────────────────────────
상태 선택
    │
    ├─ ① 셀렉트에 **완료(DONE)를 넣지 않는다**
    │     완료는 [완료 처리] 버튼 → 별도 API (미완료 이슈 선택이 필요)
    │
    ├─ ② 진척률 막대와 **상태는 다른 값**임을 UI 로 구분 (STP-004)
    │
    ├─ PATCH .../steps/10/status ─────────→ ① 인증 검증 → 401 · 스텝 EDITOR → 403
    │   {status:"IN_PROGRESS"}              ② 스텝 조회 → 404
    │                                       ③ 값 검증 (DONE 오면 거부) → 400 STEP_STATUS_INVALID
    │                                       ④ UPDATE · 로그 기록
    │                                  ←── 200 {stepId, status}
    │
    └─ ③ 응답 분기
          200 → 뱃지 갱신
          400 → 셀렉트 원복
```

### 4-7. 스텝 완료 처리 — `POST /api/v1/steps/{stepId}/complete`

```
FE (P-22A [완료 처리] 버튼)                     BE
──────────────────────────────────────────────────────────────
[완료 처리] 클릭
    │
    ├─ ① **미완료 이슈 수를 먼저 보여준다** (STP-006)
    │     "남은 이슈 3개가 있습니다"
    │     ( ) 그대로 두기   ( ) 함께 종료      ← 필수 선택
    │     미선택 시 [확인] 비활성
    │     ⚠️ 이슈가 남아도 **완료를 막지 마라** (STP-005)
    │
    ├─ ② 버튼 비활성화 + 로딩
    │
    ├─ POST .../steps/10/complete ────────→ ① 인증 검증 → 401 · 스텝 EDITOR → 403
    │   {openIssueAction:"KEEP"}            ② 스텝 조회 → 404
    │                                       ③ 값 누락/오류 → 400 OPEN_ISSUE_ACTION_*
    │                                       ④ **미완료 이슈가 있어도 완료 진행**
    │                                          KEEP  → 이슈 유지 + `완료된 스텝` 배지 부여
    │                                          CLOSE → 미완료 이슈 종료
    │                                       ⑤ status='DONE' · completed_by/at 기록
    │                                  ←── 200 {status:"DONE", closedIssueCount}
    │
    └─ ③ 응답 분기
          200 → 상태 갱신 + **프로젝트 진척률 재조회** (분자가 바뀐다)
                KEEP 이었으면 이슈 목록에 배지 반영
          400 → 모달 유지
```

### 4-8. 스텝 삭제 — `DELETE /api/v1/steps/{stepId}`

```
FE (P-22 카드 메뉴 · [삭제])                    BE
──────────────────────────────────────────────────────────────
[삭제] 클릭
    │
    ├─ ① **이슈 처리 방식 선택 모달** (STP-008)
    │     "이 스텝의 이슈 3개를 어떻게 할까요?"
    │     ( ) 다른 스텝으로 이동 [스텝 선택 ▾]
    │     ( ) 함께 종료
    │     미선택 시 [확인] 비활성 — ⛔ 조용히 같이 지우지 않는다
    │
    ├─ DELETE /api/v1/steps/10            → ① 인증 검증 → 401 · EDITOR → 403
    │   ?issueAction=MOVE&moveToStepId=11   ② 스텝 조회 → 404
    │                                       ③ 파라미터 누락 → 400 ISSUE_ACTION_REQUIRED /
    │                                          ISSUE_MOVE_TARGET_REQUIRED
    │                                       ④ **잠금 블록 검사** → 409 STEP_DELETE_LOCKED
    │                                          입금 연결 입금확인 · 진행 중 결재 ·
    │                                          결재 대상 파일 · 계산서 연결 조회
    │                                       ⑤ 이슈 이동/종료 → 스텝 soft delete · 로그
    │                                  ←── 200 {movedIssueCount}
    │
    └─ ② 응답 분기
          200 → 카드 제거 + 진척률 재조회
          409 → "입금이 연결된 블록이 있어 삭제할 수 없습니다.
                 **재무 담당자가 연결을 해제해야 합니다**" 토스트
          404 → 카드 제거
```

---

## 5. StepPermission

### 5-1. 스텝 권한 목록 조회 — `GET /api/v1/steps/{stepId}/permissions`

```
FE (P-27 설정 · 스텝 권한 탭)                   BE
──────────────────────────────────────────────────────────────
스텝 선택
    │
    ├─ GET /api/v1/steps/10/permissions ──→ ① 인증 검증 → 401 · EDITOR → 403 · 404
    │                                       ② 참여자 전원에 대해 판정
    │                                          · step_permission 행 있음 → 그 값 + overridden=true
    │                                          · 행 없음 → 프로젝트 권한 + overridden=false
    │                                  ←── 200 {permissions[]}
    │
    └─ ② 응답 분기
          200 → 표 렌더
                overridden=false → "프로젝트 권한 상속" 회색 라벨 (STP-011)
                overridden=true  → [회수] 버튼 노출
                ⚠️ **행 없음을 "차단"으로 표시하지 마라** — 상속이다
```

### 5-2. 스텝 권한 부여·변경 — `PUT .../permissions/{userId}`

```
FE (P-27 · 권한 셀렉트)                         BE
──────────────────────────────────────────────────────────────
셀렉트 변경
    │
    ├─ ① 본인 행은 셀렉트 비활성 (INV-10)
    │
    ├─ ② NONE 선택 시 안내 문구
    │     "이 스텝이 해당 사용자의 목록에서 사라집니다" (STP-010)
    │
    ├─ PUT .../steps/10/permissions/E2024007 ────→ ① 인증 검증 → 401 · EDITOR → 403
    │   {grade:"NONE"}                      ② 본인 여부 → 403 MEMBER_SELF_EDIT_DENIED
    │                                       ③ grade 검증 → 400 STEP_PERMISSION_INVALID
    │                                       ④ 스텝/사원 존재 → 404
    │                                       ⑤ step_permission UPSERT · 로그 기록
    │                                  ←── 200 {grade, overridden:true}
    │
    └─ ③ 응답 분기
          200 → 라벨을 "오버라이드"로 전환 + [회수] 버튼 노출
```

### 5-3. 스텝 권한 회수 — `DELETE .../permissions/{userId}`

```
FE (P-27 · [회수])                              BE
──────────────────────────────────────────────────────────────
[회수] 클릭
    │
    ├─ ① 확인 모달 문구를 정확히 쓴다
    │     "오버라이드를 지우면 **프로젝트 권한을 따릅니다**" (STP-011)
    │     ⛔ "권한을 없앱니다" 라고 쓰면 오해를 부른다
    │
    ├─ DELETE .../steps/10/permissions/E2024007 →  ① 인증 검증 → 401 · EDITOR → 403
    │                                       ② 행 조회 → 404 STEP_PERMISSION_NOT_FOUND
    │                                       ③ 행 DELETE
    │                                       ④ **상속 결과 등급을 계산해서 응답에 담는다**
    │                                  ←── 200 {grade:"VIEWER", overridden:false}
    │
    └─ ② 응답 분기
          200 → 응답의 grade 로 셀렉트를 갱신 (재조회 불필요)
                라벨을 "프로젝트 권한 상속"으로 전환
```

---

## 6. Block

### 6-1. 스텝 블록 일괄 조회 — `GET /api/v1/steps/{stepId}/blocks`

```
FE (P-22A · 블록 탭)                            BE
──────────────────────────────────────────────────────────────
스텝 상세 진입 (상세 조회와 병렬 호출)
    │
    ├─ GET /api/v1/steps/10/blocks ───────→ ① 인증 검증 → 401 · 스텝 접근 → 403 · 404
    │                                       ② ⭐ 배치 조회 (BLK-006 · 2026-08-05 정정)
    │                                          쿼리1 block ⋈ employee (담당자명)
    │                                          쿼리2 issue_block ⋈ issue 집계 WHERE block_id IN (…)
    │                                          쿼리3+ 스텝에 존재하는 타입별 상세 IN (…)
    │                                          ⛔ 블록 개수에 비례하는 쿼리 금지 (N+1)
    │                                          ⚠️ 어댑터 없는 타입은 detail: null (부분 실패 격리)
    │                                       ③ deleted_at IS NULL 만
    │                                       ④ 연결 이슈 완료/전체 집계
    │                                          └ 삭제된 블록은 집계에서 제외 (BLK-011)
    │                                  ←── 200 {blocks[]}
    │
    └─ ② 응답 분기
          200 → 3열 그리드 렌더 (rowIndex → sortOrder → colSpan)
                · **블록에 상태 뱃지를 그리지 마라** — status 필드가 없다 (BLK-005)
                · 카드 우하단에 `완료/전체` 배지 (BLK-011)
                · `owner` 가 있으면 담당자 아바타 표시 (BLK-012 · 없으면 비운다)
                · detail 은 타입별 컴포넌트로 분기
                · ⛔ `typeId` 는 응답에 없다 — 서버 내부 매핑용이다 (ERD §0-12)
```

### 6-2. 블록 생성 — `POST /api/v1/steps/{stepId}/blocks`

```
FE (P-22A · [+ 블록])                           BE
──────────────────────────────────────────────────────────────
블록 타입 선택 → [추가]
    │
    ├─ ① 타입 선택지는 **ERD enum 고정 목록 10종** (BLK-001)
    │     ⛔ MEMO 는 **폐기됐다** — 목록에 넣지 마라. 자유 서술은 TEXT 다 (2026-08-03)
    │     ⛔ "커스텀 블록" · JSON 스키마 입력 UI 를 만들지 마라
    │
    ├─ ② colSpan 은 1~3 만 선택 가능 (총 3열 고정)
    │
    ├─ POST /api/v1/steps/10/blocks ──────→ ① 인증 검증 → 401 · 스텝 EDITOR → 403 · 404
    │   {type, title, owner?, rowIndex,     ② type enum 검증 → 400 BLOCK_TYPE_INVALID
    │    sortOrder, colSpan}                ③ colSpan 1~3 → 400 BLOCK_COL_SPAN_INVALID
    │                                       ④ **스텝당 1개 타입 2종** 검사
    │                                          · PAYMENT_CONFIRM  → 409 PAYMENT_CONFIRM_BLOCK_DUPLICATED (PCB-001B)
    │                                          · TAX_INVOICE_VIEW → 409 TAX_INVOICE_VIEW_BLOCK_DUPLICATED (TXL-001B)
    │                                       ⑤ owner 사번 존재 검증 → 404 USER_NOT_FOUND (BLK-012 · 선택 입력)
    │                                       ⑥ block INSERT (step_id NOT NULL · type_id = NULL)
    │                                       ⑦ 타입별 상세 행 INSERT (block_id = ⑥)
    │                                       ⑧ block UPDATE SET type_id = ⑦의 PK · 로그 기록
    │                                          ⚠️ ⑥⑦⑧ 은 한 트랜잭션 (FK 가 없다)
    │                                  ←── 201 {blockId, ...}
    │
    └─ ③ 응답 분기
          201 → 그리드에 카드 추가
          409 PAYMENT_CONFIRM_BLOCK_DUPLICATED
              → "이 스텝에는 이미 정산 회차가 있습니다.
                 회차를 늘리려면 **스텝을 새로 만드세요**" (INV-07C)
          409 TAX_INVOICE_VIEW_BLOCK_DUPLICATED
              → "이 스텝에는 이미 세금계산서 조회 블록이 있습니다" (TXL-001B)
```

⚠️ **스텝당 1개 제약은 두 타입 모두다.** `TAX_INVOICE_VIEW` 를 빼면 정산현황 `rounds[].taxInvoice`(단수)가
어느 계산서인지 알 수 없어진다 → [`../재무관리/TAX-V1.md`](../재무관리/TAX-V1.md) INV-08 · [`../재무관리/FIN-V1-API.md`](../재무관리/FIN-V1-API.md) §1-2

### 6-3. 블록 배치 변경 — `PATCH .../blocks/layout`

```
FE (P-22A · 드래그 재배치)                      BE
──────────────────────────────────────────────────────────────
블록 drop
    │
    ├─ ① 드래그 중 중복 sortOrder 가 생겨도 **로컬에서 막지 않는다**
    │     DB 에 UNIQUE 가 없어서 허용된 설계다 (BLK-004)
    │
    ├─ ② drop 시 그 스텝의 **전체 배치를 다시 계산해 일괄 전송**
    │
    ├─ PATCH /api/v1/steps/10/blocks/layout → ① 인증 검증 → 401 · 스텝 EDITOR → 403
    │   {layouts:[{blockId,rowIndex,          ② colSpan 1~3 → 400
    │              sortOrder,colSpan},...]}   ③ 다른 스텝 블록 섞임/빈 목록
    │                                            → 400 BLOCK_LAYOUT_INVALID
    │                                         ④ 블록 존재 → 404
    │                                         ⑤ 배치 컬럼만 UPDATE
    │                                    ←── 200 {blocks[]}
    │
    └─ ③ 응답 분기
          200 → 서버 배치로 확정
          400 → 드래그 이전 배치로 롤백
```

### 6-4. 블록 삭제 — `DELETE /api/v1/blocks/{blockId}`

```
FE (P-22A · 블록 메뉴 [삭제])                   BE
──────────────────────────────────────────────────────────────
[삭제] 클릭
    │
    ├─ ① 확인 모달 ("이 블록을 삭제할까요?")
    │
    ├─ ② 잠금 가능성이 있는 타입(입금확인·결재·파일·계산서조회)은
    │     모달에 사전 안내 문구를 붙인다
    │
    ├─ DELETE /api/v1/blocks/21 ──────────→ ① 인증 검증 → 401 · 스텝 EDITOR → 403
    │                                       ② 블록 조회 → 404 BLOCK_NOT_FOUND
    │                                       ③ **잠금 4종 검사** → 409 BLOCK_DELETE_LOCKED
    │                                          ① 입금 연결 입금확인 ② 진행 중 결재
    │                                          ③ 결재 대상 파일 ④ 계산서 연결 조회
    │                                       ④ deleted_at SET (**하드 삭제 없음** · INV-05)
    │                                          연결 행은 유지 — 로그가 고아가 되면 안 된다
    │                                  ←── 200 {data:null}
    │
    └─ ③ 응답 분기
          200 → 그리드에서 제거
          409 → "재무가 연결을 해제해야 삭제할 수 있습니다" 토스트
                ⚠️ 실무자에게 **해제 버튼을 주지 마라** (INV-06)
          404 → 그리드에서 제거
```

---

## 7. IssueBlock

### 7-1. 이슈-블록 연결 — `POST /api/v1/blocks/{blockId}/issues`

```
FE (P-22A · 블록 카드 [이슈 연결])              BE
──────────────────────────────────────────────────────────────
[이슈 연결] 클릭 → 이슈 선택
    │
    ├─ ① **선택 목록을 같은 스텝의 이슈로 제한한다** (BLK-009)
    │     다른 스텝 이슈를 보여주면 400 을 유도하는 UI 가 된다
    │
    ├─ ② 이미 연결된 이슈는 목록에서 제외 (BLK-010)
    │
    ├─ POST /api/v1/blocks/15/issues ─────→ ① 인증 검증 → 401 · 스텝 EDITOR → 403
    │   {issueId:101}                       ② 블록/이슈 조회 → 404
    │                                       ③ **block.step_id == issue.step_id 검증**
    │                                          → 400 ISSUE_BLOCK_STEP_MISMATCH
    │                                          ⚠️ DB 제약으로 못 걸어 앱이 막는다
    │                                       ④ UNIQUE(issue_id, block_id) → 409
    │                                       ⑤ issue_block INSERT
    │                                       ⑥ 완료/전체 재집계해서 응답
    │                                  ←── 201 {linkedIssueTotal, linkedIssueDone}
    │
    └─ ③ 응답 분기
          201 → 응답 집계로 **배지 즉시 갱신** (재조회 불필요)
          400 → "같은 스텝의 이슈만 연결할 수 있습니다"
          409 → "이미 연결된 이슈입니다" + 목록 갱신
```

### 7-2. 이슈-블록 연결 해제 — `DELETE .../issues/{issueId}`

```
FE (P-22A · 연결 목록 [x])                      BE
──────────────────────────────────────────────────────────────
[x] 클릭
    │
    ├─ ① 모달 없이 즉시 요청 (되돌리기 쉬운 동작)
    │
    ├─ DELETE /api/v1/blocks/15/issues/101 → ① 인증 검증 → 401 · 스텝 EDITOR → 403
    │                                        ② 연결 행 조회 → 404 ISSUE_BLOCK_NOT_FOUND
    │                                        ③ DELETE
    │                                        ④ 완료/전체 재집계
    │                                   ←── 200 {linkedIssueTotal, linkedIssueDone}
    │
    └─ ② 응답 분기
          200 → 배지 갱신 + 목록에서 제거
          404 → 목록에서 제거 (이미 없음)
```

---

## 8. ActivityLog

### 8-1. 활동기록 조회 — `GET /api/v1/projects/{projectId}/activity-logs`

```
FE (P-26 활동기록 / P-22A 활동기록 탭)          BE
──────────────────────────────────────────────────────────────
탭 진입 / 무한 스크롤
    │
    ├─ ① **삭제 버튼을 만들지 마라** — 로그 삭제 API 가 없다 (USC-LOG-007)
    │
    ├─ GET .../12/activity-logs?stepId= ──→ ① 인증 검증 → 401 · 접근 권한 → 403 · 404
    │                                       ② project_id 기준 조회 (최신순)
    │                                       ③ stepId 있으면 스텝 필터
    │                                       ④ **target_name 은 저장된 스냅샷 그대로** (INV-09)
    │                                          ⛔ 현재 이름으로 조인해 덮어쓰지 않는다
    │                                  ←── 200 {content[], totalElements}
    │
    └─ ② 응답 분기
          200 → 타임라인 렌더
                · 문구는 **targetName 스냅샷을 그대로 표시** (과거 이름이 맞다)
                · privilegedOverride=true → `상위권한으로 수정` 뱃지 (PRJ-017)
                · blockId=null → 블록 밖 사건 (스텝/프로젝트 레벨)
                · act 값 → 아이콘 매핑 ✅ CREATE·UPDATE·DELETE·COMPLETE·MOVE (ERD 확정)
```

---

## 9. 🚧 FE 가 특히 조심할 것 — 요구사항이 UI 를 직접 규정한 지점

| # | 규칙 | 어기면 |
|---|------|--------|
| 1 | **진척률이 없으면 0% 로 그리지 마라** (PRJ-013 · INV-04) | `0/0` 을 100% 로도 0% 로도 읽을 수 있어 둘 다 거짓말이 된다 |
| 2 | **팀·부서 일괄 추가 UI 금지** (PRJ-009 · INV-07) | 인턴 배정 즉시 전 프로젝트 견적·단가가 노출된다 |
| 3 | **본인 권한 행은 항상 비활성** (PRJ-011 · INV-10) | VIEWER 가 자기를 EDITOR 로 올려 권한 체계가 장식이 된다 |
| 4 | **스테이지에 상태·권한 UI 금지** (STG-004 · INV-01) | 저장하지 않는 값이라 새로고침하면 사라진다 |
| 5 | **스텝 삭제·스테이지 삭제 시 선택을 강제** (STG-003 · STP-008) | 하위 항목이 조용히 사라진다 |
| 6 | **블록에 상태 뱃지 금지** (BLK-005) | `block.status` 컬럼이 없다 |
| 7 | **이슈 연결 목록을 같은 스텝으로 제한** (BLK-009) | 400 을 유도하는 UI 가 된다 |
| 8 | **잠금 409 에 "재무에게 요청" 안내를 붙여라** (INV-06) | 실무자가 해제 방법을 못 찾고 스텝을 지우려 든다 |
| 9 | **활동기록 이름은 스냅샷 그대로** (INV-09) | 현재 이름으로 덮으면 과거 로그가 전부 거짓이 된다 |
| 10 | **템플릿 선택 UI 금지** (PRJ-V1 §1-2) | `template` 테이블이 확정 ERD 에 없다 |

