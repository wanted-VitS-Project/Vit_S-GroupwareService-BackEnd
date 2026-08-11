# 입찰 관리 API 명세

**노션 원본**: 사용자 제공 노션 정리본 (링크 미제공)
**최종 동기화**: 2026-08-11 (수집 조건별 자동 수집 주기·시각 계약 확정)
**도메인 담당**: 정현

> 상태가 `✅ 확정` 이상인 항목은 프론트와의 계약이다. 임의 변경 금지.
> 현재 문서는 노션 정리본을 로컬에 옮긴 **초안**이다. 팀 합의와 프론트 공유가 끝나면 상태를 `✅ 확정`으로 바꾼다.

---

## 엔드포인트 목록

| 상태 | 기능 | METHOD | URL | 권한 |
|------|------|--------|-----|------|
| ✅ 확정 | 수집 조건 목록 조회 | GET | `/api/v1/bidding/collection-conditions` | `BIDDING` |
| ✅ 확정 | 수집 조건 등록 | POST | `/api/v1/bidding/collection-conditions` | `BIDDING` |
| ✅ 확정 | 수집 조건 수정 | PATCH | `/api/v1/bidding/collection-conditions/{conditionId}` | `BIDDING` |
| ✅ 확정 | 입찰 공고 수집 실행 | POST | `/api/v1/bidding/collection-conditions/{conditionId}/runs` | `BIDDING` |
| ✅ 확정 | 수집 실행 결과 조회 | GET | `/api/v1/bidding/collection-runs/{runId}` | `BIDDING` |
| ✅ 확정 | 입찰 공고 목록 조회 | GET | `/api/v1/bidding/notices` | `BIDDING` |
| ✅ 확정 | 입찰 공고 상세 조회 | GET | `/api/v1/bidding/notices/{noticeId}` | `BIDDING` |
| ✅ 확정 | 입찰 공고 직접 등록 | POST | `/api/v1/bidding/notices` | `BIDDING` |
| ✅ 확정 | 직접 등록 공고 수정 | PATCH | `/api/v1/bidding/notices/{noticeId}` | `BIDDING` |
| 📝 초안 | 공고 제외 | PATCH | `/api/v1/bidding/notices/{noticeId}/dismiss` | `BIDDING` |
| 📝 초안 | 공고 복구 | PATCH | `/api/v1/bidding/notices/{noticeId}/restore` | `BIDDING` |
| 📝 초안 | 입찰 AI 요약 요청 | POST | `/api/v1/bidding/notices/{noticeId}/summaries` | `BIDDING` |
| 📝 초안 | 입찰 AI 요약 조회 | GET | `/api/v1/bidding/summaries/{summaryId}` | `BIDDING` |
| 📝 초안 | 입찰 AI 요약 수정 | PATCH | `/api/v1/bidding/summaries/{summaryId}` | `BIDDING` |
| 📝 초안 | 입찰 AI 요약 확정 | PATCH | `/api/v1/bidding/summaries/{summaryId}/confirm` | `BIDDING` |
| 📝 초안 | 공고 프로젝트 전환 | POST | `/api/v1/bidding/notices/{noticeId}/projects` | `BIDDING` |

---

## 입찰 공고 프로젝트 전환 정책

v1에서는 입찰용 블록과 프로젝트 공고 스냅샷을 사용하지 않는다.
입찰 공고에서 프로젝트를 만들 때는 `project.bid_notice_id`만 저장한다.

```text
bid_notice = 입찰 공고 원본
project = bid_notice_id 링크만 보유
```

정책:

| 항목 | 규칙 |
|------|------|
| 블록 생성 | 생성하지 않는다 |
| 수동 생성 | 제공하지 않는다 |
| 프로젝트 연결 | `project.bid_notice_id`로 전환한 공고 원본과 연결한다 |
| 입찰 상세 조회 | 입찰 공고 API 또는 `project.bid_notice_id` 기준 `bid_notice` 조회로 처리한다 |
| 공고 원본 수정 | 입찰 공고 API 정책을 따른다 |
| 프로젝트 스냅샷 | 저장하지 않는다 |
| 정정공고 변경 감지 | v1 범위 밖이다 |
| 블록 제목·위치 수정 | 대상 없음 |
| 블록 삭제 | 대상 없음 |

입찰 블록 전용 API는 만들지 않는다.
프로젝트 공고 변경 비교와 스냅샷 반영 API도 v1에서는 만들지 않는다.

금지 API:

```text
GET /api/v1/blocks/{blockId}/bid-notice
PUT /api/v1/blocks/{blockId}/bid-notice
PATCH /api/v1/blocks/{blockId}/bid-notice
DELETE /api/v1/blocks/{blockId}/bid-notice
GET /api/v1/projects/{projectId}/bid-notice/changes
PATCH /api/v1/projects/{projectId}/bid-notice-snapshot
```

---

## 수집 조건

### 공통 정책

**상태**: ✅ 확정

사용자는 임의 URL을 등록하지 않고 시스템이 지원하는 수집처를 선택한다.
공개 API에서는 실제 동작에 맞게 `collection` 용어를 사용하고, 기존 DB의 `crawl_*` 테이블명은 변경하지 않는다.

| 항목 | 규칙 |
|------|------|
| MVP 수집처 | `NARA` |
| 직접 등록 출처 | `MANUAL`. 수집 조건에는 사용할 수 없다 |
| 공고 종류 | `CONSTRUCTION`, `SERVICE` |
| 자동 스케줄링 | 사용자가 조건별 자동 수집 여부, 실행 주기와 실행 시각을 설정한다 |
| 자동 실행 주기 | `DAILY`, `WEEKDAYS`. `WEEKDAYS`는 월~금이며 공휴일은 별도로 제외하지 않는다 |
| 자동 실행 시각 | `09:00`, `13:00`, `18:00` 중 하나를 선택한다 |
| 시간대 | MVP에서는 `Asia/Seoul`만 허용한다 |
| 실행 시각 의미 | 설정 시각은 수집 Run을 생성하는 기준이다. Worker 대기열에 따라 실제 외부 API 호출은 늦어질 수 있다 |
| 비활성 조건 | `isActive=false`이면 수동 실행과 자동 실행을 모두 허용하지 않고 `nextRunAt=null`로 저장한다 |
| 수동 실행 | 자동 수집 설정과 관계없이 활성 조건은 기존 수동 실행 API를 사용할 수 있다 |
| 공통 필드 | API의 명시적 필드로 전달한다 |
| 수집처별 검색조건 | `filters` 객체로 전달하고 DB의 `crawl_condition.params` JSON에 저장한다 |
| 수집처 변경 | 등록 후 변경할 수 없다 |
| 수정 방식 | `noticeTypes`와 `filters`는 전달된 값으로 전체 교체한다 |
| 외부 조회 방식 | 공사와 용역을 각각의 나라장터 검색 오퍼레이션으로 호출한다 |
| 복수 검색조건 | 키워드·지역·업종 조합별로 외부 API를 호출하고 공고번호·차수로 중복을 제거한다 |
| 호출 조합 제한 | `noticeTypes × keywords × regionCodes × industryCodes` 결과는 최대 20개다. 비어 있는 선택 필드는 1개 조합으로 계산한다 |
| 조회 기간 | 조건에 저장하지 않는다. 실행 시 마지막 성공 시각과 현재 시각을 기준으로 결정한다 |

