# 정산 블록 API 명세

**최종 동기화**: 2026-08-09 (정산 현황 블록 조회 추가 + `includeCompleted` 판정 기준 정정)
**최종 동기화**: 2026-08-09 (정산 현황 프로젝트 조회 추가)
**최종 동기화**: 2026-08-09 (정산 항목 작성/수정 최초 작성)
**도메인 담당**: (미기재 — 작업자 본인 이름으로 채워주세요)

> 상태가 `✅ 확정` 이상인 항목은 프론트와의 계약이다. 임의 변경 금지.

---

## 🔴 2026-08-09 도메인 재설계

기존 `payment` · `block_payment_confirm`(입금확인 블록 상세) · `tax_invoice` · `tax_invoice_confirm`(세금계산서 조회 블록 상세)
4개 테이블을 폐기하고, `settlement_block`(정산 블록 상세) · `cash_flow`(입출금 내역) · `tax_invoice`(세금계산서, 신규 스키마)
3개로 교체했다 (`db/migration/settlement/V20260809130000__*.sql`).

- **블록 생성·삭제**: `SettlementBlockDetailAdapter`(`BlockDetailPort` 구현)가 붙었다 — Block 도메인이 블록 생성/삭제
  트랜잭션 중 이 포트를 호출하면 `SettlementHandlerService.create/delete`가 실제 처리한다(text/checklist와 동일 패턴).
- **`block.type` enum에 `SETTLEMENT` 추가 완료 (2026-08-09)** — `BlockType.java` + `V20260809132000__add_settlement_block_type.sql`
  (BID_NOTICE 선례와 동일하게 담당자 본인 브랜치에서 추가). `PAYMENT_CONFIRM`/`TAX_INVOICE_VIEW` 값은 이 작업 범위 밖이라
  그대로 뒀다 — 상세 테이블이 없는 채 남아있다(Block 도메인 담당자 정리 필요).
- **블록 목록 조회 연동 완료** — `SettlementBlockDetailAdapter.loadDetails()`가 `settlement_block`을 MyBatis로 배치 조회해
  `SettlementDetail`(이 API 응답과 동일한 필드)을 채운다. 계좌번호는 항상 마스킹된 값만 담긴다(원문 복호화 결과를
  그대로 내보내지 않는다).

| 상태 | 기능 | METHOD | URL | 권한 |
|------|------|--------|-----|------|
| ✅ 확정 (구현 완료) | 정산 항목 수정 시 조회 | GET | `/api/v1/blocks/settlements/{settleId}/items?type={INCOME\|OUTCOME}` | 편집 권한 보유자 |
| ✅ 확정 (구현 완료) | 정산 항목 작성/수정 | PATCH | `/api/v1/blocks/settlements/{settleId}/items?type={INCOME\|OUTCOME}` | 편집 권한 보유자 |
| ✅ 확정 (구현 완료, 임시 권한 어댑터) | 정산현황 필터 옵션 조회 | GET | `/api/v1/projects/settlements/filters` | 접근 권한 보유자(재무 관리 페이지) |
| ✅ 확정 (구현 완료, 임시 권한 어댑터) | 정산 현황 프로젝트 조회 | GET | `/api/v1/projects/settlements` | 접근 권한 보유자(재무 관리 페이지) |
| ✅ 확정 (구현 완료, 임시 권한 어댑터) | 정산 현황 블록 조회 | GET | `/api/v1/projects/{projectId}/settlements` | 접근 권한 보유자(재무 관리 페이지) |

---

### 정산현황 필터 옵션 조회 `GET /api/v1/projects/settlements/filters`

**상태**: ✅ 확정 (권한 판정은 임시 어댑터 — 아래 참고)
**인증 필요 여부**: Y

재무팀 정산현황 화면의 발주처 필터 드롭다운을 채운다. 위 두 엔드포인트(블록 단위)와 URL·데이터가 전혀
겹치지 않지만, 사용자 지시에 따라 별도 컨트롤러를 만들지 않고 같은 `SettlementController`에 메서드로만
추가했다(이 프로젝트는 `@RequestMapping` 클래스 레벨 지정을 쓰지 않고 메서드마다 전체 경로를 적는 컨벤션이라
한 컨트롤러에 여러 URL 패턴이 섞여도 문제없다).

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.clients` | List\<String\> | 정산 현황에 등장하는(활성 정산 블록이 하나 이상 있는 프로젝트의) 발주처 목록. 오름차순 정렬, 중복 없음 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "정산현황 필터 옵션 조회 성공",
  "data": {
    "clients": ["국토교통부", "부산항만공사", "서울시도로관리과", "환경부"]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "정산현황 필터 옵션 조회 성공" |
| 403 | Forbidden | `SETL-009` | "접근 권한이 없습니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **403 code — 원 명세에 값이 비어 있었다.** 정산 도메인 자체 코드 `SETL-009`를 새로 부여했다.
- **⚠️ 권한 판정은 임시 어댑터다 (2026-08-09).** "접근 권한 보유자"를 `.ai/api/page-permission.md`의
  `FINANCE` 페이지 권한으로 판정하기로 했는데, 그 도메인(담당 **김동현** — 사원/부서/직급 쪽, 동훈님 아님)에
  아직 Java 코드가 하나도 없다(`page_permission` 테이블 자체는 `V202608031500` 마이그레이션에 이미 존재).
  그래서 이미지 도메인의 선례(`ImageEligibilityPolicy.assertEditPermissionEvenIfBlockDeleted`)와 동일한
  방식으로, **`settlement` 도메인이 소비자로서 임시 어댑터를 직접 만들었다**:
  - `settlement/application/port/PagePermissionPort` — 포트(소비자 소유)
  - `settlement/infrastructure/adapter/PagePermissionMapperAdapter` + `PagePermissionMapper`(MyBatis) —
    `page_permission` 테이블을 직접 조회하는 **임시 구현**. 판정 규칙은 문서 그대로: `ADMIN`·`MASTER`는
    부여 기록 없이 항상 접근 가능(GLOBAL_ROLE), `MEMBER`는 `page_code='FINANCE'` 행이 있어야 한다
  - 김동현님 쪽에 정식 `PagePermission` 유스케이스가 생기면 `PagePermissionMapperAdapter` 내부 구현만
    그 포트 호출로 교체하면 된다(`PagePermissionPort` 인터페이스·호출부는 안 바뀐다)
  - 이미지 도메인 선례와 동일하게 **먼저 나서서 김동현님께 요청하지 않는다** — 사용자가 필요하다고
    판단하면 그때 전달
- **"정산현황에 등장하는" 발주처 범위** — 활성 정산 블록이 하나 이상 있는 프로젝트만 대상으로 했다
  (`SettlementStatusMapper.findDistinctClientNames`, `project → step → block → settlement_block` 조인).
  정산 블록이 아예 없는 프로젝트의 발주처는 필터에 담기지 않는다 — 정산현황 화면 자체에 나올 일이 없는
  발주처이기 때문. **(2026-08-09 확인 완료)** 아래 "정산 현황 프로젝트 조회"는 대상 범위가 다르다 — 그
  API는 재무팀이 "전체 프로젝트"를 봐야 해서 활성 정산 블록 유무와 무관하게 전 프로젝트를 대상으로
  한다(정산 블록이 없으면 관련 필드가 0/null로 나올 뿐 목록에서 빠지지 않는다). 의도된 차이다.
- **✅ 회사 범위(company_id) 반영 (2026-08-11 추가)** — 최초 구현 시점엔 `project`에 `company_id`가
  없어 전체 회사 대상으로 조회했다. `develop`에 `project.company_id` 마이그레이션이 들어온 뒤
  `p.company_id = #{companyId}` 조건을 추가했다 — 안 그러면 재무팀이 다른 회사 발주처명까지 보게 된다
  (finance 도메인 CodeRabbit 리뷰로 같은 종류의 문제가 지적돼 settlement도 같이 정정, 아래 두 API도 동일).

### 정산 현황 프로젝트 조회 `GET /api/v1/projects/settlements`

**상태**: ✅ 확정 (권한 판정은 임시 어댑터 — 필터 옵션 조회와 동일, 아래 참고)
**인증 필요 여부**: Y

재무팀 정산현황 화면에서 보여줄 **전체 프로젝트**를 프로젝트 단위로 집계해 조회한다(활성 정산 블록이 없는
프로젝트도 나온다 — 필터 옵션 조회의 "정산현황에 등장하는 발주처"와는 대상 범위가 다르다). 권한 판정은
필터 옵션 조회와 동일하게 `PagePermissionPort`(`FINANCE` 페이지)를 재사용한다.

