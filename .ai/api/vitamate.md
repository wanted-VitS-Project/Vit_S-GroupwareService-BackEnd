# 비타메이트 API 명세

**노션 원본**: 사용자 제공 노션 정리본 (링크 미제공)
**최종 동기화**: 2026-08-05 (Queue + callback 비동기 분석 계약 반영)
**도메인 담당**: 정현

> 이 파일이 비타메이트 API 계약 기준이다. 임의 변경 금지.
> 변경이 필요하면 이 문서를 먼저 수정하고 프론트/Python 담당자에게 공유한다.

---

## 엔드포인트 목록

| 상태 | 기능 | METHOD | URL | 권한 |
|------|------|--------|-----|------|
| ✅ 확정 | 문서 분석 요청 | POST | `/api/v1/blocks/{blockId}/vitamate/analyses` | 스텝 접근 권한 |
| ✅ 확정 | AI 분석 상태 및 결과 조회 | GET | `/api/v1/vitamate/analyses/{analysisId}` | 스텝 접근 권한 |
| ✅ 확정 | 블록별 분석 실행 이력 조회 | GET | `/api/v1/blocks/{blockId}/vitamate/analyses` | 스텝 접근 권한 |
| ✅ 확정 | Python 분석 작업 조회 | GET | `/internal/v1/vitamate/analyses/{analysisId}/jobs/{attemptId}` | 내부 서버 |
| ✅ 확정 | Python 분석 결과 콜백 | POST | `/internal/v1/vitamate/analyses/{analysisId}/callback` | 내부 서버 |
| ✅ 확정 | 파일 인덱싱 상태 콜백 | POST | `/internal/v1/vitamate/file-indexes/{fileVersionId}/callback` | 내부 서버 |

---

## 문서 분석 요청 `POST /api/v1/blocks/{blockId}/vitamate/analyses`

**상태**: ✅ 확정

선택한 문서 버전과 프롬프트를 기준으로 AI 분석을 요청한다.

**Request**

| 위치 | 파라미터 | 타입 | 필수 | 설명 |
|------|---------|------|------|------|
| Header | `Idempotency-Key` | String | Y | 같은 사용자 동작의 재시도 중복 방지 키 |
| Path | `blockId` | Long | Y | 비타메이트 AI 블록 ID |
| Body | `fileVersionIds` | Long[] | Y | 분석할 파일 버전 ID 목록 |
| Body | `prompt` | String | Y | 분석 요청 프롬프트 |

**Request 예시**

```json
{
  "fileVersionIds": [101, 102],
  "prompt": "선택한 문서에서 핵심 기술 요구사항과 위험 요소를 정리해줘."
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
| 프롬프트 | 비어 있으면 400 |

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
전체 분석 본문(`prompt`, `result`, `documents`, `citations`)은 반환하지 않는다.
다른 프로젝트 분석의 존재 여부를 숨겨야 하는 경우에는 `404`를 우선 사용한다.

**Response — `200`**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `analysisId` | Long | 분석 ID |
| `blockId` | Long | 비타메이트 블록 ID |
| `prompt` | String | 분석 프롬프트 |
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

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `analysisId` | Long | 분석 ID |
| `prompt` | String | 프롬프트 |
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
| 최소 메시지 | 큐에는 큰 문서 본문, 프롬프트 전문, 분석 결과 전문을 넣지 않는다 |
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
| 프롬프트 전문 | 로그에 남기지 않는다 |
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
| 검증 위치 | `/internal/v1/vitamate/**` 진입 전 전용 SecurityFilterChain에서 검증한다 |
| 회전 방식 | 배포 환경 Secret 교체 후 Spring Boot와 Python worker를 순차 재배포한다 |
| 네트워크 | 퍼블릭 인터넷 직접 노출 금지. 같은 VPC/보안 그룹 또는 내부 네트워크로 제한 |
| 금지 사항 | 토큰 값을 GitHub, yml, 로그, Swagger example에 남기지 않는다 |
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
| `prompt` | String | 분석 프롬프트 |
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
  "prompt": "핵심 기술 요구사항과 위험 요소를 정리해줘.",
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
| 검증 위치 | `/internal/v1/vitamate/**` 진입 전 전용 SecurityFilterChain에서 검증한다 |
| 회전 방식 | 배포 환경 Secret 교체 후 Spring Boot와 Python worker를 순차 재배포한다 |
| 네트워크 | 퍼블릭 인터넷 직접 노출 금지. 같은 VPC/보안 그룹 또는 내부 네트워크로 제한 |
| 금지 사항 | 토큰 값을 GitHub, yml, 로그, Swagger example에 남기지 않는다 |
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
| 검증 위치 | `/internal/v1/vitamate/**` 진입 전 전용 SecurityFilterChain에서 검증한다 |
| 금지 사항 | 토큰 값을 GitHub, yml, 로그, Swagger example에 남기지 않는다 |
| 실패 응답 | 인증 실패 401, 권한 없는 호출 403 |

**Path Parameter**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `fileVersionId` | Long | 인덱싱 상태를 갱신할 파일 버전 ID |

**Request**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `indexStatus` | String | `PROCESSING`, `COMPLETED`, `FAILED` 중 하나 |
| `errorMessage` | String | 실패 사유. `FAILED`일 때 필수 |

저장 상태값:

| 상태 | 의미 |
|------|------|
| `PENDING` | 아직 인덱싱 전. Spring Boot의 기본 상태이며 Python callback으로 보내지 않는다 |
| `PROCESSING` | Python worker가 인덱싱 처리 중 |
| `COMPLETED` | 인덱싱 완료. 비타메이트 분석 선택 가능 |
| `FAILED` | 인덱싱 실패. 분석 선택 불가 |

상태별 저장 규칙:

| `indexStatus` | `index_error_message` | `indexed_at` |
|---------------|-----------------------|--------------|
| `PROCESSING` | `null` | `null` |
| `COMPLETED` | `null` | 현재 시각 |
| `FAILED` | 실패 사유 저장 | `null` |

저장 규칙:

| 항목 | 규칙 |
|------|------|
| 생성/갱신 | `fileVersionId` 기준으로 `file_index`가 없으면 생성하고, 있으면 갱신한다 |
| 중복 callback | 같은 `fileVersionId`로 여러 번 호출되어도 중복 row를 만들지 않는다 |
| 상태 검증 | `PROCESSING`, `COMPLETED`, `FAILED` 외 값은 400 |
| 실패 메시지 | `FAILED`인데 `errorMessage`가 비어 있으면 400 |
| 완료 메시지 | `COMPLETED` 또는 `PROCESSING`이면 기존 `index_error_message`를 제거한다 |
| 로그 | `fileVersionId`, `indexStatus`만 남기고 문서 원문, storage key, worker token은 남기지 않는다 |

**Request 예시 — 처리 중**

```json
{
  "indexStatus": "PROCESSING",
  "errorMessage": null
}
```

**Request 예시 — 완료**

```json
{
  "indexStatus": "COMPLETED",
  "errorMessage": null
}
```

**Request 예시 — 실패**

```json
{
  "indexStatus": "FAILED",
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
| `indexStatus` | String | 저장된 인덱싱 상태 |
| `reason` | String | `accepted=false`일 때 무시 사유 |

**Response 예시**

```json
{
  "accepted": true,
  "fileVersionId": 101,
  "indexStatus": "COMPLETED",
  "reason": null
}
```