### 수집 조건 등록 `POST /api/v1/bidding/collection-conditions`

**상태**: ✅ 확정

#### Request Body

```json
{
  "conditionName": "수도권 스마트시티 용역",
  "sourceCode": "NARA",
  "noticeTypes": ["CONSTRUCTION", "SERVICE"],
  "filters": {
    "keywords": ["스마트시티", "통합관제"],
    "regionCodes": ["11", "41"],
    "industryCodes": ["6202"],
    "minimumEstimatedPrice": 100000000,
    "maximumEstimatedPrice": 1000000000,
    "excludeClosed": true,
    "internationalBidType": "DOMESTIC"
  },
  "isActive": true,
  "autoCollectionEnabled": true,
  "scheduleType": "WEEKDAYS",
  "scheduledTime": "09:00",
  "timezone": "Asia/Seoul"
}
```

| 필드 | 타입 | 필수 | 규칙 |
|------|------|------|------|
| `conditionName` | String | Y | 1~100자 |
| `sourceCode` | String | Y | MVP에서는 `NARA`만 허용 |
| `noticeTypes` | List<String> | Y | 1개 이상. `CONSTRUCTION`, `SERVICE`만 허용 |
| `filters` | Object | Y | 나라장터 검색조건 |
| `filters.keywords` | List<String> | Y | 1~10개, 각 항목 1~100자 |
| `filters.regionCodes` | List<String> | N | 지원하는 지역 코드, 중복 불가 |
| `filters.industryCodes` | List<String> | N | 나라장터 업종코드, 최대 20개, 중복 불가 |
| `filters.minimumEstimatedPrice` | Long | N | 추정가격 하한, 0 이상 |
| `filters.maximumEstimatedPrice` | Long | N | 추정가격 상한, 0 이상이며 하한보다 작을 수 없음 |
| `filters.excludeClosed` | Boolean | Y | `true`이면 입찰 마감 공고를 외부 검색에서 제외 |
| `filters.internationalBidType` | String | N | `DOMESTIC`, `INTERNATIONAL`. `null`이면 전체 |
| `isActive` | Boolean | Y | 수동 실행 가능한 활성 조건인지 여부 |
| `autoCollectionEnabled` | Boolean | Y | 정기 자동 수집 사용 여부. `true`이면 `isActive`도 `true`여야 함 |
| `scheduleType` | String | 조건부 Y | 자동 수집 사용 시 필수. `DAILY`, `WEEKDAYS` |
| `scheduledTime` | String | 조건부 Y | 자동 수집 사용 시 필수. `HH:mm` 형식이며 `09:00`, `13:00`, `18:00` 중 하나 |
| `timezone` | String | 조건부 Y | 자동 수집 사용 시 필수. MVP에서는 `Asia/Seoul`만 허용 |

`autoCollectionEnabled=false`이면 `scheduleType`, `scheduledTime`, `timezone`은 `null`로 전달한다.
서버는 등록 시점 이후 첫 실행 시각을 `nextRunAt`으로 계산한다. 같은 조건의 수집 Run이 이미 처리 중이면 해당 회차의 자동 실행을 중복 생성하지 않는다.

자동 수집 실행 정책:

| 항목 | 규칙 |
|------|------|
| 스케줄러 확인 주기 | 1분마다 `nextRunAt <= 현재 시각`인 실행 대상 조건을 확인한다 |
| 한 번에 처리할 조건 | 한 회차에서 최대 50개까지 처리한다 |
| 다중 서버 점유 | DB 잠금을 사용하여 같은 조건은 한 서버만 점유한다 |
| 기존 실행 처리 중 | 같은 조건에 `PENDING` 또는 `PROCESSING` Run이 있으면 새 Run을 만들지 않고 `nextRunAt`만 다음 회차로 이동한다 |
| 수동·자동 동시 요청 | 먼저 Run을 생성한 요청만 성공하며, 나머지는 기존 실행으로 인해 중복 생성하지 않는다 |
| 서버 중단 후 재기동 | 놓친 회차를 모두 생성하지 않고, 실행 대상 조건마다 Run을 최대 1개만 생성한 뒤 다음 미래 회차를 계산한다 |
| 이전 실행 실패 | 이전 자동 Run의 실패 여부와 관계없이 다음 예약 회차는 정상 실행한다 |
| 마지막 예약 시각 | `lastScheduledAt`은 자동 Run이 실제 생성된 경우에만 갱신한다. 기존 실행으로 건너뛴 경우에는 변경하지 않는다 |
| 다음 실행 시각 | Run 생성 또는 회차 건너뛰기와 같은 트랜잭션에서 현재 시각보다 미래인 `nextRunAt`으로 갱신한다 |

외부 API는 공고명, 참가 제한 지역 및 업종을 요청당 각각 하나만 받는다.
복수 값을 입력하면 Worker가 조합별 요청으로 분리하므로 등록·수정 시 예상 호출 조합이 20개를 초과할 수 없다.

#### 나라장터 요청 매핑

| 내부 값 | 나라장터 요청값 | 설명 |
|--------|----------------|------|
| `noticeTypes=CONSTRUCTION` | `getBidPblancListInfoCnstwkPPSSrch` | 나라장터 검색조건에 의한 공사 공고 조회 |
| `noticeTypes=SERVICE` | `getBidPblancListInfoServcPPSSrch` | 나라장터 검색조건에 의한 용역 공고 조회 |
| 기본 수집 실행 | `inqryDiv=1` | 공고게시일시 기준으로 조회 |
| 실행 조회 시작·종료 시각 | `inqryBgnDt`, `inqryEndDt` | `YYYYMMDDHHMM`. 조건에 저장하지 않고 실행 시 결정 |
| `filters.keywords[]`의 단일 값 | `bidNtceNm` | 공고명 부분 검색 |
| `filters.regionCodes[]`의 단일 값 | `prtcptLmtRgnCd` | 참가 제한 지역 코드 |
| `filters.industryCodes[]`의 단일 값 | `indstrytyCd` | 나라장터 업종 코드 |
| `filters.minimumEstimatedPrice` | `presmptPrceBgn` | 추정가격 하한 |
| `filters.maximumEstimatedPrice` | `presmptPrceEnd` | 추정가격 상한 |
| `filters.excludeClosed=true` | `bidClseExcpYn=Y` | 입찰 마감 공고 제외 |
| `filters.excludeClosed=false` | `bidClseExcpYn=N` | 입찰 마감 공고 포함 |
| `filters.internationalBidType=DOMESTIC` | `intrntnlDivCd=1` | 국내 입찰 |
| `filters.internationalBidType=INTERNATIONAL` | `intrntnlDivCd=2` | 국제 입찰 |
| `filters.internationalBidType=null` | 파라미터 미전송 | 국내·국제 전체 |

