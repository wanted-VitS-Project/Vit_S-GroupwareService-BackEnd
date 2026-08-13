# 입찰 관리 API 명세

**노션 원본**: 사용자 제공 노션 정리본 (링크 미제공)
**최종 동기화**: 2026-08-13 (검토 임시파일 보관 정책 변경 — 완료 후 고정 3시간 → 공고 입찰마감일시까지, 마감일 없으면 3시간 fallback)
**최종 동기화**: 2026-08-13 (문서 검토 Worker 401 코드를 `AUTH_UNAUTHENTICATED`로 통일 — 공용 `BiddingWorkerTokenAuthenticationFilter` 실제 동작과 일치)
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
| ✅ 확정 | 공고 제외 | PATCH | `/api/v1/bidding/notices/{noticeId}/dismiss` | `BIDDING` |
| ✅ 확정 | 공고 복구 | PATCH | `/api/v1/bidding/notices/{noticeId}/restore` | `BIDDING` |
| ✔️ 완료 | 입찰 AI 요약 요청 | POST | `/api/v1/bidding/notices/{noticeId}/summaries` | `BIDDING` |
| ✔️ 완료 | 공고별 입찰 AI 요약 이력 조회 | GET | `/api/v1/bidding/notices/{noticeId}/summaries` | `BIDDING` |
| ✅ 확정 | 입찰 AI 요약 조회 | GET | `/api/v1/bidding/summaries/{summaryId}` | `BIDDING` |
| ✅ 확정 | 입찰 AI 요약 수정 | PATCH | `/api/v1/bidding/summaries/{summaryId}` | `BIDDING` |
| ✅ 확정 | 입찰 AI 요약 확정 | PATCH | `/api/v1/bidding/summaries/{summaryId}/confirm` | `BIDDING` |
| ✔️ 완료 | Python 입찰 요약 작업 조회 | GET | `/internal/v1/bidding/summaries/{summaryId}/jobs/{attemptId}` | 내부 서버 |
| ✅ 확정 | Python 입찰 요약 결과 callback | POST | `/internal/v1/bidding/summaries/{summaryId}/callback` | 내부 서버 |
| ✅ 확정 | 입찰 문서 검토 요청 | POST | `/api/v1/bidding/notices/{noticeId}/reviews` | `BIDDING` |
| ✅ 확정 | 입찰 문서 검토 자료 조회 | GET | `/api/v1/bidding/notices/{noticeId}/review-sources` | `BIDDING` |
| ✅ 확정 | 입찰 기준자료 파일함 조회 | GET | `/api/v1/bidding/reference-files` | `BIDDING` |
| ✅ 확정 | 입찰 기준자료 업로드 시작 | POST | `/api/v1/bidding/reference-files/uploads` | `BIDDING` |
| ✅ 확정 | 입찰 기준자료 업로드 완료 | POST | `/api/v1/bidding/reference-files/uploads/{referenceFileId}/complete` | `BIDDING` |
| ✅ 확정 | 입찰 기준자료 삭제 | DELETE | `/api/v1/bidding/reference-files/{referenceFileId}` | `BIDDING` |
| ✅ 확정 | 공고별 입찰 문서 검토 이력 조회 | GET | `/api/v1/bidding/notices/{noticeId}/reviews` | `BIDDING` |
| ✅ 확정 | 입찰 문서 검토 조회 | GET | `/api/v1/bidding/reviews/{reviewId}` | `BIDDING` |
| ✅ 확정 | 입찰 문서 검토 포기 | PATCH | `/api/v1/bidding/reviews/{reviewId}/abandon` | `BIDDING` |
| ✅ 확정 | Python 입찰 문서 검토 작업 조회 | GET | `/internal/v1/bidding/reviews/{reviewId}/jobs/{attemptId}` | 내부 서버 |
| ✅ 확정 | Python 입찰 문서 검토 결과 callback | POST | `/internal/v1/bidding/reviews/{reviewId}/callback` | 내부 서버 |
| ✅ 확정 | 공고 프로젝트 전환 | POST | `/api/v1/bidding/notices/{noticeId}/projects` | `BIDDING` |

---

## 입찰 공고 프로젝트 전환 정책

v1에서는 입찰용 블록과 프로젝트 공고 스냅샷을 사용하지 않는다.
입찰 담당자가 AI 요약을 검토·확정한 뒤 별도 프로젝트 생성 명령을 실행한다.
프로젝트는 `project.bid_notice_id`로 공고를 연결하고, 확정 요약은
`bid_notice_summary.project_id`로 실제 생성된 프로젝트를 연결한다.

```text
bid_notice = 입찰 공고 원본
bid_notice_summary = 담당자가 확정한 AI 검토 결과와 생성된 project_id 보유
project = bid_notice_id로 공고 원본 연결
```

정책:

| 항목 | 규칙 |
|------|------|
| 블록 생성 | 생성하지 않는다 |
| 수동 생성 | 제공하지 않는다 |
| 프로젝트 생성 조건 | 현재 회사의 `COMPLETED`·확정 AI 요약을 선택해야 한다 |
| 프로젝트 연결 | `project.bid_notice_id`로 공고 원본을 연결하고 `bid_notice_summary.project_id`로 사용한 확정 요약을 고정한다 |
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
| `noticeStatus` | 회사별 공고 상태. `COLLECTED`, `DISMISSED`. 생략하면 `COLLECTED`로 조회하여 제외 공고를 일반 목록에서 숨긴다 |
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
| 첨부파일 | `attachments[].attachmentId`, `attachments[].attachmentOrder`, `attachments[].fileName`, `attachments[].sourceType`, `attachments[].supported` |

`dDay`는 DB에 저장하지 않고 서버에서 계산한다.

첨부파일은 삭제되지 않은 항목만 순서 오름차순으로 반환한다. 파일 크기는 수집 원천에 일관된 값이 없어 반환하지 않는다.
원본 `sourceUrl`은 프론트에 반환하지 않고 입찰 문서 검토 Worker의 내부 작업 조회에서만 사용한다.

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

**상태**: ✅ 확정

현재 회사의 공고 검토 상태만 변경한다. 공용 원본인 `bid_notice`는 수정하거나 삭제하지 않으며,
나라장터 수집 공고와 직접 등록 공고 모두 제외·복구할 수 있다.

| API | 설명 |
|-----|------|
| `PATCH /api/v1/bidding/notices/{noticeId}/dismiss` | 현재 회사의 공고를 검토 대상에서 제외 |
| `PATCH /api/v1/bidding/notices/{noticeId}/restore` | 현재 회사가 제외한 공고를 검토 대상으로 복구 |

### 공고 제외 `PATCH /api/v1/bidding/notices/{noticeId}/dismiss`

**Request Body**

```json
{
  "reason": "현재 회사의 사업 범위와 맞지 않는 공고입니다."
}
```

| 필드 | 타입 | 필수 | 규칙 |
|------|------|------|------|
| `reason` | String | Y | 공백 제외 1~500자 |

**Success Response**

```json
{
  "httpStatus": 200,
  "message": "입찰 공고 제외 성공",
  "data": {
    "noticeId": 1,
    "noticeStatus": "DISMISSED",
    "dismissReason": "현재 회사의 사업 범위와 맞지 않는 공고입니다.",
    "updatedAt": "2026-08-11T16:00:00"
  }
}
```

### 공고 복구 `PATCH /api/v1/bidding/notices/{noticeId}/restore`

Request Body는 없다.

**Success Response**

```json
{
  "httpStatus": 200,
  "message": "입찰 공고 복구 성공",
  "data": {
    "noticeId": 1,
    "noticeStatus": "COLLECTED",
    "dismissReason": null,
    "updatedAt": "2026-08-11T16:05:00"
  }
}
```

### 상태 및 이력 규칙

| 항목 | 규칙 |
|------|------|
| 회사 격리 | `company_bid_notice_state`에서 현재 회사의 행만 변경한다 |
| 제외 | `notice_status = DISMISSED`, `dismiss_reason` 저장 |
| 복구 | `notice_status = COLLECTED`, `dismiss_reason = null` |
| 공고 원본 | `bid_notice`의 공용 상태와 원본 데이터는 변경하지 않는다 |
| 이력 | 회사별 상태 변경 이력에 변경 전후 상태, 사유, 작업자, 변경 시각을 저장한다 |
| 삭제 | 영구 삭제 API는 제공하지 않는다 |

