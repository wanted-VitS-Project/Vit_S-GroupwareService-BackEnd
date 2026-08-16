# 데모 시드 — 비타웨어 무신사 입점

**시나리오 정본**: [`.ai/docs/DEMO-SCENARIO.md`](../../../../../.ai/docs/DEMO-SCENARIO.md)

> ⛔ **이 폴더는 Flyway 마이그레이션이 아니다.**
> `application.yml` 의 `spring.flyway.locations: classpath:db/migration` 이라 `db/demo` 는 **자동 적용되지 않는다.**
> `FLYWAY.md` §5 — *"개발용 더미 데이터는 Flyway에 넣지 않고 별도 수동 절차로 관리한다."*
> 반복 재적용할 데모 데이터를 마이그레이션에 넣으면 첫 적용 시점에 파일이 잠기고(§4) 운영 RDS까지 따라간다.

---

## 실행

로컬/dev DB에 **순서대로** 적용한다. 전부 명시적 PK + `INSERT IGNORE` 라 재실행해도 중복이 스킵된다.

```bash
for f in src/main/resources/db/demo/0*.sql; do mysql -h "$DB_HOST" -u "$DB_USER" -p "$DB_NAME" < "$f"; done
```

| # | 파일 | 내용 |
|:-:|---|---|
| 01 | `01_master.sql` | ⭐ **회사(테넌트) 1** · 부서 7 · 직급 6 · 사원 12 · 사업 카테고리 2 |
| 02 | `02_project.sql` | 프로젝트 3 · **카테고리 연결 4** · 참여자 8 · 스테이지 6 · 스텝 15(+16) · 스텝 권한 3 |
| 03 | `03_blocks.sql` | 블록 **102**(활성 100 + 삭제 2) · `text` 56 · `checklist_block` 18 / `checklist` 107 · `image_block` 8 / `image` 21 · `vitamate_block` 1 · `settlement_block` 3 |
| 04 | `04_files.sql` | `file` 17 · `file_version` 28 · `block_file` 17 |
| 05 | `05_issues.sql` | `issue` 13 · `issue_assign` 13 · `issue_block` 13 |
| 06 | `06_finance.sql` | `cash_flow` **5** · `tax_invoice` **4** — 연결됨 / 미연결 / 연결 제외 **3상태 전부** |
| 07 | `07_approval.sql` | `approval` 5 · `approval_revision` 6 · `approval_line` 15 · `approval_document` 7 |
| 08 | `08_activity_log.sql` | 활동 로그 34 · 알림 3 |
| 09 | `09_page_permission.sql` | 화면 접근 권한 15 (비밀번호 없음 — 커밋 가능) |
| 10 | `10_layout.sql` | 배치 교정 — 모든 `TEXT` 를 `col_span 1` 로, `APPROVAL`/`IMAGE` 만 넓게 |
| 11 | `11_text.sql` | `text` 56건 본문 교체 — **표 제거**, `**굵게**` + 빈 줄 문단 + `- ` 목록만 |
| 12 | `12_issues.sql` | `issue` +39 (**총 52**, 15개 스텝 전부) · `issue_assign` +42 · `issue_block` +35 |
| 13 | `13_files.sql` | `file` +9 (**총 28**) · `file_version` +27 (**총 57**) · `block_file` +9 |
| 14 | `14_images.sql` | `image` +26 (**총 47**) — 블록 수는 그대로, 장수만 증가 |
| 15 | `15_wording.sql` | 문구 전면 재작성 — AI 말투 제거 (제목 43 · 본문 39 · 캡션 41 · 코멘트 32) |
| 16 | `16_roles.sql` | 사원 +1 · 계정 **13** (MEMBER 6 / ADMIN 6 / MASTER 1) · `page_permission` 재구성 |
| 17 | `17_projects.sql` | 프로젝트 +7 (**총 10**) · 스테이지 +10 · 스텝 +21 · **MEMBER 6명을 10개 전 프로젝트에** |
| 18 | `18_p9002.sql` | 25 F/W 내용 채우기 — 블록 48 · 결재 12 (기안자 12명에게 1건씩) |
| 19 | `19_p9004_9010.sql` | 목록용 7개 프로젝트 — 블록 66 · 결재 24 → **계정마다 기안 3건 이상 완성** |
| 20 | `20_cashflow.sql` | 25 F/W 정산 블록 3 · 입금내역 +19 (**총 24**, 연결 4 / 미연결 17 / 제외 3) |
| 21 | `21_outsourcing.sql` | 무신사 프로젝트에 **외주 관리** 스테이지 · **OUTCOME 정산 블록 7** · 출금 +6 및 기존 4건 연결 · 세금계산서 4 → **20** |

