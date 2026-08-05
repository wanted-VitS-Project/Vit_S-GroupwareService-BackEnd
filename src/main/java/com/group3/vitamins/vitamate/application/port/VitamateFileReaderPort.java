package com.group3.vitamins.vitamate.application.port;

import java.util.List;

// 비타메이트 분석에 사용할 파일 버전의 유효성을 조회하는 포트
public interface VitamateFileReaderPort {

    // 요청한 파일 버전들이 모두 해당 프로젝트 안의 업로드 완료 파일인지 확인한다.
    boolean existsAllCompletedFileVersionsInProject(Long projectId, List<Long> fileVersionIds);
}
