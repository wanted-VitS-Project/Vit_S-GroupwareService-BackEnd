# 데모 덤프 제작 플레이북

**목적**: 시연용 더미 데이터를 처음부터 다시 만들 때, 주제만 갈아끼우면 같은 품질로 나오게 한다.
**근거**: 2026-08 VitaminS 26 S/S 데모 덤프를 만들며 실제로 걸린 함정과 내린 판단을 그대로 옮겼다.
**대상 파일**: `src/main/resources/db/demo/*.sql`

---

## §0 TL;DR

- 이 문서가 정하는 것: 시연용 더미 데이터 SQL 덤프를 처음부터 다시 만들 때 따라야 할 순서·불변식·문장 규칙 — 주제만 갈아끼우면 같은 품질로 재사용하는 플레이북.
- ⚠️ 조용히 깨지는 함정: **블록 배치(§1-6)를 내용보다 먼저 확정**하지 않으면 `col_span` 합이 안 맞아 전부 다시 써야 한다(§5-⑥) · **활동 로그(08번 파일)는 `deleted_at` 이 없어 되돌릴 수 없다** — 다른 시드를 다 넣은 뒤 반드시 맨 마지막에 실행한다.

| 섹션 | 내용 |
|---|---|
| §0 | 붙여넣어 쓰는 프롬프트 템플릿 — 주제만 바꿔 재사용 |
| §1 | 계획 순서 7단계 — 순서를 바꾸면 뒤에서 되돌아간다 |
| §2 | 파일 분할과 실행 순서(01~09) — 활동 로그는 반드시 마지막 |
| §3 | 테이블별 체크리스트 — 계정·조직 · 프로젝트/스텝/블록 · 결재 · 재무 · 이슈/파일 |
| §4 | 불변식 6개 — 어기면 조용히 깨진다 |
| §5 | 실제로 걸렸던 함정 10개(①~⑩) |
| §6~§7 | 문장 작성 규칙(AI 말투 제거) · 검증 쿼리(전부 0이어야 한다) |
| §8 | 기준선 — 2026-08 덤프 실제 수치 |

---

## 0. 붙여넣어 쓰는 프롬프트

아래를 그대로 복사해 `{{ }}` 만 채운다. **§4 불변식과 §5 함정은 지우지 말 것** — 이게 없으면 매번 같은 곳에서 깨진다.

````text
시연용 데모 덤프를 만든다. 아래 조건으로 SQL 파일을 만들고 로컬 DB에 적용한 뒤 검증까지 해줘.

## 주제
- 회사: {{예: 자체 브랜드 + OEM 병행 의류회사, 12명}}
- 메인 프로젝트: {{예: 무신사 스토어 입점 및 26 S/S 시즌 운영}}
- 기간 축: {{예: 2025-11 ~ 2026-04-08(컷오프), 시즌은 26 S/S}}
- 진행 상태: {{예: IN_PROGRESS, 2차 정산까지 끝나고 3차 진행 중}}

## 규모
- 계정: MEMBER {{6}} / ADMIN {{6}} / MASTER {{1}}
  ※ 역할별로 막히는 화면이 있다(예: ADMIN 은 결재·내 프로젝트 접근 불가). **역할을 정하기 전에
     `.ai/api/*.md` 와 `*Policy.java` 에서 그 역할이 무엇을 못 하는지 먼저 확인하고 알려줘.**
- 프로젝트: 총 {{10}}개. 그중 내용이 꽉 찬 것 {{2}}개, 나머지는 목록용
- 계정마다 기안 결재 {{3}}건 이상
- 입출금(cash_flow) {{20}}건 이상, 연결됨/미연결/연결제외 3상태 전부
- 세금계산서 {{20}}건 이상, 매출(INCOME)과 매입(OUTCOME) 둘 다
- 메인 프로젝트에 **외주 관리 스테이지**를 넣고 **출금(OUTCOME 정산 블록)** 을 연결할 것
  ({{예: 봉제 발주 회차별 / 촬영·상세페이지·물류 용역별}})
- 이슈는 모든 스텝에 붙을 것
- 문서는 v2·v3 버전이 있는 것이 여러 건

## 반드시 지킬 것
1. 시작 전에 **실제 DB 스키마를 조회**해라. ERD 문서와 마이그레이션이 어긋나 있는 테이블이 있다.
   `DESC {table}` 로 컬럼을 확인하고, 문서에 있는데 실제로 없는 컬럼은 설계에서 뺀다.
