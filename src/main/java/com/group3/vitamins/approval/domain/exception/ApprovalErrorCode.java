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

    // --- 1. 결재 블록 생성 (API 명세 요구사항: APR-001 · APR-001-1 · BND-001) ---

    /** APR-001 — block 이 없으면 404 */
    BLOCK_NOT_FOUND("BLOCK_NOT_FOUND", "블록을 찾을 수 없습니다."),
    /** APR-001 — {@code block.type != APPROVAL} 이면 400 */
    BLOCK_TYPE_MISMATCH("BLOCK_TYPE_MISMATCH", "결재 블록이 아닙니다."),
    /** BND-001(API 명세 1번 요구사항) · {@code PERMISSION.md} §6 프로젝트 진입 판정 — {@code APR-V1.md} 개별 항목 없음 */
    APPROVAL_NOT_PROJECT_MEMBER("APPROVAL_NOT_PROJECT_MEMBER", "프로젝트 참여자가 아닙니다."),

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
    /** APR-012 — 일반 결재자가 project member 아니면 400. {@code MASTER}·{@code ADMIN}은 제외(인사 계정이라 프로젝트 소속이 없음) */
    APPROVAL_LINE_APPROVER_NOT_MEMBER("APPROVAL_LINE_APPROVER_NOT_MEMBER", "결재자는 해당 프로젝트의 참여자여야 합니다."),

    // --- 8. 재상신 회차 생성 (API 명세 요구사항: SUB-005~009) ---

    /** SUB-005 — {@code approval.status != REJECTED} 면 409 */
    APPROVAL_NOT_REJECTED("APPROVAL_NOT_REJECTED", "반려된 결재만 재상신 회차를 만들 수 있습니다.");

    private final String code;
    private final String message;
}