### Status Code

| HTTP | code | 적용 API | 설명 |
|------|------|----------|------|
| 200 | - | 제외·복구 | 처리 성공 |
| 400 | `BIDDING_INVALID_DISMISS_REASON` | 제외 | 제외 사유 누락 또는 형식 오류 |
| 401 | `AUTH_UNAUTHENTICATED` | 제외·복구 | 인증되지 않음 |
| 403 | `BIDDING_ACCESS_PERMISSION_REQUIRED` | 제외·복구 | 입찰 관리 권한 없음 |
| 404 | `BIDDING_NOTICE_NOT_FOUND` | 제외·복구 | 현재 회사에서 조회할 수 없는 공고 |
| 409 | `BIDDING_NOTICE_ALREADY_DISMISSED` | 제외 | 이미 제외된 공고 |
| 409 | `BIDDING_NOTICE_NOT_DISMISSED` | 복구 | 제외 상태가 아닌 공고 |

---

## 입찰 AI 요약

**상태**: ✅ 확정

입찰 AI 요약과 비타메이트 문서 검토는 별개의 기능이다. 입찰 AI 요약은 Spring Boot에 저장된 입찰 공고의 구조화 정보와 사용자가 직접 입력한 프롬프트를 기준으로 공고를 요약한다.

### 공통 정책

| 항목 | 규칙 |
|------|------|
| 공고 원본 | `bid_notice`는 전 회사가 공유하는 공공 원천 데이터로 유지한다 |
| 회사 격리 | AI 요약은 요청 시 인증 사용자의 `companyId`를 직접 저장한다. 다른 회사의 요약은 모든 상태에서 조회할 수 없다 |
| 개인 초안 | `PENDING`, `PROCESSING`, `FAILED`와 미확정 `COMPLETED` 요약은 요청자만 조회할 수 있다 |
| 수정·확정 권한 | 미확정 `COMPLETED` 요약은 최초 요청자만 수정하고 확정할 수 있다 |
| 회사 확정본 | 확정된 요약은 같은 회사의 `BIDDING` 권한 사용자가 조회하고 프로젝트 생성에 사용할 수 있다 |
| 분석 입력 | 공고 기본 정보, 금액, 일정, 참가 자격, 계약·평가 방식과 첨부파일 메타데이터를 사용한다 |
| 첨부 본문 | 입찰 AI 요약에서는 다운로드하거나 본문을 분석하지 않는다. 프로젝트 전 공고 첨부와 사내 문서 비교는 별도 입찰 문서 검토 API가 담당한다 |
| 사용자 프롬프트 | 사용자가 `prompt`를 직접 입력한다. 서버 기본 업무 프롬프트나 도메인별 프롬프트 템플릿을 앞뒤에 추가하지 않는다 |
| 출력 형식 | Python worker는 사용자 프롬프트를 바꾸지 않고 Gemini 구조화 응답 스키마로 결과 필드만 구분한다 |
| 안전 정책 | 인증, 회사 격리, 입력 데이터 경계, 민감 정보 차단과 출력 형식 검증은 사용자 프롬프트와 분리된 실행 정책으로 적용한다 |
| 처리 방식 | Spring Boot가 DB Outbox와 Redis Stream으로 작업을 발행하고 Python worker가 Gemini 호출 후 callback한다 |
| 이력 | 요약 요청마다 새 `summaryId`를 생성하며 이전 완료 이력을 덮어쓰지 않는다 |
| 개선 요청 | 본인이 만든 같은 공고의 미확정 `COMPLETED` 요약을 `baseSummaryId`로 선택해 새 개선 요약을 요청할 수 있다 |
| 개정 계보 | 최초 요청은 `parentSummaryId = null`, `revisionNo = 1`이며 개선 요청은 기준 요약을 부모로 연결하고 개정 번호를 1 증가시킨다 |
| 개정 상한 | 하나의 개선 계보는 최대 20차까지 허용한다 |
| 중복 실행 | 같은 회사·공고·요청자에 `PENDING` 또는 `PROCESSING` 요약이 있으면 그 요청자의 새 요청을 거부한다 |
| 확정 | `COMPLETED`인 본인 미확정 요약만 확정할 수 있다. 확정 후에는 변경하지 않고 재분석 시 새 요약을 생성한다 |
| 프로젝트 전환 | 확정과 프로젝트 생성은 분리한다. 확정된 요약을 선택해 프로젝트 생성 API를 명시적으로 호출한다 |
| 프로젝트 근거 고정 | 프로젝트 생성에 사용한 확정 요약은 `projectId`로 연결하며 한 프로젝트에는 하나의 확정 요약만 연결한다 |

### 상태값

| 상태 | 설명 |
|------|------|
| `PENDING` | 요청과 Outbox가 저장되어 worker 처리를 기다리는 상태 |
| `PROCESSING` | 현재 `attemptId`를 가진 worker가 분석 중인 상태 |
| `COMPLETED` | Gemini 결과가 정상 저장되어 사용자 검토가 가능한 상태 |
| `FAILED` | 재시도 불가능하거나 최대 재시도를 초과하여 종료된 상태 |

`summaryStatus`와 사용자 확정 여부인 `confirmed`는 별도로 관리한다.

---

## 입찰 AI 요약 요청 `POST /api/v1/bidding/notices/{noticeId}/summaries`

**상태**: ✅ 확정

현재 회사가 조회할 수 있는 입찰 공고와 사용자가 입력한 프롬프트를 기준으로 비동기 AI 요약을 요청한다.

### Path Parameter

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `noticeId` | Long | Y | 요약할 입찰 공고 ID |

### Request Body

| 필드 | 타입 | 필수 | 규칙 |
|------|------|------|------|
| `prompt` | String | Y | 사용자가 직접 입력하는 분석 요청. 공백 불가, 최대 3,000자 |
| `baseSummaryId` | Long | N | 개선 기준이 되는 본인의 같은 공고 미확정 `COMPLETED` 요약 ID |

```json
{
  "prompt": "기존 요약을 유지하되 보안 인증과 일정 위험을 더 구체적으로 정리해줘.",
  "baseSummaryId": 31
}
```

### Success Response

```json
{
  "httpStatus": 202,
  "message": "입찰 공고 AI 요약 요청이 접수되었습니다.",
  "data": {
    "summaryId": 31,
    "summaryStatus": "PENDING",
    "requestedAt": "2026-08-11T17:30:00"
  }
}
```

### 처리 규칙

1. 인증 사용자의 회사와 `BIDDING` 권한을 확인한다.
2. 현재 회사가 조회할 수 있는 공고인지 확인한다.
3. 같은 회사·공고·요청자에 진행 중인 요약이 있는지 확인한다.
4. `baseSummaryId`가 있으면 본인의 같은 공고 미확정 `COMPLETED` 요약인지 확인하고 최대 20차 제한을 검증한다.
5. `bid_notice_summary(PENDING)`과 요약 요청 Outbox를 같은 DB 트랜잭션에서 저장한다.
6. 커밋 이후 Outbox Dispatcher가 Redis Stream에 작업을 발행한다.

### Status Code

| HTTP | code | 설명 |
|------|------|------|
| 202 | - | 요약 요청 접수 성공 |
| 400 | `BIDDING_INVALID_SUMMARY_REQUEST` | 공고 ID 또는 프롬프트 형식 오류 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션이 없거나 만료됨 |
| 403 | `BIDDING_ACCESS_PERMISSION_REQUIRED` | 입찰 관리 권한 없음 |
| 404 | `BIDDING_NOTICE_NOT_FOUND` | 현재 회사에서 조회할 수 있는 공고가 없음 |
| 404 | `BIDDING_SUMMARY_NOT_FOUND` | 기준 요약이 없거나 다른 회사·공고·요청자의 요약임 |
| 409 | `BIDDING_SUMMARY_ALREADY_PROCESSING` | 같은 회사·공고·요청자에 진행 중인 요약이 있음 |
| 409 | `BIDDING_SUMMARY_NOT_EDITABLE` | 기준 요약이 미완료·확정 상태이거나 이미 20차임 |

---

## 공고별 입찰 AI 요약 이력 조회 `GET /api/v1/bidding/notices/{noticeId}/summaries`

**상태**: ✅ 확정

