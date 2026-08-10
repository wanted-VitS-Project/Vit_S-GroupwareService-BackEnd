# 🗑️ 삭제 정책 — 전 도메인 공통 규칙

**최종 업데이트**: 2026-08-10 (신설 — `PRJ-V1.md` §3-1 의 `D-1`~`D-7` 을 전 도메인 기준으로 승격)
**담당**: 김동현 (프로젝트 계층 · DevOps)
**범위**: 사용자 조작으로 무언가를 **지금 지울 때**의 규칙
**범위 밖**: 보존기간 만료 후 시스템이 정리하는 하드 삭제 → [`CLEANUP.md`](CLEANUP.md) (김용준 PO 소관)

> ⚠️ **삭제는 혼자 결정할 수 없다.**
> 내 테이블을 지우는 방식이 **다른 도메인의 조회를 깨뜨린다.** 반대도 마찬가지다.
> 그래서 이 문서는 "내 도메인에서 무엇을 확인해야 하나" 를 §4 에 담당별로 적어뒀다.

---

## 0. 왜 이 문서가 있나

2026-08-10 에 프로젝트 계층의 삭제 정책을 확정하면서 **전 도메인 조회 쿼리를 실측**했다. 결과:

| 발견 | 내용 |
|---|---|
| 🔴 **삭제 처리가 도메인마다 따로 놀았다** | 같은 "참조 대상이 삭제됨" 상황에 **4가지 다른 처리**가 있었다 (§5). 그중 3개는 화면에 잘못된 값을 보여준다 |
| 🔴 **한 도메인은 조용히 다른 값을 띄운다** | 입금내역이 삭제되면 에러도 빈 값도 아니고 **그 이전 건**이 올라온다. 눈으로 못 잡는다 (§5 패턴 C) |
| 🔴 **참여자가 목록에서 사라진다** | 사원이 삭제되면 `INNER JOIN` 에 걸려 그 사람의 참여자 행이 **통째로 소멸**한다 (§5 패턴 A) |
| 🟡 **규칙이 문서 3곳에 흩어져 있었다** | `ERD.md` §0-15 · `PRJ-V1.md` INV-05 · `CLEANUP.md` — 새 도메인이 생길 때마다 같은 논의를 반복했다 |

**이 문서 하나만 보고 판단할 수 있게** 하는 것이 목적이다.

---

## 1. 판정 — 질문 3개로 끝난다

새 테이블을 만들거나 삭제 기능을 붙일 때, **이 순서로** 답하면 된다.

```
Q1 ─ 이 테이블은 soft 인가 hard 인가?          → D-1 · D-2
       │
Q2 ─ 상위가 삭제되면 나는 그걸 어떻게 아나?      → D-3 · D-4
       │
Q3 ─ 내가 참조하는 게 삭제되면 화면에 뭘 띄우나?  → D-5 · D-6
```

### Q1. soft 인가 hard 인가

```
        ┌─ 이 행에 「담긴 정보」가 있나? ─┐
       YES                              NO
        │                                │
    soft delete                  UNIQUE·복합PK 가 있나?
     (D-1)                        ┌──────┴──────┐
                                 YES            NO
                                  │              │
                            hard DELETE      soft delete
                              (D-2)            (D-1)
```

> **「담긴 정보」의 기준** — 그 행을 지운 뒤 **다시 만들 때 사용자가 뭘 입력해야 하나**.
> 아무것도 입력할 게 없다면(그냥 다시 연결만 하면 된다면) 연결 행이다.

### Q2·Q3 은 §3 의 레이어 표로 바로 간다.

---

## 2. 규칙 `D-1` ~ `D-7`

