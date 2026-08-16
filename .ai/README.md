# 📚 문서 인덱스

**최종 업데이트**: 2026-08-16 (실제 추적 파일 기준으로 지도 전면 재작성 — `docs/` · `runbook/` 등재, 유령 경로 제거)

그룹웨어 서비스 (3조) 백엔드 — AI 협업 & 운영 문서 모음.

> 이 폴더의 목적은 **AI에게 프로젝트의 기억을 파일로 만들어 주는 것**이다.
> AI는 세션마다 기억을 잃는다. 여기에 규칙과 상태를 남겨두면 매번 처음부터 설명하지 않아도 된다.

> 📍 **이 파일이 문서 지도의 정본이다.** 문서를 추가·이동·삭제하면 여기부터 고친다.
> `AGENTS.md` §1 은 진입점 몇 개만 들고 나머지는 이 파일을 가리킨다.

---

## 🗺️ 전체 구조

```
루트/
├── AGENTS.md          ⭐ 메인 규칙 (AI가 가장 먼저 읽는 파일)
├── CLAUDE.md             Claude Code 진입점 (AGENTS.md 포인터)
└── .ai/
    ├── README.md         이 파일 — 문서 지도 (정본)
    ├── API.md            API 규칙 (명세 이탈 방지 · 프론트와의 계약)
    ├── ARCHITECTURE.md   아키텍처 컨벤션 (헥사고날 계층·네이밍)
    ├── CONVENTION.md     브랜치·커밋·PR·이슈 규칙
    ├── INFRA.md          인프라 인벤토리 (실제 값은 마스킹)
    ├── PIPELINE.md       CI/CD 워크플로우 구조
    ├── api/              API 명세 사본 — README.md + {도메인}.md ×21
    ├── docs/             도메인·공통 지식
    │   ├── README.md         도메인 문서 인덱스
    │   ├── global/           전 도메인 공통 규칙 ×11
    │   ├── DEMO-SCENARIO.md · DEMO-SCENARIO-KDT.md · DEMO-DUMP-PLAYBOOK.md
    │   ├── PRESENTATION.md   발표용 정리
    │   └── domain/       🔒 도메인별 구현 계약 (gitignore — 개인 로컬 전용)
    ├── runbook/          운영 런북 — ADMIN-ONBOARDING.md
    ├── template/         PR·Issue·커밋·STATE·WORKLOG 양식 ×5
    └── local/         🔒 개인 문서 (gitignore — 각자 로컬에만 존재)
```