현재 사용자가 요청한 모든 상태의 요약과 같은 회사에서 확정된 요약을 최신순으로 조회한다.
다른 사용자의 미확정 요약은 반환하지 않는다.

### Query Parameter

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | Integer | N | `0` | 0부터 시작하는 페이지 번호 |
| `size` | Integer | N | `20` | 페이지 크기. 최대 50 |

### Response Data

| 필드 | 타입 | 설명 |
|------|------|------|
| `content[].summaryId` | Long | AI 요약 ID |
| `content[].parentSummaryId` | Long | 개선 기준 요약 ID. 최초 요청이면 `null` |
| `content[].revisionNo` | Integer | 개선 계보의 개정 번호. 최초 요청은 `1` |
| `content[].summaryStatus` | String | 처리 상태 |
| `content[].prompt` | String | 요청 당시 프롬프트 |
| `content[].confirmed` | Boolean | 확정 여부 |
| `content[].isMine` | Boolean | 현재 사용자가 요청한 요약인지 여부 |
| `content[].projectId` | Long | 이 확정 요약으로 생성한 프로젝트 ID. 미전환이면 `null` |
| `content[].requestedAt` | String | 요청 시각 |
| `content[].confirmedAt` | String | 확정 시각. 미확정이면 `null` |
| `latestMySummaryId` | Long | 현재 사용자가 요청한 요약 중 가장 최신 ID. 없으면 `null` |
| `totalElements` | Long | 조회 가능한 전체 요약 수 |
| `totalPages` | Integer | 전체 페이지 수 |
| `page` | Integer | 현재 페이지 |
| `size` | Integer | 페이지 크기 |

조회 대상은 `(requested_by = 현재 사용자) OR (company_id = 현재 회사 AND confirmed = TRUE)`로 제한한다.

### Status Code

| HTTP | code | 설명 |
|------|------|------|
| 200 | - | 이력 조회 성공 |
| 400 | `BIDDING_INVALID_SUMMARY_REQUEST` | 공고 ID 또는 페이징 값 오류 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션이 없거나 만료됨 |
| 403 | `BIDDING_ACCESS_PERMISSION_REQUIRED` | 입찰 관리 권한 없음 |
| 404 | `BIDDING_NOTICE_NOT_FOUND` | 현재 회사에서 조회할 수 있는 공고가 없음 |

---

## 입찰 AI 요약 조회 `GET /api/v1/bidding/summaries/{summaryId}`

**상태**: ✅ 확정

본인이 요청한 요약 또는 같은 회사에서 확정된 요약의 처리 상태, 구조화 결과와 프로젝트 전환 상태를 조회한다.

### Response Data

