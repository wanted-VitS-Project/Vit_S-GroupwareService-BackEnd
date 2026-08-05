# 입찰 관리 API 명세

**노션 원본**: 사용자 제공 노션 정리본 (링크 미제공)
**최종 동기화**: 2026-08-05 (입찰용 블록 미사용 정책 반영)
**도메인 담당**: 정현

> 상태가 `✅ 확정` 이상인 항목은 프론트와의 계약이다. 임의 변경 금지.
> 현재 문서는 노션 정리본을 로컬에 옮긴 **초안**이다. 팀 합의와 프론트 공유가 끝나면 상태를 `✅ 확정`으로 바꾼다.

---

## 엔드포인트 목록

| 상태 | 기능 | METHOD | URL | 권한 |
|------|------|--------|-----|------|
| 📝 초안 | 수집 조건 목록 조회 | GET | `/api/v1/bidding/crawl-conditions` | `BIDDING` |
| 📝 초안 | 수집 조건 등록 | POST | `/api/v1/bidding/crawl-conditions` | `BIDDING` |
| 📝 초안 | 수집 조건 수정 | PATCH | `/api/v1/bidding/crawl-conditions/{conditionId}` | `BIDDING` |
| 📝 초안 | 수동 수집 요청 | POST | `/api/v1/bidding/crawl-conditions/{conditionId}/runs` | `BIDDING` |
| 📝 초안 | 수집 실행 결과 조회 | GET | `/api/v1/bidding/crawl-runs/{runId}` | `BIDDING` |
| 📝 초안 | 입찰 공고 목록 조회 | GET | `/api/v1/bidding/notices` | `BIDDING` |
| 📝 초안 | 입찰 공고 상세 조회 | GET | `/api/v1/bidding/notices/{noticeId}` | `BIDDING` |
| 📝 초안 | 입찰 공고 직접 등록 | POST | `/api/v1/bidding/notices` | `BIDDING` |
| 📝 초안 | 공고 제외 | PATCH | `/api/v1/bidding/notices/{noticeId}/dismiss` | `BIDDING` |
| 📝 초안 | 공고 복구 | PATCH | `/api/v1/bidding/notices/{noticeId}/restore` | `BIDDING` |
| 📝 초안 | 입찰 AI 요약 요청 | POST | `/api/v1/bidding/notices/{noticeId}/summaries` | `BIDDING` |
| 📝 초안 | 입찰 AI 요약 조회 | GET | `/api/v1/bidding/summaries/{summaryId}` | `BIDDING` |
| 📝 초안 | 입찰 AI 요약 수정 | PATCH | `/api/v1/bidding/summaries/{summaryId}` | `BIDDING` |
| 📝 초안 | 입찰 AI 요약 확정 | PATCH | `/api/v1/bidding/summaries/{summaryId}/confirm` | `BIDDING` |
| 📝 초안 | 공고 프로젝트 전환 | POST | `/api/v1/bidding/notices/{noticeId}/projects` | `BIDDING` |
| 📝 초안 | 프로젝트 공고 변경 비교 | GET | `/api/v1/projects/{projectId}/bid-notice/changes` | 프로젝트 접근 권한 |
| 📝 초안 | 프로젝트 공고 스냅샷 반영 | PATCH | `/api/v1/projects/{projectId}/bid-notice-snapshot` | 프로젝트 편집 권한 |

---

## 입찰 블록 미사용 정책

입찰용 블록은 v1에서 사용하지 않는다.
입찰 공고에서 프로젝트를 만들 때 `block.type='BID_NOTICE'` 블록을 생성하지 않는다.

```text
bid_notice = 입찰 공고 원본
project = 전환 시점의 공고 스냅샷 보유
```

정책:

| 항목 | 규칙 |
|------|------|
| 블록 생성 | 생성하지 않는다 |
| 수동 생성 | 제공하지 않는다 |
| 프로젝트 연결 | `project.bid_notice_id`로 전환한 공고 원본과 연결한다 |
| 입찰 상세 조회 | 입찰 공고 API 또는 프로젝트 공고 스냅샷 API에서 처리한다 |
| 공고 원본 수정 | 입찰 공고 API 정책을 따른다 |
| 프로젝트 스냅샷 수정 | `PATCH /api/v1/projects/{projectId}/bid-notice-snapshot`을 사용한다 |
| 블록 제목·위치 수정 | 대상 없음 |
| 블록 삭제 | 대상 없음 |

