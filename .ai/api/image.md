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
| ✅ 확정 | 이미지 항목 조회(다음/이전) | GET | `/api/v1/blocks/images/{imgBlockId}/items/{currentOrderIndex}?direction={prev\|next}` | 접근 권한 보유자 |
| ✅ 확정 | 이미지 다운로드 | GET | `/api/v1/blocks/images/{imgBlockId}/download?imgId={imgId}` | 접근 권한 보유자 |
| ✅ 확정 | 이미지 휴지통 조회 | GET | `/api/v1/projects/{projectId}/images/trash` | 프로젝트 접근 권한 보유자 |
| ✅ 확정 | 이미지 항목 생성 | POST | `/api/v1/blocks/images/{imgBlockId}/items` | 편집 권한 보유자 |
| ✅ 확정 | 이미지 항목 수정 | PATCH | `/api/v1/blocks/images/items/{imgBlockId}` | 편집 권한 보유자 |
| ✅ 확정 | 이미지 항목 삭제 | DELETE | `/api/v1/blocks/images/items/{imgId}` | 편집 권한 보유자 |
| ✅ 확정 | 이미지 복구 | PATCH | `/api/v1/blocks/images/items/restore` | 편집 권한 보유자(이미지별 소속 블록 기준) |
| ✅ 확정 | 이미지 영구 삭제 | DELETE | `/api/v1/blocks/images/items/hard` | 편집 권한 보유자(이미지별 소속 블록 기준) |
| ✅ 확정 | 프로젝트 이미지 모아보기 | GET | `/api/v1/projects/{projectId}/images` | 프로젝트 접근 권한 보유자 |

> ⚠️ 이미지는 **두 가지 방법으로 삭제**된다 — 수정 API에서 배열 누락(§수정 API) 또는 이 단건 삭제 API. 둘 다 소프트 삭제만 하고 S3는 지우지 않는 동일한 원칙을 따른다 (아래 삭제 API 참고).

---

### 이미지 항목 조회 `GET /api/v1/blocks/images/{imgBlockId}/items/{currentOrderIndex}?direction={prev|next}`

**상태**: ✅ 확정
**인증 필요 여부**: Y

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `imgBlockId` | Long | Y | 접근할 이미지 블록 ID |
| `currentOrderIndex` | Long | Y | 현재 이미지의 정렬 번호 |

**Query Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `direction` | String | Y | 다음 이미지(`next`)를 원하는지 이전 이미지(`prev`)를 원하는지 |

