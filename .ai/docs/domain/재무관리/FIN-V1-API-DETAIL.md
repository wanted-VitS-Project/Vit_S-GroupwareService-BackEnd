# 💰 재무관리 v1 — API 상세 명세

**최종 업데이트**: 2026-08-03 (발주처 미확정 표기 제거 · `tax_invoice_confirm` 제약 정정)
**최종 업데이트**: 2026-08-03 (ERD 확정본 반영 — 미확정 표기 제거 · `managerUserId` 타입 정정)
**최종 업데이트**: 2026-08-01 (신설)
**담당**: 동훈
**목록 문서**: [`FIN-V1-API.md`](FIN-V1-API.md) (**§0-0 ERD↔API 필드 매핑표**) · **스키마**: [`ERD.md`](ERD.md) · **흐름도**: [`FIN-V1-API-FLOW.md`](FIN-V1-API-FLOW.md)
**요구사항**: [`PAY-V1.md`](PAY-V1.md) · [`TAX-V1.md`](TAX-V1.md) · [`STL-V1.md`](STL-V1.md)

> ⛔ **전 엔드포인트 `📝 초안`. 노션 반영 전까지 구현 금지** (`AGENTS.md` §3).
> 모든 응답은 `httpStatus` · `message` · `data` 봉투를 쓴다. Response Parameter 의 들여쓴 항목은 `data` 하위다.
> ✅ 사람 식별자는 신규 ERD 기준 **사번 `String`(`VARCHAR(20)`)** 이다. ⛔ `Long` 아님.
> ✅ 스키마는 [`ERD.md`](ERD.md) 로 확정됐다 (2026-08-03). ⚠️ 단 **ERD Cloud 미반영분**이 있다 → [`ERD.md`](ERD.md) §8

---

# GET `/api/v1/payments` — 입금 목록 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 입금 목록 조회 |
| Method | GET |
| URL | `/api/v1/payments` |
| 인증 필요 여부 | Y |
| 권한 | 재무 열람자 (`page_code='FINANCE'`) |
| 요구사항 | PAY-004 · PAY-005 · PAY-009 |

## Path Parameter
없음

## Request Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `paidFrom` | LocalDate | N | 입금일 시작 |
| `paidTo` | LocalDate | N | 입금일 종료 |
| `matched` | Boolean | N | `true`=매칭 완료 · `false`=미매칭 |
| `projectId` | Long | N | 프로젝트 필터 |
| `page` | int | N | 기본 0 |
| `size` | int | N | 기본 20 |

**정렬은 미매칭 우선 고정**이다 — `project_id IS NULL` 이 상단, 그 다음 입금일 내림차순 (PAY-004).
⛔ 프로젝트 참여자(실무자)는 `FINANCE` 행이 없으면 **403** (PAY-009).

## Request Body
없음