2. ID는 9000번대를 예약해서 쓰고, 파일마다 되돌리기 DELETE 문을 주석으로 남긴다.
3. 아래 불변식을 어기면 화면이 조용히 깨진다. 마지막에 검증 쿼리로 전부 0인지 확인해라.
   - 블록 행마다 col_span 합 = 3
   - issue_block 은 같은 스텝의 블록만 가리킨다
   - approval 은 block 이 있어야 목록에 뜬다 (INNER JOIN)
   - approval_line 은 approval 이 아니라 approval_revision 에 붙는다
   - file_version.version_no 는 file 안에서 1부터 연속
   - image.order_index 는 블록 안에서 0부터 연속
   - 결재 상태가 스텝 상태와 모순되면 안 된다 (NOT_STARTED 스텝에 COMPLETED 결재 금지)
   - settlement_block.actual_amount = 연결된 cash_flow 합계 (PARTIAL/COMPLETED 판정 근거)
4. 본문 문장은 실무자가 쓴 것처럼 써라. 문서가 자기 자신(블록·스텝·데모)을 설명하면 안 되고,
   마크다운 표는 렌더링이 안 되니 쓰지 마라. 자세한 규칙은 플레이북 §6.
5. 커밋·푸시는 하지 마라.
````

주제만 바꿔 재사용한 예:

| 바꾼 것 | 26 S/S 판 | 다른 주제 예시 |
|---|---|---|
| 회사 | 의류 브랜드 12명 | 건설 시공사 20명 / SI 개발사 15명 |
| 메인 프로젝트 | 무신사 입점·시즌 운영 | 관공서 조달 수주 / 사옥 리모델링 |
| 기간 축 | 25-11 ~ 26-04 | 착공~준공 / 공고~검수 |
| 정산 구조 | 위탁판매 익월 10일 | 기성 청구 3회차 / 검수 후 30일 |
| 실적 지표 | GMV·판매율·반품율 | 공정률·기성률 / 산출물 검수율 |

---

## 1. 계획 순서 — 이 순서를 바꾸면 뒤에서 되돌아간다

| # | 단계 | 산출물 | 왜 여기인가 |
|:-:|---|---|---|
| 1 | **실제 스키마 조회** | 테이블별 컬럼 목록 | ERD와 실제가 다르다. §5-① 참고 |
| 2 | **시간 축 확정** | 시작일·컷오프·상태 | 여기가 틀리면 전부 다시. §5-② |
| 3 | **인원·역할 표** | user_id / 이름 / 부서 / 직급 / role | 모든 테이블의 `created_by` 가 여기서 나온다 |
| 4 | **프로젝트 목록** | id / 이름 / 상태 / 기간 / 내용 유무 | 스텝·블록 물량이 여기서 정해진다 |
| 5 | **스텝 뼈대** | 스텝별 이름·상태·담당 | 블록은 스텝 없이 못 만든다 |
| 6 | **블록 배치 먼저** | 스텝별 row/col_span 표 | 내용 쓰고 배치하면 합이 안 맞아 다시 쓴다 |
| 7 | **내용 작성** | TEXT·체크리스트·파일·이미지 | 마지막. 여기가 제일 오래 걸린다 |

> ⚠️ 6번을 건너뛰면 반드시 되돌아온다. 이번에도 배치를 나중에 잡느라 `10_layout.sql` 을 따로 만들었다.

---

## 2. 파일 분할과 실행 순서

한 파일에 다 넣지 마라. 중간에 하나 틀리면 통째로 다시 돌려야 한다.

```
01_master     회사 · 부서 · 직급 · 사원          ← 다른 모든 것의 FK 뿌리
02_project    프로젝트 · 스테이지 · 스텝 · 멤버
03_blocks     블록 + 상세(text/checklist/image/settlement)
04_files      file · file_version · block_file
05_issues     issue · issue_assign · issue_block
06_finance    cash_flow · tax_invoice
07_approval   approval · revision · line · document
08_activity   활동 로그 · 알림                   ← ⛔ 반드시 맨 마지막
09_page_perm  화면 접근 권한
```

**08을 마지막에 두는 이유**: `activity_log` 에는 `deleted_at` 이 없다. 되돌릴 수단이 없으니 데이터가 다 들어간 뒤 한 번만 실행한다.