입찰 블록 전용 API는 만들지 않는다.

금지 API:

```text
GET /api/v1/blocks/{blockId}/bid-notice
PUT /api/v1/blocks/{blockId}/bid-notice
PATCH /api/v1/blocks/{blockId}/bid-notice
DELETE /api/v1/blocks/{blockId}/bid-notice
```

---

## 수집 조건

| API | 설명 |
|-----|------|
| `GET /api/v1/bidding/crawl-conditions` | 수집 조건 목록 조회 |
| `POST /api/v1/bidding/crawl-conditions` | 수집 조건 등록 |
| `PATCH /api/v1/bidding/crawl-conditions/{conditionId}` | 조건명·파라미터·활성화 여부 수정 |

사용자는 임의 URL을 등록하지 않고 시스템이 지원하는 수집 소스를 선택한다.

---

## 수동 수집 및 결과 조회

### 수동 수집 요청 `POST /api/v1/bidding/crawl-conditions/{conditionId}/runs`

**상태**: 📝 초안

수동 수집 요청은 비동기다.

```text
202 Accepted
→ runId 반환
→ 실행 결과 조회
```

### 수집 실행 결과 조회 `GET /api/v1/bidding/crawl-runs/{runId}`

**상태**: 📝 초안

| 응답값 | 설명 |
|--------|------|
| `runStatus` | `RUNNING/SUCCESS/FAILED` |
| `collectedCount` | 전체 조회 건수 |
| `insertedCount` | 신규 저장 건수 |
| `updatedCount` | 갱신 건수 |
| `skippedCount` | 건너뛴 건수 |
| `errorMessage` | 실패 메시지 |

---

## 입찰 공고 목록 조회 `GET /api/v1/bidding/notices`

**상태**: 📝 초안

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
| `noticeStatus` | 공고 상태 |
| `sort` | 정렬 |
| `page` | 페이지 |
| `size` | 크기 |

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
| 기준 | 마지막 성공 수집 실행에서 신규 INSERT 된 공고 |
| 유지 기간 | 다음 성공 수집 실행 전까지 |
| 수동 등록 | 등록 직후 `isNew=true`, 다음 성공 수집 실행 전까지 유지 |
| 저장 방식 | 프론트가 계산하지 않고 서버가 Boolean으로 내려준다 |

---

## 입찰 공고 상세 조회 `GET /api/v1/bidding/notices/{noticeId}`

**상태**: 📝 초안

| 영역 | 필드 |
|------|------|
| 기본 정보 | `noticeId`, `externalId`, `noticeOrder`, `noticeName`, `noticeAgency`, `demandAgency`, `noticeStatus`, `dismissReason`, `projectId` |
| 출처 | `sourceCode`, `sourceName`, `sourceUrl`, `hasAttachment` |
| 일정 | `announcedAt`, `bidStartAt`, `questionDeadlineAt`, `applicationDeadlineAt`, `bidDeadlineAt`, `openingAt`, `dDay` |
| 금액 | `baseAmount`, `estimatedAmount`, `priceRangeText`, `minimumBidRateText` |
| 계약 및 제한 | `participationQualificationText`, `regionLimitText`, `businessLimitText`, `jointContractAllowed`, `jointContractText`, `contractMethod`, `evaluationMethod` |
| 참여사 | `participantCount`, `participants` |

`dDay`는 DB에 저장하지 않고 서버에서 계산한다.

첨부파일 상세 테이블은 구현 범위에서 제외할 수 있다. 이 경우 파일명과 크기를 보여주지 않고 `hasAttachment`와 원문 링크만 제공한다.

참여 요건 필드는 REQ-02에 따라 원문 문자열을 보존한다.

