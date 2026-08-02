# 💰 재무관리 v1 (입금 · 세금계산서 · 정산현황) — API 목록

**최종 업데이트**: 2026-08-03 (ERD↔API 필드 매핑표 신설 · P-44 해소 · 미결 번호 정리)
**최종 업데이트**: 2026-08-03 (ERD 확정본 반영 — 미결 12건 → 9건)
**최종 업데이트**: 2026-08-01 (신설)
**담당**: 동훈
**근거**: [`PAY-V1.md`](PAY-V1.md) · [`TAX-V1.md`](TAX-V1.md) · [`STL-V1.md`](STL-V1.md) + 각 USECASE 문서 · **[`ERD.md`](ERD.md)**
**상세 명세**: [`FIN-V1-API-DETAIL.md`](FIN-V1-API-DETAIL.md)

> ⛔ **전 엔드포인트 `📝 초안` 이다. 노션 반영 전까지 구현 금지** (`AGENTS.md` §3 상태 게이트).
> 이 문서는 요구사항 3종에서 도출한 **설계안**이다. 노션에 올려 `✅ 확정` 을 받아야 코드를 쓴다.

---

## 0. 공통 규약

### 응답 공통 봉투

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |

### 0-0. ⭐ ERD ↔ API 필드 매핑 (확정본 기준)

> ⚠️ **컬럼명과 API 필드명이 다른 것이 4개 있다.** 아래 표가 유일한 근거다 — 구현할 때 직관으로 바꾸지 마라.

| 테이블 | 컬럼 | 타입 | API 필드 | API 타입 |
| --- | --- | --- | --- | --- |
| `payment` | `payment_id` | BIGINT | `paymentId` | Long |
| | `paid_at` | **DATE** | `paidAt` | **LocalDate** (시각 아님) |
| | `amount` | DECIMAL(18,2) | `amount` | BigDecimal |
| | `payer_name` | VARCHAR(200) | `payerName` | String |
| | `bank_memo` | VARCHAR(500) | `bankMemo` | String |
| | `source_type` | VARCHAR(20) | `sourceType` | String (`MANUAL`·`CSV`) |
| | `bank_txn_id` | VARCHAR(100) UNIQUE | `bankTxnId` | String |
| | `block_id` | BIGINT NULL | `blockId` | Long |
| | `matched_by`·`confirmed_by`·`linked_by` | **VARCHAR(20)** | `matchedBy.userId`·`confirmedBy.userId`·`linkedBy.userId` | **String (사번)** |
| | `matched_at`·`confirmed_at`·`linked_at` | DATETIME | `matchedAt`·`confirmedAt`·`linkedAt` | LocalDateTime |
| `tax_invoice` | `tax_invoice_id` | BIGINT | `taxInvoiceId` | Long |
| | `approval_no` | VARCHAR(100) UNIQUE | `approvalNo` | String (**승인번호**) |
| | **`issued_on`** | DATE | **`issuedAt`** ⚠️ **이름 다름** | LocalDate |
| | `supply_amount` | DECIMAL(18,2) | `supplyAmount` | BigDecimal |
| | **`vat_amount`** | DECIMAL(18,2) | **`taxAmount`** ⚠️ **이름 다름** | BigDecimal |
| | `total_amount` | DECIMAL(18,2) | `totalAmount` | BigDecimal |
| | `buyer_name` | VARCHAR(200) | `buyerName` | String |
| | `buyer_biz_no` | VARCHAR(20) | `buyerBizNo` | String |
| | **`created_at`** | DATETIME | **`collectedAt`** ⚠️ **이름 다름** (수집 일시) | LocalDateTime |
| `block_payment_confirm` | `round_no` | INT | `roundNo` | int |
| | `planned_date` | **DATE** | `plannedDate` | **LocalDate** |
| | `planned_amount` | DECIMAL(18,2) | `plannedAmount` | BigDecimal |
| `block` | `title` | VARCHAR(200) | `title` (= **회차명** · PCB-002) | String |
| | `owner` | VARCHAR(20) | `manager.userId` (정산현황) / `owner` (블록 API) | **String (사번)** |
| `project` | `client_name` | VARCHAR(200) | `clientName` | String |
| | `contract_amount` | DECIMAL(18,2) | `contractAmount` | BigDecimal |

