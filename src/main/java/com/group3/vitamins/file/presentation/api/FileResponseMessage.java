package com.group3.vitamins.file.presentation.api;

/** 파일 API 성공 응답 메시지 상수. */
public final class FileResponseMessage {

    public static final String UPLOAD_STARTED = "업로드 URL을 발급했습니다.";
    public static final String UPLOAD_COMPLETED = "업로드를 완료했습니다.";
    public static final String DOWNLOAD_URL_ISSUED = "다운로드 URL을 발급했습니다.";
    public static final String VERSION_DETAIL = "버전 조회 성공";
    public static final String VERSION_HISTORY = "버전 이력 조회 성공";
    public static final String BLOCK_FILES = "블록 파일 목록 조회 성공";
    public static final String FILE_RENAMED = "문서명을 수정했습니다.";
    public static final String FILE_TRASHED = "문서를 휴지통으로 이동했습니다.";
    public static final String FILE_RESTORED = "문서를 복구했습니다.";
    public static final String FILE_PERMANENTLY_DELETED = "문서를 영구 삭제했습니다.";
    public static final String PROJECT_FILE_VERSIONS = "파일 버전 목록 조회 성공";

    private FileResponseMessage() {
    }
}