| 필드 | 규칙 |
|------|------|
| `participationQualificationText` | 참가 자격 원문. 파싱하지 못해도 문자열 그대로 보존 |
| `regionLimitText` | 지역 제한 원문 |
| `businessLimitText` | 업종 제한 원문 |
| `jointContractAllowed` | 공동수급 가능 여부. 판별 불가면 `null` |
| `jointContractText` | 공동수급 관련 원문 |

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
8. 공고 스냅샷 필드 저장
9. 인증된 전환 요청자를 `project_member`에 자동 등록
10. 추가 `memberIds`를 `project_member`에 등록
11. 필요 시 입찰 프로젝트 기본 스테이지·스텝 자동 생성
12. 입찰용 블록은 생성하지 않음
13. 전체 성공 시 커밋, 중간 실패 시 전체 롤백

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
| 원자성 | 프로젝트 생성부터 공고 스냅샷 저장까지 하나의 DB 트랜잭션 |
| 중간 실패 | 전체 롤백. 불완전한 프로젝트를 남기지 않음 |
| 이미 전환된 공고 | 새 트랜잭션을 시작하지 않고 409 반환 |
| 재시도 | 이전 요청이 롤백됐다면 재시도 가능. 이미 커밋됐다면 409 |

정책:

| 항목 | 규칙 |
|------|------|
| 공고 하나당 프로젝트 | 1개만 생성 |
| 중복 방지 | `UNIQUE(project.bid_notice_id)` |
| 스냅샷 | 프로젝트 생성 당시 `noticeName`, `bidNoticeId`, `baseAmount`, `estimatedAmount`, `bidDeadlineAt`, `openingAt` 저장 |
| 정정공고 | 프로젝트 스냅샷을 자동 덮어쓰지 않음 |

입찰 블록 미사용 정책:

| 항목 | 규칙 |
|------|------|
| 블록 타입 | 사용하지 않음 |
| 생성 위치 | 대상 없음 |
| 블록 제목 | 대상 없음 |
| 연결 대상 | `project.bid_notice_id`로 전환한 `bid_notice`를 조회 |
| 원본 보호 | 프로젝트 전환·삭제가 `bid_notice` 원본을 삭제하거나 수정하지 않음 |
| 중복 방지 | 같은 프로젝트 전환은 `UNIQUE(project.bid_notice_id)`로 1회만 허용 |

스냅샷 저장 필드:

| 프로젝트 스냅샷 필드 | 원본 공고 필드 |
|---------------------|---------------|
| `bidNoticeId` | `noticeId` |
| `bidNoticeNameSnapshot` | `noticeName` |
| `baseAmountSnapshot` | `baseAmount` |
| `estimatedAmountSnapshot` | `estimatedAmount` |
| `bidDeadlineAtSnapshot` | `bidDeadlineAt` |
| `openingAtSnapshot` | `openingAt` |

---

## 프로젝트 공고 변경 비교 `GET /api/v1/projects/{projectId}/bid-notice/changes`

**상태**: 📝 초안

프로젝트 스냅샷과 현재 최신 공고를 비교한다.

**Response — `200`**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `projectId` | Long | 프로젝트 ID |
| `bidNoticeId` | Long | 원본 공고 ID |
| `noticeChanged` | Boolean | 현재 공고와 스냅샷의 차이 여부 |
| `snapshotVersion` | Long | 스냅샷 낙관적 락 버전 |
| `changedFields` | Object[] | 변경된 필드 목록 |

**changedFields**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `fieldName` | String | 반영 가능한 필드명 |
| `displayName` | String | 화면 표시명 |
| `snapshotValue` | String | 프로젝트에 저장된 그때 값 |
| `currentValue` | String | 최신 공고 값 |

반영 가능한 `fieldName`은 `noticeName`, `baseAmount`, `estimatedAmount`, `bidDeadlineAt`, `openingAt` 으로 제한한다.

---

## 프로젝트 공고 스냅샷 반영 `PATCH /api/v1/projects/{projectId}/bid-notice-snapshot`

**상태**: 📝 초안

사용자가 선택한 최신 값만 프로젝트에 반영한다. 자동으로 프로젝트 스냅샷을 덮어쓰지 않는다.

**Request**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `snapshotVersion` | Long | Y | 사용자가 비교 화면에서 본 스냅샷 버전 |
| `fields` | String[] | Y | 반영할 필드명 목록 |

**Request 예시**

```json
{
  "snapshotVersion": 3,
  "fields": ["baseAmount", "bidDeadlineAt"]
}
```

