package com.group3.vitamins.file.application.usecase;

import com.group3.vitamins.file.application.command.RenameFileCommand;
import com.group3.vitamins.file.application.command.TrashFileCommand;
import com.group3.vitamins.file.application.result.FileRenameResult;
import com.group3.vitamins.file.application.result.FileTrashResult;

/** 파일(문서) 쓰기 유스케이스 (#135). 스텝 EDITOR 권한을 따른다. */
public interface FileCommandUseCase {

    /** 문서명 수정(§4). */
    FileRenameResult rename(RenameFileCommand command);

    /** 휴지통으로 이동(§5). 진행 중 결재 대상이면 막는다. */
    FileTrashResult moveToTrash(TrashFileCommand command);
}
