# 데모 시드 — 비타에듀 K-디지털 기초역량훈련 심사 신청

**시나리오 정본**: [`.ai/docs/DEMO-SCENARIO-KDT.md`](../../../../../../.ai/docs/DEMO-SCENARIO-KDT.md)
**제작 지침**: [`.ai/docs/DEMO-DUMP-PLAYBOOK.md`](../../../../../../.ai/docs/DEMO-DUMP-PLAYBOOK.md)

> ⛔ **이 폴더는 Flyway 마이그레이션이 아니다.**
> `application.yml` 의 `spring.flyway.locations: classpath:db/migration` 이라 `db/demo` 는 **자동 적용되지 않는다.**
> `FLYWAY.md` §5 — *"개발용 더미 데이터는 Flyway에 넣지 않고 별도 수동 절차로 관리한다."*

> 🚨 **아직 어느 DB 에도 적용하지 않았다.** 검증 쿼리를 한 번도 돌리지 않았다는 뜻이다.
> 첫 적용 때는 반드시 각 파일 하단의 검증 쿼리를 순서대로 돌려라 — 불변식 위반은 화면에서 안 보인다.

---

## 의류 데모(`../`)와의 관계

| | 의류 데모 | **이 폴더** |
|---|---|---|
| 회사 | `company_id 2` · `vitawear` | **`company_id 3` · `vitaedu`** |
| 주제 | 무신사 입점·시즌 운영 | **정부 심사 신청(K-디지털 기초역량훈련)** |
| ID 대역 | 9000번대 | **8000번대** |
| 입찰 도메인 | 안 씀 (`bid_notice_id` 전부 NULL) | **씀 — 공고에서 시작한다** |

**둘은 병존한다.** 서로 다른 테넌트라 목록·권한·재무가 섞이지 않는다.

---

## 실행

로컬/dev DB 에 **순서대로** 적용한다. 전부 명시적 PK + `INSERT IGNORE` 라 재실행해도 중복이 스킵된다.

```bash
for f in src/main/resources/db/demo/kdt/*.sql; do mysql -h "$DB_HOST" -u "$DB_USER" -p "$DB_NAME" < "$f"; done
```

| # | 파일 | 내용 |
|:-:|---|---|
| 01 | `01_master.sql` | ⭐ 회사 3 · 부서 6 · 직급 7 · 사원 13 · 사업 카테고리 3 |
| 02 | `02_bid.sql` | 수집조건 1 · 실행 2 · **공고 7** · 첨부 5 · AI 요약 1 · 회사 상태 7 · 상태이력 6 · 사내문서 6 / 버전 8 |
| 03 | `03_project.sql` | 프로젝트 10 · 카테고리 연결 12 · 참여자 51 · 스테이지 12 · **스텝 50** · 스텝권한 4 |
| 04 | `04_blocks.sql` | **블록 252**(활성 250 + 삭제 2) · `text` 144 · `checklist_block` 25 / `checklist` 135 · `image_block` 15 / `image` 34 · `settlement_block` 11 |
| 05 | `05_files.sql` | `file` 29 · `file_version` 40 · `block_file` 29 |
| 06 | `06_issues.sql` | `issue` 71 · `issue_assign` 76 · `issue_block` 67 — **50개 스텝 전부** |
| 07 | `07_finance.sql` | `cash_flow` 22 · `tax_invoice` 20 — 연결됨 / 미연결 / 연결 제외 **3상태 전부** |
| 08 | `08_approval.sql` | `approval` **43** · `revision` 44 · `line` 86 · `document` 14 |
| 09 | `09_bid_review.sql` | `bid_review` 1 · 확정 요약에 프로젝트 연결 |
| 10 | `10_page_permission.sql` | 화면 접근 권한 **4** (`BIDDING` 3 · `FINANCE` 1) |
| 11 | `11_activity_log.sql` | 활동 로그 34 · 알림 3 — ⛔ K-디지털 시드의 맨 마지막 |
| 12 | `12_kb_bid.sql` | ⭐ **KB 추가분** — KB국민은행 디지털 위탁교육 공고 8011(직접등록)·첨부 6·AI요약·상태·이력 |
| 13 | `13_kb_project.sql` | P8011 · 스테이지 7 · 스텝 17 · 참여자 11 · 스텝권한 4 |
| 14 | `14_kb_blocks.sql` | 블록 121(+삭제 2) · text 57 · checklist 75 · image 19 · 정산 8 · 결재블록 13 |
| 15 | `15_kb_files.sql` | file 21 · file_version 27 · block_file |
| 16 | `16_kb_issues.sql` | issue 16 · issue_assign · issue_block |
| 17 | `17_kb_finance.sql` | cash_flow 14 · tax_invoice 12 — ⚠️ **KB 위탁료는 과세** (KDT 훈련비 면세와 반대) |
| 18 | `18_kb_approval.sql` | approval 13 · revision · line · document |
| 19 | `19_kb_bid_review.sql` | bid_review 8011 · summary.project_id = 8011 연결 |
| 20 | `20_kb_activity.sql` | 활동 로그 27 · 알림 3 — ⛔ **반드시 맨 마지막** |