| ID | 규칙 | 근거 |
|----|------|------|
| **D-1** | **실물은 전부 soft delete.** 예외 없다 | `ERD.md` §0-15 |
| **D-2** | **하드 DELETE 는 「연결 행 7종」뿐이다** — ① 담긴 정보가 없고 ② `UNIQUE`·복합 PK 를 가진 것 | §2-1 |
| **D-3** | ⭐ **하드의 트리거는 「상위 삭제」가 아니라 「사용자의 연결 해제 API」다.** 상대가 soft 로 남아 있으면 연결 행도 남는다 | `ERD.md` §5-3 |
| **D-4** | **전파는 어댑터 조합으로만.** 상세의 `deleted_at` 은 `block.deleted_at` 의 **미러**이고, 미러 유지 책임은 **어댑터 등록**에 있다 | [`BLOCK.md`](BLOCK.md) §2 |
| **D-5** | ⛔ **`activity_log` 는 지우지 않는다.** `deleted_at` 컬럼 자체가 없다 | `ERD.md` §0-16 |
| **D-6** | ⭐ **참조 대상이 삭제되면 값을 숨기지 말고 「상태」를 함께 내보낸다** | §5 |
| **D-7** | ⛔ **soft delete 테이블에 `UNIQUE` 를 새로 걸지 않는다.** 걸어야 하면 삭제 시 그 컬럼을 `NULL` 로 비운다 | §6 |

### 2-1. D-2 대상 — 하드 DELETE 7종

이게 **전부**다. 여기 없으면 soft 다.

| 테이블 | 제약 | 하드인 이유 (= soft 면 재연결이 `1062` 로 죽는다) | 담당 |
|---|---|---|---|
| `project_member` | `uk_pm_project_user` | 참여자 재초대 | 동훈 |
| `step_permission` | `uk_sp_step_user` | 권한 재부여 | 동훈 |
| `stage_permission_default` | `uk_spd_stage_user` | 〃 | 동훈 |
| `project_business_category` | `uk_pbc` | 사업분류 재연결 | 동훈 |
| `issue_block` | `uk_ib` | 이슈-블록 재연결 | 김용준 |
| `issue_assign` | `UK_issue_assign` | 담당자 재배정 | 김용준 |
| `block_file` | **`PK(block_id, file_id)`** | 파일 재부착 — 복합 PK 라 `UNIQUE` 보다 강하다 | 김동현 |

> ⚠️ **`project_member` 는 두 도메인이 걸린다** — 행 자체는 프로젝트 소관(동훈)이지만, 조회할 때 조인하는 `employee.deleted_at` 은 **사원 도메인(김동현)** 의 값이다. D-6 을 적용할 때 **양쪽이 합의**해야 한다.

### 2-2. D-3 — 하드가 실제로 실행되는 순간

⚠️ **여기가 가장 많이 오해되는 지점이다.** 하드 DELETE 는 **cascade 로 실행되지 않는다.**

| 상황 | 연결 행 |
|---|---|
| 블록이 soft 삭제됨 | ✅ **남는다** — 조회에서 `block.deleted_at IS NULL` 로 거른다 |
| 사원이 퇴사·삭제됨 | ✅ **남는다** — 그래야 D-6 「퇴사함」 표시의 대상이 존재한다 |
| **사용자가 「연결 해제」 버튼을 눌렀다** | ⛔ **여기서만 지운다** |

> `ERD.md` §5-3 — *"블록을 지울 땐 `block.deleted_at` 만 찍고 이 행은 **건드리지 않는다.**
> 행을 지우는 건 **사용자가 연결을 끊었을 때뿐**이다."*

---

## 3. 어디서 무엇을 하나 — 레이어별

삭제 한 건은 **4개 레이어**에 흔적을 남긴다. 하나라도 빠지면 조용히 깨진다.