| 22 | `22_approval_pending.sql` | 결재 8건 추가 — `ACTIVE` 결재선을 흩뿌려 **결재 대기(scope=pending) 탭**을 채운다 |
| 23 | `23_admin_policy.sql` | **명세 준수** — ADMIN 을 결재선·기안·프로젝트 멤버·화면 권한에서 전부 분리, 기안 24건 재배정 |

> ⛔ **21·22·23 은 재실행하면 중복키에서 멈춘다.** 되돌리기 주석대로 지운 뒤 다시 돌려라.
> 파일 끝의 `정합성 보정` / `부작용 정리` 절만 다시 돌리고 싶으면 그 부분만 잘라 실행한다.

### 역할별로 보이는 것 (23 적용 후)

| role | 인원 | 결재관리 | 내 프로젝트 | 관리자 콘솔 |
|---|:-:|---|:-:|:-:|
| MEMBER | 6 | 내가올린 3~9건 · 결재대기 1~3건 | ✅ 10개 전부 | ❌ |
| MASTER | 1 | 내가올린 7 · 결재대기 2 · `scope=all` | ✅ | ✅ |
| ADMIN | 6 | **403 (명세대로)** | ❌ | ✅ |

> ADMIN 은 `.ai/api/approval.md` 상 결재 권한이 없다. 시연에서 ADMIN 계정으로는 결재 페이지를 열지 마라 —
> 관리자 콘솔·전사현황·재무로 시연한다.

> 📘 **다음에 처음부터 다시 만들 때는 [.ai/docs/DEMO-DUMP-PLAYBOOK.md](../../../../../.ai/docs/DEMO-DUMP-PLAYBOOK.md) 를 먼저 읽어라.**
> 계획 순서, 테이블별 체크리스트, 이번에 걸린 함정, 검증 쿼리, 그리고 주제만 갈아끼우면 되는 프롬프트가 들어 있다.

⛔ **08 은 반드시 마지막**이다 — 가 아니라, **10~14 뒤에 08 을 돌려라.**
`activity_log` 에는 `deleted_at` 이 없어(`D-5`) 되돌릴 수단이 없으므로, 데이터가 다 들어간 뒤 마지막에 한 번만 실행한다.

> **10~14 는 01~09 의 후속 패치다.** 01~09 를 고치는 대신 뒤에 얹는 방식이라 실행 순서를 어기면
> 배치가 깨지거나(`10` 이 없는 블록을 건드림) 버전 번호가 어긋난다(`13` 은 `04` 의 마지막 `version_no` 를 전제로 이어 붙인다).

### ⚠️ 10~14 가 전제하는 것

| 파일 | 전제 | 어기면 |
|---|---|---|
| `10_layout.sql` | 블록 9001~9102 가 그대로 존재 | 행별 `col_span` 합이 3 이 아니게 되어 그리드가 깨진다 (BLK-003) |
| `13_files.sql` | `file` 9001·9003·9004·9005·9010·9011·9013 이 **v1 까지만** 있음 | `version_no` 가 중복되거나 건너뛴다 |
| `14_images.sql` | `img_block` 9001~9008 의 `order_index` 가 각 0·0·0·0·0·0·0·0 부터 연속 | 갤러리 순서가 중복된다 |