| 필드 | 타입 | null 가능 | 설명 |
|------|------|-----------|------|
| `summaryId` | Long | N | AI 요약 ID |
| `noticeId` | Long | N | 입찰 공고 ID |
| `parentSummaryId` | Long | Y | 개선 기준 요약 ID. 최초 요청이면 `null` |
| `revisionNo` | Integer | N | 개선 계보의 개정 번호. 최초 요청은 `1` |
| `prompt` | String | N | 요청 당시 사용자가 입력한 프롬프트 원문 |
| `summaryStatus` | String | N | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` |
| `overviewSummary` | String | Y | 공고 개요. 완료 전 또는 근거 부족 시 `null` |
| `amountSummary` | String | Y | 금액 요약 |
| `scheduleSummary` | String | Y | 일정 요약 |
| `qualificationSummary` | String | Y | 참가 자격 요약 |
| `taskSummary` | String | Y | 주요 과업 요약 |
| `riskSummary` | String | Y | 위험 요소 요약 |
| `confirmed` | Boolean | N | 사용자 최종 확정 여부 |
| `confirmedBy` | String | Y | 확정 사용자 ID. 미확정이면 `null` |
| `confirmedAt` | String | Y | 확정 시각. 미확정이면 `null` |
| `projectId` | Long | Y | 이 확정 요약으로 생성한 프로젝트 ID. 미전환이면 `null` |
| `errorMessage` | String | Y | 실패 사유. `FAILED`가 아니면 `null` |
| `requestedAt` | String | N | 요청 생성 시각 |
| `completedAt` | String | Y | 분석 완료 또는 실패 시각 |
| `updatedAt` | String | Y | 사용자가 요약 결과를 마지막으로 수정한 시각 |

```json
{
  "httpStatus": 200,
  "message": "입찰 공고 AI 요약 조회 성공",
  "data": {
    "summaryId": 31,
    "noticeId": 317,
    "prompt": "이 공고의 금액, 일정, 참가 자격과 수행 위험을 실무 검토용으로 정리해줘.",
    "summaryStatus": "COMPLETED",
    "overviewSummary": "스마트시티 통합관제 플랫폼 구축 용역입니다.",
    "amountSummary": "추정가격은 3억 3천만 원입니다.",
    "scheduleSummary": "입찰 마감은 2026-08-20 18:00입니다.",
    "qualificationSummary": "관련 사업 수행 실적을 보유한 소프트웨어사업자가 대상입니다.",
    "taskSummary": "통합관제 플랫폼 구축과 운영 체계 수립이 주요 과업입니다.",
    "riskSummary": "입찰 마감까지 준비 기간이 짧고 관련 실적 증빙이 필요합니다.",
    "confirmed": false,
    "confirmedBy": null,
    "confirmedAt": null,
    "projectId": null,
    "errorMessage": null,
    "requestedAt": "2026-08-11T17:30:00",
    "completedAt": "2026-08-11T17:30:12",
    "updatedAt": null
  }
}
```

### Status Code

| HTTP | code | 설명 |
|------|------|------|
| 200 | - | 요약 조회 성공 |
| 400 | `BIDDING_INVALID_SUMMARY_REQUEST` | 유효하지 않은 요약 ID |
| 401 | `AUTH_UNAUTHENTICATED` | 세션이 없거나 만료됨 |
| 403 | `BIDDING_ACCESS_PERMISSION_REQUIRED` | 입찰 관리 권한 없음 |
| 404 | `BIDDING_SUMMARY_NOT_FOUND` | 본인 요약 또는 현재 회사의 확정 요약이 존재하지 않음 |

---

## 입찰 AI 요약 수정 `PATCH /api/v1/bidding/summaries/{summaryId}`

**상태**: ✅ 확정

AI가 생성한 구조화 결과를 요청자가 검토하여 부분 수정한다. 본인이 요청한 `COMPLETED`이면서 미확정인 요약만 수정할 수 있다.

### Request Body

아래 필드는 모두 선택이며 최소 한 개를 전달해야 한다. 생략한 필드는 유지하고, 전달한 필드는 공백이 아닌 문자열로 교체한다. `null`로 필드를 삭제하는 것은 허용하지 않는다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `overviewSummary` | String | 공고 개요 수정값 |
| `amountSummary` | String | 금액 요약 수정값 |
| `scheduleSummary` | String | 일정 요약 수정값 |
| `qualificationSummary` | String | 참가 자격 수정값 |
| `taskSummary` | String | 주요 과업 수정값 |
| `riskSummary` | String | 위험 요소 수정값 |

```json
{
  "riskSummary": "실적 증빙과 보안 인증 자료를 입찰 마감 전에 확보해야 합니다.",
  "taskSummary": "관제 플랫폼 구축, 데이터 연계와 운영자 교육이 주요 과업입니다."
}
```

성공 응답의 `data`는 AI 요약 조회 응답과 동일하다.

### Status Code

| HTTP | code | 설명 |
|------|------|------|
| 200 | - | 요약 수정 성공 |
| 400 | `BIDDING_INVALID_SUMMARY_UPDATE` | 수정 필드가 없거나 값 형식이 올바르지 않음 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션이 없거나 만료됨 |
| 403 | `BIDDING_ACCESS_PERMISSION_REQUIRED` | 입찰 관리 권한 없음 |
| 404 | `BIDDING_SUMMARY_NOT_FOUND` | 현재 회사의 요약이 존재하지 않음 |
| 409 | `BIDDING_SUMMARY_NOT_EDITABLE` | 분석 미완료, 실패 또는 이미 확정되어 수정할 수 없음 |

---

## 입찰 AI 요약 확정 `PATCH /api/v1/bidding/summaries/{summaryId}/confirm`

**상태**: ✅ 확정

요청자의 검토가 끝난 AI 요약을 최종 확정한다. Request Body는 없다.
확정 성공 후 프로젝트 생성이 가능해지지만 프로젝트를 자동으로 생성하지는 않는다.

```json
{
  "httpStatus": 200,
  "message": "입찰 공고 AI 요약 확정 성공",
  "data": {
    "summaryId": 31,
    "confirmed": true,
    "confirmedBy": "vitas-USER001",
    "confirmedAt": "2026-08-11T17:40:00",
    "projectCreationAllowed": true
  }
}
```

### Status Code

| HTTP | code | 설명 |
|------|------|------|
| 200 | - | 요약 확정 성공 |
| 400 | `BIDDING_INVALID_SUMMARY_REQUEST` | 유효하지 않은 요약 ID |
| 401 | `AUTH_UNAUTHENTICATED` | 세션이 없거나 만료됨 |
| 403 | `BIDDING_ACCESS_PERMISSION_REQUIRED` | 입찰 관리 권한 없음 |
| 404 | `BIDDING_SUMMARY_NOT_FOUND` | 현재 회사의 요약이 존재하지 않음 |
| 409 | `BIDDING_SUMMARY_NOT_COMPLETED` | 완료되지 않은 요약을 확정하려고 함 |
| 409 | `BIDDING_SUMMARY_ALREADY_CONFIRMED` | 이미 확정된 요약 |

---

## Python 입찰 요약 작업 조회 `GET /internal/v1/bidding/summaries/{summaryId}/jobs/{attemptId}`

**상태**: ✅ 확정

Python worker가 Redis 작업을 받은 뒤 현재 시도와 분석 입력을 Spring Boot에서 조회한다.

### 인증

| 항목 | 규칙 |
|------|------|
| Header | `X-Bidding-Worker-Token` |
| 환경변수 | Spring Boot와 Python worker에 `BIDDING_WORKER_TOKEN`으로 주입한다 |
| 전송 | 배포 환경에서는 HTTPS와 인증서 검증을 강제하며 redirect 요청에는 토큰을 전달하지 않는다 |

### Response Data

| 필드 | 타입 | 설명 |
|------|------|------|
| `summaryId` | Long | AI 요약 ID |
| `companyId` | Long | 회사 ID. worker 로그에는 노출하지 않는다 |
| `attemptId` | String | 현재 작업 시도 ID |
| `prompt` | String | 사용자가 입력한 프롬프트 원문 |
| `previousSummary` | Object | 개선 기준 요약의 구조화 결과. 최초 요청이면 `null` |
| `previousSummary.summaryId` | Long | 개선 기준 요약 ID |
| `previousSummary.revisionNo` | Integer | 개선 기준 요약의 개정 번호 |
| `previousSummary.overviewSummary` | String | 이전 공고 개요 |
| `previousSummary.amountSummary` | String | 이전 금액 요약 |
| `previousSummary.scheduleSummary` | String | 이전 일정 요약 |
| `previousSummary.qualificationSummary` | String | 이전 참가 자격 요약 |
| `previousSummary.taskSummary` | String | 이전 주요 과업 요약 |
| `previousSummary.riskSummary` | String | 이전 위험 요소 요약 |
| `notice` | Object | 요청 시점의 입찰 공고 구조화 스냅샷 |
| `notice.attachments` | List&lt;Object&gt; | 파일명과 원문 URL 등 첨부 메타데이터. 본문은 포함하지 않는다 |

Spring Boot는 `summaryId`, `companyId`, `attemptId`가 현재 처리 대상과 모두 일치할 때만 작업 입력을 반환한다.

### Status Code

| HTTP | code | 설명 |
|------|------|------|
| 200 | - | 작업 입력 조회 성공 |
| 400 | `BIDDING_INVALID_SUMMARY_JOB_REQUEST` | 경로 값 또는 작업 시도 형식 오류 |
| 401 | `AUTH_UNAUTHENTICATED` | worker 토큰 누락 또는 불일치 |
| 404 | `BIDDING_SUMMARY_JOB_NOT_FOUND` | 현재 시도와 일치하는 작업이 없음 |

---

## Python 입찰 요약 결과 callback `POST /internal/v1/bidding/summaries/{summaryId}/callback`

**상태**: ✅ 확정

Python worker가 Gemini 처리 결과를 Spring Boot에 저장한다.

### 인증

`X-Bidding-Worker-Token`을 사용하며 작업 조회 API와 같은 전송 보안 규칙을 따른다.

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `attemptId` | String | Y | Redis 작업과 작업 조회 응답에서 받은 현재 시도 ID |
| `summaryStatus` | String | Y | `COMPLETED` 또는 `FAILED` |
| `overviewSummary` | String | 조건부 | `COMPLETED` 결과 |
| `amountSummary` | String | 조건부 | `COMPLETED` 결과. 근거가 없으면 `null` |
| `scheduleSummary` | String | 조건부 | `COMPLETED` 결과. 근거가 없으면 `null` |
| `qualificationSummary` | String | 조건부 | `COMPLETED` 결과. 근거가 없으면 `null` |
| `taskSummary` | String | 조건부 | `COMPLETED` 결과. 근거가 없으면 `null` |
| `riskSummary` | String | 조건부 | `COMPLETED` 결과. 근거가 없으면 `null` |
| `errorMessage` | String | 조건부 | `FAILED`이면 필수. 민감 정보와 외부 응답 원문 금지 |
| `retryable` | Boolean | N | `FAILED` 오류의 일시 장애 여부. 생략 시 `false`로 처리하며 `COMPLETED`에서는 `false`여야 함 |

`COMPLETED`에서는 `overviewSummary`가 필수이고 `errorMessage`는 `null`, `retryable`은 `false`여야 한다. `FAILED`에서는 모든 결과 필드가 `null`이고 `errorMessage`가 필수다.

```json
{
  "attemptId": "4b0f03bb-c04d-4ff0-997b-3ff762cbfe22",
  "summaryStatus": "COMPLETED",
  "overviewSummary": "스마트시티 통합관제 플랫폼 구축 용역입니다.",
  "amountSummary": "추정가격은 3억 3천만 원입니다.",
  "scheduleSummary": "입찰 마감은 2026-08-20 18:00입니다.",
  "qualificationSummary": "관련 사업 수행 실적 보유 업체가 대상입니다.",
  "taskSummary": "통합관제 플랫폼 구축과 운영 체계 수립이 주요 과업입니다.",
  "riskSummary": "관련 실적과 보안 인증 증빙 준비가 필요합니다.",
  "errorMessage": null,
  "retryable": false
}
```

일시 장애 callback 예시:

```json
{
  "attemptId": "4b0f03bb-c04d-4ff0-997b-3ff762cbfe22",
  "summaryStatus": "FAILED",
  "overviewSummary": null,
  "amountSummary": null,
  "scheduleSummary": null,
  "qualificationSummary": null,
  "taskSummary": null,
  "riskSummary": null,
  "errorMessage": "AI provider temporarily unavailable",
  "retryable": true
}
```

### Success Response

```json
{
  "accepted": true,
  "summaryId": 31,
  "summaryStatus": "COMPLETED",
  "reason": null
}
```

오래된 `attemptId`, 중복 완료 callback 또는 이미 확정된 요약에는 HTTP 200과 `accepted=false`를 반환한다. Python worker는 이를 정상적인 멱등 처리로 보고 Redis 메시지를 ACK한다.

### Status Code

| HTTP | code | 설명 |
|------|------|------|
| 200 | - | callback 처리 성공 또는 오래된 결과의 멱등 거절 |
| 400 | `BIDDING_INVALID_SUMMARY_CALLBACK` | 상태별 필드 규칙 또는 요청 형식 오류 |
| 401 | `AUTH_UNAUTHENTICATED` | worker 토큰 누락 또는 불일치 |
| 404 | `BIDDING_SUMMARY_NOT_FOUND` | 요약 자체가 존재하지 않음 |

### 비동기 신뢰성 규칙

| 항목 | 규칙 |
|------|------|
| 요청 원자성 | 요약 `PENDING` 저장과 Outbox 저장은 같은 DB 트랜잭션에서 처리한다 |
| 메시지 | Redis에는 `summaryId`, `companyId`, `attemptId`, `retryCount`만 전달한다 |
| 작업 점유 | worker가 작업 조회에 성공하면 Spring Boot가 현재 요약을 `PROCESSING`으로 전이한다 |
| callback 검증 | 현재 저장된 `attemptId`와 일치하는 결과만 반영한다 |
| 총 시도 횟수 | 최초 1회와 재시도 2회를 합쳐 최대 3회 처리한다 |
| 재시도 대상 | Gemini HTTP 429·5xx, Timeout, 일시적 네트워크 오류만 `retryable=true`로 전달한다 |
| 즉시 종료 대상 | 잘못된 API 키·요청·설정 오류는 `retryable=false`로 전달하고 `FAILED`로 종료한다 |
| 재시도 간격 | 첫 실패 후 10초, 두 번째 실패 후 30초 뒤 Outbox 발행이 가능하다 |
| 새 시도 식별자 | 재시도마다 Spring Boot가 새 `attemptId`를 발급하고 이전 callback은 `accepted=false`로 거절한다 |
| 재시도 원자성 | 현재 요약의 `PENDING` 전이와 새 attempt Outbox 저장은 같은 DB 트랜잭션에서 처리한다 |
| 최종 실패 | 복구 불가능한 오류 또는 최대 재시도 초과 시 `FAILED` callback으로 종료한다 |
| ACK | callback에서 `accepted=true` 또는 멱등 거절 응답을 받은 뒤 Redis 메시지를 ACK한다 |
| 로그 | `summaryId`, `attemptId`, 상태와 단계만 기록하며 프롬프트·공고 원문·회사 ID·토큰은 기록하지 않는다 |

---

## 입찰 문서 비교 검토

**상태**: ✅ 확정

입찰 문서 비교 검토는 공고의 구조화 정보만 사용하는 입찰 AI 요약과 별개의 기능이다.
사용자가 선택한 공고 첨부파일을 검토 시작 시점에 내려받고, 선택한 사내 문서를 기준 자료로 사용하여
참가 조건, 인력·자격·실적, 재무 조건, 일정과 수행 위험을 비교한다.

### 공통 정책

| 항목 | 규칙 |
|------|------|
| 회사 격리 | `bid_review.company_id`를 요청 시점에 저장하며 다른 회사의 검토는 조회하거나 사용할 수 없다 |
| 요청자 권한 | 요청·조회·포기 모두 현재 회사의 `BIDDING` 권한이 필요하다 |
| 공고 첨부 표시 | 검토 전에는 `attachmentId`, `fileName`, `sourceType`과 선택 여부 판단에 필요한 메타데이터만 프론트에 반환한다 |
| 원본 URL | 프론트에 반환하지 않는다. Spring DB에 보관하고 내부 Worker 작업 조회에서만 전달한다 |
| 실제 다운로드 | 사용자가 검토 요청 API를 호출한 뒤 Python Worker가 선택된 `bidAttachmentIds`만 다운로드한다 |
| 사내 기준자료 | 입찰 도메인의 회사별 기준자료 파일함(`bid_reference_file`)에 영구 보관한다. 프로젝트에 귀속되지 않으며 동일 회사의 여러 공고 검토에서 재사용할 수 있다 |
| 사내 문서함 참조 (2026-08-13 추가) | 별도 업로드 없이 사내 문서함(`companydocument`)의 완료된 최신 버전을 버전 고정(`companyDocumentVersionId`)으로 참조한다. 조회는 `CompanyDocumentReferenceUseCase`(김동현 소유 포트)로만 하며 직접 SQL 조회는 금지한다. 사내 기준자료 파일함과 병행 운영하며, FE 전환이 확인되면 기존 파일함 API를 단계적으로 폐기할 예정이다(`.ai/local/STATE.md` 참고) |
| 문서 역할 | 공고 첨부는 `BID_ATTACHMENT`, 사내 기준자료 파일함 문서는 `INTERNAL_REFERENCE`, 사내 문서함 참조는 `COMPANY_DOCUMENT_REFERENCE`로 저장한다 |
| 기준자료 저장 | 기준자료 메타데이터는 `bid_reference_file`, 바이너리는 회사별 S3 prefix에 저장하며 `storageKey`를 외부 응답에 노출하지 않는다. 사내 문서함 참조는 `company_document_version_id`만 저장하고 바이너리는 `companydocument`가 소유한다 |
| 임시 저장 | 공고 첨부 원본은 프로젝트 파일과 분리된 임시 저장소 prefix에 저장한다. 임시 `storageKey`는 외부 응답에 노출하지 않는다 |
| 보관 시간(2026-08-13 정책 변경) | 검토가 `COMPLETED` 또는 `FAILED`로 종료되면 **해당 공고의 입찰마감일시(`bid_notice.bid_deadline_at`)까지** 보관한다. 마감일시가 없거나(NULL) 이미 종료 시각보다 과거면 종전 정책(종료 시각 + 3시간)으로 되돌아간다. 처리 중에는 만료 정리하지 않는다 |
| 화면 이탈 | 뒤로 가기나 브라우저 종료는 별도 요청을 보내지 않는다. 임시파일은 만료 시각까지 유지한다 |
| 검토 포기 | 포기 API는 검토만 `ABANDONED`로 전이하고 정리 작업을 즉시 요청한다. 입찰 공고 자체는 제외하거나 삭제하지 않는다 |
| 프로젝트 귀속 | 프로젝트 생성 요청에 `reviewId`가 있으면 해당 검토에서 실제 사용한 공고 첨부만 파일 도메인으로 정식 귀속한다 |
| 정리 실패 | 임시 객체와 파생 데이터 삭제는 재시도하며 최종 실패 작업은 DLQ로 분리한다 |
| 결과 근거 | 결과는 문서명, 페이지 또는 시트, 발췌문을 citation으로 남긴다. 근거가 없으면 추정으로 단정하지 않는다 |
| 처리 방식 | 검토 요청과 Outbox를 같은 DB 트랜잭션에 저장하고 Redis Stream을 통해 Python Worker가 비동기로 처리한다 |
| 시도 격리 | 매 처리 시도마다 UUID 형식 `attemptId`를 발급하며 현재 시도와 다른 callback은 저장하지 않는다 |

### 입력 제한과 다운로드 안전 정책

| 항목 | 규칙 |
|------|------|
| 공고 첨부 개수 | 1개 이상 10개 이하 |
| 사내 기준자료 개수 | 0개 이상 10개 이하 |
| 사내 문서함 참조 개수 (2026-08-13 추가) | 0개 이상 10개 이하. 사내 기준자료 개수와 별도 상한(합산 제한 없음) |
| 전체 문서 개수 | 최대 30개(공고 첨부 10 + 사내 기준자료 10 + 사내 문서함 참조 10, 2026-08-13 20→30 조정) |
| 프롬프트 | 공백 불가, 최대 3,000자 |
| 지원 확장자 | `pdf`, `docx`, `xlsx`, `csv`, `txt`, `hwp`, `hwpx` |
| 파일 크기 | 파일당 최대 50MB, 공고 첨부 합계 최대 200MB |
| URL 검증 | `http` 또는 `https`만 허용하고 loopback, private, link-local 주소를 차단한다. redirect 대상도 매번 다시 검증한다 |
| 다운로드 제한 | 연결·응답 timeout과 최대 응답 크기를 강제하고 파일명은 경로 문자를 제거하여 저장한다 |
| 미지원·손상 파일 | 해당 문서를 실패로 기록한다. 선택한 공고 첨부를 하나도 분석할 수 없으면 검토 전체를 `FAILED`로 종료한다 |

### 상태값

| 상태 | 설명 |
|------|------|
| `PENDING` | 검토 요청과 Outbox가 저장되어 Worker 처리를 기다림 |
| `PROCESSING` | 현재 `attemptId`의 Worker가 다운로드·추출·비교 중 |
| `COMPLETED` | 비교 결과와 citation 저장 완료 |
| `FAILED` | 복구 불가능한 오류 또는 재시도 초과로 종료 |
| `ABANDONED` | 사용자가 검토를 포기하여 즉시 정리 대상으로 전환 |
| `EXPIRED` | 프로젝트로 귀속되지 않고 보관 시간이 지나 정리 완료 |

문서 처리 상태는 `PENDING`, `DOWNLOADING`, `READY`, `FAILED`, `PROMOTED`, `DELETED`를 사용한다.

---

## 입찰 문서 검토 자료 조회 `GET /api/v1/bidding/notices/{noticeId}/review-sources`

**상태**: ✅ 확정

검토 화면에 표시할 공고 첨부파일 메타데이터를 조회한다. 사내 기준자료 목록은
`GET /api/v1/bidding/reference-files`를 별도로 호출하며 이 API에서 합쳐 반환하지 않는다.

### Response Data

| 필드 | 타입 | 설명 |
|------|------|------|
| `noticeId` | Long | 입찰 공고 ID |
| `attachments` | List | 삭제되지 않은 공고 첨부 목록 |
| `attachments[].attachmentId` | Long | 검토 요청에서 사용하는 첨부 ID |
| `attachments[].fileName` | String | 화면 표시용 원본 파일명 |
| `attachments[].sourceType` | String | `NARA` 또는 `MANUAL` 등 수집 출처 코드 |
| `attachments[].supported` | Boolean | 현재 문서 추출 지원 형식 여부 |

`sourceUrl`, 인증 정보와 임시 저장 키는 반환하지 않는다.

### Status Code

| HTTP | code | 설명 |
|------|------|------|
| 200 | - | 조회 성공 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션이 없거나 만료됨 |
| 403 | `BIDDING_ACCESS_PERMISSION_REQUIRED` | 입찰 관리 권한 없음 |
| 404 | `BIDDING_NOTICE_NOT_FOUND` | 현재 회사에서 조회할 수 있는 공고가 없음 |

---

## 입찰 기준자료 파일함 조회 `GET /api/v1/bidding/reference-files`

**상태**: ✅ 확정

현재 회사가 입찰 문서 검토에 반복 사용할 기준자료를 조회한다. 다른 회사의 파일과 삭제된 파일은 반환하지 않는다.

### Response Data

| 필드 | 타입 | 설명 |
|------|------|------|
| `referenceFileId` | Long | 검토 요청에서 사용할 기준자료 ID |
| `fileName` | String | 원본 파일명 |
| `extension` | String | 소문자 확장자 |
| `mimeType` | String | 업로드 시 확인한 MIME 타입 |
| `sizeBytes` | Long | 파일 크기 |
| `uploadStatus` | String | `UPLOADING`, `COMPLETED`, `FAILED` |
| `indexStatus` | String | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` |
| `selectable` | Boolean | 업로드와 인덱싱이 모두 완료되어 검토에 사용할 수 있는지 |
| `createdAt` | LocalDateTime | 등록 시각 |

