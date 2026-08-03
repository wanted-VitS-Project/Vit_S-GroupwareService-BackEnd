# 🤖 비타메이트 문서 분석 AI v1 — 요구사항 명세

**최종 업데이트**: 2026-08-03 (CodeRabbit 피드백 반영 — 상태 머신·권한·중복 방지 보강)
**담당**: 정현
**근거**: [`BLOCK.md`](../../global/BLOCK.md) §4-9 · 사용자 제공 노션 정리본(2026-08-03)

> 이 문서는 **비타메이트 AI 블록**의 구현 계약이다.
> ⛔ API 형태는 [`.ai/api/vitamate.md`](../../../api/vitamate.md) 소관이다. 명세가 `📝 초안` 이면 구현 금지다.

---

## 0. 한 줄 정의

**사용자가 프로젝트 문서 버전들을 직접 선택하고 프롬프트를 입력하면, Python AI 서버가 관련 문서 청크를 근거로 분석 결과를 생성해 이력으로 저장하는 AI 블록이다.**

ChatGPT처럼 대화를 계속 이어가는 채팅형 기능이 아니다.

```text
문서 선택
→ 프롬프트 입력
→ 분석 요청
→ 결과 저장
→ 필요하면 조건을 바꿔 새 분석 실행
```

분석을 다시 실행할 때 기존 결과를 수정하지 않고 **새 분석 이력**을 만든다.

---

## 1. 범위

### 1-1. 만든다

| 영역 | 내용 |
|------|------|
| AI 블록 | `block.type='AI'` 의 상세 테이블 `vitamate_block` |
| 분석 실행 | 선택 문서 버전 + 프롬프트 기반 분석 요청 |
| 이력 | 분석 실행마다 `vitamate_analysis` 1행 생성 |
| 결과 조회 | 상태, 결과, 실패 메시지, 선택 문서, 근거 청크 조회 |
| RAG 메타데이터 | 문서 청크 메타데이터와 ChromaDB 식별자 저장 |
| Python 연동 | Spring Boot → Python FastAPI 내부 API 호출 |

### 1-2. 안 한다

| 항목 | 이유 |
|------|------|
| 채팅형 대화 | 현재 기능은 분석 실행형이다. 별도 message 테이블을 두지 않는다 |
| 기존 분석 결과 덮어쓰기 | 재실행은 새 이력이다. 과거 결과를 보존한다 |
| 벡터를 MySQL에 저장 | 실제 임베딩 벡터는 ChromaDB에 저장하고 MySQL에는 `chroma_id`만 둔다 |
| 내부 Python API를 프론트/피그마에 노출 | 서버 간 내부 API다 |

---

## 2. 처리 흐름

```text
프론트엔드
→ Spring Boot에 분석 요청
→ Spring Boot가 분석 상태 PENDING 저장
→ Spring Boot가 백그라운드 워커에 작업 등록
→ 워커가 분석 상태 PROCESSING 변경
→ 워커가 Python FastAPI 호출
→ Python이 문서 청크 검색
→ 관련 청크 기반 AI 분석
→ Python이 분석 결과와 근거 반환
→ 워커가 결과 저장
→ 프론트가 상태 및 결과 조회
```

| 구간 | 방식 |
|------|------|
| 프론트엔드 ↔ Spring Boot | 비동기. 요청은 `202 Accepted` + `analysisId`, 결과는 조회 API |
| Spring Boot 내부 실행 | v1은 Spring Boot 백그라운드 워커(TaskExecutor 또는 동등한 방식)가 처리. 요청 스레드와 Python 호출을 분리 |
| Spring Boot ↔ Python FastAPI | 워커가 내부 동기 호출. 결과를 받아 DB에 저장 |

실패 처리:

| 상황 | 처리 |
|------|------|
| 워커 등록 실패 | 분석 상태 `FAILED`, `error_message` 저장 |
| Python 호출 타임아웃 | 분석 상태 `FAILED`, `error_message` 저장 |
| Python 응답 실패 | 분석 상태 `FAILED`, `error_message` 저장 |
| 서버 재시작으로 `PENDING` 잔류 | 재처리 대상. 일정 시간 이상 `PENDING`이면 워커가 다시 집는다 |

---

## 3. 요구사항

### 3-A. 분석 요청 (`VIT`)

| ID | 요구사항 | 수용 기준 |
|----|----------|----------|
| **VIT-001** | 사용자는 AI 블록에서 문서 분석을 요청할 수 있다 | 요청 시 `vitamate_analysis`가 생성되고 초기 상태가 `PENDING`이다 |
| **VIT-002** | 사용자는 분석 대상 문서 버전을 여러 개 선택할 수 있다 | 선택한 `file_version_id`가 `vitamate_analysis_document`에 저장된다 |
| **VIT-003** | 사용자는 자연어 프롬프트를 입력한다 | 프롬프트가 `vitamate_analysis.prompt`에 저장된다 |
| **VIT-004** | 요청 API는 즉시 `analysisId`를 반환한다 | HTTP `202 Accepted`와 `analysisStatus=PENDING`을 반환한다 |
| **VIT-005** | 재분석은 새 이력을 만든다 | 같은 블록에서 다시 실행해도 기존 `vitamate_analysis` 행을 수정하지 않는다 |
| **VIT-006** | 같은 사용자 동작의 재시도는 중복 분석을 만들지 않는다 | `Idempotency-Key` 기준으로 같은 요청은 같은 `analysisId`를 반환한다 |