> ⚠️ `.ai/docs/domain/` 은 위치는 `docs/` 안이지만 **커밋되지 않는다**(gitignore).
> 클론 직후엔 존재하지 않는다 — `.ai/local/` 과 동급으로 다뤄라. 아래 [🔒 개인 로컬 문서](#-개인-로컬-문서) 참고.

## 🔌 도구별 자동 인식

AI 도구마다 가장 먼저 읽는 파일 이름이 달라서 두 파일을 둔다.

| 도구 | 자동으로 읽는 파일 |
|------|-------------------|
| **Codex** | `AGENTS.md` (바로 읽음) |
| **Claude Code** | `CLAUDE.md` → `@AGENTS.md` 로 연결 |

규칙 본문은 **`AGENTS.md` 하나에만** 있다. 규칙을 고칠 땐 항상 `AGENTS.md` 만 수정한다.

---

## 📦 팀 공유 문서 (git 추적)

### 규칙 · 운영 (`.ai/` 루트)

| 문서 | 역할 | 언제 읽나 |
|------|------|----------|
| [API.md](API.md) | **명세 이탈 방지 규칙** + 공통 컨벤션 · Swagger 규칙 | API 코드 작성 전 (필수) |
| [ARCHITECTURE.md](ARCHITECTURE.md) | **아키텍처 컨벤션** — 헥사고날 계층 구조·네이밍 규칙 | 새 도메인·패키지 만들 때 (필수) |
| [CONVENTION.md](CONVENTION.md) | 브랜치 전략·커밋·PR·이슈 규칙 | 브랜치 만들 때, 커밋·PR 쓸 때 |
| [INFRA.md](INFRA.md) | 서버·리소스 구조, 포트, 시크릿 **키 이름** 목록 | 배포·인프라 관련 작업 전 |
| [PIPELINE.md](PIPELINE.md) | CI/CD 워크플로우 구조, 배포 흐름 | `.github/workflows/` 수정 전 |

### 도메인 공통 지식 (`.ai/docs/global/`)

**어느 도메인을 하든 걸린다.** 도메인 문서 인덱스는 [docs/README.md](docs/README.md).

| 문서 | 역할 | 언제 읽나 |
|------|------|----------|
| [PRODUCT.md](docs/global/PRODUCT.md) | 제품 정의 — 무엇을 왜 만드나 · **범위 밖** | 기능 범위를 다툴 때 |
| [USECASE.md](docs/global/USECASE.md) | 공고 → 실적 전 과정 흐름 | 기능 구현 전 흐름 확인 |
| [PAGE.md](docs/global/PAGE.md) | 페이지 체계 — 탭·화면 P-00~P-70 역할·담당 | 화면·라우팅 설계 |
| [PERMISSION.md](docs/global/PERMISSION.md) | 권한 체계 — 3층 판정 · role 차이표 ⚠️ **`MASTER` 는 최상위가 아니다** | 권한 검사 코드·관리자 화면 |
| [BLOCK.md](docs/global/BLOCK.md) | 블록 — 공통 규칙 · 배치 · 카탈로그 · 삭제 전파/이동 | 블록이 걸린 **모든** 작업 |
| [DELETE.md](docs/global/DELETE.md) | 삭제 정책 — soft/hard 판정 기준 · 예외 목록 | 삭제 동작을 만들거나 고칠 때 |
| [CLEANUP.md](docs/global/CLEANUP.md) | 삭제 데이터 정리 — 명시적 영구 삭제 vs `CASCADE` 판단 | soft delete 테이블 신설 · 종속 데이터 정리 설계 |
| [CONCURRENCY.md](docs/global/CONCURRENCY.md) | 동시수정 정합성 — **낙관적 락 단일 정책** | 동시 편집이 걸리는 수정 API |
| [PERSISTENCE.md](docs/global/PERSISTENCE.md) | JPA(쓰기) ↔ MyBatis(조회) 역할 분리 | 저장/조회 코드 위치 판단 |
| [MYBATIS.md](docs/global/MYBATIS.md) | MyBatis SQL 위치·작성 방식 (XML 분리) | Mapper·XML 작성/수정 (필수) |
| [FLYWAY.md](docs/global/FLYWAY.md) | DB 마이그레이션 컨벤션 · RDS 반영 규칙 | 테이블·컬럼·인덱스·FK·seed 변경 전 (필수) |

### 시연 · 발표 (`.ai/docs/`)

| 문서 | 역할 |
|------|------|
| [DEMO-SCENARIO.md](docs/DEMO-SCENARIO.md) | 더미 시나리오 — 비타웨어 무신사 입점 (company 2) |
| [DEMO-SCENARIO-KDT.md](docs/DEMO-SCENARIO-KDT.md) | 더미 시나리오 — 비타에듀 K-디지털 심사 신청 (company 3) |
| [DEMO-DUMP-PLAYBOOK.md](docs/DEMO-DUMP-PLAYBOOK.md) | 시연용 더미 데이터를 다시 만들 때의 제작 플레이북 |
| [PRESENTATION.md](docs/PRESENTATION.md) | 발표 스크립트 원본 (구현 규칙 정본은 아니다) |

### API 명세 사본 (`.ai/api/`)

**프론트와의 계약이다.** 새 API 는 여기서 설계하고, 이 md 가 곧 계약이 된다.

| 문서 | 역할 |
|------|------|
| [api/README.md](api/README.md) | 도메인별 명세 인덱스 · 작성 규칙 |
| `api/{도메인}.md` ×21 | account · activity-log · approval · auth · bid · checklist · company-document · department · employee · employee-group · file · finance · image · issue · job-position · notification · page-permission · qualification · settlement · text · vitamate |

### 런북 (`.ai/runbook/`)

| 문서 | 역할 |
|------|------|
| [runbook/ADMIN-ONBOARDING.md](runbook/ADMIN-ONBOARDING.md) | 신규 회사(테넌트) + ADMIN 온보딩 절차 |

### 양식 (`.ai/template/`)

| 양식 | 파일 |
|------|------|
| PR 본문 | [template/PR.md](template/PR.md) |
| Issue 본문 | [template/ISSUE.md](template/ISSUE.md) |
| 커밋 메시지 | [template/COMMIT.md](template/COMMIT.md) |
| STATE 초기 템플릿 | [template/STATE.template.md](template/STATE.template.md) |
| WORKLOG 초기 템플릿 | [template/WORKLOG.template.md](template/WORKLOG.template.md) |

---

## 🔒 개인 로컬 문서

**git에 올라가지 않는다.** 클론 직후엔 존재하지 않으므로 각자 만들어 쓴다.

### `.ai/local/` — 내 상태 기록

| 문서 | 역할 |
|------|------|
| `local/STATE.md` | 내 현재 진행 상황·로드맵·백로그 |
| `local/WORKLOG.md` | 내 완료 작업 기록·트러블슈팅 |
| `local/INFRA-real.md` | 실제 IP·엔드포인트 등 민감 값 |
| `local/archive/` | 마일스톤 종료 시 STATE·WORKLOG 이관 |

### `.ai/docs/domain/` — 도메인별 구현 계약 (로컬 정본)

**도메인 정본이지만 커밋되지 않는다.** 담당자가 각자 로컬에 보유하며, 팀 공유는 파일이 아니라 PR·이슈로 한다.

| 종류 | 무엇 |
|------|------|
| `{도메인}-V1.md` | 요구사항 + **수용 기준** + 불변식 — 이게 구현 계약이다 |
| `{도메인}-V1-USECASE.md` | 테이블 단위 시나리오 목록 — 빠짐없이 세는 용도 |

> ⚠️ 문서가 로컬에 없으면 **그 도메인 작업을 추측으로 시작하지 마라.** 담당자에게 문서를 요청하라.

### 왜 개인 문서로 두나

1. **충돌 회피** — 매 세션 수정되는 파일을 여러 명이 공유하면 머지 충돌이 계속 난다.
2. **팀엔 이미 보인다** — 팀이 알아야 할 진행 상황은 GitHub 이슈/PR에 있다. WORKLOG는 그 과정 기록이다.
3. **보안** — PUBLIC 레포라 실제 인프라 값은 올릴 수 없다.

### 처음 세팅

```bash
mkdir -p .ai/local/archive
cp .ai/template/STATE.template.md .ai/local/STATE.md
cp .ai/template/WORKLOG.template.md .ai/local/WORKLOG.md
```

### 팀 문서로 승격

개인 WORKLOG에 쌓인 것 중 팀이 알아야 할 내용은 공유 문서로 옮긴다.
민감 정보(IP·계정ID·시크릿 값)는 **제거하고** 옮긴다.

| 개인 기록 | 승격 대상 |
|---|---|
| 인프라 구성 변경 | [INFRA.md](INFRA.md) |
| 워크플로우 변경 | [PIPELINE.md](PIPELINE.md) |
| 컨벤션 결정 | [CONVENTION.md](CONVENTION.md) |
| 운영 절차 | `runbook/` (이미 있다 — 문서 추가) |
| 장애 기록 | `incident/` (아직 없다 — 첫 건에서 신설) |

---

## ⚠️ 보안 — 이 레포는 PUBLIC

커밋되는 문서에 아래를 **절대 쓰지 않는다.**

- IP, RDS/Redis 엔드포인트, 도메인
- AWS 계정 ID, ARN, 액세스 키, 키페어 이름
- 비밀번호·API 키·토큰 등 시크릿 **값**

시크릿은 **키 이름만** 적고, 실제 값은 GitHub Repo Secrets 또는 `local/INFRA-real.md` 에 둔다.

---

## 💡 자주 쓰는 문구

| 하고 싶은 것 | AI에게 말하기 |
|--------------|--------------|
| 지난 작업 이어서 | "이어서 하자" / "전에 어디까지 했지?" |
| 작업 완료 기록 | "완료" / "WORKLOG에 기록해줘" |
| 백로그 등록 | "이건 백로그에 넣어줘" |
| 인프라 작업 | "OO 배포 설정해줘" (AI가 INFRA.md 먼저 읽음) |
| 도메인 규칙 확인 | "블록 규칙 보고 와" (AI가 docs/global 먼저 읽음) |