외부 요청의 `ServiceKey`, `numOfRows`, `pageNo`, `type`은 사용자 입력이 아니라 Worker 운영 설정으로 관리한다.
인증키는 환경변수로 주입하며 DB, API 응답, 로그에 저장하지 않는다.

#### 나라장터 응답 저장 매핑

나라장터 원문 응답은 `bid_notice_raw.raw_payload`에 보존하고, 목록·상세 조회에 필요한 값만 정규화한다.
내부 상태와 나라장터 공고 상태는 의미가 다르므로 하나의 컬럼에 섞지 않는다.

| 나라장터 응답값 | 저장 위치 | 규칙 |
|-----------------|----------|------|
| `bidNtceNo` | `bid_notice.external_id` | 나라장터 입찰공고번호 |
| `bidNtceOrd` | `bid_notice.notice_ord` | 재공고·재입찰 차수 |
| 호출 오퍼레이션 | `bid_notice.notice_type` | 공사 조회는 `CONSTRUCTION`, 용역 조회는 `SERVICE` |
| `bidNtceNm` | `bid_notice.notice_name` | 최대 1,000자 |
| `ntceInsttNm` | `bid_notice.notice_agency` | 공고기관명, 최대 400자 |
| `dminsttNm` | `bid_notice.demand_agency` | 수요기관명, 최대 400자 |
| `ntceKindNm` | `bid_notice.external_notice_status` | 등록공고·변경공고·취소공고·재공고 등 나라장터 상태 |
| `intrbidYn` | `bid_notice.international_bid_type` | `Y`는 `INTERNATIONAL`, `N`은 `DOMESTIC`, 값이 없으면 `null` |
| `bidNtceDt` | `bid_notice.announced_at` | 입찰공고일시 |
| `bidBeginDt` | `bid_notice.bid_start_at` | 입찰개시일시 |
| `bidClseDt` | `bid_notice.bid_deadline_at` | 입찰마감일시 |
| `opengDt` | `bid_notice.opening_at` | 개찰 가능 시작일시 |
| `bdgtAmt` | `bid_notice.base_amount` | 배정예산액. 값이 없으면 `null` |
| `presmptPrce` | `bid_notice.estimated_amount` | 부가세·조달수수료를 제외한 추정가격 |
| `bidMethdNm` | `bid_notice.bid_method` | 전자입찰·직찰 등 입찰 방식 |
| `cntrctCnclsMthdNm` | `bid_notice.contract_method` | 일반경쟁·제한경쟁·수의계약 등 계약 체결 방식 |
| `bidQlfctRgstCntnts` | `bid_notice.participation_qualification_text` | 참가자격 원문 |
| `cmmnSpldmdMethdNm` | `bid_notice.joint_contract_text` | 공동수급 방식 원문 |
| `bidNtceDtlUrl` | `bid_notice.source_url` | 나라장터 공고 상세 링크 |
| `ntceSpecFileNm1..10` | `bid_notice_attachment.file_name` | 비어 있지 않은 항목만 순서대로 저장 |
| `ntceSpecDocUrl1..10` | `bid_notice_attachment.source_url` | 같은 번호의 파일명과 한 행으로 저장 |

| 상태 구분 | 저장값 | 역할 |
|----------|--------|------|
| 내부 처리 상태 | `bid_notice.notice_status` | `COLLECTED`, `DISMISSED` 등 서비스 내 검토 상태 |
| 외부 공고 상태 | `bid_notice.external_notice_status` | 나라장터의 등록·변경·취소·재공고 상태 |

첨부파일 URL은 나라장터가 제공한 원문 링크만 저장한다. 인증키가 포함된 요청 URL이나 일시적인 인증 URL은 저장하지 않는다.
공고를 다시 수집하면 `(bid_notice_id, attachment_kind, attachment_order)`를 기준으로 첨부파일 정보를 갱신한다.

#### Response

```text
201 Created
```

```json
{
  "httpStatus": 201,
  "message": "입찰 공고 수집 조건 등록 성공",
  "data": {
    "conditionId": 1,
    "conditionName": "수도권 스마트시티 용역",
    "sourceCode": "NARA",
    "noticeTypes": ["CONSTRUCTION", "SERVICE"],
    "filters": {
      "keywords": ["스마트시티", "통합관제"],
      "regionCodes": ["11", "41"],
      "industryCodes": ["6202"],
      "minimumEstimatedPrice": 100000000,
      "maximumEstimatedPrice": 1000000000,
      "excludeClosed": true,
      "internationalBidType": "DOMESTIC"
    },
    "isActive": true,
    "autoCollectionEnabled": true,
    "scheduleType": "WEEKDAYS",
    "scheduledTime": "09:00",
    "timezone": "Asia/Seoul",
    "nextRunAt": "2026-08-10T09:00:00",
    "lastScheduledAt": null,
    "createdAt": "2026-08-09T10:00:00"
  }
}
```

### 수집 조건 목록 조회 `GET /api/v1/bidding/collection-conditions`

**상태**: ✅ 확정

등록된 수집 조건과 활성화 여부, 최근 성공 시각 및 최근 수집 건수를 조회한다.
삭제된 조건은 반환하지 않으며 최신 등록 순으로 정렬한다.

```json
{
  "httpStatus": 200,
  "message": "입찰 공고 수집 조건 목록 조회 성공",
  "data": {
    "content": [
      {
        "conditionId": 1,
        "conditionName": "수도권 스마트시티 용역",
        "sourceCode": "NARA",
        "sourceName": "나라장터",
        "noticeTypes": ["CONSTRUCTION", "SERVICE"],
        "filters": {
          "keywords": ["스마트시티", "통합관제"],
          "regionCodes": ["11", "41"],
          "industryCodes": ["6202"],
          "minimumEstimatedPrice": 100000000,
          "maximumEstimatedPrice": 1000000000,
          "excludeClosed": true,
          "internationalBidType": "DOMESTIC"
        },
        "isActive": true,
        "autoCollectionEnabled": true,
        "scheduleType": "WEEKDAYS",
        "scheduledTime": "09:00",
        "timezone": "Asia/Seoul",
        "nextRunAt": "2026-08-10T09:00:00",
        "lastScheduledAt": null,
        "lastSuccessAt": null,
        "lastCollectedCount": null,
        "createdAt": "2026-08-09T10:00:00",
        "updatedAt": null
      }
    ]
  }
}
```

### 수집 조건 수정 `PATCH /api/v1/bidding/collection-conditions/{conditionId}`

**상태**: ✅ 확정

`sourceCode`는 수정할 수 없다. `noticeTypes`, `filters`와 자동 수집 설정은 부분 병합하지 않고 요청값으로 전체 교체한다.
자동 수집 설정이 변경되면 서버는 변경 시점 이후를 기준으로 `nextRunAt`을 다시 계산한다.

