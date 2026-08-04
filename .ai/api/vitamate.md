# 비타메이트 API 명세

**노션 원본**: 사용자 제공 노션 정리본 (링크 미제공)
**최종 동기화**: 2026-08-04 (노션 반영 완료 — 비타메이트 API 구현 가능 상태로 전환)
**도메인 담당**: 정현

> 상태가 `✅ 확정` 이상인 항목은 프론트와의 계약이다. 임의 변경 금지.
> 현재 문서는 노션 반영이 끝난 비타메이트 API 계약 사본이다. 변경이 필요하면 노션을 먼저 수정하고 프론트에 공유한다.

---

## 엔드포인트 목록

| 상태 | 기능 | METHOD | URL | 권한 |
|------|------|--------|-----|------|
| ✅ 확정 | 문서 분석 요청 | POST | `/api/v1/blocks/{blockId}/vitamate/analyses` | 스텝 접근 권한 |
| ✅ 확정 | AI 분석 상태 및 결과 조회 | GET | `/api/v1/vitamate/analyses/{analysisId}` | 스텝 접근 권한 |
| ✅ 확정 | 블록별 분석 실행 이력 조회 | GET | `/api/v1/blocks/{blockId}/vitamate/analyses` | 스텝 접근 권한 |
| ✅ 확정 | Python 내부 분석 요청 | POST | `/internal/v1/vitamate/analyses` | 내부 서버 |

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

## Python 내부 분석 요청 `POST /internal/v1/vitamate/analyses`

**상태**: ✅ 확정

프론트에서 호출하지 않는 서버 간 내부 API다.

Spring Boot가 Python 서버에 전달한다.

서비스 인증:

| 항목 | 규칙 |
|------|------|
| 호출자 | Spring Boot 서버만 호출 |
| 인증 방식 | 내부 서비스 토큰 또는 mTLS 중 팀 합의 후 선택 |
| 네트워크 | 퍼블릭 인터넷 직접 노출 금지. 같은 VPC/보안 그룹 또는 내부 네트워크로 제한 |
| 실패 응답 | 인증 실패 401, 권한 없는 호출 403 |

**Request**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `analysisId` | Long | Spring Boot에서 생성한 분석 ID |
| `attemptId` | String | 현재 워커 실행 토큰. 늦은 응답 저장 방지용 UUID |
| `prompt` | String | 분석 프롬프트 |
| `searchScope` | Object | 검색 범위 |
| `documents` | Object[] | 선택 문서와 청크 후보 |

내부 요청 일관성 규칙:

| 항목 | 규칙 |
|------|------|
| 기준 집합 | `searchScope.fileVersionIds`가 분석 요청에서 선택되어 `vitamate_analysis_document`에 저장된 전체 파일 버전 집합이다 |
| `documents` 범위 | `documents[].fileVersionId` 집합은 `searchScope.fileVersionIds`와 정확히 같아야 한다. 누락/추가가 있으면 내부 요청을 만들지 않는다 |
| 청크 소속 | 각 `chunks[]`는 부모 `documents[].fileVersionId`에 속한 `document_chunk`만 포함한다 |
| 빈 청크 | 선택 문서가 검색 가능한 청크를 아직 갖지 못한 경우 `chunks: []`는 허용한다. 단, 문서 항목 자체는 누락하지 않는다 |
| 분석 소속 | `analysisId → vitamate_analysis → vitamate_block → block` 경로가 `searchScope.blockId`, `searchScope.projectId`와 일치해야 한다 |
| 신뢰 경계 | Spring Boot가 DB 검증 후 내부 요청을 구성한다. 프론트 입력값을 그대로 Python에 전달하지 않는다 |
| citation 범위 | Python 응답의 citation도 `searchScope.fileVersionIds`와 전달된 청크 범위 안으로 제한한다. Spring Boot가 저장 전 다시 검증한다 |

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

Python 서버가 반환한다.

| 파라미터 | 타입 | 설명 |
|---------|------|------|
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

내부 응답 검증:

| 항목 | 규칙 |
|------|------|
| 응답 상태 | `COMPLETED` 또는 `FAILED`만 허용한다 |
| citation 파일 | `citations[].fileVersionId`는 요청의 `searchScope.fileVersionIds` 안에 있어야 한다 |
| citation 청크 | `citations[].documentChunkId`는 해당 `fileVersionId`의 `document_chunk`여야 한다 |
| 저장 방식 | Spring Boot는 현재 `attemptId`와 `PROCESSING` 상태가 일치할 때만 결과와 citation을 저장한다 |
| 범위 위반 | 범위 밖 citation이 있으면 결과를 부분 저장하지 않고 해당 분석을 `FAILED`로 마감한다 |

내부 응답 null 규칙:

| 상태 | `result` | `citations` | `errorMessage` |
|------|----------|-------------|----------------|
| `COMPLETED` | 필수 | `[]` 가능 | `null` |
| `FAILED` | `null` | `[]` | 필수 |

> 이 API는 피그마 화면 댓글에 달지 않고 백엔드 API 문서 또는 시퀀스 다이어그램에만 기록한다.