화면에서 블록을 직접 추가·삭제했다면 `10_layout.sql` 을 다시 돌리기 전에
`SELECT step_id,row_index,SUM(col_span) FROM block WHERE deleted_at IS NULL GROUP BY 1,2 HAVING SUM(col_span)<>3;`
로 어긋난 행을 먼저 확인하라.

---

## ⭐ 별도 테넌트다 — `company_id = 2` / `company_code = 'vitawear'`

`vitas`(company 1) 는 팀이 쓰고 있다. 같은 테넌트에 섞으면 사번이 충돌하고 목록·권한 화면이 서로 오염된다.

> 🚨 **접두사만 바꾸는 건 안 된다.** 앱이 사번을 직접 만든다 —
> `EmployeeCommandService`·`EmployeeBulkService` 둘 다 `userId = companyCode + "-" + baseUserId`.
> company 1 에 `vitawear-` 를 넣으면 그 회사에서 새로 만든 사원은 `vitas-` 가 되어 **한 회사에 접두사 두 종류**가 생긴다.

| 갈리는 것 | 이유 |
|---|---|
| `department` · `job_position` **전부 새로** (본사·사원·팀장·대표 포함) | `employee` 의 FK 가 복합키 `(company_id, department_id)` 다 (`V20260814150000`). 회사 1 의 것을 참조할 수 없다 |
| `business_category` | UNIQUE 가 `(company_id, name)` — 회사가 다르면 같은 이름도 무방 |
| `project` · `activity_log` · `cash_flow` · `tax_invoice` | 전부 `company_id = 2` |
| 그 외 (`stage`·`step`·`block`·`file`·`issue`·`approval`·`settlement_block` …) | `company_id` 컬럼이 없다. 부모를 타고 결정된다 |

---

## ID 대역 — 기존 시드와 안 겹친다

기존: `project` 1~5·101~105 / `stage` 1~12·201~210 / `step` 1~14·301~320 / `block` 1~14 / `issue` 1~4

| 테이블 | 대역 |
|---|---|
| `company` | **2** (`vitawear`) |
| `department` 9009~9015 · `job_position` | 9007~9012 |
| `employee` | **`vitawear-VW101` ~ `vitawear-VW112`** (14자 · `MAX_USER_ID` 20) |
| `business_category` | 9010~9011 |
| `project` | **9001**(메인) · 9002 · 9003 |
| `stage` | 9001~9006 |
| `step` | 9001~9015 (메인) · 9101~9112 · 9201~9204 (곁들이) |
| `block` | 9001~9100 · 9101~9102 (삭제 시연용) |
| 블록 상세 PK | `text` 9001~9056 · `checklist_block` 9001~9018 / `checklist` 9001~9107 · `image_block` 9001~9008 / `image` 9001~9021 · `vitamate_block` 9001 |
| `file` 9001~9017 · `file_version` | 9001~9028 |
| `issue` · `issue_assign` · `issue_block` | 9001~9013 |
| `settlement_block` 9001~9003 · `cash_flow` 9001~9005 · `tax_invoice` | 9001~9004 |
| `project_business_category` | 9001~9004 |
| `approval` 9001~9005 · `revision` 9001~9006 · `line` 9001~9015 · `document` | 9001~9007 |
| `activity_log` 9001~9034 · `notification` | 9001~9003 |

---

## ⚠️ 이 덤프가 **넣지 않는** 것

| 안 넣음 | 이유 | 대안 |
|---|---|---|
| 🚨 **`account` (로그인 계정)** | `FLYWAY.md` §4 — *민감 정보·**더미 비밀번호**를 SQL에 쓰지 않는다*. PUBLIC 레포다 | 아래 **「계정 만들기」** 참고. 비밀번호는 각자 정하고, 생성된 SQL 은 **커밋하지 않는다** |
| 파일 실물 (S3 오브젝트) | `file_version.storage_key` 만 채운다 | 다운로드는 404 난다. **발표에서 파일을 열지 마라** — 목록·버전 표시까지만 |
| 이미지 실물 | `image.image_url` 만 채운다 | 〃 |

---

