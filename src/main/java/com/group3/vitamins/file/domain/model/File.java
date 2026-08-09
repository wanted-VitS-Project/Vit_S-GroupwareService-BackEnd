package com.group3.vitamins.file.domain.model;

import java.time.LocalDateTime;

/**
 * 파일(논리 문서) 도메인 객체 (`.ai/api/file.md`).
 *
 * <p>순수 도메인이다 — JPA·Spring 에 의존하지 않는다.
 * ⭐ **파일은 프로젝트 소속**(`project_id`)이고 블록은 `block_file` 로 참조만 한다 — 블록을 지워도 파일은 산다
 * (`BLOCK.md` §4-4). 그래서 블록 생명주기와 분리된 자체 소프트 삭제(`deletedAt`)를 가진다.
 *
 * <p>버전은 별도 애그리게이트 엔티티({@link FileVersion})로, 여기서 직접 들고 있지 않는다(1:N).
 */
public class File {

    private final Long fileId;
    private final Long projectId;
    private String name;
    private final String createdBy;
    private LocalDateTime deletedAt;

    private File(Long fileId, Long projectId, String name, String createdBy, LocalDateTime deletedAt) {
        this.fileId = fileId;
        this.projectId = projectId;
        this.name = name;
        this.createdBy = createdBy;
        this.deletedAt = deletedAt;
    }

    /** 새 문서를 만든다(버전 1 업로드 시작 시). 아직 저장 전이라 ID 가 없다. */
    public static File create(Long projectId, String name, String createdBy) {
        return new File(null, projectId, name, createdBy, null);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static File restore(Long fileId, Long projectId, String name, String createdBy, LocalDateTime deletedAt) {
        return new File(fileId, projectId, name, createdBy, deletedAt);
    }

    /** 표시명을 바꾼다(§4). 원본 파일명은 버전에 있으므로 건드리지 않는다. */
    public void rename(String name) {
        this.name = name;
    }

    /** 휴지통으로 이동(§5). 저장소 객체는 지우지 않는다. 이미 삭제 상태면 판정은 서비스가 한다. */
    public void moveToTrash(LocalDateTime now) {
        this.deletedAt = now;
    }

    /** 휴지통에서 복구(§6). 삭제 시각을 지운다. 휴지통 여부 판정은 서비스가 한다. */
    public void restoreFromTrash() {
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public Long getFileId() {
        return fileId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
