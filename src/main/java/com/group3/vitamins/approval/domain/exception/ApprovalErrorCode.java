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
     * APR-012 — 일반 결재자가 project member 아니면 400. {@code MASTER}만 제외하며
     * {@code ADMIN}은 인사 전용이라 결재자로 지정할 수 없다.
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

    // --- 블록 직접 삭제 잠금 (요구사항: DEL-016) ---

    // ⛔ APPROVAL_IN_PROGRESS 제거(2026-08-10) — BLK-008 삭제 잠금 폐기로 사용처가 0이 됐다.
    //    아래 코드로 되살리지 않는다: DEL-016은 IN_PROGRESS·REJECTED·COMPLETED 세 상태를 함께
    //    덮으므로 "진행 중"이라는 이름이 맞지 않는다. 예고대로 요구사항 이름으로 새로 만들었다.

    /**
     * DEL-016 — 한 번 상신된 결재의 블록을 직접 삭제하려 하면 409.
     * {@code IN_PROGRESS}·{@code REJECTED}·{@code COMPLETED} 가 대상이고 {@code DRAFT}·{@code CANCELED} 는 통과한다.
     *
     * <p>⛔ 메시지에 <b>"스텝을 삭제하면 함께 삭제된다"를 넣지 마라</b> (2026-08-12 결정). 스텝 삭제는
     * 그 안의 블록·이슈 전부를 되돌릴 수 없이 날리는 훨씬 큰 행동이라, 블록 하나를 못 지운 사람에게
     * 권할 안내가 아니다. 게다가 블록 삭제는 스텝 EDITOR, 스텝 삭제는 <b>프로젝트</b> EDITOR 라
     * 오버라이드로 스텝 EDITOR 만 가진 사용자에게는 할 수 없는 일을 시키는 막다른 안내가 된다.
     *
     * <p>⚠️ <b>실제로 나가는 문구는 상태별로 다르다</b> — "진행 중인/반려된/완료된 결재는 삭제할 수
     * 없습니다." 사용자는 지금 화면의 상태를 기준으로 읽기 때문이다. 문구는
     * {@code ApprovalHandlerService.LOCKED_MESSAGES} 가 갖고, 아래 메시지는 폴백이다.
     * <b>코드는 하나로 유지한다</b> — 처리가 셋 다 같아 쪼개면 프론트가 같은 분기를 세 번 짠다.
     *
     * <p>⚠️ <b>스텝 삭제 cascade 에서는 나오지 않는다</b> (DEL-017). 이 코드가 스텝 삭제 응답에 보이면
     * 판정을 공유 삭제 본체에 잘못 넣은 것이다 — {@code BlockCommandService} 참고.
     */
    APPROVAL_ALREADY_SUBMITTED("APPROVAL_ALREADY_SUBMITTED",
            "이미 상신된 결재는 삭제할 수 없습니다."),

    // --- 9. 결재관리 목록조회 (API 명세 요구사항: MGT-001~004) ---

    /** MGT-003 — MASTER가 아닌 사용자의 scope=all 요청 또는 ADMIN의 결재 목록 접근 시 403 */
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