### 순서를 어기면 깨지는 곳

| 파일 | 전제 | 어기면 |
|---|---|---|
| `03` | `02` 의 `bid_notice` 8001 | `project.bid_notice_id` FK 위반 |
| `04` | `03` 의 스텝 8001~8050 | FK 위반 |
| `05`·`06`·`07`·`08` | `04` 의 블록 · 정산 블록 | 결재가 목록에서 사라지고 정산이 빈다 |
| `08` | `05` 의 `file_version` | `approval_document` FK 위반 |
| `09` | `03` 의 `project` 8001 | `bid_review.project_id` FK 위반 |
| `11` | 01~10 전부 | 되돌릴 수 없다. `deleted_at` 이 없다 |

---

## ⭐ 별도 테넌트다 — `company_id = 3` / `company_code = 'vitaedu'`

> 🚨 **접두사만 바꾸는 건 안 된다.** 앱이 사번을 직접 만든다 —
> `EmployeeCommandService`·`EmployeeBulkService` 둘 다 `userId = companyCode + "-" + baseUserId`.
> 다른 회사에 `vitaedu-` 를 넣으면 그 회사에서 새로 만든 사원은 다른 접두사가 되어
> **한 회사에 접두사 두 종류**가 생긴다.

| 갈리는 것 | 이유 |
|---|---|
| `department` · `job_position` **전부 새로** | `employee` FK 가 복합키 `(company_id, department_id)` 다 |
| `business_category` | UNIQUE 가 `(company_id, name)` |
| `project` · `activity_log` · `cash_flow` · `tax_invoice` · `company_document` · `bid_review` | 전부 `company_id = 3` |
| `bid_notice` | ⚠️ **공용 테이블이다.** 회사별 격리는 `company_bid_notice_state` 로만 된다 |
| 그 외 (`stage`·`step`·`block`·`file`·`issue`·`approval`·`settlement_block` …) | `company_id` 컬럼이 없다. 부모를 타고 결정된다 |

---

## ID 대역 — 전부 8000번대

적용 전 **`8000~8999` 가 비어 있는지 반드시 확인해라.** (2026-08-16 dev RDS 기준 전 테이블 0건)

```sql
SELECT 'project' t, SUM(project_id BETWEEN 8000 AND 8999) FROM project
UNION ALL SELECT 'block', SUM(block_id BETWEEN 8000 AND 8999) FROM block
UNION ALL SELECT 'approval', SUM(approval_id BETWEEN 8000 AND 8999) FROM approval;
```

