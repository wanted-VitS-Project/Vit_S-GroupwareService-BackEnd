package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexDataPort;
import com.group3.vitamins.vitamate.fileindex.application.query.GetVitamateFileIndexSourceQuery;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexSourceResult;
import com.group3.vitamins.vitamate.fileindex.application.usecase.GetVitamateFileIndexSourceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Python worker가 파일 인덱싱에 사용할 파일 다운로드 정보를 조회합니다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VitamateFileIndexSourceQueryService implements GetVitamateFileIndexSourceUseCase {

    private final VitamateFileIndexDataPort fileIndexDataPort;

    @Override
    public VitamateFileIndexSourceResult handle(GetVitamateFileIndexSourceQuery query) {
        validateQuery(query);

        return fileIndexDataPort.findIndexSource(query.fileVersionId())
                .orElseThrow(() -> new NotFoundException(VitamateErrorCode.VITAMATE_FILE_VERSION_NOT_FOUND));
    }

    // 파일 버전 ID가 조회 가능한 값인지 먼저 검증합니다.
    private void validateQuery(GetVitamateFileIndexSourceQuery query) {
        if (query == null
                || query.fileVersionId() == null
                || query.fileVersionId() <= 0) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }
}