```json
{
  "conditionName": "수도권 스마트시티 공사·용역",
  "noticeTypes": ["CONSTRUCTION", "SERVICE"],
  "filters": {
    "keywords": ["스마트시티"],
    "regionCodes": ["11", "41"],
    "industryCodes": ["6202"],
    "minimumEstimatedPrice": 100000000,
    "maximumEstimatedPrice": null,
    "excludeClosed": true,
    "internationalBidType": "DOMESTIC"
  },
  "isActive": true,
  "autoCollectionEnabled": true,
  "scheduleType": "DAILY",
  "scheduledTime": "13:00",
  "timezone": "Asia/Seoul"
}
```

성공 시 수정된 수집 조건을 등록 응답의 `data`와 같은 구조로 반환한다.

### Status Code

| HTTP | code | 적용 API | 설명 |
|------|------|----------|------|
| 200 | - | 목록, 수정 | 처리 성공 |
| 201 | - | 등록 | 등록 성공 |
| 400 | `BIDDING_INVALID_COLLECTION_CONDITION` | 등록, 수정 | 검색조건 또는 금액 범위가 유효하지 않음 |
| 400 | `BIDDING_COLLECTION_QUERY_LIMIT_EXCEEDED` | 등록, 수정 | 외부 API 예상 호출 조합이 20개를 초과함 |
| 400 | `BIDDING_INVALID_COLLECTION_SCHEDULE` | 등록, 수정 | 자동 수집 활성 여부, 주기, 시각 또는 시간대 조합이 유효하지 않음 |
| 400 | `BIDDING_UNSUPPORTED_SOURCE` | 등록 | 지원하지 않는 수집처이거나 `MANUAL`을 수집 조건에 사용함 |
| 401 | `AUTH_UNAUTHENTICATED` | 전체 | 세션이 없거나 만료됨 |
| 403 | `BIDDING_ACCESS_PERMISSION_REQUIRED` | 전체 | 입찰 관리 권한 없음 |
| 404 | `BIDDING_COLLECTION_CONDITION_NOT_FOUND` | 수정 | 활성 수집 조건이 존재하지 않음 |

---

## 수동 수집 및 결과 조회

### 입찰 공고 수집 실행 `POST /api/v1/bidding/collection-conditions/{conditionId}/runs`

**상태**: ✅ 확정

현재 회사가 소유한 활성 수집 조건으로 입찰 공고 수집 작업을 요청한다.
현재 범위에서는 실행 이력을 `PENDING`으로 생성하고 `runId`를 반환한다.
Redis Stream 발행, DB Outbox, Spring Worker 처리와 외부 수집처 호출은 아래 후속 구현 계약을 따른다.

#### Path Parameter

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `conditionId` | Long | Y | 실행할 수집 조건 ID |

#### Request Body

없음

#### 처리 규칙

| 항목 | 규칙 |
|------|------|
| 조건 소유권 | 현재 회사가 소유한 삭제되지 않은 조건만 실행할 수 있다 |
| 활성 조건 | `is_active = true`인 조건만 실행할 수 있다 |
| 중복 실행 | 같은 조건에 `PENDING` 또는 `PROCESSING` 실행이 있으면 새 실행을 거부한다 |
| 초기 상태 | `PENDING` |
| 현재 실행 방식 | DB에 `crawl_run`을 `PENDING` 상태로 저장하고 `runId`를 반환한다 |
| 후속 구현 | Redis Stream 작업 전달, DB Outbox 유실 방지, Spring Worker 처리와 선택적 최대 3회 재시도 계약을 적용한다 |
| 회사 격리 | `crawl_run -> crawl_condition.company_id` 경로로 현재 회사를 검증한다 |

#### Success Response

```json
{
  "httpStatus": 202,
  "message": "입찰 공고 수집 요청이 접수되었습니다.",
  "data": {
    "runId": 1,
    "runStatus": "PENDING",
    "requestedAt": "2026-08-10T11:30:00"
  }
}
```

#### Status Code

| 코드 | code | 설명 |
|------|------|------|
| 202 | - | 수집 요청 접수 성공 |
| 400 | `BIDDING_INACTIVE_COLLECTION_CONDITION` | 비활성 수집 조건 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션이 없거나 만료됨 |
| 403 | `BIDDING_ACCESS_PERMISSION_REQUIRED` | 입찰 관리 권한 없음 |
| 404 | `BIDDING_COLLECTION_CONDITION_NOT_FOUND` | 현재 회사의 수집 조건이 존재하지 않음 |
| 409 | `BIDDING_COLLECTION_RUN_ALREADY_PROCESSING` | 같은 조건의 수집 작업이 이미 진행 중 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

### 수집 실행 결과 조회 `GET /api/v1/bidding/collection-runs/{runId}`

**상태**: ✅ 확정

현재 회사의 수집 실행 상태와 저장 결과 집계를 조회한다.

#### Path Parameter

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `runId` | Long | Y | 수집 실행 ID |

#### 실행 상태

| 상태 | 설명 |
|------|------|
| `PENDING` | 현재 구현에서 생성되는 수집 실행 접수 상태 |
| `PROCESSING` | Worker가 실행을 점유하고 외부 수집을 처리 중인 상태 |
| `COMPLETED` | 모든 요청 조합을 정상 처리한 상태 |
| `PARTIAL_SUCCESS` | 일부 요청 조합은 성공했으나 일부는 최종 실패한 상태 |
| `FAILED` | 모든 요청 조합이 실패했거나 작업을 계속할 수 없는 상태 |

| 응답값 | 설명 |
|--------|------|
| `runId` | 수집 실행 ID |
| `conditionId` | 실행한 수집 조건 ID |
| `triggerType` | 실행 방식. MVP는 `MANUAL` |
| `runStatus` | 실행 상태 |
| `collectedCount` | 전체 조회 건수 |
| `insertedCount` | 신규 저장 건수 |
| `updatedCount` | 갱신 건수 |
| `skippedCount` | 건너뛴 건수 |
| `errorMessage` | 실패 또는 부분 성공 원인. 없으면 `null` |
| `startedAt` | 실행 요청 시각 |
| `finishedAt` | 실행 종료 시각. 진행 중이면 `null` |

#### Success Response

```json
{
  "httpStatus": 200,
  "message": "입찰 공고 수집 결과 조회 성공",
  "data": {
    "runId": 1,
    "conditionId": 1,
    "triggerType": "MANUAL",
    "runStatus": "COMPLETED",
    "collectedCount": 40,
    "insertedCount": 12,
    "updatedCount": 5,
    "skippedCount": 23,
    "errorMessage": null,
    "startedAt": "2026-08-10T11:30:01",
    "finishedAt": "2026-08-10T11:30:08"
  }
}
```

#### Status Code

| 코드 | code | 설명 |
|------|------|------|
| 200 | - | 실행 결과 조회 성공 |
| 400 | `BIDDING_INVALID_COLLECTION_RUN_REQUEST` | 유효하지 않은 실행 ID |
| 401 | `AUTH_UNAUTHENTICATED` | 세션이 없거나 만료됨 |
| 403 | `BIDDING_ACCESS_PERMISSION_REQUIRED` | 입찰 관리 권한 없음 |
| 404 | `BIDDING_COLLECTION_RUN_NOT_FOUND` | 현재 회사의 실행 이력이 존재하지 않음 |