---

## 입찰 기준자료 업로드 시작 `POST /api/v1/bidding/reference-files/uploads`

**상태**: ✅ 확정

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `fileName` | String | Y | 경로 문자를 제거한 원본 파일명, 최대 255자 |
| `mimeType` | String | Y | 파일 MIME 타입, 최대 100자 |
| `sizeBytes` | Long | Y | 1바이트 이상 50MB 이하 |

### Success Response

`201 Created`로 `referenceFileId`, 10분 유효한 `uploadUrl`, `expiresAt`을 반환한다. 클라이언트는 `uploadUrl`로 바이너리를 직접 `PUT`한 뒤 완료 API를 호출한다.

---

## 입찰 기준자료 업로드 완료 `POST /api/v1/bidding/reference-files/uploads/{referenceFileId}/complete`

**상태**: ✅ 확정

Request Body는 없다. Spring은 저장소 객체의 존재와 크기를 확인한 뒤 `uploadStatus=COMPLETED`, `indexStatus=PENDING`으로 저장하고 비동기 인덱싱 Outbox를 생성한다.

### Success Response

`200 OK`로 `referenceFileId`, `fileName`, `uploadStatus`, `indexStatus`, `completedAt`을 반환한다.

---

## 입찰 기준자료 삭제 `DELETE /api/v1/bidding/reference-files/{referenceFileId}`

