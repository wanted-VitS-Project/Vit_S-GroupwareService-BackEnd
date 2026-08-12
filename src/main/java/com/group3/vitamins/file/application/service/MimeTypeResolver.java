package com.group3.vitamins.file.application.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * 확장자 → MIME 타입 추론기 (입찰 검토 파일 귀속 §2-G).
 *
 * <p>일반 업로드는 클라이언트가 {@code mimeType} 을 보내지만, 귀속은 클라이언트가 없다(서버측 복사).
 * 원본 파일명 확장자로 정적 매핑한다 — {@code Files.probeContentType} 은 실제 파일이 없으면 불안정해 쓰지 않는다.
 * 매핑에 없으면 {@code application/octet-stream}.
 */
@Component
public class MimeTypeResolver {

    private static final String DEFAULT = "application/octet-stream";

    private static final Map<String, String> BY_EXTENSION = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("hwp", "application/x-hwp"),
            Map.entry("hwpx", "application/haansofthwpx"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("zip", "application/zip"));

    public String resolve(String extension) {
        if (extension == null || extension.isBlank()) {
            return DEFAULT;
        }
        return BY_EXTENSION.getOrDefault(extension.toLowerCase(Locale.ROOT), DEFAULT);
    }
}
