# 재무 관리 API 명세

**최종 동기화**: 2026-08-13 (세금계산서 매칭 추천 조회·블록 매칭·매칭 해제 3종 추가, 상단 표에 세금계산서 API 전체 등재)
**최종 동기화**: 2026-08-10 (재무 관리 요약 조회 최초 작성)
**도메인 담당**: (미기재 — 작업자 본인 이름으로 채워주세요)

> 상태가 `✅ 확정` 이상인 항목은 프론트와의 계약이다. 임의 변경 금지.

---

| 상태 | 기능 | METHOD | URL | 권한 |
|------|------|--------|-----|------|
| ✅ 확정 | 재무 관리 요약 조회 | GET | `/api/v1/finance/summary` | 접근 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 입출금 내역 조회 | GET | `/api/v1/finance/cash-flows` | 접근 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 입출금 내역 필터 옵션 조회 | GET | `/api/v1/finance/cash-flows/filters` | 접근 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 세금계산서 조회 | GET | `/api/v1/finance/tax-invoices` | 접근 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 세금계산서 필터 옵션 조회 | GET | `/api/v1/finance/tax-invoices/filters` | 접근 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 세금계산서 CSV 컬럼 추천 조회 | POST | `/api/v1/finance/tax-invoices/csv/preview` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 세금계산서(CSV 기반) 업로드 | POST | `/api/v1/finance/tax-invoices/csv` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 세금계산서 매칭 추천 조회 | GET | `/api/v1/finance/tax-invoices/{taxId}/match-candidates` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 세금계산서 블록 매칭 | PATCH | `/api/v1/finance/tax-invoices/{taxId}/match` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 세금계산서 블록 매칭 해제 | PATCH | `/api/v1/finance/tax-invoices/{taxId}/unmatch` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 세금계산서 메모 수정 | PATCH | `/api/v1/finance/tax-invoices/{taxId}` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 세금계산서 삭제(배치) | DELETE | `/api/v1/finance/tax-invoices` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 세금계산서 연결 제외/포함 처리(배치) | PATCH | `/api/v1/finance/tax-invoices/exclude` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 입출금 내역 CSV 컬럼 추천 조회 | POST | `/api/v1/finance/cash-flows/csv/preview` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 입출금 내역(CSV 기반) 업로드 | POST | `/api/v1/finance/cash-flows/csv` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 입출금 내역 매칭 추천 조회 | GET | `/api/v1/finance/cash-flows/{cashFlowId}/match-candidates` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 입출금 내역 블록 매칭 | PATCH | `/api/v1/finance/cash-flows/{cashFlowId}/match` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 입출금 내역 블록 매칭 해제 | PATCH | `/api/v1/finance/cash-flows/{cashFlowId}/unmatch` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 입출금 내역 직접 등록 | POST | `/api/v1/finance/cash-flows` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 입출금 내역 수정 | PATCH | `/api/v1/finance/cash-flows/{cashFlowId}` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 입출금 내역 삭제(배치) | DELETE | `/api/v1/finance/cash-flows` | 편집 권한 보유자(재무 관리 페이지) |
| ✅ 확정 | 입출금 내역 연결 제외 처리(배치) | PATCH | `/api/v1/finance/cash-flows/exclude` | 편집 권한 보유자(재무 관리 페이지) |

---

## 재무 관리 요약 조회 `GET /api/v1/finance/summary`

**상태**: ✅ 확정
**인증 필요 여부**: Y