| # | 레이어 | 할 일 | 빠뜨리면 |
|---|---|---|---|
| **1** | **DDL / 마이그레이션** | soft 면 `deleted_at DATETIME NULL` + `KEY idx_..._deleted`. hard 면 컬럼을 **넣지 않는다** | UNIQUE 슬롯 점유 → 재연결 `1062` (§6-1) |
| **2** | **삭제 경로 (Command)** | soft = `deleted_at` 찍기. hard = `DELETE`. **멱등해야 한다** — 이미 삭제된 행이면 조용히 통과 | 중복 이벤트에서 500. 상세 삭제가 막히면 **블록 삭제 전체가 실패** |
| **3** | **전파 (어댑터)** | 상위(블록·스텝) 삭제가 내 상세를 지우도록 `BlockDetailPort` 어댑터를 **등록**한다 | ⚠️ 미등록이면 `registry.find()` 가 **조용히 넘어간다**. 예외도 로그도 없다 (§6-3) |
| **4** | **조회 (Query)** | 내 화면에서 ① 삭제된 내 행을 거르고 ② **내가 참조하는 것의 `deleted_at` 상태를 함께 내보낸다** (D-6) | 삭제된 데이터가 화면에 남거나, 반대로 살아있는 행이 사라진다 (§5) |

### 3-1. 레이어 2 — 삭제 경로의 순서 함정

⚠️ **상세를 먼저 지우고, 그다음 `block.deleted_at` 을 찍는다. 순서를 뒤집으면 `block.deleted_at` 이 유실된다.**

타입 도메인의 삭제가 `@Modifying(clearAutomatically = true)` 벌크 UPDATE 인데 `flushAutomatically` 가 기본값(`false`) 이다. `block` 을 먼저 `save()` 하면 그 pending UPDATE 를 **flush 하지 않고 영속성 컨텍스트를 비운다.** 커밋해도 블록은 안 지워지고 상세만 지워져서, **조회에는 블록이 그대로 뜬다.**

> 2026-08-05 런타임 검증으로 발견된 실제 버그다. 컴파일도 되고 예외도 안 난다.
> `BlockCommandService.deleteBlock` 의 순서와 주석을 **뒤집지 마라.**

### 3-2. 레이어 3 — 전파 진입점은 2곳뿐이다

| 상위 삭제 | 전파 |
|---|---|
| `project` | 스텝 0개일 때만 삭제 허용 → **전파 대상이 없다** |
| `stage` | 하위 스텝을 **지우지 않는다** (STG-003) → 전파 없음 |
| **`step`** | ✅ 하위 블록 + 이슈 |
| **`block`** | ✅ 타입별 상세 (어댑터) |

---

## 4. 도메인별 할 일 — 각자 확인할 것

> 아래는 **2026-08-10 실측 기준**이다. 확인 후 이 표를 갱신해 주면 된다.

### 🟢 동훈 — 프로젝트 · 스테이지 · 스텝 · 블록

| 대상 | 상태 | 할 일 |
|---|---|---|
| 참여자 목록 (`ProjectMemberQueryMapper`) | 🔴 **패턴 A** | `INNER JOIN employee … AND e.deleted_at IS NULL` → `LEFT JOIN` + `(e.deleted_at IS NOT NULL) AS deleted`. **지금은 삭제된 사원의 참여자 행이 목록에서 사라진다** → §5-2 예시 1. ⚠️ `employee` 는 김동현 소관이라 **필드 추가는 양쪽 합의** |
| 프로젝트 상세·목록의 카테고리 | 🟡 **패턴 B** | `categoryDeleted` 필드 추가 → §5-2 예시 2 |
| `BusinessCategoryLookupQueryMapper.findByIds` | 🟡 **패턴 B** | `WHERE deleted_at IS NULL` 제거 + 상태 컬럼 |
| `block.owner` 담당자 조회 | ❓ **미확인** | 실측 필요 — `owner` 가 사번이라 `employee.deleted_at` 을 봐야 한다 |
| 하드 4종 (`project_member`·`step_permission`·`stage_permission_default`·`project_business_category`) | ✅ 하드 | **D-3 확인** — 상위가 soft 삭제될 때 이 행을 지우면 안 된다 |

