package com.group3.vitamins.approval.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * APPROVAL 도메인 에러 코드.
 *
 * <p>⛔ 코드·메시지는 프론트와의 계약이다 (`.ai/api/approval.md` · 노션 확정).
 * 임의로 추가·변경하지 마라 (`.ai/API.md` §0).
 *
 * <p>각 코드에 근거 요구사항 ID를 달아둔다. {@code APR-V1.md} 표에 코드명까지 명시된 항목은 그 ID를,
 * {@code approval.md}(API 명세)의 상태코드 표에만 있고 {@code APR-V1.md} 개별 항목엔 없는 것은
 * "API 명세 n번 요구사항"으로 구분해서 적는다 — 없는 ID를 지어내지 않기 위함이다.
 */
@Getter
@RequiredArgsConstructor
public enum ApprovalErrorCode implements ErrorCode {

    // --- 3. 결재 제목·내용 수정 (API 명세 요구사항: APR-002) — 이후 회차 편집형 엔드포인트에서도 공용 ---

    /** API 명세 3번 상태코드 표 — {@code APR-V1.md} 개별 항목 없음(결재 자체가 없는 경우) */
    APPROVAL_NOT_FOUND("APPROVAL_NOT_FOUND", "결재를 찾을 수 없습니다."),
    /** API 명세 3번 상태코드 표 — {@code APR-V1.md} 개별 항목 없음(회차가 없거나 다른 결재 소속인 경우) */
    APPROVAL_REVISION_NOT_FOUND("APPROVAL_REVISION_NOT_FOUND", "회차를 찾을 수 없습니다."),
    /** APR-002 — 기안자가 아니면 403 */
    APPROVAL_NOT_DRAFTER("APPROVAL_NOT_DRAFTER", "기안자가 아닙니다."),
    /** APR-002 · INV-02 — {@code DRAFT} 아닌 회차 수정 시도 시 409(상신 후 불변) */
    APPROVAL_REVISION_NOT_DRAFT("APPROVAL_REVISION_NOT_DRAFT", "DRAFT 상태의 회차만 수정할 수 있습니다."),

    // --- 6. 결재선 등록·수정 (API 명세 요구사항: APR-009~014) ---

    /** APR-010 — 결재선 0명이면 400 */
    APPROVAL_LINE_EMPTY("APPROVAL_LINE_EMPTY", "결재자는 최소 1명이어야 합니다."),
    /** APR-011 — 순서(1부터 연속)가 중복·누락되면 400 */
    APPROVAL_LINE_ORDER_INVALID("APPROVAL_LINE_ORDER_INVALID", "결재 순서는 1부터 중복·누락 없이 연속되어야 합니다."),
    /**
     * APR-012 — 일반 결재자가 project member 아니면 400. {@code MASTER}·{@code ADMIN}은 제외(인사 계정이라
     * 프로젝트 소속이 없음).
     *
     * <p>결재선 등록(#91)뿐 아니라 <b>상신(#7)</b>에서도 난다 — 결재자가 그 사이 프로젝트에서 제거되면
     * 재상신이 복사한 결재선에 비멤버가 남기 때문이다. 그때 사용자가 할 일은 결재선 재지정이라
     * 메시지가 그걸 안내한다(자동 재지정은 만들지 않는다 — `APR-DELETE-DRAFT.md` §10).
     */
    APPROVAL_LINE_APPROVER_NOT_MEMBER("APPROVAL_LINE_APPROVER_NOT_MEMBER",
            "결재자 중 프로젝트 참여자가 아닌 사람이 있습니다. 결재선을 다시 지정해 주세요."),

    // --- 8. 재상신 회차 생성 (API 명세 요구사항: SUB-005~009) ---

    /** SUB-005 — {@code approval.status != REJECTED} 면 409 */
    APPROVAL_NOT_REJECTED("APPROVAL_NOT_REJECTED", "반려된 결재만 재상신 회차를 만들 수 있습니다."),

