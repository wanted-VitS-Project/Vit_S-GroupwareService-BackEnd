package com.group3.vitamins.vitamate.filecleanup.application.usecase;

import com.group3.vitamins.vitamate.filecleanup.application.command.CleanupVitamateFileDerivedDataCommand;
import com.group3.vitamins.vitamate.filecleanup.application.result.CleanupVitamateFileDerivedDataResult;

// 파일 도메인이 호출하는 비타메이트 파생 데이터 정리 유스케이스
public interface CleanupVitamateFileDerivedDataUseCase {

    CleanupVitamateFileDerivedDataResult handle(CleanupVitamateFileDerivedDataCommand command);
}