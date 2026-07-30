# 🗂️ ERD — 핵심 도메인

**최종 업데이트**: 2026-07-31 (초안)
**근거**: [`DOMAIN.md`](DOMAIN.md) · 실제 DDL 은 `src/main/resources/db/migration/V202607311200__core_schema.sql`

> 이 문서는 **읽기용 지도**다. 컬럼의 정확한 타입·제약은 마이그레이션 파일이 원본이다.
> 둘이 다르면 **마이그레이션이 맞다.**

---

## 전체 조감

```mermaid
flowchart LR
    subgraph BID["공고 · 입찰 — 프로젝트 이전"]
        CL[crawl_link] --> BN[bid_notice] --> BS[bid_notice_summary]
    end

    subgraph PRJ["프로젝트 — 본체"]
        P[project] --> ST[stage] --> S[step] --> B[block]
        S --> I[issue]
    end

    subgraph FIN["재무 — 프로젝트 밖 · 병렬"]
        PAY[payment]
        TI[tax_invoice]
        PERF[performance]
    end

    subgraph ORG["조직 · 계정 — 기반"]
        U[users] --> UR[user_role]
        D[department]
        BC[business_category]
    end

    BN -.스냅샷 복사.-> P
    PAY -.① 프로젝트 매칭.-> P
    PAY -.② 블록 연결.-> B
    U --> PRJ
```

**핵심**: 재무는 프로젝트 **안**이 아니라 **옆**에 있다. 입금이 프로젝트로 들어오는 경로가 **2단**이다.

---

## 1. 조직 · 계정

```mermaid
erDiagram
    department ||--o{ users : "소속"
    users ||--o{ user_role : "다중 부여"

    department {
        bigint id PK
        varchar name UK
        datetime deleted_at
    }
    users {
        bigint id PK
        varchar login_id UK
        varchar name
        bigint department_id FK
        varchar position "직급"
        varchar status "ACTIVE·INACTIVE"
        tinyint password_change_required
        date resigned_on "퇴사해도 레코드는 남는다"
    }
    user_role {
        bigint id PK
        bigint user_id FK
        varchar role "DEVELOPER·ADMIN·EXECUTIVE·FINANCE·BIDDING·MEMBER"
    }
    business_category {
        bigint id PK
        varchar name UK
    }
```

- **계정과 사원은 한 테이블(`users`)** 이다. 이벤트스토밍에서 둘로 나뉜 건 행위 종류가 달라서지 엔티티가 둘이라서가 아니다
- **`user_role` 은 부서에서 상속하지 않는다.** 인원별 지정만 — 잊었을 때 닫히는 쪽으로 실패해야 한다
- ~~`MASTER`~~ · ~~`QUALITY`~~ role 은 없다. Master 는 결재선의 마지막 노드고, 품질검토는 결재선의 한 노드다

---

## 2. 공고 → 프로젝트

```mermaid
erDiagram
    crawl_link ||--o{ bid_notice : "주기 크롤링"
    bid_notice ||--o{ bid_notice_summary : "AI 요약"
    bid_notice ||--o{ project : "스냅샷 복사 (선택)"
    business_category ||--o{ project : ""

    bid_notice {
        bigint id PK
        varchar notice_no
        varchar title
        decimal base_amount "기초금액"
        decimal estimated_price "추정가격"
        datetime bid_deadline_at "투찰마감 — 불가역"
        datetime opening_at "개찰일 — 불가역"
        varchar evaluation_type
    }
    project {
        bigint id PK
        varchar name
        varchar status "NOT_STARTED·IN_PROGRESS·SETTLEMENT·COMPLETED·CLOSED"
        bigint bid_notice_id FK "NULL이면 B2B 직접 생성"
        tinyint notice_changed "재크롤링으로 원본 변경됨"
        decimal contract_amount "계약금액 — 잔액 계산의 기준"
        varchar close_reason_code "종결 사유 필수"
        int issue_seq "이슈 번호 발급용"
    }
```

- **`bid_notice_id` 가 NULL 인 프로젝트가 정상 케이스**다 (B2B 는 계약부터 시작)
- 재크롤링은 **프로젝트 값을 덮지 않는다.** `notice_changed` 배지만 세우고 사람이 반영 여부를 결정한다
- **금액은 전부 프로젝트 속성**이다. 블록에 흩지 않는다