    // --- 4·5. 결재 문서 추가·제거 (API 명세 요구사항: APR-005~007) ---

    /** APR-005 — 연결하려는 file_version 이 없으면 404 */
    FILE_VERSION_NOT_FOUND("FILE_VERSION_NOT_FOUND", "존재하지 않는 파일 버전입니다."),
    /** APR-005 — {@code file_version.upload_status != COMPLETED} 면 409 */
    FILE_VERSION_NOT_READY("FILE_VERSION_NOT_READY", "업로드가 완료되지 않은 파일입니다."),
    /** APR-006 — 동일 회차에 동일 file_version_id 중복 연결 시 409(DB UNIQUE 대신 애플리케이션 검증) */
    DOCUMENT_ALREADY_LINKED("DOCUMENT_ALREADY_LINKED", "이미 연결된 파일입니다."),
    /** API 명세 5번 상태코드 표 — {@code APR-V1.md} 개별 항목 없음(문서가 없거나 다른 회차 소속인 경우) */
    APPROVAL_DOCUMENT_NOT_FOUND("APPROVAL_DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다."),

    // --- 7. 결재 상신 (API 명세 요구사항: SUB-001~004) ---

    /** SUB-001 — 제목·내용 중 하나라도 비어 있으면 400 */
    APPROVAL_CONTENT_REQUIRED("APPROVAL_CONTENT_REQUIRED", "제목과 내용을 모두 입력해야 합니다."),
    /** SUB-001 — 문서 0건이면 400 */
    APPROVAL_DOCUMENT_REQUIRED("APPROVAL_DOCUMENT_REQUIRED", "결재 문서를 최소 1건 첨부해야 합니다."),

    // --- 2. 결재 회차 상세조회 (API 명세 요구사항: MGT-005) ---

    /** MGT-005 — 차례 안 온 결재자(WAITING)이거나 관련 없는 사용자의 조회 시 403 */
    APPROVAL_LINE_NOT_VIEWABLE("APPROVAL_LINE_NOT_VIEWABLE", "조회 권한이 없습니다."),

    // ⛔ APPROVAL_IN_PROGRESS 제거(2026-08-10) — BLK-008 삭제 잠금 폐기로 사용처가 0이 됐다.
    //    진행 중 결재도 블록과 함께 삭제되며, 종결은 CANCELED 전이로 표현한다(DEL-002).
    //    회수(WITHDRAWN)가 생겨 409가 다시 필요해지면 그 요구사항 이름으로 새로 만든다.

    // --- 9. 결재관리 목록조회 (API 명세 요구사항: MGT-001~004) ---

    /** MGT-003 — MASTER·ADMIN이 아닌 사용자의 scope=all 요청 시 403 */
    APPROVAL_SCOPE_ALL_FORBIDDEN("APPROVAL_SCOPE_ALL_FORBIDDEN", "전체 조회는 MASTER만 가능합니다."),

    // --- 11. 결재 승인 (API 명세 요구사항: PRC-001~004) — 이후 반려(PRC-005~009)에서도 공용 ---

    /** PRC-001 — 해당 결재선의 결재자가 아님(존재하지 않는 lineId 포함, 리소스 존재 여부 비노출) */
    APPROVAL_LINE_FORBIDDEN("APPROVAL_LINE_FORBIDDEN", "해당 결재선의 결재자가 아닙니다."),
    /** PRC-001 — line.status가 ACTIVE가 아직 아님(WAITING 등 차례 안 옴) */
    APPROVAL_LINE_NOT_ACTIVE("APPROVAL_LINE_NOT_ACTIVE", "아직 처리할 차례가 아닙니다."),
    /** PRC-009 — 이미 처리 종결(APPROVED/REJECTED/CANCELED)된 결재선의 중복 처리(동시 요청·이중 클릭) */
    APPROVAL_LINE_ALREADY_PROCESSED("APPROVAL_LINE_ALREADY_PROCESSED", "이미 처리된 결재선입니다.");

    private final String code;
    private final String message;
}