---

## 수집 Worker 비동기 처리 계약

**상태**: ✅ 확정

수집 실행 요청과 외부 API 호출을 분리하고, DB 커밋과 Redis 발행 사이의 작업 유실을 막기 위해 Redis Stream과 DB Outbox를 함께 사용한다.

### 처리 흐름

```text
수집 실행 API
→ 같은 DB 트랜잭션에서 crawl_run(PENDING)과 Outbox 저장
→ 트랜잭션 커밋
→ Outbox Dispatcher가 미발행 이벤트 조회
→ Redis Stream에 작업 발행
→ Outbox를 PUBLISHED로 변경
→ Spring Worker가 Consumer Group으로 작업 소비
→ crawl_run을 PROCESSING으로 전이
→ `crawl_run.condition_snapshot`에서 요청 당시 조건을 복원
→ 나라장터 OpenAPI 호출·공고 정규화·저장
→ crawl_run 결과 집계와 최종 상태 저장
→ Redis ACK
```

### Redis Stream 계약

| 항목 | 규칙 |
|------|------|
| Stream | 입찰 수집 작업 전용 Stream을 사용하며 실제 키 이름은 운영 설정으로 주입한다 |
| 소비 방식 | Spring Worker가 Consumer Group으로 소비한다 |
| 메시지 원칙 | 수집 조건 전체를 넣지 않고 작업 식별에 필요한 최소 정보만 넣는다 |
| 조건 조회 | Worker는 `runId`와 `companyId`를 검증한 뒤 `crawl_run.condition_snapshot`의 요청 당시 조건을 사용한다 |
| 성공 처리 | 실행 결과가 DB에 커밋된 뒤 Redis 메시지를 ACK한다 |
| 중복 메시지 | 동일한 `runId`와 `attemptId`의 재전달을 허용하고 Worker가 멱등 처리한다 |

```json
{
  "runId": 1,
  "conditionId": 10,
  "companyId": 3,
  "attemptId": "서버가 생성한 작업 시도 식별자",
  "retryCount": 0
}
```

### DB Outbox 계약

| 항목 | 규칙 |
|------|------|
| 원자성 | `crawl_run`과 Outbox 이벤트는 같은 DB 트랜잭션에서 저장한다 |
| 발행 주체 | 별도 Outbox Dispatcher가 커밋된 미발행 이벤트를 조회하여 Redis에 발행한다 |
| 발행 성공 | Redis 발행 성공 후 Outbox 상태를 `PUBLISHED`로 변경한다 |
| 발행 실패 | 안전한 오류 유형을 기록하고 `nextAttemptAt` 이후 최대 5회 다시 발행한다 |
| 발행 종료 | 5회 발행 실패 또는 손상된 payload는 `FAILED`로 종료하고 운영 알림 대상으로 분류한다 |
| 중복 가능성 | Redis 발행 성공 후 상태 변경 전에 장애가 발생할 수 있으므로 중복 발행을 허용한다 |
| 민감 정보 | 외부 API 인증키, 원문 응답 및 개인정보를 Outbox payload와 오류에 저장하지 않는다 |

Outbox는 최소한 아래 정보를 관리한다.

| 필드 | 설명 |
|------|------|
| `outboxId` | Outbox 식별자 |
| `runId` | 수집 실행 ID |
| `eventType` | 수집 요청 이벤트 유형 |
| `status` | 발행 상태 |
| `attemptCount` | Redis 발행 시도 횟수 |
| `nextAttemptAt` | 다음 발행 가능 시각 |
| `publishedAt` | 발행 완료 시각 |
| `lastError` | 민감 정보를 제거한 마지막 발행 오류 |
| `createdAt` | 생성 시각 |

### Task 영구 실패 DLQ Outbox 계약

| 항목 | 규칙 |
|------|------|
| 원자성 | `crawl_run_task`의 `FAILED` 전이와 Task DLQ Outbox 저장은 같은 DB 트랜잭션에서 처리한다 |
| 직접 발행 금지 | Worker 처리 흐름에서는 Task 실패 직후 Redis DLQ를 직접 호출하지 않는다 |
| 발행 주체 | Outbox Dispatcher가 `BIDDING_COLLECTION_TASK_DLQ_REQUESTED` 이벤트를 Task DLQ Stream으로 발행한다 |
| Redis 장애 | 발행 실패 시 Outbox를 `PENDING`으로 되돌리고 최대 5회까지 재시도한다 |
| 중복 방지 | DB는 `eventType + taskId + attemptId` 조합의 Outbox 중복 생성을 차단한다 |
| 소비자 멱등성 | Redis 중복 전달에 대비해 DLQ 소비자는 `dedupKey = taskId:attemptId`로 중복 처리를 차단한다 |
| 보관 정보 | `runId`, `taskId`, `companyId`, `attemptId`, 재시도 횟수, 실패 유형과 수집 조건 조합만 저장한다 |

### Worker 재시도 정책

외부 요청 조합 하나를 독립적인 처리 단위로 보고 일시적 오류만 최대 3회 재시도한다.

재시도 작업은 Redis 지연 저장소에 예약하며, 예약 저장이 성공한 뒤 현재 메시지를 ACK한다.
처리 중 프로세스가 종료되어 PEL에 남은 메시지는 유휴 시간이 지난 뒤 다른 Consumer가 회수한다.

| 실패 유형 | 재시도 | 처리 |
|----------|--------|------|
| 연결 실패·Timeout | O | 최대 3회 재시도 |
| HTTP 429 | O | 최대 3회 재시도 |
| HTTP 5xx | O | 최대 3회 재시도 |
| HTTP 400 | X | 요청 오류로 기록하고 해당 조합 실패 처리 |
| HTTP 401·403 | X | 인증·권한 설정 오류로 기록하고 실행 중단 |
| 응답 파싱 실패 | X | 외부 계약 변경 가능성이 있으므로 원문을 남기지 않고 파싱 오류만 기록 |

재시도 간격은 30초, 2분, 10분의 지수 백오프를 적용한다.

### 최종 상태와 실패 처리

| 결과 | `crawl_run.run_status` | Redis 처리 |
|------|------------------------|------------|
| 모든 요청 조합 성공 | `COMPLETED` | DB 커밋 후 ACK |
| 일부 요청 조합 성공 | `PARTIAL_SUCCESS` | 성공 결과와 실패 요약을 커밋한 뒤 ACK |
| 모든 요청 조합 실패 | `FAILED` | 실패 상태 커밋 후 DLQ 기록 및 ACK |
| 재시도 가능한 실패 | 기존 처리 상태 유지 | 재시도 메시지를 발행한 뒤 현재 메시지 ACK |

최종 실패 작업은 DLQ에 `runId`, `conditionId`, `companyId`, `attemptId`, `retryCount`, 오류 유형만 남긴다.
외부 API 인증키와 응답 원문은 DB, Redis, DLQ 및 애플리케이션 로그에 남기지 않는다.

---

