# 비타메이트 API 명세

**노션 원본**: 사용자 제공 노션 정리본 (링크 미제공)
**최종 동기화**: 2026-08-07 (검토 문서 종류 확장 및 임원 검토 관점 템플릿 반영)
**도메인 담당**: 정현

> 이 파일이 비타메이트 API 계약 기준이다. 임의 변경 금지.
> 변경이 필요하면 이 문서를 먼저 수정하고 프론트/Python 담당자에게 공유한다.

---

## 엔드포인트 목록

| 상태 | 기능 | METHOD | URL | 권한 |
|------|------|--------|-----|------|
| ✅ 확정 | 문서 분석 요청 | POST | `/api/v1/blocks/{blockId}/vitamate/analyses` | 스텝 접근 권한 |
| ✅ 확정 | 검토 템플릿 목록 조회 | GET | `/api/v1/vitamate/review-templates` | 로그인 사용자 |
| ✅ 확정 | AI 분석 상태 및 결과 조회 | GET | `/api/v1/vitamate/analyses/{analysisId}` | 스텝 접근 권한 |
| ✅ 확정 | 블록별 분석 실행 이력 조회 | GET | `/api/v1/blocks/{blockId}/vitamate/analyses` | 스텝 접근 권한 |
| ✅ 확정 | Python 분석 작업 조회 | GET | `/internal/v1/vitamate/analyses/{analysisId}/jobs/{attemptId}` | 내부 서버 |
| ✅ 확정 | Python 분석 결과 콜백 | POST | `/internal/v1/vitamate/analyses/{analysisId}/callback` | 내부 서버 |
| ✅ 확정 | 파일 인덱싱 소스 조회 | GET | `/internal/v1/vitamate/file-versions/{fileVersionId}/index-source` | 내부 서버 |
| ✅ 확정 | 문서 청크 저장 | POST | `/internal/v1/vitamate/file-versions/{fileVersionId}/chunks` | 내부 서버 |
| ✅ 확정 | 문서 청크 임베딩 결과 저장 | POST | `/internal/v1/vitamate/file-versions/{fileVersionId}/chunks/embeddings` | 내부 서버 |
| ✅ 확정 | 파일 인덱싱 상태 콜백 | POST | `/internal/v1/vitamate/file-indexes/{fileVersionId}/callback` | 내부 서버 |

---

## 검토 템플릿 목록 조회 `GET /api/v1/vitamate/review-templates`

**상태**: ✅ 확정

프론트가 비타메이트 분석 요청 화면에서 검토 유형과 세부 검토 항목을 표시할 수 있도록
활성화된 검토 템플릿 목록을 조회한다.

템플릿 정본은 Spring Boot DB의 `vitamate_review_type`, `vitamate_review_template`이다.
Python worker는 별도 템플릿 목록을 들고 있지 않고, 분석 작업 조회 API에서 전달받은 템플릿만 사용한다.

**Request**

없음

**Response — `200`**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `reviewTypes` | Object[] | 검토 유형 목록 |
| `reviewTypes[].reviewType` | String | 검토 유형 코드. 분석 요청의 `reviewType`으로 사용 |
| `reviewTypes[].reviewTypeName` | String | 화면 표시용 검토 유형명 |
| `reviewTypes[].description` | String | 검토 유형 설명. 없으면 `null` |
| `reviewTypes[].categories` | Object[] | 해당 검토 유형에서 선택 가능한 세부 검토 항목 |
| `reviewTypes[].categories[].categoryCode` | String | 세부 검토 항목 코드. 분석 요청의 `reviewCategoryCodes[]`로 사용 |
| `reviewTypes[].categories[].categoryName` | String | 화면 표시용 세부 검토 항목명 |
| `reviewTypes[].categories[].guideText` | String | 사용자가 입력 내용을 준비할 때 보는 안내 문구. 없으면 `null` |
| `reviewTypes[].categories[].exampleText` | String | 사용자 입력 예시 또는 참고 문구. 없으면 `null` |
| `reviewTypes[].categories[].templateVersion` | String | 템플릿 버전 |

반환 규칙:

| 항목 | 규칙 |
|------|------|
| 정렬 | `review_type.sort_order ASC`, `review_template.sort_order ASC` |
| 비활성 항목 | `enabled = false`인 검토 유형과 세부 템플릿은 반환하지 않는다 |
| 프롬프트 전문 | 공개 API에서는 `prompt_template` 전문을 반환하지 않는다. 프론트에는 안내와 예시만 내려준다 |
| 분석 요청 기준 | 분석 요청의 `reviewType`, `reviewCategoryCodes`는 이 API 응답에 포함된 활성 값만 허용한다 |

초기 제공 템플릿 예시:

| 검토 유형 | 세부 항목 코드 | 설명 |
|----------|---------------|------|
| `COMMON_REVIEW` | `COMMON_DOCUMENT_QUALITY` | 공통 검토. 오탈자, 문단 형식, 목차-본문 일치, 표번호, 주석, 약자, 계산 결과 |
| `COST_REPORT` | `COST_RESULT` | 원가계산 결과 |
| `COST_REPORT` | `COST_OVERVIEW` | 원가계산 개요 |
| `COST_REPORT` | `COST_ELEMENT_CRITERIA` | 원가요소별 계산기준 |
| `COST_REPORT` | `COST_STATEMENT` | 원가계산서 |
| `COST_REPORT` | `COST_BREAKDOWN` | 산출내역 |
| `DELIVERY_PRICE_LINKAGE` | `DELIVERY_LINKAGE_GUIDE` | 납품대금 연동제 가이드 검토 |
| `CONSTRUCTION_REPORT` | `CONSTRUCTION_COST_MANUAL` | 공사원가 실무매뉴얼 검토 |
| `BID_NOTICE` | `BID_NOTICE_REQUIREMENT` | 공고 기본 정보와 요구사항 |
| `BID_NOTICE` | `BID_NOTICE_QUALIFICATION` | 참가 조건과 제한사항 |
| `BID_NOTICE` | `BID_NOTICE_SCHEDULE_RISK` | 일정과 제출 리스크 |
| `PROPOSAL_DOCUMENT` | `PROPOSAL_REQUIREMENT_COVERAGE` | 제안서 요구사항 대응성 |
| `PROPOSAL_DOCUMENT` | `PROPOSAL_EVALUATION_STRATEGY` | 제안서 평가 기준 대응 전략 |
| `PROPOSAL_DOCUMENT` | `PROPOSAL_EXECUTIVE_RISK` | 제안서 임원 승인 리스크 |
| `ETC_DOCUMENT` | `COMPLETION_REPORT` | 기타서류 - 완료계 |
| `ETC_DOCUMENT` | `INVOICE` | 기타서류 - 청구서 |
| `ETC_DOCUMENT` | `CONTRACT_DOCUMENT` | 기타서류 - 계약서류 |

문서 종류 선정 기준:

| 문서 종류 | 포함 이유 |
|----------|-----------|
| `COMMON_REVIEW` | 모든 제출 문서에 반복 적용되는 오탈자, 형식, 표번호, 주석, 계산 결과 검토가 필요하다 |
| `COST_REPORT` | 사용자가 제공한 원가계산보고서 검토 시나리오의 핵심 대상이다 |
| `DELIVERY_PRICE_LINKAGE` | 납품대금 연동제 가이드북 기준 검토가 별도 업무 흐름으로 존재한다 |
| `CONSTRUCTION_REPORT` | 공사원가 실무매뉴얼 기준 검토가 원가계산과 다른 판단 기준을 가진다 |
| `BID_NOTICE` | 입찰 공고를 프로젝트로 전환하기 전 참가 가능성, 일정, 제출 리스크를 임원에게 보고해야 한다 |
| `PROPOSAL_DOCUMENT` | RFP 요구사항 대응성과 평가 기준 대응 전략을 제출 전 검토해야 한다 |
| `ETC_DOCUMENT` | 완료계, 청구서, 계약서류처럼 제출·정산에 필요한 보조 문서 검토가 필요하다 |

