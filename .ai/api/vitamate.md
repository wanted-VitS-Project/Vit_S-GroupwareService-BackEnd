# 비타메이트 API 명세

**노션 원본**: 사용자 제공 노션 정리본 (링크 미제공)
**최종 동기화**: 2026-08-03 (CodeRabbit 피드백 반영)
**도메인 담당**: 정현

> 상태가 `✅ 확정` 이상인 항목은 프론트와의 계약이다. 임의 변경 금지.
> 현재 문서는 노션 정리본을 로컬에 옮긴 **초안**이다. 팀 합의와 프론트 공유가 끝나면 상태를 `✅ 확정`으로 바꾼다.

---

## 엔드포인트 목록

| 상태 | 기능 | METHOD | URL | 권한 |
|------|------|--------|-----|------|
| 📝 초안 | 문서 분석 요청 | POST | `/api/v1/blocks/{blockId}/vitamate/analyses` | 스텝 접근 권한 |
| 📝 초안 | AI 분석 상태 및 결과 조회 | GET | `/api/v1/vitamate/analyses/{analysisId}` | 스텝 접근 권한 |
| 📝 초안 | 블록별 분석 실행 이력 조회 | GET | `/api/v1/blocks/{blockId}/vitamate/analyses` | 스텝 접근 권한 |
| 📝 초안 | Python 내부 분석 요청 | POST | `/internal/v1/vitamate/analyses` | 내부 서버 |

---

## 문서 분석 요청 `POST /api/v1/blocks/{blockId}/vitamate/analyses`

**상태**: 📝 초안

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

**상태**: 📝 초안

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

권한이 없으면 `prompt`, `result`, `documents`, `citations`를 반환하지 않는다.

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

| 상태 | `result` | `errorMessage` | `completedAt` | `citations` |
|------|----------|----------------|---------------|-------------|
| `PENDING` | `null` | `null` | `null` | `[]` |
| `PROCESSING` | `null` | `null` | `null` | `[]` |
| `COMPLETED` | 필수 | `null` | 필수 | `[]` 가능 |
| `FAILED` | `null` | 필수 | 필수 | `[]` |

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

**상태**: 📝 초안

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

**상태**: 📝 초안

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
| `prompt` | String | 분석 프롬프트 |
| `searchScope` | Object | 검색 범위 |
| `documents` | Object[] | 선택 문서와 청크 후보 |

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

내부 응답 null 규칙:

| 상태 | `result` | `citations` | `errorMessage` |
|------|----------|-------------|----------------|
| `COMPLETED` | 필수 | `[]` 가능 | `null` |
| `FAILED` | `null` | `[]` | 필수 |

> 이 API는 피그마 화면 댓글에 달지 않고 백엔드 API 문서 또는 시퀀스 다이어그램에만 기록한다.