### 🟡 서정림 — 텍스트 · 체크리스트 · 이미지 · **재무(정산)**

| 대상 | 상태 | 할 일 |
|---|---|---|
| `SettlementStatusMapper` 입출금·계산서 | 🔴 **패턴 C — 전체에서 가장 위험** | 파생 서브쿼리가 `deleted_at IS NULL` + `LIMIT 1` 이라, 연결된 입금내역이 삭제되면 **`linked_at` 이 그다음으로 최신인 다른 건**이 올라온다. 화면엔 정상적인 금액이 뜬다 → §5-2 예시 3 |
| 이미지 휴지통 · 복구 응답 | 🟡 **필드 없음** | **`blockDeleted` 추가.** cascade 로 삭제된 이미지는 휴지통에 보이지만 복구는 `IMG-009` 로 거부된다(**의도된 동작** — 영구삭제 진입로를 남기려고). 지금은 사용자가 **눌러봐야** 안다. 파일 도메인이 이미 같은 이름·같은 의미로 쓰는 필드다 |
| `cash_flow`·`tax_invoice` → `settlement_block` | ⚠️ **진짜 FK** | 하드 삭제 정리가 붙으면 연결을 끊기 전까지 **FK 위반으로 실패**한다 → BLK-013 선행 |
| 상세 어댑터 4종 (`text`·`checklist`·`image`·`settlement`) | ✅ 등록됨 | 없음 |

### 🔷 김동현 — auth · 사원/계정 · 파일

| 대상 | 상태 | 할 일 |
|---|---|---|
| `FileQueryMapper` | ✅ **기준 구현** | 없음 — D-6 패턴 D 의 모범이다. 다른 도메인이 이걸 보고 맞춘다 |
| ⭐ **`employee.deleted_at` 제공 측** | ⚠️ **합의 필요** | 사원을 참조하는 곳이 **7곳**이다 — `project_member`·`step_permission`·`stage_permission_default`·`issue_assign`·`block.owner`·`activity_log.user_id`·`block_file.linked_by`. **삭제된 사원을 어떻게 표시할지 한 번 정해서 전 도메인이 같이 쓰는 게 낫다** (필드명·문구) |
| `resigned_at` vs `deleted_at` 구분 | ✅ DDL 에 명시 | 없음 — 단 §5-3 을 각 도메인에 알려야 한다 |
| `block_file` | ✅ 하드 | **D-3 확인** — 블록이 soft 삭제될 때 이 행을 지우면 안 된다 |

### 🔵 정현 — 공고 · 비타메이트

| 대상 | 상태 | 할 일 |
|---|---|---|
| `BID_NOTICE` 상세 어댑터 | 🔴 **미등록** | `BlockDetailPort` 어댑터가 없어서 블록 삭제 시 `bid_notice_block.deleted_at` 이 **안 찍힌다** = D-4 미러가 거짓이다. ⚠️ `registry.find(type).ifPresent(...)` 라 **예외도 로그도 없이 조용히 넘어간다** |
| `AI` (`vitamate_block`) | ✅ 등록됨 | 없음 |

### 🟣 이강욱 — 결재

| 대상 | 상태 | 할 일 |
|---|---|---|
| 진행 중 결재의 블록 삭제 | ⚠️ **차단 제거됨** (2026-08-10) | 블록이 사라지면 진행 중이어도 함께 삭제된다. 결재는 블록에서만 올라가므로 정상 판단이지만 **결재 도메인에서 확인 필요** |
| `approval` 상세 삭제 | ✅ 멱등 | 없음 |

### ⚪ 김용준 — 이슈 · PO