**후속 패치는 뒤에 얹는다.** 01~09를 고치는 대신 `10_`, `11_`… 로 번호를 이어 붙이면 앞 파일을 다시 안 건드려도 된다. 다만 패치는 앞 파일의 **결과 상태를 전제**하므로, 파일 머리에 전제를 적어라.

```sql
-- ⚠️ 이 파일이 전제하는 것
--   file 9001·9003 이 v1 까지만 있음  → 어기면 version_no 가 중복된다
--   img_block 9001~9008 의 order_index 가 0부터 연속 → 어기면 갤러리 순서가 겹친다
```

**Flyway가 아니다.** `spring.flyway.locations: classpath:db/migration` 이라 `db/demo` 는 자동 적용되지 않는다. 수동으로 돌린다.

```bash
mysqlsh --sql --host=localhost --user=vitas --schema=vitaS --file=01_master.sql
```

---

## 3. 테이블별 체크리스트 — 무엇이 중점적으로 들어가야 하나

### 3-1. 계정 · 조직

| 테이블 | 꼭 넣을 것 | 흔한 실수 |
|---|---|---|
| `employee` | 부서·직급이 실제 역할과 맞을 것 | 재무 담당이 없어서 정산 결재선이 이상해진다 |
| `account` | `role` 배분, `must_change_password=0`, `terms_agreed_at` 채움 | 이 둘을 비우면 로그인 후 강제 변경·약관 화면에 막힌다 |
| `page_permission` | `MY_PROJECT` 전원 · `ADMIN_CONSOLE` 은 ADMIN/MASTER | 없으면 메뉴가 안 보인다 |

**user_id 규칙**: `companyCode + "-" + baseUserId` (예: `vitawear-VW101`). 앱이 회사코드로 테넌트를 가르므로 접두사만 바꾸면 안 되고 **회사 자체를 새로 만들어야** 한다.

**비밀번호**: Argon2id(m=65536, t=3, p=1). 계정마다 `encode()` 를 따로 불러 salt 를 다르게 한다. 같은 해시를 복붙하면 비밀번호가 같다는 게 해시만 봐도 드러난다.
평문은 **어떤 커밋 파일에도 쓰지 않는다.**

### 3-2. 프로젝트 · 스텝 · 블록

| 테이블 | 꼭 넣을 것 |
|---|---|
| `project` | `status` 를 다양하게 (IN_PROGRESS / COMPLETED / NOT_STARTED / CLOSED) |
| `project_member` | **한 팀을 보여주려면 같은 사람들을 전 프로젝트에 넣는다.** `permission` 은 EDITOR/VIEWER 섞기 |
| `stage` | 스텝을 3~6개 묶음으로. 없어도 되지만 있으면 화면이 정리된다 |
| `step` | `status` 가 프로젝트 상태와 모순 없게 |
| `block` | 타입을 골고루 (TEXT/CHECKLIST/FILE/IMAGE/APPROVAL/SETTLEMENT/AI) |

**블록 다형성** — FK가 **양쪽 다 없다**. 앱이 정합성을 전부 책임진다.

```
block.type    = 'CHECKLIST'      (판별자)
block.type_id = 9101             ──┐
                                   ├─ 둘 다 맞아야 화면에 뜬다
checklist_block.block_id = 9202  ──┘
```

| block.type | 상세 테이블 | type_id 가 가리키는 PK |
|---|---|---|
| TEXT | `text` | `txt_id` |
| CHECKLIST | `checklist_block` | `chk_block_id` |
| IMAGE | `image_block` | `img_block_id` |
| APPROVAL | `approval` | `approval_id` |
| SETTLEMENT | `settlement_block` | `settle_id` |
| FILE | (없음 — `block_file` 조인) | `NULL` |

### 3-3. 결재 — 제일 많이 틀리는 곳

```
approval (block_id UNIQUE)
   └─ approval_revision (회차)
         └─ approval_line (결재선)      ← ⚠️ approval 이 아니라 revision 에 붙는다
         └─ approval_document → file_version
```

- 목록 쿼리가 `approval → block → step → project` **INNER JOIN** 이다. **블록 없는 결재는 목록에 안 뜬다.**
- 그래서 `계정 N명 × 3건` 을 만들려면 **APPROVAL 블록도 그만큼** 필요하다. 블록 물량을 먼저 계산해라.
- 회사 격리는 **기안자 소속**으로 판정한다 (`drafter.company_id`).

**⭐ 목록에 뜨는 조건은 딱 두 가지다.** 프로젝트 멤버라는 사실은 아무 상관이 없다.

