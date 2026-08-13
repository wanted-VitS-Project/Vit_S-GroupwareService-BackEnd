package com.group3.vitamins.project.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectErrorCode implements ErrorCode {

    PROJECT_NAME_REQUIRED("PROJECT_NAME_REQUIRED",
            "과업명을 입력해 주세요."),
    PROJECT_NAME_TOO_LONG("PROJECT_NAME_TOO_LONG",
            "과업명은 300자를 넘을 수 없습니다."),
    PROJECT_DATE_RANGE_INVALID("PROJECT_DATE_RANGE_INVALID",
            "시작일은 종료일보다 늦을 수 없습니다."),
    CONTRACT_AMOUNT_INVALID("CONTRACT_AMOUNT_INVALID",
            "계약금액은 0보다 작을 수 없습니다."),
    CLOSE_REASON_REQUIRED("CLOSE_REASON_REQUIRED",
            "종결 사유를 선택해 주세요."),
    CLOSE_REASON_INVALID("CLOSE_REASON_INVALID",
            "허용되지 않은 종결 사유 코드입니다."),
    CLOSE_REASON_NOTE_TOO_LONG("CLOSE_REASON_NOTE_TOO_LONG",
            "종결 사유 상세는 500자를 넘을 수 없습니다."),
    PROJECT_STATUS_INVALID("PROJECT_STATUS_INVALID",
            "허용되지 않은 프로젝트 상태 값입니다."),
    PROJECT_BID_NOTICE_ALREADY_LINKED("PROJECT_BID_NOTICE_ALREADY_LINKED",
            "이미 다른 프로젝트가 연결된 공고입니다."),
    PROJECT_NOT_FOUND("PROJECT_NOT_FOUND",
            "프로젝트를 찾을 수 없습니다."),
    PROJECT_ACCESS_DENIED("PROJECT_ACCESS_DENIED",
            "프로젝트에 접근할 권한이 없습니다."),
    PROJECT_EDIT_DENIED("PROJECT_EDIT_DENIED",
            "프로젝트를 편집할 권한이 없습니다."),
    /**
     * PRJ-014 — 진행 전이 아니거나 스텝이 남은 프로젝트를 확인 없이 지우려 할 때 (DEL-016 패턴).
     *
     * <p>⚠️ <b>금지가 아니다.</b> {@code confirm=true} 재요청이면 그대로 삭제된다. 프론트는 이 코드를
     * 받으면 메시지를 그대로 띄우고 확인 버튼을 붙인다 — 예전 {@code PROJECT_DELETE_NOT_ALLOWED} 처럼
     * 「종결로 처리하라」로 유도하면 사용자가 삭제할 방법을 영영 못 찾는다.
     *
     * <p>메시지는 지워질 스텝 수를 담아 {@code ConflictException(code, message)} 로 덮어쓴다.
     * 코드가 바뀌면 프론트 분기가 깨지므로 <b>메시지만</b> 바꾼다.
     */
    PROJECT_DELETE_CONFIRM_REQUIRED("PROJECT_DELETE_CONFIRM_REQUIRED",
            "삭제하면 되돌릴 수 없습니다. 삭제하려면 확인이 필요합니다."),
    CATEGORY_IDS_REQUIRED("CATEGORY_IDS_REQUIRED",
            "연결할 사업 카테고리를 선택해 주세요."),
    BUSINESS_CATEGORY_DUPLICATED("BUSINESS_CATEGORY_DUPLICATED",
            "이미 연결된 사업 카테고리입니다."),
    BUSINESS_CATEGORY_NOT_LINKED("BUSINESS_CATEGORY_NOT_LINKED",
            "연결되지 않은 사업 카테고리입니다."),
    USER_NOT_FOUND("USER_NOT_FOUND",
            "지정한 사용자를 찾을 수 없습니다."),
    MEMBER_PERMISSION_INVALID("MEMBER_PERMISSION_INVALID",
            "허용되지 않은 권한 등급입니다."),
    MEMBER_ALREADY_EXISTS("MEMBER_ALREADY_EXISTS",
            "이미 참여자로 등록된 사용자입니다."),
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND",
            "참여자를 찾을 수 없습니다."),
    MEMBER_SELF_EDIT_DENIED("MEMBER_SELF_EDIT_DENIED",
            "자기 자신의 권한 행은 변경할 수 없습니다."),
    /**
     * 복제 대상이 너무 커서 거부한다 (PRJ-018). 메시지는 실제 블록 수를 담아
     * {@code ValidationException(code, message)} 로 덮어쓴다 — 코드가 바뀌면 프론트 분기가 깨진다.
     */
    PROJECT_DUPLICATE_TOO_LARGE("PROJECT_DUPLICATE_TOO_LARGE",
            "복제할 블록이 너무 많습니다. 원본의 블록을 줄여 주세요."),
    PROJECT_VERSION_REQUIRED("PROJECT_VERSION_REQUIRED",
            "버전 정보가 없습니다. 화면을 새로고침해 주세요."),
    PROJECT_VERSION_CONFLICT("PROJECT_VERSION_CONFLICT",
            "다른 사용자가 먼저 수정했습니다.");


    private final String code;
    private final String message;
}