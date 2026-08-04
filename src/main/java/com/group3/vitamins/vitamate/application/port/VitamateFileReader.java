package com.group3.vitamins.vitamate.application.port;

import java.util.List;

// 비타메이트 분석에 사용할 파일 버전의 유효성을 조회하는 포트
public interface VitamateFileReader {

    boolean existsAllCompletedFileVersionsInProject(Long projectId, List<Long> fileVersionIds);
}