재무 관리 페이지 진입 시 입출금 내역 · 세금계산서 · 정산 현황을 한 화면에 요약해서 보여준다.
권한은 정산현황 화면(`.ai/api/settlement.md`)과 동일하게 `PagePermissionPort`(`FINANCE` 페이지)로 판정한다.

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.cashFlow.unlinkedCount` | Int | 입출금 내역 중 정산 블록과 연결되지 않은 건수 |
| `data.cashFlow.totalCount` | Int | 입출금 내역 전체 건수 |
| `data.taxInvoice.unlinkedCount` | Int | 세금계산서 중 정산 블록과 연결되지 않은 건수 |
| `data.taxInvoice.totalCount` | Int | 세금계산서 전체 건수 |
| `data.settlement.unlinkedCount` | Int | 연결되지 않은(미연결) 정산 블록 개수 |
| `data.settlement.inProgressCount` | Int | 상태가 완료·종료가 아닌(진행 중) 프로젝트 개수 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "재무 관리 요약 조회 성공",
  "data": {
    "cashFlow": { "unlinkedCount": 3, "totalCount": 7 },
    "taxInvoice": { "unlinkedCount": 2, "totalCount": 5 },
    "settlement": { "unlinkedCount": 5, "inProgressCount": 3 }
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "재무 관리 요약 조회 성공" |
| 403 | Forbidden | `FINANCE_ACCESS_DENIED` | "접근 권한이 없습니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **403 code — 원 명세에 값이 비어 있었다.** `finance`가 새 도메인이라 팀 에러코드 컨벤션
  (`.ai/API.md` §3-3, 의미식 `{도메인}_{의미}`)에 맞춰 `FINANCE_ACCESS_DENIED`를 신규 부여했다.
  정산현황 3개 API(`settlement` 도메인)가 쓰는 `SETL-009`와는 별개 코드다 — 도메인이 다르면
  접두어도 다르게 선점한다는 기존 규칙을 그대로 따랐다.
- **`cashFlow.unlinkedCount`/`taxInvoice.unlinkedCount` 판정 기준 (사용자 확정)** — 각 테이블의
  `is_excluded = FALSE`(연결 대상에서 제외되지 않음) **AND** `settle_block_id IS NULL`(정산 블록과
  매칭된 적 없음)인 행만 센다. `is_excluded = TRUE`인 행은 애초에 정산 대상이 아니므로 "미연결"이
  아니라 집계에서 아예 빠진다.
- **`cashFlow.totalCount`/`taxInvoice.totalCount`는 `is_excluded` 무관 전체 행 개수다** — 미연결
  건수와 달리 제외 여부를 따지지 않는다. 소프트 삭제된 행(`deleted_at IS NOT NULL`)만 제외한다.
- **`settlement.unlinkedCount` = `status IN ('PENDING','WAITING')`인 활성 정산 블록 개수 (2026-08-13 변경)**
  — 즉 **입출금이 아직 연결되지 않은 회차** 수다. 처음엔 `PENDING`만 셌는데, 세금계산서 매칭이 붙으면서
  `WAITING`(세금계산서만 연결되고 입금은 아직)이 실제로 쓰이기 시작해 **그 회차가 어느 카운트에도 안
  잡히는 문제**가 생겼다. 요약 지표에서는 둘 다 "정산이 아직 안 끝난 회차"로 묶는다(사용자 확정).
  세금계산서 미연결은 이 카운트와 **별개 개념**이라 여기 섞지 않는다 — 정산 현황 프로젝트 조회의
  `taxInvoiceUnlinkedCount`로 따로 내려간다(위 "⭐ 정산 블록 status 규칙" 절 참고).
  삭제 판정은 `settlement_block.deleted_at`과 `block.deleted_at` 둘 다 확인한다
  (정산 현황 블록 조회와 동일 패턴, `block.deleted_at`이 우선이라는 `BLK-007` 규칙 반영).
- **`settlement.inProgressCount`는 정산 블록 유무와 무관하게 전체 프로젝트를 센다 (사용자 확정)** —
  "프로젝트 상태가 완료나 종료되지 않은 진행 중인 프로젝트의 개수"를 그대로 반영해
  `project.status NOT IN ('COMPLETED', 'CLOSED')`(정산 현황 프로젝트 조회의 `includeCompleted`
  판정과 동일 기준)로 카운트했다. 정산현황 필터 옵션 조회처럼 "정산 블록이 있는 프로젝트"로 좁히지
  않는다 — 재무 관리 요약은 회사 전체 진행 상황을 보여주는 화면이라는 사용자 설명에 따른 것.
- **✅ 회사(멀티테넌시) 범위 반영 (2026-08-11 추가)** — 최초 구현 시점엔 `cash_flow`/`tax_invoice`만
  자체 `company_id`가 있었고 `settlement_block`/`project` 집계는 스코프가 없어 **다른 회사 데이터까지
  합산되는 상태**였다(CodeRabbit Major 지적). `develop`에 `project.company_id`가 들어온 뒤
  `findSummary(companyId)`로 바꿔 6개 서브쿼리 전부 현재 회사로 좁혔다 — `settlement_block`은
  `block→step→project`를 타고 `project.company_id`로, 나머지는 자체 컬럼으로 직접.
- **구현 위치 — 신규 `finance` 도메인 패키지 (2026-08-10 확정)** — `feature/finance` 브랜치 작업이라
  `settlement` 패키지에 얹지 않고 `com.group3.vitamins.finance`를 새로 만들었다. `cash_flow`/
  `tax_invoice`/`settlement_block`/`project`는 전부 다른 도메인(settlement·project) 소유 테이블이라
  §2-1/2-2 규칙대로 finance가 **소비자로서** 조회 어댑터를 직접 든다 — 재사용할 기존 서비스 로직이
  없어 `FinanceSummaryMapper`(MyBatis)가 SQL로 직접 집계한다.
  `PagePermissionPort`도 settlement와 별개로 finance 자신의 임시 어댑터를 새로 만들었다(포트는
  소비자가 소유한다는 규칙 그대로 — settlement의 임시 구현을 그대로 복제한 것이지 공유 참조가 아니다).
  finance는 자기 소유 테이블이 없는 순수 조회/오케스트레이션 도메인이라 `auth`와 동일하게
  `domain/model`·`domain/repository`가 없다.
- **`cash_flow`/`tax_invoice`에 `company_id` 추가 (2026-08-10, 아래 입출금 내역 조회와 같이 결정)** —
  두 테이블은 정산 블록에 연결되기 전(또는 영원히 미연결로) `project`를 모르는 상태로 존재할 수 있어서,
  `project`를 거쳐 회사를 유추할 수 없다. `finance/V20260810090000`에서 `company_id BIGINT NOT NULL`
  (DEFAULT 없음)로 마이그레이션했다. `project`/`step`/`block`/`settlement_block`은 아직 회사 컬럼이
  없어 이번 확장과 무관 — 그쪽은 별도 멀티테넌시 확장 대상이다.
  ⚠️ **HR 4테이블(department/job_position/employee/employee_group)과 다르게 DEFAULT 단계를 안 거쳤다** —
  HR은 `DEFAULT 1`로 먼저 깔고 나중에(Phase 1, `V20260809120000`) DEFAULT를 떼는 2단계였는데, 그건
  이미 돌고 있던 기존 INSERT 코드(`EmployeeCommandService` 등)가 컬럼을 몰라도 계속 동작해야 했기
  때문이다. `cash_flow`/`tax_invoice`는 애초에 INSERT하는 코드 자체가 없어서(업로드 API 미구현, 지금은
  전부 수동 SQL) 보호할 기존 경로가 없다 — 한 마이그레이션 파일 안에서 `ADD COLUMN ... DEFAULT 1`(기존
  테스트 행 백필용) 후 바로 `DROP DEFAULT`까지 끝냈다(사용자 확인, "굳이 DEFAULT를 남길 필요 없다").

---

## 입출금 내역 조회 `GET /api/v1/finance/cash-flows`

**상태**: ✅ 확정
**인증 필요 여부**: Y

재무 관리 페이지의 입출금 내역 목록. 권한은 요약 조회와 동일하게 `PagePermissionPort`(`FINANCE` 페이지)로 판정한다.

**Request Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `startDate` | LocalDate | N | 조회 시작일 (`tradedAt` 날짜 기준) |
| `endDate` | LocalDate | N | 조회 종료일 (`tradedAt` 날짜 기준) |
| `unlinked` | Boolean | N | 미연결 항목만 조회 (true: 미매칭, 없으면 전체) |
| `projectId` | Long | N | 매칭 프로젝트 필터 |
| `keyword` | String | N | 적요(`bankMemo`) 또는 입금자명(`depositorName`) 검색 키워드 |
| `page` | Int | N | 0-base 페이지 번호. 생략하면 0 (2026-08-12 페이징 추가) |
| `size` | Int | N | 페이지당 개수. 생략하면 20, 최대 100 |
| `sort` | String | N | 정렬 기준. `TRADED_AT_DESC`(거래일시 최신순, 기본값) \| `TRADED_AT_ASC`(거래일시 오래된순) \| `AMOUNT_DESC`(거래금액 큰순) |

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.page` | Int | 현재 페이지 번호 (0-base) |
| `data.size` | Int | 페이지당 개수 |
| `data.totalElements` | Long | 전체 항목 수 |
| `data.totalPages` | Int | 전체 페이지 수 |
| `data.cashFlows[].cashFlowId` | Long | 입출금 내역 ID |
| `data.cashFlows[].tradedAt` | LocalDateTime | 거래 일시 |
| `data.cashFlows[].bankTxnId` | String | 거래고유번호 |
| `data.cashFlows[].type` | String | 구분 (INCOME/OUTCOME) |
| `data.cashFlows[].amount` | BigDecimal | 거래 금액 |
| `data.cashFlows[].depositorName` | String | 입금자명/수취인명 |
| `data.cashFlows[].bankMemo` | String | 적요/통장 메모 |
| `data.cashFlows[].sourceType` | String | 수집 출처 (MANUAL/CSV/API) |
| `data.cashFlows[].projectId` | Long | 연결된 프로젝트 ID (미연결이거나 프로젝트 자체가 삭제됐으면 null) |
| `data.cashFlows[].projectName` | String | 연결 프로젝트명 (미연결이거나 프로젝트 자체가 삭제됐으면 null) |
| `data.cashFlows[].settleId` | Long | 연결된 정산 블록 아이디 (미연결이면 null — 블록이 삭제돼도 값은 유지됨, `linkStatus` 참고) |
| `data.cashFlows[].roundName` | String | 연결된 정산 블록명 (미연결이면 null — 블록이 삭제돼도 값은 유지됨, `linkStatus` 참고) |
| `data.cashFlows[].linkedBy` | String | 매칭 처리자 사번 (미연결이면 null) |
| `data.cashFlows[].linkedByName` | String | 매칭 처리자 이름 (미연결이면 null) |
| `data.cashFlows[].linkedAt` | LocalDateTime | 매칭 일시 (미연결이면 null) |
| `data.cashFlows[].isExcluded` | Boolean | 연결 제외 여부 |
| `data.cashFlows[].linkStatus` | String | 정산 블록 연결 상태 — `UNLINKED`(미연결) / `LINKED`(연결됨) / `LINK_BLOCK_DELETED`(연결됐던 정산 블록이 삭제됨). 원 명세엔 없던 필드(2026-08-10 추가) |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "입출금 내역 조회 성공",
  "data": {
    "page": 0,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1,
    "cashFlows": [
      {
        "cashFlowId": 1,
        "tradedAt": "2026-07-15T10:30:00",
        "bankTxnId": "신한-20260715103000",
        "type": "INCOME",
        "amount": 30000000,
        "depositorName": "(주)한국기술공사",
        "bankMemo": "선급금",
        "sourceType": "CSV",
        "projectId": null,
        "projectName": null,
        "settleId": null,
        "roundName": null,
        "linkedBy": null,
        "linkedByName": null,
        "linkedAt": null,
        "isExcluded": true,
        "linkStatus": "UNLINKED"
      },
      {
        "cashFlowId": 5,
        "tradedAt": "2026-06-30T09:00:00",
        "bankTxnId": "신한-20260630090000",
        "type": "INCOME",
        "amount": 270000000,
        "depositorName": "환경부",
        "bankMemo": "1차 정산 선급",
        "sourceType": "CSV",
        "projectId": 1,
        "projectName": "한강 생태교육 환경개선사업",
        "settleId": 10,
        "roundName": "1차 정산(선급 60%)",
        "linkedBy": "vitas-EMP004",
        "linkedByName": "김재무",
        "linkedAt": "2026-06-30T14:00:00",
        "isExcluded": false,
        "linkStatus": "LINKED"
      },
      {
        "cashFlowId": 8,
        "tradedAt": "2026-05-10T11:00:00",
        "bankTxnId": "신한-20260510110000",
        "type": "INCOME",
        "amount": 90000000,
        "depositorName": "환경부",
        "bankMemo": "2차 정산 잔금",
        "sourceType": "CSV",
        "projectId": 1,
        "projectName": "한강 생태교육 환경개선사업",
        "settleId": 11,
        "roundName": "2차 정산(잔금)",
        "linkedBy": "vitas-EMP004",
        "linkedByName": "김재무",
        "linkedAt": "2026-05-10T15:00:00",
        "isExcluded": false,
        "linkStatus": "LINK_BLOCK_DELETED"
      }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "입출금 내역 조회 성공" |
| 400 | Bad Request | `FINANCE_PAGE_QUERY_INVALID` | "페이지 조회 조건이 올바르지 않습니다." — `page`<0 · `size`≤0 또는 >100 · `sort` 허용값 아님 · `startDate`>`endDate` |
| 403 | Forbidden | `FINANCE_ACCESS_DENIED` | "접근 권한이 없습니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **`isExcluded` 필드명 확정 (2026-08-10)** — 원 명세는 Response Parameter 표엔 `isExcluded`, Success
  Example JSON엔 `excluded`로 서로 달랐다. 사용자 확인 결과 `isExcluded`가 맞다.
- **`projectName`/`roundName`도 미연결 시 `null` (2026-08-10 정정)** — 원 명세의 Success Example엔
  문자열 `"미연결"`이 박혀 있었으나, 사용자 확인 결과 이건 프론트가 렌더링한 표시값이지 API가 그
  문자열을 직접 내려줘야 한다는 뜻이 아니었다. `projectId`/`settleId`/`linkedBy`류와 동일하게 `null`을
  내려주고, "미연결"로 표시하는 건 프론트 몫이다.
- **`company_id` 필터 (2026-08-10)** — `cash_flow`에 새로 추가한 `company_id` 컬럼(위 요약 조회 메모
  참고)을 `CurrentCompanyIdProvider.currentCompanyId()`로 걸러 조회한다. HR 도메인들과 동일한 패턴.
- **`linkStatus` 신규 추가 + 연결됐던 블록이 삭제된 경우도 구분해서 표시 (2026-08-10)** — 처음엔 연결된
  정산 블록/공용 블록이 삭제되면 미연결과 동일하게(`settleId`/`roundName` 등 전부 null) 취급했으나,
  "실무팀이 삭제 여부와 무관하게 정산 현황엔 안 보이더라도, 입출금 쪽에선 예전에 뭐랑 연결돼 있었는지는
  알아야 한다"는 요청으로 정정. `settlement_block`/`block` 조인에서 `deleted_at IS NULL` 필터를 빼서
  블록이 삭제돼도 `settleId`/`roundName`은 계속 채워지고, 대신 `linkStatus`로 상태를 구분한다
  (`settle_block_id`가 없으면 `UNLINKED`, 있는데 연결된 정산 블록/공용 블록이 삭제됐으면
  `LINK_BLOCK_DELETED`, 그 외엔 `LINKED`). 별도 상태 컬럼을 DB에 추가하지 않고 조회 시점에 계산만
  한다 — 블록 삭제 로직(Block 도메인)이 재무 테이블을 갱신할 필요가 없어 도메인 경계를 넘지 않음.
  단 `project`(위 상위 개념) 조인의 `deleted_at IS NULL` 필터는 그대로 둬서, 프로젝트 자체가 삭제되면
  `projectId`/`projectName`은 여전히 null로 나간다(요청사항 — "프로젝트가 삭제되면 안 보여야지").
- **`unlinked=false`를 명시적으로 보내면 생략과 동일하게 전체 조회** — 명세가 `true`(미매칭만)와 생략
  (전체) 두 경우만 정의해서, `false`는 정의되지 않은 입력이다. 생략과 같은 동작(필터 없음)으로 처리했다.
- **`keyword` 검색** — `bankMemo` 또는 `depositorName`에 부분 일치(`LIKE %keyword%`)로 구현했다.
- **✅ 페이징 추가 (2026-08-12)** — 프론트 요청으로 `page`/`size`/`sort` + `{page,size,totalElements,totalPages}`
  추가(공고·프로젝트 목록과 같은 컨벤션 — `page`≥0·`size`(1~100)·`sort` 허용값 검증, 위반 시
  `FINANCE_PAGE_QUERY_INVALID` 400, silent clamp 아님). **`cashFlows` 키 이름은 유지**(아직 프론트 연동 전이라
  `content`로 바꿀 필요 없다고 확인). `sort` 기본값 `TRADED_AT_DESC` — 이전 고정 정렬(`tradedAt` 내림차순)과
  동작이 같다(생략 시 회귀 없음).

---

## 입출금 내역 필터 옵션 조회 `GET /api/v1/finance/cash-flows/filters`

**상태**: ✅ 확정
**인증 필요 여부**: Y

입출금 내역 조회 화면의 `projectId` 필터 드롭다운을 채운다. 권한은 위 조회와 동일.

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.projects[].projectId` | Long | 프로젝트 ID |
| `data.projects[].projectName` | String | 프로젝트명 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "입출금 내역 필터 옵션 조회 성공",
  "data": {
    "projects": [
      { "projectId": 1, "projectName": "한강 생태교육 환경개선사업" },
      { "projectId": 2, "projectName": "신월 ICD 복합물류센터 구조설계 용역" }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "입출금 내역 필터 옵션 조회 성공" |
| 403 | Forbidden | `FINANCE_ACCESS_DENIED` | "접근 권한이 없습니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **"매칭된 프로젝트 목록"의 범위** — `cash_flow`가 하나라도 연결된 정산 블록을 가진 프로젝트만 대상으로
  했다(정산현황 필터 옵션 조회의 "정산현황에 등장하는" 발주처와 동일한 사고방식, `.ai/api/settlement.md`
  참고). 연결된 입출금 내역이 하나도 없는 프로젝트는 이 드롭다운에 나올 일이 없다.
- **정렬 순서** — 명세에 없어 `projectName` 오름차순으로 뒀다.
- **`company_id` 필터** — 프로젝트 자체엔 아직 `company_id`가 없어(위 메모 참고) 이 조회는 회사로 거르지
  않는다. 대신 `cash_flow.company_id = 현재 회사`인 행에 연결된 프로젝트만 대상으로 삼아 간접적으로
  회사를 좁힌다.

---

## 세금계산서 조회 `GET /api/v1/finance/tax-invoices`

**상태**: ✅ 확정
**인증 필요 여부**: Y

재무 관리 페이지의 세금계산서 목록. 권한은 입출금 내역 조회와 동일하게 `PagePermissionPort`(`FINANCE` 페이지)로
판정한다. **구조는 입출금 내역 조회와 완전히 동일한 컨벤션으로 맞췄다**(2026-08-12, 원 명세가 그 사이 바뀐
입출금 내역 조회 규칙을 못 따라가고 있어서 사용자 확인 후 통일) — 아래 "원 명세와 다르게 처리한 것" 참고.

**Request Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `startDate` | LocalDate | N | 조회 시작일 (`issuedNo` 날짜 기준) |
| `endDate` | LocalDate | N | 조회 종료일 (`issuedNo` 날짜 기준) |
| `unlinked` | Boolean | N | 미연결 항목만 조회 (true: 미매칭, 없으면 전체) |
| `projectId` | Long | N | 매칭 프로젝트 필터 |
| `keyword` | String | N | 승인번호(`approvalNo`) 또는 공급받는자 상호명(`buyerName`) 검색 키워드 |
| `page` | Int | N | 0-base 페이지 번호. 생략하면 0 |
| `size` | Int | N | 페이지당 개수. 생략하면 20, 최대 100 |
| `sort` | String | N | 정렬 기준. `ISSUED_NO_DESC`(발행일 최신순, 기본값) \| `ISSUED_NO_ASC`(발행일 오래된순) \| `AMOUNT_DESC`(합계 큰순) |

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.page` | Int | 현재 페이지 번호 (0-base) |
| `data.size` | Int | 페이지당 개수 |
| `data.totalElements` | Long | 전체 항목 수 |
| `data.totalPages` | Int | 전체 페이지 수 |
| `data.taxInvoices[].taxId` | Long | 세금계산서 ID |
| `data.taxInvoices[].issuedNo` | LocalDate | 발행일 (컬럼명은 `issuedNo`지만 실제 값은 날짜다 — DB 컬럼명 그대로) |
| `data.taxInvoices[].approvalNo` | String | 승인번호 |
| `data.taxInvoices[].type` | String | 구분 (INCOME/OUTCOME) |
| `data.taxInvoices[].buyerName` | String | 공급받는자 상호명 |
| `data.taxInvoices[].buyerBizNo` | String | 공급받는자 사업자번호 |
| `data.taxInvoices[].supplierBizNo` | String | 공급자 사업자번호 (nullable) |
| `data.taxInvoices[].subBizNo` | String | 종사업장번호 (nullable) |
| `data.taxInvoices[].ceoName` | String | 대표자명 (nullable) |
| `data.taxInvoices[].itemName` | String | 품목명 (nullable) |
| `data.taxInvoices[].supplyAmount` | BigDecimal | 공급가액 |
| `data.taxInvoices[].taxAmount` | BigDecimal | 세액 |
| `data.taxInvoices[].totalAmount` | BigDecimal | 합계 |
| `data.taxInvoices[].memo` | String | 비고/메모 (nullable) |
| `data.taxInvoices[].sourceType` | String | 수집 출처 (CSV/HOMETAX_API) |
| `data.taxInvoices[].projectId` | Long | 연결된 프로젝트 ID (미연결이거나 프로젝트 자체가 삭제됐으면 null) |
| `data.taxInvoices[].projectName` | String | 연결 프로젝트명 (미연결이거나 프로젝트 자체가 삭제됐으면 null) |
| `data.taxInvoices[].settleId` | Long | 연결 정산 블록 ID (미연결이면 null — 블록이 삭제돼도 값은 유지됨, `linkStatus` 참고) |
| `data.taxInvoices[].roundName` | String | 연결 정산 블록명 (미연결이면 null — 블록이 삭제돼도 값은 유지됨, `linkStatus` 참고) |
| `data.taxInvoices[].linkedBy` | String | 매칭 처리자 사번 (미연결이면 null) |
| `data.taxInvoices[].linkedByName` | String | 매칭 처리자 이름 (미연결이면 null) |
| `data.taxInvoices[].linkedAt` | LocalDateTime | 매칭 일시 (미연결이면 null) |
| `data.taxInvoices[].isExcluded` | Boolean | 연결 제외 여부 |
| `data.taxInvoices[].linkStatus` | String | 정산 블록 연결 상태 — `UNLINKED`(미연결) / `LINKED`(연결됨) / `LINK_BLOCK_DELETED`(연결됐던 정산 블록이 삭제됨). 원 명세엔 없던 필드 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "세금계산서 조회 성공",
  "data": {
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "taxInvoices": [
      {
        "taxId": 1,
        "issuedNo": "2026-07-20",
        "approvalNo": "20260720-12345678",
        "type": "INCOME",
        "buyerName": "환경부",
        "buyerBizNo": "1234567890",
        "supplierBizNo": "9876543210",
        "subBizNo": null,
        "ceoName": "홍길동",
        "itemName": "환경개선 컨설팅 용역",
        "supplyAmount": 40909090,
        "taxAmount": 4090910,
        "totalAmount": 45000000,
        "memo": null,
        "sourceType": "HOMETAX_API",
        "projectId": null,
        "projectName": null,
        "settleId": null,
        "roundName": null,
        "linkedBy": null,
        "linkedByName": null,
        "linkedAt": null,
        "isExcluded": true,
        "linkStatus": "UNLINKED"
      }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "세금계산서 조회 성공" |
| 400 | Bad Request | `FINANCE_PAGE_QUERY_INVALID` | "페이지 조회 조건이 올바르지 않습니다." — `page`<0 · `size`≤0 또는 >100 · `sort` 허용값 아님 · `startDate`>`endDate` |
| 403 | Forbidden | `FINANCE_ACCESS_DENIED` | "접근 권한이 없습니다." |

**원 명세와 다르게 처리한 것 / 구현 메모** (2026-08-12, 사용자 확인 — "입출금 내역 양식에 맞춰서"):
- **페이징 추가** — 원 명세엔 없었지만 입출금 내역 조회가 먼저 페이징됐고, 세금계산서도 같은 화면 패턴(공고·
  프로젝트 목록과 동일 컨벤션)을 따라야 해서 `page`/`size`/`sort` + `{page,size,totalElements,totalPages}`를
  같이 추가했다. `taxInvoices` 키 이름은 유지.
- **`isExcluded` 필드명** — 원 명세 Success Example엔 `excluded`로 오타가 있었다(Response Parameter 표엔
  `isExcluded`로 이미 맞게 적혀 있었음). 입출금 내역 조회에서 이미 겪은 같은 오타라 그대로 `isExcluded`로
  통일.
- **`projectName`/`roundName` 미연결 시 `null`** — 원 명세 Success Example엔 문자열 `"미연결"`이 박혀
  있었으나, 입출금 내역 조회와 동일하게 이건 프론트 렌더링 표시값이지 API가 그 문자열을 직접 내려줘야
  한다는 뜻이 아니다. `null`로 통일.
- **`linkStatus` 신규 추가** — 원 명세에 없던 필드. 입출금 내역 조회와 동일하게 연결됐던 정산 블록이
  삭제된 경우("한때 연결됐었다"는 이력)를 구분해서 보여준다. `settlement_block`/`block` 조인에서
  `deleted_at IS NULL` 필터를 뺐다(입출금 내역 조회와 동일 패턴).
- **403 `code` 값 — 원 명세엔 빈칸이었다.** 입출금 내역 조회와 같은 권한 판정(`PagePermissionPort`, `FINANCE`
  페이지)이라 `FINANCE_ACCESS_DENIED`를 그대로 재사용했다.
- **`keyword` 검색** — `approvalNo` 또는 `buyerName`에 부분 일치(`LIKE %keyword%`)로 구현했다(입출금 내역의
  `bankMemo`/`depositorName` 검색과 같은 방식).

---

## 세금계산서 필터 옵션 조회 `GET /api/v1/finance/tax-invoices/filters`

**상태**: ✅ 확정
**인증 필요 여부**: Y

세금계산서 조회 화면의 `projectId` 필터 드롭다운을 채운다. 권한은 위 조회와 동일.

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.projects[].projectId` | Long | 프로젝트 ID |
| `data.projects[].projectName` | String | 프로젝트명 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "세금계산서 필터 옵션 조회 성공",
  "data": {
    "projects": [
      { "projectId": 1, "projectName": "한강 생태교육 환경개선사업" }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "세금계산서 필터 옵션 조회 성공" |
| 403 | Forbidden | `FINANCE_ACCESS_DENIED` | "접근 권한이 없습니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **"매칭된 프로젝트 목록"의 범위** — `tax_invoice`가 하나라도 연결된 정산 블록을 가진 프로젝트만 대상으로
  했다(입출금 내역 필터 옵션 조회와 동일한 사고방식).
- **정렬 순서** — 명세에 없어 `projectName` 오름차순으로 뒀다(입출금 내역 필터 옵션 조회와 동일).
- **403 `code` 값 — 원 명세엔 빈칸이었다.** `FINANCE_ACCESS_DENIED` 재사용.

---

## 세금계산서 CSV 컬럼 추천 조회 `POST /api/v1/finance/tax-invoices/csv/preview`

**상태**: ✅ 확정
**인증 필요 여부**: Y — **편집 권한** 보유자

CSV 파일 하나를 업로드하면 컬럼 목록·상위 5행 미리보기·추천 컬럼 매핑 + 추천 구분(INCOME/OUTCOME)을
내려준다. 파일은 서버에 저장하지 않는다(stateless) — 업로드 API를 호출할 때 같은 파일을 다시 첨부해야
한다.

**⭐ CSV 뿐 아니라 엑셀(.xlsx/.xls)·비밀번호로 보호된 엑셀도 받는다 (2026-08-12, 사용자 확정)** — 원
명세는 "CSV 파일"만 언급하지만, 컬럼 구성만 명세를 따르고 나머지 동작은 입출금 내역 CSV와 동일하게
맞추라는 방침에 따라 `cash_flow`의 `CashFlowUploadFileReader`/`CashFlowExcelParser`를 그대로 옮긴
`TaxInvoiceUploadFileReader`/`TaxInvoiceExcelParser`를 쓴다. 업로드 파일명 확장자로 CSV/엑셀을
구분해 같은 결과 구조로 통일하며, 엑셀에 여러 시트가 있어도 **첫 번째 시트만** 읽는다(시트 자동 판정은
지원하지 않음 — 필요하면 원하는 시트를 활성 시트로 두고 저장해서 올려야 한다).

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `file` | File | Y | 업로드한 CSV 또는 엑셀(.xlsx/.xls) 파일 |
| `password` | String | N | 파일이 비밀번호로 보호돼 있으면 그 비밀번호(엑셀만 해당) — 신규 필드 |

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.columns` | List\<String\> | CSV에 있는 전체 컬럼명 목록. 같은 이름이 여러 번 나오면(공급자/공급받는자 블록 각각의 "상호"/"대표자명"/"종사업장번호") 두 번째부터 " (2)", " (3)"... 접미사가 붙어 구분된다 |
| `data.sampleRows` | List\<Object\> | 상위 5행 미리보기 (컬럼명: 값) |
| `data.recommendedType` | String | 추천 구분(INCOME/OUTCOME). 판단 불가하면 null |
| `data.recommendedMapping.*` | String | 추천 컬럼 매핑 12종(원 명세와 동일) — `itemNameColumn`/`ceoNameColumn`/`subBizNoColumn`/`memoColumn`은 없으면 null |

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "세금계산서 CSV 컬럼 추천 조회 성공" |
| 400 | Bad Request | `FINANCE_CSV_PASSWORD_REQUIRED` | "비밀번호가 필요한 파일입니다." (신규) |
| 400 | Bad Request | `FINANCE_CSV_PASSWORD_INVALID` | "비밀번호가 올바르지 않습니다." (신규) |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." — 원 명세는 code 빈칸, 입출금 내역 CSV 미리보기와 동일 코드 재사용 |
| 404 | Not Found | `FINANCE_INVALID_CSV_FILE` | "유효하지 않은 형식입니다." — 원 명세는 code 빈칸, 입출금 내역과 동일 코드 재사용 |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **`recommendedType` 판단 기준 — 헤더 위 제목 줄의 "매출"/"매입" 키워드 (2026-08-13 정정)** — 처음엔
  공급자/공급받는자 사업자번호 컬럼 중 어느 쪽에 값이 채워져 있는지로 추천했는데(2026-08-12), **실제
  세금계산서는 둘 다 항상 채워져 있어서**(사업자번호는 법적으로 양쪽 다 필수 기재 항목) 그 기준으로는
  구분이 안 된다는 걸 사용자가 실제 홈택스 발급목록 샘플로 재확인했다. "우리 회사 사업자번호"를 저장하는
  곳이 코드베이스에 없어서(company 테이블에도 없음) 절대비교는 여전히 불가능 — 대신 홈택스 발급목록
  CSV/엑셀 상단에 "2022년도 매출세금계산서"처럼 종류가 적힌 제목 줄이 있는 경우가 많다는 점을 이용해,
  **헤더 판정용으로만 훑고 버려지던 그 제목 줄의 텍스트**(`TaxInvoiceCsvTable.titleText`)에 "매출"이
  있으면 `INCOME`, "매입"이 있으면 `OUTCOME`으로 추천한다. 제목 줄이 없거나 둘 다 없으면 null(사용자가
  라디오 버튼으로 직접 선택).
- **중복 헤더(공급자/공급받는자 "상호"·"대표자명"·"종사업장번호") 구분 + 방향에 맞는 컬럼 추천
  (2026-08-13, 실제 파일로 발견·사용자 확인)** — 세금계산서는 공급자·공급받는자 블록이 각각 상호/
  대표자명/종사업장번호를 갖고 있어서 헤더 이름이 겹친다. 이름이 같으면 행 파싱 시 `Map` 키가 겹쳐
  나중 값이 앞 값을 덮어써 버리는 문제가 있어(공급자 쪽 값이 조용히 사라짐), 파서가 두 번째 occurrence
  부터 `" (2)"`를 붙여 구분하도록 고쳤다. 이제 `buyerNameColumn`/`ceoNameColumn`/`subBizNoColumn` 추천은
  **`recommendedType`이 `OUTCOME`(매입)이면 공급자 쪽(접미사 없는 첫 occurrence), `INCOME`(매출)이거나
  판단 불가면 공급받는자 쪽(마지막 occurrence)**을 고른다 — "매입이면 외주업체(공급자) 정보가, 매출이면
  우리에게 돈을 주는 업체(공급받는자) 정보가 저장돼야 한다"는 사용자 확인에 따른 것. `supplierBizNoColumn`/
  `buyerBizNoColumn`은 이름이 겹치지 않아(각각 "공급자사업자등록번호"/"공급받는자사업자등록번호") 이 로직과
  무관하게 항상 그대로 매핑된다.
- **엑셀/비밀번호 지원 — 입출금 내역과 동일 (2026-08-12 정정)** — 처음엔 원 명세 문구("CSV 파일")를
  그대로 좁혀서 CSV 전용으로 만들었는데, "컬럼만 명세대로 하고 나머지는 입출금 양식에 맞추라"는 지시와
  맞지 않아 바로 잡았다. 여러 시트가 있는 엑셀은 첫 번째 시트만 읽는다(cash_flow와 동일 — 시트 자동
  판정 기능은 없음).
- **403/404 `code` — 원 명세엔 빈칸이었다.** 입출금 내역 CSV 미리보기와 같은 성격의 에러라 동일 코드
  (`FINANCE_EDIT_ACCESS_DENIED`/`FINANCE_INVALID_CSV_FILE`) 재사용.

---

## 세금계산서(CSV 기반) 업로드 `POST /api/v1/finance/tax-invoices/csv`

**상태**: ✅ 확정
**인증 필요 여부**: Y — **편집 권한** 보유자

미리보기에서 확정한 구분(`type`)·컬럼 매핑으로 CSV(또는 엑셀 .xlsx/.xls)를 파싱해 세금계산서로
저장한다. 엑셀·비밀번호 지원은 위 미리보기 API와 동일 — `TaxInvoiceUploadFileReader`가 파일명
확장자로 자동 판단한다.

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `file` | File | Y | CSV 또는 엑셀 파일 (프리뷰 때와 동일 파일 재전송) |
| `request` | JSON | Y | 구분 + 컬럼 매핑 정보 |
| `request.type` | String | Y | 구분 (INCOME: 매출/OUTCOME: 매입), 라디오 버튼으로 선택 |
| `request.approvalNoColumn` | String | Y | 승인번호 컬럼명 |
| `request.issuedDateColumn` | String | Y | 작성일자 컬럼명 |
| `request.supplierBizNoColumn` | String | Y | 공급자 사업자번호 컬럼명 |
| `request.buyerBizNoColumn` | String | Y | 공급받는자 사업자번호 컬럼명 |
| `request.buyerNameColumn` | String | Y | 공급받는자 상호 컬럼명 |
| `request.supplyAmountColumn` | String | Y | 공급가액 컬럼명 |
| `request.taxAmountColumn` | String | Y | 세액 컬럼명 |
| `request.totalAmountColumn` | String | Y | 합계금액 컬럼명 |
| `request.itemNameColumn` | String | N | 품목명 컬럼명 (없으면 null) |
| `request.ceoNameColumn` | String | N | 대표자명 컬럼명 (없으면 null) |
| `request.subBizNoColumn` | String | N | 종사업장번호 컬럼명 (없으면 null) |
| `request.memoColumn` | String | N | 비고/메모 컬럼명 (없으면 null) |
| `request.password` | String | N | 파일이 비밀번호로 보호돼 있으면 그 비밀번호(엑셀만 해당) — 신규 필드 |

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.totalRows` | Int | 전체 행 수 |
| `data.savedCount` | Int | 저장 성공 건수 |
| `data.duplicateCount` | Int | 중복 제외 건수 |
| `data.duplicateRows[].approvalNo` | String | 중복 승인번호 |
| `data.duplicateRows[].reason` | String | 제외 사유 |

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | — | "세금계산서(CSV 기반) 업로드 성공" |
| 400 | Bad Request | `FINANCE_CSV_MAPPING_REQUIRED` | "필수 컬럼 매핑이 누락되었습니다." — `type` 값이 INCOME/OUTCOME이 아니거나, 필수 컬럼이 CSV에 없는 경우도 포함 |
| 400 | Bad Request | `FINANCE_CSV_PASSWORD_REQUIRED` | "비밀번호가 필요한 파일입니다." (신규) |
| 400 | Bad Request | `FINANCE_CSV_PASSWORD_INVALID` | "비밀번호가 올바르지 않습니다." (신규) |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." — 원 명세는 code 빈칸, 입출금 내역과 동일 코드 재사용 |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **중복 판정 기준 — `approval_no` 단일 컬럼.** `cash_flow`처럼 (은행명·거래일시·금액) 복합키가 아니다 —
  `tax_invoice.approval_no`가 이미 `uk_tax_invoice_approval_no`로 **회사 스코프 없이 테이블 전체에서**
  유일해야 하는 값이라서(국세청 승인번호는 전국 유일값, 2026-08-09 확정), 중복 조회(`findExistingApprovalNos`)도
  `company_id`로 안 좁힌다.
- **동시 업로드 시 배치 충돌 재시도** — `cash_flow`의 `insertWithConcurrentDuplicateRetry`와 완전히 동일한
  구조. `insertAll`이 단일 배치 INSERT라 그중 한 행이라도 유니크 충돌하면 배치 전체가 실패하는데, 조회
  시점 이후 동시에 들어온 다른 요청이 같은 승인번호를 먼저 커밋한 경우가 이에 해당한다. 최신 상태로 한
  번만 다시 걸러서 재시도 — 그래도 또 걸리면(극히 드묾) 예외를 그대로 던진다.
- **`type`은 행마다가 아니라 요청 전체에 하나(라디오 버튼)** — CSV 자체에 구분 컬럼이 없다(원 명세에
  없음). 업로드 파일 전체가 매출 또는 매입 중 하나로 일괄 저장된다.
- **금액은 부호 그대로 저장** — `cash_flow`처럼 방향(입출금)을 절댓값+type으로 분리하는 개념이 없다.
  세금계산서 금액은 원본 값 그대로 저장한다(음수도 허용 — 수정세금계산서 등 실제로 음수가 나올 수 있음).
- **엑셀/비밀번호 지원 — 입출금 내역과 동일 (2026-08-12 정정)** — 위 미리보기 API와 같은 이유로 CSV
  전용에서 바로잡았다.
- **400 `code` — 원 명세엔 빈칸이었다.** `FINANCE_CSV_MAPPING_REQUIRED` 재사용(입출금 내역 CSV 업로드와
  동일한 성격의 에러).

---

## ⭐ 정산 블록 status 규칙 — 입출금·세금계산서 공통 (2026-08-13 확정·구현 완료)

**상태**: ✅ 확정

정산 블록 하나에는 **입출금 1건 + 세금계산서 1장**이 각각 따로 붙을 수 있다. 두 원장은 서로를 막지
않는다. `settlement_block.status`는 그 둘의 조합으로 결정된다.

| 입출금(`cash_flow`) | 세금계산서(`tax_invoice`) | status | 화면 표기 |
|---|---|---|---|
| ✗ | ✗ | `PENDING` | 미연결 |
| ✗ | ✓ | `WAITING` | 정산 대기 |
| ✓ | ✗ 또는 ✓ | `PARTIAL` / `COMPLETED` | 부분 정산 / 정산 완료 |

- **입출금이 붙으면 세금계산서 유무와 무관하게 `PARTIAL`/`COMPLETED`다.** 세금계산서가 먼저 붙어
  `WAITING`이던 블록에 입출금이 붙어도, 입출금이 먼저 붙은 블록에 세금계산서가 나중에 붙어도 결과는 같다.
- `PARTIAL`/`COMPLETED` 판정은 지금과 동일하게 `cash_flow.amount` vs `settlement_block.planned_amount`.
- **실적값(`actual_amount`/`actual_date`)은 입출금 전용이다** — 실제로 돈이 움직인 기록이라, 세금계산서
  매칭은 이 두 컬럼을 건드리지 않는다(사용자 확정: "실제로 정산이 된 건 입출금이니까"). 세금계산서는
  정산 현황 화면에서 `taxLinkedBy*`로만 보인다.
- **원장별 1건 제한은 유지** — 한 블록에 입출금 2건이나 세금계산서 2장은 안 된다. 부족분은 기존 규칙대로
  실무팀이 새 회차를 만들어 매칭한다.
- `WAITING`은 `settlement_block.status` ENUM에 **처음부터 있었지만 아무도 안 쓰던 값**이다
  (2026-08-09 스키마, 컬럼 주석 `미연결|정산 대기|부분 정산|정산 완료`). 이 규칙으로 처음 쓰이게 된다.

**구현 방식 (2026-08-13 반영 완료)**

핵심은 **매칭 가능 판정을 `status`에서 "그 원장이 이미 붙어 있나"(`settle_block_id` EXISTS)로 옮긴 것**이다.
`PARTIAL`은 "입출금이 붙었다"만 말해주고 세금계산서 유무는 알려주지 않으므로, status로는 판정할 수 없다.
이제 **status는 판정 기준이 아니라 결과값**이다.

| 지점 | 구현 |
|---|---|
| 세금계산서 후보 조회 | `tax_invoice` 미부착 블록 (status 무관 — 입금 끝난 블록도 후보) |
| 세금계산서 매칭 | 미부착이면 허용 → `PENDING`이면 `WAITING`으로, 이미 `PARTIAL`/`COMPLETED`면 그대로. **실적값 미변경** |
| 세금계산서 해제 | `status = 'WAITING'` 조건부 UPDATE로 `PENDING` 복귀 — 입출금이 남아 있으면 0행이라 상태·실적값 유지 |
| 입출금 후보 조회 | `cash_flow` 미부착 블록 (`WAITING` 블록 포함) |
| 입출금 매칭 | `status IN ('PENDING','WAITING')` 조건부 UPDATE → `PARTIAL`/`COMPLETED` + 실적값 채움 |
| 입출금 해제 | 세금계산서 있으면 `WAITING`, 없으면 `PENDING`(쿼리 안 `CASE WHEN EXISTS`) + 실적값 NULL |

**동시성 — 두 원장의 방어 방식이 다르다** ⚠️

| | 방어 수단 |
|---|---|
| 입출금 | **조건부 UPDATE로 충분.** 매칭되면 status가 반드시 `PENDING`/`WAITING` → `PARTIAL`/`COMPLETED`로 바뀌므로 그 UPDATE 자체가 경합 지점이 된다(먼저 온 쪽만 1행 성공) |
| 세금계산서 | **블록 행 잠금 필요.** 이미 `PARTIAL`인 블록에 붙을 때 status를 안 바꿔서 경합할 행 변경이 없다 → 서로 다른 세금계산서 2건이 동시에 들어오면 둘 다 통과해 "블록당 1장"이 깨진다 |

세금계산서 매칭은 `lockSettlementBlockForUpdate`(조인 없는 단일 테이블 `SELECT ... FOR UPDATE`)로 블록
행만 잠근 뒤 `findLinkedTaxInvoiceId`로 **다시** 확인한다.

- ⚠️ **조인 쿼리에 `FOR UPDATE`를 붙이지 않는다** — MySQL은 조인이 훑은 모든 테이블의 행을 잠그므로
  `block`·`step`·`project` 행까지 잠기고, 매칭과 무관한 스텝 수정·블록 생성이 대기한다.
- ⚠️ **잠금 후 재확인 쿼리도 `FOR UPDATE`여야 한다** — REPEATABLE READ에서 일반 SELECT는 트랜잭션 최초
  조회 시점의 스냅샷을 보므로, 줄 서서 기다린 뒤에도 옛 값을 볼 수 있다. 정산 도메인이 `SETL-008`에서
  실제로 겪은 문제다.

**카운트 반영 (2026-08-13 확정·구현 완료)**

`WAITING`이 실제로 쓰이기 시작하면서 "미연결" 집계에서 조용히 빠지는 문제가 있었다. 두 원장의 미연결은
**서로 다른 개념**이라 아래처럼 갈라서 센다.

| 집계 | 기준 | 위치 |
|---|---|---|
| 입출금 미연결 | `status IN ('PENDING','WAITING')` | 재무 요약 `settlement.unlinkedCount` / 정산 현황 `settlementStatusSummary`의 "미연결 N건" |
| 세금계산서 미연결 | `tax_invoice` **미부착 블록** (status 무관) | 정산 현황 프로젝트 조회 **신규 필드** `taxInvoiceUnlinkedCount` |

- ⚠️ **`WAITING`을 "세금계산서 미연결"에 넣으면 안 된다** — `WAITING`은 세금계산서가 **이미 붙은** 상태다
  (붙지 않은 건 입출금). 넣으면 세금계산서를 받아둔 회차가 미수취로 잡힌다.
- 세금계산서 미연결은 `status`로 판정 불가 — `PARTIAL`/`COMPLETED`는 "입출금이 붙었다"만 말해준다.
- 요약 문구(`settlementStatusSummary`)는 **문자열 계약이라 형태를 안 바꿨다**. 세금계산서 정보는 문구에
  섞지 않고 숫자 필드로 따로 내려서, 프론트가 원하는 배지를 직접 조립하게 했다(사용자 확정).
- 정산 현황 **블록** 조회는 그대로다 — `taxLinkedBy`/`cashFlowLinkedBy`의 null 여부로 이미 회차별 판정이
  가능해서 파생 필드를 추가하지 않았다.
- ⚠️ 정산 현황 블록 조회 응답의 `status` 값으로 **`WAITING`이 프론트에 새로 나가기 시작한다.**

---

## 세금계산서 매칭 추천 조회 `GET /api/v1/finance/tax-invoices/{taxId}/match-candidates`

**상태**: ✅ 확정
**인증 필요 여부**: Y — **편집 권한** 보유자

세금계산서 하나를 골랐을 때, 금액·세액·상호명·발행일이 비슷한 정산 블록을 추천한다. 권한은 입출금 내역
매칭 추천 조회와 동일한 편집 권한(`PagePermissionPort.hasEditAccess`, `FINANCE` 페이지).

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `taxId` | Long | Y | 매칭할 세금계산서 ID |

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.candidates[].settleId` | Long | 정산 블록 ID |
| `data.candidates[].roundName` | String | 정산 블록명(회차명) |
| `data.candidates[].projectName` | String | 프로젝트명 |
| `data.candidates[].plannedAmount` | BigDecimal | 예정 금액 |
| `data.candidates[].plannedTaxAmount` | BigDecimal | 예정 세금 금액 (입출금 매칭 추천에는 없는 필드 — 아래 메모 참고) |
| `data.candidates[].plannedDate` | LocalDate | 예정일 |
| `data.candidates[].traderName` | String | 거래처명 |
| `data.candidates[].matchTags` | List\<String\> | 추천 이유 태그 (예: "금액 일치", "세액 일치", "상호명 유사", "일자 유사") |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "매칭 추천 조회 성공",
  "data": {
    "candidates": [
      {
        "settleId": 11,
        "roundName": "2차 기성(중도금)",
        "projectName": "한강 생태교육 환경개선사업",
        "plannedAmount": 90000000,
        "plannedTaxAmount": 9000000,
        "plannedDate": "2026-09-10",
        "traderName": "환경부",
        "matchTags": ["금액 일치", "세액 일치", "상호명 일치"]
      }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "매칭 추천 조회 성공" |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." |
| 404 | Not Found | `FINANCE_TAX_INVOICE_NOT_FOUND` | "존재하지 않는 세금계산서입니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- ⚠️ **원 명세(노션)가 없는 API다 (2026-08-13)** — 세금계산서 쪽 매칭 3종은 명세가 따로 없어서,
  **이미 확정·구현된 입출금 내역 매칭 3종을 세금계산서 컬럼으로 그대로 옮겼다**(사용자 확정). 경로·응답
  구조·에러 처리 방식이 전부 입출금 쪽과 1:1 대응이다. 노션에 세금계산서 매칭 명세가 따로 올라오면
  이 문서와 대조해서 차이를 먼저 확인할 것.
- **매칭 기준 4종 (입출금은 3종)** — 세금계산서는 세액(`tax_amount`)이 따로 있어서 기준이 하나 늘었다.
  - 대상 정산 블록: **세금계산서가 아직 안 붙은 블록** + **같은 타입(INCOME/OUTCOME)만**.
    ⚠️ status는 안 본다 — 입출금이 이미 매칭된 `PARTIAL`/`COMPLETED` 블록도 후보에 들어온다(입금은
    끝났고 세금계산서만 기다리는 정상 대상이다). 위 "⭐ 정산 블록 status 규칙" 참고
  - 금액: `tax_invoice.total_amount` ↔ `settlement_block.planned_amount`, 완전 동일 = `EXACT`("금액 일치"),
    ±5% 이내 = `SIMILAR`("금액 유사")
  - 세액: `tax_invoice.tax_amount` ↔ `settlement_block.planned_tax_amount`, 판정 규칙은 금액과 동일
    ("세액 일치"/"세액 유사")
  - 상호명: `tax_invoice.buyer_name` ↔ `settlement_block.trader_name`, 완전 동일 = `EXACT`, 한쪽이 다른
    쪽을 포함하면(양방향) `SIMILAR` — 입출금의 `depositorName` 자리에 `buyerName`이 들어간 것
  - 일자: `tax_invoice.issued_no`(발행일) ↔ `settlement_block.planned_date`, 같은 날 = `EXACT`, ±7일 이내
    = `SIMILAR`. ⚠️ 발행일은 입출금의 `traded_at`("돈이 실제로 움직인 날")과 달리 **세금계산서가 발행된
    날**이라 신뢰도가 낮지만, 결제 예정일 근처에 발행되는 경우가 많아 약한 신호로는 유지한다
  - 네 기준 중 **하나도 안 걸리면 후보에서 제외** / 정렬은 걸린 기준 개수 많은 순 → 발행일 가까운 순 /
    **최대 5건**(페이지네이션 없음) — 전부 입출금과 동일
- **`taxId`가 다른 회사 소속이거나 삭제됐으면 404** — `company_id`·`deleted_at IS NULL`까지 걸어서 조회.
  후보 조회도 `project.company_id`로 회사 스코프를 건다(입출금 쪽 크로스테넌트 유출 수정과 동일).
- **404 코드는 신규 `FINANCE_TAX_INVOICE_NOT_FOUND`** — 입출금의 `FINANCE_CASH_FLOW_NOT_FOUND`는 메시지가
  "존재하지 않는 입출금 내역입니다."라 재사용할 수 없어서 세금계산서용으로 새로 만들었다.

---

## 세금계산서 블록 매칭 `PATCH /api/v1/finance/tax-invoices/{taxId}/match`

**상태**: ✅ 확정
**인증 필요 여부**: Y — **편집 권한** 보유자

세금계산서 하나를 정산 블록 하나에 연결한다. 매칭 추천 조회가 준 후보 중 하나를 고르거나, 프론트가 임의의
`settleId`를 보내도 동일하게 동작한다.

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `taxId` | Long | Y | 매칭할 세금계산서 ID |

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `settleId` | Long | Y | 연결할 정산 블록 ID |

**Request Example**

```json
{
  "settleId": 11
}
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.taxId` | Long | 세금계산서 ID |
| `data.settleId` | Long | 연결된 정산 블록 ID |
| `data.roundName` | String | 연결된 정산 블록명 |
| `data.projectName` | String | 연결된 프로젝트명 |
| `data.linkedBy` | String | 매칭 처리자 사번 |
| `data.linkedByName` | String | 매칭 처리자 이름 |
| `data.linkedAt` | LocalDateTime | 매칭 일시 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "세금계산서 블록 매칭 성공",
  "data": {
    "taxId": 3,
    "settleId": 11,
    "roundName": "2차 기성(중도금)",
    "projectName": "한강 생태교육 환경개선사업",
    "linkedBy": "vitas-EMP004",
    "linkedByName": "김재무",
    "linkedAt": "2026-08-13T15:30:00"
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "세금계산서 블록 매칭 성공" |
| 400 | Bad Request | `FINANCE_TAX_INVOICE_ALREADY_MATCHED` | "이미 매칭된 항목입니다." (이 세금계산서가 이미 다른 정산 블록에 연결돼 있음) |
| 400 | Bad Request | `FINANCE_TAX_TYPE_MISMATCH` | "세금계산서 구분과 정산 블록 타입이 일치하지 않습니다." |
| 400 | Bad Request | `FINANCE_SETTLEMENT_BLOCK_ALREADY_MATCHED` | "이미 매칭된 정산 블록입니다." (입출금 매칭과 **동일 코드 재사용** — 메시지가 도메인 중립이라 그대로 씀) |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." |
| 404 | Not Found | `FINANCE_TAX_MATCH_TARGET_NOT_FOUND` | "존재하지 않는 세금계산서 또는 정산 블록입니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **에러코드 3개는 신규, 1개는 재사용** — `FINANCE_MATCH_TYPE_MISMATCH`/`FINANCE_MATCH_TARGET_NOT_FOUND`는
  메시지가 "입출금 구분…"/"존재하지 않는 입출금 내역 또는…"으로 도메인 특정 문구라 재사용하지 않고
  `FINANCE_TAX_TYPE_MISMATCH`/`FINANCE_TAX_MATCH_TARGET_NOT_FOUND`를 새로 만들었다. 반대로
  `FINANCE_SETTLEMENT_BLOCK_ALREADY_MATCHED`는 메시지가 "이미 매칭된 정산 블록입니다."로 중립이라 재사용.
- **매칭 가능 조건은 `status`가 아니라 "세금계산서 부착 여부"다** — 입출금이 이미 매칭된
  `PARTIAL`/`COMPLETED` 블록에도 세금계산서를 붙일 수 있다. `FINANCE_SETTLEMENT_BLOCK_ALREADY_MATCHED`
  (400)는 **그 블록에 세금계산서가 이미 1장 있을 때만** 나간다(입출금이 붙어 있는 것과는 무관).
  자세한 규칙과 구현 방식은 위 "⭐ 정산 블록 status 규칙" 절 참고.
- **매칭 시 status만 올리고 실적값은 안 건드린다** — `PENDING`이면 `WAITING`(정산 대기)으로,
  이미 `PARTIAL`/`COMPLETED`면 그대로 둔다. `actual_amount`/`actual_date`는 입출금 소관이라 손대지
  않는다(세금계산서 금액을 실적으로 넣으면 `paidAmountRatio`가 실입금 진행률이 아니게 된다).
- **동시성 방어는 입출금과 방식이 다르다** — `tax_invoice`는 `settle_block_id IS NULL` 조건부 UPDATE로
  "이 세금계산서가 이미 매칭됐나"를 막고, **블록당 1장** 규칙은 블록 행 잠금(`FOR UPDATE`) + 잠금 후
  재확인으로 지킨다. 입출금처럼 상태 조건부 UPDATE에 기댈 수 없는 이유는 위 절에 정리해뒀다.
- **빈 블록(`type IS NULL`) 처리도 동일** — `Objects.equals`로 비교해 null이면 타입 불일치(400)로 막는다.

---

## 세금계산서 블록 매칭 해제 `PATCH /api/v1/finance/tax-invoices/{taxId}/unmatch`

**상태**: ✅ 확정
**인증 필요 여부**: Y — **편집 권한** 보유자

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `taxId` | Long | Y | 매칭 해제할 세금계산서 ID |

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | null | 항상 null |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "세금계산서 블록 매칭 해제 성공",
  "data": null
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "세금계산서 블록 매칭 해제 성공" |
| 400 | Bad Request | `FINANCE_TAX_INVOICE_NOT_MATCHED` | "매칭되지 않은 항목입니다." |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." |
| 404 | Not Found | `FINANCE_TAX_INVOICE_NOT_FOUND` | "존재하지 않는 세금계산서입니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **입출금이 남아 있으면 블록 상태를 그대로 둔다** — `status = 'WAITING'` 조건부 UPDATE로 `PENDING`으로
  되돌리므로, 입출금이 붙어 있는 블록(`PARTIAL`/`COMPLETED`)은 0행이 되어 상태·실적값이 그대로 유지된다.
  세금계산서만 붙어 있던 블록(`WAITING`)일 때만 `PENDING`(미연결)으로 돌아간다.
- **실적값은 건드리지 않는다** — 입출금 매칭 해제와 정반대다(그쪽은 항상 `NULL`로 비운다).

---

## 세금계산서 메모 수정 `PATCH /api/v1/finance/tax-invoices/{taxId}`

**상태**: ✅ 확정
**인증 필요 여부**: Y — **편집 권한** 보유자

세금계산서의 비고/메모를 수정한다. **세금계산서는 수동 등록이 없어(전부 CSV/엑셀 업로드) 수정 대상이
메모뿐이다** — 승인번호·금액·사업자번호는 국세청 발급 원본 값이라 고치지 않는다.

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `taxId` | Long | Y | 수정할 세금계산서 ID |

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `memo` | String | Y | 비고/메모. `null`·빈 문자열이면 메모를 지운다 |

**Request Example**

```json
{
  "memo": "재입고 관련 확인 필요"
}
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.taxId` | Long | 세금계산서 ID |
| `data.memo` | String | 수정된 메모 |
| `data.updatedAt` | LocalDateTime | 수정일시 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "세금계산서 메모 수정 성공",
  "data": {
    "taxId": 1,
    "memo": "재입고 관련 확인 필요",
    "updatedAt": "2026-08-07T16:30:00"
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "세금계산서 메모 수정 성공" |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." |
| 404 | Not Found | `FINANCE_TAX_INVOICE_NOT_FOUND` | "존재하지 않는 세금계산서입니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **HTTP 상태 코드 200 (원 명세 Status Code 표는 201)** — Success Example JSON이 200이고, 입출금 내역
  수정도 200이라 그쪽에 맞췄다(다른 재무 API와 동일한 판단).
- **403·404 code 신규 부여** — 원 명세에 code가 빈칸이었다. 기존 `FINANCE_EDIT_ACCESS_DENIED`·
  `FINANCE_TAX_INVOICE_NOT_FOUND` 재사용(원 명세 문구 오타 "존재하지 않는 t세금계산서입니다."도 정정).
- **`memo` 외 필드를 보낼 경로 자체를 없앴다** — 입출금 수정 API는 전체 필드를 받고 `sourceType != MANUAL`
  이면 400(`FINANCE_CASH_FLOW_FIELD_EDIT_NOT_ALLOWED`)으로 거부하는데, 세금계산서는 **모든 건이 CSV 유입**
  이라 그 분기가 항상 같은 결과다. 요청 바디에 `memo` 하나만 둬서 400 케이스 자체를 만들지 않았다.
- **매칭 여부는 보지 않는다** — 입출금도 매칭된 항목의 메모는 항상 수정 가능하다(같은 규칙).
- `updated_at`을 `NOW()`로 같이 갱신한다 — 응답의 `updatedAt`이 이 값이다.

---

## 세금계산서 삭제(배치) `DELETE /api/v1/finance/tax-invoices`

**상태**: ✅ 확정
**인증 필요 여부**: Y — **편집 권한** 보유자

세금계산서 여러 건을 한 번에 삭제한다(소프트 삭제 — `deleted_at`만 채운다). 정산 블록에 매칭된 항목은
**먼저 매칭을 해제해야** 삭제할 수 있다.

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `taxIds` | List\<Long\> | Y | 삭제할 세금계산서 ID 목록 |

**Request Example**

```json
{
  "taxIds": [1, 2, 3]
}
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.deletedCount` | Int | 실제 삭제된 건수 |
| `data.skippedItems[].taxId` | Long | 삭제되지 못한 세금계산서 ID |
| `data.skippedItems[].reason` | String | 삭제되지 못한 사유 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "세금계산서 삭제 성공",
  "data": {
    "deletedCount": 2,
    "skippedItems": [
      {
        "taxId": 3,
        "reason": "매칭된 항목은 삭제할 수 없습니다. 먼저 매칭을 해제해주세요."
      }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "세금계산서 삭제 성공" |
| 400 | Bad Request | `FINANCE_TAX_INVOICE_REQUIRED_FIELD_MISSING` | "삭제할 항목을 선택해주세요." (`taxIds` 비어있음) |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **단건(`{taxId}`) → 배치로 바꿨다 (사용자 확정)** — 원 명세는 Path Parameter 단건이었지만 입출금 내역
  삭제가 배치(`DELETE /cash-flows` + `cashFlowIds`)라 형식을 통일했다. 목록에서 여러 건을 골라 지우는
  화면이라 배치가 맞다.
- **HTTP 상태 코드 200 (원 명세 Status Code 표는 201)** — 위 메모 수정과 동일 사유.
- **매칭된 항목·없는 ID는 400/404가 아니라 `skippedItems`** — 배치에서 한 건 때문에 전체를 실패시키면
  화면을 쓸 수 없다. 처리 가능한 것만 지우고 나머지는 사유와 함께 돌려준다(입출금과 동일).
  그래서 이 API는 **매칭된 항목 때문에 400이 나가지 않고, 404도 나가지 않는다.**
- **에러코드 신규 `FINANCE_TAX_INVOICE_LINKED_CANNOT_DELETE`** — `skippedItems`의 `reason` 문구로 쓰인다.
  메시지는 입출금의 `FINANCE_CASH_FLOW_LINKED_CANNOT_DELETE`와 같지만, **code 문자열 자체가 API 계약**
  이라(프론트가 이 값으로 분기한다) 세금계산서 전용으로 만들었다. 매칭 3종에서 세운 기조와 동일하다.
- **중복 ID는 제거하고 센다** — 같은 ID를 두 번 보내면 IN절은 한 행만 지우는데 카운트는 두 번 세는 문제.
- ⚠️ **`deletedCount`는 실제로 지운 행 수다** — 삭제 가능하다고 판정한 개수가 아니다. 조회~삭제 사이에
  동시 매칭돼 조건부 UPDATE(`settle_block_id IS NULL`)가 걸러낸 행까지 "삭제 완료"로 보고하면 안 된다
  (입출금 쪽 CodeRabbit 지적으로 확립된 규칙).

---

## 세금계산서 연결 제외/포함 처리(배치) `PATCH /api/v1/finance/tax-invoices/exclude`

**상태**: ✅ 확정
**인증 필요 여부**: Y — **편집 권한** 보유자

프로젝트와 무관한 세금계산서를 미연결 건수 집계에서 빼거나(제외), 다시 포함시킨다.

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `taxIds` | List\<Long\> | Y | 연결 제외 처리할 세금계산서 ID 목록 |
| `isExcluded` | Boolean | Y | 제외 여부 (true: 제외, false: 제외 취소) |

**Request Example**

```json
{
  "taxIds": [30, 31, 32],
  "isExcluded": true
}
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.updatedCount` | Int | 처리된 건수 |
| `data.skippedItems[].taxId` | Long | 처리되지 못한 세금계산서 ID (원 명세엔 없던 필드 — 아래 메모 참고) |
| `data.skippedItems[].reason` | String | 처리되지 못한 사유 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "세금계산서 연결 제외 처리 성공",
  "data": {
    "updatedCount": 2,
    "skippedItems": [
      {
        "taxId": 31,
        "reason": "이미 매칭된 항목은 제외 처리할 수 없습니다."
      }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "세금계산서 연결 제외 처리 성공" |
| 400 | Bad Request | `FINANCE_TAX_INVOICE_REQUIRED_FIELD_MISSING` | "필수 항목이 누락되었습니다." (`taxIds` 비어있음 또는 `isExcluded` 누락) |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **`skippedItems` 추가 (원 명세엔 `updatedCount`만)** — 원 명세는 "이미 매칭된 항목은 제외 처리할 수
  없습니다"를 400으로 규정했는데, **배치 API에서 한 건 때문에 전체를 실패시키면 목록에서 여러 건을 골라
  처리하는 화면을 쓸 수 없다.** 입출금 연결 제외 처리와 동일하게 처리 못 한 항목만 사유와 함께 돌려주고
  나머지는 처리한다. 그래서 이 API는 **매칭된 항목 때문에 400이 나가지 않는다.**
- **제외 취소(`isExcluded=false`)는 매칭 여부와 무관하게 항상 허용** — "이미 매칭됨"은 제외(true)할 때만
  막는다(입출금과 동일 규칙). 그래서 취소 요청의 `skippedItems`는 존재하지 않는 ID만 담긴다.
- **존재하지 않는 ID도 `skippedItems`로 돌려준다** — 사유는 "존재하지 않는 세금계산서입니다."
- **중복 ID는 제거하고 센다** — 같은 ID를 두 번 보내면 `updatedCount`가 부풀려지는 문제(입출금 쪽
  CodeRabbit 지적)를 여기서도 처음부터 막았다.
- **400 code 신규 `FINANCE_TAX_INVOICE_REQUIRED_FIELD_MISSING`** — 원 명세 code 빈칸. 삭제 API와 같은
  이유로 세금계산서 전용 코드를 만들었다.
- **URL이 `{taxId}` 패턴과 겹치는 것처럼 보이지만 문제없다** — `PATCH /tax-invoices/exclude`와
  `PATCH /tax-invoices/{taxId}`는 Spring이 리터럴 경로를 우선 매칭한다(입출금 `/cash-flows/exclude`와
  동일한 구조로 이미 검증됨).

---

## 입출금 내역 CSV 컬럼 추천 조회 `POST /api/v1/finance/cash-flows/csv/preview`

**상태**: ✅ 확정
**인증 필요 여부**: Y — **편집 권한** 보유자(위 조회 3개는 접근 권한만 있으면 됐지만, 이건 쓰기 전 단계라 등급이 다르다)

CSV 파일 하나를 업로드하면 컬럼 목록·상위 5행 미리보기·추천 컬럼 매핑을 내려준다. 파일은 서버에 저장하지
않는다(stateless) — 사용자가 매핑을 확정해 업로드 API를 호출할 때 **같은 파일을 다시 첨부**해야 한다
(아래 업로드 API의 Request Example 참고).

**⭐ CSV 뿐 아니라 엑셀(.xlsx/.xls)도 받는다 (2026-08-10 확장, 사용자 확정)** — 은행에 따라 CSV 대신 엑셀로
내보내는 경우가 있어서, 업로드 파일명 확장자로 CSV/엑셀을 구분해 같은 결과 구조로 통일한다. 엔드포인트
URL·에러 메시지는 원 명세대로 "CSV"라는 이름을 그대로 쓴다(프론트 계약이 이미 이 이름으로 고정돼 있어서) —
실제로는 엑셀 파일을 넣어도 정상 동작한다.

**⭐ 비밀번호로 보호된 엑셀도 받는다 (2026-08-10 확장, 사용자 확정, 원 명세에 없던 필드)** — 국내 은행
엑셀 내보내기가 개인정보 보호 차원에서 비밀번호로 잠겨있는 경우가 흔하다(실제 카카오뱅크 파일로 확인).
`password`를 새 파트로 추가했다 — CSV는 애초에 암호화 불가능한 포맷이라 이 값을 보내도 무시된다.

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `file` | File | Y | 업로드한 CSV 또는 엑셀(.xlsx/.xls) 파일 |
| `password` | String | N | 파일이 비밀번호로 보호돼 있으면 그 비밀번호(엑셀만 해당) — 신규 필드 |

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.columns` | List\<String\> | CSV에 있는 전체 컬럼명 목록 |
| `data.bankOptions` | List\<String\> | 은행명 선택 드롭다운에 넣을 은행 목록 |
| `data.sampleRows` | List\<Object\> | 상위 5행 미리보기 (컬럼명: 값) |
| `data.recommendedDateTimeMode` | String | 추천 일시 입력 방식 (SINGLE/SEPARATE) |
| `data.recommendedAmountMode` | String | 추천 금액 입력 방식 (SINGLE_WITH_TYPE/SEPARATE) |
| `data.recommendedMapping.*` | String | 추천 컬럼 매핑 9종(원 명세와 동일) + `balanceColumn`(신규, 없으면 null) |

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "CSV 컬럼 추천 조회 성공" |
| 400 | Bad Request | `FINANCE_CSV_PASSWORD_REQUIRED` | "비밀번호가 필요한 파일입니다." (신규) |
| 400 | Bad Request | `FINANCE_CSV_PASSWORD_INVALID` | "비밀번호가 올바르지 않습니다." (신규) |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." |
| 404 | Not Found | `FINANCE_INVALID_CSV_FILE` | "유효하지 않은 형식입니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **403/404 code — 원 명세에 값이 비어 있었다.** `FINANCE_EDIT_ACCESS_DENIED`/`FINANCE_INVALID_CSV_FILE`
  신규 부여. 404가 "파일 검증 실패"에 쓰이는 게 이례적이지만(보통 400 영역) 명세에 그대로 있어 문자 그대로
  반영했다 — 나중에 검토 필요하면 알려달라.
- **비밀번호 필요/틀림을 별도 코드 2개로 나눴다 (2026-08-10, 원 명세에 없던 흐름)** — 프론트가 "비밀번호
  입력 모달을 새로 띄울지"(`FINANCE_CSV_PASSWORD_REQUIRED`, 비밀번호를 아예 안 보냈는데 파일이 잠겨있음)
  "같은 모달에서 재입력시킬지"(`FINANCE_CSV_PASSWORD_INVALID`, 보낸 비밀번호가 틀림)를 구분해야 해서
  나눴다. 판정은 호출자가 `password`를 보냈는지 여부로 하지, POI 예외 메시지 문자열을 파싱하지 않는다
  (라이브러리 내부 문구는 버전마다 바뀔 수 있어 신뢰할 수 없음). 서버는 비밀번호 값 자체를 절대 로그에
  남기지 않는다.
- **404 메시지 문구 — 원 명세는 "유효하지 않은 CSV 파일입니다."였으나 "유효하지 않은 형식입니다."로
  정정했다 (2026-08-10, 사용자 확정)** — 위에서 CSV뿐 아니라 엑셀도 받도록 넓혔는데 메시지에 "CSV"가
  박혀 있으면 엑셀 파일이 잘못됐을 때도 "CSV 파일이 아니다"라고 오도한다. 코드값(`FINANCE_INVALID_CSV_FILE`)
  자체는 그대로 뒀다 — URL·클래스명 등 나머지가 전부 "csv" 계열 이름이라 코드까지 바꾸면 일관성이
  더 깨진다고 판단, 사람이 보는 메시지만 정정했다.
- **"편집 권한" 판정 (사용자 확정)** — 지금까지 나머지 finance GET들은 `page_permission`에 행이 있으면
  등급(VIEWER/EDITOR) 무관하게 통과시키는 `hasAccess`를 썼는데, 이 2개(미리보기·업로드)는 **등급이
  `EDITOR`인 행만** 통과하는 `hasEditAccess`를 별도로 만들었다. `ADMIN`·`MASTER`는 여전히 GLOBAL_ROLE로
  무조건 통과(page-permission.md 규칙 그대로).
- **CSV 파싱 라이브러리 신규 도입 (`commons-csv:1.12.0`)** — 코드베이스에 CSV 파서가 없었다. 직접 split
  파싱은 따옴표로 감싼 필드 안의 콤마·개행을 못 다뤄 은행마다 다른 CSV 포맷에 취약하다고 판단해 검증된
  라이브러리를 추가했다(사용자 확인).
- **인코딩 — BOM 있으면 UTF-8, 없으면 EUC-KR로 가정한다.** 국내 은행/엑셀 CSV 내보내기가 EUC-KR인 경우가
  흔해서 넣은 추정 로직이다 — 완벽한 감지가 아니라 실제 은행 CSV 샘플로 검증 전까지는 최선 추정치다.
- **엑셀(.xlsx/.xls) 지원 추가 (2026-08-10, 사용자 확정)** — 업로드 파일명 확장자가 `xlsx`/`xls`면
  `CashFlowExcelParser`(Apache POI, 이미 `employee` 사원 일괄등록에 쓰던 라이브러리라 신규 의존성 추가는
  없었다)로, 그 외(`csv` 포함 확장자 불명)는 기존 `CashFlowCsvParser`로 읽는다 — `CashFlowUploadFileReader`가
  파일명 확장자만 보고 갈라준다. 둘 다 같은 `CashFlowCsvTable`(헤더+행)을 만들어내서 추천·행변환·저장
  로직은 원본이 CSV였는지 엑셀이었는지 몰라도 된다. 날짜 서식 셀은 시간이 자정이면 날짜만, 아니면
  날짜+시간으로 정규화해 `CashFlowCsvRowParser`가 시도하는 포맷 목록과 맞춘다.
- **⚠️ 컬럼 추천 로직(키워드 사전)은 명세에 규칙이 없어 직접 설계했다.** `CashFlowCsvColumnRecommender`가
  헤더명에 "일시"/"일자"·"날짜"/"시간"·"시각"/"입금액"·"입금"/"출금액"·"출금"/"금액"/"구분"/"적요"·"메모"·
  "내용"/"입금자"·"송금인"·"거래처"·"이름"·"내용" 키워드를 매칭한다. 추천이 틀려도 사용자가 화면에서
  드롭다운으로 고쳐 확정하므로(업로드 API의 `request`) 최종 저장 값에는 영향 없다 — 실제 은행 CSV
  샘플이 들어오면 이 사전을 검증·보강해야 한다.
- **`depositorColumn` 추천에 "내용"도 포함 (2026-08-10, 실제 파일로 확인·사용자 확정)** — 은행 CSV에
  거래처(예금주) 전용 컬럼이 따로 없고 "내용" 컬럼에 그 정보가 실려오는 경우가 흔했다. `DEPOSITOR_KEYWORDS`
  맨 뒤에 "내용"을 추가했다 — "거래처"/"입금자" 등 더 명확한 컬럼이 있으면 그게 우선이고, 없을 때만
  대체 후보로 추천된다.
- **`memoColumn` 추천에서 "내용"을 빼고 "비고"를 넣었다 (2026-08-13 정정, 사용자 확정)** — `MEMO_KEYWORDS`가
  `["적요","메모","내용"]`이라 **"적요"가 없고 "내용"만 있는 파일(카카오뱅크 등)에서 `memoColumn`과
  `depositorColumn`이 같은 컬럼으로 추천됐다.** 그대로 업로드하면 `bank_memo`와 `depositor_name`에 똑같은
  문자열이 중복 저장된다(실제 테스트 데이터가 그렇게 들어갔다). `depositorName`은 필수(`NOT NULL`)고 메모는
  선택이라, 후보가 하나뿐이면 **필수인 쪽에 주고 메모는 `null`로 비워둔다.** 지금 목록은
  `["적요","메모","비고"]`다 — "비고"는 실제 은행 파일에 쓰이는데 빠져 있어 같이 추가했다(세금계산서 쪽
  `MEMO_KEYWORDS`엔 이미 있었다 — 도메인마다 목록이 따로라 갈려 있었다).
  ⚠️ 추천값만 바뀐 것이고 **API 제약은 그대로다** — 사용자가 화면에서 `memoColumn`을 `depositorColumn`과
  같은 컬럼으로 직접 지정하는 것은 여전히 가능하다.
- **`typeColumn` 추천 — 정확히 "구분"인 컬럼을 최우선으로 본다 (2026-08-10, 실제 파일로 발견)** — 카카오뱅크
  파일에 "구분"(입금/출금이 그대로 적힘)과 "거래구분"(계좌간자동이체·일반입금 등 거래 방식, 방향과
  무관)이 같이 있는데, 부분 일치 우선순위(`TYPE_KEYWORDS` = 입출금구분·거래구분·구분 순)로는 "거래구분"이
  먼저 걸려서 잘못 추천됐다("계좌간자동이체"는 "입"/"출"이 없어 실제 업로드 시 분류 실패로 이어짐).
  헤더 중 이름이 정확히 "구분"인 컬럼이 있으면 그걸 최우선으로 쓰고, 없을 때만 기존 부분 일치 목록으로
  대체하도록 고쳤다(`CashFlowCsvColumnRecommender.findTypeColumn`).
- **날짜만 있고 시간이 없는 컬럼도 SINGLE 모드로 추천될 수 있다** — Success Example의 "날짜"(값이
  `2026-07-15`, 시간 없음)가 `tradedDateTimeColumn`으로 추천된 것과 동일하게, 시간이 없으면 자정(00:00)
  으로 채운다(업로드 시 실제 파싱 로직도 동일).
- **`sampleRows`의 빈 셀은 빈 문자열로 내려준다** — 내부적으로는 매핑 필수값 체크를 위해 빈 셀을 `null`로
  정규화해 쓰지만, 미리보기 화면에는 원본 CSV처럼 빈 문자열로 보여준다(명세 Success Example의
  `"출금금액": ""` 그대로).
- **⚠️ 헤더 행이 1번째 줄이라고 가정하지 않는다 (2026-08-10, 실제 은행 CSV로 발견)** — 처음엔 CSV의
  1번째 줄을 무조건 헤더로 읽었는데, 실제 은행이 내려주는 CSV 중 "계좌번호"/"조회기간"/"총건수" 같은
  안내 줄이 진짜 표 앞에 붙는 형식이 있어서, 그 안내 줄의 첫 칸("거래내역조회")이 헤더로 잘못 잡히고
  `columns`가 통째로 깨지는 문제가 있었다(사용자가 실제 파일로 재현). **전체 행의 "칸 개수(폭)" 최댓값을
  진짜 표의 폭으로 보고, 그 폭과 처음 일치하는 행을 헤더로 판정**하도록 고쳤다 — 헤더는 모든 칸에
  이름이 채워져 있어 항상 표에서 가장 넓은 줄이라는 성질을 이용한 것이다. 안내 줄은 보통 폭이 좁아서
  (1~2칸) 자연히 제외되고, 안내 줄이 아예 없는 파일(헤더가 1번째 줄)도 그대로 맞다.
  CSV(`CashFlowCsvParser`)·엑셀(`CashFlowExcelParser`) 둘 다 동일하게 적용했다.
  ⚠️ **최빈값 → 최댓값 → (엑셀만) "실제 값 채워진 칸 개수" 순으로 같은 날 두 번 더 정정했다** — 실제
  파일로 재현하며 드러난 순서 그대로 적는다:
  1. 최빈값(가장 흔한 폭)으로 짰더니, 카카오뱅크 CSV처럼 "메모" 같은 특정 컬럼이 데이터 행마다 자주
     비어있으면 그만큼 짧아진 데이터 행 폭이 오히려 더 흔해져서, 헤더(모든 칸이 채워져 더 넓음)보다
     좁은 데이터 행이 헤더로 잘못 뽑혔다.
  2. 최댓값(가장 넓은 폭)으로 바꿨더니, 카카오뱅크 엑셀 실제 파일(성명·계좌번호·조회기간·요청일시·
     유의사항 안내 블록이 앞에 붙는 리포트 양식)에서 또 깨졌다 — 은행 리포트 템플릿이 안내 줄에도
     표 전체 너비만큼 **서식(스타일)만** 미리 입혀두는 경우가 있어서, 안내 줄의 실제 값은 3~4개뿐인데도
     POI의 `getLastCellNum()`(칸이 존재하는 범위)은 진짜 헤더와 똑같이 잡혀 안내 줄이 먼저 헤더로
     뽑혔다. `columns`가 전부 "컬럼1"~"컬럼9" 자리 표시자로 나오는 증상으로 드러났다(CSV는 이 문제가
     없다 — 콤마 구분 특성상 빈 필드도 콤마 자리 자체는 차지해서 "존재 범위"가 스타일에 좌우되지 않는다).
  3. **엑셀만** "칸 존재 범위"가 아니라 **"실제 값이 들어있는 칸의 개수"** 기준으로 다시 고쳤다
     (`CashFlowExcelParser.countFilledCells`) — 안내 줄은 스타일이 넓어도 실제 값은 몇 개 안 되고,
     진짜 헤더는 모든 칸에 이름이 차 있어 값 개수가 최대이므로 서식 폭에 안 흔들린다. CSV는 애초에
     이 문제가 없어 최댓값 기준(2번) 그대로 둔다.
  4. **엑셀만, 같은 날 한 번 더** — 헤더 판정 자체는 맞았는데 그 실제 헤더 행의 **맨 앞 칸이 진짜로
     비어있는** 파일이 있어서(예: 0번 칸 빈 칸, 1번 칸부터 "거래일시" 시작), 뒤쪽 빈 칸만 잘라내고
     앞쪽 빈 칸은 안 잘라 `columns` 맨 앞에 `"컬럼1"`이 계속 남았다. 앞쪽·뒤쪽 빈 칸을 모두 잘라내도록
     고치면서, 데이터 행도 **같은 시작 칸 위치**로 읽도록 오프셋을 맞췄다(`CashFlowExcelParser.
     HeaderColumns.startColumn`) — 헤더만 앞칸을 건너뛰고 데이터는 그대로 읽으면 컬럼이 한 칸씩
     밀려버리기 때문이다.

---

## 입출금 내역(CSV 기반) 업로드 `POST /api/v1/finance/cash-flows/csv`

**상태**: ✅ 확정
**인증 필요 여부**: Y — 편집 권한 보유자(위 미리보기와 동일)

미리보기에서 확정한 컬럼 매핑으로 CSV를 다시 파싱해 실제로 저장한다. 미리보기 API가 파일을 들고 있지
않으므로 **같은 파일을 재전송**해야 한다.

**Request Body** (multipart/form-data, 파트 2개)

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `file` | File | Y | CSV 또는 엑셀 파일 (프리뷰 때와 동일 파일 재전송) |
| `request` | JSON 문자열 | Y | 은행명 + 컬럼 매핑 정보 — 필드는 원 명세와 동일(`bankName`/`dateTimeMode`/`amountMode`/매핑 9종) + 신규 `balanceColumn`(선택, 중복 판정 보강용)·`password`(선택, 파일이 비밀번호로 보호된 경우) |

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.totalRows` | Int | 전체 행 수 |
| `data.savedCount` | Int | 저장 성공 건수 |
| `data.duplicateCount` | Int | 중복으로 제외된 건수 |
| `data.duplicateRows[].tradedAt` | LocalDateTime | 중복 거래 일시 |
| `data.duplicateRows[].amount` | BigDecimal | 중복 거래 금액 |
| `data.duplicateRows[].reason` | String | 제외 사유 ("이미 등록된 거래입니다.") |

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | — | "입출금 내역(CSV 기반) 업로드 성공" |
| 400 | Bad Request | `FINANCE_CSV_MAPPING_REQUIRED` | "필수 컬럼 매핑이 누락되었습니다." |
| 400 | Bad Request | `FINANCE_CSV_PASSWORD_REQUIRED` | "비밀번호가 필요한 파일입니다." (신규) |
| 400 | Bad Request | `FINANCE_CSV_PASSWORD_INVALID` | "비밀번호가 올바르지 않습니다." (신규) |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **비밀번호 지원 추가 (2026-08-10, 원 명세에 없던 흐름)** — 위 미리보기 API와 동일한 이유·동일한 코드
  구분(`FINANCE_CSV_PASSWORD_REQUIRED`/`FINANCE_CSV_PASSWORD_INVALID`)이다. `request` JSON에 `password`
  필드를 추가했다.
- **`request` 파트를 문자열로 받아 직접 파싱한다** — `@RequestPart(CashFlowCsvUploadRequest)`로 바로
  받으면 클라이언트가 그 파트에 `Content-Type`을 안 붙였을 때(Swagger UI 기본 동작) 415로 거부된다.
  image 도메인의 이미지 항목 생성 API와 동일한 이유·동일한 해법이다(`.ai/api/image.md` 참고).
- **`FINANCE_CSV_MAPPING_REQUIRED` 재사용 범위를 넓혔다** — 원 명세는 "필수 컬럼 매핑이 누락되었습니다"
  하나만 정의했는데, 아래 경우 전부 이 코드로 묶었다: 은행명 누락, `dateTimeMode`/`amountMode` 값이
  `SINGLE`/`SEPARATE`/`SINGLE_WITH_TYPE` 중 하나가 아님, 모드별 필수 매핑 필드 누락, 매핑에 적힌 컬럼명이
  실제 CSV 헤더에 없음, 날짜·시간·금액 셀 값을 해석할 수 없음, "구분" 셀 값을 입금/출금으로 분류할 수
  없음. 400 코드가 이거 하나뿐이라 세분화하지 않았다 — 프론트가 코드별 분기가 아니라 메시지를 그대로
  보여주는 흐름이라면 문제없지만, 세분화가 필요해지면 알려달라.
- **⚠️ "구분" 컬럼(SINGLE_WITH_TYPE 모드) 값 해석 규칙도 명세에 없어 직접 설계했다** — 셀 값에 "입금"·
  "INCOME"이 포함되면 INCOME, "출금"·"OUTCOME"이 포함되면 OUTCOME. 실제 은행 CSV의 "구분"
  컬럼 값(예: "입금"/"출금"만 쓰는지, 다른 표기가 있는지)으로 검증·보강이 필요하다.
  ⚠️ **1글자 키워드("입"/"출")는 완전 일치로만 인정한다 (2026-08-11, CodeRabbit 지적으로 수정)** —
  원래는 "입"/"출"도 부분 문자열로 인정해서, "카드매입"처럼 무관한 단어 속에 "입" 한 글자가 우연히
  들어있으면 출금인데 입금으로 오판정될 위험이 있었다(신한·카카오 테스트 파일은 "입금"/"출금" 전체
  단어만 써서 실제로는 안 겪었음). 셀 값이 정확히 "입"/"출" 한 글자뿐일 때만 인정하도록 좁혔다 — 그
  외 애매한 값은 조용히 잘못 저장하지 않고 `FINANCE_CSV_MAPPING_REQUIRED`로 명확히 실패시킨다.
- **⚠️ 날짜/시간 포맷도 직접 설계했다** — `yyyy-MM-dd`, `yyyy/MM/dd`, `yyyy.MM.dd`, `yyyyMMdd` (+시간
  `HH:mm:ss`, `HH:mm`, `HHmmss`, 그리고 이 조합들의 datetime 버전)를 순서대로 시도한다. 전부 실패하면
  `FINANCE_CSV_MAPPING_REQUIRED`. 실제 은행 CSV 샘플이 들어오면 포맷 목록을 검증·보강해야 한다.
- **`depositorColumn`(거래처)은 모드와 무관하게 항상 필수 매핑이다 (2026-08-10 정정)** — 원 명세 표는
  선택(N)이었으나, `cash_flow.depositor_name`이 DB에서 `NOT NULL`이고 업로드 화면에서도 거래처를 필수로
  받기로 확정돼 실제로는 필수다. 매핑에 없거나 CSV 헤더에 없는 컬럼을 가리키면 다른 필수 매핑과 동일하게
  `FINANCE_CSV_MAPPING_REQUIRED`(400)로 막는다. (초안에서 `memoColumn`/`"-"`로 대체하는 방식을 시도했었는데
  불필요한 우회였다 — 폐기)
- **`bankTxnId`(거래고유번호)는 CSV 컬럼이 아니라 서버가 생성한다** — 명세의 `recommendedMapping` 9종에
  `bankTxnId` 항목이 없어서, `cash_flow.bank_txn_id` 컬럼 코멘트("은행명 4자리+거래일시+순번 조합")를
  그대로 따라 `은행명 앞 4자 + yyyyMMddHHmmss(tradedAt)`로 생성하고, 같은 배치 안에서 값이 겹치면 뒤에
  `-2`, `-3`처럼 일련번호를 붙인다.
- **중복 판정 — 같은 회사·같은 은행 안에서 `(tradedAt, amount)` 조합 기준** — DB의
  `uk_cash_flow_dedup(company_id, bank_name, traded_at, amount)` 유니크 제약과 동일한 기준으로, INSERT
  전에 먼저 SELECT로 이미 존재하는 조합을 걸러낸다(제약 위반으로 배치 전체가 실패하는 것을 막기 위해).
  **같은 파일 안에서의 중복도 같은 로직으로 걸러진다**(먼저 나온 행이 저장되고 그 다음 겹치는 행이
  `duplicateRows`로 빠진다).
  ⚠️ 동시에 같은 내용으로 두 번 업로드하는 극단적인 동시성 케이스는 이 사전 체크로 완전히 막지 못한다
  (DB 유니크 제약이 최종 방어선). 이 프로젝트가 테스트 0개 기조라 이 경합까지 별도 처리하지 않았다.
- **SEPARATE 금액 모드에서 입금액·출금액 셀이 둘 다 비어있는 행은 조용히 건너뛴다** — 은행 CSV의 합계/
  총계 행 등을 배제하기 위한 것으로, `totalRows`(원본 행 수)와 `savedCount + duplicateCount`가 다를 수
  있다(건너뛴 행만큼 차이 남).
- **⚠️ SEPARATE 금액 모드에서 "값 있음" 판정은 `null`이 아니라 `0이 아님` 기준이다 (2026-08-10, 실제
  파일로 발견)** — 은행이 거래 없는 쪽을 빈 칸이 아니라 문자 그대로 `"0"`으로 채워 내려주는 경우가 흔했다
  (엑셀에서는 서식 때문에 빈칸처럼 보였을 뿐 원본 셀 값은 `"0"`). 처음엔 "칸이 null이 아니면 그 타입"으로
  판정해서, 예를 들어 `출금(원)=17900, 입금(원)=0`인 행을 "입금 0원"으로 완전히 잘못 분류하는 버그가
  있었다 — 두 컬럼 모두 파싱한 뒤 값이 있고 **0이 아닌** 쪽만 실제 거래로 인정하도록 고쳤다. 양쪽 다
  0이거나 없으면(위 항목의 합계/총계 행 등) 건너뛴다.
- **⚠️ `amount`는 항상 절댓값(양수)으로 저장한다 (2026-08-10, 실제 파일로 발견)** — 카카오뱅크 파일처럼
  "거래금액"에 부호(`"-10,000"`)를 같이 싣는 은행이 있는데, `구분`(SINGLE_WITH_TYPE의 typeColumn)으로
  방향을 이미 판정하면서 금액에 부호까지 남겨두면 `type=OUTCOME`인데 `amount=-10000`처럼 방향이
  이중으로(type과 부호 둘 다) 표현돼 나중에 합계 계산이 꼬인다. 원본 CSV/엑셀 값 자체는 그대로 읽되,
  DB에 저장하는 시점(`CashFlowCsvRowParser.parseAmount`)에만 절댓값으로 변환한다 — 방향은 오직 `type`
  필드만 책임진다.
- **⚠️ "구분 컬럼 자체가 없고 금액 부호만으로 방향을 판정하는" 형태는 아직 지원하지 않는다** — 원 명세의
  2가지 모드(`SINGLE_WITH_TYPE`: 금액+구분 컬럼 세트 / `SEPARATE`: 입금액+출금액 컬럼 세트) 어디에도
  안 맞는 제3의 형태다. `SINGLE_WITH_TYPE`은 `typeColumn`이 필수라, 그 컬럼이 파일에 아예 없으면 업로드가
  막힌다. 실제로 이런 파일이 있는지 확인 안 됐고(2026-08-10 기준), 확인되면 그때 새 모드 추가를 논의한다.
- **`balanceColumn`(잔액) 신규 추가 — 중복 판정 정밀도 보강 (2026-08-10, 실제 파일로 발견·사용자 확정)**
  — 원 명세의 매핑 9종에 없던 필드다. 카카오뱅크 CSV에서 **같은 은행·같은 초·같은 금액**인데 실제로는
  서로 다른 거래(잔액이 다름) 2건이 있었는데, 기존 중복 판정 기준(은행명+거래일시+금액)으론 이 둘을
  구분 못 해서 두 번째를 "이미 등록된 거래"로 잘못 걸렀다. `cash_flow`에 `balance_after` 컬럼을 추가하고
  (`finance/V20260810100000`) 중복 판정 유니크 제약(`uk_cash_flow_dedup`)에도 포함시켰다. CSV에 잔액
  컬럼이 없으면 매핑 안 해도 된다(선택) — 그 경우 `balance_after`는 NULL로 저장되고 기존과 동일하게
  은행명+거래일시+금액만으로 판정된다.
  ⚠️ **MySQL 유니크 인덱스는 NULL끼리 서로 다른 값으로 취급한다** — 잔액 컬럼이 없는 은행(NULL로 저장)은
  이 보강의 혜택을 못 받는다는 뜻이다(완전한 해결이 아니라 잔액을 아는 은행에 한해 정밀도가 올라가는
  정도). 중복 사전 조회 쿼리(`findExistingDedupKeys`)도 이 NULL 특성 때문에 로우값 `IN((?,?,?),...)`
  문법을 못 쓰고(그 문법은 NULL이 낀 튜플을 아예 못 매칭시켜서 잔액 없는 은행의 재업로드 중복 검사가
  통째로 무력화된다) `OR`로 묶은 `AND` 조건 + NULL-safe 비교 연산자(`<=>`)로 다시 짰다.
- **업로드 메서드에 `@Transactional`을 일부러 안 붙인다 (2026-08-11, CodeRabbit 지적으로 제거)** —
  파일 파싱(POI/commons-csv)은 DB 접근이 없는데, 트랜잭션이 붙어있으면 파싱하는 몇 초 동안에도
  커넥션 풀에서 커넥션을 하나 붙잡고 있게 된다. `insertAll`이 여러 행을 한 번에 묶은 단일 INSERT문이라
  그 자체로 원자적이라, 조회(`findExistingDedupKeys`)와 굳이 트랜잭션으로 묶을 필요가 없다.
- **CodeRabbit이 지적했지만 반영 안 한 것 2건**:
  1. **`SINGLE_WITH_TYPE` 모드에서 금액 칸이 빈 행이면 예외 대신 건너뛰자는 제안** — 실제 입출금
     내역인 이상 금액이 없을 수 없다는 판단으로 반영 안 함(SEPARATE 모드가 빈 칸을 건너뛰는 건 "입금
     칸/출금 칸 중 한쪽만" 비는 정상 케이스라 다른 상황).
  2. **헤더 중복 시 대비(접미사 부여)** — 신한·카카오 실 테스트 파일 둘 다 중복 헤더가 없어서 근거
     없는 가정으로 판단, 나중에 실제로 그런 파일이 나오면 그때 반영.
- **⚠️ 중복 판정 키에 `type` 누락 — 진짜 버그, 수정 완료 (2026-08-11, CodeRabbit Critical 지적)** —
  `amount`는 항상 절댓값으로 저장하므로(방향은 `type`만 책임짐), `type`이 빠진 채로는 같은 시각·같은
  절댓값의 입금 1건과 출금 1건이 완전히 같은 dedup 키가 됐다. 잔액 컬럼이 없는 은행(둘 다
  `balance_after = NULL`)이거나, 시간 없이 날짜만 있는 CSV(`atStartOfDay()`로 자정 고정)라면 실제로
  터질 수 있는 시나리오였다. `dedupKey`·`findExistingDedupKeys`·`uk_cash_flow_dedup`(신규
  `V20260809140200`) 세 곳 전부에 `type`을 추가해 맞췄다.

---

## 입출금 내역 매칭 추천 조회 `GET /api/v1/finance/cash-flows/{cashFlowId}/match-candidates`

**상태**: ✅ 확정
**인증 필요 여부**: Y

입출금 내역 하나를 골랐을 때, 일자·금액·거래처명이 비슷한 정산 블록을 추천한다. 권한은 CSV 업로드와
동일한 편집 권한(`PagePermissionPort.hasEditAccess`, `FINANCE` 페이지).

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `cashFlowId` | Long | Y | 매칭할 입출금 내역 ID |

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.candidates[].settleId` | Long | 정산 블록 ID |
| `data.candidates[].roundName` | String | 정산 블록명(회차명) |
| `data.candidates[].projectName` | String | 프로젝트명 |
| `data.candidates[].plannedAmount` | BigDecimal | 예정 금액 |
| `data.candidates[].plannedDate` | LocalDate | 예정일 |
| `data.candidates[].traderName` | String | 거래처명 |
| `data.candidates[].matchTags` | List\<String\> | 추천 이유 태그 (예: "금액 일치", "일자 유사", "상호명 유사") |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "매칭 추천 조회 성공",
  "data": {
    "candidates": [
      {
        "settleId": 11,
        "roundName": "2차 기성(중도금)",
        "projectName": "한강 생태교육 환경개선사업",
        "plannedAmount": 90000000,
        "plannedDate": "2026-09-10",
        "traderName": "환경부",
        "matchTags": ["금액 일치", "상호명 일치"]
      },
      {
        "settleId": 15,
        "roundName": "1차 정산(선급)",
        "projectName": "신월 ICD 복합물류센터 구조설계 용역",
        "plannedAmount": 88000000,
        "plannedDate": "2026-08-20",
        "traderName": "국토교통부",
        "matchTags": ["금액 유사", "일자 일치"]
      }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "매칭 추천 조회 성공" |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." |
| 404 | Not Found | `FINANCE_CASH_FLOW_NOT_FOUND` | "존재하지 않는 입출금 내역입니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **✅ "회사 단위" 스코프 반영 완료 (2026-08-11)** — 원래는 `project`에 `company_id`가 없어 스코프 없이
  전체 프로젝트 대상으로 조회했는데(백로그로 남겨뒀던 항목), `develop`에 `project.company_id` 마이그레이션이
  들어오면서(동훈님, 멀티테넌시 P1-2a) 실제로 걸었다. CodeRabbit이 Critical로 지적한 크로스테넌트 유출
  ("타 회사 정산 블록/프로젝트 정보가 매칭 후보로 노출됨")도 같은 작업으로 해결됨 — `findMatchCandidates`·
  `findSettlementBlockForMatch` 둘 다 `project.company_id = #{companyId}` 조건 추가.
- **매칭 기준 3종 전부 명세에 없어 직접 설계했다 (사용자 확정)**:
  - 대상 정산 블록: **PENDING(미연결)만**, **같은 타입(INCOME/OUTCOME)만** — 이미 연결됐거나 반대
    방향인 블록은 추천 대상에서 제외
  - 금액: 완전 동일 = `EXACT`("금액 일치"), ±5% 이내 = `SIMILAR`("금액 유사")
  - 일자: `cash_flow.tradedAt`의 날짜와 `settlement_block.planned_date`가 같으면 `EXACT`("일자 일치"),
    ±7일 이내면 `SIMILAR`("일자 유사")
  - 거래처명: `cash_flow.depositorName`과 `settlement_block.traderName`이 완전 동일하면 `EXACT`("상호명
    일치"), 한쪽이 다른 쪽 문자열을 포함하면(양방향, 예: "신한" ↔ "신한은행") `SIMILAR`("상호명 유사") —
    맞춤법 오탈자 수준의 유사도(편집 거리 등)까지는 보지 않는다
  - 세 기준 중 **하나도 안 걸리면 후보에서 제외**한다(안 그러면 조건에 걸린 회사 전체 PENDING 블록이
    다 나온다)
  - 정렬: 걸린 기준 개수(EXACT/SIMILAR 구분 없이 카운트) 많은 순 → 그 안에서 일자 가까운 순
  - **최대 5건** — 페이지네이션 없음, 필요해지면 추가
- **`cashFlowId`가 다른 회사 소속이거나 삭제됐으면 미연결과 동일하게 404** — `company_id`·
  `deleted_at IS NULL` 조건까지 걸어서 조회, 못 찾으면 `FINANCE_CASH_FLOW_NOT_FOUND`.

---

## 입출금 내역 블록 매칭 `PATCH /api/v1/finance/cash-flows/{cashFlowId}/match`

**상태**: ✅ 확정
**인증 필요 여부**: Y

입출금 내역 하나를 정산 블록 하나에 연결한다. 매칭 추천 조회가 준 후보 중 하나를 고르거나, 프론트가 임의의
`settleId`를 보내도 동일하게 동작한다.

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `cashFlowId` | Long | Y | 매칭할 입출금 내역 ID |

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `settleId` | Long | Y | 연결할 정산 블록 ID |

**Request Example**

```json
{
  "settleId": 11
}
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.cashFlowId` | Long | 입출금 내역 ID |
| `data.settleId` | Long | 연결된 정산 블록 ID |
| `data.roundName` | String | 연결된 정산 블록명 |
| `data.projectName` | String | 연결된 프로젝트명 |
| `data.linkedBy` | String | 매칭 처리자 사번 |
| `data.linkedByName` | String | 매칭 처리자 이름 |
| `data.linkedAt` | LocalDateTime | 매칭 일시 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "입출금 내역 블록 매칭 성공",
  "data": {
    "cashFlowId": 5,
    "settleId": 11,
    "roundName": "2차 기성(중도금)",
    "projectName": "한강 생태교육 환경개선사업",
    "linkedBy": "vitas-EMP004",
    "linkedByName": "김재무",
    "linkedAt": "2026-08-07T15:30:00"
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "입출금 내역 블록 매칭 성공" |
| 400 | Bad Request | `FINANCE_CASH_FLOW_ALREADY_MATCHED` | "이미 매칭된 항목입니다." (이 입출금 내역이 이미 다른 정산 블록에 연결돼 있음) |
| 400 | Bad Request | `FINANCE_MATCH_TYPE_MISMATCH` | "입출금 구분과 정산 블록 타입이 일치하지 않습니다." (신규 — 아래 메모 참고) |
| 400 | Bad Request | `FINANCE_SETTLEMENT_BLOCK_ALREADY_MATCHED` | "이미 매칭된 정산 블록입니다." (신규 — 아래 메모 참고) |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." |
| 404 | Not Found | `FINANCE_MATCH_TARGET_NOT_FOUND` | "존재하지 않는 입출금 내역 또는 정산 블록입니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **HTTP 상태 코드 200 (원 명세는 201)** — 정산 PATCH 때와 동일한 불일치(Status Code 표는 201, Success
  Example JSON은 200)였다. 그때와 같은 이유로 JSON 예시를 신뢰해 200으로 구현했다.
- **404 코드 신규 부여** — 원 명세에 code 값이 비어 있었다. 입출금 내역/정산 블록 둘 다 "못 찾으면 같은
  메시지"라 하나의 코드(`FINANCE_MATCH_TARGET_NOT_FOUND`)로 묶었다. 기존 `FINANCE_CASH_FLOW_NOT_FOUND`는
  메시지가 "존재하지 않는 입출금 내역입니다."로 달라서 재사용하지 않았다.
- **타입 일치 검증 신규 추가 (명세에 없음, 사용자 확정)** — `cash_flow.type`과 `settlement_block.type`이
  다르면(예: INCOME 입출금 내역을 OUTCOME 정산 블록에 매칭) 막는다. 매칭 추천 조회가 애초에 같은 타입만
  후보로 주지만, 수동으로 다른 `settleId`를 보내는 경로까지 막으려면 매칭 시점에도 검증이 필요하다.
  ⚠️ **아직 항목을 한 번도 작성 안 한 빈 블록은 `type`이 `NULL`** — 처음엔 `.equals()`로 비교하다가
  `NullPointerException`(500)이 났다(2026-08-10, 실제 테스트로 발견). `Objects.equals`로 바꿔서 null도
  안전하게 "타입 불일치"(400 `FINANCE_MATCH_TYPE_MISMATCH`)로 처리한다 — 빈 블록은 애초에 타입이 안
  정해졌으니 매칭 대상이 될 수 없는 게 맞다.
- ⭐ **2026-08-13 규칙 변경 반영 완료** — 세금계산서 매칭이 붙으면서 "블록 1건당 매칭 1번"의 의미가
  **"원장별로 1건씩"**(입출금 1건 + 세금계산서 1장)으로 바뀌었다. 이 API의 실제 변경은 두 가지다.
  - 대상 조건이 `status = PENDING` → **`status IN ('PENDING','WAITING')`** — 세금계산서가 먼저 붙어
    `WAITING`(정산 대기)인 블록에도 입금을 매칭할 수 있다. `PARTIAL`/`COMPLETED`면 이미 입출금이
    붙었다는 뜻이라 계속 `FINANCE_SETTLEMENT_BLOCK_ALREADY_MATCHED`(400)로 막는다.
  - 매칭 후보 조회 대상도 `cash_flow`가 안 붙은 블록으로 바뀌었다(`WAITING` 블록 포함).
  - 상세는 위 "⭐ 정산 블록 status 규칙" 절 참고. **아래 항목들은 이 변경 전에 쓰인 설명이라
    "PENDING만 허용" 부분만 위 내용으로 읽으면 된다.**
- **⭐ 정산 블록 1건당 매칭은 1번뿐 (명세에 없음, 사용자 확정, 핵심 설계 결정)** — 정산 블록에 실제
  입금액이 예정보다 부족해도(부분 정산), **같은 블록에 두 번째 입출금 내역을 매칭해서 채우지 않는다.**
  실무팀이 부족분을 위한 **새 회차(블록)를 만들어서 그 블록에 다시 매칭**하는 방식으로 처리한다.
  그래서 이 API는 대상 정산 블록의 `status`가 `PENDING`(아직 아무것도 안 붙음)이어야만 매칭을 허용하고,
  이미 `PARTIAL`/`COMPLETED`인 블록에 또 매칭하려 하면 `FINANCE_SETTLEMENT_BLOCK_ALREADY_MATCHED`(400)로
  막는다. (스키마 자체는 `cash_flow.settle_block_id`가 같은 값을 여러 행이 가리키는 걸 막지 않지만,
  애플리케이션 레벨에서 1:1로 강제한다.)
- **매칭 시 정산 블록 상태·실적값도 같이 갱신 (명세에 없음, 사용자 확정)** — 이 API의 응답 필드엔 없지만,
  매칭되는 순간 `settlement_block.status`/`actual_amount`/`actual_date`도 이 요청의 cash_flow 값으로
  갱신된다:
  - `cash_flow.amount < settlement_block.planned_amount` → `status = PARTIAL`
  - `cash_flow.amount >= settlement_block.planned_amount` → `status = COMPLETED` (초과 입금도 완료로
    처리 — 초과분에 대한 환불 등 후속 처리는 이 API 범위 밖)
  - `actual_amount = cash_flow.amount`, `actual_date = cash_flow.tradedAt`
  - 이걸로 `SETL-007`(정산 블록이 연결되면 PATCH로 수정 불가)이 실제로 발동하게 된다 — 정산 도메인
    구현 당시 "연결 API가 붙을 때를 대비한 선제 방어"로만 남겨뒀던 코드가 이 API로 처음 실제 동작한다.
- **쓰기 방향 — `finance`가 `settlement_block`을 직접 UPDATE** — 지금까지 finance는 `settlement_block`을
  MyBatis로 직접 조인해서 **읽기만** 했는데(포트 없이), 이번엔 처음으로 **쓰기**까지 직접 한다
  (`CashFlowCommandMapper.updateSettlementBlockMatchResult`). 정식 포트로 감싸는 것도 고려했지만, 지금
  단계에서 정산 도메인에 이 갱신을 위임할 만한 기존 유스케이스가 없어서(정산 쪽 PATCH는 "내용 수정"이지
  "매칭 결과 반영"이 아님) 과설계로 판단해 finance가 직접 쓰는 걸로 갔다. 정산 도메인에 전용 유스케이스가
  생기면 그때 포트로 교체 검토.

---

## 입출금 내역 블록 매칭 해제 `PATCH /api/v1/finance/cash-flows/{cashFlowId}/unmatch`

**상태**: ✅ 확정
**인증 필요 여부**: Y

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `cashFlowId` | Long | Y | 매칭 해제할 입출금 내역 ID |

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | null | 항상 null |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "입출금 내역 블록 매칭 해제 성공",
  "data": null
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "입출금 내역 블록 매칭 해제 성공" |
| 400 | Bad Request | `FINANCE_CASH_FLOW_NOT_MATCHED` | "매칭되지 않은 항목입니다." |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." |
| 404 | Not Found | `FINANCE_CASH_FLOW_NOT_FOUND` | "존재하지 않는 입출금 내역입니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **HTTP 상태 코드 200 (원 명세는 201)** — 매칭 API와 동일한 사유.
- **실적값은 항상 비우고, 되돌릴 상태는 세금계산서 유무로 갈린다 (2026-08-13 변경)** — 블록당 입출금이
  1건이라 합계 재계산은 필요 없고 `actual_amount`/`actual_date`를 `null`로 되돌리면 된다. 다만 `status`는
  무조건 `PENDING`이 아니라, **세금계산서가 아직 붙어 있으면 `WAITING`(정산 대기)** 으로 간다(쿼리 안에서
  `CASE WHEN EXISTS`로 판정). 무조건 `PENDING`으로 되돌리면 세금계산서가 연결돼 있는데도 블록이 미연결로
  보이고 재무 요약의 미연결 카운트에도 잘못 잡힌다.

---

## 입출금 내역 직접 등록 `POST /api/v1/finance/cash-flows`

**상태**: ✅ 확정
**인증 필요 여부**: Y

은행 CSV 업로드 없이 입출금 내역 한 건을 직접 등록한다(`source_type = MANUAL`).

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `bankName` | String | Y | 은행명 |
| `tradedAt` | LocalDateTime | Y | 거래일시 |
| `type` | String | Y | 구분 (INCOME/OUTCOME) |
| `amount` | BigDecimal | Y | 거래금액 |
| `depositorName` | String | Y | 입금자명/수취인명 (원 명세는 선택(N)이었으나 정정 — 아래 메모 참고) |
| `memo` | String | N | 적요/메모 |

**Request Example**

```json
{
  "bankName": "신한은행",
  "tradedAt": "2026-08-07T14:30:00",
  "type": "INCOME",
  "amount": 5000000,
  "depositorName": "(주)테스트기업",
  "memo": "계약금 입금"
}
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.cashFlowId` | Long | 생성된 입출금 내역 ID |
| `data.bankTxnId` | String | 거래고유번호 (은행명 앞 4자 + 거래일시 기반 자동생성) |
| `data.bankName` | String | 은행명 |
| `data.tradedAt` | LocalDateTime | 거래일시 |
| `data.type` | String | 구분 |
| `data.amount` | BigDecimal | 거래금액 |
| `data.depositorName` | String | 입금자명/수취인명 |
| `data.memo` | String | 적요/메모 |
| `data.sourceType` | String | 수집 출처 (MANUAL 고정) |
| `data.createdAt` | LocalDateTime | 생성일시 |

**Success Example**

```json
{
  "httpStatus": 201,
  "message": "입출금 내역 등록 성공",
  "data": {
    "cashFlowId": 20,
    "bankTxnId": "신한-20260807143000",
    "bankName": "신한은행",
    "tradedAt": "2026-08-07T14:30:00",
    "type": "INCOME",
    "amount": 5000000,
    "depositorName": "(주)테스트기업",
    "memo": "계약금 입금",
    "sourceType": "MANUAL",
    "createdAt": "2026-08-07T15:00:00"
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | — | "입출금 내역 등록 성공" |
| 400 | Bad Request | `FINANCE_CASH_FLOW_REQUIRED_FIELD_MISSING` | "필수 항목이 누락되었습니다." (신규 — 아래 메모 참고) |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." |
| 409 | Conflict | `FINANCE_CASH_FLOW_DUPLICATE` | "이미 등록된 거래입니다." (신규) |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **400 메시지 교체** — 원 명세는 "필수 컬럼 매핑이 누락되었습니다.."로 CSV 업로드 명세("컬럼 매핑")가
  그대로 복붙된 것으로 보인다. JSON 직접 등록엔 "컬럼 매핑" 개념이 없어 "필수 항목이 누락되었습니다."로
  바꾸고 신규 코드(`FINANCE_CASH_FLOW_REQUIRED_FIELD_MISSING`)를 부여했다. `type`이 `INCOME`/`OUTCOME`이
  아닌 값이어도 이 코드로 처리한다.
- **`depositorName` 필수로 정정 (사용자 확정)** — 원 명세 표는 선택(N)이었으나 `cash_flow.depositor_name`이
  DB에서 `NOT NULL`이다. CSV 업로드 때 이미 같은 이유로 필수로 정정했던 것과 동일하게 처리했다.
- **409 중복 판정 기준 (사용자 확정)** — CSV 업로드와 동일한 유니크 제약(`uk_cash_flow_dedup`) 기준으로
  판단한다: 같은 회사·은행명·거래일시·금액이 이미 있으면 막는다. 직접 등록은 잔액(`balance_after`) 값이
  없어(`NULL`) 항상 그 조건까지 같이 본다.
- **`bankTxnId` 생성 — 배치 충돌 방지 순번 없음** — CSV 업로드는 한 파일 안에서 같은 은행+거래일시가
  겹치면 `-2`,`-3` 순번을 붙였지만, 이건 단건 등록이라 그 로직이 필요 없다. `은행명 앞4자-yyyyMMddHHmmss`만
  생성한다(극히 드물게 다른 은행의 기존 값과 겹쳐도 `bank_txn_id`엔 유니크 제약이 없어 문제 없음).

---

## 입출금 내역 수정 `PATCH /api/v1/finance/cash-flows/{cashFlowId}`

**상태**: ✅ 확정
**인증 필요 여부**: Y

직접 등록(`MANUAL`)한 입출금 내역을 수정한다. CSV/API로 들어온 항목이거나, 이미 정산 블록에 매칭된
항목(직접 등록 포함)은 **메모만** 수정할 수 있다.

```
sourceType != MANUAL  또는  이미 매칭됨(settle_block_id IS NOT NULL)
    → memo 외 필드를 보내면 400(FINANCE_CASH_FLOW_FIELD_EDIT_NOT_ALLOWED)으로 막는다
sourceType == MANUAL  그리고  미매칭
    → 보낸 필드만 부분 수정(전부 선택)
```

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `cashFlowId` | Long | Y | 수정할 입출금 내역 ID |

**Request Body** (전부 선택 — 부분 수정)

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `bankName` | String | N | 은행명 (직접등록·미매칭 항목만 반영) |
| `tradedAt` | LocalDateTime | N | 거래일시 (직접등록·미매칭 항목만 반영) |
| `type` | String | N | 구분 (직접등록·미매칭 항목만 반영) |
| `amount` | BigDecimal | N | 거래금액 (직접등록·미매칭 항목만 반영) |
| `depositorName` | String | N | 입금자명/수취인명 (직접등록·미매칭 항목만 반영) |
| `memo` | String | N | 적요/메모 (모든 항목에 적용) |

**Request Example**

```json
{
  "memo": "선급금(수정됨)"
}
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.cashFlowId` | Long | 입출금 내역 ID |
| `data.bankName` | String | 은행명 |
| `data.tradedAt` | LocalDateTime | 거래일시 |
| `data.type` | String | 구분 |
| `data.amount` | BigDecimal | 거래금액 |
| `data.depositorName` | String | 입금자명/수취인명 |
| `data.memo` | String | 적요/메모 |
| `data.sourceType` | String | 수집 출처 |
| `data.updatedAt` | LocalDateTime | 수정일시 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "입출금 내역 수정 성공",
  "data": {
    "cashFlowId": 20,
    "bankName": "신한은행",
    "tradedAt": "2026-08-07T14:30:00",
    "type": "INCOME",
    "amount": 5000000,
    "depositorName": "(주)테스트기업",
    "memo": "선급금(수정됨)",
    "sourceType": "MANUAL",
    "updatedAt": "2026-08-07T16:00:00"
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "입출금 내역 수정 성공" |
| 400 | Bad Request | `FINANCE_CASH_FLOW_REQUIRED_FIELD_MISSING` | "필수 항목이 누락되었습니다." (`type`이 INCOME/OUTCOME이 아님) |
| 400 | Bad Request | `FINANCE_CASH_FLOW_FIELD_EDIT_NOT_ALLOWED` | "메모만 수정할 수 있습니다." (신규 — 아래 메모 참고) |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." |
| 404 | Not Found | `FINANCE_CASH_FLOW_NOT_FOUND` | "존재하지 않는 입출금 내역입니다." |
| 409 | Conflict | `FINANCE_CASH_FLOW_DUPLICATE` | "이미 등록된 거래입니다." (신규 — 아래 메모 참고) |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **HTTP 상태 코드 200 (원 명세는 201)** — 다른 PATCH들과 동일한 사유(JSON 예시가 200).
- **400 메시지 교체** — POST와 동일하게 "필수 컬럼 매핑이 누락되었습니다.." 문구가 CSV 명세 복붙으로
  보여 정리했다. PATCH는 전부 선택 필드라 "누락"이 성립하지 않고, `type`을 보냈는데 INCOME/OUTCOME이
  아닐 때만 이 코드가 나간다.
- **⭐ "메모만 수정 가능" 위반 시 에러로 막는다 (사용자 확정, 원 명세보다 엄격)** — 처음엔 "다른 필드를
  보내도 조용히 무시"하는 안을 제안했으나, 사용자가 "다른 필드 보내면 에러를 내야지"라고 확정해서
  `FINANCE_CASH_FLOW_FIELD_EDIT_NOT_ALLOWED`(400) 신규 코드로 명시적으로 막는다 — 프론트가 잘못된
  필드를 보내고 있다는 걸 조용히 넘기지 않고 바로 알 수 있게.
- **409 신규 추가 (원 명세엔 없음, 사용자 확정)** — `bankName`/`tradedAt`/`amount`를 수정해서 다른 기존
  행과 `(company_id, bank_name, traded_at, amount, balance_after=NULL)` 조합이 겹치면(자기 자신은 제외)
  POST와 동일하게 409로 막는다. 안 그러면 DB 유니크 제약 위반으로 처리되지 않은 예외(500)가 날 수 있었다.
- **부분 수정(merge) 방식** — 요청에 없는(=null) 필드는 기존 값을 그대로 유지한다. 식별 필드(은행명·
  거래일시·금액) 중 하나라도 바뀔 때만 위 409 중복 재검사를 한다(메모만 바뀌는 흔한 경우까지 매번
  검사하지 않기 위함).

---

## 입출금 내역 삭제(배치) `DELETE /api/v1/finance/cash-flows`

**상태**: ✅ 확정 (원 명세는 단건 `{cashFlowId}` — 아래 메모 참고)
**인증 필요 여부**: Y

매칭되지 않은 건에 한해서 삭제할 수 있다(소프트 삭제). 화면에 체크박스로 여러 건을 선택하는 UX라
**배치(배열)로 받도록 변경했다** — 사용자 확정.

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `cashFlowIds` | List\<Long\> | Y | 삭제할 입출금 내역 ID 목록 |

**Request Example**

```json
{
  "cashFlowIds": [1, 2, 3]
}
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.deletedCount` | Int | 실제 삭제된 건수 |
| `data.skippedItems[].cashFlowId` | Long | 삭제하지 못한 입출금 내역 ID |
| `data.skippedItems[].reason` | String | 삭제하지 못한 사유("매칭된 항목은 삭제할 수 없습니다. 먼저 매칭을 해제해주세요." 또는 "존재하지 않는 입출금 내역입니다.") |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "입출금 내역 삭제 성공",
  "data": {
    "deletedCount": 2,
    "skippedItems": [
      { "cashFlowId": 3, "reason": "매칭된 항목은 삭제할 수 없습니다. 먼저 매칭을 해제해주세요." }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "입출금 내역 삭제 성공" |
| 400 | Bad Request | `FINANCE_CASH_FLOW_REQUIRED_FIELD_MISSING` | "삭제할 항목을 선택해주세요." (`cashFlowIds` 비어있음) |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **⭐ 단건(`DELETE /cash-flows/{cashFlowId}`) → 배치(`DELETE /cash-flows` + body 배열)로 변경 (사용자
  확정, 핵심 설계 변경)** — 원 명세는 path의 단일 `cashFlowId`였다. 그런데 화면에 체크박스로 여러 건을
  선택하는 UX가 있어서, 이미지 도메인의 복구 API(`imgIds` 배열)와 동일한 선례로 배치 삭제로 바꿨다.
- **부분 성공 방식 (사용자 확정)** — 선택한 것 중 매칭된 게 섞여 있으면, **매칭 안 된 것만 삭제**하고
  매칭된 것은 `skippedItems`에 사유와 함께 담아 알려준다(요청 전체를 막지 않음). 존재하지 않는 ID가
  섞여 있어도 동일하게 `skippedItems`로 알려준다.
- **404 없음 — 존재하지 않는 ID도 `skippedItems`로 처리** — 배치라 일부만 잘못돼도 나머지는 처리해야
  해서, 원 명세의 단건용 404(`존재하지 않는 입출금 내역입니다.`)를 최상위 에러로 쓰지 않고 개별 항목의
  스킵 사유로 내려준다.
- **HTTP 상태 코드 200 (원 명세는 201)** — 다른 것들과 동일한 사유.
- **소프트 삭제** — `cash_flow.deleted_at`만 세팅한다. 하드 삭제 정책은 다른 도메인과 동일하게 아직
  없음(팀 전체 백로그).
- **⚠️ `Collectors.toMap` NPE 버그 수정 (2026-08-10, 실제 테스트로 발견)** — 삭제 대상 판정 맵을
  `Collectors.toMap(cashFlowId, settleBlockId)`로 만들었는데, `Collectors.toMap`은 내부적으로
  `Map.merge`를 써서 값이 `null`이면(미매칭 = `settleBlockId` `null`인 정상 케이스) `NullPointerException`을
  던진다 — 매칭 안 된 것과 매칭된 것을 섞어서 삭제 요청하면 500이 났다. `HashMap.put`으로 직접 채우는
  방식으로 수정했다(`put`은 `null` 값을 허용).

---

## 입출금 내역 연결 제외 처리(배치) `PATCH /api/v1/finance/cash-flows/exclude`

**상태**: ✅ 확정
**인증 필요 여부**: Y

입출금 내역 업로드분 중 프로젝트와 무관한 건을 재무 요약(`GET /finance/summary`)의 `cashFlowUnlinkedCount`
집계에서 빼거나(`isExcluded: true`) 다시 포함시킨다(`isExcluded: false`) — 하나의 API로 양방향 다 처리한다.
삭제 API와 동일하게 체크박스로 여러 건을 고르는 UX라 배치로 받는다.

> 참고: `cashFlowUnlinkedCount`는 이 기능이 생기기 전부터 이미 `is_excluded = FALSE` 조건을 걸어두고
> 있었다(재무 요약 조회 최초 구현 때부터) — 이 API는 그 플래그를 사용자가 직접 켜고 끌 수 있게 해주는
> 것이다. 입출금 내역 목록 조회(`GET /finance/cash-flows`)의 `unlinked` 필터는 의도적으로 이 플래그를
> 안 본다(위 목록 조회 메모 참고) — 둘의 "미연결" 정의가 원래부터 다르다.

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `cashFlowIds` | List\<Long\> | Y | 연결 제외 처리(또는 취소)할 입출금 내역 ID 목록 |
| `isExcluded` | Boolean | Y | 제외 여부 (true: 제외, false: 제외 취소) |

**Request Example**

```json
{
  "cashFlowIds": [30, 31, 32],
  "isExcluded": true
}
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.updatedCount` | Int | 처리된 건수 |
| `data.skippedItems[].cashFlowId` | Long | 처리되지 못한 입출금 내역 ID |
| `data.skippedItems[].reason` | String | 처리되지 못한 사유("이미 매칭된 항목은 제외 처리할 수 없습니다." 또는 "존재하지 않는 입출금 내역입니다.") |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "입출금 내역 연결 제외 처리 성공",
  "data": {
    "updatedCount": 2,
    "skippedItems": [
      { "cashFlowId": 31, "reason": "이미 매칭된 항목은 제외 처리할 수 없습니다." }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "입출금 내역 연결 제외 처리 성공" |
| 400 | Bad Request | `FINANCE_CASH_FLOW_REQUIRED_FIELD_MISSING` | "필수 항목이 누락되었습니다." (`cashFlowIds`/`isExcluded` 누락) |
| 403 | Forbidden | `FINANCE_EDIT_ACCESS_DENIED` | "편집 권한이 없습니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **HTTP 상태 코드 200 (원 명세는 201)** — 다른 배치 API들과 동일한 사유.
- **응답 메시지 교체** — 원 명세의 Success Example엔 "세금계산서 메모 수정 성공"이 그대로 남아있었다(다른
  API 명세 복붙 잔재). "입출금 내역 연결 제외 처리 성공"으로 정정했다.
- **⭐ 전체 실패 → 부분 처리로 변경 (사용자 확정, 원 명세보다 관대함)** — 원 명세는 "이미 매칭된 항목이
  섞여 있으면 400으로 요청 전체를 막는다"는 단일 에러 코드뿐이었다(`skippedItems` 개념 없음). 삭제
  API와 같은 이유로(체크박스 UX에서 하나 때문에 전체가 막히면 불편함) 부분 처리로 바꿨다 — 제외 취소는
  되돌릴 수 있는 작업이라 안전성 문제도 없다는 점도 고려했다. 매칭 안 된 것만 처리하고, 매칭된 것은
  `skippedItems`에 사유와 함께 담아 알려준다. 존재하지 않는 ID도 동일하게 `skippedItems`로 처리한다.
- **"이미 매칭됨" 검증은 제외(`isExcluded: true`)할 때만 적용** — 제외를 취소(`isExcluded: false`,
  다시 미연결 집계에 포함)하는 건 매칭 여부와 무관하게 항상 허용한다. "매칭된 건 제외 처리 못 함"이라는
  규칙 자체가 "이 거래는 프로젝트와 무관하다"는 뜻과 "이미 정산 블록에 연결됐다"는 사실이 모순되기
  때문이지, 제외를 되돌리는 방향엔 그 모순이 없다.
