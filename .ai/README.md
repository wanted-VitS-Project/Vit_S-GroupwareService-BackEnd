# 📚 문서 인덱스

그룹웨어 서비스 (3조) 백엔드 — AI 협업 & 운영 문서 모음.

> 이 폴더의 목적은 **AI에게 프로젝트의 기억을 파일로 만들어 주는 것**이다.
> AI는 세션마다 기억을 잃는다. 여기에 규칙과 상태를 남겨두면 매번 처음부터 설명하지 않아도 된다.

---

## 🗺️ 전체 구조

```
루트/
├── AGENTS.md          ⭐ 메인 규칙 (AI가 가장 먼저 읽는 파일)
├── CLAUDE.md             Claude Code 진입점 (AGENTS.md 포인터)
└── .ai/
    ├── README.md         이 파일 — 문서 인덱스
    ├── PRODUCT.md        제품 정의 (문제·방향성·핵심 기능)
    ├── DOMAIN.md         도메인 모델 (스테이지·스텝·블록·이슈 정의)
    ├── USECASE.md        유스케이스 (공고→실적 전 과정 흐름)
    ├── ERD.md            테이블 지도 (실제 DDL 은 db/migration)
    ├── API.md            API 규칙 (프론트와의 계약 보호)
    ├── api/              노션 명세 도메인별 사본
    ├── CONVENTION.md     브랜치·커밋·PR 규칙
    ├── INFRA.md          인프라 인벤토리
    ├── PIPELINE.md       CI/CD 구조
    ├── template/         PR·Issue·커밋·문서 양식
    └── local/         🔒 개인 문서 (gitignore — 각자 로컬에만 존재)
```

## 🔌 도구별 자동 인식

AI 도구마다 가장 먼저 읽는 파일 이름이 달라서 두 파일을 둔다.

| 도구 | 자동으로 읽는 파일 |
|------|-------------------|
| **Codex** | `AGENTS.md` (바로 읽음) |
| **Claude Code** | `CLAUDE.md` → `@AGENTS.md` 로 연결 |

규칙 본문은 **`AGENTS.md` 하나에만** 있다. 규칙을 고칠 땐 항상 `AGENTS.md` 만 수정한다.

---

## 📦 팀 공유 문서

| 문서 | 역할 | 언제 읽나 |
|------|------|----------|
| [PRODUCT.md](PRODUCT.md) | **제품 정의** — 무엇을 왜 만드나, 무엇을 안 만드나 | 기능 범위를 다툴 때 |
| [DOMAIN.md](DOMAIN.md) | **도메인 모델** — 스테이지·스텝·블록·이슈가 각각 무엇인가 | 엔티티·테이블·API 설계 전 (필수) |
| [USECASE.md](USECASE.md) | **유스케이스** — 공고에서 실적까지 전 과정 흐름 | 기능 구현 전, 흐름 확인용 |
| [ERD.md](ERD.md) | **테이블 지도** — 영역별 ERD (원본은 `db/migration`) | 엔티티·쿼리 작성 전 |
| [API.md](API.md) | **명세 이탈 방지 규칙** + 공통 컨벤션 | API 코드 작성 전 (필수) |
| [api/README.md](api/README.md) | 노션 명세의 도메인별 작업용 사본 | API 코드 작성 전 (필수) |
| [CONVENTION.md](CONVENTION.md) | 브랜치 전략·커밋·PR·이슈 규칙 | 브랜치 만들 때, 커밋·PR 쓸 때 |
| [INFRA.md](INFRA.md) | 서버·리소스 구조, 포트, 시크릿 키 목록 | 배포·인프라 관련 작업 전 |
| [PIPELINE.md](PIPELINE.md) | CI/CD 워크플로우 구조, 배포 흐름 | `.github/workflows/` 수정 전 |

### 양식

| 양식 | 파일 |
|------|------|
| PR 본문 | [template/PR.md](template/PR.md) |
| Issue 본문 | [template/ISSUE.md](template/ISSUE.md) |
| 커밋 메시지 | [template/COMMIT.md](template/COMMIT.md) |
| STATE 초기 템플릿 | [template/STATE.template.md](template/STATE.template.md) |
| WORKLOG 초기 템플릿 | [template/WORKLOG.template.md](template/WORKLOG.template.md) |

---

## 🔒 개인 문서 (`.ai/local/`)

**git에 올라가지 않는다.** 클론 직후엔 존재하지 않으므로 각자 만들어 쓴다.

| 문서 | 역할 |
|------|------|
| `local/STATE.md` | 내 현재 진행 상황·로드맵·백로그 |
| `local/WORKLOG.md` | 내 완료 작업 기록·트러블슈팅 |
| `local/INFRA-real.md` | 실제 IP·엔드포인트 등 민감 값 |
| `local/archive/` | 마일스톤 종료 시 STATE·WORKLOG 이관 |

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
| 인프라 구성 변경 | `INFRA.md` |
| 워크플로우 변경 | `PIPELINE.md` |
| 컨벤션 결정 | `CONVENTION.md` |
| 장애·운영 절차 | `incident/` · `runbook/` 폴더 신설 후 이관 |

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
