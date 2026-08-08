package com.group3.vitamins.file.application.usecase;

import com.group3.vitamins.file.application.command.PermanentDeleteFileCommand;
import com.group3.vitamins.file.application.command.RenameFileCommand;
import com.group3.vitamins.file.application.command.RestoreFileCommand;
import com.group3.vitamins.file.application.command.TrashFileCommand;
import com.group3.vitamins.file.application.result.FilePermanentDeleteResult;
import com.group3.vitamins.file.application.result.FileRenameResult;
import com.group3.vitamins.file.application.result.FileRestoreResult;
import com.group3.vitamins.file.application.result.FileTrashResult;

/** 파일(문서) 쓰기 유스케이스 (#135). 스텝 EDITOR 권한을 따른다. */
public interface FileCommandUseCase {

    /** 문서명 수정(§4). */
    FileRenameResult rename(RenameFileCommand command);

    /** 휴지통으로 이동(§5). 진행 중 결재 대상이면 막는다. */
    FileTrashResult moveToTrash(TrashFileCommand command);

    /** 휴지통에서 복구(§6). 블록이 삭제됐어도 복구되며 그때는 blockId=null·blockDeleted=true. */
    FileRestoreResult restore(RestoreFileCommand command);

    /** 영구 삭제(§7). 휴지통 문서만 대상. 파생데이터 정리 → 전 버전·file 삭제 → 저장소 객체 삭제. */
    FilePermanentDeleteResult permanentDelete(PermanentDeleteFileCommand command);
}
