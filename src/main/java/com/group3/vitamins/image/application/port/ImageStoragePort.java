package com.group3.vitamins.image.application.port;

import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 파일 저장소(S3) 아웃바운드 포트. 이미지 도메인은 이 인터페이스만 알고,
 * 리사이즈·실제 업로드 방식은 infrastructure/storage 구현체가 처리한다.
 *
 * <p>버킷이 퍼블릭 액세스를 전부 차단하고 있어(2026-08-04 콘솔 확인), 영구적으로 열리는
 * URL을 만들 수 없다. 그래서 저장할 때는 키만 받고, 프론트에 보여줄 URL은 응답을
 * 만드는 시점에 {@link #presignViewUrl} 로 그때그때 서명해서 발급한다 — 파일(File)
 * 도메인의 "다운로드 URL 발급"과 같은 원칙이다.
 */
public interface ImageStoragePort {

    /**
     * 파일을 저장소에 올린다. 큰 이미지는 구현체가 업로드 전에 축소할 수 있다 —
     * 그 경우 반환되는 {@code sizeBytes} 는 축소 후 실제로 저장된 크기다.
     */
    UploadedImage upload(Long imgBlockId, MultipartFile file, String extension);

    /** 저장된 키로 한시적으로 열리는 조회용 URL을 발급한다. */
    String presignViewUrl(String storageKey);

    /** 다운로드 API용 — 저장소에서 실제 파일 바이트를 그대로 읽어온다(응답 바디에 직접 실어 보내야 해서). */
    byte[] download(String storageKey);

    /** 확장자에 대응하는 Content-Type. 다운로드 응답 헤더용. */
    String contentTypeOf(String extension);

    /** 완전 삭제(하드 삭제) 전용 — 저장소에서 실제 객체를 지운다. 되돌릴 수 없다. */
    void delete(String storageKey);

    record UploadedImage(String storageKey, long sizeBytes) {
    }
}
