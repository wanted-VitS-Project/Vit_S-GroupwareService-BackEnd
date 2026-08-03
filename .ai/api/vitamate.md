# 비타메이트 API 명세

**노션 원본**: 사용자 제공 노션 정리본 (링크 미제공)
**최종 동기화**: 2026-08-03
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
| `analysisStatus` | String | 초기 상태. `PROCESSING` |
| `requestedAt` | LocalDateTime | 요청 시각 |

---

## AI 분석 상태 및 결과 조회 `GET /api/v1/vitamate/analyses/{analysisId}`

**상태**: 📝 초안

분석 요청 정보, 처리 상태, 생성 결과, 실패 메시지, 선택 문서와 근거를 조회한다.

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
| `completedAt` | LocalDateTime | 완료 시각 |
| `documents` | Object[] | 분석 대상 문서 목록 |
| `citations` | Object[] | 분석 근거 목록 |

**documents**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `fileVersionId` | Long | 파일 버전 ID |
| `fileName` | String | 파일명 |

**citations**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `rankOrder` | Integer | 근거 순서 |
| `documentChunkId` | Long | 문서 청크 ID |
| `pageNumber` | Integer | 페이지 번호 |
| `excerpt` | String | 근거 발췌문 |

---

## 블록별 분석 실행 이력 조회 `GET /api/v1/blocks/{blockId}/vitamate/analyses`

**상태**: 📝 초안

해당 비타메이트 블록에서 수행한 분석 실행 이력을 조회한다.

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

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `analysisId` | Long | Spring Boot에서 생성한 분석 ID |
| `documents` | Object[] | 선택 문서의 청크 정보 |
| `prompt` | String | 분석 프롬프트 |
| `searchScope` | Object | 검색 범위 |

Python 서버가 반환한다.

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `analysisStatus` | String | 처리 결과 상태 |
| `result` | String | 분석 결과 |
| `citations` | Object[] | 검색 근거 청크 |
| `errorMessage` | String | 오류 메시지 |

> 이 API는 피그마 화면 댓글에 달지 않고 백엔드 API 문서 또는 시퀀스 다이어그램에만 기록한다.