### 3-B. 분석 처리 (`AIP`)

| ID | 요구사항 | 수용 기준 |
|----|----------|----------|
| **AIP-001** | Spring Boot는 Python FastAPI 내부 API를 호출한다 | 프론트가 Python 서버를 직접 호출하는 경로가 없다 |
| **AIP-002** | 워커는 `PENDING` 분석을 집어 `PROCESSING`으로 바꾼 뒤 Python을 호출한다 | 요청 스레드에서 Python 호출을 기다리지 않는다 |
| **AIP-003** | Python은 선택 문서의 청크를 검색한다 | 검색 대상은 선택한 `fileVersionIds` 범위로 제한된다 |
| **AIP-004** | Python은 분석 결과와 근거 청크를 반환한다 | 결과 본문과 citation 목록이 Spring Boot에 전달된다 |
| **AIP-005** | 처리 성공 시 상태를 `COMPLETED`로 바꾼다 | `result`, `completed_at`, citation이 저장된다 |
| **AIP-006** | 처리 실패 시 상태를 `FAILED`로 바꾼다 | `error_message`, `completed_at`이 저장되고 기존 이력은 삭제하지 않는다 |
| **AIP-007** | Python 호출에는 타임아웃을 둔다 | 타임아웃 시 `FAILED`로 종료하고 무한 대기하지 않는다 |

### 3-C. 결과 조회 (`QRY`)

| ID | 요구사항 | 수용 기준 |
|----|----------|----------|
| **QRY-001** | 사용자는 분석 상태와 결과를 조회할 수 있다 | `PENDING/PROCESSING/COMPLETED/FAILED` 중 하나를 반환한다 |
| **QRY-002** | 결과 조회에는 선택 문서 정보가 포함된다 | `fileVersionId`, `fileName`이 내려간다 |
| **QRY-003** | 결과 조회에는 근거 청크 정보가 포함된다 | `rankOrder`, `documentChunkId`, `pageNumber`, `excerpt`가 내려간다 |
| **QRY-004** | 사용자는 블록별 분석 실행 이력을 볼 수 있다 | 해당 `blockId`에 연결된 분석 이력을 최신순으로 조회한다 |

---

## 4. 상태값

| 상태 | 의미 |
|------|------|
| `PENDING` | 분석 요청 저장 완료, 워커 처리 전 |
| `PROCESSING` | Python 서버 분석 처리 중 |
| `COMPLETED` | 분석 완료 |
| `FAILED` | 분석 실패 |

상태 전이는 아래만 허용한다.

```text
PENDING → PROCESSING → COMPLETED
PENDING → PROCESSING → FAILED
PENDING → FAILED
```

`PROCESSING`에서 장시간 멈춘 작업은 운영 정책에 따라 `FAILED`로 마감하고 재요청하도록 한다.

---

## 5. ERD

```text
block
  1
  │
  1
vitamate_block
  1
  │
  N
vitamate_analysis
  ├── N vitamate_analysis_document
  └── N vitamate_analysis_citation

file_version
  ├── N vitamate_analysis_document
  └── N document_chunk

document_chunk
  1
  │
  N
vitamate_analysis_citation
```

### 5-1. `vitamate_block`

| 컬럼 | 타입 | 규칙 |
|------|------|------|
| `vitamate_block_id` | BIGINT | PK |
| `block_id` | BIGINT | FK, UNIQUE |
| `welcome_message` | VARCHAR(500) | 선택 |
| `created_at` | DATETIME | |
| `updated_at` | DATETIME | |

### 5-2. `vitamate_analysis`

분석 실행 한 번당 한 행이 저장된다.

| 컬럼 | 타입 | 규칙 |
|------|------|------|
| `vitamate_analysis_id` | BIGINT | PK |
| `vitamate_block_id` | BIGINT | FK |
| `requested_by` | VARCHAR(20) | FK → `employee.user_id` |
| `idempotency_key` | VARCHAR(100) | 같은 사용자 동작의 재시도 중복 방지 |
| `request_hash` | CHAR(64) | 같은 idempotency key에 다른 본문이 들어오는지 검증 |
| `prompt` | TEXT | |
| `result` | LONGTEXT | |
| `analysis_status` | VARCHAR(20) | `PENDING/PROCESSING/COMPLETED/FAILED` |
| `error_message` | TEXT | |
| `completed_at` | DATETIME | |
| `created_at` | DATETIME | |
| `updated_at` | DATETIME | |
| `deleted_at` | DATETIME | soft delete |