✅ **`issued_on` → `issuedAt`, `vat_amount` → `taxAmount` 로 확정 (2026-08-03).**
컬럼명과 다르지만 **API 필드명은 이 표가 정본**이다. 엔티티 매핑에서 `@Column(name="issued_on")` 처럼 명시해 붙인다.
⚠️ `payment.paid_at` · `block_payment_confirm.planned_date` 는 **`DATE`** 다. `LocalDateTime` 으로 받지 마라 ([`ERD.md`](ERD.md) §0 결정 7).

### ⭐ ERD 정합 — ✅ 확정본 반영 (2026-08-03)

스키마는 [`ERD.md`](ERD.md) 로 확정됐다. **필드 타입은 그 문서를 따른다.**
⚠️ 단 **ERD Cloud 에 아직 안 들어간 항목이 있다** → [`ERD.md`](ERD.md) §8

| 항목 | 반영 |
| --- | --- |
| 사람 식별자 (`matchedBy` · `confirmedBy` · `linkedBy` · `manager`) | `userId` **String (사번 `VARCHAR(20)`)** — ⛔ `Long` 아님 |
| 프로젝트 상태 필터 | `NOT_STARTED`·`IN_PROGRESS`·`SETTLEMENT`·`COMPLETED`·`CLOSED` |
| 프로젝트 계약금액 | ✅ `project.contract_amount DECIMAL(18,2)` — 금액의 **유일한 저장 지점** (INV-08) |
| ✅ **발주처** | `project.client_name VARCHAR(200)` — `clientName` 응답·필터가 열렸다 |
| 입금↔블록 연결 | ✅ `payment.block_id BIGINT NULL` — ⛔ **UNIQUE 없음** (1블록 N입금) |
| 입금 확정 | ✅ `payment.confirmed_by`·`confirmed_at` — **매칭(`matched_*`)과 별개 컬럼** |
| 회차 계획 | ✅ `block_payment_confirm.round_no`·`planned_date`·`planned_amount` |
| 계산서 본체 | ✅ `tax_invoice` 전 컬럼 확정 (`approval_no` UNIQUE · `buyer_name` NOT NULL 등) |
| 입금확인 블록 | `block.type = 'PAYMENT_CONFIRM'` |
| 계산서 조회 블록 | `block.type = 'TAX_INVOICE_VIEW'` · **스텝당 1개** (TXL-001B) |
| 담당자 | ✅ `block.owner VARCHAR(20)` — 상위 행은 **다음 예정일 회차 블록의 owner** (`STL-V1.md` §5-6) |

### 권한 표기

| 표기 | 의미 | 근거 |
| --- | --- | --- |
| `재무 담당자` | `page_permission` 에 `page_code='FINANCE'` + `EDITOR` | PAY-001 · TAX-001 |
| `재무 열람자` | `page_code='FINANCE'` 행 보유 (`VIEWER` 이상) | PAY-009 · TAX-010 · STL-001 |
| `스텝 접근 권한` | `step_permission` 판정 결과 `VIEWER` 이상 | PCB-003 · TXL-006 |
| `스텝 EDITOR` | `step_permission` 판정 결과 `EDITOR` | PCB-001 · TXL-001 |

⚠️ **실무자는 재무 탭에 못 들어온다.** 프로젝트 참여자는 **블록으로만** 입금·계산서를 본다 (PAY-009 · TAX-010 · STL-007/INV-07).
⚠️ **블록 쪽은 전원 읽기 전용.** 스텝 `EDITOR` 여도 확정·연결 요청은 403 (PCB-003 · TXL-006).

