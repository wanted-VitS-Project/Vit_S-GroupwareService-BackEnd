package com.group3.vitamins.companydocument.presentation.api;

/** 사내 문서함 API 성공 응답 메시지 상수. */
public final class CompanyDocumentResponseMessage {

    public static final String UPLOAD_STARTED = "업로드 URL을 발급했습니다.";
    public static final String UPLOAD_COMPLETED = "업로드를 완료했습니다.";
    public static final String DOCUMENT_LIST = "사내 문서 목록 조회 성공";
    public static final String VERSION_HISTORY = "버전 이력 조회 성공";
    public static final String DOWNLOAD_URL_ISSUED = "다운로드 URL을 발급했습니다.";
    public static final String DOCUMENT_UPDATED = "문서를 수정했습니다.";
    public static final String DOCUMENT_DELETED = "문서를 삭제했습니다.";
    public static final String DOCUMENT_RESTORED = "문서를 복구했습니다.";

    private CompanyDocumentResponseMessage() {
    }
}