**상태**: ✅ 확정

현재 회사의 기준자료를 삭제한다. `PENDING` 또는 `PROCESSING` 검토가 해당 파일을 사용 중이면 `409 BIDDING_REFERENCE_FILE_IN_USE`로 거절한다. 삭제가 허용되면 DB 논리 삭제와 S3·임베딩 파생 데이터 정리 Outbox를 같은 트랜잭션에 저장한다. 기존 완료 검토의 문서명·결과·citation 스냅샷은 유지한다.

---

## 입찰 문서 검토 요청 `POST /api/v1/bidding/notices/{noticeId}/reviews`

**상태**: ✅ 확정 (2026-08-13 추가 — `companyDocumentVersionIds` 신설. 사내 문서함(`companydocument`)
참조 선택 새 경로 병행 지원. `referenceFileIds`(기존 `bid_reference_file` 파일함)는 폐기 전까지 그대로 유지 —
FE가 새 경로로 전환했는지 확인되면 별도 PR로 폐기한다. FE에 신규 필드 추가를 공유해야 한다.)

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `bidAttachmentIds` | Long[] | Y | 검토할 공고 첨부 ID. 1~10개, 중복 불가 |
| `referenceFileIds` | Long[] | N | 비교 기준으로 사용할 입찰 기준자료(`bid_reference_file`) ID. 최대 10개, 중복 불가 |
| `companyDocumentVersionIds` | Long[] | N | 비교 기준으로 사용할 사내 문서함 참조 버전(`company_document_version`) ID. 최대 10개, 중복 불가. `GET /api/v1/company-documents/selectable`로 조회한 `companyDocumentVersionId`를 사용 |
| `prompt` | String | Y | 사용자가 직접 입력한 검토 지시. 공백 불가, 최대 3,000자 |

```json
{
  "bidAttachmentIds": [31, 32],
  "referenceFileIds": [501, 502],
  "companyDocumentVersionIds": [9001, 9002],
  "prompt": "우리 회사의 재정 상태와 보유 인력으로 수행 가능한지, 부족한 자격과 실적을 근거와 함께 검토해줘."
}
```

### Success Response

```json
{
  "httpStatus": 202,
  "message": "입찰 문서 검토 요청이 접수되었습니다.",
  "data": {
    "reviewId": 71,
    "reviewStatus": "PENDING",
    "requestedAt": "2026-08-12T14:00:00"
  }
}
```

### 처리 규칙

1. 공고와 선택 첨부가 현재 회사에서 조회 가능한 활성 데이터인지 검증한다.
2. 입찰 기준자료(`bid_reference_file`)가 현재 회사에 속하고 업로드와 인덱싱이 완료됐는지 검증한다.
3. 사내 문서함 참조(`companyDocumentVersionIds`)가 현재 회사의 참조 대상으로 유효한지 검증한다(완료 최신 버전만, `CompanyDocumentReferenceUseCase` 경유 — 직접 SQL 조회 금지).
4. 검토 요청, 선택 문서 스냅샷과 Outbox를 같은 DB 트랜잭션에서 저장한다.
5. Worker가 선택된 공고 첨부만 다운로드하며 선택하지 않은 첨부에는 접근하지 않는다.
6. 같은 회사·공고·요청자에 `PENDING` 또는 `PROCESSING` 검토가 있으면 새 요청을 거부한다.

### Status Code

| HTTP | code | 설명 |
|------|------|------|
| 202 | - | 요청 접수 성공 |
| 400 | `BIDDING_INVALID_REVIEW_REQUEST` | ID, 개수, 중복 또는 프롬프트 형식 오류 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션이 없거나 만료됨 |
| 403 | `BIDDING_ACCESS_PERMISSION_REQUIRED` | 입찰 관리 권한 없음 |
| 403 | `BIDDING_REVIEW_DOCUMENT_ACCESS_DENIED` | 입찰 기준자료 또는 사내 문서함 참조에 접근할 수 없음(`companyDocumentVersionIds`는 다른 회사 문서·미완료 버전도 이 코드로 묶인다 — 사내 문서함 조회 포트가 완료 최신 버전만 노출해 세분화된 사유를 구분하지 않음) |
| 404 | `BIDDING_NOTICE_NOT_FOUND` | 현재 회사에서 조회할 수 있는 공고가 없음 |
| 404 | `BIDDING_NOTICE_ATTACHMENT_NOT_FOUND` | 선택한 공고 첨부가 없거나 해당 공고 소속이 아님 |
| 409 | `BIDDING_REVIEW_ALREADY_PROCESSING` | 같은 요청자에게 진행 중인 검토가 있음 |
| 409 | `BIDDING_REVIEW_DOCUMENT_NOT_READY` | 입찰 기준자료(`bid_reference_file`) 업로드 또는 인덱싱이 완료되지 않음. `companyDocumentVersionIds`에는 해당하지 않음(위 403 참고) |
| 422 | `BIDDING_REVIEW_UNSUPPORTED_FILE` | 지원하지 않는 공고 첨부 형식 |