| 테이블 | 대역 |
|---|---|
| `company` | **3** (`vitaedu`) |
| `department` 8001~8006 · `job_position` | 8001~8007 |
| `employee` | **`vitaedu-VE101` ~ `vitaedu-VE113`** (13자 · 컬럼은 varchar 20) |
| `business_category` | 8001~8003 |
| `crawl_condition` 8001 · `crawl_run` | 8001~8002 |
| `bid_notice` 8001~8007 · `attachment` 8001~8005 · `summary` 8001 · `state` 8001~8007 · `status_history` | 8001~8006 |
| `company_document` 8001~8006 · `company_document_version` | 8001~8008 |
| `project` | **8001**(메인) · 8002 · 8003~8010 |
| `stage` 8001~8012 · `step` | 8001~8050 |
| `block` | 8001~8250 · **8251~8252**(삭제 시연용) |
| 블록 상세 | `text` 8001~8144 · `checklist_block` 8001~8025 / `checklist` 8001~8135 · `image_block` 8001~8015 / `image` 8001~8034 |
| `settlement_block` | 8001~8011 |
| `file` 8001~8029 · `file_version` 8001~8040 |
| `issue` 8001~8071 · `issue_assign` 8001~8076 · `issue_block` | 8001~8067 |
| `cash_flow` 8001~8022 · `tax_invoice` | 8001~8020 |
| `approval` 8001~8043 · `revision` 8001~8044 · `line` 8001~8086 · `document` | 8001~8014 |
| `bid_review` 8001 · `page_permission` | 8001~8004 |
| `activity_log` 8001~8034 · `notification` | 8001~8003 |

---

## 🚨 의류 데모에서 **복사하면 안 되는** 것 셋

| | 왜 |
|---|---|
| `09_page_permission.sql` 의 15행 | `page_permission` 행이 생기는 코드는 **`BIDDING`·`FINANCE` 둘뿐이다** (`PageCode.Category.GRANTABLE`). `MY_PROJECT`·`PROJECT_CREATE`·`ADMIN_CONSOLE` 행은 INSERT 는 되지만 화면 판정에 아무 영향이 없다 |
| `tax_invoice` 의 부가세 10퍼센트 | **훈련비는 면세다.** 매출 계산서 3건은 `tax_amount = 0` 이고 공급가 = 총액이다. 외주비(매입)만 과세다 |
| ADMIN 에게 결재·프로젝트를 주는 것 | 의류 데모는 ADMIN 6명에게 24건을 기안시켰다가 `23_admin_policy.sql` 로 전부 되돌렸다. 여기서는 처음부터 뺐다 |

---

## 역할별로 보이는 것

| role | 인원 | 결재관리 | 내 프로젝트 | 공고 | 재무 | 관리자 콘솔 |
|---|:-:|---|:-:|:-:|:-:|:-:|
| MEMBER | 10 | 내가올린 3~6건 · **결재대기 1건 이상** | ✅ | VE101·VE103·VE102 만 | VE109 만 | ❌ |
| MASTER | 1 (VE111) | 내가올린 3 · 결재대기 1 · `scope=all` | ✅ | ✅ (role) | ✅ (role) | ✅ |
| ADMIN | 2 | **403 (명세대로)** | ❌ | ✅ (role) | ✅ (role) | ✅ |

> ⛔ ADMIN 계정으로 결재 페이지를 열지 마라. `.ai/api/approval.md` 상 결재 권한이 없다.
> 관리자 콘솔·전사현황으로 시연한다.

---

## ⚠️ 이 덤프가 **넣지 않는** 것

| 안 넣음 | 이유 | 대안 |
|---|---|---|
| 🚨 **`account` (로그인 계정)** | `FLYWAY.md` §4 — 민감 정보·**더미 비밀번호**를 SQL 에 쓰지 않는다. PUBLIC 레포다 | 아래 「계정 만들기」. 생성된 SQL 은 **커밋하지 않는다** |
| 파일 실물 (S3 오브젝트) | `file_version.storage_key` 만 채운다 | 다운로드는 404 난다. **발표에서 파일을 열지 마라** |
| 이미지 실물 | `image.image_url` 만 채운다 | 〃 |
| 공고 첨부 실물 | `bid_notice_attachment.storage_key` 만 채운다 | 〃 |
| `settlement_block` 의 계좌 정보 | 앱이 암호화해 저장하고 조회 시 복호화 후 마스킹한다. 평문을 넣으면 복호화가 깨진다 | `bank_name`·`account_number`·`account_holder` 는 NULL 이 정상이다 |