---

## 3. 계층 · 권한

```mermaid
erDiagram
    project ||--o{ stage : ""
    project ||--o{ step : ""
    stage ||--o{ step : "분류만"
    step ||--o{ block : "소유"
    project ||--o{ project_member : ""
    project ||--o{ project_department : "담당팀 = 집계 라벨"
    step ||--o{ step_permission : "오버라이드"

    stage {
        bigint id PK
        varchar name
        int sort_order
    }
    step {
        bigint id PK
        bigint project_id FK
        bigint stage_id FK "NULL 허용"
        varchar name "타입이 없다"
        int sort_order "정렬일 뿐. 선행 강제 없음"
        date started_on "날짜가 있으면 캘린더에 뜬다"
        bigint owner_user_id FK "책임자 — 작업자가 아니다"
        varchar status "NOT_STARTED·IN_PROGRESS·DONE"
        bigint completed_by "송부 스텝에서는 곧 발송자"
        datetime completed_at "송부 스텝에서는 곧 발송일시"
    }
    project_member {
        bigint project_id FK
        bigint user_id FK
        varchar permission "VIEWER·EDITOR·MANAGER"
    }
    step_permission {
        bigint step_id FK
        bigint user_id FK
        varchar permission
    }
```

- **권한 저장은 프로젝트 + 스텝 2층뿐.** `stage` 에는 권한 테이블이 **없다** — 3층 오버라이드는 사람이 추적 못 한다
- **스테이지는 라벨이다.** 삭제 시 하위 스텝을 다른 스테이지로 옮기기만 한다
- 스텝 완료는 **수동 버튼**이다. 이슈가 미완료여도 완료할 수 있다

---

## 4. 블록 — 공통 + 타입별 상세

```mermaid
erDiagram
    block ||--o| block_text : "TEXT"
    block ||--o| block_memo : "MEMO"
    block ||--o{ checklist_item : "CHECKLIST"
    block ||--o{ block_file_version : "FILE"
    block ||--o| block_image : "IMAGE"
    block ||--o| block_ai : "AI"
    block ||--o| block_approval : "APPROVAL"
    block ||--o| block_payment_confirm : "PAYMENT_CONFIRM"
    block_approval ||--o{ approval_line : "순차"
    block_file_version }o--|| attachment : ""
    block_image }o--|| attachment : ""

    block {
        bigint id PK
        bigint step_id FK "소유자는 언제나 스텝"
        bigint project_id FK "비정규화 — 전역 조회용"
        varchar type "닫힌 enum"
        varchar title "입금확인 블록에서는 회차명"
        int sort_order
    }
    block_file_version {
        bigint id PK
        bigint block_id FK
        int version_no "파일은 하나, 버전이 붙는다"
        bigint attachment_id FK
    }
    block_approval {
        bigint block_id PK
        varchar status "DRAFT·IN_PROGRESS·APPROVED·REJECTED·WITHDRAWN"
        bigint target_block_id FK "파일 블록을 지목"
        bigint target_version_id FK "상신 시점 버전 고정"
        int current_line_order
    }
    approval_line {
        bigint id PK
        int line_order "마지막이 Master"
        bigint approver_user_id FK
        varchar status "WAITING·ACTIVE·APPROVED·REJECTED"
        text comment "반려 사유 필수"
    }
    checklist_item {
        bigint id PK
        varchar content
        tinyint checked
        bigint issue_id FK "이슈로 승격 시 연결"
    }
```

**두 가지가 이 도메인의 급소다:**

1. **`block_approval.target_version_id`** — 결재는 **파일을 복사하지 않고 특정 버전을 지목**한다. 이게 없으면 "대표가 v4 를 승인했는데 지금은 v5" 상황에서 결재 이력이 거짓이 된다
2. **재상신 시 재개 지점은 이 값으로 자동 판정**한다. 버전이 그대로면 반려 지점부터, 바뀌었으면 처음부터 전원 재승인

---

## 5. 재무 — 2단 매칭