| 대상 | 상태 | 할 일 |
|---|---|---|
| `issue_block`·`issue_assign` | ✅ 하드 (D-2) | 없음 — 단 **D-3 확인**: 블록이 soft 삭제될 때 이 행을 지우면 안 된다 |
| `CLEANUP.md` 스케줄러 | ⏸️ 계층 미등록 | 계층(project~block)은 **v1 에서 등록하지 않는다** — 선행 3건(BLK-013 · `fk_al_block` SET NULL · 실행 순서 보장) 미완 |

---

## 5. D-6 상세 — 조회 패턴과 쿼리 구조

**참조 대상이 삭제됐을 때** 조회가 뭘 하는지. **D 만 정답이다.**

| | 패턴 | 삭제되면 | 심각도 |
|---|---|---|---|
| **A** | `INNER JOIN … AND x.deleted_at IS NULL` | **부모 행까지 목록에서 사라진다** | 🔴 데이터 소실로 보인다 |
| **B** | `LEFT JOIN … AND x.deleted_at IS NULL` | 필드가 `NULL`. 「삭제됨」인지 「원래 없음」인지 **구분 불가** | 🟡 정보 손실 |
| **C** | 파생 서브쿼리 + `deleted_at IS NULL` + `LIMIT 1` | ⚠️ **조용히 그 이전 건으로 바뀐다** | 🔴 **잘못된 값을 정상처럼 표시** |
| **D** | `CASE WHEN … END` + `(x.deleted_at IS NOT NULL) AS xDeleted` | 값 유지 + 상태 노출 | ✅ **기준** |

### 5-1. 쿼리 구조 — 4가지 기본형

이 4개로 거의 다 된다. **어느 쪽인지 먼저 정하고 쓰라.**

#### ① 내 행을 거른다 — 일반 조회

```sql
-- 내가 소유한 테이블의 deleted_at 은 WHERE 에 둔다
SELECT b.block_id, b.title
  FROM block b
 WHERE b.step_id = #{stepId}
   AND b.deleted_at IS NULL          -- 내 행 필터는 WHERE
```

#### ② 휴지통 — 방향만 반대다

```sql
SELECT f.file_id, f.name, f.deleted_at
  FROM file f
 WHERE f.project_id = #{projectId}
   AND f.deleted_at IS NOT NULL      -- 삭제된 것만
 ORDER BY f.deleted_at DESC          -- 최근 삭제 순
```

> ⚠️ **휴지통에서 상위(블록)의 생존을 조건으로 걸지 마라.** 복구가 불가능한 항목도 보여야 한다 (§6-4).
> 대신 ③ 으로 **복구 가능 여부를 함께 내려준다.**

#### ③ 남의 것을 참조한다 — 패턴 D ⭐

```sql
-- ⚠️ 참조 대상의 deleted_at 은 WHERE 가 아니라 SELECT 로 간다
SELECT f.file_id                                        AS fileId,
       CASE WHEN b.deleted_at IS NULL THEN b.block_id END AS blockId,
       CASE WHEN b.deleted_at IS NULL THEN b.title    END AS blockTitle,
       (b.deleted_at IS NOT NULL)                       AS blockDeleted
  FROM file f
  JOIN block_file bf ON bf.file_id = f.file_id
  JOIN block b       ON b.block_id = bf.block_id      -- 조건 없이 조인한다
 WHERE f.project_id = #{projectId}
   AND f.deleted_at IS NULL                            -- 내 행만 WHERE
```

**핵심 세 줄**

| 위치 | `deleted_at` 을 |
|---|---|
| **WHERE** | **내 행에만** 쓴다 |
| **JOIN ON** | ⛔ **쓰지 않는다** — 여기 쓰면 패턴 A·B 가 된다 |
| **SELECT** | 참조 대상은 `CASE WHEN` + `(… IS NOT NULL) AS xxxDeleted` |

#### ④ 연결 행 조회 — 상대의 `deleted_at` 으로 거른다 (D-3)

