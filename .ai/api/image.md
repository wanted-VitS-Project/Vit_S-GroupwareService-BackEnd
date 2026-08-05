# 이미지 블록 API 명세

**노션 원본**: 반영됨 (사용자 확인, 프론트 확인 여부는 미확인) — 전달받은 명세를 그대로 옮김
**최종 동기화**: 2026-08-04
**도메인 담당**: 서정림

> 상태가 `✅ 확정` 이상인 항목은 프론트와의 계약이다. 임의 변경 금지.
> ⚠️ 에러 코드는 체크리스트·텍스트와 동일한 이유로 **실제로 던지는 것만** 순번대로 만든다 (§공통 참고).

---

## 개요

블록 생성은 공용 블록 담당자(동훈님)가 처리한다. 블록 삭제는 텍스트·체크리스트와 동일하게 이벤트로 처리될 예정이며,
이 문서는 **이미지 항목 생성·수정·삭제** 3건을 다룬다.

| 상태 | 기능 | METHOD | URL | 권한 |
|------|------|--------|-----|------|
| ✅ 확정 | 이미지 항목 생성 | POST | `/api/v1/blocks/images/{imgBlockId}/items` | 편집 권한 보유자 |
| ✅ 확정 | 이미지 항목 수정 | PATCH | `/api/v1/blocks/images/items/{imgBlockId}` | 편집 권한 보유자 |
| ✅ 확정 | 이미지 항목 삭제 | DELETE | `/api/v1/blocks/images/items/{imgId}` | 편집 권한 보유자 |

> ⚠️ 이미지는 **두 가지 방법으로 삭제**된다 — 수정 API에서 배열 누락(§수정 API) 또는 이 단건 삭제 API. 둘 다 소프트 삭제만 하고 S3는 지우지 않는 동일한 원칙을 따른다 (아래 삭제 API 참고).

---

### 이미지 항목 생성 `POST /api/v1/blocks/images/{imgBlockId}/items`

**상태**: ✅ 확정
**인증 필요 여부**: Y
**Content-Type**: `multipart/form-data`

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `imgBlockId` | Long | Y | 이미지 항목을 생성할 블록의 ID |

**Request Body (multipart)**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `files` | List<File> | Y | 업로드할 이미지 파일들 (프론트에서 정렬된 순서 그대로 전송: 첫 번째 값이 1번) |
| `request` | JSON (part) | N | `{ "captions": ["회의실 전경", "", "화이트보드"] }` — 각 이미지에 대응하는 캡션(files와 같은 순서, 없으면 빈 문자열) |

> ⚠️ **`captions`를 보냈다면 개수가 `files`와 정확히 같아야 한다** (2026-08-04 결정). 아예 안 보내는 건 허용(전부 `""`)하지만, 보냈는데 개수가 다르면 `400 IMG-004`로 거부한다. 정상적인 프론트 요청이라면 항상 개수가 맞으므로, 다르면 비정상 요청으로 간주한다.

**Request Example**

```
Content-Type: multipart/form-data

files: [image1.jpg, image2.jpg, image3.jpg]
request: { "captions": ["회의실 전경", "", "화이트보드"] }
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.imgBlockId` | Long | 항목이 생성된 이미지 블록 ID |
| `data.images` | List<Object> | 업로드된 이미지 목록 |
| `data.images[].imgId` | Long | 생성된 이미지 ID |
| `data.images[].originalName` | String | 원본 파일명 |
| `data.images[].imageUrl` | String | 저장소에 업로드된 이미지 URL |
| `data.images[].caption` | String | 이미지 캡션 |
| `data.images[].orderIndex` | Int | 이미지 순서 |
| `data.images[].createdAt` | LocalDateTime | 생성일 |

**Success Example**