중복 방지:

```sql
UNIQUE (vitamate_block_id, requested_by, idempotency_key)
```

### 5-3. `vitamate_analysis_document`

분석 실행에 사용된 문서 버전을 저장한다.

| 컬럼 | 타입 | 규칙 |
|------|------|------|
| `vitamate_analysis_document_id` | BIGINT | PK |
| `vitamate_analysis_id` | BIGINT | FK |
| `file_version_id` | BIGINT | FK |
| `created_at` | DATETIME | |

중복 방지:

```sql
UNIQUE (vitamate_analysis_id, file_version_id)
```

### 5-4. `document_chunk`

RAG 검색용 문서 청크 메타데이터를 저장한다.

| 컬럼 | 타입 | 규칙 |
|------|------|------|
| `document_chunk_id` | BIGINT | PK |
| `file_version_id` | BIGINT | FK |
| `chunk_index` | INT | |
| `page_number` | INT | |
| `section_title` | VARCHAR(255) | |
| `start_offset` | INT | |
| `end_offset` | INT | |
| `token_count` | INT | |
| `chroma_id` | VARCHAR(150) | ChromaDB 식별자 |
| `embedding_model` | VARCHAR(100) | |
| `embedding_status` | VARCHAR(20) | `PENDING/PROCESSING/COMPLETED/FAILED` |
| `created_at` | DATETIME | |
| `updated_at` | DATETIME | |

### 5-5. `vitamate_analysis_citation`

AI 결과의 근거가 된 문서 청크를 저장한다.

| 컬럼 | 타입 | 규칙 |
|------|------|------|
| `vitamate_analysis_citation_id` | BIGINT | PK |
| `vitamate_analysis_id` | BIGINT | FK |
| `vitamate_analysis_document_id` | BIGINT | FK → 선택된 분석 문서 |
| `document_chunk_id` | BIGINT | FK |
| `rank_order` | INT | |
| `distance_score` | DECIMAL(10,6) | |
| `excerpt` | TEXT | |
| `created_at` | DATETIME | |

중복 방지:

```sql
UNIQUE (vitamate_analysis_id, rank_order)
```

저장 시 검증:

```text
vitamate_analysis_citation.vitamate_analysis_document_id
→ vitamate_analysis_document.file_version_id
→ document_chunk.file_version_id 와 일치해야 한다.
```

즉, 분석 요청에서 선택하지 않은 파일 버전의 청크는 citation으로 저장할 수 없다.

---

## 6. 불변식

| ID | 규칙 | 왜 |
|----|------|-----|
| **INV-01** | 비타메이트는 채팅형이 아니라 분석 실행형이다 | message 테이블 없이 `vitamate_analysis`가 이력 역할을 한다 |
| **INV-02** | 재실행은 기존 결과 수정이 아니라 새 분석 생성이다 | 과거 분석 조건과 결과가 감사 가능한 이력으로 남아야 한다 |
| **INV-03** | 프론트는 Python 내부 API를 호출하지 않는다 | Python API는 서버 간 내부 계약이다 |
| **INV-04** | 실제 벡터는 MySQL에 저장하지 않는다 | ChromaDB가 벡터 저장소이고 MySQL은 메타데이터만 가진다 |
| **INV-05** | 공고 AI 요약과 비타메이트 AI는 별개다 | 공고 요약은 입찰 도메인, 비타메이트는 프로젝트 스텝 안의 AI 블록이다 |
| **INV-06** | citation은 선택 문서 범위 안의 청크만 가리킨다 | 선택하지 않은 프로젝트 파일이나 다른 프로젝트 파일이 분석 근거로 섞이면 안 된다 |
| **INV-07** | `analysisId` 조회는 스텝 권한 검증을 통과해야 한다 | 분석 결과와 프롬프트는 프로젝트 내부 정보다 |

---

## 7. 시작 전 확인

| # | 확인 | 영향 |
|---|------|------|
| 1 | `employee.user_id`와 파일 도메인의 `file_version` 스키마 확정 | FK 타입과 테이블명 확정 필요 |
| 2 | Python FastAPI 내부 요청/응답 DTO 확정 | Spring ↔ Python 계약 필요 |
| 3 | 문서 청크 생성 주체 확정 | 파일 업로드 시점인지 분석 요청 시점인지 결정 필요 |
| 4 | ChromaDB 컬렉션/네임스페이스 규칙 확정 | `chroma_id` 관리 방식 결정 필요 |
| 5 | API 명세 상태 | `.ai/api/vitamate.md`가 `📝 초안`이면 컨트롤러 구현 금지 |
| 6 | 백그라운드 워커 구현 방식 | v1은 Spring TaskExecutor, 추후 큐 도입 여부 결정 |
| 7 | 내부 서비스 인증 방식 | 서비스 토큰 또는 mTLS 중 팀 합의 필요 |
