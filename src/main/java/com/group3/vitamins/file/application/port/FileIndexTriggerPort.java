package com.group3.vitamins.file.application.port;

// 파일 버전 완료 시 비타메이트 문서 인덱싱을 시작시키는 아웃바운드 포트.
// 구현은 vitamate.fileindex 쪽 유스케이스를 호출한다 (infrastructure/adapter/VitamateFileIndexTriggerAdapter).
public interface FileIndexTriggerPort {

    // 완료된 파일 버전의 인덱싱을 시작한다.
    void triggerIndexing(Long fileVersionId);
}