**Request Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `startDate` | LocalDate | N | 조회 시작일 — **다음 정산 예정일(`nextPlannedDate`) 기준** |
| `endDate` | LocalDate | N | 조회 종료일 — 〃 |
| `client` | String | N | 발주처 (정확히 일치) |
| `includeCompleted` | Boolean | N | 종결(완료) 프로젝트 포함 여부. **생략하면 `false`(제외)** |
| `page` | Int | N | 0-base 페이지 번호. 생략하면 0 (2026-08-12 페이징 추가) |
| `size` | Int | N | 페이지당 개수. 생략하면 20, 최대 100 |
| `sort` | String | N | 정렬 기준. `NEXT_PLANNED_DATE_ASC`(다음 정산 예정일 빠른 순, 기본값) \| `TOTAL_AMOUNT_DESC`(총 합계 큰 순) |

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `data.page` | Int | 현재 페이지 번호 (0-base) |
| `data.size` | Int | 페이지당 개수 |
| `data.totalElements` | Long | 전체 항목 수 |
| `data.totalPages` | Int | 전체 페이지 수 |
| `data.projects[].projectId` | Long | 프로젝트 ID |
| `data.projects[].projectName` | String | 과업명 |
| `data.projects[].clientName` | String | 발주처 (nullable) |
| `data.projects[].projectManager` | String | 담당자(프로젝트 제작자) |
| `data.projects[].totalPlannedAmount` | Long | 총 계약금액. INCOME 정산 블록이 하나도 없으면 null |
| `data.projects[].totalOutcome` | Long | 총 비용(OUTCOME 실입금 합계). 없으면 0 |
| `data.projects[].totalIncome` | Long | 총 수입(INCOME 실입금 합계). 없으면 0 |
| `data.projects[].totalAmount` | Long | 총 합계 (`totalIncome - totalOutcome`) |
| `data.projects[].completedRoundCount` | Int | 완료된 회차 수 (INCOME+OUTCOME 합산) |
| `data.projects[].totalRoundCount` | Int | 전체 회차 수 (INCOME+OUTCOME 합산) |
| `data.projects[].nextPlannedDate` | LocalDate | 다음 정산 예정일. 미완료 회차가 없으면 null |
| `data.projects[].settlementStatusSummary` | String | 대표 상태 문구 — 지금은 `"정산완료"` 또는 `"미연결 N건"` 둘 중 하나. **미연결은 입출금 기준**(세금계산서만 연결된 회차도 포함) |
| `data.projects[].taxInvoiceUnlinkedCount` | Int | **세금계산서가 연결되지 않은 회차 수** (2026-08-13 신규). 입출금 연결 여부와 무관 — 입금이 끝난 회차도 세금계산서가 없으면 포함 |
| `data.projects[].projectStatus` | String | 프로젝트 상태 (`project.status`) |
| `data.projects[].endedOn` | LocalDate | 프로젝트 종료일. 종료되지 않았으면 null |

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "정산 현황 조회 성공" |
| 400 | Bad Request | `SETL-012` | "페이지 조회 조건이 올바르지 않습니다." — `page`<0 · `size`≤0 또는 >100 · `sort` 허용값 아님 · `startDate`>`endDate` |
| 403 | Forbidden | `SETL-009` | "접근 권한이 없습니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **403 코드** — 원 명세엔 `IMG-007`(이미지 도메인 코드)이 적혀 있었다. 필터 옵션 조회와 동일한 사유(다른
  도메인 명세 복붙 잔재)로 판단해 필터 옵션 조회에서 이미 쓰는 `SETL-009`를 재사용했다(같은 `FINANCE`
  페이지 권한 판정이라 프론트가 이미 처리 중인 분기를 그대로 쓸 수 있다).
- **`totalPlannedAmount` 계산 기준 (2026-08-09 확정)** — `project.contract_amount`가 아니라 **그 프로젝트
  INCOME 타입 정산 블록들의 `total_amount`**를 쓴다(SETL-008이 같은 프로젝트·같은 타입 내에서 회차 간
  항상 같은 값으로 강제하므로 `MAX` 하나만 뽑아도 안전). INCOME 정산 블록이 아직 하나도 없으면 null.
- **`completedRoundCount`/`totalRoundCount` 집계 기준 (2026-08-09 확정)** — 타입(INCOME/OUTCOME) 구분 없이
  그 프로젝트의 활성 정산 블록 전체를 합산해서 센다. `completedRoundCount`는 `status = 'COMPLETED'`인 것.
  `SettlementStatusMapper.findProjectSettlements`(마이바티스, 파생 테이블 1개로 프로젝트당 한 번에 집계)로
  계산한다.
- **`settlementStatusSummary` 규칙 (2026-08-09 확정, 의도적으로 단순화)** — 회차별 예정일까지 따지는
  세분화("입금 대기 N일" 등)는 아직 규칙이 없어 보류했다. 지금은 **딱 두 가지만** 구분한다:
  전체 회차가 다 `COMPLETED`면 `"정산완료"`, 아니면 미연결 회차 개수를 `"미연결 N건"`으로
  보여준다(회차가 하나도 없는 프로젝트도 `"미연결 0건"`으로 떨어진다). ⚠️ **이 문구 규칙은 나중에 늘어날
  수 있다** — 담당자가 세분화 규칙을 확정하면 `SettlementQueryService.settlementStatusSummary`를 손보면 된다.
- **미연결 기준이 `PENDING` → `PENDING`+`WAITING`으로 바뀌었다 (2026-08-13)** — 재무 도메인에 세금계산서
  매칭이 붙으면서 `WAITING`(세금계산서만 연결되고 입금은 아직)이 실제로 쓰이기 시작했다. 예전 기준으로는
  그 회차가 "미연결"에도 "정산완료"에도 안 잡혀 **조용히 사라졌다.** 이제 이 문구의 "미연결"은
  **입출금 기준**으로 읽는다. 재무 요약(`GET /finance/summary`)의 `settlement.unlinkedCount`도 같은 기준이다.
- **`taxInvoiceUnlinkedCount` 신규 (2026-08-13, 사용자 확정)** — 세금계산서 미연결은 위 문구와 **다른 개념**이라
  섞지 않고 숫자 필드로 따로 내린다(문자열 계약을 안 건드려야 프론트가 안 깨지고, 프론트가 배지를 직접
  조립할 수 있다). ⚠️ **판정을 `status`로 할 수 없다** — `PARTIAL`/`COMPLETED`는 "입출금이 붙었다"만
  말해주고 세금계산서 유무는 알려주지 않으므로, `tax_invoice.settle_block_id`를 직접 확인한다.
  ⚠️ **`WAITING`은 여기서 빠진다** — 세금계산서가 이미 붙은 상태이기 때문이다(붙지 않은 건 입출금 쪽).
  집계 대상은 활성 정산 블록 전체이며, 빈 블록(내용 미작성)도 세금계산서가 없으므로 포함된다.
- **`startDate`/`endDate` 필터 기준 (2026-08-09 확정)** — 정산 회차 하나하나의 `plannedDate`가 아니라,
  프로젝트 단위로 계산한 **`nextPlannedDate`**로 거른다. `nextPlannedDate` 자체는 "미완료(`status != COMPLETED`)
  회차 중 회차 번호(`round_no`)가 가장 낮은 것의 `planned_date`"다 — 1차가 미완료면 1차 예정일, 1차가
  완료되고 2차가 미완료면 2차 예정일이 뜨는 방식. 같은 회차 번호가 INCOME/OUTCOME 양쪽에 걸쳐 있으면
  그중 더 빠른 예정일을 쓴다.
- **`includeCompleted` 판정 기준 (2026-08-09 확정 → 같은 날 정정)** — 처음엔 `project.ended_on`(종료일) 유무로
  판정했으나, `ended_on`은 **생성 시점에 입력하는 예정 종료일**이라 진행 중인 프로젝트도 이미 값이 차 있을 수
  있어(예: 생성 요청에 `endedOn`을 같이 넣는 케이스) "종결(완료)" 판정 기준으로 틀렸다는 게 확인돼 정정했다.
  올바른 기준은 **`project.status`가 `COMPLETED` 또는 `CLOSED`인지**다(정상 완료·비정상 종결 둘 다 "종결"로
  묶어서 제외). **생략하면 `false`로 취급**해 두 상태를 제외한다.
- **대상 범위 — 필터 옵션 조회와 다르다** — 필터 옵션 조회(`/filters`)는 "활성 정산 블록이 하나라도 있는
  프로젝트"만 대상이지만, 이 API는 **전체 프로젝트**를 대상으로 한다(정산 블록이 아직 없는 프로젝트도
  0/null 값으로 나온다). "재무팀 쪽에서 보일 전체 프로젝트"라는 요구사항 그대로 반영.
- **✅ 페이징 추가 (2026-08-12)** — 프론트 요청으로 `page`/`size`/`sort` + `{page,size,totalElements,totalPages}`
  추가(공고·프로젝트 목록과 같은 컨벤션 — `page`≥0·`size`(1~100)·`sort` 허용값 검증, 위반 시 `SETL-012` 400,
  silent clamp 아님). **`projects` 키 이름은 유지**(아직 프론트 연동 전이라 `content`로 바꿀 필요 없다고 확인).
  `sort` 기본값 `NEXT_PLANNED_DATE_ASC`(널은 `IS NULL` 우선순위로 항상 뒤로) — 이전 고정 정렬(`projectId`
  오름차순)은 폐기, 이제 필요한 값은 이 두 가지뿐이라고 확인해 추가 검토 없이 확정.