---

## 공고별 입찰 문서 검토 이력 조회 `GET /api/v1/bidding/notices/{noticeId}/reviews`

**상태**: ✅ 확정

현재 회사에서 본인이 요청한 검토 이력을 최신순으로 조회한다. 최대 20건을 반환하며 임시파일의 원본 URL과 저장 키는 포함하지 않는다.

각 항목은 `reviewId`, `reviewStatus`, `prompt`, `requestedAt`, `completedAt`, `expiresAt`, `projectId`를 반환한다.

### Status Code

| HTTP | code | 설명 |
|------|------|------|
| 200 | - | 조회 성공. 이력이 없으면 빈 배열 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션이 없거나 만료됨 |
| 403 | `BIDDING_ACCESS_PERMISSION_REQUIRED` | 입찰 관리 권한 없음 |
| 404 | `BIDDING_NOTICE_NOT_FOUND` | 현재 회사에서 조회할 수 있는 공고가 없음 |

---

## 입찰 문서 검토 조회 `GET /api/v1/bidding/reviews/{reviewId}`

**상태**: ✅ 확정

### Response Data

| 필드 | 타입 | 설명 |
|------|------|------|
| `reviewId` | Long | 검토 ID |
| `noticeId` | Long | 입찰 공고 ID |
| `prompt` | String | 요청 당시 사용자 프롬프트 |
| `reviewStatus` | String | 검토 상태 |
| `result` | String | 근거 번호를 포함한 검토 결과. 완료 전에는 `null` |
| `errorMessage` | String | 실패 사유. 실패가 아니면 `null` |
| `requestedAt` | LocalDateTime | 요청 시각 |
| `completedAt` | LocalDateTime | 완료 또는 실패 시각 |
| `expiresAt` | LocalDateTime | 임시파일 정리 예정 시각. 처리 중이거나 귀속 완료면 `null` |
| `projectId` | Long | 정식 프로젝트로 귀속됐으면 프로젝트 ID |
| `documents` | List | 요청 당시 선택한 문서 목록과 처리 상태 |
| `citations` | List | 검토 결과의 근거 목록 |

`documents[]`는 `documentRole`, `bidAttachmentId`, `referenceFileId`, `companyDocumentVersionId`, `fileName`,
`processingStatus`를 반환한다(2026-08-13 `companyDocumentVersionId` 추가). `documentRole`은 `BID_ATTACHMENT`·
`INTERNAL_REFERENCE`·`COMPANY_DOCUMENT_REFERENCE` 중 하나이며, 세 ID 중 문서 역할에 해당하지 않는 값은 `null`이다.

`citations[]`는 `rankOrder`, `documentRole`, `bidAttachmentId`, `referenceFileId`, `companyDocumentVersionId`,
`fileName`, `pageNumber`, `sheetName`, `excerpt`를 반환한다(2026-08-13 `companyDocumentVersionId` 추가).
페이지 또는 시트가 없는 형식은 해당 필드가 `null`이다.

### Status Code

| HTTP | code | 설명 |
|------|------|------|
| 200 | - | 조회 성공 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션이 없거나 만료됨 |
| 403 | `BIDDING_REVIEW_ACCESS_DENIED` | 다른 회사 또는 다른 요청자의 검토 |
| 404 | `BIDDING_REVIEW_NOT_FOUND` | 검토가 존재하지 않음 |

---

## 입찰 문서 검토 포기 `PATCH /api/v1/bidding/reviews/{reviewId}/abandon`

**상태**: ✅ 확정

현재 요청자가 프로젝트로 전환하지 않은 검토를 포기하고 임시파일 정리를 즉시 요청한다. Request Body는 없다.

`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` 상태에서만 포기할 수 있다. 이미 `ABANDONED`, `EXPIRED`이거나
프로젝트로 귀속된 검토는 변경하지 않는다. 처리 중 Worker의 이후 callback은 `accepted=false`로 거절한다.

### Status Code

| HTTP | code | 설명 |
|------|------|------|
| 200 | - | 포기 접수 및 정리 요청 성공 |
| 401 | `AUTH_UNAUTHENTICATED` | 세션이 없거나 만료됨 |
| 403 | `BIDDING_REVIEW_ACCESS_DENIED` | 다른 회사 또는 다른 요청자의 검토 |
| 404 | `BIDDING_REVIEW_NOT_FOUND` | 검토가 존재하지 않음 |
| 409 | `BIDDING_REVIEW_NOT_ABANDONABLE` | 이미 정리됐거나 프로젝트에 귀속됨 |

---

## Python 입찰 문서 검토 작업 조회 `GET /internal/v1/bidding/reviews/{reviewId}/jobs/{attemptId}`

**상태**: ✅ 확정

Worker 전용 토큰으로 인증한다. 응답에는 `reviewId`, `companyId`, `attemptId`, `prompt`, 공고 기본 정보,
선택한 공고 첨부의 `attachmentId`, `fileName`, 내부 원본 `sourceUrl`, 선택한 입찰 기준자료(`referenceFiles[]`)의
`referenceFileId`, `fileName`, 단명 내부 다운로드 URL, 선택한 사내 문서함 참조(`companyDocuments[]`, 2026-08-13 추가)의
`companyDocumentVersionId`, `fileName`, 단명 내부 다운로드 URL을 포함한다. 프론트용 API에서는 이 URL들을 제공하지 않는다.

**공고 첨부 임시 저장(2026-08-13 추가)**: `attachments[]`의 각 항목은 `sourceUrl`(외부 원본 다운로드) 외에
`uploadUrl`(presigned PUT, 만료 10분)과 `temporaryStorageKey`를 함께 받는다. Worker는 ① `sourceUrl`에서
원본을 내려받고 ② **Content-Type을 `application/octet-stream`으로 고정해서** `uploadUrl`로 그대로 PUT하고
③ callback의 `documents[]`에 그 `temporaryStorageKey`를 그대로 실어 보낸다(Spring이 새로 생성하지 않고
Worker가 받은 값을 그대로 돌려주는 왕복 값). Content-Type이 다르면 presigned URL 서명이 안 맞아 PUT이
403으로 거절된다. 이 임시 복사본은 검토가 프로젝트로 전환될 때 원본 URL을 다시 신뢰하지 않고 그대로
정식 파일로 승격하는 데 쓴다.

**`qualificationSummary`(2026-08-13 추가)**: 현재 회사의 재직 중(퇴사·삭제 제외, 시스템 계정 제외) 사원 기준
전공·학력·자격증 보유 현황을 인원수로만 집계한 텍스트("전기공학 5명, 컴퓨터공학 12명" 형태). **이름·사번 등 개인
식별 정보는 절대 포함하지 않는다** — 개인정보 보호를 위해 전공×학력처럼 교차 집계하지 않고 세 항목을 항상 독립
집계한다. Python Worker가 이 텍스트를 LLM 프롬프트 컨텍스트에 포함해 "보유 인력으로 수행 가능한지"를 판단하는
데 쓴다(Python 쪽 프롬프트 반영은 별도 작업 — 아직 미착수).

현재 `attemptId`와 일치하는 `PENDING` 또는 `PROCESSING` 작업만 반환한다.

### Status Code

| HTTP | code | 설명 |
|------|------|------|
| 200 | - | 작업 조회 성공 |
| 400 | `BIDDING_INVALID_REVIEW_REQUEST` | ID 또는 UUID 형식 오류 |
| 401 | `AUTH_UNAUTHENTICATED` | worker 토큰 누락 또는 불일치 |
| 404 | `BIDDING_REVIEW_JOB_NOT_FOUND` | 현재 시도와 일치하는 작업 없음 |

---

## Python 입찰 문서 검토 결과 callback `POST /internal/v1/bidding/reviews/{reviewId}/callback`