## 🚨 활동 로그가 문서와 다르다 (2026-08-15 확인)

`V20260804123025__align_activity_log_schema.sql` 이 `project_id` · `resource_type` · `target_name` · `privileged_override` 를 **DROP** 했고 `block_id` 를 **`NOT NULL`** 로 바꿨다. `ActivityLogEntity` 도 그대로다.

| 결과 | |
|---|---|
| 기록 범위 | **블록 사건만.** 프로젝트·스테이지·스텝·멤버·이슈 로그는 **넣을 자리가 없다** |
| `act` | `ENUM('create','delete','modify','restore','purge')` — **소문자** |
| `target_name` | `resource_name TEXT` 로 대체 |
| ⛔ `privileged_override` | **컬럼이 없다** — `PRJ-017` 「상위권한으로 수정」 시연 불가 |

→ `08_activity_log.sql` 은 **블록 스코프 34건**만 넣는다.
→ `ERD.md` §5-4 확정본과 마이그레이션이 어긋나 있다. `PRJ-V1.md` §5-1 이 경고한 것과 같은 종류의 문제이며 **팀 확인이 필요하다** (요구사항을 접을지, 컬럼을 되살릴지).

---

## 🔑 계정 만들기 — 덤프 밖에서 한 번만

비밀번호 인코더는 **Argon2** 다 (`SecurityConfig` · `saltLength 16 / hashLength 32 / parallelism 1 / memoryKb 65536 / iterations 3`).
파라미터가 해시 문자열 안에 함께 저장되므로 **어떤 도구로 만들든 검증은 통과한다.**

⚠️ 회사 2 에는 ADMIN 이 없고, **회사 1 관리자로는 만들 수 없다** — 사번 접두사가 자기 회사 코드로 붙기 때문이다.
그래서 첫 계정은 DB 에 직접 넣어야 한다.

```java
// Argon2Hash.java — spring-security-crypto + bcprov + spring-jcl 을 클래스패스에
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
public class Argon2Hash {
    public static void main(String[] a) {
        System.out.println(new Argon2PasswordEncoder(16, 32, 1, 65536, 3).encode(a[0]));
    }
}
```

jar 3개는 Gradle 캐시에 이미 있다 — `spring-security-crypto` · `bcprov-jdk18on` · **`spring-jcl`**
(마지막 것을 빼면 `NoClassDefFoundError: LogFactory` 가 난다).

출력 형식: `$argon2id$v=19$m=65536,t=3,p=1$<salt>$<hash>`

그 해시로 `account` 12행을 넣는다. `must_change_password = 0`(첫 로그인 변경 강제 해제) ·
`terms_agreed_at` 채움(약관 화면 건너뛰기) · **배수진(VW112)만 `MASTER`**.

> ⛔ `ADMIN` 은 사원에게 주는 role 이 아니라 **개발자가 발급하는 시스템 계정**이다 (`PRESENTATION.md` §7 주의).
> ⛔ 생성된 SQL 을 커밋하지 마라. 12명이 같은 비밀번호를 쓰므로 **로컬 데모 전용**이다.

---

## ⭐ 연결 상태 커버리지 — 「연결됨」만 있으면 화면 절반이 빈다

연결할 수 있는 것과 **연결 안 해도 되는 것**은 다른 상태다. 둘 다 있어야 재무 화면이 산다.

| 원장 | 연결됨 | 미연결 | 연결 제외 | 전체 |
|---|:-:|:-:|:-:|:-:|
| `cash_flow` (입출금) | 1 | **2** | **2** | 5 |
| `tax_invoice` (세금계산서) | 2 | **1** | **1** | 4 |

| 상태 | 조건 | 화면에서 |
|---|---|---|
| **연결됨** | `settle_block_id IS NOT NULL` | 정산 3열에 ✓ 로 뜬다 |
| **미연결** | `settle_block_id IS NULL` **AND** `is_excluded = 0` | 미연결 건수에 잡히고 매칭 화면 **상단에 정렬**된다 |
| **연결 제외** | `settle_block_id IS NULL` **AND** `is_excluded = 1` | 집계에서 빠진다. 정산 대상이 아니다 |

