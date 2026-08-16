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

    /**
     * 블록 삭제로 그 블록의 활성 파일을 휴지통으로 이동한다 (D안 · {@code BlockFileTrashPort} 구현 경로).
     *
     * <p>개별 휴지통 이동(§5)과 달리 파일별 스텝 편집권한을 재검사하지 않는다 — 블록 삭제 권한이
     * 상위(블록/스텝 삭제)에서 이미 판정됐다. 단 진행 중 결재가 참조하는 파일이 하나라도 있으면
     * {@code FILE_APPROVAL_IN_PROGRESS}(409)를 던져 블록 삭제 트랜잭션을 통째로 롤백한다.
     *
     * @return 휴지통으로 이동한 파일 수(이미 휴지통이거나 사라진 파일은 제외)
     */
    int trashByBlockDeletion(Long blockId, String actorUserId);
}