## 입찰 공고 목록 조회 `GET /api/v1/bidding/notices`

**상태**: ✅ 확정

회사 경계:

| 항목 | 규칙 |
|------|------|
| 공고 원본 | `bid_notice`는 모든 회사가 공유하는 공공 원천 데이터다 |
| 회사별 상태 | `company_bid_notice_state`에 회사별 `COLLECTED`, `DISMISSED`, 제외 사유와 최초·최종 확인 실행을 저장한다 |
| 목록 노출 | 현재 회사의 `company_bid_notice_state`가 존재하는 공고만 반환한다 |
| 다른 회사 접근 | 목록에서 제외하고 상세 조회는 `BIDDING_NOTICE_NOT_FOUND`로 응답한다 |
| 공용 칼럼 | 기존 `bid_notice.notice_status`, `dismiss_reason`은 호환 목적으로 유지하되 조회 계약에는 사용하지 않는다 |

**Query**

| 파라미터 | 설명 |
|---------|------|
| `startDate` | 조회 시작일 |
| `endDate` | 조회 종료일 |
| `noticeAgency` | 발주처 |
| `businessCategoryId` | 사업 카테고리 ID |
| `region` | 지역 |
| `deadlineSoon` | 마감 임박 여부 |
| `keyword` | 검색어 |
| `noticeStatus` | 회사별 공고 상태. `COLLECTED`, `DISMISSED` |
| `sort` | `ANNOUNCED_DESC`(기본), `DEADLINE_ASC`, `AMOUNT_DESC` |
| `page` | 0부터 시작하는 페이지. 기본 `0` |
| `size` | 페이지 크기. 기본 `20`, 최대 `100` |

**Response 주요값**

| 파라미터 | 설명 |
|---------|------|
| `noticeId` | 공고 ID |
| `noticeName` | 공고명 |
| `sourceCode` | 출처 코드 |
| `sourceName` | 출처명 |
| `sourceUrl` | 원문 URL |
| `noticeAgency` | 발주처 |
| `businessCategoryId` | 사업 카테고리 ID |
| `businessCategoryName` | 사업 카테고리명 |
| `baseAmount` | 기초금액 |
| `estimatedAmount` | 추정가격 |
| `announcedAt` | 공고일 |
| `bidDeadlineAt` | 마감일 |
| `dDay` | D-day |
| `isNew` | 신규 배지 표시 여부 |
| `noticeStatus` | 공고 상태 |
| `projectId` | 전환된 프로젝트 ID |

목록 응답은 `content`, `totalElements`, `totalPages`, `page`, `size`를 반환한다.

화면 규칙:

| 조건 | 표시 |
|------|------|
| `projectId = null` | 프로젝트 생성 버튼 |
| `projectId` 존재 | 프로젝트 보기 |
| `dDay > 0` | `D-n` |
| `dDay = 0` | `D-Day` |
| `dDay < 0` | 마감 |

`isNew` 산정 기준:

| 항목 | 규칙 |
|------|------|
| 기준 | 현재 회사의 마지막 성공 또는 부분 성공 수집 실행과 `firstSeenRunId`가 같은 공고 |
| 유지 기간 | 다음 성공 수집 실행 전까지 |
| 수동 등록 | 등록 직후 `isNew=true`, 다음 성공 수집 실행 전까지 유지 |
| 저장 방식 | 프론트가 계산하지 않고 서버가 Boolean으로 내려준다 |

---

## 입찰 공고 상세 조회 `GET /api/v1/bidding/notices/{noticeId}`

**상태**: ✅ 확정

| 영역 | 필드 |
|------|------|
| 기본 정보 | `noticeId`, `externalId`, `noticeOrder`, `noticeName`, `noticeType`, `externalNoticeStatus`, `noticeAgency`, `demandAgency`, `noticeStatus`, `dismissReason`, `projectId` |
| 출처 | `sourceCode`, `sourceName`, `sourceUrl`, `hasAttachment` |
| 일정 | `announcedAt`, `bidStartAt`, `questionDeadlineAt`, `applicationDeadlineAt`, `bidDeadlineAt`, `openingAt`, `dDay` |
| 금액 | `baseAmount`, `estimatedAmount`, `priceRangeText`, `minimumBidRateText` |
| 계약 및 제한 | `participationQualificationText`, `regionLimitText`, `businessLimitText`, `jointContractAllowed`, `jointContractText`, `contractMethod`, `evaluationMethod` |
| 첨부파일 | `attachments[].attachmentOrder`, `attachments[].fileName`, `attachments[].sourceUrl` |

`dDay`는 DB에 저장하지 않고 서버에서 계산한다.

첨부파일은 삭제되지 않은 항목만 순서 오름차순으로 반환한다. 파일 크기는 수집 원천에 일관된 값이 없어 반환하지 않는다.

**Status Code**

| 코드 | code | 설명 |
|------|------|------|
| 200 | - | 조회 성공 |
| 400 | `BIDDING_INVALID_NOTICE_QUERY` | 조회 조건이 올바르지 않음 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션 없음 또는 만료 |
| 403 | `BIDDING_ACCESS_PERMISSION_REQUIRED` | 입찰 관리 권한 없음 |
| 404 | `BIDDING_NOTICE_NOT_FOUND` | 현재 회사에서 조회할 수 있는 공고가 없음 |

참여 요건 필드는 REQ-02에 따라 원문 문자열을 보존한다.

| 필드 | 규칙 |
|------|------|
| `participationQualificationText` | 참가 자격 원문. 파싱하지 못해도 문자열 그대로 보존 |
| `regionLimitText` | 지역 제한 원문 |
| `businessLimitText` | 업종 제한 원문 |
| `jointContractAllowed` | 공동수급 가능 여부. 판별 불가면 `null` |
| `jointContractText` | 공동수급 관련 원문 |

---

## 입찰 공고 직접 등록 및 수정

### 공통 정책

**상태**: ✅ 확정

| 항목 | 규칙 |
|------|------|
| 등록 출처 | 직접 등록 공고는 `crawl_source.source_code = MANUAL`을 사용한다 |
| 회사 소유권 | `bid_notice.owner_company_id`에 현재 회사 ID를 저장하며 등록 회사에서만 조회·수정할 수 있다 |
| 공용 공고 | `NARA`, `KCAA` 등 외부 수집 공고의 `owner_company_id`는 `NULL`이다 |
| 최초 회사 상태 | 등록과 같은 트랜잭션에서 현재 회사의 `company_bid_notice_state`를 `COLLECTED`로 생성한다 |
| 외부 식별자 | 서버가 직접 등록 공고용 고유 `externalId`를 생성한다. 클라이언트는 전달하지 않는다 |
| 공고 차수 | 직접 등록 공고는 `noticeOrder = 00`으로 저장한다 |
| 중복 기준 | 현재 회사에서 공고명, 공고기관, 공고일시, 입찰마감일시가 모두 같은 활성 공고를 중복으로 본다 |
| 동시 요청 방지 | 정규화한 중복 기준으로 `manualDedupKey`를 생성하고 DB UNIQUE 제약으로 최종 차단한다 |
| 수정 대상 | `MANUAL` 출처이며 `owner_company_id`가 현재 회사인 공고만 수정할 수 있다 |
| 외부 공고 보호 | 나라장터·KCAA 등 외부 수집 공고는 수정 API로 변경할 수 없다 |
| 수정 방식 | 전달한 필드만 수정하고 생략한 필드는 기존 값을 유지한다 |
| 선택값 해제 | 수정 요청에서 선택 필드를 명시적으로 `null`로 보내면 기존 값을 해제한다 |
| 삭제 | 영구 삭제 API를 제공하지 않는다. 추후 공고 제외·복구 API로 회사별 노출 상태만 변경한다 |
| 프로젝트 보호 | 프로젝트 전환 여부와 관계없이 공고 원본과 첨부 링크는 물리 삭제하지 않는다 |

