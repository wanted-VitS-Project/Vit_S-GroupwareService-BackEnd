package com.group3.vitamins.text.application.port;

/**
 * Block/Step 도메인(동훈님 소관)에 물어보는 아웃바운드 포트.
 * 텍스트 도메인은 이 인터페이스만 알고, 실제 조회는 infrastructure/adapter 구현체가 처리한다.
 *
 * <p>공용 block 테이블의 {@code type}/{@code blockTypeId} 컬럼으로 상세 테이블(예: text.txt_id)을
 * 가리키는 방향이라, 그 두 값 + userId 로 편집 권한을 한 번에 물어본다.
 */
public interface BlockCatalogPort {

    /**
     * @deprecated role 없이 판정하던 예전 시그니처. 체크리스트 호출부가 아직 role 을 안 실어 날라서
     *             호환용으로 남겨둔다 — 체크리스트가 {@link #hasEditPermission(String, Long, String, String)}로
     *             옮겨가면 제거할 것. 항상 true 를 반환한다(기존 스텁 동작 그대로).
     */
    @Deprecated
    boolean hasEditPermission(String blockType, Long blockTypeId, String userId);

    /**
     * blockType + blockTypeId(상세 테이블 PK) 로 그 블록이 속한 step 에 대해
     * 현재 사용자가 편집 권한이 있는지 확인한다. Step 도메인의 {@code StepAccessUseCase}를 재사용한다.
     */
    boolean hasEditPermission(String blockType, Long blockTypeId, String userId, String role);

    /**
     * blockType + blockTypeId 로 그 블록이 속한 step 을 현재 사용자가 조회(VIEWER 이상)할 수 있는지 확인한다.
     */
    boolean hasViewPermission(String blockType, Long blockTypeId, String userId, String role);

    /** 활동 로그(Block 생성/삭제)에 남길 Block명을 blockType + blockTypeId 로 조회한다. */
    String getBlockTitle(String blockType, Long blockTypeId);
}
