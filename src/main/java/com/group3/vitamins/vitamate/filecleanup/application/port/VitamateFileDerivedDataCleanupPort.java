package com.group3.vitamins.vitamate.filecleanup.application.port;

import com.group3.vitamins.vitamate.filecleanup.application.result.CleanupVitamateFileDerivedDataResult;

// fileId 기준으로 비타메이트 파생 데이터를 실제 DB에서 정리하는 포트
public interface VitamateFileDerivedDataCleanupPort {

    CleanupVitamateFileDerivedDataResult cleanupByFileId(Long fileId);
}