```sql
-- 연결 행에는 deleted_at 이 없다(하드 7종). 그래서 상대를 봐야 한다
SELECT ib.issue_id, ib.block_id
  FROM issue_block ib
  JOIN block b ON b.block_id = ib.block_id
 WHERE ib.issue_id = #{issueId}
   AND b.deleted_at IS NULL          -- 블록이 죽었으면 이 연결은 안 보인다
```

> 여기서는 `JOIN ON` 이 아니라 **`WHERE`** 에 뒀다. 연결 행은 **상대가 죽으면 존재 의미가 없어서** 목록에서 빼는 게 맞다 — ③ 과 목적이 다르다.

### 5-2. Before → After — 실제 3건

#### 예시 1. 패턴 A → D (참여자 목록 · 동훈)

**🔴 Before** — 삭제된 사원의 **참여자 행이 사라진다**

```sql
SELECT pm.user_id AS userId, e.name AS name,
       (e.resigned_at IS NOT NULL) AS resigned
  FROM project_member pm
  JOIN employee e                      -- ⚠️ INNER JOIN
    ON e.user_id = pm.user_id
   AND e.deleted_at IS NULL            -- ⚠️ 조인 조건이 행을 없앤다
 WHERE pm.project_id = #{projectId}
```

**✅ After**

```sql
SELECT pm.user_id                                   AS userId,
       CASE WHEN e.deleted_at IS NULL THEN e.name END AS name,
       (e.resigned_at IS NOT NULL)                  AS resigned,
       (e.deleted_at  IS NOT NULL)                  AS deleted
  FROM project_member pm
  LEFT JOIN employee e ON e.user_id = pm.user_id    -- 조건 없이
 WHERE pm.project_id = #{projectId}
```

> ⚠️ 이름을 `NULL` 로 비우면 화면에 빈칸이 뜬다. **FE 가 `deleted` 로 「삭제된 사용자」 를 렌더**하게 하거나,
> 이름 스냅샷이 필요하면 `activity_log.target_name` 처럼 별도 컬럼을 두는 걸 검토한다 (김동현과 합의).

#### 예시 2. 패턴 B → D (프로젝트 카테고리 · 동훈)

**🟡 Before** — 카테고리가 조용히 `NULL` 이 돼서 **「삭제됨」과 「원래 없음」이 구분 안 된다**

```sql
  LEFT JOIN business_category bc
    ON bc.business_category_id = pbc.business_category_id
   AND bc.deleted_at IS NULL           -- ⚠️ 여기가 문제
```

**✅ After**

```sql
SELECT CASE WHEN bc.deleted_at IS NULL THEN bc.business_category_id END AS categoryId,
       bc.name                                     AS categoryName,   -- 이름은 남긴다
       (bc.deleted_at IS NOT NULL)                 AS categoryDeleted
  ...
  LEFT JOIN business_category bc
    ON bc.business_category_id = pbc.business_category_id   -- 조건 제거
```

> 카테고리는 **이름을 남기는 게 낫다** — 사용자가 "이 프로젝트가 무슨 분류였는지" 를 알아야 한다.
> 사원과 판단이 갈리는 지점이다: **개인정보성 값은 비우고, 분류·라벨은 남긴다.**

#### 예시 3. 패턴 C → D (정산 입출금 · 서정림) — ⚠️ 가장 어렵다

**🔴 Before** — 삭제되면 **그 이전 건이 정상 금액처럼 올라온다**

```sql
LEFT JOIN cash_flow cf ON cf.cash_flow_id = (
    SELECT cf2.cash_flow_id
      FROM cash_flow cf2
     WHERE cf2.settle_block_id = sb.settle_id
       AND cf2.deleted_at IS NULL      -- ⚠️ 최신 건이 삭제되면 그 앞 건이 뽑힌다
     ORDER BY cf2.linked_at DESC, cf2.cash_flow_id DESC
     LIMIT 1
)
```

**✅ After** — 서브쿼리에서 조건을 **빼고**, 상태를 내보낸다