| `scope` | 조건 | 안 채우면 |
|---|---|---|
| `drafted` (기본) | `approval.user_id` = 나 (또는 `acting_drafter_id`) | "내가 올린 결재" 가 빈다 |
| `pending` | 현재 회차의 내 `approval_line.status = 'ACTIVE'` | **"결재 대기" 가 빈다** ← 놓치기 쉽다 |
| `all` | MASTER 전용 | 그 외는 403 |

`pending` 을 채우려면 **IN_PROGRESS 결재**가 있어야 하고, 그 현재 회차에 `ACTIVE` 라인이 있어야 한다.
COMPLETED 만 잔뜩 만들면 모든 계정의 결재 대기 탭이 0이 된다. 이번에 실제로 그랬다 —
13명 중 ACTIVE 를 가진 사람이 2명뿐이었다.

계정마다 `ACTIVE` 를 하나씩 배분하려면 그만큼 IN_PROGRESS 결재가 필요하다 (결재 1건에 ACTIVE 는 1명).
IN_PROGRESS **스텝**에만 붙여야 한다.
- 상태 조합을 반드시 맞춘다.

| 스텝 status | approval.status | line.status |
|---|---|---|
| DONE | COMPLETED | 전원 APPROVED |
| IN_PROGRESS | IN_PROGRESS | 앞 단계 APPROVED, 현재 단계 **ACTIVE** |
| NOT_STARTED | DRAFT | 전원 DRAFT, `submitted_at` NULL |

- **반려 이력을 하나는 넣어라.** rev1 REJECTED → rev2 COMPLETED. 버전 화면이 살아난다.

### 3-4. 재무

| 테이블 | 성격 |
|---|---|
| `cash_flow` | 회사 단위 통장 원장. 프로젝트에 안 묶여 있다 |
| `tax_invoice` | 회사 단위 계산서 원장 |
| `settlement_block` | 프로젝트 정산 블록. `cash_flow.settle_block_id` 로 **연결**한다 |

**3상태를 전부 만들어라.** 이게 재무 화면의 핵심 시연 포인트다.

| 상태 | 조건 |
|---|---|
| 연결됨 | `settle_block_id IS NOT NULL` |
| 미연결 | `settle_block_id IS NULL AND is_excluded = 0` ← "연결하기" 버튼이 뜬다 |
| 연결 제외 | `is_excluded = 1` ← 이자·임대료처럼 정산과 무관한 건 |

`settlement_block` 의 UNIQUE 는 `block_id` 뿐이다. → **한 스텝에 회차 블록 여러 개**를 나란히 둘 수 있다. 회차마다 스텝을 만들지 마라.

#### ⭐ 들어오는 돈만 만들지 마라 — OUTCOME 을 반드시 넣는다

처음 덤프를 만들면 정산 블록이 전부 `INCOME`(받을 돈)만 되기 쉽다. 그러면 프로젝트 손익이 반쪽이고,
통장의 출금은 프로젝트에 안 붙은 채 원장에만 떠 있는다.

`settlement_block.type` 과 `tax_invoice.type` 은 둘 다 `enum('INCOME','OUTCOME')` 이다. **같은 테이블로 수입과 지출을 다 쓴다.**

| type | 뜻 | 대표 사례 | 세금계산서 |
|---|---|---|---|
| `INCOME` | 받을 돈 | 플랫폼 정산금, 수주 대금 | 매출 계산서 (`buyer_name` = 상대방) |
| `OUTCOME` | 줄 돈 | **외주비**, 발주 대금, 용역비, 물류비 | 매입 계산서 (`buyer_name` = 우리 회사) |

**외주 관리 섹션을 하나 만들어라.** 스테이지 하나에 스텝 2~3개면 충분하다.

```
스테이지 "외주 관리"
 ├─ 외주 업체 계약      TEXT · TEXT · CHECKLIST / FILE(계약서·단가표)
 ├─ 생산 외주비 지급    TEXT · TEXT · CHECKLIST / SETTLEMENT ×3(회차별) / FILE
 └─ 용역 외주비 지급    TEXT · TEXT · CHECKLIST / SETTLEMENT ×3(용역별) / SETTLEMENT · FILE
```

정산 블록을 **무엇 단위로 끊을지**가 설계의 핵심이다.