**상태**: ✅ 확정

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `attemptId` | String(UUID) | Y | 현재 처리 시도 ID |
| `reviewStatus` | String | Y | `PROCESSING`, `COMPLETED`, `FAILED` |
| `result` | String | 조건부 | `COMPLETED`이면 필수 |
| `errorCode` | String | 조건부 | `FAILED`이면 오류 분류 코드 |
| `errorMessage` | String | 조건부 | `FAILED`이면 필수, 최대 500자 |
| `retryable` | Boolean | N | `FAILED` 오류의 일시 장애 여부. 생략 시 `false`로 처리하며 `PROCESSING`·`COMPLETED`에서는 `false`여야 함 |
| `documents` | List | N | 공고 첨부별 임시 저장·처리 결과 |
| `citations` | List | N | `COMPLETED` 검토 근거 목록 |

`documents[]`는 `bidAttachmentId`, `processingStatus`, `temporaryStorageKey`, `fileSize`, `mimeType`을 전달한다.
`temporaryStorageKey`는 Spring DB 저장용 내부 값이며 외부 조회 응답에서는 제거한다. `PROCESSING` 콜백에서도
문서별 중간 진행 상태를 갱신하기 위해 `documents[]`만 담아 보낼 수 있다(`result`·`citations` 없이).

`citations[]`는 `rankOrder`, `documentRole`, `bidAttachmentId`, `referenceFileId`, `companyDocumentVersionId`,
`fileName`, `pageNumber`, `sheetName`, `excerpt`를 전달한다(2026-08-13 `companyDocumentVersionId` 추가,
`documentRole=COMPANY_DOCUMENT_REFERENCE`일 때 사용).

재시도 대상은 다운로드 연결 실패·Timeout·Gemini 429·5xx 등 일시적 오류만 `retryable=true`로 전달한다.
잘못된 요청·지원하지 않는 형식·설정 오류 등 복구 불가능한 오류는 `retryable=false`로 전달하고 `FAILED`로 종료한다.

Spring은 현재 `attemptId`와 상태 전이가 일치할 때만 저장하며 응답으로 `accepted`, `reviewId`,
`reviewStatus`, `reason`을 반환한다. `COMPLETED` 또는 `FAILED`(재시도 아님) 저장 시
`expiresAt = 공고 입찰마감일시`로 계산한다(2026-08-13 정책 변경 - 종전 "완료 시각 + 3시간").
마감일시가 없거나 이미 완료·실패 시각보다 과거면 `완료(실패) 시각 + 3시간`으로 되돌아간다.

### Status Code

| HTTP | code | 설명 |
|------|------|------|
| 200 | - | callback 접수. 멱등 거절도 `accepted=false`로 200 반환 |
| 400 | `BIDDING_INVALID_REVIEW_CALLBACK` | 상태별 필수값 또는 UUID 형식 오류 |
| 401 | `AUTH_UNAUTHENTICATED` | worker 토큰 누락 또는 불일치 |
| 404 | `BIDDING_REVIEW_NOT_FOUND` | 검토가 존재하지 않음 |

### 비동기 신뢰성 및 정리 규칙

| 항목 | 규칙 |
|------|------|
| 재시도 | 일시적 다운로드·Spring·Gemini 오류는 최대 3회 재시도한다 |
| 새 시도 | 재시도마다 새 `attemptId`를 발급하고 이전 callback은 거절한다 |
| ACK | callback에서 수락 또는 멱등 거절을 확인한 뒤 Redis 메시지를 ACK한다 |
| 만료 스캔 | Spring 스케줄러가 `expiresAt <= now`이고 프로젝트에 귀속되지 않은 검토를 원자적으로 점유해 정리 Outbox를 저장한다 |
| 정리 범위 | 임시 S3 객체와 해당 검토 전용 임시 파생 데이터만 삭제하며 입찰 기준자료와 공고 첨부 메타데이터는 삭제하지 않는다 |
| 포기 | 포기 트랜잭션에서 상태 전이와 즉시 정리 Outbox를 함께 저장한다 |
| 정리 완료 | 모든 임시 데이터 삭제 성공 후 `EXPIRED`로 전이한다. 포기 이력은 별도 포기 시각으로 보존한다 |
| DLQ | 최대 재시도 초과 정리 작업은 reviewId와 attemptId 기준 중복 제거 후 DLQ에 기록한다 |

---

## 공고 프로젝트 전환 `POST /api/v1/bidding/notices/{noticeId}/projects`

**상태**: ✅ 확정 (2026-08-13 정정 — `reviewId`/`summaryId` 필수·선택 관계가 실제 사용 흐름과 반대로 적혀 있었음.
실제 흐름은 "문서 검토(bidreview) 완료 후 프로젝트 전환"이 주 경로이며, AI 요약(summaryId) 연결은 있으면 같이
하는 선택 사항이다. 프론트에 이 필드 관계 변경을 공유해야 한다.)

**Request Body**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `reviewId` | Long | Y | 같은 공고·회사·요청자의 `COMPLETED` 문서 검토 ID. 만료·포기되지 않았어야 함. 누락 시 `COMMON_INVALID_REQUEST`. 검토에서 실제 사용한 공고 첨부를 프로젝트 파일로 귀속 |
| `summaryId` | Long | N | 현재 회사에서 확정된 `COMPLETED` AI 요약 ID. 지정하면 `bid_notice_summary.project_id`에 연결, 생략하면 연결 없이 진행 |
| `name` | String | Y | 프로젝트명 |
| `description` | String | N | 설명 |
| `businessCategoryId` | Long | Y | 사업 카테고리 ID |
| `startedOn` | Date | Y | 시작일 |
| `endedOn` | Date | Y | 종료일 |
| `memberIds` | String[] | N | 추가 참여자 user ID 목록. 전환 요청자는 서버가 자동 포함 |

서버 처리:

1. 공고 존재 여부와 현재 회사의 접근 권한 확인
2. `reviewId`가 같은 공고·회사·요청자의 `COMPLETED` 검토이며 만료·포기되지 않았는지 확인
3. `summaryId`가 있으면 같은 공고·회사에 속한 `COMPLETED`·확정 요약이며 아직 프로젝트에 연결되지 않았는지 확인
4. 기존 프로젝트 전환 여부 확인
5. 요청자가 `BIDDING` 권한을 갖는지 확인
6. 추가 `memberIds`가 초대 가능한 사용자이며 프로젝트 참여자로 등록 가능한지 확인
7. 하나의 DB 트랜잭션 시작
8. 프로젝트 생성과 `project.bid_notice_id` 저장
9. `summaryId`가 있으면 `bid_notice_summary.project_id`에 생성된 프로젝트 ID 저장
10. 검토에서 실제 사용한 공고 첨부의 정식 파일 귀속 요청을 기록하고 `bid_review.project_id`를 저장
11. 인증된 전환 요청자를 `project_member`에 자동 등록
12. 추가 `memberIds`를 `project_member`에 등록
13. 필요 시 입찰 프로젝트 기본 스테이지·스텝 자동 생성
14. 입찰용 블록은 생성하지 않음
15. 전체 성공 시 커밋, 중간 실패 시 전체 롤백

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
| 원자성 | 프로젝트 생성부터 `project.bid_notice_id`, `bid_review.project_id`, (있으면) `bid_notice_summary.project_id`, 참여자 등록과 기본 단계 생성까지 하나의 DB 트랜잭션 |
| 중간 실패 | 전체 롤백. 불완전한 프로젝트를 남기지 않음 |
| 이미 전환된 공고 | 새 트랜잭션을 시작하지 않고 409 반환 |
| 재시도 | 이전 요청이 롤백됐다면 재시도 가능. 이미 커밋됐다면 409 |

정책:

| 항목 | 규칙 |
|------|------|
| 공고 하나당 프로젝트 | 1개만 생성 |
| 중복 방지 | `UNIQUE(project.bid_notice_id)` |
| 확정 요약 연결 | `summaryId`가 있으면 `UNIQUE(bid_notice_summary.project_id)`로 한 프로젝트의 근거 요약을 하나로 고정. 없으면 연결 생략 |
| 검토 파일 귀속 | `reviewId`(필수)로 지정한 검토에서 선택하고 실제 다운로드에 성공한 공고 첨부만 정식 프로젝트 파일로 귀속 |
| 귀속 멱등성 | `bidReviewDocumentId`를 파일 도메인 멱등키로 전달하여 재시도 시 파일을 중복 생성하지 않음 |
| 귀속 완료 | 파일 도메인이 정식 `fileVersionId`를 반환한 뒤 문서 상태를 `PROMOTED`로 저장하고 임시 객체를 삭제 |
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
