# 입찰 관리 API 명세

**노션 원본**: 사용자 제공 노션 정리본 (링크 미제공)
**최종 동기화**: 2026-08-03
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
| 📝 초안 | 프로젝트 입찰 블록 조회 | GET | `/api/v1/blocks/{blockId}/bid-notice` | 스텝 접근 권한 |

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
| `noticeAgency` | 발주처 |
| `businessCategoryId` | 사업 카테고리 ID |
| `businessCategoryName` | 사업 카테고리명 |
| `baseAmount` | 기초금액 |
| `estimatedAmount` | 추정가격 |
| `announcedAt` | 공고일 |
| `bidDeadlineAt` | 마감일 |
| `dDay` | D-day |
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

---

## 입찰 공고 상세 조회 `GET /api/v1/bidding/notices/{noticeId}`

**상태**: 📝 초안

| 영역 | 필드 |
|------|------|
| 기본 정보 | `noticeId`, `externalId`, `noticeOrder`, `noticeName`, `noticeAgency`, `demandAgency`, `noticeStatus`, `dismissReason`, `projectId` |
| 일정 | `announcedAt`, `bidStartAt`, `questionDeadlineAt`, `applicationDeadlineAt`, `bidDeadlineAt`, `openingAt`, `dDay` |
| 금액 | `baseAmount`, `estimatedAmount`, `priceRangeText`, `minimumBidRateText` |
| 계약 및 제한 | `regionLimitText`, `businessLimitText`, `contractMethod`, `evaluationMethod`, `sourceUrl`, `hasAttachment` |
| 참여사 | `participantCount`, `participants` |

`dDay`는 DB에 저장하지 않고 서버에서 계산한다.

첨부파일 상세 테이블은 구현 범위에서 제외할 수 있다. 이 경우 파일명과 크기를 보여주지 않고 `hasAttachment`와 원문 링크만 제공한다.

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
| `memberIds` | String[] | 참여자 user ID 목록 |

서버 처리:

1. 공고 존재 여부 확인
2. 기존 프로젝트 전환 여부 확인
3. 프로젝트 생성
4. `project.bid_notice_id` 저장
5. 공고 주요 값 스냅샷 저장
6. `project_member` 등록
7. 입찰 스테이지·스텝·블록 자동 생성
8. 생성된 입찰 블록 연결

정책:

| 항목 | 규칙 |
|------|------|
| 공고 하나당 프로젝트 | 1개만 생성 |
| 중복 방지 | `UNIQUE(project.bid_notice_id)` |
| 스냅샷 | 프로젝트 생성 당시 금액·마감·개찰일 저장 |
| 정정공고 | 프로젝트 스냅샷을 자동 덮어쓰지 않음 |

---

## 프로젝트 공고 변경 비교 `GET /api/v1/projects/{projectId}/bid-notice/changes`

**상태**: 📝 초안

프로젝트 스냅샷과 현재 최신 공고를 비교한다.

---

## 프로젝트 공고 스냅샷 반영 `PATCH /api/v1/projects/{projectId}/bid-notice-snapshot`

**상태**: 📝 초안

사용자가 선택한 최신 값만 프로젝트에 반영한다. 자동으로 프로젝트 스냅샷을 덮어쓰지 않는다.

---

## 프로젝트 입찰 블록 조회 `GET /api/v1/blocks/{blockId}/bid-notice`

**상태**: 📝 초안

입찰 블록은 프로젝트 생성 시 자동으로 만들어지는 조회 블록이다.

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
| 참여사 수 | `participantCount` |
| 진행 단계 | 프로젝트 스테이지·스텝 상태 기반 |
| 원본 공고 변경 여부 | `noticeChanged` |

금지 API:

```text
PUT /api/v1/blocks/{blockId}/bid-notice
DELETE /api/v1/blocks/{blockId}/bid-notice
```

프로젝트 안에서 공고를 다시 선택하거나 연결 해제하지 않는다.