| 기준 | 언제 |
|---|---|
| **회차 단위** (1차/2차/3차 발주 대금) | 출금이 "선금 / 잔금" 으로 끊겨 있을 때 ← 대부분 이쪽 |
| 업체 단위 (A공장/B공장/C공장) | 업체별로 따로 청구서가 올 때 |
| 용역 단위 (촬영/디자인/물류/검수) | 성격이 다른 용역을 섞어 쓸 때 |

> ⚠️ **기존 출금 데이터에 블록 기준을 맞춰라.** 이번에 공장 단위로 쪼개려다, 이미 넣어둔
> "1차 발주 선금 36,210,000 / 잔금 36,210,000" 과 금액이 안 맞아 전부 고쳐야 했다. 회차 단위로 바꿔 해결했다.

상태는 **연결된 출금 합계와 반드시 일치**해야 한다 (§5-⑧).

| `status` | 조건 |
|---|---|
| `COMPLETED` | 연결 합계 = 계약액 |
| `PARTIAL` | 0 < 연결 합계 < 계약액 (예: 계약금만 지급) |
| `WAITING` | 지급 예정, 연결 없음 |
| `PENDING` | 금액 자체가 미정 (`total_amount` NULL) |

### 3-5. 이슈 · 파일 · 이미지

| 테이블 | 규칙 |
|---|---|
| `issue` | 스텝 status 와 모순 없게. 단 **완료 스텝에 미완 이슈 1건**은 일부러 남겨라 (배지 시연) |
| `issue_block` | **같은 스텝의 블록만.** 몇 건은 일부러 미연결로 남겨라 |
| `file_version` | `version_no` 는 file 안에서 1부터 연속. `comment` 에 **무엇이 바뀌었는지** 적는다 |
| `image` | `order_index` 는 블록 안에서 0부터 연속 |

`comment` 예시 — 좌측처럼 쓰면 버전 화면이 죽는다.

| 나쁜 예 | 좋은 예 |
|---|---|
| 수정 | inch 로 적힌 5건을 cm 로 통일 |
| 최종본 | 118건 전부 통과. 최종본 |
| v2 | 니트 3종 원단이 단종돼 대체 원단 스펙으로 교체 |

---

## 4. 불변식 — 어기면 조용히 깨진다

| 코드 | 규칙 | 깨지면 |
|---|---|---|
| BLK-003 | 한 `row_index` 의 `col_span` 합 = 3 | 그리드가 밀리거나 빈칸이 생긴다 |
| BLK-009 | `issue_block` 은 같은 스텝의 블록 | 다른 스텝 이슈가 섞여 보인다 |
| INV-04 | 이슈 0건인 스텝은 진행률 % 를 안 띄운다 | 0% 로 오해된다 |
| — | `approval.block_id` 없으면 목록 누락 | 만들었는데 안 보인다 |
| — | `version_no` 1부터 연속 | 버전 드롭다운이 비거나 중복된다 |
| — | `order_index` 0부터 연속 | 갤러리 순서가 겹친다 |

---

## 5. 실제로 걸렸던 함정

### ① ERD 문서와 실제 스키마가 다르다
`activity_log` 는 마이그레이션에서 `project_id`·`resource_type`·`target_name`·`privileged_override` 가 **삭제**됐고 `block_id` 가 NOT NULL 이 됐으며 `act` 는 소문자 enum이다. 문서 기준으로 96건을 설계했다가 전부 못 쓰고 블록 스코프 34건으로 줄였다.
→ **작업 전 `DESC` 로 실제 컬럼을 확인해라.** 문서를 믿지 마라.

### ② 시간 축이 시나리오의 전제다
"정산 완료본"을 보여주려면 판매가 이미 끝나 있어야 한다. 시즌을 F/W로 잡으면 판매·정산이 미래라 물리적으로 불가능하다.
→ **컷오프 날짜를 먼저 못 박고**, 그 뒤 사건은 전부 미래형(예정)으로 쓴다.

### ③ 서류 시점 역설
결재 대상 파일이 "계약서_서명본" 이면 "서명하고 나서 승인받은" 게 된다.
→ v1 최종안(결재 대상) / v2 서명본(승인 후 업로드)으로 쪼갠다. 덤으로 "대상보다 새 버전 있음" 배지도 살아난다.

### ④ 마크다운 표가 렌더링되지 않는다
`remark-gfm` 이 적용돼 있지 않아 표가 한 줄로 뭉개진다. 단일 개행도 무시된다.
→ 표 대신 `**굵게**` + 빈 줄 문단 + `- ` 목록. 문단은 반드시 **빈 줄**로 끊는다.