```mermaid
erDiagram
    project ||--o{ payment : "① 프로젝트 매칭 (필수)"
    payment ||--o| block_payment_confirm : "② 블록 연결 (나중 가능)"
    block ||--o| block_payment_confirm : ""
    project ||--o{ tax_invoice : ""
    project ||--o{ performance : ""

    payment {
        bigint id PK
        bigint project_id FK "NULL = 미매칭"
        datetime paid_at
        decimal amount
        varchar bank_memo "적요 — 용역명은 안 온다"
        bigint matched_by "확정은 사람이 누른다"
    }
    block_payment_confirm {
        bigint block_id PK
        bigint payment_id FK "채워지면 블록·스텝 삭제 불가"
        bigint linked_by "재무만 연결·해제"
    }
```

**왜 2단인가 — 돈이 블록보다 먼저 들어올 수 있기 때문이다.**

```
계약 직후 선급금 입금  →  재무가 프로젝트에 매칭 (①)
                       →  실무자는 아직 정산 스텝을 안 팠다
                       →  ①만 있는 상태로 대기
                       →  입금확인 블록이 생기면 ② 연결
```

1단만 있으면 그 입금은 시스템 밖에서 떠돈다 — Excel 시절과 똑같다.
프로젝트 > 정산 메뉴는 **`블록이 있다 OR 매칭된 입금이 있다`** 로 활성화된다.

---

## 6. 이슈 · 로그 · 알림 · 템플릿

```mermaid
erDiagram
    step ||--o{ issue : "소유"
    issue ||--o{ issue_block : "연관 블록 (선택)"
    block ||--o{ issue_block : ""
    project ||--o{ activity_log : ""
    users ||--o{ notification : ""
    users ||--o{ template : ""

    issue {
        bigint id PK
        bigint project_id FK
        bigint step_id FK "소유. 변경으로 이동 가능"
        int issue_no "프로젝트 전역 연번"
        varchar status "TODO·IN_PROGRESS·DONE"
        bigint assignee_user_id FK "담당자가 있어야 이슈다"
        date due_on
    }
    activity_log {
        bigint id PK
        varchar action
        varchar target_type
        bigint target_id
        varchar target_name "⚠️ 스냅샷 — 원본이 지워져도 읽혀야 한다"
        tinyint admin_override "ADMIN 권한으로 수정했는가"
    }
    notification {
        bigint id PK
        varchar kind "ACTION(뱃지) · INFO(피드)"
        varchar category
        datetime resolved_at "ACTION은 처리까지 안 사라진다"
    }
    template {
        bigint id PK
        varchar scope_type "PROJECT·STAGE·STEP·BLOCK"
        varchar visibility "PRIVATE·TEAM·COMPANY"
        json payload "구조+설정 스냅샷"
    }
```

- **이슈 소유는 스텝 하나.** `project_id` 는 전역 번호와 조회 편의를 위한 비정규화다
- **이슈는 진척률에 안 들어간다.** 프로젝트 진척률은 `완료 스텝 / 전체 스텝` 하나뿐이다
- **`activity_log.target_name` 을 반드시 채워라.** FK 만 두면 대상이 지워질 때 로그 표시가 깨진다
- **템플릿은 `payload` JSON 하나**로 끝난다. 중첩이 스냅샷이라 하위 템플릿을 참조하지 않기 때문이다

---

## 테이블 목록 (27개)

| 영역 | 테이블 |
|------|--------|
| 조직·계정 | `department` `users` `user_role` `business_category` |
| 공고·입찰 | `crawl_link` `bid_notice` `bid_notice_summary` |
| 프로젝트 | `project` `project_member` `project_department` `stage` `step` `step_permission` |
| 블록 | `block` `block_text` `block_memo` `checklist_item` `attachment` `block_file_version` `block_image` `block_ai` `block_approval` `approval_line` `block_payment_confirm` |
| 재무 | `payment` `tax_invoice` `performance` |
| 이슈 | `issue` `issue_block` |
| 템플릿 | `template` |
| 로그·알림 | `activity_log` `notification` |

> `TAX_INVOICE_VIEW` · `PERFORMANCE_VIEW` 블록은 **상세 테이블이 없다.** 조회 전용이라 `block.type` 만으로 충분하다.