첨부는 파일 도메인의 업로드 파일이 아니라 공개된 원문 링크다. 파일 도메인의 파일은 프로젝트 소속이므로,
프로젝트 전환 전 공고에 연결하지 않는다. 인증키·토큰·서명이 포함된 임시 URL은 저장하지 않는다.

### 직접 등록 `POST /api/v1/bidding/notices`

#### Request Body

```json
{
  "noticeName": "스마트시티 통합관제 플랫폼 구축 용역",
  "noticeType": "SERVICE",
  "noticeAgency": "서울특별시",
  "demandAgency": "서울특별시 정보화담당관",
  "internationalBidType": "DOMESTIC",
  "announcedAt": "2026-08-11T09:00:00",
  "bidStartAt": "2026-08-12T09:00:00",
  "bidDeadlineAt": "2026-08-20T18:00:00",
  "openingAt": "2026-08-21T10:00:00",
  "baseAmount": 300000000,
  "estimatedAmount": 330000000,
  "bidMethod": "전자입찰",
  "contractMethod": "협상에 의한 계약",
  "participationQualificationText": "소프트웨어사업자 및 관련 실적 보유 업체",
  "regionLimitText": "서울특별시",
  "businessLimitText": "소프트웨어사업자",
  "jointContractAllowed": false,
  "jointContractText": null,
  "evaluationMethod": "기술·가격 종합평가",
  "sourceUrl": "https://example.org/notices/2026-001",
  "attachments": [
    {
      "fileName": "제안요청서.pdf",
      "sourceUrl": "https://example.org/notices/2026-001/rfp.pdf"
    }
  ]
}
```

| 필드 | 타입 | 필수 | 규칙 |
|------|------|------|------|
| `noticeName` | String | Y | 1~1,000자 |
| `noticeType` | String | Y | `CONSTRUCTION`, `SERVICE` |
| `noticeAgency` | String | Y | 1~400자 |
| `demandAgency` | String | N | 최대 400자 |
| `internationalBidType` | String | N | `DOMESTIC`, `INTERNATIONAL` |
| `announcedAt` | LocalDateTime | Y | 공고일시 |
| `bidStartAt` | LocalDateTime | N | 입찰개시일시 |
| `bidDeadlineAt` | LocalDateTime | Y | 공고일시보다 이후여야 한다 |
| `openingAt` | LocalDateTime | N | 개찰 가능 시작일시 |
| `baseAmount` | BigDecimal | N | 0 이상 |
| `estimatedAmount` | BigDecimal | N | 0 이상 |
| `bidMethod` | String | N | 최대 100자 |
| `contractMethod` | String | N | 최대 100자 |
| `participationQualificationText` | String | N | 최대 1,000자 |
| `regionLimitText` | String | N | 최대 500자 |
| `businessLimitText` | String | N | 최대 500자 |
| `jointContractAllowed` | Boolean | N | 공동수급 가능 여부 |
| `jointContractText` | String | N | 최대 500자 |
| `evaluationMethod` | String | N | 최대 100자 |
| `sourceUrl` | String | N | 최대 1,000자, 공개된 `http` 또는 `https` URL |
| `attachments` | List<Object> | N | 최대 10개. 생략하거나 빈 배열이면 첨부 없이 등록 |
| `attachments[].fileName` | String | Y | 1~255자 |
| `attachments[].sourceUrl` | String | Y | 최대 1,000자, 공개된 `http` 또는 `https` URL, 요청 내 중복 불가 |

#### Success Response

```text
201 Created
```

```json
{
  "httpStatus": 201,
  "message": "입찰 공고 직접 등록 성공",
  "data": {
    "noticeId": 101,
    "externalId": "서버가 생성한 직접 등록 식별자",
    "noticeOrder": "00",
    "sourceCode": "MANUAL",
    "sourceName": "직접 등록",
    "noticeStatus": "COLLECTED",
    "noticeName": "스마트시티 통합관제 플랫폼 구축 용역",
    "noticeType": "SERVICE",
    "noticeAgency": "서울특별시",
    "announcedAt": "2026-08-11T09:00:00",
    "bidDeadlineAt": "2026-08-20T18:00:00",
    "attachments": [
      {
        "attachmentOrder": 1,
        "fileName": "제안요청서.pdf",
        "sourceUrl": "https://example.org/notices/2026-001/rfp.pdf"
      }
    ],
    "createdAt": "2026-08-11T10:00:00",
    "updatedAt": null
  }
}
```

### 직접 등록 공고 수정 `PATCH /api/v1/bidding/notices/{noticeId}`

요청 필드는 직접 등록 요청과 같으며 모두 선택이다. 단, 변경할 필드를 한 개 이상 전달해야 한다.
`sourceCode`, `externalId`, `noticeOrder`, `ownerCompanyId`, `noticeStatus`는 수정 요청으로 변경할 수 없다.

첨부 필드는 다음 세 가지를 구분한다.

| 요청 | 처리 |
|------|------|
| `attachments` 생략 | 기존 첨부 유지 |
| `"attachments": []` | 기존 첨부 전체 논리 삭제 |
| 비어 있지 않은 배열 | 요청 배열 순서대로 기존 첨부 전체 교체 |

선택 가능한 일반 필드는 명시적 `null`로 기존 값을 해제할 수 있다. 필수 필드인 `noticeName`, `noticeType`,
`noticeAgency`, `announcedAt`, `bidDeadlineAt`에는 `null`을 전달할 수 없다. 수정 후에도 등록 요청과 같은 필드 검증과
`bidDeadlineAt > announcedAt` 불변식을 만족해야 하며, 중복 기준 필드가 바뀌면 `manualDedupKey`를 다시 계산한다.

성공 시 `200 OK`, 메시지는 `입찰 공고 수정 성공`이며 `data`는 직접 등록 성공 응답과 같은 구조로 반환한다.

### Status Code