### ⑤ col_span 이 글자 수를 정한다
`col_span 1` = 약 297px. 여기에 400자를 넣으면 세로로 길게 늘어진다.
→ TEXT는 col_span 1에 **150자 내외**. 넓게 둘 건 APPROVAL·IMAGE 다.

### ⑥ 화면에서 손으로 고치면 배치가 깨진다
시연 준비 중 UI에서 블록을 지우고 새로 추가하면 `col_span` 합이 3이 아니게 된다.
→ 레이아웃 SQL을 다시 돌리기 전에 어긋난 행부터 확인해라 (§7).

### ⑦ 정산 status 와 연결 합계가 어긋난다
`status='PARTIAL'` 로 적어놓고 선금·잔금을 **둘 다** 연결하면 합계는 계약액과 같아진다. 화면에는 "일부 지급"이라 뜨는데 숫자는 완납이다.
컴파일도 되고 예외도 안 나고, **금액만 조용히 틀린다.**
→ §7 대조 쿼리로 `actual_amount` = `SUM(연결된 cash_flow.amount)` 를 반드시 확인해라. 이번에 2건이 걸렸다.

### ⑧ 역할이 화면을 막으면 데이터로는 못 고친다 ⭐
"계정마다 결재 3건" 을 맞추려고 ADMIN 6명에게 24건을 기안시켰는데, 정작 **ADMIN 은 결재 화면 자체가 403** 이었다.
`.ai/api/approval.md` 에 "ADMIN 은 모든 범위에서 결재 권한이 없다" 고 적혀 있고 `ApprovalListScopePolicy` 가 그대로 막는다.
데이터를 아무리 잘 넣어도 안 보인다.

같은 문서에 "내 프로젝트·프로젝트 생성은 ADMIN 제외" 도 있어서, `page_permission` 과 `project_member` 까지 4곳이 명세 위반이었다.

→ **역할을 배분하기 전에 그 역할이 어떤 화면에서 막히는지 먼저 확인해라.**

| 확인할 것 | 어디서 |
|---|---|
| 역할별 화면 접근 제한 | `.ai/api/{도메인}.md` 의 "인증 필요" 줄, `*Policy.java` |
| 역할별 데이터 참여 제한 | 결재자 지정 가능 여부, 프로젝트 멤버 가능 여부 |

역할 배분표를 만들 때 이 열을 같이 채워라.

| role | 결재 조회 | 결재자 지정 | project_member | 관리자 콘솔 |
|---|:-:|:-:|:-:|:-:|
| MEMBER | ✅ | ✅ | ✅ | ❌ |
| ADMIN | ❌ | ❌ | ❌ | ✅ |
| MASTER | ✅ (scope=all 포함) | ✅ (멤버 검증 면제) | ✅ | ✅ |

### ⑨ 일괄 UPDATE 로 사람을 바꿀 때 중복이 생긴다
`UPDATE approval_line SET user_id='B' WHERE user_id='A'` 를 돌렸더니, 이미 B 가 들어 있던 결재선에 B 가 두 번 생겼다.
기안자 자리를 미리 다른 사람으로 밀어냈던 결재선들이 그랬다.
→ 치환 전에 **대상이 이미 그 그룹에 있는지** 확인하고, 겹치는 건만 먼저 따로 처리해라.

```sql
SELECT approval_revision_id, user_id FROM approval_line GROUP BY 1,2 HAVING COUNT(*)>1;
```

