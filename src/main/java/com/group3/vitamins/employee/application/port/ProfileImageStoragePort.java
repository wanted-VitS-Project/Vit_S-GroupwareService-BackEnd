package com.group3.vitamins.employee.application.port;

import org.springframework.web.multipart.MultipartFile;

/**
 * 프로필 사진 저장소(S3) 아웃바운드 포트. 사원 도메인은 이 인터페이스만 알고, 리사이즈·실제
 * 업로드 방식은 {@code infrastructure/storage} 구현체가 처리한다.
 *
 * <p>이미지 블록 도메인의 {@code ImageStoragePort} 와 원칙은 같다(버킷이 퍼블릭 액세스를 전부
 * 차단하고 있어 영구 URL 을 만들 수 없으므로, 저장은 키만 하고 URL 은 조회 시점에 그때그때
 * {@link #presignViewUrl} 로 서명해 발급한다). 다만 키 네임스페이스가 사원 단위라 별도 포트로 둔다
 * — 이미지 도메인에 대한 의존을 만들지 않기 위함.
 */
public interface ProfileImageStoragePort {

    /**
     * 프로필 사진을 저장소에 올린다. 큰 이미지는 구현체가 업로드 전에 축소할 수 있다.
     *
     * @return 저장된 S3 키 (DB {@code employee.profile_image_key} 에 그대로 저장)
     */
    String upload(String userId, MultipartFile file, String extension);

    /** 저장된 키로 한시적으로 열리는 조회용 URL 을 발급한다(서빙 API 의 302 redirect 대상). */
    String presignViewUrl(String storageKey);

    /**
     * 저장소에서 객체를 지운다. <b>실패 경로 보상용</b> — 업로드는 됐는데 DB 키 반영이 실패해
     * 참조가 없어진 새 객체(고아)를 정리할 때 쓴다. 교체 시 이전 사진을 지우는 용도가 아니다(소프트 정책).
     */
    void delete(String storageKey);
}