- **CodeRabbit 리뷰 반영 (2026-08-12, 페이징 PR)**:
  1. **날짜 역전(`startDate > endDate`) 테스트 시나리오 누락 지적 — 반영.** `request.http`에 추가.
  2. **`SettlementQueryService`가 `SettlementStatusMapper`/`SettlementProjectRow`(MyBatis 구현 타입)를
     직접 참조해 Application 계층 경계를 넘는다는 지적(신규 `countProjectSettlements` 호출이 결합을 더
     늘림) — 반영 안 함, 별도 리팩터로 분리 제안.** 지적 자체는 맞다 — `.ai/ARCHITECTURE.md` §2-1의
     "다른 애그리게이트 테이블은 `application/port` + MyBatis 어댑터로" 원칙과 결이 같다. 하지만 이
     서비스는 **2026-08-09 최초 구현 시점부터** 이미 `SettlementStatusMapper`를 직접 주입받는 구조였고
     (`findDistinctClientNames`/`findProjectSettlements`/`findProjectSettlementBlocks`/`existsActiveProject`
     4개 기존 메서드 전부 동일 패턴), `finance/FinanceQueryService`도 `CashFlowMapper`를 똑같이 직접
     주입받는다 — 이번 PR(페이징) 하나만 고쳐서 해결되는 범위가 아니라 두 도메인의 Query Service 전체를
     Port/Adapter로 감싸는 별도 리팩터가 필요하다(Heavy lift, CodeRabbit 표기와 동일). 페이징 PR 범위를
     벗어난다고 판단해 반영 안 함 — 김동현님께 별도 리팩터링 이슈로 전달 필요.
- **`client` 필터는 정확히 일치** — 필터 옵션 조회가 내려주는 드롭다운 값을 그대로 선택하는 구조라 부분
  검색이 아니라 완전 일치로 구현했다.
- **✅ 회사 범위(company_id) 반영 (2026-08-11 추가)** — 위 필터 옵션 조회와 동일한 사유로
  `p.company_id = #{companyId}` 조건 추가.

### 정산 현황 블록 조회 `GET /api/v1/projects/{projectId}/settlements`

**상태**: ✅ 확정 (권한 판정은 임시 어댑터 — 위 두 개와 동일, 아래 참고)
**인증 필요 여부**: Y

정산 현황 프로젝트 조회(목록)에서 프로젝트 하나를 드릴다운했을 때 그 프로젝트에 속한 정산 블록을 회차별로
보여준다. **권한은 위 두 개(필터 옵션·프로젝트 목록)와 동일하게 FINANCE 페이지 권한이다** — 정산현황
페이지의 화면이라 프로젝트 참여자가 아니어도 재무팀이면 봐야 한다(2026-08-09, 최초엔 프로젝트 접근
권한으로 잘못 판단했다가 정정). 존재 확인은 프로젝트 멤버 여부와 무관하게 단순히 프로젝트가 살아있는지만
본다(`SettlementStatusMapper.existsActiveProject`) — 프로젝트 도메인의 `ProjectAccessUseCase`는 멤버십까지
같이 판정해서 이 용도에 안 맞는다.

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 정산 블록 정보를 조회할 프로젝트 ID |

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `data.blocks[].settleId` | Long | 정산 블록 아이디 |
| `data.blocks[].roundNo` | Int | 회차 번호 (nullable — 아직 작성 전인 빈 블록) |
| `data.blocks[].roundName` | String | 회차명(정산 블록명, `block.title`) |
| `data.blocks[].plannedDate` | LocalDate | 예정일 |
| `data.blocks[].plannedAmount` | Long | 예정금액 |
| `data.blocks[].plannedTaxAmount` | Long | 예정 세금 금액 |
| `data.blocks[].taxInvoiceDate` | LocalDate | 세금계산서 발행일. 연결된 세금계산서 없으면 null |
| `data.blocks[].taxInvoiceAmount` | Long | 세금계산서 금액 — **이 정산 블록의 `plannedTaxAmount`와 같은 값** (아래 메모 참고) |
| `data.blocks[].paidType` | String | 입출금 구분(`INCOME`\|`OUTCOME`) |
| `data.blocks[].bankName` | String | 은행명(`OUTCOME`만, 아니면 null) |
| `data.blocks[].accountNumber` | String | 계좌번호(마스킹, `OUTCOME`만, 아니면 null) |
| `data.blocks[].accountHolder` | String | 예금주(`OUTCOME`만, 아니면 null) |
| `data.blocks[].paidDate` | LocalDate | 실제 입출금일 |
| `data.blocks[].paidAmount` | Long | 실제 입출금액 |
| `data.blocks[].status` | String | 정산 블록 회차 상태 |
| `data.blocks[].taxLinkedBy` | String | **세금계산서** 매칭 처리자 사번. 없으면 null |
| `data.blocks[].taxLinkedByName` | String | 〃 이름 |
| `data.blocks[].taxLinkedAt` | LocalDateTime | 〃 매칭 처리일시 |
| `data.blocks[].cashFlowLinkedBy` | String | **입출금 내역** 매칭 처리자 사번. 없으면 null |
| `data.blocks[].cashFlowLinkedByName` | String | 〃 이름 |
| `data.blocks[].cashFlowLinkedAt` | LocalDateTime | 〃 매칭 처리일시 |

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "정산 현황 조회 성공" |
| 403 | Forbidden | `SETL-009` | "접근 권한이 없습니다." |
| 404 | Not Found | `SETL-010` | "존재하지 않는 프로젝트입니다." |

**원 명세와 다르게 처리한 것 / 구현 메모**:
- **권한 판정을 프로젝트 접근 권한으로 잘못 짰다가 정정 (2026-08-09, 같은 날)** — 처음엔 path에
  `projectId`가 있고 명세의 권한 컬럼이 "접근 권한 보유자"로만 적혀 있어(재무 관리 페이지라는 수식이 없어서)
  프로젝트 멤버 권한(`ProjectAccessUseCase.requireAccess`)으로 구현했었다. 담당자가 "정산 현황 페이지라
  재무팀만 봐야 한다"고 정정해서, 위 두 엔드포인트와 동일하게 **`PagePermissionPort`(`FINANCE` 페이지)**로
  바꿨다. 404 코드도 그에 맞춰 `project` 도메인 코드(`PROJECT_NOT_FOUND`) 대신 정산 도메인 자체 코드
  (`SETL-010`, 신규)로 바꿨다 — 존재 확인 자체는 `SettlementStatusMapper.existsActiveProject`로 직접
  하고(멤버십 무관, 단순 `deleted_at IS NULL` 확인), 403은 필터 옵션·프로젝트 목록과 동일한 `SETL-009`를
  재사용한다.
- **`linkedBy`/`linkedByName`/`linkedAt`을 `taxLinkedBy*`/`cashFlowLinkedBy*` 둘로 나눴다 (요청사항)** —
  원 명세는 단일 `linkedBy`/`linkedByName`/`linkedAt`였다. 하지만 `settlement_block` 자체엔 이 컬럼이
  없고, `cash_flow`·`tax_invoice` 두 원장 테이블이 **각자** `linked_by`/`linked_at`을 갖고 있어서 한 정산
  블록에 세금계산서와 입출금 내역이 **둘 다** 연결될 수 있다. 하나로 합치면 "누가 세금계산서를 맞췄는지"와
  "누가 입출금을 맞췄는지"가 뭉개지므로, 담당자 지시대로 `tax`/`cashFlow` 접두어로 나눠 응답한다. 각각
  그 블록에 연결된 것 중 가장 최근(`linked_at` 내림차순) 한 건만 노출한다(한 블록에 여러 건이 연결될
  수 있는 스키마라 배열이 아니라 단일 값을 위해 최신 것 하나로 확정).
- **`taxInvoiceAmount`는 `tax_invoice` 테이블이 아니라 이 정산 블록의 `planned_tax_amount`다 (요청사항)** —
  원 명세는 `tax_invoice` 테이블에서 가져오는 것처럼 보였으나(응답명이 "세금계산서 금액"), 담당자가
  **정산 블록 테이블의 세금 금액**(`plannedTaxAmount`와 같은 컬럼)을 쓰라고 확정해서 그대로 반영했다.
  결과적으로 이 응답에서 `taxInvoiceAmount`와 `plannedTaxAmount`는 항상 같은 값이 나간다 — 의도된
  중복이다(프론트가 두 이름으로 각각 참조할 걸 대비한 것으로 보임). `taxInvoiceDate`는 그대로
  `tax_invoice.issued_no`에서 가져온다(세금계산서 자체가 없으면 null이 되는 유일한 필드).
- **403 코드** — 원 명세엔 `IMG-007`(이미지 도메인 코드, 다른 명세 복붙 잔재로 판단)이 있었다. 필터
  옵션·프로젝트 목록과 동일한 `SETL-009`로 구현했다(위 정정 메모 참고).
- **정렬 순서** — 명세에 없어 `roundNo` 오름차순(null은 뒤로)으로 뒀다. 같은 회차 번호가 없다는 전제(같은
  프로젝트 내 회차 번호는 타입별로만 겹칠 수 있음)에서 `settleId` 오름차순으로 2차 정렬한다.
- **한 블록에 세금계산서/입출금이 여러 건 연결되는 경우** — 지금은 최신 것 하나만 보여준다(위 참고).
  프론트가 전체 연결 이력을 봐야 하면 별도 API가 필요할 수 있다 — 필요해지면 알려달라.