### ⑩ 데이터가 아니라 설정 문제인 경우
"로그인이 안 된다"의 원인이 Redis가 아니라 CORS 화이트리스트와 프론트 API 주소였다.
→ 증상이 데이터처럼 보여도 **API를 직접 호출해 응답 코드부터** 봐라.

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/v1/auth/login -H "Origin: http://<접속주소>" -H "Content-Type: application/json" -d '{"userId":"...","password":"..."}'
```

---

## 6. 문장 작성 규칙 — AI 말투 제거

전수조사에서 걸러낸 유형이다. 작성 후 이 목록으로 한 번 훑어라.

| 유형 | 나쁜 예 | 고친 예 |
|---|---|---|
| **메타 발언** (문서가 시스템을 설명) | "송부용 스텝 타입을 따로 만들지 않았다. 이 블록 하나로 표현된다." | (삭제) |
| | "더미값이다. 실제 값은 여기 쓰지 않는다." | (삭제) |
| | "회차마다 스텝을 만들지 않고 이 스텝 하나에 회차 블록을 나란히 둔다." | (삭제) |
| **사람이 안 쓰는 제목** | 막힌 지점 | 계정 등록 반려 건 |
| | AI 결과 — 사람 확정본 | AI 검토 결과 확인 |
| | 투입 인원·R&R | 담당자 배정 |
| | 송부 기록 | 제출 내역 |
| **가운뎃점 압축** | `[1차] 발주·입고·검품` | 1차 발주와 입고 |
| | 노출·판매·CS | 판매 운영 |
| **줄표(—) 남발** | 반려 — 규격 미달 900x900 | 반려된 컷. 900x900 으로 규격 미달 |
| **"(가정)" 딱지** | 수수료율 15% (가정) | 수수료율 15% |
| **영어 차용** | 개선 액션 / 입점 자격 스크리닝 | 개선 과제 / 입점 자격 확인 |

**판별 기준 하나**: 그 문장을 실제 담당자가 사내 문서에 쓸까? 안 쓰면 지운다.

**남겨야 할 것**: 업계 실무어(GMV·SKU·캐파·리오더)는 그대로 둔다. 이건 AI 티가 아니라 도메인 어휘다.

---

## 7. 검증 쿼리 — 전부 0이어야 한다

```sql
SELECT '배치 합≠3' k, COUNT(*) v FROM (
  SELECT step_id,row_index FROM block WHERE deleted_at IS NULL
  GROUP BY 1,2 HAVING SUM(col_span)<>3) t
UNION ALL SELECT 'BLK-009 위반', COUNT(*) FROM issue_block ib
  JOIN issue i USING(issue_id) JOIN block b ON b.block_id=ib.block_id
  WHERE i.step_id<>b.step_id
UNION ALL SELECT '블록 없는 결재', COUNT(*) FROM approval a
  LEFT JOIN block b ON b.block_id=a.block_id WHERE b.block_id IS NULL
UNION ALL SELECT '현재회차 리비전 없음', COUNT(*) FROM approval a
  WHERE NOT EXISTS(SELECT 1 FROM approval_revision r
                   WHERE r.approval_id=a.approval_id AND r.revision_no=a.current_revision_no)
UNION ALL SELECT 'version_no 불연속', COUNT(*) FROM (
  SELECT file_id FROM file_version GROUP BY file_id
  HAVING COUNT(*)<>MAX(version_no) OR MIN(version_no)<>1) t
UNION ALL SELECT 'order_index 중복', COUNT(*) FROM (
  SELECT img_block_id,order_index FROM image WHERE deleted_at IS NULL
  GROUP BY 1,2 HAVING COUNT(*)>1) t
UNION ALL SELECT '고아 상세', COUNT(*) FROM `text` t
  LEFT JOIN block b ON b.block_id=t.block_id WHERE b.block_id IS NULL
UNION ALL SELECT '담당자 없는 이슈', COUNT(*) FROM issue i
  WHERE NOT EXISTS(SELECT 1 FROM issue_assign a WHERE a.issue_id=i.issue_id)
UNION ALL SELECT '기안 3건 미만 계정', COUNT(*) FROM account a
  WHERE a.user_id LIKE 'vitawear-%'
    AND (SELECT COUNT(*) FROM approval ap WHERE ap.user_id=a.user_id)<3
UNION ALL SELECT '정산 실지급≠연결합계', COUNT(*) FROM settlement_block s
  WHERE IFNULL(s.actual_amount,0)
     <> IFNULL((SELECT SUM(c.amount) FROM cash_flow c WHERE c.settle_block_id=s.settle_id),0)
UNION ALL SELECT '연결 대상 없는 cash_flow', COUNT(*) FROM cash_flow c
  LEFT JOIN settlement_block s ON s.settle_id=c.settle_block_id
  WHERE c.settle_block_id IS NOT NULL AND s.settle_id IS NULL
UNION ALL SELECT '연결 대상 없는 tax_invoice', COUNT(*) FROM tax_invoice x
  LEFT JOIN settlement_block s ON s.settle_id=x.settle_block_id
  WHERE x.settle_block_id IS NOT NULL AND s.settle_id IS NULL
UNION ALL SELECT '기안자가 자기 결재선에', COUNT(*) FROM approval ap
  JOIN approval_revision r ON r.approval_id=ap.approval_id AND r.revision_no=ap.current_revision_no
  JOIN approval_line l ON l.approval_revision_id=r.approval_revision_id AND l.user_id=ap.user_id