| HTTP | code | 적용 API | 설명 |
|------|------|----------|------|
| 201 | - | 직접 등록 | 등록 성공 |
| 200 | - | 수정 | 수정 성공 |
| 400 | `BIDDING_INVALID_MANUAL_NOTICE` | 전체 | 필수값, 길이, 날짜 관계, 금액 또는 URL 형식이 올바르지 않음 |
| 401 | `AUTH_UNAUTHENTICATED` | 전체 | 세션 없음 또는 만료 |
| 403 | `BIDDING_ACCESS_PERMISSION_REQUIRED` | 전체 | 입찰 관리 권한 없음 |
| 404 | `BIDDING_NOTICE_NOT_FOUND` | 수정 | 현재 회사에서 수정할 수 있는 공고가 없음 |
| 409 | `BIDDING_MANUAL_NOTICE_DUPLICATED` | 전체 | 현재 회사에 같은 중복 기준의 활성 직접 등록 공고가 존재함 |
| 409 | `BIDDING_NOTICE_EDIT_NOT_ALLOWED` | 수정 | 외부 수집 공고처럼 직접 등록 수정 대상이 아님 |

---

## 공고 제외 및 복구

| API | 설명 |
|-----|------|
| `PATCH /api/v1/bidding/notices/{noticeId}/dismiss` | 공고 제외 |
| `PATCH /api/v1/bidding/notices/{noticeId}/restore` | 공고 복구 |

**제외 Request**

```json
{
  "reason": "지역 제한 조건을 충족하지 못함"
}
```

제외 시 `notice_status = DISMISSED`, `dismiss_reason` 저장, 상태 변경 이력을 남긴다.

---

## 입찰 AI 요약

입찰 AI 요약과 비타메이트 AI는 별개의 기능이다.

| API | 설명 |
|-----|------|
| `POST /api/v1/bidding/notices/{noticeId}/summaries` | 요약 요청 |
| `GET /api/v1/bidding/summaries/{summaryId}` | 상태 및 결과 조회 |
| `PATCH /api/v1/bidding/summaries/{summaryId}` | 사용자 수정 |
| `PATCH /api/v1/bidding/summaries/{summaryId}/confirm` | 최종 확정 |

요약 요청은 비동기다.

```text
202 Accepted
→ summaryId 반환
→ 상태 및 결과 조회
```

---

## 공고 프로젝트 전환 `POST /api/v1/bidding/notices/{noticeId}/projects`

**상태**: 📝 초안

**Request 권장값**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `name` | String | 프로젝트명 |
| `description` | String | 설명 |
| `businessCategoryId` | Long | 사업 카테고리 ID |
| `startedOn` | Date | 시작일 |
| `endedOn` | Date | 종료일 |
| `memberIds` | String[] | 추가 참여자 user ID 목록. 전환 요청자는 서버가 자동 포함 |

서버 처리:

1. 공고 존재 여부 확인
2. 기존 프로젝트 전환 여부 확인
3. 요청자가 `BIDDING` 권한을 갖는지 확인
4. 추가 `memberIds`가 초대 가능한 사용자이며 프로젝트 참여자로 등록 가능한지 확인
5. 하나의 DB 트랜잭션 시작
6. 프로젝트 생성
7. `project.bid_notice_id` 저장
8. 인증된 전환 요청자를 `project_member`에 자동 등록
9. 추가 `memberIds`를 `project_member`에 등록
10. 필요 시 입찰 프로젝트 기본 스테이지·스텝 자동 생성
11. 입찰용 블록은 생성하지 않음
12. 전체 성공 시 커밋, 중간 실패 시 전체 롤백

권한 정책:

| 항목 | 규칙 |
|------|------|
| 전환 요청자 | 요청 바디에 없어도 서버가 반드시 `project_member`에 등록 |
| 추가 참여자 | `memberIds`로 받은 사용자만 추가 등록 |
| 권한 검사 | 요청자는 `BIDDING` 권한 필요. 추가 참여자는 존재 여부와 초대 가능 여부 검증 |
| 중복 참여자 | 요청자와 `memberIds`가 겹치면 한 번만 등록 |

트랜잭션 정책:

| 항목 | 규칙 |
|------|------|
| 원자성 | 프로젝트 생성부터 `project.bid_notice_id` 저장, 참여자 등록, 기본 단계 생성까지 하나의 DB 트랜잭션 |
| 중간 실패 | 전체 롤백. 불완전한 프로젝트를 남기지 않음 |
| 이미 전환된 공고 | 새 트랜잭션을 시작하지 않고 409 반환 |
| 재시도 | 이전 요청이 롤백됐다면 재시도 가능. 이미 커밋됐다면 409 |

정책:

| 항목 | 규칙 |
|------|------|
| 공고 하나당 프로젝트 | 1개만 생성 |
| 중복 방지 | `UNIQUE(project.bid_notice_id)` |
| 스냅샷 | 저장하지 않음 |
| 정정공고 | 변경 감지와 자동 반영은 v1 범위 밖 |

입찰 블록 미사용 정책:

| 항목 | 규칙 |
|------|------|
| 블록 타입 | 사용하지 않음 |
| 생성 위치 | 대상 없음 |
| 블록 제목 | 대상 없음 |
| 연결 대상 | `project.bid_notice_id`로 전환한 `bid_notice`를 조회 |
| 원본 보호 | 프로젝트 전환·삭제가 `bid_notice` 원본을 삭제하거나 수정하지 않음 |
| 중복 방지 | 같은 프로젝트 전환은 `UNIQUE(project.bid_notice_id)`로 1회만 허용 |

---

## v1 범위 밖 — 프로젝트 공고 변경 감지와 스냅샷 반영

v1에서는 프로젝트에 공고 스냅샷을 저장하지 않는다.
따라서 현재 공고와 전환 당시 공고값을 비교하거나, 변경분을 프로젝트 스냅샷에 반영하는 API도 제공하지 않는다.

범위 밖 API:

```text
GET /api/v1/projects/{projectId}/bid-notice/changes
PATCH /api/v1/projects/{projectId}/bid-notice-snapshot
```

추후 정정공고 변경 감지가 필요해지면 새 요구사항으로 아래를 함께 설계한다.

| 항목 | 필요 작업 |
|------|-----------|
| 저장소 | 프로젝트 또는 별도 테이블에 공고 스냅샷 필드 추가 |
| 전환 API | 같은 트랜잭션 안에서 공고 원본 조회 후 스냅샷 저장 |
| 비교 API | 저장된 스냅샷과 최신 `bid_notice` 비교 |
| 반영 API | 사용자가 승인한 필드만 스냅샷에 반영 |

---

## 프로젝트 입찰 블록 조회 API 폐기

v1에서는 입찰용 블록을 사용하지 않으므로 아래 API를 제공하지 않는다.

폐기 API:

```text
GET /api/v1/blocks/{blockId}/bid-notice
PUT /api/v1/blocks/{blockId}/bid-notice
PATCH /api/v1/blocks/{blockId}/bid-notice
DELETE /api/v1/blocks/{blockId}/bid-notice
```

입찰 상세는 `GET /api/v1/bidding/notices/{noticeId}` 또는 프로젝트의 `bid_notice_id`로 조회한 `bid_notice` 원본을 표시한다.
프로젝트 삭제·상태 변경이 필요하면 프로젝트 API 정책을 따르고, 이 경우에도 `bid_notice` 원본은 유지한다.