> `totalCount` 는 `is_excluded` 무관 전체다 (`api/finance.md` §86~90). 미연결 건수와 기준이 다르다.
> 세금계산서 미연결은 입출금과 **다른 개념**이라 `taxInvoiceUnlinkedCount` 로 따로 내려간다.

⭐ **`cash_flow` 9002 가 매칭 화면의 주인공이다.** 적요 `무신사정산조정202603`, 금액 1,240,000 —
2차 정산 조정분처럼 보이지만 회차 금액과 안 맞는다. *"자동 매칭은 후보 추천까지만, 확정 버튼은 사람이 누른다"* 를 그대로 보여준다.
⛔ **리허설에서 이걸 연결하지 마라.** 매칭 화면이 빈 목록이 된다.

이것으로 `PATCH /finance/cash-flows/exclude` · `/tax-invoices/exclude` 배치 처리도 대상이 생긴다.

---

## 검증 쿼리

```sql
-- 진척률 10/15 = 67%
SELECT status, COUNT(*) FROM step WHERE project_id = 9001 AND deleted_at IS NULL GROUP BY status;

-- 블록 100개 · 스텝마다 3열이 2~3행
SELECT s.sort_order, s.name, COUNT(b.block_id) AS blocks
FROM step s LEFT JOIN block b ON b.step_id = s.step_id AND b.deleted_at IS NULL
WHERE s.project_id = 9001 GROUP BY s.step_id ORDER BY s.sort_order;

-- 배치 검증: 행마다 col_span 합이 3인가
SELECT step_id, row_index, SUM(col_span) FROM block
WHERE step_id BETWEEN 9001 AND 9015 AND deleted_at IS NULL
GROUP BY step_id, row_index HAVING SUM(col_span) <> 3;

-- 다형성: type_id 가 상세 행을 정확히 가리키나 (FILE 은 NULL 이 정상)
SELECT type, COUNT(*) , SUM(type_id IS NULL) AS null_type_id
FROM block WHERE step_id BETWEEN 9001 AND 9015 GROUP BY type;

-- 정산 3금액 일치 (0행이어야 정상)
SELECT sb.round_no, sb.planned_amount, ti.total_amount, cf.amount
FROM settlement_block sb
LEFT JOIN tax_invoice ti ON ti.settle_block_id = sb.settle_id
LEFT JOIN cash_flow  cf ON cf.settle_block_id = sb.settle_id
WHERE sb.project_id = 9001
  AND (ti.total_amount <> sb.planned_amount OR cf.amount <> sb.planned_amount);

-- issue_block 이 같은 스텝인가 (0행이어야 정상 · BLK-009)
SELECT ib.* FROM issue_block ib
JOIN issue i ON i.issue_id = ib.issue_id
JOIN block b ON b.block_id = ib.block_id
WHERE i.step_id <> b.step_id;
```

---

## 되돌리기

Flyway 밖이라 `flyway_schema_history` 에 안 남는다. ID 대역이 전부 9000+ 라 **대역으로 지운다.**

```sql
-- 역순으로. FK 때문에 순서가 중요하다.
DELETE FROM activity_log  WHERE block_id  BETWEEN 9001 AND 9102;
DELETE FROM issue_block   WHERE issue_id  BETWEEN 9001 AND 9011;
DELETE FROM issue_assign  WHERE issue_id  BETWEEN 9001 AND 9011;
DELETE FROM issue         WHERE issue_id  BETWEEN 9001 AND 9011;
DELETE FROM cash_flow     WHERE settle_block_id BETWEEN 9001 AND 9003;
DELETE FROM tax_invoice   WHERE settle_block_id BETWEEN 9001 AND 9003;
-- … 이하 03~07 의 대역. 각 파일 하단 주석에 적어두었다.
```

⚠️ **운영 DB 에서 실행하지 마라.** 대역 삭제라 같은 ID 가 운영에 있으면 진짜 데이터가 지워진다.