---

## 🔑 계정 만들기 — 덤프 밖에서 한 번만

비밀번호 인코더는 **Argon2** 다 (`SecurityConfig` · `saltLength 16 / hashLength 32 / parallelism 1 / memoryKb 65536 / iterations 3`).

⚠️ 회사 3 에는 ADMIN 이 없고, **다른 회사 관리자로는 만들 수 없다** — 사번 접두사가 자기 회사 코드로 붙는다.
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

출력 형식: `$argon2id$v=19$m=65536,t=3,p=1$<salt>$<hash>`

| 지킬 것 | 왜 |
|---|---|
| `must_change_password = 0` | 안 그러면 로그인 직후 비밀번호 변경 화면에 막힌다 |
| `terms_agreed_at` 채움 | 안 그러면 약관 화면에 막힌다 |
| 계정마다 `encode()` 를 따로 호출 | 같은 해시를 복붙하면 비밀번호가 같다는 게 해시만 봐도 드러난다 |
| role — MEMBER 10(VE101~110) · MASTER 1(VE111) · ADMIN 2(VE112·VE113) | ADMIN 은 사원에게 주는 role 이 아니라 개발자가 발급하는 시스템 계정이다 |
| 평문은 **어떤 커밋 파일에도 쓰지 않는다** | PUBLIC 레포다 |

---

## 시연 급소 셋

| | 무엇을 증명하나 |
|---|---|
| **공고 → 요약 → 검토 → 프로젝트** | 공고 8001 을 직접 등록하고, AI 요약을 확정하고, 사내 자료와 비교 검토한 결과가 프로젝트 8001 이 됐다. 의류 데모로는 못 보여주는 구간이다 |
| **자막 품질** | 스텝 8007 은 8월 14일 완료인데 「7차시 자막 동기화 재검수」 이슈가 미완이다. 「완료된 스텝」 배지가 뜨고, 그게 자막 외주 잔금 4,400,000 이 `WAITING` 인 이유와 이어진다 |
| **3차 훈련비** | 계산서는 8월 5일에 발행됐고 입금 예정은 8월 25일이다. 기준일 8월 16일에 정확히 `WAITING` 이고, 기한 미도래라 **지연으로도 안 잡힌다** |

⛔ **리허설에서 건드리지 마라**

| 대상 | 건드리면 |
|---|---|
| 이슈 8011 「7차시 자막 동기화 재검수」 | 완료 처리하면 「완료된 스텝」 배지가 사라진다 |
| `cash_flow` 8022 「훈련비조정202607 / 1,742,400」 | 연결하면 매칭 화면이 빈 목록이 된다 |

---

## 되돌리기

Flyway 밖이라 `flyway_schema_history` 에 안 남는다. ID 가 전부 8000번대라 **대역으로 지운다.**

⚠️ **역순으로. FK 때문에 순서가 중요하다.** 각 파일 하단 주석에도 같은 내용이 있다.

