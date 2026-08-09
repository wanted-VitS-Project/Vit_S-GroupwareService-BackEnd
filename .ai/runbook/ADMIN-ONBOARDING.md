# 🏢 신규 회사(테넌트) + ADMIN 온보딩 런북

**최종 업데이트**: 2026-08-10 · **담당**: DevOps(김동현)

> ⚠️ **왜 이 문서가 필요한가** — 멀티테넌트 전환(P1-1a)에서 HR 4테이블의 `company_id` **DEFAULT 를 제거**했다.
> 그래서 회사·ADMIN 을 직접 INSERT 할 때 **`company_id` 를 반드시 명시**해야 한다. 빠뜨리면
> `Field 'company_id' doesn't have a default value` 로 INSERT 가 실패한다(과거처럼 조용히 1번 회사로 찍히지 않는다).
>
> 🔒 이 문서는 **절차만** 담는다. 실제 회사코드·비밀번호·해시는 여기에 쓰지 마라 (⚠️ PUBLIC 레포).

---

## 0. 준비물

| 항목 | 결정 주체 | 규칙 |
|---|---|---|
| **회사 코드**(`company_code`) | 고객사 요청 | URL-safe 소문자, 전역 유일(`uk_company_code`). 사번 접두사로 쓰인다 (`{code}-{사번}`). ⚠️ **길이 제약**: `len(company_code) + 1 + len(base사번) ≤ 20` (`user_id` 컬럼 폭). 즉 `company_code` 가 19자 이상이면 base 사번을 넣을 자리가 없다 |
| **회사명**(`name`) | 고객사 | 표시용 |
| **ADMIN 초기 비밀번호** | DevOps 생성 | 평문 보관 금지. Argon2id 해시만 DB 에 넣는다(아래 2단계) |

> 사번 접두사 길이 예산: `user_id` 컬럼은 **20자** = `len(company_code) + 1('-') + len(base사번)`.
> base 최대 길이 = `20 - len(company_code) - 1`. (예: 코드 `vitas`(5) → `vitas-`(6) → base ≤ 14자)
> ⚠️ 이 계산은 문서뿐 아니라 **회사 생성·사원 등록 애플리케이션 검증에도 동일하게** 적용한다.

---

## 1. 회사 INSERT → `company_id` 확보

```sql
INSERT INTO company (name, company_code) VALUES ('{{회사명}}', '{{company_code}}');
SELECT company_id FROM company WHERE company_code = '{{company_code}}';  -- 이하 {{COMPANY_ID}} 로 사용
```

## 2. ADMIN 초기 비밀번호 → Argon2id 해시 생성

- 애플리케이션과 **같은 인코더**로 해시해야 로그인이 된다: `Argon2PasswordEncoder(16, 32, 1, 65536, 3)`
  = (saltLength=16, hashLength=32, parallelism=1, memory=65536KB, iterations=3).
- 생성 방법(택1): 일회용 부트스트랩 유틸/테스트로 `encode("{{초기비번}}")` 출력 → 그 해시 문자열을 `{{PW_HASH}}` 로 사용.
- ⚠️ 평문 비밀번호·해시를 git·이슈·채팅에 남기지 마라. 고객사에는 안전 채널로 초기 비번만 전달한다.

## 3. ADMIN 사원(employee) INSERT

> ⚠️ **3·4단계는 하나의 트랜잭션이다.** employee 만 커밋되고 account 가 실패하면 **계정 없는 ADMIN 사원**이 남는다.
> 아래처럼 `START TRANSACTION` … `COMMIT` 으로 묶고, 어느 INSERT 라도 실패하면 `ROLLBACK` 한다.

```sql
START TRANSACTION;

-- 3. ADMIN 사원(employee)
-- user_id = {{company_code}}-{{base사번}}  (전역 유일). ADMIN 은 실제 인사담당자라 is_system=0.
-- department_id·job_position_id 는 아직 부서/직급이 없으면 NULL 로 둔다(나중에 배정).
INSERT INTO employee (user_id, company_id, name, is_system, department_id, job_position_id,
                      email, phone, hired_at)
VALUES ('{{company_code}}-{{base}}', {{COMPANY_ID}}, '{{ADMIN이름}}', 0, NULL, NULL,
        '{{ADMIN이메일}}', NULL, CURDATE());

-- 4. ADMIN 계정(account)
-- role=ADMIN 은 이 절차(직접 발급)로만 부여된다 — API 로는 부여 불가(ACC-023).
-- must_change_password=1 → 최초 로그인 시 비번 변경 강제.
INSERT INTO account (user_id, password, role, status, must_change_password, login_fail_count)
VALUES ('{{company_code}}-{{base}}', '{{PW_HASH}}', 'ADMIN', 'ACTIVE', 1, 0);

COMMIT;  -- 어느 INSERT 라도 실패하면 ROLLBACK;
```

## 5. 검증

| 확인 | 방법 |
|---|---|
| 로그인 | `user_id = {{company_code}}-{{base}}` + 초기 비번으로 로그인 → 세션에 `companyId={{COMPANY_ID}}` 적재 |
| 회사 격리 | 로그인 후 사원/부서/직급 목록이 **이 회사 것만** 보이는지 |
| 비번 변경 강제 | 최초 로그인이 비번 변경 플로우로 유도되는지(`must_change_password=1`) |

---

## 이후 흐름 (ADMIN 이 직접)

1. ADMIN 로그인 → **부서·직급 생성** → **사원 등록**(단건/엑셀). 등록 시 `company_id` 는 세션에서 자동 스탬핑된다.
2. 사원 `user_id` 도 `{{company_code}}-` 접두사가 자동으로 붙는다(입력은 base 사번만).
3. 업로드 파일은 S3 `companies/{{COMPANY_ID}}/...` 접두사 아래로 저장된다.

## 주의

- **`company_id` 컬럼이 있는 테이블(예: `employee`)에 직접 INSERT 할 땐 반드시 명시** — DEFAULT 없음. `account` 에는 `company_id` 컬럼이 없다 — 계정의 회사 범위는 `account.user_id → employee.user_id` 로 이어져 **`employee.company_id` 를 통해 파생**된다(조회도 employee 조인으로 회사를 판정).
- `company_code` 오타 주의 — 사번 접두사로 굳으므로 사후 변경 비용이 크다.
- 시스템/공용 ADMIN(배치용)이 필요하면 `is_system=1` 로 별도 발급하되, 인사관리 대상에서 제외된다.