**Response 예시**

```json
{
  "httpStatus": 200,
  "message": "비타메이트 검토 템플릿 목록 조회 성공",
  "data": {
    "reviewTypes": [
      {
        "reviewType": "COST_REPORT",
        "reviewTypeName": "원가계산보고서 검토",
        "description": "원가계산 결과, 개요, 원가요소별 계산기준, 원가계산서, 산출내역을 기준으로 검토한다.",
        "categories": [
          {
            "categoryCode": "COST_RESULT",
            "categoryName": "원가계산 결과",
            "guideText": "발주처, 용역명, 규격, 단위, 금액, 비고, 부가세 포함 여부, 제출일자와 제출자를 확인한다.",
            "exampleText": "발주처, 용역명, 규격, 단위, 금액, 부가세 포함 여부와 제출 정보를 확인해주세요.",
            "templateVersion": "COST_REPORT_V1"
          },
          {
            "categoryCode": "COST_OVERVIEW",
            "categoryName": "원가계산 개요",
            "guideText": "목적, 대상, 적용근거, 전제조건이 과업과 법령 기준에 맞게 작성되었는지 확인한다.",
            "exampleText": "발주처, 과업명, 조사일, 적용 법령, 재수정 판단 기준이 명확한지 확인해주세요.",
            "templateVersion": "COST_REPORT_V1"
          }
        ]
      }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
|------|------|------|------|
| 200 | OK | - | 조회 성공 |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | 세션이 없거나 만료됨 |

---

## 문서 분석 요청 `POST /api/v1/blocks/{blockId}/vitamate/analyses`

**상태**: ✅ 확정

선택한 문서 버전과 검토 카테고리를 기준으로 AI 분석을 요청한다.

비타메이트는 사용자가 자유 프롬프트를 처음부터 작성하는 구조가 아니라,
서비스가 제공하는 검토 템플릿을 기준으로 문서를 검토한다.
사용자는 검토 유형과 세부 카테고리를 선택하고, 필요한 보완 요청만 `additionalInstruction`에 입력한다.

**Request**

| 위치 | 파라미터 | 타입 | 필수 | 설명 |
|------|---------|------|------|------|
| Header | `Idempotency-Key` | String | Y | 같은 사용자 동작의 재시도 중복 방지 키 |
| Path | `blockId` | Long | Y | 비타메이트 AI 블록 ID |
| Body | `fileVersionIds` | Long[] | Y | 분석할 파일 버전 ID 목록 |
| Body | `reviewType` | String | Y | 검토 템플릿 목록 조회 API의 `reviewType` 값 |
| Body | `reviewCategoryCodes` | String[] | Y | 검토 템플릿 목록 조회 API에서 선택한 세부 카테고리 코드 목록 |
| Body | `additionalInstruction` | String | N | 사용자가 템플릿에 덧붙이는 추가 요청. 템플릿과 보안 규칙을 덮어쓸 수 없음 |

검토 유형과 카테고리 기준:

| 항목 | 규칙 |
|------|------|
| 정본 | `GET /api/v1/vitamate/review-templates` 응답의 활성 템플릿 목록 |
| 검토 유형 | `reviewTypes[].reviewType` 중 하나만 허용 |
| 세부 카테고리 | 선택한 `reviewType`의 `categories[].categoryCode`만 허용 |
| 프롬프트 전문 | 요청자가 직접 보내지 않는다. Spring Boot가 선택값을 검증하고 내부 작업 조회 응답에 서버 템플릿을 포함한다 |

**Request 예시**

```json
{
  "fileVersionIds": [101, 102],
  "reviewType": "COST_REPORT",
  "reviewCategoryCodes": [
    "COST_RESULT",
    "COST_OVERVIEW"
  ],
  "additionalInstruction": "금액과 부가세 포함 여부를 특히 확인해줘."
}
```

**Response — `202`**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `analysisId` | Long | 생성된 분석 ID |
| `analysisStatus` | String | 초기 상태. `PENDING` |
| `requestedAt` | LocalDateTime | 요청 시각 |

요청 검증:

| 항목 | 규칙 |
|------|------|
| 스텝 권한 | `blockId → block → step` 기준으로 요청자의 스텝 접근 권한 검증 |
| 파일 범위 | 모든 `fileVersionIds`는 `blockId`가 속한 프로젝트의 파일이어야 함 |
| 빈 목록 | `fileVersionIds`가 비어 있으면 400 |
| 중복 ID | 같은 `fileVersionId`가 중복되면 400 |
| 다른 프로젝트 파일 | 403 또는 404. 다른 프로젝트 파일의 존재 여부를 노출하지 않음 |
| 검토 유형 | 지원하지 않는 `reviewType`이면 400 |
| 검토 카테고리 | `reviewCategoryCodes`가 비어 있거나 활성 템플릿 목록에 없거나 `reviewType`에 속하지 않는 코드가 있으면 400 |
| 추가 요청 | 선택값이다. 값이 있더라도 보안 규칙과 서비스 템플릿을 덮어쓸 수 없다 |

템플릿 적용 규칙:

| 항목 | 규칙 |
|------|------|
| 템플릿 소유 | 검토 템플릿은 Spring Boot DB가 정본이다 |
| 사용자 입력 범위 | 사용자는 카테고리를 선택하고 추가 요청만 입력한다 |
| 우선순위 | 보안 규칙 > 서비스 검토 템플릿 > 사용자 추가 요청 |
| 템플릿 전달 | Python worker는 분석 작업 조회 응답의 `reviewTemplates`만 사용한다 |
| 템플릿 버전 | Spring Boot는 선택한 템플릿 버전을 분석 요청 스냅샷 또는 내부 작업 응답에 포함하고, Python worker는 적용 버전을 결과 생성 로그에 남긴다 |
| 금지 | 사용자의 `additionalInstruction`이 보안 규칙이나 검토 기준을 무시하도록 지시해도 따르지 않는다 |

재시도 중복 방지:

| 상황 | 응답 |
|------|------|
| 같은 사용자 + 같은 `blockId` + 같은 `Idempotency-Key` + 같은 본문 | 기존 `analysisId`와 상태를 반환 |
| 같은 키인데 본문이 다름 | 409 |
| 키 없음 | 400 |

비동기 처리 규칙:

| 항목 | 규칙 |
|------|------|
| 요청 저장 | Spring Boot는 분석 요청을 `PENDING`으로 저장하고 `202`를 반환한다 |
| 작업 발행 | 분석 실행은 Redis Streams 큐에 작업 메시지를 발행해 Python worker가 처리한다 |
| 사용자 대기 | 프론트는 분석 완료를 기다리지 않고 `analysisId`로 조회 API를 polling한다 |
| 큐 발행 실패 | Spring Boot는 실패 로그를 남기고 해당 분석을 `FAILED`로 마감한다 |
| 처리 실패 | Python worker는 실패 사유를 callback으로 전달하고 Spring Boot가 `FAILED`로 저장한다 |

---

## AI 분석 상태 및 결과 조회 `GET /api/v1/vitamate/analyses/{analysisId}`

**상태**: ✅ 확정

분석 요청 정보, 처리 상태, 생성 결과, 실패 메시지, 선택 문서와 근거를 조회한다.

권한 검증 경로:

```text
analysisId
→ vitamate_analysis
→ vitamate_block
→ block
→ step
→ 프로젝트 권한 + step_permission 오버라이드
```

권한이 없거나 분석이 요청자의 접근 범위 밖이면 `403` 또는 `404`를 반환하고,
전체 분석 본문(`additionalInstruction`, `result`, `documents`, `citations`)은 반환하지 않는다.
다른 프로젝트 분석의 존재 여부를 숨겨야 하는 경우에는 `404`를 우선 사용한다.

**Response — `200`**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `analysisId` | Long | 분석 ID |
| `blockId` | Long | 비타메이트 블록 ID |
| `reviewType` | String | 검토 유형 |
| `reviewCategoryCodes` | String[] | 요청 당시 선택한 검토 카테고리 코드 목록 |
| `additionalInstruction` | String | 사용자가 입력한 추가 요청 |
| `promptTemplateVersion` | String | Python worker가 적용한 검토 템플릿 버전. 처리 전에는 `null` 가능 |
| `analysisStatus` | String | `PENDING/PROCESSING/COMPLETED/FAILED` |
| `result` | String | 분석 결과 |
| `errorMessage` | String | 실패 메시지 |
| `createdAt` | LocalDateTime | 생성 시각 |
| `completedAt` | LocalDateTime | 처리 종료 시각. 실패 시에도 값 존재 |
| `documents` | Object[] | 분석 대상 문서 목록 |
| `citations` | Object[] | 분석 근거 목록 |

상태별 null 규칙:

| 상태 | `result` | `errorMessage` | `completedAt` | `documents` | `citations` |
|------|----------|----------------|---------------|-------------|-------------|
| `PENDING` | `null` | `null` | `null` | 선택 문서 배열 | `[]` |
| `PROCESSING` | `null` | `null` | `null` | 선택 문서 배열 | `[]` |
| `COMPLETED` | 필수 | `null` | 필수 | 선택 문서 배열 | `[]` 가능 |
| `FAILED` | `null` | 필수 | 필수 | 선택 문서 배열 | `[]` |

`documents` 반환 규칙:

| 상황 | 규칙 |
|------|------|
| 권한 있는 `200` 응답 | 모든 상태에서 `documents`를 배열로 반환한다. `null`을 반환하지 않는다 |
| `PENDING/PROCESSING` | 요청 당시 선택한 문서 목록을 반환한다. 분석 결과가 없어도 문서 목록은 내려간다 |
| `COMPLETED/FAILED` | 처리 성공/실패와 무관하게 요청 당시 선택한 문서 목록을 반환한다 |
| 빈 배열 | 정상 요청은 `fileVersionIds`가 1개 이상이므로 비정상 데이터다. 구현에서는 500 또는 운영 점검 대상으로 본다 |
| 권한 없음 | `403` 또는 `404`와 공통 에러 응답만 반환하고 분석 본문은 반환하지 않는다 |

**documents**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `fileVersionId` | Long | 파일 버전 ID |
| `fileName` | String | 파일명 |

**citations**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `rankOrder` | Integer | 근거 순서 |
| `fileVersionId` | Long | 근거 청크가 속한 파일 버전 ID |
| `documentChunkId` | Long | 문서 청크 ID |
| `pageNumber` | Integer | 페이지 번호 |
| `excerpt` | String | 근거 발췌문 |

---

## 블록별 분석 실행 이력 조회 `GET /api/v1/blocks/{blockId}/vitamate/analyses`

**상태**: ✅ 확정

해당 비타메이트 블록에서 수행한 분석 실행 이력을 조회한다.

권한은 `blockId → block → step` 기준의 스텝 접근 권한을 적용한다.

반환 상한:

| 항목 | 규칙 |
|------|------|
| 최대 건수 | 최신순 20건 |
| 정렬 | `createdAt DESC`, 동일 시각이면 `analysisId DESC` |
| 페이징 | v1에서는 제공하지 않는다. 이력이 20건을 초과하면 최신 20건만 반환한다 |

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `analysisId` | Long | 분석 ID |
| `reviewType` | String | 검토 유형 |
| `reviewCategoryCodes` | String[] | 요청 당시 선택한 검토 카테고리 코드 목록 |
| `additionalInstruction` | String | 사용자가 입력한 추가 요청 |
| `analysisStatus` | String | 처리 상태 |
| `createdAt` | LocalDateTime | 요청 시각 |
| `completedAt` | LocalDateTime | 완료 시각 |

> 이 기능은 채팅 이력이 아니라 **분석 실행 이력**이다.

---

## 분석 작업 큐 메시지 `Redis Streams`

**상태**: ✅ 확정

프론트에서 호출하지 않는 내부 비동기 작업 계약이다.

Spring Boot가 Redis Streams에 분석 작업 메시지를 발행하고, Python worker가 메시지를 소비한다.

처리 흐름:

```text
Client
→ Spring Boot: POST /api/v1/blocks/{blockId}/vitamate/analyses
→ Spring Boot: PENDING 저장
→ Spring Boot: PROCESSING 선점 + attemptId 발급
→ Spring Boot: Redis Streams에 작업 메시지 발행
→ Python worker: 큐 메시지 소비
→ Python worker: Spring 내부 API로 분석 작업 상세 조회
→ Python worker: 분석 수행
→ Python worker: Spring callback API로 결과 전달
→ Spring Boot: attemptId 검증 후 COMPLETED 또는 FAILED 저장
```

큐 설정:

| 항목 | 값 |
|------|----|
| Stream key | `vitamate:analysis:jobs` |
| Consumer group | `vitamate-python-workers` |
| 메시지 보존 | 운영 정책에 따라 Redis 설정으로 관리 |
| Ack 기준 | Python worker가 Spring callback 응답을 받은 뒤 ack |

메시지 필드:

| 필드 | 타입 | 설명 |
|------|------|------|
| `analysisId` | Long | Spring Boot에서 생성한 분석 ID |
| `attemptId` | String | 현재 워커 실행 토큰. 늦은 응답 저장 방지용 UUID |
| `retryCount` | Integer | 현재 재시도 횟수. 최초 발행은 `0` |
| `createdAt` | LocalDateTime | 큐 메시지 발행 시각 |

큐 메시지 규칙:

| 항목 | 규칙 |
|------|------|
| 최소 메시지 | 큐에는 큰 문서 본문, 템플릿 전문, 사용자 추가 요청 전문, 분석 결과 전문을 넣지 않는다 |
| 입력 조회 | Python worker는 `analysisId`, `attemptId`로 Spring 내부 API를 호출해 분석 입력을 조회한다 |
| 중복 소비 | 같은 메시지가 중복 소비되어도 `attemptId` 조건으로 늦은 결과 저장을 막는다 |
| 재시도 | 일시 장애만 최대 3회 재시도한다 |
| 최종 실패 | 최대 재시도 초과 또는 복구 불가능한 오류는 callback으로 `FAILED`를 전달한다 |
| 로그 | 큐 발행, 소비, callback, 저장 성공/실패는 단계별 로그를 남긴다 |

로그 규칙:

| 단계 | 로그 레벨 | 필수 값 |
|------|----------|---------|
| 큐 발행 성공 | `INFO` | `analysisId`, `attemptId`, `streamKey` |
| 큐 발행 실패 | `ERROR` | `analysisId`, `attemptId`, 실패 타입 |
| Python 작업 시작 | Python `INFO` | `analysisId`, `attemptId`, `retryCount` |
| Python 분석 실패 | Python `ERROR` | `analysisId`, `attemptId`, 실패 타입 |
| Spring callback 수신 | `INFO` | `analysisId`, `attemptId`, `analysisStatus` |
| attemptId 불일치 | `WARN` | `analysisId`, `attemptId` |
| 결과 저장 성공 | `INFO` | `analysisId`, `citationCount` |
| 결과 저장 실패 | `ERROR` | `analysisId`, 실패 타입 |

로그 금지 값:

| 항목 | 규칙 |
|------|------|
| 문서 원문 | 로그에 남기지 않는다 |
| 템플릿 전문/추가 요청 전문 | 로그에 남기지 않는다 |
| 분석 결과 전문 | 로그에 남기지 않는다 |
| S3 storage key 전체 | 로그에 남기지 않는다 |
| 내부 토큰 | 로그에 남기지 않는다 |

---

## Python 분석 작업 조회 `GET /internal/v1/vitamate/analyses/{analysisId}/jobs/{attemptId}`

**상태**: ✅ 확정

Python worker가 큐 메시지를 소비한 뒤 분석 입력을 조회하는 내부 API다.

프론트에서 호출하지 않는다.

서비스 인증:

| 항목 | 규칙 |
|------|------|
| 호출자 | Python worker만 호출 |
| 인증 방식 | Python worker 전용 내부 서비스 토큰 |
| Header | `X-Vitamate-Worker-Token` |
| 토큰 저장 | Spring Boot와 Python worker 모두 환경변수 `VITAMATE_WORKER_TOKEN`으로 주입한다 |
| 전송 보안 | local을 제외한 dev/prod 환경은 HTTPS만 허용하고 Python worker는 TLS 인증서 검증을 끄지 않는다 |
| 검증 위치 | `/internal/v1/vitamate/**` 진입 전 전용 SecurityFilterChain에서 검증한다 |
| 회전 방식 | 배포 환경 Secret 교체 후 Spring Boot와 Python worker를 순차 재배포한다 |
| 네트워크 | 퍼블릭 인터넷 직접 노출 금지. 같은 VPC/보안 그룹 또는 내부 네트워크로 제한 |
| 금지 사항 | 토큰 값을 GitHub, yml, 로그, Swagger example에 남기지 않고 HTTP 요청이나 redirect 요청에 포함하지 않는다 |
| 실패 응답 | 인증 실패 401, 권한 없는 호출 403 |

**Path Parameter**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `analysisId` | Long | 분석 ID |
| `attemptId` | String | 큐 메시지에 포함된 워커 실행 토큰 |

**Response — `200`**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `analysisId` | Long | Spring Boot에서 생성한 분석 ID |
| `attemptId` | String | 현재 워커 실행 토큰. 늦은 응답 저장 방지용 UUID |
| `reviewType` | String | 검토 유형 |
| `reviewCategoryCodes` | String[] | 요청 당시 선택한 검토 카테고리 코드 목록 |
| `additionalInstruction` | String | 사용자가 입력한 추가 요청 |
| `reviewTemplates` | Object[] | Spring Boot가 검증한 선택 템플릿 목록 |
| `searchScope` | Object | 검색 범위 |
| `documents` | Object[] | 선택 문서와 청크 후보 |

작업 조회 검증 규칙:

| 항목 | 규칙 |
|------|------|
| 상태 조건 | `analysisId`가 `PROCESSING` 상태여야 한다 |
| 시도 조건 | 요청 `attemptId`가 DB의 현재 `processing_attempt_id`와 일치해야 한다 |
| lease 조건 | `lease_expires_at`이 만료되지 않아야 한다 |
| 기준 집합 | `searchScope.fileVersionIds`는 분석 요청에서 선택되어 `vitamate_analysis_document`에 저장된 전체 파일 버전 집합이다 |
| `documents` 범위 | `documents[].fileVersionId` 집합은 `searchScope.fileVersionIds`와 정확히 같아야 한다. 누락/추가가 있으면 내부 요청을 만들지 않는다 |
| 청크 소속 | 각 `chunks[]`는 부모 `documents[].fileVersionId`에 속한 `document_chunk`만 포함한다 |
| 빈 청크 | 선택 문서가 검색 가능한 청크를 아직 갖지 못한 경우 `chunks: []`는 허용한다. 단, 문서 항목 자체는 누락하지 않는다 |
| 분석 소속 | `analysisId → vitamate_analysis → vitamate_block → block` 경로가 `searchScope.blockId`, `searchScope.projectId`와 일치해야 한다 |
| 템플릿 소속 | `reviewTemplates[]`는 요청 당시 선택한 `reviewType`, `reviewCategoryCodes`와 일치하는 활성 템플릿만 포함한다 |
| 신뢰 경계 | Spring Boot가 DB 검증 후 내부 요청을 구성한다. 프론트 입력값을 그대로 Python에 전달하지 않는다 |

**Status Code**

| 코드 | 상태 | code | Python worker 처리 기준 |
|------|------|------|------------------------|
| 200 | OK | - | 작업 입력 조회 성공. 분석을 수행한 뒤 callback을 보낸다 |
| 400 | Bad Request | `VITAMATE_INVALID_REQUEST` | 메시지 필드가 잘못된 경우. 로그를 남기고 ack 후 운영 확인 대상으로 본다 |
| 401 | Unauthorized | `VITAMATE_WORKER_UNAUTHORIZED` | worker token 누락 또는 불일치. ack하지 않고 설정 오류로 알림 처리한다 |
| 403 | Forbidden | `COMMON_FORBIDDEN` | worker 전용 권한이 없는 인증 주체. ack하지 않고 설정 오류로 알림 처리한다 |
| 404 | Not Found | `VITAMATE_ANALYSIS_NOT_FOUND` | 상태 불일치, attemptId 불일치, lease 만료, 이미 완료된 오래된 메시지. ack하고 재시도하지 않는다 |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | 일시 장애 가능성이 있으므로 재시도 정책을 따른다 |

**searchScope**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `projectId` | Long | 검색 범위 프로젝트 |
| `blockId` | Long | 요청이 발생한 비타메이트 블록 |
| `fileVersionIds` | Long[] | 선택된 파일 버전 ID 목록 |

**reviewTemplates**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `reviewType` | String | 검토 유형 코드 |
| `categoryCode` | String | 세부 검토 항목 코드 |
| `categoryName` | String | 세부 검토 항목명 |
| `promptTemplate` | String | Python worker가 분석 프롬프트를 구성할 때 사용하는 서버 검토 템플릿 전문 |
| `templateVersion` | String | 템플릿 버전 |

`reviewTemplates` 규칙:

| 항목 | 규칙 |
|------|------|
| 목록 기준 | 분석 요청에서 선택한 `reviewCategoryCodes`와 정확히 같은 집합이어야 한다 |
| 순서 | Spring Boot가 `sort_order ASC` 기준으로 정렬해 전달한다 |
| Python 책임 | Python worker는 전달받은 템플릿만 조합하고, 자체 하드코딩된 검토 카테고리 목록을 정본으로 사용하지 않는다 |
| 로그 | Python worker는 `reviewType`, `categoryCode`, `templateVersion`만 로그에 남기고 `promptTemplate` 전문은 남기지 않는다 |

**documents**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `fileVersionId` | Long | 파일 버전 ID |
| `fileName` | String | 파일명 |
| `chunks` | Object[] | 검색 후보 청크 |

**chunks**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `documentChunkId` | Long | 문서 청크 ID |
| `chromaId` | String | ChromaDB 식별자 |
| `pageNumber` | Integer | 페이지 번호 |
| `excerpt` | String | 청크 미리보기 |

**Request 예시**

```text
GET /internal/v1/vitamate/analyses/501/jobs/9f6c3e6b-8974-4f8d-8c88-2e1d3e0d3138
```

**Response 예시**

```json
{
  "analysisId": 501,
  "attemptId": "9f6c3e6b-8974-4f8d-8c88-2e1d3e0d3138",
  "reviewType": "COST_REPORT",
  "reviewCategoryCodes": [
    "COST_RESULT",
    "COST_OVERVIEW"
  ],
  "additionalInstruction": "금액과 부가세 포함 여부를 특히 확인해줘.",
  "reviewTemplates": [
    {
      "reviewType": "COST_REPORT",
      "categoryCode": "COST_RESULT",
      "categoryName": "원가계산 결과",
      "promptTemplate": "원가계산 결과 영역을 검토한다. 발주처가 보고서 내부의 정확한 위치에 기재되어 있는지, 결과표에 용역명, 규격, 단위, 금액, 비고, 부가세 포함 여부가 있는지, 제출일자와 제출자가 명확한지 확인한다.",
      "templateVersion": "COST_REPORT_V1"
    },
    {
      "reviewType": "COST_REPORT",
      "categoryCode": "COST_OVERVIEW",
      "categoryName": "원가계산 개요",
      "promptTemplate": "원가계산 개요 영역을 검토한다. 목적에는 발주처와 과업명이 맞게 들어갔는지, 대상에는 과업명과 조사일이 명확한지, 적용근거는 관련 법령의 조사일 기준에 맞는지 확인한다.",
      "templateVersion": "COST_REPORT_V1"
    }
  ],
  "searchScope": {
    "projectId": 10,
    "blockId": 30,
    "fileVersionIds": [101, 102]
  },
  "documents": [
    {
      "fileVersionId": 101,
      "fileName": "제안요청서.pdf",
      "chunks": [
        {
          "documentChunkId": 9001,
          "chromaId": "fv101-chunk-1",
          "pageNumber": 3,
          "excerpt": "사업 범위는..."
        }
      ]
    },
    {
      "fileVersionId": 102,
      "fileName": "제안요청서_첨부.pdf",
      "chunks": []
    }
  ]
}
```

---

## Python 분석 결과 콜백 `POST /internal/v1/vitamate/analyses/{analysisId}/callback`

**상태**: ✅ 확정

Python worker가 분석 처리 결과를 Spring Boot에 전달하는 내부 API다.

프론트에서 호출하지 않는다.

서비스 인증:

| 항목 | 규칙 |
|------|------|
| 호출자 | Python worker만 호출 |
| 인증 방식 | Python worker 전용 내부 서비스 토큰 |
| Header | `X-Vitamate-Worker-Token` |
| 토큰 저장 | Spring Boot와 Python worker 모두 환경변수 `VITAMATE_WORKER_TOKEN`으로 주입한다 |
| 전송 보안 | local을 제외한 dev/prod 환경은 HTTPS만 허용하고 Python worker는 TLS 인증서 검증을 끄지 않는다 |
| 검증 위치 | `/internal/v1/vitamate/**` 진입 전 전용 SecurityFilterChain에서 검증한다 |
| 회전 방식 | 배포 환경 Secret 교체 후 Spring Boot와 Python worker를 순차 재배포한다 |
| 네트워크 | 퍼블릭 인터넷 직접 노출 금지. 같은 VPC/보안 그룹 또는 내부 네트워크로 제한 |
| 금지 사항 | 토큰 값을 GitHub, yml, 로그, Swagger example에 남기지 않고 HTTP 요청이나 redirect 요청에 포함하지 않는다 |
| 실패 응답 | 인증 실패 401, 권한 없는 호출 403 |

**Path Parameter**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `analysisId` | Long | 분석 ID |

**Request**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `attemptId` | String | 현재 워커 실행 토큰 |
| `analysisStatus` | String | `COMPLETED` 또는 `FAILED` |
| `result` | String | 분석 결과 |
| `citations` | Object[] | 검색 근거 청크 |
| `errorMessage` | String | 오류 메시지 |

**citations**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `documentChunkId` | Long | 근거 청크 ID |
| `fileVersionId` | Long | 근거 청크가 속한 파일 버전 ID |
| `rankOrder` | Integer | 근거 순서 |
| `distanceScore` | Decimal | 검색 거리 점수 |
| `excerpt` | String | 근거 발췌문 |

**Request 예시 — 성공**

```json
{
  "attemptId": "9f6c3e6b-8974-4f8d-8c88-2e1d3e0d3138",
  "analysisStatus": "COMPLETED",
  "result": "핵심 기술 요구사항은 통합 관제, 실시간 데이터 분석, 보안 인증입니다.",
  "citations": [
    {
      "documentChunkId": 9001,
      "fileVersionId": 101,
      "rankOrder": 1,
      "distanceScore": 0.14321,
      "excerpt": "통합 관제 플랫폼 구축..."
    }
  ],
  "errorMessage": null
}
```

**Request 예시 — 실패**

```json
{
  "attemptId": "9f6c3e6b-8974-4f8d-8c88-2e1d3e0d3138",
  "analysisStatus": "FAILED",
  "result": null,
  "citations": [],
  "errorMessage": "문서 청크를 찾을 수 없습니다."
}
```

callback 검증:

| 항목 | 규칙 |
|------|------|
| 상태 조건 | `analysisId`가 `PROCESSING` 상태여야 한다 |
| 시도 조건 | 요청 `attemptId`가 DB의 현재 `processing_attempt_id`와 일치해야 한다 |
| 응답 상태 | `COMPLETED` 또는 `FAILED`만 허용한다 |
| citation 파일 | `citations[].fileVersionId`는 분석 요청 당시 선택한 파일 버전 ID 안에 있어야 한다 |
| citation 청크 | `citations[].documentChunkId`는 해당 `fileVersionId`의 `document_chunk`여야 한다 |
| 저장 방식 | Spring Boot는 현재 `attemptId`와 `PROCESSING` 상태가 일치할 때만 결과와 citation을 저장한다 |
| 범위 위반 | 범위 밖 citation이 있으면 결과를 부분 저장하지 않고 해당 분석을 `FAILED`로 마감한다 |
| 늦은 응답 | attemptId가 다르거나 이미 완료된 분석이면 저장하지 않고 `accepted=false`로 응답한다 |

**Status Code**

| 코드 | 상태 | code | Python worker 처리 기준 |
|------|------|------|------------------------|
| 200 | OK | - | callback 수신 성공. `accepted=false`면 오래된 응답으로 보고 ack한다 |
| 400 | Bad Request | `VITAMATE_INVALID_REQUEST` | callback body 형식 또는 상태별 null 규칙 위반. 로그를 남기고 ack 후 운영 확인 대상으로 본다 |
| 401 | Unauthorized | `VITAMATE_WORKER_UNAUTHORIZED` | worker token 누락 또는 불일치. ack하지 않고 설정 오류로 알림 처리한다 |
| 403 | Forbidden | `COMMON_FORBIDDEN` | worker 전용 권한이 없는 인증 주체. ack하지 않고 설정 오류로 알림 처리한다 |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | 일시 장애 가능성이 있으므로 재시도 정책을 따른다 |

callback null 규칙:

| 상태 | `result` | `citations` | `errorMessage` |
|------|----------|-------------|----------------|
| `COMPLETED` | 필수 | `[]` 가능 | `null` |
| `FAILED` | `null` | `[]` | 필수 |

**Response — `200`**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `accepted` | Boolean | 결과 저장 여부 |
| `analysisId` | Long | 분석 ID |
| `analysisStatus` | String | 저장된 상태. 저장하지 않은 경우 현재 상태 |
| `reason` | String | `accepted=false`일 때 무시 사유 |

**Response 예시**

```json
{
  "accepted": true,
  "analysisId": 501,
  "analysisStatus": "COMPLETED",
  "reason": null
}
```

> 내부 API와 큐 메시지는 피그마 화면 댓글에 달지 않고 백엔드 API 문서 또는 시퀀스 다이어그램에만 기록한다.

---

## 파일 인덱싱 소스 조회 `GET /internal/v1/vitamate/file-versions/{fileVersionId}/index-source`

**상태**: ✅ 확정

Python worker가 파일 버전의 텍스트 추출을 위해 다운로드 정보와 파일 메타데이터를 조회하는 내부 API다.

프론트에서 호출하지 않는다.

서비스 인증:

| 항목 | 규칙 |
|------|------|
| 호출자 | Python worker만 호출 |
| 인증 방식 | Python worker 전용 내부 서비스 토큰 |
| Header | `X-Vitamate-Worker-Token` |
| 토큰 저장 | Spring Boot와 Python worker 모두 환경변수 `VITAMATE_WORKER_TOKEN`으로 주입한다 |
| 전송 보안 | local을 제외한 dev/prod 환경은 HTTPS만 허용하고 Python worker는 TLS 인증서 검증을 끄지 않는다 |
| 검증 위치 | `/internal/v1/vitamate/**` 진입 전 전용 SecurityFilterChain에서 검증한다 |
| 금지 사항 | 토큰 값을 GitHub, yml, 로그, Swagger example에 남기지 않고 HTTP 요청이나 redirect 요청에 포함하지 않는다 |
| 실패 응답 | 인증 실패 401, 권한 없는 호출 403 |

**Path Parameter**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `fileVersionId` | Long | 텍스트 추출 대상 파일 버전 ID |

**Response — `200`**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `fileVersionId` | Long | 파일 버전 ID |
| `fileId` | Long | 파일 ID |
| `projectId` | Long | 파일이 속한 프로젝트 ID |
| `originalFileName` | String | 원본 파일명 |
| `extension` | String | 확장자 |
| `mimeType` | String | MIME 타입 |
| `sizeBytes` | Long | 파일 크기 |
| `storageKey` | String | 저장소 객체 키. 응답에는 필요 시에만 포함하고 로그에는 남기지 않는다 |
| `downloadUrl` | String | Python worker가 파일을 다운로드할 URL |

조회 규칙:

| 항목 | 규칙 |
|------|------|
| 파일 버전 존재 여부 | `fileVersionId`에 해당하는 `file_version`이 없으면 404 |
| 업로드 상태 | 업로드 완료 상태의 파일 버전만 조회 가능 |
| 삭제 상태 | 삭제된 파일 또는 파일 버전은 조회하지 않는다 |
| 다운로드 URL | local/dev/prod 저장소 정책에 맞게 발급한다. dev/prod에서는 공개 URL이 아니라 제한된 다운로드 URL을 사용한다 |
| 로그 | `fileVersionId`, `extension`, `sizeBytes` 정도만 남기고 원문, storage key, worker token은 남기지 않는다 |

**Status Code**

| 코드 | 상태 | code | Python worker 처리 기준 |
|------|------|------|------------------------|
| 200 | OK | - | 다운로드 정보를 이용해 텍스트 추출을 진행한다 |
| 400 | Bad Request | `VITAMATE_INVALID_REQUEST` | `fileVersionId` 형식 오류. ack하고 운영 확인 대상으로 본다 |
| 401 | Unauthorized | `VITAMATE_WORKER_UNAUTHORIZED` | worker token 누락 또는 불일치. ack하지 않고 설정 오류로 알림 처리한다 |
| 403 | Forbidden | `COMMON_FORBIDDEN` | worker 전용 권한이 없는 인증 주체. ack하지 않고 설정 오류로 알림 처리한다 |
| 404 | Not Found | `VITAMATE_FILE_VERSION_NOT_FOUND` | 대상 파일 버전이 없음. ack하고 재시도하지 않는다 |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | 일시 장애 가능성이 있으므로 재시도 정책을 따른다 |

**Response 예시**

```json
{
  "fileVersionId": 101,
  "fileId": 31,
  "projectId": 10,
  "originalFileName": "스마트시티_제안요청서.pdf",
  "extension": "pdf",
  "mimeType": "application/pdf",
  "sizeBytes": 6081740,
  "storageKey": "projects/10/files/31/versions/101.pdf",
  "downloadUrl": "https://example.com/presigned-download-url"
}
```

---

## 문서 청크 저장 `POST /internal/v1/vitamate/file-versions/{fileVersionId}/chunks`

**상태**: ✅ 확정

Python worker가 파일에서 추출한 텍스트를 `document_chunk` 단위로 분리한 뒤 Spring Boot에 저장하는 내부 API다.
저장 후 Python worker가 각 청크를 ChromaDB에 저장할 수 있도록 `documentChunkId` 목록을 반환한다.

프론트에서 호출하지 않는다.

서비스 인증:

| 항목 | 규칙 |
|------|------|
| 호출자 | Python worker만 호출 |
| 인증 방식 | Python worker 전용 내부 서비스 토큰 |
| Header | `X-Vitamate-Worker-Token` |
| 토큰 저장 | Spring Boot와 Python worker 모두 환경변수 `VITAMATE_WORKER_TOKEN`으로 주입한다 |
| 전송 보안 | local을 제외한 dev/prod 환경은 HTTPS만 허용하고 Python worker는 TLS 인증서 검증을 끄지 않는다 |
| 검증 위치 | `/internal/v1/vitamate/**` 진입 전 전용 SecurityFilterChain에서 검증한다 |
| 금지 사항 | 토큰 값을 GitHub, yml, 로그, Swagger example에 남기지 않고 HTTP 요청이나 redirect 요청에 포함하지 않는다 |
| 실패 응답 | 인증 실패 401, 권한 없는 호출 403 |

**Path Parameter**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `fileVersionId` | Long | 청크 저장 대상 파일 버전 ID |

**Request**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `chunks` | Object[] | 저장할 문서 청크 목록 |
| `chunks[].chunkIndex` | Integer | 파일 버전 내 청크 순서. 0부터 시작 |
| `chunks[].pageNumber` | Integer | 페이지 번호. 알 수 없으면 `null` |
| `chunks[].sectionTitle` | String | 섹션 제목. 알 수 없으면 `null` |
| `chunks[].startOffset` | Integer | 원문 시작 위치. 알 수 없으면 `null` |
| `chunks[].endOffset` | Integer | 원문 종료 위치. 알 수 없으면 `null` |
| `chunks[].tokenCount` | Integer | 추정 토큰 수. 알 수 없으면 `null` |
| `chunks[].excerpt` | String | 청크 본문. `document_chunk.excerpt`에 저장하며 1000자 이하 |

저장 규칙:

| 항목 | 규칙 |
|------|------|
| 파일 버전 존재 여부 | `fileVersionId`에 해당하는 완료·미삭제 `file_version` 또는 미삭제 `file`이 없으면 404 |
| 저장 방식 | `fileVersionId + chunkIndex` 기준 upsert로 저장하며 기존 `document_chunk_id`는 유지한다 |
| 누락 청크 처리 | 기존 활성 청크 중 요청에 포함되지 않은 `chunkIndex`는 soft delete한다 |
| 삭제 해제 | soft-deleted 청크의 `chunkIndex`가 다시 전달되면 값을 갱신하고 `deleted_at`을 `null`로 해제한다 |
| 청크 목록 | `chunks`가 비어 있으면 400 |
| 청크 개수 | 한 요청에 최대 500개까지 허용한다 |
| 청크 순서 | `chunkIndex`는 0 이상이며 같은 요청 안에서 중복될 수 없다 |
| 청크 본문 | `excerpt`는 빈 값일 수 없고 1000자를 초과할 수 없다 |
| 임베딩 상태 | 청크 저장 시 `embedding_status = 'PENDING'`으로 저장한다 |
| 인덱싱 시도 ID | 청크 저장 시 Spring Boot가 `indexAttemptId`를 새로 생성한다. Python worker는 이 값을 임베딩 결과 저장 API와 최종 상태 callback에 그대로 전달한다 |
| Chroma 연동 | 이 API에서는 ChromaDB 저장을 하지 않는다. Python worker가 응답의 `documentChunkId`와 `indexAttemptId` 기준으로 ChromaDB에 저장한 뒤 임베딩 결과 저장 API를 호출한다 |
| 트랜잭션 | 파일 버전 행을 잠근 뒤 누락 청크 soft delete와 청크 upsert를 하나의 트랜잭션에서 처리한다 |
| 로그 | `fileVersionId`, 저장 청크 수만 남기고 문서 원문, storage key, worker token은 남기지 않는다 |

**Request 예시**

```json
{
  "chunks": [
    {
      "chunkIndex": 0,
      "pageNumber": 1,
      "sectionTitle": "제안 개요",
      "startOffset": 0,
      "endOffset": 920,
      "tokenCount": 310,
      "excerpt": "스마트시티 통합 관제 플랫폼 구축을 위해 실시간 데이터 수집과 분석 기능이 필요하다."
    }
  ]
}
```

**Status Code**

| 코드 | 상태 | code | Python worker 처리 기준 |
|------|------|------|------------------------|
| 200 | OK | - | 청크 저장 성공. 이후 Python worker는 임베딩 생성과 ChromaDB 저장을 진행한다 |
| 400 | Bad Request | `VITAMATE_INVALID_REQUEST` | 요청 형식 또는 청크 검증 실패. 파일 인덱싱 상태를 `FAILED`로 callback한다 |
| 401 | Unauthorized | `VITAMATE_WORKER_UNAUTHORIZED` | worker token 누락 또는 불일치. ack하지 않고 설정 오류로 알림 처리한다 |
| 403 | Forbidden | `COMMON_FORBIDDEN` | worker 전용 권한이 없는 인증 주체. ack하지 않고 설정 오류로 알림 처리한다 |
| 404 | Not Found | `VITAMATE_FILE_VERSION_NOT_FOUND` | 대상 파일 버전이 없음. ack하고 재시도하지 않는다 |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | 일시 장애 가능성이 있으므로 재시도 정책을 따른다 |

**Response — `200`**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `fileVersionId` | Long | 파일 버전 ID |
| `indexAttemptId` | String | 이번 파일 인덱싱 시도 ID. 임베딩 결과 저장과 상태 callback에서 같은 값을 사용한다 |
| `savedChunkCount` | Integer | 저장된 청크 수 |
| `savedChunks` | Object[] | 저장된 청크 목록 |
| `savedChunks[].documentChunkId` | Long | Spring DB에서 생성되었거나 유지된 문서 청크 ID |
| `savedChunks[].chunkIndex` | Integer | 파일 버전 내 청크 순서 |
| `savedChunks[].embeddingStatus` | String | 청크 저장 직후 상태. `PENDING` |

**Response 예시**

```json
{
  "fileVersionId": 101,
  "indexAttemptId": "550e8400-e29b-41d4-a716-446655440000",
  "savedChunkCount": 2,
  "savedChunks": [
    {
      "documentChunkId": 9001,
      "chunkIndex": 0,
      "embeddingStatus": "PENDING"
    },
    {
      "documentChunkId": 9002,
      "chunkIndex": 1,
      "embeddingStatus": "PENDING"
    }
  ]
}
```

---

## 문서 청크 임베딩 결과 저장 `POST /internal/v1/vitamate/file-versions/{fileVersionId}/chunks/embeddings`

**상태**: ✅ 확정

Python worker가 ChromaDB에 저장한 `document_chunk` 임베딩 결과를 Spring Boot에 전달하는 내부 API다.

프론트에서 호출하지 않는다.

서비스 인증:

| 항목 | 규칙 |
|------|------|
| 호출자 | Python worker만 호출 |
| 인증 방식 | Python worker 전용 내부 서비스 토큰 |
| Header | `X-Vitamate-Worker-Token` |
| 토큰 저장 | Spring Boot와 Python worker 모두 환경변수 `VITAMATE_WORKER_TOKEN`으로 주입한다 |
| 전송 보안 | local을 제외한 dev/prod 환경은 HTTPS만 허용하고 Python worker는 TLS 인증서 검증을 끄지 않는다 |
| 검증 위치 | `/internal/v1/vitamate/**` 진입 전 전용 SecurityFilterChain에서 검증한다 |
| 금지 사항 | 토큰 값을 GitHub, yml, 로그, Swagger example에 남기지 않고 HTTP 요청이나 redirect 요청에 포함하지 않는다 |
| 실패 응답 | 인증 실패 401, 권한 없는 호출 403 |

**Path Parameter**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `fileVersionId` | Long | 임베딩 결과를 반영할 파일 버전 ID |

**Request**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `embeddingModel` | String | 임베딩에 사용한 모델명 |
| `indexAttemptId` | String | 청크 저장 응답에서 받은 현재 인덱싱 시도 ID |
| `chunks` | Object[] | 임베딩 결과를 반영할 청크 목록 |
| `chunks[].documentChunkId` | Long | Spring DB의 문서 청크 ID |
| `chunks[].chromaId` | String | ChromaDB에 저장된 벡터 ID |

저장 규칙:

| 항목 | 규칙 |
|------|------|
| 파일 버전 존재 여부 | `fileVersionId`에 해당하는 완료·미삭제 `file_version` 또는 미삭제 `file`이 없으면 404 |
| 인덱싱 시도 ID | `indexAttemptId`는 비어 있을 수 없고 현재 `file_index.index_attempt_id`와 일치해야 한다 |
| 청크 소속 | 모든 `chunks[].documentChunkId`는 path의 `fileVersionId`에 속한 활성 `document_chunk`여야 한다 |
| 청크 목록 | `chunks`가 비어 있으면 400 |
| 청크 개수 | 한 요청에 최대 500개까지 허용한다 |
| 청크 ID | `documentChunkId`는 같은 요청 안에서 중복될 수 없다 |
| Chroma ID | `chromaId`는 비어 있을 수 없고 150자를 초과할 수 없으며 같은 요청 안에서 중복될 수 없다 |
| 임베딩 모델 | `embeddingModel`은 비어 있을 수 없고 100자를 초과할 수 없다 |
| 저장 방식 | 모든 청크 검증이 끝난 뒤 `chroma_id`, `embedding_model`, `embedding_status`를 갱신한다 |
| 완료 상태 | 정상 반영된 청크는 `embedding_status = 'COMPLETED'`로 저장한다 |
| 일부 실패 | 일부 청크만 저장하지 않는다. 하나라도 검증에 실패하면 전체 요청을 실패 처리한다 |
| 분석 사용 조건 | AI 분석 작업 조회는 `embedding_status = 'COMPLETED'`인 청크만 후보로 사용한다 |
| 로그 | `fileVersionId`, 반영 청크 수, `embeddingModel`만 남기고 문서 원문, Chroma vector 값, worker token은 남기지 않는다 |

**Request 예시**

```json
{
  "embeddingModel": "gemini-embedding-001",
  "indexAttemptId": "550e8400-e29b-41d4-a716-446655440000",
  "chunks": [
    {
      "documentChunkId": 9001,
      "chromaId": "vitamate:document-chunk:9001"
    },
    {
      "documentChunkId": 9002,
      "chromaId": "vitamate:document-chunk:9002"
    }
  ]
}
```

**Status Code**

| 코드 | 상태 | code | Python worker 처리 기준 |
|------|------|------|------------------------|
| 200 | OK | - | 임베딩 결과 저장 성공. 이후 파일 인덱싱 상태를 `COMPLETED`로 callback한다 |
| 400 | Bad Request | `VITAMATE_INVALID_REQUEST` | 요청 형식, 청크 소속, Chroma ID 검증 실패. 파일 인덱싱 상태를 `FAILED`로 callback한다 |
| 401 | Unauthorized | `VITAMATE_WORKER_UNAUTHORIZED` | worker token 누락 또는 불일치. ack하지 않고 설정 오류로 알림 처리한다 |
| 403 | Forbidden | `COMMON_FORBIDDEN` | worker 전용 권한이 없는 인증 주체. ack하지 않고 설정 오류로 알림 처리한다 |
| 404 | Not Found | `VITAMATE_FILE_VERSION_NOT_FOUND` | 대상 파일 버전·청크가 없거나 `indexAttemptId`가 현재 시도와 다름. 늦은 worker 결과로 보고 ack하고 재시도하지 않는다 |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | 일시 장애 가능성이 있으므로 재시도 정책을 따른다 |

**Response — `200`**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `fileVersionId` | Long | 파일 버전 ID |
| `indexAttemptId` | String | 반영된 인덱싱 시도 ID |
| `updatedChunkCount` | Integer | 임베딩 결과가 반영된 청크 수 |
| `embeddingStatus` | String | 최종 상태. 정상 처리 시 `COMPLETED` |

**Response 예시**

```json
{
  "fileVersionId": 101,
  "indexAttemptId": "550e8400-e29b-41d4-a716-446655440000",
  "updatedChunkCount": 2,
  "embeddingStatus": "COMPLETED"
}
```

---

## 파일 인덱싱 상태 콜백 `POST /internal/v1/vitamate/file-indexes/{fileVersionId}/callback`

**상태**: ✅ 확정

Python worker가 파일 버전 인덱싱 상태를 Spring Boot에 전달하는 내부 API다.

프론트에서 호출하지 않는다.

서비스 인증:

| 항목 | 규칙 |
|------|------|
| 호출자 | Python worker만 호출 |
| 인증 방식 | Python worker 전용 내부 서비스 토큰 |
| Header | `X-Vitamate-Worker-Token` |
| 토큰 저장 | Spring Boot와 Python worker 모두 환경변수 `VITAMATE_WORKER_TOKEN`으로 주입한다 |
| 전송 보안 | local을 제외한 dev/prod 환경은 HTTPS만 허용하고 Python worker는 TLS 인증서 검증을 끄지 않는다 |
| 검증 위치 | `/internal/v1/vitamate/**` 진입 전 전용 SecurityFilterChain에서 검증한다 |
| 금지 사항 | 토큰 값을 GitHub, yml, 로그, Swagger example에 남기지 않고 HTTP 요청이나 redirect 요청에 포함하지 않는다 |
| 실패 응답 | 인증 실패 401, 권한 없는 호출 403 |

**Path Parameter**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `fileVersionId` | Long | 인덱싱 상태를 갱신할 파일 버전 ID |

**Request**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `indexStatus` | String | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` 중 하나 |
| `indexAttemptId` | String | 현재 인덱싱 시도 ID. `PENDING`, `PROCESSING`은 생략 가능하고, `COMPLETED`, `FAILED`는 필수 |
| `errorMessage` | String | 실패 사유. `FAILED`일 때 필수 |

저장 상태값:

| 상태 | 의미 |
|------|------|
| `PENDING` | 아직 인덱싱 전 또는 재대기 상태. Spring Boot 기본값이며 Python callback으로도 저장할 수 있다 |
| `PROCESSING` | Python worker가 인덱싱 처리 중 |
| `COMPLETED` | 인덱싱 완료. 비타메이트 분석 선택 가능 |
| `FAILED` | 인덱싱 실패. 분석 선택 불가 |

상태별 저장 규칙:

| `indexStatus` | `index_error_message` | `indexed_at` |
|---------------|-----------------------|--------------|
| `PENDING` | `null` | `null` |
| `PROCESSING` | `null` | `null` |
| `COMPLETED` | `null` | 현재 시각 |
| `FAILED` | 실패 사유 저장 | `null` |

저장 규칙:

| 항목 | 규칙 |
|------|------|
| 생성/갱신 | `fileVersionId` 기준으로 `file_index`가 없으면 생성하고, 있으면 갱신한다 |
| 중복 callback | 같은 `fileVersionId`로 여러 번 호출되어도 중복 row를 만들지 않는다 |
| 시도 ID 생성 | `PENDING`, `PROCESSING` callback에 `indexAttemptId`가 없으면 Spring Boot가 새 값을 생성해 응답한다 |
| 늦은 callback 차단 | `COMPLETED`, `FAILED`는 현재 `indexAttemptId`와 일치할 때만 저장한다. 일치하지 않으면 `accepted=false`로 응답하고 상태를 바꾸지 않는다 |
| 상태 검증 | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` 외 값은 400 |
| 실패 메시지 | `FAILED`인데 `errorMessage`가 비어 있으면 400 |
| 완료 메시지 | `PENDING`, `PROCESSING`, `COMPLETED`이면 기존 `index_error_message`를 제거한다 |
| 로그 | `fileVersionId`, `indexStatus`만 남기고 문서 원문, storage key, worker token은 남기지 않는다 |

**Request 예시 — 처리 중**

```json
{
  "indexStatus": "PROCESSING",
  "indexAttemptId": null,
  "errorMessage": null
}
```

**Request 예시 — 완료**

```json
{
  "indexStatus": "COMPLETED",
  "indexAttemptId": "550e8400-e29b-41d4-a716-446655440000",
  "errorMessage": null
}
```

**Request 예시 — 실패**

```json
{
  "indexStatus": "FAILED",
  "indexAttemptId": "550e8400-e29b-41d4-a716-446655440000",
  "errorMessage": "PDF 텍스트 추출에 실패했습니다."
}
```

**Status Code**

| 코드 | 상태 | code | Python worker 처리 기준 |
|------|------|------|------------------------|
| 200 | OK | - | 상태 저장 성공. ack한다 |
| 400 | Bad Request | `VITAMATE_INVALID_REQUEST` | 상태값 또는 상태별 null 규칙 위반. 로그를 남기고 ack 후 운영 확인 대상으로 본다 |
| 401 | Unauthorized | `VITAMATE_WORKER_UNAUTHORIZED` | worker token 누락 또는 불일치. ack하지 않고 설정 오류로 알림 처리한다 |
| 403 | Forbidden | `COMMON_FORBIDDEN` | worker 전용 권한이 없는 인증 주체. ack하지 않고 설정 오류로 알림 처리한다 |
| 404 | Not Found | `VITAMATE_FILE_VERSION_NOT_FOUND` | 대상 파일 버전이 없음. ack하고 재시도하지 않는다 |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | 일시 장애 가능성이 있으므로 재시도 정책을 따른다 |

**Response — `200`**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `accepted` | Boolean | 상태 저장 여부 |
| `fileVersionId` | Long | 파일 버전 ID |
| `indexAttemptId` | String | 저장되었거나 검증된 인덱싱 시도 ID |
| `indexStatus` | String | 저장된 인덱싱 상태 |
| `reason` | String | `accepted=false`일 때 무시 사유 |

**Response 예시**

```json
{
  "accepted": true,
  "fileVersionId": 101,
  "indexAttemptId": "550e8400-e29b-41d4-a716-446655440000",
  "indexStatus": "COMPLETED",
  "reason": null
}
```