```sql
SELECT CASE WHEN cf.deleted_at IS NULL THEN cf.amount    END AS cashFlowAmount,
       CASE WHEN cf.deleted_at IS NULL THEN cf.linked_at END AS cashFlowLinkedAt,
       (cf.deleted_at IS NOT NULL)                          AS cashFlowDeleted
  ...
LEFT JOIN cash_flow cf ON cf.cash_flow_id = (
    SELECT cf2.cash_flow_id
      FROM cash_flow cf2
     WHERE cf2.settle_block_id = sb.settle_id   -- ⚠️ deleted_at 조건을 뺀다
     ORDER BY cf2.linked_at DESC, cf2.cash_flow_id DESC
     LIMIT 1
)
```

⚠️ **왜 조건을 빼는가** — 조건을 남기면 *"마지막에 연결한 입금이 삭제됐다"* 는 사실이 **사라지고**, 그 앞 건이 슬쩍 대체된다. 사용자는 지금도 정상 연결돼 있다고 믿는다. **조용한 거짓말보다 「삭제됨」 표시가 낫다.**

> 살아있는 이전 건까지 보여줘야 한다면 그건 **별도 요구사항**이다 — 「연결 이력」 목록을 따로 내려야 하고, 최신 1건 슬롯에 섞으면 안 된다.

### 5-3. 🔑 `resigned_at` ≠ `deleted_at`

`employee` DDL 이 못 박아뒀다 — `deleted_at DATETIME NULL COMMENT '삭제일 (퇴사일 resigned_at 과 다르다)'`.

| | 뜻 | 상태 |
|---|---|---|
| `resigned_at` | 회사를 나갔다. **계정·이력은 유효** | ✅ 이미 표시된다 |
| `deleted_at` | 사원 데이터 자체가 삭제됐다 | 🔴 패턴 A 로 숨겨진다 |

---

## 6. 함정 — 조용히 깨지는 것들

### 6-1. ⚠️ `UNIQUE(…, deleted_at)` 는 작동하지 않는다

soft delete 와 UNIQUE 를 공존시키려고 제약에 `deleted_at` 을 끼우는 건 **오답**이다.

MySQL 은 `UNIQUE` 안의 `NULL` 을 **서로 다른 값으로 취급**한다. 활성 행은 전부 `deleted_at IS NULL` 이니 **중복을 아예 못 막는다.** 제약을 걸어놓고 방어가 사라진 상태가 된다.

| 해법 | 언제 |
|---|---|
| 삭제 시 그 컬럼을 `NULL` 로 비운다 | `uk_project_bid_notice` 방식 — 참조 컬럼일 때 |
| 아예 하드 DELETE 로 돌린다 | 연결 행일 때 (D-2) |
| `UNIQUE` 를 걷고 앱이 **활성 행만** 중복 검사 | 마스터 데이터일 때 — 🔴 `business_category` `uk_bc_name` **미해결** |

### 6-2. ⚠️ 삭제 순서를 뒤집으면 `block.deleted_at` 이 유실된다

→ §3-1 참고. **컴파일도 되고 예외도 안 난다.**

### 6-3. ⚠️ 어댑터 미등록은 예외를 던지지 않는다

`blockDetailRegistry.find(block.getType()).ifPresent(...)` — 어댑터가 없으면 **조용히 넘어간다.** 로그도 없다. 새 블록 타입을 만들면 **어댑터 등록을 반드시 확인**하라.

| 타입 | 어댑터 |
|---|---|
| `TEXT` · `CHECKLIST` · `IMAGE` · `SETTLEMENT` · `APPROVAL` · `AI` | ✅ |
| **`BID_NOTICE`** | 🔴 **미등록** |
| `FILE` | — `type_id` 가 `NULL`(복합 PK)이라 애초에 대상 아님 |

### 6-4. ⚠️ 휴지통은 「복구 가능한 것」의 목록이 아니다

