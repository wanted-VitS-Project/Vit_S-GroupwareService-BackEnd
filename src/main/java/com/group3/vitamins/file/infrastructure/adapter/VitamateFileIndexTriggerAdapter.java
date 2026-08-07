package com.group3.vitamins.file.infrastructure.adapter;

import com.group3.vitamins.file.application.port.FileIndexTriggerPort;
import com.group3.vitamins.vitamate.fileindex.application.command.DispatchVitamateFileIndexCommand;
import com.group3.vitamins.vitamate.fileindex.application.usecase.DispatchVitamateFileIndexUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// FileIndexTriggerPort 구현체. 비타메이트 fileindex 도메인의 인바운드 유스케이스를 그대로 호출한다
// (ARCHITECTURE.md §2-2 — 재사용할 로직이 있으면 상대 유스케이스를 호출하고 복제하지 않는다).
@Component
@RequiredArgsConstructor
public class VitamateFileIndexTriggerAdapter implements FileIndexTriggerPort {

    private final DispatchVitamateFileIndexUseCase dispatchVitamateFileIndexUseCase;

    @Override
    public void triggerIndexing(Long fileVersionId) {
        dispatchVitamateFileIndexUseCase.handle(new DispatchVitamateFileIndexCommand(fileVersionId));
    }
}