```sql
DELETE FROM notification        WHERE notification_id BETWEEN 8001 AND 8003;
DELETE FROM activity_log        WHERE activity_log_id BETWEEN 8001 AND 8034;
DELETE FROM page_permission     WHERE page_permission_id BETWEEN 8001 AND 8004;
UPDATE bid_notice_summary SET project_id = NULL WHERE bid_notice_summary_id = 8001;
DELETE FROM bid_review          WHERE bid_review_id = 8001;
DELETE FROM approval_document   WHERE approval_document_id BETWEEN 8001 AND 8014;
DELETE FROM approval_line       WHERE approval_line_id     BETWEEN 8001 AND 8086;
DELETE FROM approval_revision   WHERE approval_revision_id BETWEEN 8001 AND 8044;
DELETE FROM approval            WHERE approval_id BETWEEN 8001 AND 8043;
DELETE FROM tax_invoice         WHERE tax_id       BETWEEN 8001 AND 8020;
DELETE FROM cash_flow           WHERE cash_flow_id BETWEEN 8001 AND 8022;
DELETE FROM issue_block         WHERE issue_block_id  BETWEEN 8001 AND 8067;
DELETE FROM issue_assign        WHERE issue_assign_id BETWEEN 8001 AND 8076;
DELETE FROM issue               WHERE issue_id        BETWEEN 8001 AND 8071;
DELETE FROM block_file          WHERE file_id BETWEEN 8001 AND 8029;
DELETE FROM file_version        WHERE file_version_id BETWEEN 8001 AND 8040;
DELETE FROM file                WHERE file_id BETWEEN 8001 AND 8029;
DELETE FROM checklist           WHERE chk_id       BETWEEN 8001 AND 8135;
DELETE FROM checklist_block     WHERE chk_block_id BETWEEN 8001 AND 8025;
DELETE FROM image               WHERE img_id       BETWEEN 8001 AND 8034;
DELETE FROM image_block         WHERE img_block_id BETWEEN 8001 AND 8015;
DELETE FROM `text`              WHERE txt_id       BETWEEN 8001 AND 8144;
DELETE FROM settlement_block    WHERE settle_id    BETWEEN 8001 AND 8011;
DELETE FROM block               WHERE block_id     BETWEEN 8001 AND 8252;
DELETE FROM step_permission     WHERE step_permission_id BETWEEN 8001 AND 8004;
DELETE FROM step                WHERE project_id BETWEEN 8001 AND 8010;
DELETE FROM stage               WHERE project_id BETWEEN 8001 AND 8010;
DELETE FROM project_business_category WHERE project_business_category_id BETWEEN 8001 AND 8012;
DELETE FROM project_member      WHERE project_id BETWEEN 8001 AND 8010;
DELETE FROM project             WHERE project_id BETWEEN 8001 AND 8010;
DELETE FROM company_document_version  WHERE company_document_version_id BETWEEN 8001 AND 8008;
DELETE FROM company_document          WHERE company_document_id BETWEEN 8001 AND 8006;
DELETE FROM bid_notice_status_history WHERE bid_notice_status_history_id BETWEEN 8001 AND 8006;
DELETE FROM company_bid_notice_state  WHERE company_bid_notice_state_id  BETWEEN 8001 AND 8007;
DELETE FROM bid_notice_summary    WHERE bid_notice_summary_id = 8001;
DELETE FROM bid_notice_attachment WHERE bid_notice_attachment_id BETWEEN 8001 AND 8005;
DELETE FROM bid_notice            WHERE bid_notice_id BETWEEN 8001 AND 8007;
DELETE FROM crawl_run             WHERE crawl_run_id  BETWEEN 8001 AND 8002;
DELETE FROM crawl_condition       WHERE crawl_condition_id = 8001;
DELETE FROM business_category WHERE company_id = 3;
DELETE FROM employee          WHERE company_id = 3;
DELETE FROM job_position      WHERE company_id = 3;
DELETE FROM department        WHERE company_id = 3 AND parent_id IS NOT NULL;
DELETE FROM department        WHERE company_id = 3;
DELETE FROM company           WHERE company_id = 3;
```

⚠️ **운영 DB 에서 실행하지 마라.** 대역 삭제라 같은 ID 가 운영에 있으면 진짜 데이터가 지워진다.

---

## 검증

각 파일 하단에 그 파일 전용 검증 쿼리가 있다. 전체를 한 번에 보려면
[`99_verify.sql`](99_verify.sql) 을 돌려라 — **전부 0이어야 정상이다.**