UNION ALL SELECT '결재선 내 동일인 중복', COUNT(*) FROM (
  SELECT approval_revision_id,user_id FROM approval_line GROUP BY 1,2 HAVING COUNT(*)>1) t
UNION ALL SELECT 'ACTIVE 없는 IN_PROGRESS 결재', COUNT(*) FROM approval ap
  JOIN approval_revision r ON r.approval_id=ap.approval_id AND r.revision_no=ap.current_revision_no
  WHERE ap.status='IN_PROGRESS'
    AND NOT EXISTS(SELECT 1 FROM approval_line l
                   WHERE l.approval_revision_id=r.approval_revision_id AND l.status='ACTIVE')
UNION ALL SELECT '결재 권한 없는 역할이 결재선에', COUNT(*) FROM approval_line l
  JOIN account a ON a.user_id=l.user_id WHERE a.role='ADMIN';
```

**화면에서 실제로 보이는지는 API 로 확인해라.** DB 카운트만 봐서는 역할 게이트(§5-⑧)에 걸리는 걸 못 잡는다.

```bash
curl -s -c /tmp/c -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' -d '{"userId":"...","password":"..."}'
curl -s -b /tmp/c 'localhost:8080/api/v1/approvals?scope=drafted&size=1'
curl -s -b /tmp/c 'localhost:8080/api/v1/approvals?scope=pending&size=1'
```

정산 블록은 눈으로도 한 번 봐라. 금액 3열이 나란히 맞는지가 제일 빠른 검사다.

```sql
SELECT s.settle_id, b.title, s.type, s.status,
       s.total_amount 계약, s.actual_amount 실지급,
       (SELECT SUM(c.amount) FROM cash_flow c WHERE c.settle_block_id=s.settle_id) 연결합계
FROM settlement_block s JOIN block b ON b.block_id=s.block_id
ORDER BY s.type DESC, s.settle_id;
```

문구 잔재 검사도 같이 돌린다.

```sql
SELECT block_id,title FROM block WHERE title LIKE '%—%' OR title REGEXP '^S[0-9]';
SELECT txt_id FROM `text` WHERE content LIKE '%—%' OR content LIKE '%(가정)%'
   OR content LIKE '%블록%' OR content LIKE '%스텝%' OR content LIKE '%더미%';
SELECT img_id FROM image WHERE caption LIKE '%—%';
SELECT file_version_id FROM file_version WHERE comment LIKE '%—%' OR comment IS NULL OR comment='';
```

---

## 8. 기준선 — 2026-08 덤프 실제 수치

다음에 만들 때 "이 정도면 충분한가"를 가늠하는 잣대로 쓴다.

| 항목 | 수량 |
|---|---|
| 회사 / 부서 / 직급 / 사원 | 1 / 7 / 6 / 13 |
| 계정 | 13 (MEMBER 6 · ADMIN 6 · MASTER 1) |
| 프로젝트 | 10 (내용 있음 2, 목록용 8) |
| 스테이지 / 스텝 | 17 / 55 |
| 블록 | 236 (메인 119 · 25 F/W 51 · 나머지 66) |
| 이슈 / 담당배정 / 블록연결 | 52 / 55 / 46 |
| 파일 / 버전 / 블록연결 | 32 / 62 / 30 |
| 이미지 | 47 |
| 결재 / 회차 / 결재선 | 41 / 42 / 99 |
| 입출금 | 30 (INCOME 16 · OUTCOME 14 / 연결 12 · 미연결 15 · 제외 3) |
| 정산 블록 | 13 (**INCOME 6 · OUTCOME 7**) |
| 세금계산서 | 20 (매출 10 · 매입 10 / 연결 10 · 미연결 8 · 제외 2) |
| 활동 로그 / 알림 | 34 / 3 |

**소요**: 계획 1회 + 파일 21개. 재작업이 컸던 지점은 넷이다.

| 재작업 | 원인 | 다음에 피하는 법 |
|---|---|---|
| 활동 로그 전면 재설계 | ERD 문서와 실제 스키마 불일치 | §1-1 스키마 조회를 먼저 |
| 배치 전체 UPDATE | 내용 쓰고 배치를 잡음 | §1-6 배치를 내용보다 먼저 |
| 문구 전수 재작성 | AI 말투로 초안을 씀 | §6 체크리스트를 쓰면서 적용 |
| 외주·출금 추가 | 처음에 INCOME 만 만듦 | §3-4 처음부터 OUTCOME 을 설계에 넣기 |