- **✅ 회사 범위(company_id) 반영 (2026-08-11 추가)** — `existsActiveProject`(404 판정)와
  `findProjectSettlementBlocks` 둘 다 `project.company_id = #{companyId}` 조건을 추가했다 — 다른 회사의
  `projectId`를 넣으면 존재하지 않는 것과 동일하게 404로 처리된다(크로스테넌트 조회 차단).

### 정산 항목 수정 시 조회 `GET /api/v1/blocks/settlements/{settleId}/items?type={INCOME|OUTCOME}`

**상태**: ✅ 확정 (2026-08-09 — `type` 필수 쿼리파라미터 추가)
**인증 필요 여부**: Y

정산 항목 수정 화면의 **타입 변경 탭 클릭 시** 호출한다(수정 버튼 클릭 시가 아니다 — 2026-08-09 트리거 지점
확정). 그 타입(INCOME/OUTCOME) 기준으로 추천 회차 번호·추천 총 금액을 조회하고, `OUTCOME`인 경우 마스킹
없는 원본 계좌번호를 조회한다. 실제 저장은 별도로 "저장하기" 클릭 시 PATCH가 호출된다. 조회인데 **편집
권한**을 요구하는 이유는 "수정을 위한 조회"이기 때문이다(image 도메인의 항목 전체 조회 API와 동일한 판단
— `.ai/api/image.md` 참고).

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `settleId` | Long | Y | 항목을 수정할 정산 블록 ID |

**Request Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `type` | String | Y | 지금 화면에서 선택 중인 타입. 이 타입 기준으로 추천값을 계산한다 |

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.settleId` | Long | 수정할 정산 블록 ID |
| `data.recommendRoundNo` | Int | 같은 프로젝트·같은 타입에 지금까지 존재했던(삭제 포함) `round_no` 중 최댓값 + 1(하나도 없으면 1). **이 블록에 이미 내용이 있으면(재수정) null** |
| `data.recommendTotalAmount` | Long | 같은 프로젝트·같은 타입의 다른 정산 블록 중 이미 값이 채워진 총 예정 금액. **이 블록에 이미 내용이 있으면(재수정) null** |
| `data.originalAccountNumber` | String | 마스킹 없는 원본 계좌번호. 이 블록에 **이미 내용이 있고** 요청한 `type`이 `OUTCOME`인 경우에만 값이 있다(그 외 null) |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "정산 항목 수정 시 조회 성공",
  "data": {
    "settleId": 1,
    "recommendRoundNo": 2,
    "recommendTotalAmount": 4500000,
    "originalAccountNumber": "100555574444"
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "정산 항목 수정 시 조회 성공" |
| 400 | Bad Request | `SETL-005` | "정산 블록의 타입 지정은 필수입니다." (`type` 쿼리파라미터 누락/오값) |
| 403 | Forbidden | `SETL-001` | "편집 권한이 없습니다." |
| 404 | Not Found | `SETL-002` | "존재하지 않는 블록입니다." |
| 409 | Conflict | `SETL-006` | "출금(OUTCOME)에서 입금(INCOME)으로는 타입을 변경할 수 없습니다." (이미 `OUTCOME`으로 저장된 블록을 `type=INCOME`으로 조회 시도) |

> 401(`AUTH_UNAUTHENTICATED`)·500(`COMMON_INTERNAL_ERROR`)은 전 엔드포인트 공통이라 이 문서·Swagger에는
> 표시하지 않는다 (2026-08-09부터 이 도메인 컨벤션 — 아래 PATCH 엔드포인트도 동일).

**원 명세와 다르게 처리한 것**:
- **403/404 코드** — 원 명세엔 `IMG-007`/`IMG-003`(이미지 도메인 코드)가 적혀 있었다. 다른 도메인 명세에서
  복붙되며 남은 것으로 보여 정산 도메인 자체 코드 `SETL-001`/`SETL-002`로 구현했다(PATCH 엔드포인트와 동일해
  프론트가 이미 처리 중인 분기를 재사용할 수 있다).
- **`recommendRoundNo` 철자 수정 완료 (2026-08-09)** — 원래 `recommendAroundNo`로 구현했었으나 오타로 확인돼
  `recommendRoundNo`로 정정했다(노션 명세도 사용자가 직접 정정함).
- **`type` 쿼리파라미터 추가 (2026-08-09, 최초 구현 이후 변경)** — 처음엔 이 API가 블록 생성 직후(타입 미정)
  호출된다고 보고 타입 파라미터 없이 프로젝트 전체 기준으로 추천했다. 그런데 그러면 한 프로젝트에 INCOME/
  OUTCOME 총 금액이 실제로 다르게 운영될 때 추천값이 반대 타입 금액을 잘못 추천할 수 있다는 문제가 있었고,
  사용자가 "이왕 추천할 거면 확실하게"라며 **트리거 지점을 수정 버튼이 아니라 타입 변경 탭으로 바꾸고
  `type`을 필수 파라미터로 받기로 결정**했다. 이제 `recommendRoundNo`·`recommendTotalAmount`·
  `originalAccountNumber` 셋 다 이 `type` 기준으로 정확하게 계산된다. `type` 검증 실패는 PATCH와 동일하게
  `SETL-005`를 재사용한다.
- **회차 번호 추천 계산 (2026-08-10 변경)** — 원래는 "같은 프로젝트·같은 타입 **활성** 정산 블록 개수(이 블록
  제외) + 1"이었다. 그런데 정산 블록 삭제(실무팀이 잘못 만든 회차를 지우는 경우)를 고려하니, 예를 들어
  1·2·3차가 있다가 2차가 삭제되면 남은 활성 블록은 1·3차 둘뿐이라 개수+1인 "3"을 추천해버려 **이미 존재하는
  회차 번호와 충돌**하는 문제가 있었다. 회차 번호는 삭제돼도 재사용하지 않는다는 정책(세금계산서/전표 번호처럼
  취소돼도 결번으로 남기는 회계 관행과 동일 — 이미 외부에 "2차"로 커뮤니케이션했을 수 있어 재사용하면 혼동)으로
  확정해, **"같은 프로젝트·같은 타입에 지금까지 존재했던(삭제 포함) round_no 중 최댓값 + 1"**로 바꿨다(하나도
  없으면 1). 삭제 패턴과 무관하게 항상 다음 빈 번호를 안전하게 추천한다. **회차 번호 중복 자체는 막지 않는다**
  — `paidAmountRatio`(프로젝트+타입 단위 합산)·`SETL-008`(총액 일치성)·"다음 정산 예정일" 계산(정산현황
  프로젝트 조회) 전부 프로젝트 단위로 `GROUP BY`/집계하지 round_no 유일성에 의존하지 않아서, 중복이 생겨도
  기존 로직이 깨지지 않음을 코드로 확인했다. 필요하면 실무팀이 회차 번호를 직접 수정해서 자유롭게 쓸 수 있다.
- **총 금액 추천 계산** — `SETL-008` 검증에 쓰는 것과 같은 우선순위 규칙을 그대로 쓴다: 같은 프로젝트·같은
  타입의 다른 정산 블록 중 이미 연결된(status != PENDING) 블록의 값을 최우선으로, 없으면 아무 값이나(먼저
  만들어진 순) 추천한다.
- **원본 계좌번호는 저장된 값이 아니라 요청한 `type` 기준으로 노출 여부를 판단한다** — "타입 변경 탭"에서
  호출되므로 화면에 표시 중인 타입이 이 블록에 이미 저장된 타입과 다를 수 있다(예: 저장은 INCOME으로 돼
  있는데 사용자가 지금 OUTCOME 탭을 보는 중). `AccountNumberCipher.decrypt`를 이 엔드포인트에서만 예외적으로
  그대로 노출한다(다른 모든 응답은 마스킹만 내려준다).
- **빈 블록일 때만 추천값을 준다 (2026-08-09 추가)** — 처음엔 이미 내용이 채워진 블록을 조회해도 추천값이
  나가는 버그가 있었다(사용자가 직접 테스트로 발견 — "이미 값 채워져 있으면 추천이 왜 나오나"). `roundNo`가
  `null`인지로 "빈 블록(최초 작성 전)"을 판정한다 — `roundNo`는 항목 작성/수정 API의 공통 필수 필드(`SETL-003`)
  라 다른 필드들과 항상 세트로 채워지므로, 이 필드 하나만 봐도 충분하다. **이미 채워진 블록**이면
  `recommendRoundNo`/`recommendTotalAmount`는 `null`(추천이 무의미), `originalAccountNumber`만 조건에 따라
  값이 나간다 — 두 그룹의 필드가 서로 배타적으로 채워진다.
- **저장된 타입과 다른 타입으로 조회하면 PATCH와 같은 규칙으로 막는다 (2026-08-09 추가)** — 이미
  `OUTCOME`으로 저장된 블록을 `type=INCOME`으로 조회하면 `SETL-006`(409)으로 막는다. 어차피 저장 시점에
  막힐 전환(OUTCOME → INCOME)을 조회 단계에서 미리 알려주는 것. `assertNoTypeDowngrade`를
  `SettlementCommandService`에서 `SettlementEligibilityPolicy`로 옮겨 PATCH·GET 양쪽이 같은 메서드를 쓴다.
- **`Cache-Control: no-store` 추가 (2026-08-09, CodeRabbit)** — 이 엔드포인트는 이미 채워진 `OUTCOME` 블록을
  조회하면 마스킹 없는 원본 계좌번호를 응답에 그대로 싣는다. GET 응답이라 브라우저·중간 프록시가 캐시할
  수 있어서, 편집 권한 검사만으로는 캐시된 사본까지 통제할 수 없다는 지적을 받아 응답에 캐시 차단 헤더를
  명시적으로 달았다.

---

### 정산 항목 작성/수정 `PATCH /api/v1/blocks/settlements/{settleId}/items?type={INCOME|OUTCOME}`

**상태**: ✅ 확정 (Block 도메인 enum 연동 대기)
**인증 필요 여부**: Y

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `settleId` | Long | Y | 정산 내용을 작성할 정산 블록의 ID |

**Request Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `type` | String | Y | 우리 회사 입장에서 입금(INCOME)인지 출금(OUTCOME)인지 여부 |

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `roundNo` | Int | Y | 정산 회차 |
| `totalAmount` | Long | Y | 프로젝트 정산 예정 총 금액 |
| `plannedAmount` | Long | Y | 회차별 정산 예정 금액 |
| `plannedTaxAmount` | Long | Y | 회차별 정산 예정 세금 금액 |
| `plannedDate` | LocalDate | Y | 회차별 정산 예정일 |
| `traderName` | String | Y | 거래처명(입금자명) |
| `bankName` | String | N | `OUTCOME` 타입인 경우만 필수. 외주 업체 은행명 |
| `accountNumber` | String | N | `OUTCOME` 타입인 경우만 필수. 외주 업체 계좌번호(하이픈·띄어쓰기 없이) |
| `accountHolder` | String | N | `OUTCOME` 타입인 경우만 필수. 외주 업체 예금주 |
| `version` | Integer | Y | 블록 목록 조회에서 받은 version 그대로. 2026-08-11 낙관적 락 추가(`.ai/docs/global/CONCURRENCY.md`) |
| `overwrite` | Boolean | N | true면 충돌 무시하고 덮어씀. 생략 시 false. 2026-08-11 추가 |

**Request Example**

```json
{
  "roundNo": 1,
  "totalAmount": 4500000,
  "plannedAmount": 1500000,
  "plannedTaxAmount": 200000,
  "plannedDate": "2026-09-01",
  "traderName": "(주)대한항공",
  "version": 1
}
```

```json
{
  "roundNo": 1,
  "totalAmount": 4500000,
  "plannedAmount": 1500000,
  "plannedTaxAmount": 200000,
  "plannedDate": "2026-09-01",
  "traderName": "(주)대한항공",
  "bankName": "신한은행",
  "accountNumber": "100555074444",
  "accountHolder": "홍길동",
  "version": 1
}
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.settleId` | Long | 정산 블록 ID |
| `data.roundNo` | Int | 정산 회차 |
| `data.totalAmount` | Long | 프로젝트 정산 예정 총 금액 |
| `data.plannedAmount` | Long | 회차별 정산 예정 금액 |
| `data.plannedTaxAmount` | Long | 회차별 정산 예정 세금 금액 |
| `data.plannedDate` | LocalDate | 회차별 정산 예정일 |
| `data.traderName` | String | 거래처명(입금자명) |
| `data.bankName` | String | `OUTCOME` 타입인 경우만 값 있음. 외주 업체 은행명 (nullable) |
| `data.accountNumber` | String | `OUTCOME` 타입인 경우만 값 있음. 앞·뒤 3자리만 남기고 마스킹 (nullable) |
| `data.accountHolder` | String | `OUTCOME` 타입인 경우만 값 있음. 외주 업체 예금주 (nullable) |
| `data.actualAmount` | Long | 재무팀에서 입력할 실제 입출금 금액 (초기 null) |
| `data.actualDate` | LocalDateTime | 재무팀에서 입력할 실제 입출금 시간 (초기 null) |
| `data.status` | String | 정산 상태: `PENDING`(미연결) \| `WAITING`(정산 대기) \| `PARTIAL`(부분 정산) \| `COMPLETED`(정산 완료) |
| `data.paidAmountRatio` | Double | 금액 기준 진행률 — 이 블록 하나가 아니라 **같은 프로젝트·같은 타입**(INCOME/OUTCOME) 정산 블록 전체의 실제 금액 합계를 이 타입의 프로젝트 총 예정 금액(`totalAmount`)으로 나눈 값. INCOME 블록은 입금 진행률, OUTCOME 블록은 외주 출금 진행률을 뜻한다 |
| `data.createdAt` | LocalDateTime | 정산 블록에 내용이 생성된 일시 |
| `data.version` | int | 수정 후 버전(기존값+1). 2026-08-11 추가 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "정산 항목 작성/수정 성공",
  "data": {
    "settleId": 1,
    "roundNo": 1,
    "totalAmount": 4500000,
    "plannedAmount": 1500000,
    "plannedTaxAmount": 200000,
    "plannedDate": "2026-09-01",
    "traderName": "(주)대한항공",
    "bankName": null,
    "accountNumber": null,
    "accountHolder": null,
    "actualAmount": null,
    "actualDate": null,
    "status": "PENDING",
    "paidAmountRatio": 0.0,
    "createdAt": "2026-08-07T17:00:00",
    "version": 2
  }
}
```