```json
{
  "httpStatus": 201,
  "message": "이미지 항목 생성 성공",
  "data": {
    "imgBlockId": 1,
    "images": [
      {
        "imgId": 10,
        "originalName": "image1.jpg",
        "imageUrl": "https://s3.../abc.jpg",
        "caption": "회의실 전경",
        "orderIndex": 1,
        "createdAt": "2026-07-31T15:20:00"
      },
      {
        "imgId": 11,
        "originalName": "image2.jpg",
        "imageUrl": "https://s3.../def.jpg",
        "caption": "",
        "orderIndex": 2,
        "createdAt": "2026-07-31T15:20:00"
      }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | — | "이미지 항목 생성 성공" |
| 400 | Bad Request | `IMG-001` | "지원하지 않는 파일 형식입니다." |
| 400 | Bad Request | `IMG-004` | "이미지 개수와 캡션 개수가 일치하지 않습니다." |
| 403 | Forbidden | `IMG-002` | "편집 권한이 없습니다." |
| 403 | Forbidden | `AUTH_PASSWORD_RESET_REQUIRED` | "초기 비밀번호를 먼저 변경해 주세요." (전 도메인 공통 게이트) |
| 404 | Not Found | `IMG-003` | "존재하지 않는 블록입니다." |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | "로그인이 필요합니다." (전 도메인 공통) |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | "서버 내부 오류가 발생했습니다." (전 도메인 공통 폴백) |

> 🔄 **원 명세와의 차이 — 사용자 확인 후 반영 (2026-08-04)**
> - `IMG-016`(401)·`IMG-017`(500) → 체크리스트·텍스트와 동일 이유로 실제로 던지지 않는 도메인 401/500은 만들지 않고 전 도메인 공통 코드(`AUTH_UNAUTHENTICATED`/`COMMON_INTERNAL_ERROR`)로 대체.
> - 나머지 도메인 코드(원본 `IMG-008`·`IMG-010`·`IMG-015`)는 실제로 던지는 순서대로 `IMG-001`부터 재번호.
> - 성공 코드 `IMG-005`는 에러가 아니므로 코드 없이 기록(체크리스트 `CHK` 표기 방식과 동일).

---

### 이미지 항목 수정 `PATCH /api/v1/blocks/images/items/{imgBlockId}`

**상태**: ✅ 확정
**인증 필요 여부**: Y

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `imgBlockId` | Long | Y | 수정할 이미지 블록 ID |

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `images` | List<Object> | Y | 정렬된 순서대로 나열된 이미지 목록(캡션 포함) |
| `images[].imgId` | Long | Y | 이미지 ID |
| `images[].caption` | String | N | 이미지 캡션(없으면 빈 문자열로 저장) |

> 🔄 **삭제 버튼 반영 (2026-08-04 팀 확인)**: 이 화면에 이미지 삭제 버튼이 같이 있어서, **요청 `images`에서 빠진 이미지 = 삭제로 간주한다.** 그 블록의 활성 이미지 중 요청에 없는 것은 이 호출로 소프트 삭제된다. 빈 배열(`images: []`)을 보내면 그 블록의 이미지 전체가 삭제된다.
> ⚠️ **검증**: `images`에 있는 imgId는 전부 그 블록 소속의 활성 이미지여야 한다(부분집합 OK). 중복 imgId·다른 블록 소속·존재하지 않는 imgId가 섞이면 배열 위치 기준 `orderIndex` 계산이 깨지므로 `400 IMG-005`로 거부한다 (명세에 이 기준이 없어 임의로 정함).
> ⚠️ **S3는 지우지 않는다** (2026-08-04 팀 확인) — 이미지는 복구(휴지통) 기능이 없어서 File 도메인처럼 소프트 삭제 후 남겨두는 게 지금은 의미가 없어 보이지만, DB는 팀 전체 방침대로 소프트 삭제만 하고 **S3 실제 삭제는 나중에 하드 삭제 정책이 정해지면 그때 처리한다** (별도 정리 배치로 예정, 아래 "미확정" 참고). 그 전까지 이 사이에 지워진 이미지의 S3 객체는 버킷에 계속 남아 있다 — 필요하면 `aws s3 rm`으로 수동 정리.
> 🔄 **캡션 기본값 — 원 명세("없으면 null")에서 변경 (2026-08-04 담당자 확인)**: 생성 API와 동일하게 **없으면 `""`(빈 문자열)로 저장**한다. `null`/`""` 두 상태가 공존하면 "캡션 지우기"(있던 캡션을 빈 값으로 보냄)와 "원래 없음"을 구분해야 할 이유가 없는데도 프론트·활동 로그 양쪽이 두 값을 다 처리해야 해서 통일했다.

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "이미지 수정 성공",
  "data": {
    "images": [
      { "imgId": 13, "orderIndex": 1, "caption": "회의실 전경" },
      { "imgId": 10, "orderIndex": 2, "caption": "화이트보드" },
      { "imgId": 15, "orderIndex": 3, "caption": "" }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "이미지 수정 성공" |
| 400 | Bad Request | `IMG-005` | "요청한 이미지 목록이 유효하지 않습니다." |
| 403 | Forbidden | `IMG-002` | "편집 권한이 없습니다." |
| 403 | Forbidden | `AUTH_PASSWORD_RESET_REQUIRED` | "초기 비밀번호를 먼저 변경해 주세요." (전 도메인 공통 게이트) |
| 404 | Not Found | `IMG-003` | "존재하지 않는 블록입니다." |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | "로그인이 필요합니다." (전 도메인 공통) |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | "서버 내부 오류가 발생했습니다." (전 도메인 공통 폴백) |

> 🔄 **원 명세와의 차이** — `IMG-006`(성공, 코드 없음)·`IMG-010`(→ 기존 `IMG-002` 재사용)·`IMG-015`(→ 기존 `IMG-003` 재사용)·`IMG-016`/`IMG-017`(→ 공통 코드) 는 생성 API와 동일한 원칙으로 정리. 새로 필요한 코드(원본 `IMG-009`)만 다음 순번인 `IMG-005`로 부여.

**활동 로그** (`.ai/api/activity-log.md` §5.3) — 이미지마다 `caption`·`orderIndex` 중 실제로 값이 바뀐 필드만 `MODIFY`로 남긴다. ⚠️ 이 API는 매번 이미지 전체 목록을 다시 받는 구조라, "사용자가 직접 옮긴 이미지 한 건만 로그"라는 명세 원칙과 어긋난다 — 서버는 어떤 게 사용자가 실제로 옮긴 이미지인지 구분할 수 없어서, **`orderIndex`가 실제로 달라진 이미지 전부**를 로그로 남기도록 구현했다. 과다 로그로 판단되면 프론트가 이동된 이미지 ID를 별도 필드로 알려주는 방향으로 명세를 바꿔야 한다. 요청에서 빠져서 삭제된 이미지는 각각 `DELETE`로 남긴다(생성과 동일하게 `changes`는 전부 null인 항목 하나).

---

### 이미지 항목 삭제 `DELETE /api/v1/blocks/images/items/{imgId}`

**상태**: ✅ 확정
**인증 필요 여부**: Y

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `imgId` | Long | Y | 삭제할 이미지 항목 ID |

**Request Body**: 없음

> ⚠️ **소프트 삭제만 하고 S3는 지우지 않는다** (2026-08-04 팀 확인) — 수정 API의 배열 누락 삭제와 정확히 같은 원칙. 같은 "삭제"인데 어느 경로로 지웠는지에 따라 S3 처리가 달라지면 안 되므로 통일했다. S3 정리는 하드 삭제 정책이 정해지면 별도 배치로 처리한다 (§S3 저장 정책 참고).

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "이미지 항목 삭제 성공",
  "data": null
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "이미지 항목 삭제 성공" |
| 403 | Forbidden | `IMG-002` | "편집 권한이 없습니다." |
| 403 | Forbidden | `AUTH_PASSWORD_RESET_REQUIRED` | "초기 비밀번호를 먼저 변경해 주세요." (전 도메인 공통 게이트) |
| 404 | Not Found | `IMG-006` | "존재하지 않는 항목입니다." |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | "로그인이 필요합니다." (전 도메인 공통) |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | "서버 내부 오류가 발생했습니다." (전 도메인 공통 폴백) |

> 🔄 **원 명세와의 차이** — `IMG-007`(성공, 코드 없음)·`IMG-010`(→ 기존 `IMG-002` 재사용)·`IMG-016`/`IMG-017`(→ 공통 코드)는 앞선 API들과 동일한 원칙으로 정리. 새로 필요한 코드(원본 `IMG-012` "존재하지 않는 항목입니다")만 다음 순번인 `IMG-006`으로 부여.

**활동 로그** — 삭제된 이미지 하나에 `DELETE` 한 건(생성과 동일하게 `changes`는 전부 null인 항목 하나).

---

## ⭐ S3 저장 정책 (구현 결정 — 2026-08-04)

| 항목 | 값 | 근거 |
|---|---|---|
| 저장소 키 | `images/{imgBlockId}/{UUID}.{ext}` | "회사별 접두사" 요청 검토 중 — 현재 도메인 모델엔 회사/고객사 개념이 없어(`PRODUCT.md` 단일 회사 내부 시스템) 보류. 대신 `imgBlockId`를 키에 포함해 나중에 블록↔회사/부서 매핑이 생기면 그 기준으로 묶을 수 있게 함 |
| 리사이즈 | 원본이 임계값(가로/세로 1920px 또는 5MB) 초과 시 축소 후 업로드 | 원본 훼손 없이 저장 용량 절약 |
| 원본 파일명 | DB `image.original_name` 에 그대로 보존 | 리사이즈와 무관하게 사용자가 올린 파일명 유지 |
| 허용 확장자 | `jpg`·`jpeg`·`png`·`gif`·`webp` (화이트리스트) | 명세에 없어 구현 시 임의 결정 — 확장 필요하면 사용자 확인 후 추가 |
| **응답 `imageUrl`** | **presigned GET URL (유효기간 1시간)** | 2026-08-04 콘솔 확인 — 버킷이 퍼블릭 액세스 4종을 전부 차단하고 있어 영구 URL이 작동하지 않음. DB `image.image_url` 컬럼엔 실제로는 **S3 키**만 저장하고, 응답을 만드는 시점에 매번 새로 서명해서 발급한다 (File 도메인의 "다운로드 URL 발급"과 동일 원칙) |

> ⚠️ **이미지 목록 조회 API를 나중에 추가할 때 주의** — 위 presigned 방식 때문에, 그 API도 저장된 URL을 그대로 내려주면 안 되고 매 요청마다 새로 서명해야 한다. 1시간 지난 URL은 403이 난다.

## 미확정

- [x] 실제 S3 버킷 이름 — `vitamins-dev-files` (`.ai/local/INFRA-real.md`에 기록됨)
- [x] 이미지 삭제 — 단건 삭제 API(DELETE) + 수정 API 배열 누락, 두 경로 다 지원 (2026-08-04 결정, §삭제 API·§수정 API 참고)
- [ ] presigned URL 유효기간 1시간 — 임의 결정값, 프론트 요구사항에 따라 조정 필요
- [ ] 수정 API의 "순서 변경 로그를 이동 이미지 1건으로 제한" 명세 원칙과 실제 구현(바뀐 이미지 전부 로그) 차이 — §수정 API 참고, 팀 논의 필요
- [ ] **하드 삭제 정책** — 소프트 삭제된 이미지를 언제·어떻게 하드 삭제할지(정리 배치 주기 등) 팀 결정 대기. 정해지면 그 배치에서 S3 객체도 같이 지운다 (2026-08-04 팀 확인 — 지금은 소프트 삭제만, S3는 유지)
- [ ] **S3 스토리지 클래스/라이프사이클** — 오래됐거나 삭제된 이미지를 저렴한 스토리지 클래스로 전환하는 것도 검토 가능하나, 지금은 구현하지 않고 방향만 기록 (2026-08-04 팀 확인). 필요해지면 버킷 라이프사이클 규칙으로 처리 — 애플리케이션 코드 변경은 불필요할 가능성이 높음

## 범위 밖 (블록 자체 CRUD)

- **블록 생성**: 공용 블록 담당자가 `block` + `image_block` 행을 함께 만든다. 이 도메인은 다루지 않는다.
- **블록 삭제**: 텍스트·체크리스트와 동일하게 Block 도메인이 이벤트를 발행하면 리스너로 받아 처리할 예정(아직 미구현).