**「아직 실물이 남아 있는 것」의 목록이다.** cascade 로 삭제돼 복구가 불가능한 항목도 **보여야 한다** — 영구삭제 API 의 유일한 진입 화면이 휴지통이라서, 목록에서 빼면 사용자가 그 항목을 **볼 수도 지울 수도 없고 스토리지 객체는 계속 남는다.**

### 6-5. ⚠️ 휴지통이 있는 도메인은 2개뿐이다

| 도메인 | 휴지통 | 이유 |
|---|:---:|---|
| `file` · `image` | ✅ | 재업로드 비용이 큰 **개별 자산**이다 |
| 그 외 전부 (계층 · `text` · `checklist` · `approval` · `settlement` · `vitamate`) | ⛔ | 상세는 `block_id NOT NULL` 이라 **독립 생명주기가 없다** |

계층(프로젝트·스테이지·스텝·블록)에는 **복구가 없다.** 살리는 수단은 **지우기 전에 옮기는 것**이다 — STG-003(스텝 이전) · STP-013(살릴 블록 선별) · BLK-014(블록 이동).
⚠️ 그래서 **삭제 확인 모달에 "되돌릴 수 없습니다" 를 반드시 넣어야 한다.** 없으면 사용자는 휴지통이 있다고 가정한다.

### 6-6. `file` 과 `image` 는 복구 규칙이 반대다 — **버그가 아니다**

| | 소속 컬럼 | 블록이 죽으면 | 복구 |
|---|---|---|:---:|
| `image` | `image.img_block_id` → **블록 소속** | 붙을 자리가 없다 | ⛔ 거부 |
| `file` | `file.project_id` → **프로젝트 소속** | **프로젝트 문서함에 산다** | ✅ 허용 |

> `BLOCK.md` §4-4 — *"블록을 지워도 파일은 산다"*. 파일에게 블록은 **부착 지점**일 뿐 생명주기의 주인이 아니다.
> **두 도메인을 통일하려 하지 마라.** 소속이 다르다.

---

## 7. 판정 예시

| 시나리오 | 판정 | 근거 |
|---|---|---|
| 새 연결 테이블 `block_tag(block_id, tag_id)` 를 만든다 | **hard.** `deleted_at` 을 넣지 않는다 | 담긴 정보 없음 + 복합 PK → D-2 |
| 태그 마스터 `tag(name UNIQUE)` 를 만든다 | **soft.** 단 `UNIQUE(name)` 과 충돌 → 앱이 활성 행만 중복 검사 | D-1 + D-7 · §6-1 |
| 블록이 삭제될 때 `block_tag` 를 지워야 하나 | ⛔ **안 지운다.** 조회에서 `block.deleted_at IS NULL` 로 거른다 | D-3 |
| 태그가 삭제됐는데 블록 상세에 뭘 띄우나 | 태그명을 유지하고 `tagDeleted: true` 를 함께 내려 배지로 표시 | D-6 · §5 패턴 D |
| 새 블록 타입 `SURVEY` 를 추가한다 | `survey_block` 에 `deleted_at` + **`BlockDetailPort` 어댑터 등록** | D-1 + D-4 · §6-3 |

---

## 8. 관련 문서

| 문서 | 무엇이 있나 |
|---|---|
| [`CLEANUP.md`](CLEANUP.md) | 보존기간 만료 **하드 딜리트 스케줄러** SPI · CASCADE 판단 기준 (김용준) |
| [`BLOCK.md`](BLOCK.md) §2 | 블록 상세 확장 가이드 — **어댑터 등록 방법** · 상세 삭제 계약 |
| [`PERSISTENCE.md`](PERSISTENCE.md) | JPA(쓰기) ↔ MyBatis(조회) 역할 분리 |
| `ERD.md` §0-15·§0-16 · §5-3 | 삭제 정책 원문 · `activity_log` 보존 · 연결 행 처리 |