### 공통 에러

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_ACCESS_DENIED` | 재무 탭 접근 권한 없음 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무 편집 권한 없음 (`FINANCE`+`EDITOR` 아님) |

---

## 1. 전체 엔드포인트 (28)

### 1-1. 입금 (P-40 · P-41) — [`PAY-V1.md`](PAY-V1.md)

| 개발상태 | 명세상태 | 도메인 | 기능 | Method | URL | 권한 | 설명 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 개발 전 | 📝 초안 | Payment | 입금 목록 조회 | GET | `/api/v1/payments` | 재무 열람자 | 기간·매칭 여부·프로젝트로 필터한 입금 목록을 **미매칭 우선**으로 조회한다. |
| 개발 전 | 📝 초안 | Payment | 입금 상세 조회 | GET | `/api/v1/payments/{paymentId}` | 재무 열람자 | 입금 1건의 상세와 매칭·연결 상태를 조회한다. |
| 개발 전 | 📝 초안 | Payment | 입금 직접 등록 | POST | `/api/v1/payments` | 재무 담당자 | 입금 건을 직접 등록한다. 미매칭 상태로 저장된다. |
| 개발 전 | 📝 초안 | Payment | 입금 수정 | PATCH | `/api/v1/payments/{paymentId}` | 재무 담당자 | 입금 금액·일자·적요를 수정한다. |
| 개발 전 | 📝 초안 | Payment | 입금 삭제 | DELETE | `/api/v1/payments/{paymentId}` | 재무 담당자 | 입금을 삭제한다. 블록에 연결돼 있으면 409. |
| 개발 전 | 📝 초안 | Payment | 입금 확정 | POST | `/api/v1/payments/{paymentId}/confirm` | 재무 담당자 | 재무가 입금을 확정한다. 확정자를 기록한다. |
| 개발 전 | 📝 초안 | Payment | 거래내역 CSV 수집 | POST | `/api/v1/payments/import/csv` | 재무 담당자 | 은행 거래내역 CSV 를 컬럼 매핑과 함께 올려 일괄 등록한다. |
| 개발 전 | 📝 초안 | PaymentMatch | 매칭 후보 조회 | GET | `/api/v1/payments/{paymentId}/match-candidates` | 재무 열람자 | 발주처↔적요 · 금액 · 입금일 근접 순 후보를 조회한다. |
| 개발 전 | 📝 초안 | PaymentMatch | 프로젝트 매칭 | PATCH | `/api/v1/payments/{paymentId}/project` | 재무 담당자 | 입금을 프로젝트에 매칭한다. **필수 1단계**. |
| 개발 전 | 📝 초안 | PaymentMatch | 프로젝트 매칭 해제 | DELETE | `/api/v1/payments/{paymentId}/project` | 재무 담당자 | 매칭을 해제한다. 블록 연결 상태면 409. |
| 개발 전 | 📝 초안 | PaymentMatch | 입금확인 블록 연결 | PATCH | `/api/v1/payments/{paymentId}/block` | 재무 담당자 | 입금을 입금확인 블록에 연결한다. **선택 2단계**. |
| 개발 전 | 📝 초안 | PaymentMatch | 블록 연결 해제 | DELETE | `/api/v1/payments/{paymentId}/block` | 재무 담당자 | 블록 연결을 해제한다. 삭제 잠금이 풀린다. |
| 개발 전 | 📝 초안 | PaymentConfirmBlock | 정산 회차 생성 | POST | `/api/v1/steps/{stepId}/payment-confirm` | 스텝 EDITOR | 정산 스텝에 입금확인 블록(=회차)을 만든다. 스텝당 1개. |
| 개발 전 | 📝 초안 | PaymentConfirmBlock | 회차 정보 수정 | PATCH | `/api/v1/blocks/{blockId}/payment-confirm` | 스텝 EDITOR | 회차번호·예정일·예정금액을 수정한다. |
| 개발 전 | 📝 초안 | PaymentConfirmBlock | 회차 상세 조회 | GET | `/api/v1/blocks/{blockId}/payment-confirm` | 스텝 접근 권한 | 연결된 입금을 건별·합계로 조회한다. **읽기 전용**. |

### 1-2. 세금계산서 (P-42 · P-22A) — [`TAX-V1.md`](TAX-V1.md)

| 개발상태 | 명세상태 | 도메인 | 기능 | Method | URL | 권한 | 설명 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 개발 전 | 📝 초안 | TaxInvoice | 계산서 목록 조회 | GET | `/api/v1/tax-invoices` | 재무 열람자 | 기간·프로젝트·매칭 여부로 필터한 계산서 목록을 조회한다. |
| 개발 전 | 📝 초안 | TaxInvoice | 계산서 상세 조회 | GET | `/api/v1/tax-invoices/{taxInvoiceId}` | 재무 열람자 | 계산서 1건의 상세와 매칭·연결 상태를 조회한다. |
| 개발 전 | 📝 초안 | TaxInvoice | 홈택스 CSV 수집 | POST | `/api/v1/tax-invoices/import/csv` | 재무 담당자 | 홈택스 CSV 를 컬럼 매핑과 함께 올려 일괄 등록한다. 매출분만. |
| 개발 전 | 📝 초안 | TaxInvoice | 홈택스 API 수집 | POST | `/api/v1/tax-invoices/import/hometax` | 재무 담당자 | 홈택스 API 로 발행 내역을 조회해 등록한다. |
| 개발 전 | 📝 초안 | TaxInvoice | 계산서 제거 | DELETE | `/api/v1/tax-invoices/{taxInvoiceId}` | 재무 담당자 | 잘못 수집된 계산서를 논리 삭제한다. |
| 개발 전 | 📝 초안 | TaxInvoiceMatch | 매칭 후보 조회 | GET | `/api/v1/tax-invoices/{taxInvoiceId}/match-candidates` | 재무 열람자 | 공급받는자↔발주처 일치 순 후보를 조회한다. |
| 개발 전 | 📝 초안 | TaxInvoiceMatch | 프로젝트 매칭 | PATCH | `/api/v1/tax-invoices/{taxInvoiceId}/project` | 재무 담당자 | 계산서를 프로젝트에 매칭한다. |
| 개발 전 | 📝 초안 | TaxInvoiceMatch | 프로젝트 매칭 해제 | DELETE | `/api/v1/tax-invoices/{taxInvoiceId}/project` | 재무 담당자 | 매칭을 해제한다. 블록 연결 상태면 409. |
| 개발 전 | 📝 초안 | TaxInvoiceLink | 조회 블록 연결 | PATCH | `/api/v1/tax-invoices/{taxInvoiceId}/block` | 재무 담당자 | 계산서를 세금계산서 조회 블록에 연결한다. 1블록 1계산서. |
| 개발 전 | 📝 초안 | TaxInvoiceLink | 블록 연결 해제 | DELETE | `/api/v1/tax-invoices/{taxInvoiceId}/block` | 재무 담당자 | 블록 연결을 해제한다. 삭제 잠금이 풀린다. |
| 개발 전 | 📝 초안 | TaxInvoiceLink | 조회 블록 상세 조회 | GET | `/api/v1/blocks/{blockId}/tax-invoice` | 스텝 접근 권한 | 발행일·공급가액·세액·합계를 조회한다. **읽기 전용**. |

⚠️ **세금계산서 조회 블록 생성은 프로젝트 도메인 `POST /api/v1/steps/{stepId}/blocks` 다** (`type=TAX_INVOICE_VIEW`).
그 API 가 **스텝당 1개** 제약(TXL-001B)을 검사해야 한다 — 두 번째 생성은 409.

### 1-3. 정산 현황 (P-44) — [`STL-V1.md`](STL-V1.md)

| 개발상태 | 명세상태 | 도메인 | 기능 | Method | URL | 권한 | 설명 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 개발 전 | 📝 초안 | Settlement | 정산 현황 목록 조회 | GET | `/api/v1/settlements` | 재무 열람자 | 프로젝트 축 정산 현황을 **지연 우선**으로 조회한다. |
| 개발 전 | 📝 초안 | Settlement | 프로젝트 회차 상세 조회 | GET | `/api/v1/settlements/{projectId}` | 재무 열람자 | 행 펼침 — 회차별 상세와 미연결 입금·계산서를 조회한다. |

⛔ **정산 현황에는 쓰기 API 가 없다** (STL-008 · INV-01).

---

## 2. 요구사항 ↔ 엔드포인트 대응

| 요구사항 | 엔드포인트 |
| --- | --- |
| PAY-001~003 | `POST /api/v1/payments` |
| PAY-004 · 005 · 009 | `GET /api/v1/payments` |
| PAY-006 | `POST .../payments/{id}/confirm` |
| PAY-007 | `PATCH .../payments/{id}` |
| PAY-008 | `DELETE .../payments/{id}` |
| IMP-001~008 | `POST /api/v1/payments/import/csv` |
| MTC-001 · 005B | `PATCH`·`DELETE .../payments/{id}/project` |
| MTC-002~004 · 007~010 | `PATCH`·`DELETE .../payments/{id}/block` |
| MTC-005 · 006 | `GET .../payments/{id}/match-candidates` |
| PCB-001 · 001B | `POST /api/v1/steps/{stepId}/payment-confirm` |
| PCB-002~002D | `PATCH /api/v1/blocks/{blockId}/payment-confirm` |
| PCB-003 · 004 · 004B | `GET /api/v1/blocks/{blockId}/payment-confirm` |
| PCB-005 · 006 | `DELETE /api/v1/blocks/{id}` · `DELETE /api/v1/steps/{id}` (프로젝트 도메인) |
| TAX-001~003 · 006~008 | `POST /api/v1/tax-invoices/import/csv` |
| TAX-004 · 005 | `POST /api/v1/tax-invoices/import/hometax` |
| TAX-007B | `DELETE /api/v1/tax-invoices/{id}` |
| TAX-009 · 010 | `GET /api/v1/tax-invoices` |
| TXM-001 · 003~005 | `PATCH`·`DELETE .../tax-invoices/{id}/project` |
| TXM-002 | `GET .../tax-invoices/{id}/match-candidates` |
| TXM-006 · TXL-002~005B · 007 | `PATCH`·`DELETE .../tax-invoices/{id}/block` |
| TXL-001 | `POST /api/v1/steps/{stepId}/blocks` (프로젝트 도메인 · `type=TAX_INVOICE_VIEW`) |
| TXL-006 · 008 | `GET /api/v1/blocks/{blockId}/tax-invoice` |
| STL-001~014 | `GET /api/v1/settlements` |
| STL-013 · 015~019 | `GET /api/v1/settlements/{projectId}` |

---

## 3. 🚧 명세 확정 전 확인 필요

### ✅ 해소됨 (2026-08-03 · [`ERD.md`](ERD.md) 확정)

| 이전 미결 | 해소 |
| --- | --- |
| `payment` 출처·은행 거래 고유번호 | ✅ `source_type VARCHAR(20)` · `bank_txn_id VARCHAR(100) UNIQUE` |
| `payment.block_id` | ✅ `BIGINT NULL` FK — UNIQUE 없음 |
| `block_payment_confirm` 회차 컬럼 | ✅ `round_no`·`planned_date`·`planned_amount` |
| `tax_invoice` 본체 컬럼 | ✅ 전 컬럼 확정 |
| **`project` 발주처** | ✅ `client_name VARCHAR(200)` |
| 정산현황 기간 필터 기준 | ✅ **회차 예정일** |
| 받은 금액에 미확정 입금 포함 | ✅ **포함** (STL-009) |
| 회차 1개에 계산서 몇 장 | ✅ **스텝당 조회 블록 1개** (TXL-001B) |
| 정산현황 담당자 정의 | ✅ **다음 예정일 회차 블록의 `block.owner`** |
| `activity_log` 스키마 | ✅ 확정 → [`../프로젝트/ERD.md`](../프로젝트/ERD.md) §5-4 |

### 🚧 남은 것

| # | 항목 | 영향 | 근거 |
| --- | --- | --- | --- |
| 1 | `activity_log` **기록 지점** | ✅ 스키마는 확정됐다(`block_id`·`project_id` 둘 다 NULL 허용) → [`../프로젝트/ERD.md`](../프로젝트/ERD.md) §5-4. **어느 사건에 로그를 남길지**만 미정 | [`HANDOFF.md`](../HANDOFF.md) §L-2 |
| 2 | 🚨 홈택스 API 접근 방식 미정 | `POST .../import/hometax` 의 파라미터·인증 방식 | `TAX-V1.md` §5-6 |
| 3 | 🚨 우리 사업자번호 보관 위치 미정 (설정값 / 하드코딩) | 매출/매입 구분(TAX-002B · INV-09) | `TAX-V1.md` §5-7B |
| 4 | 🚨 CSV 에 고유번호·승인번호 열이 실제로 있는지 미확인 | 없으면 IMP-006 · TAX-006B 가 전 행을 실패시킨다 | `PAY-V1.md` §5-6 · `TAX-V1.md` §5-7 |
| 5 | 🚨 **ERD Cloud 미반영분** | `payment.block_id` · `tax_invoice.project_id` 가 아직 안 들어가 있다 | [`ERD.md`](ERD.md) §8 |
| 6 | MTC-005 유사도 계산식 미정 | 매칭 후보 정렬 기준 | `PAY-V1.md` §5-3 |
| 7 | P-25 (프로젝트 정산) 후순위 | 이 문서에 엔드포인트를 만들지 않았다 | `STL-V1.md` §2-B |
| 8 | 인증 방식 (세션 쿠키 / 토큰) 미확정 | 전 API 401 처리 · `AUTH_TOKEN_EXPIRED` 문구와 Spring Session 이 안 맞는다 | [`FIN-V1-API-FLOW.md`](FIN-V1-API-FLOW.md) §0-3 · `.ai/API.md` §2 |

> ✅ **`P-44` 미승인은 해소됐다** — [`PAGE.md`](../../global/PAGE.md) §2-5 에 등재돼 있다 (`STL-V1.md` §5-1).