## Request Example
```
GET /api/v1/payments?matched=false&paidFrom=2026-07-01&page=0&size=20
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `content` | List\<Object\> | 입금 목록 |
| `content[].paymentId` | Long | 입금 ID |
| `content[].paidAt` | LocalDate | 입금일 |
| `content[].amount` | BigDecimal | 금액 |
| `content[].bankMemo` | String | 적요 |
| `content[].sourceType` | String | 출처 — `MANUAL`·`CSV` (IMP-007) |
| `content[].bankTxnId` | String | 은행 거래 고유번호 (IMP-005) |
| `content[].projectId` | Long | 매칭된 프로젝트 ID. **미매칭이면 `null`** |
| `content[].projectName` | String | 매칭된 프로젝트명 |
| `content[].blockId` | Long | 연결된 입금확인 블록 ID. 미연결이면 `null` |
| `content[].confirmedBy` | Object | 확정자 (`userId`·`name`) |
| `content[].confirmedAt` | LocalDateTime | 확정 일시 |
| `page` / `size` / `totalElements` / `totalPages` | - | 페이징 정보 |

## Success Example
```
{
  "httpStatus":200,
  "message":"입금 목록 조회 성공",
  "data": {
    "content": [
      {
        "paymentId":301,
        "paidAt":"2026-07-31",
        "amount":50000000,
        "bankMemo":"OO시청 용역대금",
        "sourceType":"CSV",
        "bankTxnId":"20260731-000148",
        "projectId":null,
        "projectName":null,
        "blockId":null,
        "confirmedBy":null,
        "confirmedAt":null
      }
    ],
    "page":0,
    "size":20,
    "totalElements":1,
    "totalPages":1
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 입금 목록 조회 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_ACCESS_DENIED` | 재무 탭 접근 권한 없음 |

---

# GET `/api/v1/payments/{paymentId}` — 입금 상세 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 입금 상세 조회 |
| Method | GET |
| URL | `/api/v1/payments/{paymentId}` |
| 인증 필요 여부 | Y |
| 권한 | 재무 열람자 |
| 요구사항 | PAY-004 · USC-PAY-004 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `paymentId` | Long | Y | 입금 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
GET /api/v1/payments/301
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `paymentId` | Long | 입금 ID |
| `paidAt` | LocalDate | 입금일 |
| `amount` | BigDecimal | 금액 |
| `bankMemo` | String | 적요 |
| `payerName` | String | 입금자명 (`payment.payer_name VARCHAR(200)`) |
| `sourceType` | String | 출처 (`MANUAL`·`CSV`) |
| `bankTxnId` | String | 은행 거래 고유번호 |
| `project` | Object | 매칭된 프로젝트 (`projectId`·`name`). 미매칭이면 `null` |
| `block` | Object | 연결된 입금확인 블록 (`blockId`·`title`·`roundNo`). 미연결이면 `null` |
| `matchedBy` | Object | 프로젝트 매칭자 (`matched_by`) |
| `confirmedBy` | Object | 입금 확정자 (`confirmed_by`) — **매칭자와 다른 컬럼이다** |
| `confirmedAt` | LocalDateTime | 확정 일시 |
| `createdAt` | LocalDateTime | 등록 일시 |

## Success Example
```
{
  "httpStatus":200,
  "message":"입금 상세 조회 성공",
  "data": {
    "paymentId":301,
    "paidAt":"2026-07-31",
    "amount":50000000,
    "bankMemo":"OO시청 용역대금",
    "payerName":"OO시청",
    "sourceType":"CSV",
    "bankTxnId":"20260731-000148",
    "project": { "projectId":12, "name":"OO시 상수도 관리 용역" },
    "block": { "blockId":40, "title":"1차 정산(선급 60%)", "roundNo":1 },
    "matchedBy": { "userId":"E2023011", "name":"박재무" },
    "confirmedBy": { "userId":"E2023011", "name":"박재무" },
    "confirmedAt":"2026-08-01T09:30:00",
    "createdAt":"2026-08-01T09:00:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 입금 상세 조회 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_ACCESS_DENIED` | 재무 탭 접근 권한 없음 |
| 404 | Not Found | `PAYMENT_NOT_FOUND` | 입금이 존재하지 않음 |

---

# POST `/api/v1/payments` — 입금 직접 등록

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 입금 직접 등록 |
| Method | POST |
| URL | `/api/v1/payments` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 (`FINANCE`+`EDITOR`) |
| 요구사항 | PAY-001 · PAY-002 · PAY-003 · INV-02B |

## Path Parameter
없음

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `paidAt` | LocalDate | Y | 입금일 |
| `amount` | BigDecimal | Y | 금액 |
| `bankMemo` | String | N | 적요 |
| `payerName` | String | N | 입금자명 |
| `bankTxnId` | String | N | 은행 거래 고유번호 (있으면 중복 차단에 쓰인다) |

**어느 프로젝트인지 몰라도 등록된다.** `project_id` 가 `NULL` 인 행이 정상이고 경고를 띄우지 않는다 (PAY-003 · INV-04).
`sourceType` 은 시스템이 `MANUAL` 로 기록한다 (IMP-007).
**수집 경로가 죽어도 이 API 는 항상 열려 있다** (INV-02B).

## Request Example
```
{
  "paidAt":"2026-07-31",
  "amount":50000000,
  "bankMemo":"OO시청 용역대금",
  "payerName":"OO시청"
}
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `paymentId` | Long | 생성된 입금 ID |
| `paidAt` | LocalDate | 입금일 |
| `amount` | BigDecimal | 금액 |
| `bankMemo` | String | 적요 |
| `sourceType` | String | `MANUAL` |
| `projectId` | Long | `null` (미매칭) |
| `createdAt` | LocalDateTime | 등록 일시 |

## Success Example
```
{
  "httpStatus":201,
  "message":"입금 등록 성공",
  "data": {
    "paymentId":301,
    "paidAt":"2026-07-31",
    "amount":50000000,
    "bankMemo":"OO시청 용역대금",
    "sourceType":"MANUAL",
    "projectId":null,
    "createdAt":"2026-08-01T09:00:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | - | 입금 등록 성공 |
| 400 | Bad Request | `PAYMENT_PAID_AT_REQUIRED` | 입금일이 입력되지 않음 |
| 400 | Bad Request | `PAYMENT_AMOUNT_REQUIRED` | 금액이 입력되지 않음 |
| 400 | Bad Request | `PAYMENT_AMOUNT_INVALID` | 금액이 0 이하 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무 편집 권한 없음 |
| 409 | Conflict | `PAYMENT_BANK_TXN_DUPLICATED` | 같은 은행 거래 고유번호가 이미 존재함 |

---

# PATCH `/api/v1/payments/{paymentId}` — 입금 수정

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 입금 수정 |
| Method | PATCH |
| URL | `/api/v1/payments/{paymentId}` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 |
| 요구사항 | PAY-007 · USC-PAY-011 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `paymentId` | Long | Y | 입금 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `paidAt` | LocalDate | N | 입금일 |
| `amount` | BigDecimal | N | 금액 |
| `bankMemo` | String | N | 적요 |
| `payerName` | String | N | 입금자명 |

수정 사실은 로그에 남는다 (PAY-007 · USC-PAY-011).

## Request Example
```
{ "amount":52000000, "bankMemo":"OO시청 용역대금(정정)" }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `paymentId` | Long | 입금 ID |
| `paidAt` | LocalDate | 입금일 |
| `amount` | BigDecimal | 금액 |
| `bankMemo` | String | 적요 |
| `updatedAt` | LocalDateTime | 수정 일시 |

## Success Example
```
{
  "httpStatus":200,
  "message":"입금 수정 성공",
  "data": {
    "paymentId":301,
    "paidAt":"2026-07-31",
    "amount":52000000,
    "bankMemo":"OO시청 용역대금(정정)",
    "updatedAt":"2026-08-01T10:00:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 입금 수정 성공 |
| 400 | Bad Request | `PAYMENT_AMOUNT_INVALID` | 금액이 0 이하 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무 편집 권한 없음 |
| 404 | Not Found | `PAYMENT_NOT_FOUND` | 입금이 존재하지 않음 |

---

# DELETE `/api/v1/payments/{paymentId}` — 입금 삭제

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 입금 삭제 |
| Method | DELETE |
| URL | `/api/v1/payments/{paymentId}` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 |
| 요구사항 | PAY-008 · USC-PAY-013 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `paymentId` | Long | Y | 삭제할 입금 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
DELETE /api/v1/payments/301
```

**블록에 연결된 입금은 삭제할 수 없다.** 먼저 블록 연결 해제 API(`MTC-007`)를 호출해야 한다 (PAY-008).

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | `null` |

## Success Example
```
{ "httpStatus":200, "message":"입금 삭제 성공", "data":null }
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 입금 삭제 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무 편집 권한 없음 |
| 404 | Not Found | `PAYMENT_NOT_FOUND` | 입금이 존재하지 않음 |
| 409 | Conflict | `PAYMENT_BLOCK_LINKED` | 블록에 연결된 입금은 삭제할 수 없음 |

---

# POST `/api/v1/payments/{paymentId}/confirm` — 입금 확정

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 입금 확정 |
| Method | POST |
| URL | `/api/v1/payments/{paymentId}/confirm` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 |
| 요구사항 | PAY-006 · INV-01 · USC-PAY-007~009 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `paymentId` | Long | Y | 입금 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
POST /api/v1/payments/301/confirm
```

⛔ **실무자는 입금을 확정할 수 없다** (INV-01). 프로젝트 참여자 요청은 403 — 스텝 `EDITOR` 여도 마찬가지다.
확정자는 **`confirmed_by`·`confirmed_at`** 에 사번으로 기록된다 (PAY-006). ⛔ `matched_by` 가 아니다 — 매칭과 확정은 별개 사건이다.

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `paymentId` | Long | 입금 ID |
| `confirmedBy` | Object | 확정자 (`userId`·`name`) |
| `confirmedAt` | LocalDateTime | 확정 일시 |

## Success Example
```
{
  "httpStatus":200,
  "message":"입금 확정 성공",
  "data": {
    "paymentId":301,
    "confirmedBy": { "userId":"E2023011", "name":"박재무" },
    "confirmedAt":"2026-08-01T09:30:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 입금 확정 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무만 확정할 수 있음 |
| 404 | Not Found | `PAYMENT_NOT_FOUND` | 입금이 존재하지 않음 |
| 409 | Conflict | `PAYMENT_ALREADY_CONFIRMED` | 이미 확정된 입금 |

---

# POST `/api/v1/payments/import/csv` — 거래내역 CSV 수집

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 거래내역 CSV 수집 |
| Method | POST |
| URL | `/api/v1/payments/import/csv` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 |
| Content-Type | `multipart/form-data` |
| 요구사항 | IMP-001~008 · INV-02 · INV-08 |

## Path Parameter
없음

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `file` | MultipartFile | Y | 은행 거래내역 CSV |
| `mapping.paidAt` | String | Y | 입금일에 해당하는 CSV 헤더명 |
| `mapping.amount` | String | Y | 금액에 해당하는 CSV 헤더명 |
| `mapping.bankMemo` | String | Y | 적요에 해당하는 CSV 헤더명 |
| `mapping.bankTxnId` | String | Y | 은행 거래 고유번호에 해당하는 CSV 헤더명 |
| `mapping.payerName` | String | N | 입금자명에 해당하는 CSV 헤더명 |

**은행마다 헤더가 달라 고정 파서를 쓰지 않는다.** 매핑 없이 업로드하면 400 (IMP-002).
출금 행은 건너뛴다 (IMP-003). 고유번호 중복 행도 **에러가 아니라 건너뜀**이다 (IMP-005 · INV-08).
고유번호가 없는 행은 실패로 집계된다 (IMP-006). **전체 롤백하지 않는다** (IMP-004).
수집된 입금은 전부 **미매칭(`project_id = NULL`)** 으로 들어온다 (IMP-008 · INV-02).

## Request Example
```
POST /api/v1/payments/import/csv
Content-Type: multipart/form-data

file: 202607_거래내역.csv
mapping.paidAt: 거래일자
mapping.amount: 입금액
mapping.bankMemo: 적요
mapping.bankTxnId: 거래고유번호
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `totalRowCount` | int | CSV 전체 행 수 |
| `successCount` | int | 저장된 행 수 |
| `skippedCount` | int | 건너뛴 행 수 (출금 · 고유번호 중복) |
| `failedCount` | int | 실패한 행 수 |
| `skippedRows` | List\<Object\> | 건너뛴 행 (`rowNumber`·`reason`) |
| `failedRows` | List\<Object\> | 실패한 행 (`rowNumber`·`reason`) |
| `createdPaymentIds` | List\<Long\> | 생성된 입금 ID 목록 |

## Success Example
```
{
  "httpStatus":200,
  "message":"CSV 수집 완료",
  "data": {
    "totalRowCount":120,
    "successCount":95,
    "skippedCount":22,
    "failedCount":3,
    "skippedRows": [
      { "rowNumber":4, "reason":"WITHDRAWAL_ROW" },
      { "rowNumber":18, "reason":"BANK_TXN_DUPLICATED" }
    ],
    "failedRows": [
      { "rowNumber":51, "reason":"BANK_TXN_ID_MISSING" },
      { "rowNumber":77, "reason":"AMOUNT_PARSE_FAILED" }
    ],
    "createdPaymentIds": [301,302,303]
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | CSV 수집 완료 (부분 실패 포함) |
| 400 | Bad Request | `CSV_FILE_REQUIRED` | 파일이 첨부되지 않음 |
| 400 | Bad Request | `CSV_COLUMN_MAPPING_REQUIRED` | 필수 컬럼 매핑이 지정되지 않음 |
| 400 | Bad Request | `CSV_COLUMN_NOT_FOUND` | 매핑한 헤더가 CSV 에 없음 |
| 400 | Bad Request | `CSV_PARSE_FAILED` | 파일 전체를 읽을 수 없음 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무 편집 권한 없음 |

---

# GET `/api/v1/payments/{paymentId}/match-candidates` — 입금 매칭 후보 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 입금 매칭 후보 조회 |
| Method | GET |
| URL | `/api/v1/payments/{paymentId}/match-candidates` |
| 인증 필요 여부 | Y |
| 권한 | 재무 열람자 |
| 요구사항 | MTC-005 · MTC-006 · INV-02 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `paymentId` | Long | Y | 입금 ID |

## Request Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `keyword` | String | N | 후보 밖 프로젝트를 직접 검색할 때 쓰는 과업명·발주처 검색어 |
| `size` | int | N | 기본 10 |

**정렬 기준**: 발주처명 ↔ 적요 일치 → 금액 일치 → 입금일 근접 (MTC-005).
⛔ **자동으로 확정하지 않는다.** 이 API 는 추천만 하고 확정은 사람이 매칭 API 를 호출한다 (MTC-006 · INV-02).
⚠️ 유사도 계산식은 미확정이다 (`PAY-V1.md` §5-3).

## Request Body
없음

## Request Example
```
GET /api/v1/payments/301/match-candidates?size=10
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `candidates` | List\<Object\> | 후보 목록 (추천 순) |
| `candidates[].projectId` | Long | 프로젝트 ID |
| `candidates[].name` | String | 과업명 |
| `candidates[].clientName` | String | 발주처 (`project.client_name VARCHAR(200)`) |
| `candidates[].contractAmount` | BigDecimal | 계약금액 |
| `candidates[].receivedAmount` | BigDecimal | 이미 매칭된 입금 합계 |
| `candidates[].matchReason` | String | 추천 근거 (`CLIENT_NAME`·`AMOUNT`·`PAID_DATE`) |

## Success Example
```
{
  "httpStatus":200,
  "message":"매칭 후보 조회 성공",
  "data": {
    "candidates": [
      {
        "projectId":12,
        "name":"OO시 상수도 관리 용역",
        "clientName":"OO시청",
        "contractAmount":120000000,
        "receivedAmount":0,
        "matchReason":"CLIENT_NAME"
      }
    ]
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 매칭 후보 조회 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_ACCESS_DENIED` | 재무 탭 접근 권한 없음 |
| 404 | Not Found | `PAYMENT_NOT_FOUND` | 입금이 존재하지 않음 |

---

# PATCH `/api/v1/payments/{paymentId}/project` — 입금 프로젝트 매칭

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 입금 프로젝트 매칭 (매칭 ①) |
| Method | PATCH |
| URL | `/api/v1/payments/{paymentId}/project` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 |
| 요구사항 | MTC-001 · MTC-006 · INV-03 · INV-04 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `paymentId` | Long | Y | 입금 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 매칭할 프로젝트 ID |

**이것이 필수 단계다.** 입금확인 블록이 하나도 없어도 성공한다 (MTC-001 · INV-03).
**블록 없이 매칭만 된 상태가 정상이다** (INV-04).

## Request Example
```
{ "projectId":12 }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `paymentId` | Long | 입금 ID |
| `projectId` | Long | 매칭된 프로젝트 ID |
| `projectName` | String | 프로젝트명 |
| `matchedBy` | Object | 매칭자 |
| `matchedAt` | LocalDateTime | 매칭 일시 (`payment.matched_at DATETIME`) |

## Success Example
```
{
  "httpStatus":200,
  "message":"프로젝트 매칭 성공",
  "data": {
    "paymentId":301,
    "projectId":12,
    "projectName":"OO시 상수도 관리 용역",
    "matchedBy": { "userId":"E2023011", "name":"박재무" },
    "matchedAt":"2026-08-01T09:40:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 프로젝트 매칭 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무만 매칭할 수 있음 |
| 404 | Not Found | `PAYMENT_NOT_FOUND` | 입금이 존재하지 않음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |
| 409 | Conflict | `PAYMENT_ALREADY_MATCHED` | 이미 다른 프로젝트에 매칭됨 — 먼저 해제해야 함 |

---

# DELETE `/api/v1/payments/{paymentId}/project` — 입금 프로젝트 매칭 해제

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 입금 프로젝트 매칭 해제 |
| Method | DELETE |
| URL | `/api/v1/payments/{paymentId}/project` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 |
| 요구사항 | MTC-005B · MTC-005C |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `paymentId` | Long | Y | 입금 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
DELETE /api/v1/payments/301/project
```

해제하면 `project_id` 가 `NULL` 로 돌아간다 (MTC-005B).
**블록에 연결된 입금은 매칭을 해제할 수 없다.** 먼저 블록 연결을 해제해야 한다 (MTC-005C).

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `paymentId` | Long | 입금 ID |
| `projectId` | Long | `null` |

## Success Example
```
{
  "httpStatus":200,
  "message":"프로젝트 매칭 해제 성공",
  "data": { "paymentId":301, "projectId":null }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 프로젝트 매칭 해제 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무만 해제할 수 있음 |
| 404 | Not Found | `PAYMENT_NOT_FOUND` | 입금이 존재하지 않음 |
| 409 | Conflict | `PAYMENT_BLOCK_LINKED` | 블록에 연결된 입금은 매칭을 해제할 수 없음 |

---

# PATCH `/api/v1/payments/{paymentId}/block` — 입금확인 블록 연결

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 입금확인 블록 연결 (매칭 ②) |
| Method | PATCH |
| URL | `/api/v1/payments/{paymentId}/block` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 |
| 요구사항 | MTC-002 · MTC-003 · MTC-004 · MTC-009 · MTC-010 · INV-07B |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `paymentId` | Long | Y | 입금 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `blockId` | Long | Y | 연결할 입금확인 블록 ID |

**선택 단계다.** 다만 아래 순서·제약이 강제된다.

| 규칙 | 위반 시 |
| --- | --- |
| 프로젝트 매칭이 **먼저** 되어 있어야 한다 (MTC-003) | 400 |
| 블록이 **매칭된 그 프로젝트**에 속해야 한다 (MTC-004) | 400 |
| 1블록 **N입금** 허용 (MTC-009) | — |
| 1입금 **1블록** (MTC-010 · INV-07B) | 409 |

✅ 저장 위치는 **`payment.block_id`** 로 확정됐다 ([`ERD.md`](ERD.md) §2). ⛔ UNIQUE 를 걸지 않아 1블록 N입금이 성립한다.

## Request Example
```
{ "blockId":40 }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `paymentId` | Long | 입금 ID |
| `blockId` | Long | 연결된 블록 ID |
| `blockTitle` | String | 블록 제목(=회차명) |
| `roundNo` | int | 회차 번호 |
| `linkedBy` | Object | 연결자 |
| `linkedAt` | LocalDateTime | 연결 일시 |

## Success Example
```
{
  "httpStatus":200,
  "message":"입금확인 블록 연결 성공",
  "data": {
    "paymentId":301,
    "blockId":40,
    "blockTitle":"1차 정산(선급 60%)",
    "roundNo":1,
    "linkedBy": { "userId":"E2023011", "name":"박재무" },
    "linkedAt":"2026-08-01T09:50:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 블록 연결 성공 |
| 400 | Bad Request | `PAYMENT_NOT_MATCHED` | 프로젝트 매칭이 안 된 입금 |
| 400 | Bad Request | `PAYMENT_BLOCK_PROJECT_MISMATCH` | 블록이 매칭된 프로젝트에 속하지 않음 |
| 400 | Bad Request | `BLOCK_TYPE_NOT_PAYMENT_CONFIRM` | 입금확인 블록이 아님 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무만 연결할 수 있음 |
| 404 | Not Found | `PAYMENT_NOT_FOUND` | 입금이 존재하지 않음 |
| 404 | Not Found | `BLOCK_NOT_FOUND` | 블록이 존재하지 않음 |
| 409 | Conflict | `PAYMENT_ALREADY_LINKED` | 이미 다른 블록에 연결된 입금 |

---

# DELETE `/api/v1/payments/{paymentId}/block` — 입금확인 블록 연결 해제

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 입금확인 블록 연결 해제 |
| Method | DELETE |
| URL | `/api/v1/payments/{paymentId}/block` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 |
| 요구사항 | MTC-007 · MTC-008 · INV-06 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `paymentId` | Long | Y | 입금 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
DELETE /api/v1/payments/301/block
```

⛔ **재무만 해제할 수 있다.** 실무자는 요청 경로 자체가 없다 (MTC-007 · INV-06).
**연결된 입금이 0건이 되어야** 블록·스텝을 다시 삭제할 수 있다 (MTC-008).

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `paymentId` | Long | 입금 ID |
| `blockId` | Long | `null` |
| `remainingLinkedCount` | int | 해당 블록에 남은 연결 입금 수 — **0 이면 삭제 잠금이 풀린다** |

## Success Example
```
{
  "httpStatus":200,
  "message":"블록 연결 해제 성공",
  "data": { "paymentId":301, "blockId":null, "remainingLinkedCount":2 }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 블록 연결 해제 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무만 해제할 수 있음 |
| 404 | Not Found | `PAYMENT_NOT_FOUND` | 입금이 존재하지 않음 |
| 409 | Conflict | `PAYMENT_NOT_LINKED` | 블록에 연결되지 않은 입금 |

---

# POST `/api/v1/steps/{stepId}/payment-confirm` — 정산 회차 생성

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 정산 회차 생성 (입금확인 블록) |
| Method | POST |
| URL | `/api/v1/steps/{stepId}/payment-confirm` |
| 인증 필요 여부 | Y |
| 권한 | 스텝 EDITOR |
| 요구사항 | PCB-001 · PCB-001B · PCB-002 · PCB-002B · INV-07 · INV-07C |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `stepId` | Long | Y | 정산 스텝 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `title` | String | Y | 블록 제목 = **회차명** (`1차 정산(선급 60%)`) |
| `roundNo` | int | N | 회차 번호. 미지정 시 프로젝트 내 `max+1` 자동 부여 |
| `plannedDate` | LocalDate | N | 입금 예정일 |
| `plannedAmount` | BigDecimal | N | 예정금액 |

**블록 1개 = 회차 1개다.** 별도 회차 테이블이 없다 (PCB-001 · INV-07).
**한 스텝에 입금확인 블록은 하나만.** 두 번째 생성은 409 (PCB-001B · INV-07C).
회차 예정금액의 합이 계약금액을 넘으면 400 (PCB-002D).
✅ `round_no`·`planned_date`·`planned_amount` 는 `block_payment_confirm` 에 확정됐다 ([`ERD.md`](ERD.md) §4-1).

## Request Example
```
{
  "title":"1차 정산(선급 60%)",
  "plannedDate":"2026-08-31",
  "plannedAmount":72000000
}
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `blockId` | Long | 생성된 블록 ID |
| `stepId` | Long | 소속 스텝 ID |
| `projectId` | Long | 소속 프로젝트 ID |
| `type` | String | `PAYMENT_CONFIRM` |
| `title` | String | 회차명 |
| `roundNo` | int | 회차 번호 |
| `plannedDate` | LocalDate | 입금 예정일 |
| `plannedAmount` | BigDecimal | 예정금액 |
| `createdAt` | LocalDateTime | 생성 일시 |

## Success Example
```
{
  "httpStatus":201,
  "message":"정산 회차 생성 성공",
  "data": {
    "blockId":40,
    "stepId":10,
    "projectId":12,
    "type":"PAYMENT_CONFIRM",
    "title":"1차 정산(선급 60%)",
    "roundNo":1,
    "plannedDate":"2026-08-31",
    "plannedAmount":72000000,
    "createdAt":"2026-08-01T15:00:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 201 | Created | - | 정산 회차 생성 성공 |
| 400 | Bad Request | `ROUND_TITLE_REQUIRED` | 회차명이 입력되지 않음 |
| 400 | Bad Request | `PLANNED_AMOUNT_EXCEEDS_CONTRACT` | 회차 예정금액 합이 계약금액을 초과 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `STEP_EDIT_DENIED` | 스텝 편집 권한 없음 |
| 404 | Not Found | `STEP_NOT_FOUND` | 스텝이 존재하지 않음 |
| 409 | Conflict | `PAYMENT_CONFIRM_BLOCK_DUPLICATED` | 스텝에 이미 입금확인 블록이 있음 |
| 409 | Conflict | `ROUND_NO_DUPLICATED` | 프로젝트 안에 같은 회차 번호가 존재 |

---

# PATCH `/api/v1/blocks/{blockId}/payment-confirm` — 회차 정보 수정

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 회차 정보 수정 |
| Method | PATCH |
| URL | `/api/v1/blocks/{blockId}/payment-confirm` |
| 인증 필요 여부 | Y |
| 권한 | 스텝 EDITOR |
| 요구사항 | PCB-002 · PCB-002B · PCB-002C · PCB-002D |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `blockId` | Long | Y | 입금확인 블록 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `title` | String | N | 회차명 |
| `roundNo` | int | N | 회차 번호 (프로젝트 단위 `UNIQUE`) |
| `plannedDate` | LocalDate | N | 입금 예정일 |
| `plannedAmount` | BigDecimal | N | 예정금액 |

예정일·예정금액은 **둘 다 선택 입력**이다 — 계약 조건을 아는 건 실무자다 (PCB-002C).
`Σ 예정금액 > contract_amount` 면 400. **모자란 것(미계획)은 정상**이다 (PCB-002D).
⛔ 이 API 로 연결된 입금을 바꿀 수 없다. 입금 쪽은 **재무만** 건드린다 (PCB-003).

## Request Example
```
{ "plannedDate":"2026-09-15", "plannedAmount":60000000 }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `blockId` | Long | 블록 ID |
| `title` | String | 회차명 |
| `roundNo` | int | 회차 번호 |
| `plannedDate` | LocalDate | 입금 예정일 |
| `plannedAmount` | BigDecimal | 예정금액 |
| `updatedAt` | LocalDateTime | 수정 일시 |

## Success Example
```
{
  "httpStatus":200,
  "message":"회차 정보 수정 성공",
  "data": {
    "blockId":40,
    "title":"1차 정산(선급 60%)",
    "roundNo":1,
    "plannedDate":"2026-09-15",
    "plannedAmount":60000000,
    "updatedAt":"2026-08-01T15:30:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 회차 정보 수정 성공 |
| 400 | Bad Request | `PLANNED_AMOUNT_EXCEEDS_CONTRACT` | 회차 예정금액 합이 계약금액을 초과 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `STEP_EDIT_DENIED` | 스텝 편집 권한 없음 |
| 404 | Not Found | `BLOCK_NOT_FOUND` | 블록이 존재하지 않음 |
| 409 | Conflict | `ROUND_NO_DUPLICATED` | 프로젝트 안에 같은 회차 번호가 존재 |

---

# GET `/api/v1/blocks/{blockId}/payment-confirm` — 회차 상세 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 회차 상세 조회 (입금확인 블록) |
| Method | GET |
| URL | `/api/v1/blocks/{blockId}/payment-confirm` |
| 인증 필요 여부 | Y |
| 권한 | 스텝 접근 권한 (**읽기 전용**) |
| 요구사항 | PCB-003 · PCB-004 · PCB-004B · INV-07B |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `blockId` | Long | Y | 입금확인 블록 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
GET /api/v1/blocks/40/payment-confirm
```

⛔ **프로젝트 참여자는 읽기만 한다.** 스텝 `EDITOR` 여도 확정·연결 요청은 403 (PCB-003).
미연결이면 `status` 가 `WAITING` 이다. **빈 화면이 아니다** (PCB-004).

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `blockId` | Long | 블록 ID |
| `title` | String | 회차명 |
| `roundNo` | int | 회차 번호 |
| `plannedDate` | LocalDate | 입금 예정일 |
| `plannedAmount` | BigDecimal | 예정금액 |
| `status` | String | `WAITING`(미연결) · `RECEIVED`(연결됨) |
| `receivedTotal` | BigDecimal | **연결된 입금 합계** (PCB-004B) |
| `payments` | List\<Object\> | 연결된 입금 건별 목록 (PCB-004) |
| `payments[].paymentId` | Long | 입금 ID |
| `payments[].paidAt` | LocalDate | 입금일 |
| `payments[].amount` | BigDecimal | 금액 |
| `payments[].confirmedBy` | Object | 확정자 |

## Success Example
```
{
  "httpStatus":200,
  "message":"회차 상세 조회 성공",
  "data": {
    "blockId":40,
    "title":"1차 정산(선급 60%)",
    "roundNo":1,
    "plannedDate":"2026-08-31",
    "plannedAmount":72000000,
    "status":"RECEIVED",
    "receivedTotal":72000000,
    "payments": [
      { "paymentId":301, "paidAt":"2026-07-31", "amount":50000000, "confirmedBy": { "userId":"E2023011", "name":"박재무" } },
      { "paymentId":305, "paidAt":"2026-08-05", "amount":22000000, "confirmedBy": { "userId":"E2023011", "name":"박재무" } }
    ]
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 회차 상세 조회 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `STEP_ACCESS_DENIED` | 스텝 접근 권한 없음 |
| 404 | Not Found | `BLOCK_NOT_FOUND` | 블록이 존재하지 않음 |

---

# GET `/api/v1/tax-invoices` — 세금계산서 목록 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 세금계산서 목록 조회 |
| Method | GET |
| URL | `/api/v1/tax-invoices` |
| 인증 필요 여부 | Y |
| 권한 | 재무 열람자 |
| 요구사항 | TAX-009 · TAX-010 · USC-TXQ-001·002·003 |

## Path Parameter
없음

## Request Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `issuedFrom` | LocalDate | N | 발행일 시작 |
| `issuedTo` | LocalDate | N | 발행일 종료 |
| `projectId` | Long | N | 프로젝트 필터 |
| `matched` | Boolean | N | `true`=매칭 완료 · `false`=미매칭 |
| `page` | int | N | 기본 0 |
| `size` | int | N | 기본 20 |

**미매칭 건이 상단에 온다** (TAX-009). 실무자는 `FINANCE` 행이 없으면 403 (TAX-010).
✅ `tax_invoice` 전 컬럼이 확정됐다 ([`ERD.md`](ERD.md) §3).

## Request Body
없음

## Request Example
```
GET /api/v1/tax-invoices?matched=false&issuedFrom=2026-07-01&page=0&size=20
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `content` | List\<Object\> | 계산서 목록 |
| `content[].taxInvoiceId` | Long | 계산서 ID |
| `content[].approvalNo` | String | 승인번호 (중복 판정 키) |
| `content[].issuedAt` | LocalDate | 발행일 |
| `content[].supplyAmount` | BigDecimal | 공급가액 |
| `content[].taxAmount` | BigDecimal | 세액 |
| `content[].totalAmount` | BigDecimal | 합계 |
| `content[].buyerName` | String | 공급받는자 |
| `content[].sourceType` | String | 출처 — `CSV`·`HOMETAX_API` (TAX-007) |
| `content[].projectId` | Long | 매칭된 프로젝트 ID. 미매칭이면 `null` |
| `content[].projectName` | String | 프로젝트명 |
| `content[].blockId` | Long | 연결된 조회 블록 ID. 미연결이면 `null` |
| `page` / `size` / `totalElements` / `totalPages` | - | 페이징 정보 |

## Success Example
```
{
  "httpStatus":200,
  "message":"세금계산서 목록 조회 성공",
  "data": {
    "content": [
      {
        "taxInvoiceId":201,
        "approvalNo":"20260731-41000000-11112222",
        "issuedAt":"2026-07-31",
        "supplyAmount":65454545,
        "taxAmount":6545455,
        "totalAmount":72000000,
        "buyerName":"OO시청",
        "sourceType":"HOMETAX_API",
        "projectId":null,
        "projectName":null,
        "blockId":null
      }
    ],
    "page":0,
    "size":20,
    "totalElements":1,
    "totalPages":1
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 세금계산서 목록 조회 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_ACCESS_DENIED` | 재무 탭 접근 권한 없음 |

---

# GET `/api/v1/tax-invoices/{taxInvoiceId}` — 세금계산서 상세 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 세금계산서 상세 조회 |
| Method | GET |
| URL | `/api/v1/tax-invoices/{taxInvoiceId}` |
| 인증 필요 여부 | Y |
| 권한 | 재무 열람자 |
| 요구사항 | USC-TXQ-001 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `taxInvoiceId` | Long | Y | 계산서 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
GET /api/v1/tax-invoices/201
```

⛔ **발행 상태 필드가 없다.** 행이 있으면 발행된 것이다 (`TAX-V1.md` §5-3).

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `taxInvoiceId` | Long | 계산서 ID |
| `approvalNo` | String | 승인번호 |
| `issuedAt` | LocalDate | 발행일 |
| `supplyAmount` | BigDecimal | 공급가액 |
| `taxAmount` | BigDecimal | 세액 |
| `totalAmount` | BigDecimal | 합계 |
| `buyerName` | String | 공급받는자 |
| `buyerBizNo` | String | 공급받는자 사업자번호 |
| `sourceType` | String | 출처 (`CSV`·`HOMETAX_API`) |
| `project` | Object | 매칭된 프로젝트 (`projectId`·`name`) |
| `block` | Object | 연결된 블록 (`blockId`·`title`) |
| `linkedBy` | Object | 연결자 |
| `collectedAt` | LocalDateTime | 수집 일시 |

## Success Example
```
{
  "httpStatus":200,
  "message":"세금계산서 상세 조회 성공",
  "data": {
    "taxInvoiceId":201,
    "approvalNo":"20260731-41000000-11112222",
    "issuedAt":"2026-07-31",
    "supplyAmount":65454545,
    "taxAmount":6545455,
    "totalAmount":72000000,
    "buyerName":"OO시청",
    "buyerBizNo":"123-45-67890",
    "sourceType":"HOMETAX_API",
    "project": { "projectId":12, "name":"OO시 상수도 관리 용역" },
    "block": { "blockId":41, "title":"1차 계산서" },
    "linkedBy": { "userId":"E2023011", "name":"박재무" },
    "collectedAt":"2026-08-01T08:00:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 세금계산서 상세 조회 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_ACCESS_DENIED` | 재무 탭 접근 권한 없음 |
| 404 | Not Found | `TAX_INVOICE_NOT_FOUND` | 계산서가 존재하지 않음 |

---

# POST `/api/v1/tax-invoices/import/csv` — 홈택스 CSV 수집

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 홈택스 CSV 수집 |
| Method | POST |
| URL | `/api/v1/tax-invoices/import/csv` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 |
| Content-Type | `multipart/form-data` |
| 요구사항 | TAX-001~003 · TAX-006~008 · INV-04 · INV-09 |

## Path Parameter
없음

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `file` | MultipartFile | Y | 홈택스에서 내려받은 CSV |
| `mapping.issuedAt` | String | Y | 발행일 헤더명 |
| `mapping.supplyAmount` | String | Y | 공급가액 헤더명 |
| `mapping.taxAmount` | String | Y | 세액 헤더명 |
| `mapping.totalAmount` | String | Y | 합계 헤더명 |
| `mapping.buyerName` | String | Y | 공급받는자 헤더명 |
| `mapping.approvalNo` | String | Y | 승인번호 헤더명 (중복 판정 키) |
| `mapping.supplierBizNo` | String | Y | 공급자 사업자번호 헤더명 — **매출분 판정용** |

⛔ **매출분만 수집한다.** 공급자가 우리 사업자번호가 아닌 행은 건너뛴다 (TAX-002B · INV-09).
승인번호 중복 행은 **에러가 아니라 건너뜀** (TAX-006 · INV-04), 승인번호 없는 행은 실패 (TAX-006B).
부분 실패해도 나머지는 저장된다 — **전체 롤백하지 않는다** (TAX-003).
수집된 계산서는 전부 **미매칭(`project_id = NULL`)** 이다 (TAX-008 · INV-05).
⚠️ **우리 사업자번호 보관 위치가 미정**이다 (`TAX-V1.md` §5-7B).

## Request Example
```
POST /api/v1/tax-invoices/import/csv
Content-Type: multipart/form-data

file: 202607_전자세금계산서.csv
mapping.issuedAt: 작성일자
mapping.supplyAmount: 공급가액
mapping.taxAmount: 세액
mapping.totalAmount: 합계금액
mapping.buyerName: 공급받는자 상호
mapping.approvalNo: 승인번호
mapping.supplierBizNo: 공급자 사업자등록번호
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `totalRowCount` | int | CSV 전체 행 수 |
| `successCount` | int | 저장된 행 수 |
| `skippedCount` | int | 건너뛴 행 수 (매입분 · 승인번호 중복) |
| `failedCount` | int | 실패한 행 수 |
| `skippedRows` | List\<Object\> | 건너뛴 행 (`rowNumber`·`reason`) |
| `failedRows` | List\<Object\> | 실패한 행 (`rowNumber`·`reason`) |
| `createdTaxInvoiceIds` | List\<Long\> | 생성된 계산서 ID 목록 |

## Success Example
```
{
  "httpStatus":200,
  "message":"세금계산서 CSV 수집 완료",
  "data": {
    "totalRowCount":80,
    "successCount":61,
    "skippedCount":17,
    "failedCount":2,
    "skippedRows": [
      { "rowNumber":6, "reason":"PURCHASE_INVOICE" },
      { "rowNumber":22, "reason":"APPROVAL_NO_DUPLICATED" }
    ],
    "failedRows": [
      { "rowNumber":40, "reason":"APPROVAL_NO_MISSING" }
    ],
    "createdTaxInvoiceIds": [201,202,203]
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | CSV 수집 완료 (부분 실패 포함) |
| 400 | Bad Request | `CSV_FILE_REQUIRED` | 파일이 첨부되지 않음 |
| 400 | Bad Request | `CSV_COLUMN_MAPPING_REQUIRED` | 필수 컬럼 매핑이 지정되지 않음 |
| 400 | Bad Request | `CSV_COLUMN_NOT_FOUND` | 매핑한 헤더가 CSV 에 없음 |
| 400 | Bad Request | `CSV_PARSE_FAILED` | 파일 전체를 읽을 수 없음 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무 편집 권한 없음 |

---

# POST `/api/v1/tax-invoices/import/hometax` — 홈택스 API 수집

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 홈택스 API 수집 |
| Method | POST |
| URL | `/api/v1/tax-invoices/import/hometax` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 |
| 요구사항 | TAX-004 · TAX-005 · INV-03 |

## Path Parameter
없음

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `issuedFrom` | LocalDate | Y | 조회 시작일 |
| `issuedTo` | LocalDate | Y | 조회 종료일 |

수집된 행은 CSV 경로와 **같은 `tax_invoice` 구조**로 저장된다 (TAX-004).
**연동이 실패해도 CSV 경로는 살아 있다** — API 오류가 화면을 막지 않고 실패 사실이 로그에 남는다 (TAX-005 · INV-03).
🚨 **홈택스 API 접근 방식(국세청 오픈API / 민간 대행 · 인증 방식)이 미정**이다 (`TAX-V1.md` §5-6).
자격정보는 **키 이름만** 문서에 쓴다 (PUBLIC 레포).

## Request Example
```
{ "issuedFrom":"2026-07-01", "issuedTo":"2026-07-31" }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `fetchedCount` | int | 홈택스에서 받아온 건수 |
| `successCount` | int | 저장된 건수 |
| `skippedCount` | int | 건너뛴 건수 (매입분 · 승인번호 중복) |
| `failedCount` | int | 실패한 건수 |
| `createdTaxInvoiceIds` | List\<Long\> | 생성된 계산서 ID 목록 |

## Success Example
```
{
  "httpStatus":200,
  "message":"홈택스 수집 완료",
  "data": {
    "fetchedCount":40,
    "successCount":31,
    "skippedCount":9,
    "failedCount":0,
    "createdTaxInvoiceIds": [204,205,206]
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 홈택스 수집 완료 |
| 400 | Bad Request | `DATE_RANGE_REQUIRED` | 조회 기간이 입력되지 않음 |
| 400 | Bad Request | `DATE_RANGE_INVALID` | 시작일이 종료일보다 늦음 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무 편집 권한 없음 |
| 502 | Bad Gateway | `HOMETAX_API_FAILED` | 홈택스 연동 실패 — CSV 경로는 계속 사용 가능 |

---

# DELETE `/api/v1/tax-invoices/{taxInvoiceId}` — 세금계산서 제거

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 세금계산서 제거 (잘못 수집분) |
| Method | DELETE |
| URL | `/api/v1/tax-invoices/{taxInvoiceId}` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 |
| 요구사항 | TAX-007B · INV-10 · USC-TXQ-006·007 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `taxInvoiceId` | Long | Y | 제거할 계산서 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
DELETE /api/v1/tax-invoices/201
```

soft delete 다. **블록에 연결돼 있으면 409** — 먼저 블록 연결을 해제해야 한다 (TAX-007B).
⛔ **수정 API 는 없다.** 고칠 일이 생기면 홈택스에서 고치고 다시 수집한다 (INV-10).

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | `null` |

## Success Example
```
{ "httpStatus":200, "message":"세금계산서 제거 성공", "data":null }
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 세금계산서 제거 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무 편집 권한 없음 |
| 404 | Not Found | `TAX_INVOICE_NOT_FOUND` | 계산서가 존재하지 않음 |
| 409 | Conflict | `TAX_INVOICE_BLOCK_LINKED` | 블록에 연결된 계산서는 제거할 수 없음 |

---

# GET `/api/v1/tax-invoices/{taxInvoiceId}/match-candidates` — 계산서 매칭 후보 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 계산서 매칭 후보 조회 |
| Method | GET |
| URL | `/api/v1/tax-invoices/{taxInvoiceId}/match-candidates` |
| 인증 필요 여부 | Y |
| 권한 | 재무 열람자 |
| 요구사항 | TXM-002 · TXM-003 · TXM-004 · INV-02 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `taxInvoiceId` | Long | Y | 계산서 ID |

## Request Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `keyword` | String | N | 후보 밖 프로젝트를 직접 검색할 때 쓰는 검색어 (TXM-004) |
| `size` | int | N | 기본 10 |

**정렬 기준**: 계산서의 **공급받는자**(`tax_invoice.buyer_name`) ↔ 프로젝트 **발주처**(`project.client_name`) 일치 → 발행일 근접 → 금액 (TXM-002).
⛔ **자동 확정 경로가 없다.** 한 발주처에 프로젝트가 여러 개일 수 있다 (TXM-003 · INV-02).
✅ 발주처는 `project.client_name VARCHAR(200)` 로 확정됐다 ([`ERD.md`](ERD.md) §5 · `TAX-V1.md` §5-4).

## Request Body
없음

## Request Example
```
GET /api/v1/tax-invoices/201/match-candidates?size=10
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `candidates` | List\<Object\> | 후보 목록 (추천 순) |
| `candidates[].projectId` | Long | 프로젝트 ID |
| `candidates[].name` | String | 과업명 |
| `candidates[].clientName` | String | 발주처 (`project.client_name VARCHAR(200)`) |
| `candidates[].contractAmount` | BigDecimal | 계약금액 |
| `candidates[].matchReason` | String | 추천 근거 (`CLIENT_NAME`·`ISSUED_DATE`·`AMOUNT`) |

## Success Example
```
{
  "httpStatus":200,
  "message":"매칭 후보 조회 성공",
  "data": {
    "candidates": [
      {
        "projectId":12,
        "name":"OO시 상수도 관리 용역",
        "clientName":"OO시청",
        "contractAmount":120000000,
        "matchReason":"CLIENT_NAME"
      }
    ]
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 매칭 후보 조회 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_ACCESS_DENIED` | 재무 탭 접근 권한 없음 |
| 404 | Not Found | `TAX_INVOICE_NOT_FOUND` | 계산서가 존재하지 않음 |

---

# PATCH `/api/v1/tax-invoices/{taxInvoiceId}/project` — 계산서 프로젝트 매칭

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 계산서 프로젝트 매칭 |
| Method | PATCH |
| URL | `/api/v1/tax-invoices/{taxInvoiceId}/project` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 |
| 요구사항 | TXM-001 · TXM-003 · TXM-004 · INV-05 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `taxInvoiceId` | Long | Y | 계산서 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 매칭할 프로젝트 ID |

**후보 목록 밖의 프로젝트도 지정할 수 있다** (TXM-004). 확정 버튼은 사람이 누른다 (TXM-003).

## Request Example
```
{ "projectId":12 }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `taxInvoiceId` | Long | 계산서 ID |
| `projectId` | Long | 매칭된 프로젝트 ID |
| `projectName` | String | 프로젝트명 |
| `matchedBy` | Object | 매칭자 |
| `matchedAt` | LocalDateTime | 매칭 일시 |

## Success Example
```
{
  "httpStatus":200,
  "message":"프로젝트 매칭 성공",
  "data": {
    "taxInvoiceId":201,
    "projectId":12,
    "projectName":"OO시 상수도 관리 용역",
    "matchedBy": { "userId":"E2023011", "name":"박재무" },
    "matchedAt":"2026-08-01T09:45:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 프로젝트 매칭 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무만 매칭할 수 있음 |
| 404 | Not Found | `TAX_INVOICE_NOT_FOUND` | 계산서가 존재하지 않음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |
| 409 | Conflict | `TAX_INVOICE_ALREADY_MATCHED` | 이미 다른 프로젝트에 매칭됨 |

---

# DELETE `/api/v1/tax-invoices/{taxInvoiceId}/project` — 계산서 프로젝트 매칭 해제

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 계산서 프로젝트 매칭 해제 |
| Method | DELETE |
| URL | `/api/v1/tax-invoices/{taxInvoiceId}/project` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 |
| 요구사항 | TXM-005 · TXM-006 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `taxInvoiceId` | Long | Y | 계산서 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
DELETE /api/v1/tax-invoices/201/project
```

⛔ **재무만 해제할 수 있다** (TXM-005). **블록에 연결된 계산서는 매칭을 해제할 수 없다** (TXM-006).

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `taxInvoiceId` | Long | 계산서 ID |
| `projectId` | Long | `null` |

## Success Example
```
{
  "httpStatus":200,
  "message":"프로젝트 매칭 해제 성공",
  "data": { "taxInvoiceId":201, "projectId":null }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 프로젝트 매칭 해제 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무만 해제할 수 있음 |
| 404 | Not Found | `TAX_INVOICE_NOT_FOUND` | 계산서가 존재하지 않음 |
| 409 | Conflict | `TAX_INVOICE_BLOCK_LINKED` | 블록에 연결된 계산서는 매칭을 해제할 수 없음 |

---

# PATCH `/api/v1/tax-invoices/{taxInvoiceId}/block` — 조회 블록 연결

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 세금계산서 조회 블록 연결 |
| Method | PATCH |
| URL | `/api/v1/tax-invoices/{taxInvoiceId}/block` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 |
| 요구사항 | TXL-002~005B · INV-06 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `taxInvoiceId` | Long | Y | 계산서 ID |

## Request Parameter
없음

## Request Body
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `blockId` | Long | Y | 연결할 `TAX_INVOICE_VIEW` 블록 ID |

| 규칙 | 위반 시 |
| --- | --- |
| **재무만** 연결할 수 있다 (TXL-002 · INV-06) | 403 |
| 프로젝트 매칭이 **먼저** 되어 있어야 한다 (TXL-003) | 400 |
| 블록이 **매칭된 그 프로젝트**에 속해야 한다 (TXL-004) | 400 |
| 1블록 **1계산서** — `tax_invoice_confirm` 에 `UNIQUE(block_id)` (TXL-005) | 409 |
| 1계산서 **1블록** (TXL-005B) | 409 |

`tax_invoice_confirm` 상세 행은 **연결 시점에** 생긴다 (TXL-001). `linked_by`·`linked_at` 를 사번·일시로 기록한다.
⛔ 이 테이블에 `round_no` 를 두지 않는다 — 회차 번호는 `block_payment_confirm` 한 곳에만 산다 (`TAX-V1.md` INV-11).

## Request Example
```
{ "blockId":41 }
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `taxInvoiceId` | Long | 계산서 ID |
| `blockId` | Long | 연결된 블록 ID |
| `blockTitle` | String | 블록 제목 |
| `linkedBy` | Object | 연결자 |
| `linkedAt` | LocalDateTime | 연결 일시 |

## Success Example
```
{
  "httpStatus":200,
  "message":"조회 블록 연결 성공",
  "data": {
    "taxInvoiceId":201,
    "blockId":41,
    "blockTitle":"1차 계산서",
    "linkedBy": { "userId":"E2023011", "name":"박재무" },
    "linkedAt":"2026-08-01T09:55:00"
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 조회 블록 연결 성공 |
| 400 | Bad Request | `TAX_INVOICE_NOT_MATCHED` | 프로젝트 매칭이 안 된 계산서 |
| 400 | Bad Request | `TAX_INVOICE_BLOCK_PROJECT_MISMATCH` | 블록이 매칭된 프로젝트에 속하지 않음 |
| 400 | Bad Request | `BLOCK_TYPE_NOT_TAX_INVOICE_VIEW` | 세금계산서 조회 블록이 아님 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무만 연결할 수 있음 |
| 404 | Not Found | `TAX_INVOICE_NOT_FOUND` | 계산서가 존재하지 않음 |
| 404 | Not Found | `BLOCK_NOT_FOUND` | 블록이 존재하지 않음 |
| 409 | Conflict | `BLOCK_ALREADY_LINKED` | 블록에 이미 다른 계산서가 연결됨 |
| 409 | Conflict | `TAX_INVOICE_ALREADY_LINKED` | 계산서가 이미 다른 블록에 연결됨 |

---

# DELETE `/api/v1/tax-invoices/{taxInvoiceId}/block` — 조회 블록 연결 해제

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 세금계산서 조회 블록 연결 해제 |
| Method | DELETE |
| URL | `/api/v1/tax-invoices/{taxInvoiceId}/block` |
| 인증 필요 여부 | Y |
| 권한 | 재무 담당자 |
| 요구사항 | TXL-007 · TXL-009 · TXL-010 · INV-06 · INV-07 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `taxInvoiceId` | Long | Y | 계산서 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
DELETE /api/v1/tax-invoices/201/block
```

⛔ **재무만 해제할 수 있다.** 실무자가 풀 수 있으면 블록 삭제 잠금이 무의미하다 (TXL-007 · INV-06).
해제하면 그 블록과 **소유 스텝의 삭제 잠금이 함께 풀린다** (TXL-009 · TXL-010 · INV-07).

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `taxInvoiceId` | Long | 계산서 ID |
| `blockId` | Long | `null` |

## Success Example
```
{
  "httpStatus":200,
  "message":"조회 블록 연결 해제 성공",
  "data": { "taxInvoiceId":201, "blockId":null }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 조회 블록 연결 해제 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_EDIT_DENIED` | 재무만 해제할 수 있음 |
| 404 | Not Found | `TAX_INVOICE_NOT_FOUND` | 계산서가 존재하지 않음 |
| 409 | Conflict | `TAX_INVOICE_NOT_LINKED` | 블록에 연결되지 않은 계산서 |

---

# GET `/api/v1/blocks/{blockId}/tax-invoice` — 조회 블록 상세 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 세금계산서 조회 블록 상세 조회 |
| Method | GET |
| URL | `/api/v1/blocks/{blockId}/tax-invoice` |
| 인증 필요 여부 | Y |
| 권한 | 스텝 접근 권한 (**읽기 전용**) |
| 요구사항 | TXL-006 · TXL-008 · INV-01 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `blockId` | Long | Y | `TAX_INVOICE_VIEW` 블록 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
GET /api/v1/blocks/41/tax-invoice
```

⛔ **블록에 발행·수정 버튼이 없다.** 스텝 `EDITOR` 여도 쓰기 요청은 403 (TXL-006 · INV-01).
미연결이면 `status` 가 `WAITING` 이다. **빈 화면이 아니다** (TXL-008).

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `blockId` | Long | 블록 ID |
| `title` | String | 블록 제목 |
| `status` | String | `WAITING`(미연결) · `LINKED`(연결됨) |
| `taxInvoice` | Object | 연결된 계산서. 미연결이면 `null` |
| `taxInvoice.taxInvoiceId` | Long | 계산서 ID |
| `taxInvoice.issuedAt` | LocalDate | 발행일 |
| `taxInvoice.supplyAmount` | BigDecimal | 공급가액 |
| `taxInvoice.taxAmount` | BigDecimal | 세액 |
| `taxInvoice.totalAmount` | BigDecimal | 합계 |

## Success Example
```
{
  "httpStatus":200,
  "message":"조회 블록 상세 조회 성공",
  "data": {
    "blockId":41,
    "title":"1차 계산서",
    "status":"LINKED",
    "taxInvoice": {
      "taxInvoiceId":201,
      "issuedAt":"2026-07-31",
      "supplyAmount":65454545,
      "taxAmount":6545455,
      "totalAmount":72000000
    }
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 조회 블록 상세 조회 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `STEP_ACCESS_DENIED` | 스텝 접근 권한 없음 |
| 404 | Not Found | `BLOCK_NOT_FOUND` | 블록이 존재하지 않음 |

---

# GET `/api/v1/settlements` — 정산 현황 목록 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 정산 현황 목록 조회 (P-44) |
| Method | GET |
| URL | `/api/v1/settlements` |
| 인증 필요 여부 | Y |
| 권한 | 재무 열람자 (**전 프로젝트**) |
| 요구사항 | STL-001~014 · STL-018 · INV-02·03·07 |

## Path Parameter
없음

## Request Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `dateFrom` | LocalDate | N | 기간 필터 시작 — **기준은 회차 예정일(`planned_date`)** |
| `dateTo` | LocalDate | N | 기간 필터 종료 — 같음 |
| `clientName` | String | N | 발주처 필터 |
| `managerUserId` | **String** | N | 담당자 **사번**. 판정 = **다음 예정일 회차 블록의 `block.owner`** (전액 입금이면 마지막 회차) — `STL-V1.md` §5-6 |
| `status` | String | N | 프로젝트 상태 필터 |
| `includeClosed` | Boolean | N | 기본 `false` — **종결 프로젝트는 기본으로 숨긴다** (STL-005) |
| `page` | int | N | 기본 0 |
| `size` | int | N | 기본 20 |

**재무는 참여하지 않은 프로젝트도 본다** — `project_member` 를 보지 않는다 (STL-002).
⛔ **실무자는 못 들어온다** (STL-001 · INV-07). ⛔ **쓰기 API 가 없다** (STL-008 · INV-01).
**정렬은 지연 일수 내림차순 → 다음 예정일 오름차순 고정** (STL-006).

## Request Body
없음

## Request Example
```
GET /api/v1/settlements?clientName=OO시청&includeClosed=false&page=0&size=20
```

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `content` | List\<Object\> | **프로젝트 1건 = 1행** (STL-003) |
| `content[].projectId` | Long | 프로젝트 ID |
| `content[].name` | String | 과업명 |
| `content[].clientName` | String | 발주처 (`project.client_name VARCHAR(200)`) |
| `content[].manager` | Object | 담당자 (`userId`·`name`) — **다음 예정일 회차 블록의 `owner`.** 회차 0개면 `null` |
| `content[].contractAmount` | BigDecimal | 계약금액. 없으면 `null` |
| `content[].roundProgress` | String | 회차 진행 `진행/전체` (예: `2/3`) — 입금이 연결된 회차를 진행으로 센다 (STL-012) |
| `content[].receivedAmount` | BigDecimal | 받은 금액 — **프로젝트에 매칭된 입금의 합. 블록 연결을 보지 않는다** (STL-009 · INV-02) |
| `content[].remainingAmount` | BigDecimal | 남은 금액 = `계약금액 − 받은 금액`. **계약금액이 없으면 `null`** (STL-010 · INV-03) |
| `content[].unplannedAmount` | BigDecimal | 미계획 금액 = `계약금액 − Σ 예정금액`. 0 이하면 `null` (STL-011) |
| `content[].nextPlannedDate` | LocalDate | 다음 예정일 — **미입금 회차 중 가장 이른 예정일.** 전부 입금됐으면 `null` (STL-014) |
| `content[].delayType` | String | `INVOICE_MISSING`(계산서 미발행) · `PAYMENT_WAITING`(입금 대기) · `null` |
| `content[].delayDays` | Integer | 지연 일수 |
| `content[].unlinkedPaymentCount` | int | 미연결 입금 건수 — **뱃지로 노출** (STL-018) |
| `content[].unlinkedTaxInvoiceCount` | int | 미연결 계산서 건수 (STL-019) |
| `page` / `size` / `totalElements` / `totalPages` | - | 페이징 정보 |

**열 개수는 회차 수와 무관하게 고정이다** (STL-004).

## Success Example
```
{
  "httpStatus":200,
  "message":"정산 현황 조회 성공",
  "data": {
    "content": [
      {
        "projectId":12,
        "name":"OO시 상수도 관리 용역",
        "clientName":"OO시청",
        "manager": { "userId":"E2024001", "name":"김용준" },
        "contractAmount":120000000,
        "roundProgress":"1/2",
        "receivedAmount":72000000,
        "remainingAmount":48000000,
        "unplannedAmount":40000000,
        "nextPlannedDate":"2026-11-30",
        "delayType":"PAYMENT_WAITING",
        "delayDays":12,
        "unlinkedPaymentCount":1,
        "unlinkedTaxInvoiceCount":0
      }
    ],
    "page":0,
    "size":20,
    "totalElements":1,
    "totalPages":1
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 정산 현황 조회 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_ACCESS_DENIED` | 재무 탭 접근 권한 없음 — 실무자는 들어올 수 없음 |

---

# GET `/api/v1/settlements/{projectId}` — 프로젝트 회차 상세 조회

## 기본 정보
| 항목 | 내용 |
| --- | --- |
| API 명 | 프로젝트 회차 상세 조회 (행 펼침) |
| Method | GET |
| URL | `/api/v1/settlements/{projectId}` |
| 인증 필요 여부 | Y |
| 권한 | 재무 열람자 |
| 요구사항 | STL-013 · STL-015~019 · INV-04 · INV-05 |

## Path Parameter
| 파라미터명 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| `projectId` | Long | Y | 프로젝트 ID |

## Request Parameter
없음

## Request Body
없음

## Request Example
```
GET /api/v1/settlements/12
```

회차는 `round_no` 순으로 나온다 (STL-013). **상위 합계와 회차 합계는 다를 수 있고, 그 차이가 곧 미연결이다** (INV-05).
**어긋남을 숨기지 않는다** — 미연결 입금·계산서, 예정일 미입력, 미계획 금액 셋 다 정상 상태이고 드러낸다 (INV-04).

## Response Parameter
| 파라미터명 | 타입 | 설명 |
| --- | --- | --- |
| `httpStatus` | int | HTTP 상태 코드 |
| `message` | String | 응답 메시지 |
| `data` | Object | 응답 데이터 |
| `projectId` | Long | 프로젝트 ID |
| `rounds` | List\<Object\> | 회차 목록 (`roundNo` 오름차순) |
| `rounds[].roundNo` | int | 회차 번호 |
| `rounds[].blockId` | Long | 입금확인 블록 ID |
| `rounds[].title` | String | 회차명 |
| `rounds[].plannedDate` | LocalDate | 예정일. 없으면 `null` |
| `rounds[].plannedDateMissing` | boolean | `true` 면 `예정일 미입력` 표시 (STL-017) |
| `rounds[].plannedAmount` | BigDecimal | 예정금액 |
| `rounds[].taxInvoice` | Object | 회차에 붙은 계산서 (`taxInvoiceId`·`issuedAt`·`totalAmount`). 없으면 `null` |
| `rounds[].receivedAmount` | BigDecimal | 회차에 연결된 입금 합계 |
| `rounds[].delayType` | String | `INVOICE_MISSING`(우리 잘못 · STL-015) · `PAYMENT_WAITING`(발주처 쪽 · STL-016) · `null` |
| `rounds[].delayDays` | Integer | 지연 일수 |
| `unlinkedPayments` | List\<Object\> | **미연결 입금** — 매칭됐지만 회차에 안 붙은 건 (STL-018) |
| `unlinkedPayments[].paymentId` | Long | 입금 ID |
| `unlinkedPayments[].paidAt` | LocalDate | 입금일 |
| `unlinkedPayments[].amount` | BigDecimal | 금액 |
| `unlinkedTaxInvoices` | List\<Object\> | **미연결 계산서** (STL-019) |
| `unlinkedTaxInvoices[].taxInvoiceId` | Long | 계산서 ID |
| `unlinkedTaxInvoices[].issuedAt` | LocalDate | 발행일 |
| `unlinkedTaxInvoices[].totalAmount` | BigDecimal | 합계 |

## Success Example
```
{
  "httpStatus":200,
  "message":"프로젝트 회차 상세 조회 성공",
  "data": {
    "projectId":12,
    "rounds": [
      {
        "roundNo":1,
        "blockId":40,
        "title":"1차 정산(선급 60%)",
        "plannedDate":"2026-08-31",
        "plannedDateMissing":false,
        "plannedAmount":72000000,
        "taxInvoice": { "taxInvoiceId":201, "issuedAt":"2026-07-31", "totalAmount":72000000 },
        "receivedAmount":72000000,
        "delayType":null,
        "delayDays":null
      },
      {
        "roundNo":2,
        "blockId":45,
        "title":"2차 정산(잔금 40%)",
        "plannedDate":null,
        "plannedDateMissing":true,
        "plannedAmount":48000000,
        "taxInvoice":null,
        "receivedAmount":0,
        "delayType":null,
        "delayDays":null
      }
    ],
    "unlinkedPayments": [
      { "paymentId":312, "paidAt":"2026-08-20", "amount":10000000 }
    ],
    "unlinkedTaxInvoices": []
  }
}
```

## Status Code
| 코드 | 상태 | code | 설명 |
| --- | --- | --- | --- |
| 200 | OK | - | 프로젝트 회차 상세 조회 성공 |
| 401 | Unauthorized | `AUTH_TOKEN_EXPIRED` | 인증 토큰 만료 |
| 403 | Forbidden | `FINANCE_ACCESS_DENIED` | 재무 탭 접근 권한 없음 |
| 404 | Not Found | `PROJECT_NOT_FOUND` | 프로젝트가 존재하지 않음 |