**Request Body**: 없음

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.imgId` | Long | 조회된 이미지 ID |
| `data.originalName` | String | 원본 파일명 |
| `data.imageUrl` | String | 저장소에 업로드된 이미지 URL(presigned) |
| `data.caption` | String | 이미지 캡션 |
| `data.orderIndex` | Int | 조회된 이미지의 정렬 번호 |
| `data.totalCount` | Int | 해당 블록의 전체(활성) 이미지 개수 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "이미지 항목 조회 성공",
  "data": {
    "imgId": 10,
    "originalName": "image1.jpg",
    "imageUrl": "https://s3.../abc.jpg",
    "caption": "회의실 전경",
    "orderIndex": 2,
    "totalCount": 5
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "이미지 항목 조회 성공" |
| 400 | Bad Request | `IMG-011` | "direction 값이 올바르지 않습니다." |
| 403 | Forbidden | `IMG-007` | "접근 권한이 없습니다." |
| 403 | Forbidden | `AUTH_PASSWORD_RESET_REQUIRED` | "초기 비밀번호를 먼저 변경해 주세요." (전 도메인 공통 게이트) |
| 404 | Not Found | `IMG-006` | "존재하지 않는 항목입니다." (블록은 있는데 활성 이미지가 하나도 없음) |
| 404 | Not Found | `IMG-003` | "존재하지 않는 블록입니다." |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | "로그인이 필요합니다." (전 도메인 공통) |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | "서버 내부 오류가 발생했습니다." (전 도메인 공통 폴백) |

> 🔄 **원 명세와의 차이 (2026-08-05, 구현 시 정리)**
> - 성공 코드(원본 `IMG-003`)는 에러가 아니므로 코드 없이 기록.
> - 403 원본 `IMG-011`("접근 권한이 없습니다")은 다른 API의 `IMG-002`(편집 권한, "편집 권한이 없습니다")와 메시지가 달라 별도 코드가 필요 — 실제로 던지는 순서대로 `IMG-007`로 번호 부여.
> - 404 원본 `IMG-012`(항목 없음)·`IMG-015`(블록 없음)는 기존에 이미 있는 `ITEM_NOT_FOUND`(`IMG-006`)·`BLOCK_NOT_FOUND`(`IMG-003`)를 그대로 재사용 — 같은 뜻의 코드를 중복으로 안 만든다는 기존 원칙 그대로 적용.
> - `IMG-016`(401)·`IMG-017`(500)은 다른 API와 동일 이유로 공통 코드(`AUTH_UNAUTHENTICATED`/`COMMON_INTERNAL_ERROR`)로 대체.
> - **`IMG-011`(400, 신규, 2026-08-06)** — 원 명세에 없던 케이스, 코드 리뷰로 발견. `direction`이 `next`/`prev`가 아닌 값으로 오면 `ImageNavigationDirection.valueOf`가 `IllegalArgumentException`을 던지는데, 이걸 그냥 두면 500(`COMMON_INTERNAL_ERROR`)으로 새서 400으로 바꿔 처리했다. ⚠️ 이 코드는 원본 노션 명세의 `IMG-011`(그건 이미 `IMG-007`로 재번호됨, 위 참고)과는 번호만 같고 무관한, 이번에 새로 부여한 번호다.
>
> ⚠️ **권한 종류가 다름** — 이 API만 유일하게 **접근(VIEW) 권한**을 검사한다(`BlockCatalogPort.hasViewPermission`). 생성·수정·삭제는 전부 **편집 권한**(`hasEditPermission`)이다. `hasViewPermission`은 그동안 포트 인터페이스에만 있고 아무도 안 쓰던 메서드였는데, 이 API가 처음으로 실사용한다.
>
> **순환(wrap-around) 동작**: `direction=next`인데 다음 이미지가 없으면(현재가 마지막) 가장 작은 orderIndex(첫 이미지)로 순환한다. `direction=prev`인데 이전 이미지가 없으면(현재가 처음) 가장 큰 orderIndex(마지막 이미지)로 순환한다. 이미지가 1장뿐이면 자기 자신을 반환한다.
>
> ⚠️ **`currentOrderIndex` 자체가 그 블록의 실제 활성 이미지를 가리키는지 먼저 검증한다.** 이 검증이 없으면 "다음이 없다"(순환 대상)와 "애초에 존재하지 않는 orderIndex를 보냈다"를 구분할 방법이 없어서, 예를 들어 이미지가 3장(orderIndex 1~3)뿐인데 `currentOrderIndex=999`로 조회해도 조용히 1번 이미지가 나가버리는 버그가 있었다(2026-08-06 발견·수정). 존재하지 않는 `currentOrderIndex`는 `404 IMG-006`으로 거부한다.
>
> **프론트 캐싱 관련**: 위 "이미지 목록/다음 이미지 조회 API를 나중에 추가할 때 주의" 콜아웃 참고 — 이 API도 캐싱하지 말고 다음/이전 클릭마다 매번 호출해야 한다.

---

### 이미지 다운로드 `GET /api/v1/blocks/images/{imgBlockId}/download?imgId={imgId}`

**상태**: ✅ 확정
**인증 필요 여부**: Y

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `imgBlockId` | Long | Y | 다운로드할 이미지 블록 ID(블록 이름으로 zip 생성) |

**Query Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `imgId` | Long | N | 다운로드할 이미지 ID(없으면 블록 전체 다운로드) |

**Request Body**: 없음

**Response**: JSON이 아니라 파일 바이너리를 응답 바디에 직접 담아 내려준다.

```text
단건: Content-Type: image/jpeg 등,
      Content-Disposition: attachment; filename="download.jpg"; filename*=UTF-8''원본파일명.jpg
전체: Content-Type: application/zip,
      Content-Disposition: attachment; filename="download.zip"; filename*=UTF-8''블록명.zip