**Response — `200`**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `projectId` | Long | 프로젝트 ID |
| `bidNoticeId` | Long | 원본 공고 ID |
| `snapshotVersion` | Long | 반영 후 새 스냅샷 버전 |
| `appliedFields` | String[] | 실제 반영된 필드 목록 |
| `changeApplyHistoryId` | Long | 반영 이력 ID |

충돌 및 오류:

| 상태 | 상황 |
|------|------|
| 400 | `fields`가 비어 있거나 허용되지 않은 필드 포함 |
| 403 | 프로젝트 편집 권한 없음 |
| 404 | 프로젝트 또는 연결된 공고 없음 |
| 409 | 요청의 `snapshotVersion`이 현재 버전과 다름 |

---

## 프로젝트 입찰 블록 조회 API 폐기

**상태**: 폐기

v1에서는 입찰용 블록을 사용하지 않으므로 아래 API를 제공하지 않는다.

```text
GET /api/v1/blocks/{blockId}/bid-notice
```

입찰 상세와 프로젝트 전환 후 공고 정보는 아래 API로 처리한다.

| 목적 | API |
|------|-----|
| 입찰 공고 원본 상세 조회 | `GET /api/v1/bidding/notices/{noticeId}` |
| 프로젝트 전환 당시 스냅샷과 현재 공고 비교 | `GET /api/v1/projects/{projectId}/bid-notice/changes` |
| 승인한 최신 공고값을 프로젝트 스냅샷에 반영 | `PATCH /api/v1/projects/{projectId}/bid-notice-snapshot` |

표시값:

| 항목 | 설명 |
|------|------|
| 공고명 | 입찰 공고명 |
| 공고일 | 공고일 |
| 질의응답 마감 | `questionDeadlineAt` |
| 제출 마감 | `bidDeadlineAt` |
| 개찰일 | `openingAt` |
| 발주처 | `noticeAgency` |
| 기초금액 | `baseAmount` |
| 계약 방식 | `contractMethod` |
| 참가 자격 | `participationQualificationText` |
| 지역 제한 | `regionLimitText` |
| 업종 제한 | `businessLimitText` |
| 공동수급 가능 여부 | `jointContractAllowed` |
| 공동수급 원문 | `jointContractText` |
| 참여사 수 | `participantCount` |
| 진행 단계 | 프로젝트 상태 기반 |
| 원본 공고 변경 여부 | `noticeChanged` |

**notice**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `noticeName` | String | 입찰 공고명 |
| `announcedAt` | LocalDateTime | 공고일 |
| `questionDeadlineAt` | LocalDateTime | 질의응답 마감 |
| `bidDeadlineAt` | LocalDateTime | 제출 마감 |
| `openingAt` | LocalDateTime | 개찰일 |
| `noticeAgency` | String | 발주처 |
| `baseAmount` | Decimal | 기초금액 |
| `estimatedAmount` | Decimal | 추정가격 |
| `contractMethod` | String | 계약 방식 |
| `participationQualificationText` | String | 참가 자격 원문 |
| `regionLimitText` | String | 지역 제한 원문 |
| `businessLimitText` | String | 업종 제한 원문 |
| `jointContractAllowed` | Boolean | 공동수급 가능 여부. 판별 불가면 `null` |
| `jointContractText` | String | 공동수급 원문 |
| `participantCount` | Integer | 참여사 수 |
| `sourceUrl` | String | 원문 URL |

**snapshot**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `noticeName` | String | 전환 당시 공고명 |
| `baseAmount` | Decimal | 전환 당시 기초금액 |
| `estimatedAmount` | Decimal | 전환 당시 추정가격 |
| `bidDeadlineAt` | LocalDateTime | 전환 당시 제출 마감 |
| `openingAt` | LocalDateTime | 전환 당시 개찰일 |
| `snapshotVersion` | Long | 스냅샷 낙관적 락 버전 |

폐기 API:

```text
GET /api/v1/blocks/{blockId}/bid-notice
PUT /api/v1/blocks/{blockId}/bid-notice
PATCH /api/v1/blocks/{blockId}/bid-notice
DELETE /api/v1/blocks/{blockId}/bid-notice
```

프로젝트 안에서 공고를 다시 선택하거나 연결 해제하지 않는다.
프로젝트 삭제·상태 변경이 필요하면 프로젝트 API 정책을 따르고, 이 경우에도 `bid_notice` 원본은 유지한다.