```json
{
  "httpStatus": 200,
  "message": "정산 항목 작성/수정 성공",
  "data": {
    "settleId": 1,
    "roundNo": 1,
    "totalAmount": 4500000,
    "plannedAmount": 1500000,
    "plannedTaxAmount": 200000,
    "plannedDate": "2026-09-01",
    "traderName": "(주)대한항공",
    "bankName": "신한은행",
    "accountNumber": "100******444",
    "accountHolder": "홍길동",
    "actualAmount": null,
    "actualDate": null,
    "status": "PENDING",
    "paidAmountRatio": 0.0,
    "createdAt": "2026-08-07T17:00:00",
    "version": 2
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "정산 항목 작성/수정 성공" |
| 400 | Bad Request | `SETL-005` | "정산 블록의 타입 지정은 필수입니다." (`type` 쿼리파라미터 누락/오값) |
| 400 | Bad Request | `SETL-003` | "내용을 입력해 주세요." (공통 필수 필드 누락) |
| 400 | Bad Request | `SETL-004` | "출금 타입은 계좌정보가 필수입니다." (`OUTCOME`인데 계좌정보 누락) |
| 400 | Bad Request | `SETL-011` | "회차 번호는 1 이상이어야 합니다." (`roundNo <= 0`) |
| 400 | Bad Request | `SETTLEMENT_VERSION_REQUIRED` | "버전 정보가 없습니다. 화면을 새로고침해 주세요." (2026-08-11 추가) |
| 403 | Forbidden | `SETL-001` | "편집 권한이 없습니다." |
| 404 | Not Found | `SETL-002` | "존재하지 않는 블록입니다." |
| 409 | Conflict | `SETL-006` | "출금(OUTCOME)에서 입금(INCOME)으로는 타입을 변경할 수 없습니다." (OUTCOME → INCOME 다운그레이드 시도) |
| 409 | Conflict | `SETL-007` | "세금계산서 또는 입출금 내역이 연결되어 있어 수정할 수 없습니다." (`status != PENDING`) |
| 409 | Conflict | `SETL-008` | "같은 프로젝트의 다른 정산 블록과 총 예정 금액이 일치하지 않습니다. (기존 등록된 금액: N원)" (같은 프로젝트·같은 타입의 다른 회차가 이미 정해둔 `totalAmount`와 다름) |
| 409 | Conflict | `SETTLEMENT_VERSION_CONFLICT` | "다른 사용자가 먼저 수정했습니다." (2026-08-11 추가 — 낙관적 락. `SETL-007`과 판정 순서: 삭제→상태(연결)→버전 순으로 확인) |

---

## 구현 메모 — 낙관적 락 (2026-08-11)

`.ai/docs/global/CONCURRENCY.md` 팀 표준 반영 — `settlement_block` 테이블에 `version` 컬럼 추가
(`V20260812150000__add_version_settlement.sql` — ⚠️ 문서 §7-1 표는 `V20260811150000`을 배정했으나
계속 재배정됨(150000 → 170000 → 170100 → 110000/0812 → 120000/0812 → 150000/0812) — CI "마이그레이션
검증"이 기준 브랜치 최대 버전보다 커야 한다고 요구하는데, 다른 PR들이 그 최대 버전을 계속 올려서 매번
따라잡힘. 문서 수정 권한이 없어 이 사실은 마이그레이션 파일 주석에만 남김 — 김동현님께 §7-2 번호 배분
방식 자체 재검토 요청 필요(반복되는 패턴이라 개별 재배정으로는 안 끝날 수 있음)).

기존에도 `status = PENDING` 조건부 UPDATE(세금계산서/입출금 연결 시 잠금)가 있었는데, 여기에
`version = ?` 조건을 추가로 걸었다. 원인을 3단계로 구분한다: ① 삭제됨 → `SETL-002` 404,
② 상태가 PENDING을 벗어남(연결됨) → `SETL-007` 409, ③ 그 외(버전 불일치) →
`SETTLEMENT_VERSION_CONFLICT` 409. **①·②는 조건부 UPDATE를 실행하기 전에, 잠금(`FOR UPDATE`) 하에
다시 읽은 현재 상태로 먼저 판정한다**(아래 CodeRabbit 2번 항목 참고 — 갱신 실패 후 재조회하는 방식은
REPEATABLE READ 스냅샷 때문에 부정확할 수 있어서 순서를 바꿨다) — 그래서 조건부 UPDATE 자체가 0행이면
남는 원인은 ③뿐이다. 블록 목록 조회(프로젝트 상세의 블록 카드)의 `SETTLEMENT` 상세에도 `version`이
함께 내려간다(§5-1 "목록도" 규칙).

**CodeRabbit 리뷰 반영 (2026-08-12, 낙관적 락 PR)**:

1. **`overwrite=true`가 잠금 이전 버전을 기대값으로 쓰던 문제 — 반영.**
2. **0행 갱신 시 원인 분류(`SettlementRepositoryAdapter.updateItem`)가 REPEATABLE READ 스냅샷 때문에 부정확할 수
   있다는 지적 — 3차 재지적 끝에 근본 반영(2026-08-12).** 앞선 두 라운드는 "영향이 메시지 정확도뿐"이라며
   반영 안 함으로 넘겼는데, 3차 리뷰가 "이 오류 코드는 API 계약이라 메시지 정확도만의 문제가 아니다"라고
   재반박했고 — 맞는 말이다. `SETTLEMENT_VERSION_CONFLICT`(재조회 없이 `overwrite`로 덮어쓰기 가능)와
   `SETL-007`/`SETL-002`(막혀있음, 재조회해도 소용없음)는 프론트 처리 분기가 다르니 코드 자체가 계약이다.
   **"실패 후 재분류" 대신 "쓰기 전에 잠금 하에 미리 확정"으로 근본 해결**했다 — 마침 1번 수정으로 이미
   만든 `FOR UPDATE` 잠금 재확인 인프라를 그대로 확장하면 되어 비용도 낮아졌다:
   - `SettlementSiblingLookupPort.findCurrentVersionForUpdate`(버전만 반환)를 **`findCurrentStateForUpdate`로
     교체** — 이제 `version`/`status`/`deletedAt` 셋 다 `FOR UPDATE`로 함께 반환한다.
   - `SettlementCommandService.upsertItem`이 `lockSiblingSettlementBlocksForUpdate` 직후(= 이 행이 이미
     잠긴 시점) 이 조회를 **overwrite 여부와 무관하게 항상** 호출해서, 삭제됨(`SETL-002`)·연결됨(`SETL-007`)을
     여기서 먼저 판정한다. 이 판정 이후로는 트랜잭션이 끝날 때까지 이 행을 아무도 못 바꾼다(같은 잠금이
     계속 유지됨).
   - 그래서 `SettlementRepositoryAdapter.updateItem`의 조건부 UPDATE가 0건이 되는 원인은 **이제 버전
     불일치뿐임이 보장**된다 — 뒤따르던 일반 `findById` 재분류 코드 자체를 제거하고 바로
     `SETTLEMENT_VERSION_CONFLICT`를 던지도록 단순화했다.
   - 부수 효과: overwrite 경로도 더 견고해졌다 — 1번에서 만든 잠금-재조회가 이제 판정과 버전 조회를 한 번에
     하므로, "판정은 프론트 사이드가 아니라 항상 최신 커밋 상태 기준"이라는 게 명확해졌다.
3. **`GET /api/v1/projects/{projectId}/settlements` 응답에 `data.blocks[].version`이 없다는 지적(라인 403의
   "블록 목록 조회에서 받은 version"과의 정합성) — 반영 안 함, 다른 엔드포인트를 가리킨 것으로 판단.** 라인
   403의 "블록 목록 조회"는 `GET /api/v1/steps/{stepId}/blocks`(블록 카드, `SettlementDetail`이 `version` 포함)를
   가리킨다 — PATCH가 실제로 호출되는 화면(프로젝트 상세)이 거기다. 이 엔드포인트(정산현황 화면의 읽기 전용
   드릴다운)는 PATCH에 필수인 `traderName`조차 응답에 없다 — 애초에 여기서 받은 값으로 PATCH를 채울 수 있는
   구조가 아니다. `version`만 추가해도 편집이 안 되는 응답이라 의미가 없어서 반영 안 함.
4. **request.http의 SETL-003~011 시나리오 전부가 version 컬럼 추가 이후 400 SETTLEMENT_VERSION_REQUIRED로만
   막힌다는 지적 — 반영.** 각 PATCH에 실제 누적 버전값을 채워 넣었다(버전 검사보다 먼저 걸리는 검증들은 정확한
   값이 굳이 필요 없지만, 파일을 읽는 사람이 헷갈리지 않도록 실제 값을 그대로 넣음). `overwrite=true` 시나리오
   (10-7)도 대상 settleId·totalAmount가 10-6과 어긋나 있던 걸 맞췄고, 로그인 계정 안내 문구(EMP003 고정 서술)도
   맨 위 로그인 계정을 가리키도록 정정.
5. **타입 다운그레이드 판정·활동 로그 이전값 비교가 여전히 잠금 이전 `before`를 쓴다는 지적(2번 항목의 후속,
   2026-08-12 2차) — 반영.** 2번에서 `findCurrentStateForUpdate`를 만들 때 `version`/`status`/`deletedAt`
   3개만 반환해서, 그 사이 남이 타입/내용을 바꿔도 `assertNoTypeDowngrade`와 `detectChanges`(활동 로그)는
   여전히 잠금 이전 값 기준으로 판정·기록했다 — 진짜 놓친 지점이었다. `findCurrentStateForUpdate`가
   `type`/`roundNo`/금액류/`traderName`/`bankName`/`accountNumber`/`accountHolder`까지 같은 조회로 함께
   반환하도록 확장했고, 타입 다운그레이드 판정은 이 값 기준으로 잠금 후로 옮겼다(잠금 전 얕은 검사는
   제거 — 옛 값 기준으로 오탐/누락 둘 다 가능해서 얕은 버전을 남겨둘 이유가 없었다). 활동 로그 이전값
   비교(`detectChanges`)도 이제 이 값을 쓴다.

⚠️ 위 1번 수정은 **overwrite=true 경로에서만** 동작이 바뀐다. overwrite=false(일반 저장)는 여전히 클라이언트가
보낸 `version`을 그대로 검증하므로 영향 없다.

## 구현 메모 (사람이 확인할 것)

- **원 명세와 다르게 처리한 것 — 반드시 확인**:
  1. **HTTP 상태 코드 200 vs 201 불일치** — 사용자가 준 Status Code 표는 `201 Created`였지만, 같이 준 Success Example
     JSON은 둘 다 `"httpStatus": 200`이었다. PATCH 의미상, 그리고 JSON 예시 쪽을 신뢰해 **200으로 구현**했다.
     201이 맞다면 `SettlementController`의 `ResponseEntity.status(...)`와 `ApiResponse.success→created` 한 줄만 바꾸면 된다.
  2. **`settlement_block.block_id`를 `NOT NULL`로 강제** — 원 DDL은 `NULL`이었지만, 팀 컨벤션(`BLOCK.md` 다형성 규약 3)이
     상세 테이블의 `block_id`를 항상 `NOT NULL + UNIQUE`로 요구한다. 작업자 확인 후 `NOT NULL`로 확정.
  3. **`cash_flow.linked_by`/`tax_invoice.linked_by`를 `NULL` 허용으로 변경** — 원 DDL은 `NOT NULL`이었지만, 두 값 모두
     "정산 블록에 연결됐을 때"만 채워지는 값이라(연결 전엔 주인이 없음) `NOT NULL`이면 최초 수집 시점에 INSERT 자체가 불가능하다.
     기존 `payment.linked_by`/`tax_invoice_confirm.linked_by` 설계와 동일하게 `NULL` 허용으로 맞췄다. ⚠️ **이건 의도된
     예외다** — `linked_by`/`linked_at`을 제외한 나머지 "(필수)" 표시 컬럼들은 최초 구현 때 실수로 전부 `NULL`로
     나갔던 버그였고(2026-08-09, CodeRabbit 리뷰로 발견), 원래 설계대로 `NOT NULL`로 되돌렸다. 이 과정에서
     `uk_cash_flow_dedup UNIQUE(bank_name, traded_at, amount)`도 세 컬럼이 `NOT NULL`이 되면서 MySQL이 NULL을
     서로 다른 값으로 취급해 중복을 못 막던 문제가 같이 해결됐다.
  4. **`settlement_block.status` 기본값** — 설명에 "기본값 PENDING"이 있어 컬럼을 `NOT NULL DEFAULT 'PENDING'`으로 뒀다
     (원 DDL은 `NULL`로 선언돼 있었음).
- **기존 실제 테이블과의 충돌** — `payment`/`tax_invoice`/`block_payment_confirm`/`tax_invoice_confirm`은 실제로
  `init` 마이그레이션(`V202608031739`)에 이미 존재했다(사용자가 "삭제해달라"고 한 4개와 정확히 일치). `tax_invoice_confirm`이
  `tax_invoice.tax_invoice_id`를 FK로 참조하고 있어서 새 마이그레이션에서 **자식(tax_invoice_confirm) → 부모(tax_invoice)**
  순서로 DROP했다. 새 `tax_invoice` 테이블은 **이름은 같지만 스키마가 완전히 다르다** (구 스키마는 `project_id`/`matched_by`
  FK가 있었고, 신규 스키마는 `settle_block_id`만 있다).
- **계좌번호 암호화** — `settlement/infrastructure/security/AccountNumberCipher`(AES/GCM)로 저장 전 암호화한다.
  키는 `SETTLEMENT_ACCOUNT_ENC_KEY` 환경변수(Base64 인코딩된 32바이트 AES-256 키)로 주입한다 — **로컬에서 이 값을
  설정하지 않으면 앱이 기동하지 않는다.** 응답의 마스킹(`100******444`)은 이 요청에서 받은 평문 값을 그대로 마스킹한
  것이지, 저장된 값을 복호화해서 만든 게 아니다(이 API가 쓰기 전용이라 복호화 로직 자체가 없다).
- **편집 권한(403) 검사** — `SettlementEligibilityPolicy.assertEditPermission`이 `BlockCatalogPort.hasEditPermission
  ("SETTLEMENT", settleId, userId, role)`을 호출한다(text/checklist와 동일한 공유 포트 재사용). `BlockType.SETTLEMENT`가
  2026-08-09에 추가돼 이제 정상 동작한다.
- **응답 필드 null 표기** — `INCOME` 타입 응답에도 `bankName`/`accountNumber`/`accountHolder` 키 자체는 내려간다
  (값이 `null`). 팀 공통 `ApiResponse` 관례(다른 도메인도 미사용 필드를 `null`로 명시)를 따랐다.
- **`paidAmountRatio` 계산 규칙 (2026-08-09 재설계)** — 처음엔 "이 블록의 `actualAmount / plannedAmount`"로 잘못 구현했었다.
  실제로는 **프로젝트 단위** 지표다: `block_id → block.step_id → step.project_id`를 타고 가서, **같은 프로젝트·같은
  타입**(INCOME/OUTCOME)의 활성 정산 블록 전체에 걸친 `actual_amount` 합계를 그 타입의 `total_amount`(프로젝트 총
  예정 금액)로 나눈다. INCOME 블록은 입금 진행률, OUTCOME 블록은 외주 출금 진행률을 보여준다 — 두 방향의 돈을 섞지 않는다.
  이 합계는 소비자별로 별도 쿼리로 계산한다(2026-08-09 매퍼 분리 이후) — 블록 목록 조회
  (`SettlementBlockDetailAdapter`)는 `SettlementDetailMapper.findBySettleIds`(배치, JOIN+GROUP BY 파생
  테이블)를, 이 PATCH 응답(`SettlementCommandService`)은 `SettlementSiblingLookupPort.findActualAmountSum`
  (`SettlementSiblingMapper`, 단건 전용 쿼리)을 쓴다. 쿼리는 다르지만 **같은 계산식**(`SettlementProgress.ratio`)을
  재사용한다 — 계산 공식이 두 곳에서 갈리지 않게.
- **수정 제약 2건 (2026-08-09 추가)**:
  1. **타입 다운그레이드 금지(`SETL-006`)** — `OUTCOME → INCOME`은 막는다. OUTCOME 전용 필드(계좌정보)를 버려야 하는
     손실성 변경이기 때문. 반대로 `INCOME → OUTCOME`(계좌정보를 새로 받기만 하면 됨)과 최초 작성은 허용한다.
  2. **연결되면 수정 불가(`SETL-007`)** — `settlement_block.status`가 `PENDING`이 아니면(세금계산서·입출금 내역이 연결되면)
     PATCH 자체를 막는다. 지금은 상태를 `PENDING` 밖으로 바꾸는 연결 API가 아직 없어서 이 코드가 실제로 나갈 일은 없다 —
     연결 API가 붙을 때를 대비한 선제 방어다.
  3. **프로젝트 내 총 예정 금액 일관성(`SETL-008`, 2026-08-09 추가, 같은 날 우선순위 고도화)** — `totalAmount`는
     타입(INCOME/OUTCOME)별로 한 프로젝트 안의 모든 회차가 같은 값이어야 한다(그래야 `paidAmountRatio` 계산이
     성립한다 — "회차마다 총 금액이 같다"는 전제로 프로젝트+타입 단위 합산을 나누고 있음). 같은 프로젝트·같은 타입의
     **다른** 회차가 이미 값을 정해뒀는데 이번 요청이 다른 값을 보내면 막는다. **비교 기준값이 여럿이면 이미
     연결된(`status != PENDING`) 회차를 최우선으로 쓴다** — 연결된 회차는 더 이상 안 바뀌는 진짜 확정값이고,
     아직 `PENDING`인 다른 회차는 이 검증이 생기기 전에 잘못 들어간 값일 수 있어 후순위다. 연결된 회차가 하나도
     없으면 `PENDING` 중 아무 값이나(먼저 만들어진 순) 기준으로 쓴다. 아직 아무 회차도 값을 안 정했으면(전부 null)
     이번 요청이 그 프로젝트의 첫 기준값이 되므로 통과한다. `SettlementSiblingLookupPort.findEstablishedTotalAmount`
     (`SettlementSiblingMapper`, `ORDER BY (status != 'PENDING') DESC` 로 우선순위, 쿼리 1발, 2026-08-09 매퍼
     분리 이후 이 경로)로 확인한다. 메시지에 기존 등록된 금액을 담아 사용자가 바로 어떤 값으로 맞춰야 하는지
     알 수 있게 했다.
     ⚠️ 나중에 "회차·총 금액 추천" API(프로젝트의 기존 정산 블록을 조회해 다음 회차 번호·총 금액을 미리 채워주는 기능)가
     붙으면 이 검증과 값 출처가 같아진다 — 별도 설계 시 이 메서드를 재사용할 수 있는지 검토할 것.
- **블록 껍데기 생성/삭제/조회 (2026-08-09 추가)** — text 도메인을 그대로 참고해 구현했다.
  - 생성: `SettlementBlockDetailAdapter.createDetail` → `SettlementHandlerService.create` → `SettlementRepository.create`
    (JPA `save()`로 `settlement_block` 빈 행 INSERT, `status=PENDING`).
  - 삭제: `SettlementBlockDetailAdapter.deleteDetail` → `SettlementHandlerService.delete` — 조건부 UPDATE로
    멱등 처리(이미 삭제된 행이면 무시), `deletedAt` null 방어 포함. 블록 자체의 생성/삭제 활동 로그는 text·checklist와
    동일한 이유로 여기서 발행하지 않는다(Block 도메인 책임).
  - 조회: `SettlementDetailMapper`(MyBatis, `resources/mapper/settlement/SettlementDetailMapper.xml`)로
    `settle_id IN (...)` 배치 조회 → `SettlementDetail`로 매핑. `accountNumber`는 `AccountNumberCipher.decryptAndMask`로
    복호화 직후 바로 마스킹해서 담는다(복호화 원문이 어댑터 밖으로 나가지 않는다).
- **활동 로그(수정) — 2026-08-09 추가** — `SettlementCommandService.upsertItem`이 text와 동일하게 "실제로 바뀐 필드가
  있을 때만" `MODIFY` 이벤트를 발행한다. `roundNo`/`type`/`totalAmount`/`plannedAmount`/`plannedTaxAmount`/`plannedDate`/
  `traderName`/`bankName`/`accountHolder`는 실제 값으로 비교·기록하고, `accountNumber`만 예외로 **양쪽 다 마스킹된 값으로
  비교·기록한다** (이전 값은 저장된 암호문을 복호화 후 마스킹, 이후 값은 이번 요청 평문을 마스킹) — 활동 로그에
  계좌번호 원문이 절대 남지 않게 하기 위함이다. `resourceName`은 `traderName`을 쓴다.
- **CodeRabbit 리뷰 반영 (2026-08-09)**:
  1. **`roundNo <= 0` 검증 추가, 금액은 그대로 둠** — `validateRequiredFields`에 회차 번호 양수 검증을 추가했다.
     `totalAmount`/`plannedAmount`/`plannedTaxAmount`엔 음수 금지를 안 걸었다 — 은행 CSV/API 수집 양식에 따라
     `OUTCOME` 거래가 음수로 표기되는 경우가 있어서, 여기서 부호를 강제하면 실제 데이터를 못 받는다(작업자 확인).
     여전히 **Bean Validation은 안 쓴다** — `@Valid`를 붙이면 실패 시 `code`가 `COMMON_INVALID_REQUEST`로 뭉개져
     계약이 깨진다(2026-08-04 팀 결정과 동일 이유). 서비스 내부 수동 검증으로 처리했다.
     ⚠️ **처음엔 `SETL-003`("내용을 입력해 주세요")을 재사용했으나 신규 코드 `SETL-011`("회차 번호는 1 이상이어야
     합니다.")로 분리했다** — 값을 아예 안 넣은 것(필드 누락)과 값은 넣었는데 범위가 틀린 것은 사용자에게 다른
     메시지로 보여야 한다. `SETL-003`을 그대로 쓰면 "분명히 입력했는데 왜 내용을 입력하라는 거냐"는 오해를 준다
     (사용자 피드백으로 발견). 이 도메인은 시나리오마다 코드를 따로 두는 기존 패턴(`SETL-004`/`SETL-005`)과도
     맞다.
  2. **SETL-008 검증 직전에 같은 프로젝트의 정산 블록 전체를 `FOR UPDATE`로 잠금** —
     `SettlementDetailMapper.lockSiblingSettlementBlocksForUpdate` 신설. 서로 다른(둘 다 빈) 정산 블록을 동시에
     PATCH하면 둘 다 `findEstablishedTotalAmount`에서 "기준값 없음"으로 읽고 각자 다른 `totalAmount`를 저장할 수
     있었던 레이스(체크 후 쓰기 사이의 틈)를 막는다. 이 블록의 행 자체는 블록 생성 시점에 이미 만들어져 있어(내용은
     비어 있어도 행은 존재) 최초 회차 케이스도 이 잠금으로 커버된다. 스키마 변경(별도 기준값 테이블+유니크 키) 없이
     기존 행을 잠그는 가벼운 방식으로 처리했다.
     ⚠️ **1차 반영이 불완전했다 (2026-08-09, 같은 날 CodeRabbit 2차 리뷰로 발견) — `findEstablishedTotalAmount`에도
     `FOR UPDATE`를 추가로 걸었다.** MySQL InnoDB REPEATABLE READ에서는 일반 SELECT가 "이 트랜잭션의 첫 읽기
     시점 스냅샷"을 계속 쓴다. `lockSiblingSettlementBlocksForUpdate`가 잠그고 상대 트랜잭션의 커밋을 기다려도,
     그 뒤에 실행되는 `findEstablishedTotalAmount`가 평범한 SELECT면 그 커밋 이후 값이 아니라 스냅샷(옛 값)을 볼
     수 있어 레이스가 그대로 남아 있었다. 두 쿼리 다 `FOR UPDATE`(현재 읽기)여야 최신 커밋값을 보장한다.
  3. **`AccountNumberCipher` 생성자에서 키 길이 검증** — `SETTLEMENT_ACCOUNT_ENC_KEY`를 Base64 디코드한 값이 정확히
     32바이트(AES-256)가 아니면 기동 시점에 `IllegalStateException`을 던진다. 이전엔 16/24바이트를 넣어도 조용히
     AES-128/192로 동작했다.
  4. **`SettlementDetailMapper.findBySettleIds`의 `agg` 서브쿼리를 요청받은 settleIds의 project_id로 제한** —
     기존엔 `settlement_block` 테이블 전체를 project_id·type으로 집계했다(관계없는 프로젝트까지 매번 스캔).
     블록 목록을 불러올 때마다 도는 조회라 데이터가 쌓이면 느려질 수 있었다. 앱→DB 왕복 횟수는 그대로 1번이고,
     SQL 안에 스캔 범위를 좁히는 서브쿼리만 추가했다.
  5. `PagePermissionMapperAdapter`가 `MEMBER` 외 role도 권한 행이 있으면 통과시킬 수 있다는 지적은 **반영 안 함** —
     `ADMIN`/`MASTER`는 이 메서드 맨 앞에서 이미 무조건 `true`를 반환해 `existsGrant` 자체를 안 보고, 이 시스템에서
     `role` 값은 `ADMIN`/`MASTER`/`MEMBER`/빈 문자열뿐이라(`RequesterRole.from`) 실제로 뚫릴 경로가 없다. `permission`
     컬럼(`VIEWER`/`EDITOR`)을 지금 안 쓰는 것도 지적됐는데, FINANCE로 막힌 3개 API가 전부 조회(GET)라 구분할 필요가
     아직 없다 — 재무팀이 "쓰는" 화면이 생기면 그때 설계.
  6. **마이그레이션이 기존 데이터 보존 없이 DROP한다는 지적** — 이 4테이블은 사용자가 직접 "삭제해달라"고 확정한
     것이고(위 "기존 실제 테이블과의 충돌" 참고) 프로젝트가 아직 초기 세팅 단계라 지킬 운영 데이터가 없다고 판단해
     반영 안 함.
  7. **목록 조회 2개(정산현황 프로젝트/블록 조회)에 페이지네이션 명세가 없다는 지적** — 원 명세 자체에 페이징이 없어서
     지금 넣으면 프론트 계약을 새로 만드는 일이 된다. 필요해지면 명세부터 정하고 오는 게 맞다고 보고 반영 안 함.
  8. **`request.http`가 응답으로 받은 ID 대신 `1`/`2`/`3` 등을 그대로 써서 실제 auto-increment 값과 다르면 다른
     프로젝트/블록을 건드릴 수 있다는 지적 — 반영 안 함.** 로컬 테스트용 시나리오 파일이고 각 요청 위에 "URL의 N을
     실제 응답 ID로 교체할 것"이라는 주석이 이미 있어 수동 테스트 시 그렇게 쓰는 걸 전제로 한다. HTTP 클라이언트
     변수로 자동화하는 건 이 파일의 성격(사람이 순서대로 실행하며 읽는 시나리오 로그)과 안 맞다고 판단.
  9. **`Cache-Control: no-store` 통합 테스트, `getRecommendation` 캐시 헤더 검증 테스트 추가 제안 — 반영 안 함.**
     이 프로젝트는 테스트 0개 기조라(AGENTS.md 알려진 이슈) 이 PR에서만 기준을 올리지 않기로 함. 동작 자체는
     `SettlementController.getRecommendation`에서 `ResponseEntity.ok().cacheControl(CacheControl.noStore())`로
     이미 처리돼 있다.
  10. **request.http `roundNo: -2` 버그, `type` 파라미터 OpenAPI `required` 미표시, 음수 금액 테스트 시나리오가
      다른 검증에 걸려 실제로 검증이 안 되던 문제 — 전부 반영.** `roundNo`는 유효값으로, `@Parameter(required = true)`
      추가(`@RequestParam(required = false)`는 `SETL-005` 처리를 위해 그대로 둠), 음수 금액 테스트는 `roundNo=1`·
      기존에 확정된 `totalAmount`를 쓰고 `plannedTaxAmount`만 음수로 바꾸는 별도 요청으로 분리.
  11. **SETL-008 검증의 잠금 순서 문제 — 반영.** `assertModifiable`이 락 걸기 전에 읽은 `before.status`로 판단해서,
      상태를 PENDING 밖으로 바꾸는 연결 API가 생기면 두 PATCH가 동시에 PENDING을 읽고 그중 하나가 먼저
      연결돼도 나머지가 SETL-007을 우회할 수 있었다(지금은 그 연결 API가 없어서 도달 불가능한 경로지만
      선제 방어). `deleted_at IS NULL`과 동일한 패턴으로 UPDATE 문 자체에 `status = 'PENDING'` 조건을 추가
      (`SpringDataSettlementRepository.updateItemIfActive`). 0건이 됐을 때 원인(삭제 vs 상태 변경)을 구분해서
      `SETL-002`/`SETL-007`을 각각 던진다(`SettlementRepositoryAdapter`).
  12. **`application`이 `infrastructure`(MyBatis 매퍼·Row 타입)를 직접 참조 — 반영.** `SettlementCommandService`/
      `SettlementQueryService`가 `SettlementDetailMapper`의 일부 메서드(`findEstablishedTotalAmount`·
      `lockSiblingSettlementBlocksForUpdate`·`findRecommendation`)를 직접 호출하던 것을 정리했다.
      - **매퍼를 목적별로 나눴다** — `SettlementDetailMapper`(공용 블록 상세 조회, `SettlementBlockDetailAdapter`
        전용, `findBySettleIds`만 남음)와 `SettlementSiblingMapper`(정산 도메인 자기 자신의 비즈니스 로직 전용,
        신규)로 분리했다. 한 매퍼를 성격이 다른 두 소비자가 나눠 쓰던 것을 정리한 것 — SQL 자체는 안 바뀌었다.
      - `application/port/SettlementSiblingLookupPort`(신규) + `infrastructure/blockdetail/
        SettlementSiblingLookupAdapter`(신규, `SettlementSiblingMapper`를 감쌈)로 Command/Query 서비스가
        더 이상 매퍼·Row 타입을 직접 모른다.
      - `findActualAmountSum`은 기존에 `findBySettleIds`(배치 조회용 메서드)를 재사용해 필드 하나만 꺼내 쓰던
        방식을 버리고, `SettlementSiblingMapper`에 전용 쿼리를 새로 만들었다(단일 값만 필요한데 배치 조회
        메서드를 억지로 갖다 쓰지 않게).
      - `AccountNumberCipher`는 그대로 직접 참조로 둔다 — 시그니처가 순수 String이라 infra 세부사항이 안
        새고, 포트로 감싸는 게 실익이 거의 없다고 판단(둘 다 공통으로 쓰는 것 자체는 문제가 아니라는 점도 확인).