```

> ⚠️ **`filename=`은 항상 ASCII 폴백(`download.확장자`)이고, 실제 파일명(원본 파일명/블록명, 한글 포함)은 `filename*=UTF-8''...`에만 있다** (RFC 5987/6266) — `filename=`만 읽는 구식 클라이언트는 실제 이름 대신 `download.jpg`/`download.zip`을 보게 된다. HTTP 헤더 값이 ISO-8859-1로 나가서 한글을 `filename=`에 그대로 넣으면 깨지거나 헤더 파싱이 틀어질 수 있어(2026-08-06 발견) 이렇게 분리했다 — 최신 브라우저는 `filename*=`을 우선 사용해서 실사용엔 문제없다.

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "이미지 다운로드 성공" |
| 403 | Forbidden | `IMG-007` | "접근 권한이 없습니다." |
| 403 | Forbidden | `AUTH_PASSWORD_RESET_REQUIRED` | "초기 비밀번호를 먼저 변경해 주세요." (전 도메인 공통 게이트) |
| 404 | Not Found | `IMG-006` | "존재하지 않는 항목입니다." (imgId 지정 시, 그 블록 소속 활성 이미지가 아님) |
| 404 | Not Found | `IMG-008` | "다운로드할 이미지가 없습니다." (imgId 생략 시, 블록에 활성 이미지가 0장) |
| 404 | Not Found | `IMG-003` | "존재하지 않는 블록입니다." |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | "로그인이 필요합니다." (전 도메인 공통) |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | "서버 내부 오류가 발생했습니다." (전 도메인 공통 폴백) |

> 🔄 **원 명세와의 차이 (2026-08-06, 구현 시 정리)**
> - 성공 코드(원본 `IMG-004`)는 에러가 아니므로 코드 없이 기록.
> - 403 원본 `IMG-011`("접근 권한이 없습니다")은 이미지 항목 조회 API에서 이미 만든 `VIEW_FORBIDDEN`(`IMG-007`)과 메시지가 완전히 같아 그대로 재사용 — 같은 뜻의 코드를 중복으로 안 만든다는 원칙.
> - 404 원본 `IMG-012`(항목 없음)·`IMG-015`(블록 없음)도 기존 `ITEM_NOT_FOUND`(`IMG-006`)·`BLOCK_NOT_FOUND`(`IMG-003`) 재사용.
> - 404 원본 `IMG-014`("다운로드할 이미지가 없습니다")는 새 상황(전체 다운로드인데 활성 이미지 0장)이라 다음 순번 `IMG-008`로 신규 부여.
> - `IMG-016`(401)·`IMG-017`(500)은 다른 API와 동일 이유로 공통 코드로 대체.
>
> **구현 메모**: 저장소(S3)에서 실제 파일 바이트를 읽어와야 해서 `ImageStoragePort`에 `download`/`contentTypeOf` 메서드를 추가했다(기존엔 업로드·presign URL 발급만 있었음). 블록 전체 다운로드는 원본 파일명이 겹치는 경우(같은 이름으로 여러 번 업로드) zip 엔트리 이름 충돌을 막기 위해 OS 탐색기 방식과 동일하게 `원본파일명-1.jpg`, `원본파일명-2.jpg`, ...로 구분한다(2026-08-06, 처음엔 `{imgId}_{원본파일명}`이었으나 imgId가 사용자에게 무의미해서 변경 — `-1`로도 겹치면 `-2`로 계속 재시도). zip은 이미지 개수만큼 S3에서 순차로 읽어 메모리에서 조립한다 — 이미지 개수가 아주 많아지면(수백 장) 메모리·응답 시간 부담이 커질 수 있어, 필요해지면 스트리밍 방식으로 바꾸는 걸 검토할 것.

---

### 프로젝트 이미지 모아보기 `GET /api/v1/projects/{projectId}/images`

**상태**: ✅ 확정
**인증 필요 여부**: Y

**Path Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `projectId` | Long | 이미지를 모아볼 프로젝트 ID |

**Request Body**: 없음

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.images` | List<Object> | 프로젝트 내 전체(활성) 이미지 목록 |
| `data.images[].imgBlockId` | Long | 속한 블록 ID |
| `data.images[].imgId` | Long | 이미지 ID |
| `data.images[].originalName` | String | 원본 파일명 |
| `data.images[].imageUrl` | String | 저장소 이미지 URL(presigned) |
| `data.images[].caption` | String | 이미지 캡션 |
| `data.images[].createdAt` | LocalDateTime | 생성일 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "프로젝트 이미지 모아보기 조회 성공",
  "data": {
    "images": [
      { "imgId": 10, "imgBlockId": 3, "originalName": "회의사진.jpg", "imageUrl": "https://s3.../abc.jpg", "caption": "회의실 전경", "createdAt": "2026-08-01T10:00:00" },
      { "imgId": 15, "imgBlockId": 5, "originalName": "화이트보드.jpg", "imageUrl": "https://s3.../def.jpg", "caption": "", "createdAt": "2026-08-02T14:00:00" }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "프로젝트 이미지 모아보기 조회 성공" |
| 403 | Forbidden | `PROJECT_ACCESS_DENIED` | "프로젝트에 접근할 권한이 없습니다." (프로젝트 도메인 공통 코드) |
| 403 | Forbidden | `AUTH_PASSWORD_RESET_REQUIRED` | "초기 비밀번호를 먼저 변경해 주세요." (전 도메인 공통 게이트) |
| 404 | Not Found | `PROJECT_NOT_FOUND` | "프로젝트를 찾을 수 없습니다." (프로젝트 도메인 공통 코드) |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | "로그인이 필요합니다." (전 도메인 공통) |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | "서버 내부 오류가 발생했습니다." (전 도메인 공통 폴백) |

> 🔄 **원 명세와의 차이 (2026-08-06, 구현 시 정리)** — 휴지통 조회 API와 완전히 동일한 이유로 동일하게 처리했다.
> - **권한 검증 방식** — 원 명세는 403을 `IMG-002`(편집 권한 코드, "편집 권한이 없습니다")로 뒀지만 실제 메시지는 "접근 권한이 없습니다"라 코드-문구가 안 맞았고, 애초에 이 API는 프로젝트 전체(여러 스텝에 걸침) 조회라 스텝 단위 권한 체크가 불가능하다. 휴지통 조회와 동일하게 `ProjectAccessUseCase.requireAccess(projectId, userId, role)`로 판정 — 403은 `PROJECT_ACCESS_DENIED`, 404(프로젝트 없음)는 `PROJECT_NOT_FOUND`(둘 다 프로젝트 도메인 공통 코드, IMG 코드 아님).
> - **정렬 기준(생성일 최신순)은 명세에 없어 임의로 정함** — 휴지통 조회가 삭제일 최신순인 것과 통일했다.
>
> **구현 메모** — 휴지통 조회와 조인 체인은 동일(`image → image_block → block → step`, `step.project_id`로 필터)하고 삭제 필터 방향만 반대다(`deleted_at IS NULL` — 활성 이미지만). 관심사가 달라 `ImageGalleryMapper`로 별도 파일 분리, `ImageTrashController`는 이 API 추가로 `ImageProjectController`로 이름을 바꿨다(프로젝트 단위 이미지 API 2종을 함께 담음).

---

### 이미지 휴지통 조회 `GET /api/v1/projects/{projectId}/images/trash`

**상태**: ✅ 확정
**인증 필요 여부**: Y

**Path Parameter**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 삭제된 이미지를 조회할 프로젝트 ID |

**Request Body**: 없음

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.images` | List<Object> | 삭제된 이미지 목록 |
| `data.images[].imgId` | Long | 이미지 ID |
| `data.images[].originalName` | String | 원본 파일명 |
| `data.images[].imageUrl` | String | 저장소 이미지 URL(presigned) |
| `data.images[].caption` | String | 이미지 캡션 |
| `data.images[].deletedAt` | LocalDateTime | 삭제 일시 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "이미지 휴지통 조회 성공",
  "data": {
    "images": [
      {
        "imgId": 10,
        "originalName": "회의사진.jpg",
        "imageUrl": "https://s3.../abc.jpg",
        "caption": "회의실 전경",
        "deletedAt": "2026-08-03T10:00:00"
      }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "이미지 휴지통 조회 성공" |
| 403 | Forbidden | `PROJECT_ACCESS_DENIED` | "프로젝트에 접근할 권한이 없습니다." (프로젝트 도메인 공통 코드) |
| 403 | Forbidden | `AUTH_PASSWORD_RESET_REQUIRED` | "초기 비밀번호를 먼저 변경해 주세요." (전 도메인 공통 게이트) |
| 404 | Not Found | `PROJECT_NOT_FOUND` | "프로젝트를 찾을 수 없습니다." (프로젝트 도메인 공통 코드) |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | "로그인이 필요합니다." (전 도메인 공통) |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | "서버 내부 오류가 발생했습니다." (전 도메인 공통 폴백) |

> 🔄 **원 명세와의 차이 (2026-08-06, 구현 시 정리)**
> - **권한 검증 방식 자체를 바꿈** — 원 명세는 403을 `IMG-002`(이미지 도메인 편집 권한 코드, "편집 권한이 없습니다")로 뒀지만, 이 API는 편집이 아니라 **프로젝트 접근 권한**을 보는 게 맞다고 판단(사용자와 논의 — 이 API는 특정 스텝 하나가 아니라 프로젝트 전체에 걸친 조회라 스텝 단위 권한 체크가 불가능함). `ProjectAccessUseCase.requireAccess(projectId, userId, role)`를 그대로 재사용 — 이미 구현돼 있어서 신규 코드가 필요 없었다. 403은 `PROJECT_ACCESS_DENIED`, 404(프로젝트 없음)는 `PROJECT_NOT_FOUND`(둘 다 프로젝트 도메인 공통 코드, IMG 코드 아님).
> - **404 `IMG-006`("존재하지 않는 항목입니다") 제거** — 목록 조회 API라 삭제된 이미지가 0개면 그냥 빈 배열 200이 맞고, "항목이 존재하지 않음"을 에러로 취급할 상황 자체가 없음.
> - `IMG-016`(401)·`IMG-017`(500)은 다른 API와 동일 이유로 공통 코드로 대체.
>
> **구현 메모**: `image → image_block → block → step` 을 타고 `step.project_id`로 필터링한다. 여러 테이블 조인이라 Block 도메인에 별도 어댑터를 요청하지 않고 MyBatis로 직접 조회(`ImageTrashMapper`, `.ai/docs/global/MYBATIS.md` 기준에 부합). 삭제된 이미지도 S3 객체는 남아있으므로(소프트 삭제 원칙) `imageUrl`은 정상적으로 presign해서 내려준다.

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

```text
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
| 400 | Bad Request | `IMG-010` | "한 번에 업로드할 수 있는 파일 개수를 초과했습니다." |
| 403 | Forbidden | `IMG-002` | "편집 권한이 없습니다." |
| 403 | Forbidden | `AUTH_PASSWORD_RESET_REQUIRED` | "초기 비밀번호를 먼저 변경해 주세요." (전 도메인 공통 게이트) |
| 404 | Not Found | `IMG-003` | "존재하지 않는 블록입니다." |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | "로그인이 필요합니다." (전 도메인 공통) |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | "서버 내부 오류가 발생했습니다." (전 도메인 공통 폴백) |

> 🔄 **원 명세와의 차이 — 사용자 확인 후 반영 (2026-08-04)**
> - `IMG-016`(401)·`IMG-017`(500) → 체크리스트·텍스트와 동일 이유로 실제로 던지지 않는 도메인 401/500은 만들지 않고 전 도메인 공통 코드(`AUTH_UNAUTHENTICATED`/`COMMON_INTERNAL_ERROR`)로 대체.
>
> ⚠️ **`IMG-010`(파일 개수 상한, 2026-08-06 추가, 15장으로 조정)** — 명세엔 없던 검증, 코드 리뷰(CodeRabbit)로 발견한 성능 이슈 대응. 이 API는 요청 하나가 파일마다 순차적으로 S3 업로드(느린 I/O)를 하는 동안 DB 트랜잭션(블록 활성 확인용 비관적 락 포함)을 계속 잡고 있다 — 파일이 많을수록 커넥션을 그만큼 오래 붙잡아서, 동시에 여러 사용자가 대량 업로드하면 DB 커넥션 풀이 고갈될 위험이 있다. S3 업로드를 트랜잭션 밖으로 완전히 빼는 게 정석이지만 리팩터 범위가 커서(재테스트 필요), 최악의 경우를 유한하게 만드는 가벼운 방어로 **한 요청당 파일 15장 상한**만 걸었다 — 처음엔 20장이었는데, 파일당 용량 상한(20MB, `application.yml`)과 곱하면 400MB로 전체 요청 용량 상한(300MB)을 넘을 수 있다는 걸 코드 리뷰로 다시 발견해서 `15 × 20MB = 300MB`로 정확히 맞아떨어지게 낮췄다(2026-08-06). 완전한 해결(트랜잭션 분리)은 백로그(`.ai/local/STATE.md`)로 남김.
> - 나머지 도메인 코드(원본 `IMG-008`·`IMG-010`·`IMG-015`)는 실제로 던지는 순서대로 `IMG-001`부터 재번호.
> - 성공 코드 `IMG-005`는 에러가 아니므로 코드 없이 기록(체크리스트 `CHK` 표기 방식과 동일).
> - **`IMG-001`은 확장자 불일치뿐 아니라 실제 콘텐츠 위장(2026-08-06 추가)도 같은 코드로 응답한다** — 매직 바이트로 파일 내용이 확장자와 실제로 맞는지 확인한다(확장자만 `.png`로 바꾼 텍스트 파일 등 위장 업로드 방지). `files` 전체를 업로드 시작 전에 확장자·콘텐츠 둘 다 미리 검증한다 — 업로드하면서 파일마다 검증하면 뒤쪽 파일이 걸릴 때 앞서 올라간 파일이 S3에 고아 객체로 남기 때문(원래부터 확장자 검증이 그래서 upfront였는데, 콘텐츠 검증을 처음엔 업로드 어댑터 안에 둬서 이 원칙이 깨졌던 걸 사용자가 테스트로 발견·수정).
> - ⚠️ **매직 바이트는 파일 맨 앞 몇 바이트만 봐서 "헤더는 진짜인데 본문이 손상된" 파일까진 못 잡는다** — jpg/png/gif는 JDK 표준 `ImageIO`로 실제 디코딩까지 한 번 더 확인해서 막는다(2026-08-06, 코드 리뷰로 발견). **webp는 JDK에 내장 디코더가 없어서 매직 바이트 검사만 유지한다** — 알려진 제한사항, 필요해지면 외부 라이브러리 추가를 검토(`.ai/local/STATE.md` 백로그).

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

### 이미지 복구 `PATCH /api/v1/blocks/images/items/restore`

**상태**: ✅ 확정
**인증 필요 여부**: Y

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `imgIds` | List<Long> | Y | 복구할 이미지 ID 목록 |

> ⚠️ 원 명세 표에는 `imagIds`로 적혀 있으나 Request Example의 JSON은 `imgIds`다 — 오탈자로 보고 다른 API와 이름 규칙이 같은 `imgIds`로 구현한다.

**Request Example**

```json
{
  "imgIds": [10, 15, 22]
}
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data.images` | List<Object> | 복구된 이미지 목록 |
| `data.images[].imgBlockId` | Long | 복구된 이미지가 속한 블록 ID |
| `data.images[].imgId` | Long | 복구된 이미지 ID |
| `data.images[].originalName` | String | 원본 파일명 |
| `data.images[].orderIndex` | Int | 복구 후 순서 |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "이미지 복구 성공",
  "data": {
    "images": [
      { "imgBlockId": 3, "imgId": 10, "originalName": "회의사진.jpg", "orderIndex": 6 },
      { "imgBlockId": 5, "imgId": 15, "originalName": "화이트보드.jpg", "orderIndex": 3 },
      { "imgBlockId": 3, "imgId": 22, "originalName": "발표자료.jpg", "orderIndex": 7 }
    ]
  }
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "이미지 복구 성공" |
| 400 | Bad Request | `IMG-005` | "요청한 이미지 목록이 유효하지 않습니다." |
| 403 | Forbidden | `IMG-002` | "편집 권한이 없습니다." |
| 403 | Forbidden | `AUTH_PASSWORD_RESET_REQUIRED` | "초기 비밀번호를 먼저 변경해 주세요." (전 도메인 공통 게이트) |
| 404 | Not Found | `IMG-006` | "존재하지 않는 항목입니다." |
| 404 | Not Found | `IMG-009` | "상위 블록이 삭제되어 하위 항목을 복구할 수 없습니다." |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | "로그인이 필요합니다." (전 도메인 공통) |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | "서버 내부 오류가 발생했습니다." (전 도메인 공통 폴백) |

> 🔄 **원 명세와의 차이 (2026-08-06, 구현 시 정리)**
> - 원 명세엔 400 코드가 비어 있었음 — 수정 API(IMG-005, 동일 문구)를 그대로 재사용. 새 코드를 만들지 않는 기존 원칙 그대로 적용.
> - 403(`IMG-002`)·404(`IMG-006`)도 각각 생성/삭제 API에서 이미 만든 코드를 재사용 — 새 코드 없음.
> - `imgIds`가 비어있거나 중복 imgId가 섞이면 `400 IMG-005`(순서 계산이 깨지는 문제라 수정 API와 동일한 이유). 존재하지 않는 imgId 또는 **이미 활성 상태(삭제 안 된)인 imgId**는 구분하지 않고 전부 `404 IMG-006`으로 거부한다 — 사용자와 확인.
> - **404 `IMG-009`(신규)는 원 명세에 없던 케이스, 사용자가 실제 테스트로 발견해서 추가함(2026-08-06)** — 휴지통 조회는 `image → image_block → block → step` 조인에서 상위(블록·이미지 블록)의 삭제 여부를 걸러내지 않아서, **블록 자체가 삭제된 뒤에도 그 블록에 속했던 이미지는 계속 휴지통에 조회된다**(의도된 동작 — 영구 삭제 전까지는 존재를 알 수 있어야 함). 이런 이미지를 복구 시도하면 블록이 없어 되돌릴 자리가 없다. 처음엔 기존 `IMG-003`("존재하지 않는 블록입니다")을 재사용하려 했으나, 그 코드는 "블록이 애초에 없음"(생성·수정 API)용이라 "있었는데 삭제됨"과 뉘앙스가 달라 헷갈릴 수 있어 새 코드로 분리했다.
> - **에러 확인 순서 (2026-08-06 최종)** — ①이미지 존재/삭제 상태(`IMG-006`) → ②편집 권한(`IMG-002`, 블록 삭제 여부와 무관하게 정확히 판정) → ③블록 생존 여부(`IMG-009`). ①이 권한 확인보다 먼저인 건 텍스트·체크리스트 도메인의 기존 패턴(`TextCommandService.updateContent`, `ChecklistCommandService.update`/`delete`)과 동일하게 맞춘 것 — 이미지 하나만 바꾸지 않기로 함(`.ai/local/STATE.md` 백로그 참고). 그런데 ②·③ 순서는 처음 구현과 반대로 바꿨다 — 아래 참고.
> - **권한 확인이 블록 생존 여부보다 먼저다 (순서 변경, 2026-08-06)** — 처음엔 "블록 생존 확인 → 권한 확인" 순서였는데, 이러면 그 블록에 아무 권한도 없는 사용자도 imgId를 넣어보면 "블록이 삭제됐다"는 정보를 권한 확인 전에 알 수 있다는 문제가 있었다(사용자 지적). `ImageEligibilityPolicy.assertEditPermissionEvenIfBlockDeleted`를 새로 만들어 **블록이 삭제돼 있어도 정확한 편집 권한을 판정**할 수 있게 되면서 순서를 뒤집을 수 있었다 — 블록이 살아있으면 평소처럼 공유 `BlockCatalogPort.hasEditPermission`을 쓰고, 죽어있으면 그 블록이 속했던 stepId를 이미지 도메인이 직접 찾아(`image_block→block`, 삭제 여부 무시하고 조회 — `ImageTrashMapper.findStepIdByImgBlockId`) 이미 존재하는 `StepAccessUseCase.requireEditable`로 넘긴다(실제 권한 판정 로직은 그대로 Step 도메인이 소유, 이미지 도메인은 ID만 찾아서 넘길 뿐). 그 결과 권한이 없는 사용자는 블록 상태와 무관하게 항상 `IMG-002`만 보고, 권한이 있는 사용자만 블록 삭제 여부(`IMG-009`)까지 도달한다.
> - ⚠️ **위 대체 경로를 타면 403/404 코드가 `IMG-002`가 아니라 Step 도메인 코드(`STEP_EDIT_DENIED`/`STEP_NOT_FOUND`)로 나갈 수 있다** — `StepAccessUseCase.requireEditable`이 직접 던지는 예외라 이미지 도메인 코드로 감싸지 않았다. 극히 드문 경로(블록이 삭제된 이미지에 대한 권한 판정)에서만 발생한다.
> - ⚠️ **임시 우회다** — `ImageTrashMapper.findStepIdByImgBlockId`는 공유 Block 도메인(동훈님)에 "삭제된 블록도 포함해서 stepId를 찾는" 정식 포트가 없어서 이미지 도메인이 직접 `block` 테이블을 조회하는 우회다. 정식 포트가 생기면 그걸로 교체할 것 — 지금은 요청하지 않기로 함(2026-08-06 결정).
>
> ⚠️ **`imgId` 별로 소속된 블록(=스텝) 기준으로 블록 생존 여부와 편집 권한을 각각 확인한다** — 요청 하나에 여러 블록의 이미지가 섞여 올 수 있다. 한 블록이라도 (블록이 삭제됐거나) 권한이 없으면 전체 요청을 거부한다(부분 복구 없음).
>
> ⚠️ **블록까지 삭제되어 복구가 불가능한 이미지는 결국 영구 삭제(하드 삭제)로만 정리할 수 있다** — 하드 삭제 정책 자체는 아직 팀 결정 대기 상태라(§미확정 참고) 이번엔 복구 API의 에러 처리만 정리하고, 하드 삭제 기능은 그 정책이 정해지면 별도로 구현한다.
>
> ⚠️ **복구 후 순서는 "원래 있던 자리"가 아니라 그 블록의 현재 활성 목록 맨 뒤에 순서대로 이어 붙인다** (Success Example 참고 — `imgBlockId=3`인 두 이미지가 요청 순서 그대로 6, 7번을 받음). 원래 orderIndex를 기억해뒀다가 되돌리는 방식은 그 사이 다른 이미지가 그 자리를 차지했을 수 있어 채택하지 않았다.
>
> ✅ **활동 로그 반영 완료 (2026-08-06)** — 파일 휴지통 대응으로 `ActivityLogAction.RESTORE`가 추가되면서(#206) 이미지 도메인도 그대로 사용하기 시작했다. 이미지 하나 복구당 `RESTORE` 로그 한 건, `resourceName`엔 원본 파일명, `changes`는 생성·삭제와 동일하게 전부 null인 항목 하나.

---

### 이미지 영구 삭제 `DELETE /api/v1/blocks/images/items/hard`

**상태**: ✅ 확정
**인증 필요 여부**: Y

**Request Body**

| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `imgIds` | List<Long> | Y | 영구 삭제할 이미지 ID 목록(휴지통에 있는 것만) |

**Request Example**

```json
{
  "imgIds": [10, 15, 22]
}
```

**Response Parameter**

| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 항상 `null` |

**Success Example**

```json
{
  "httpStatus": 200,
  "message": "이미지 영구 삭제 성공",
  "data": null
}
```

**Status Code**

| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | — | "이미지 영구 삭제 성공" |
| 400 | Bad Request | `IMG-005` | "요청한 이미지 목록이 유효하지 않습니다." |
| 403 | Forbidden | `IMG-002` | "편집 권한이 없습니다." |
| 403 | Forbidden | `AUTH_PASSWORD_RESET_REQUIRED` | "초기 비밀번호를 먼저 변경해 주세요." (전 도메인 공통 게이트) |
| 404 | Not Found | `IMG-006` | "존재하지 않는 항목입니다." (휴지통에 없는 이미지 포함) |
| 401 | Unauthorized | `AUTH_UNAUTHENTICATED` | "로그인이 필요합니다." (전 도메인 공통) |
| 500 | Internal Server Error | `COMMON_INTERNAL_ERROR` | "서버 내부 오류가 발생했습니다." (전 도메인 공통 폴백) |

> 🔄 **원 명세와의 차이 (2026-08-06, 구현 시 정리)** — 전부 기존 코드 재사용, 새 코드 없음. 400 원래 공란 → `IMG-005`(복구 API와 동일 문구), 403·404는 각각 `IMG-002`·`IMG-006` 그대로. 단, 블록이 삭제된 이미지를 영구 삭제할 때는 403이 `IMG-002`가 아니라 Step 도메인 코드(`STEP_EDIT_DENIED`)로 나갈 수 있다 — 아래 참고.
>
> **구현 메모** — S3 객체를 먼저 지우고 DB 행을 지운다(둘 다 같은 트랜잭션 — S3 삭제가 실패하면 예외로 트랜잭션 자체가 롤백돼 DB엔 아무 변화도 없다). 대상 판정은 복구 API와 정반대 대칭: **이미 휴지통(소프트 삭제)에 있는 이미지만** 영구 삭제 가능, 아직 삭제 안 된(활성) imgId나 존재하지 않는 imgId는 전부 `404 IMG-006`.
>
> ✅ **블록까지 삭제된 휴지통 이미지도 영구 삭제 가능 (2026-08-06 해결)** — 처음엔 복구 API(`IMG-009`)와 같은 이유로 이것도 막혀 있었다(공유 `BlockCatalogPort.hasEditPermission`이 삭제된 블록에 대해 권한 유무와 무관하게 항상 false 반환). 근데 복구와 달리 영구 삭제는 "블록이 없어도 동작 자체는 가능해야 하는" 기능이라 이 제약을 그대로 받아들일 수 없었다 — `ImageEligibilityPolicy.assertEditPermissionEvenIfBlockDeleted`를 새로 만들어, 블록이 죽어있으면 그 블록이 속했던 stepId를 이미지 도메인이 직접 찾아(`ImageTrashMapper.findStepIdByImgBlockId`, block의 삭제 여부 무시) 이미 존재하는 `StepAccessUseCase.requireEditable`로 넘기는 방식으로 해결했다(실제 권한 판정 로직은 그대로 Step 도메인 소유, 새로 만든 권한 로직 없음). §복구 API 콜아웃에 같은 메서드에 대한 상세 설명 있음.
>
> ⚠️ **임시 우회다** — 위 방식은 공유 Block 도메인(동훈님)에 "삭제된 블록도 포함해서 stepId를 찾는" 정식 포트가 없어서 이미지 도메인이 직접 `block` 테이블을 조회하는 우회다. 정식 포트가 생기면 그걸로 교체할 것 — 지금은 요청하지 않기로 함(2026-08-06 결정, `.ai/local/STATE.md` 백로그 참고).

---

## ⭐ S3 저장 정책 (구현 결정 — 2026-08-04)

| 항목 | 값 | 근거 |
|---|---|---|
| 저장소 키 | `images/{imgBlockId}/{UUID}.{ext}` | "회사별 접두사" 요청 검토 중 — 현재 도메인 모델엔 회사/고객사 개념이 없어(`PRODUCT.md` 단일 회사 내부 시스템) 보류. 대신 `imgBlockId`를 키에 포함해 나중에 블록↔회사/부서 매핑이 생기면 그 기준으로 묶을 수 있게 함 |
| 리사이즈 | 원본이 임계값(가로/세로 1920px 또는 5MB) 초과 시 축소 후 업로드 | 원본 훼손 없이 저장 용량 절약 |
| 원본 파일명 | DB `image.original_name` 에 그대로 보존 | 리사이즈와 무관하게 사용자가 올린 파일명 유지 |
| 허용 확장자 | `jpg`·`jpeg`·`png`·`gif`·`webp` (화이트리스트) | 명세에 없어 구현 시 임의 결정 — 확장 필요하면 사용자 확인 후 추가 |
| **응답 `imageUrl`** | **presigned GET URL (유효기간 1시간)** | 2026-08-04 콘솔 확인 — 버킷이 퍼블릭 액세스 4종을 전부 차단하고 있어 영구 URL이 작동하지 않음. DB `image.image_url` 컬럼엔 실제로는 **S3 키**만 저장하고, 응답을 만드는 시점에 매번 새로 서명해서 발급한다 (File 도메인의 "다운로드 URL 발급"과 동일 원칙) |

> ⚠️ **이미지 목록/다음 이미지 조회 API를 나중에 추가할 때 주의** — 위 presigned 방식 때문에, 그 API도 저장된 URL을 그대로 내려주면 안 되고 매 요청마다 새로 서명해야 한다. 1시간 지난 URL은 403이 난다.
>
> **프론트 캐싱 관련 (2026-08-05 논의)**: "다음" 버튼으로 이미지를 한 장씩 순차 조회하는 화면에서, "마지막 이미지 도달"만 프론트가 로컬로 캐싱해 그 이후 "다음" 클릭 시 API 호출을 생략하는 방식은 **권장하지 않는다** — presigned URL이 1시간마다 만료돼서 어차피 이미지를 보여줄 때마다 백엔드를 거쳐 새로 서명받아야 하고(캐싱해서 아낄 수 있는 API 호출 자체가 없음), 캐싱해두면 그 사이 새로 추가된 이미지를 프론트가 알 방법이 없어진다. **"다음" 버튼은 항상 API를 호출하는 쪽으로 구현할 것.** 이 API 응답엔 매번 최신 `totalCount`(또는 `hasNext`)를 같이 내려서, 프론트가 별도 캐싱 없이 매 응답 기준으로 "다음 버튼 비활성화 여부"를 판단하게 한다.

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
