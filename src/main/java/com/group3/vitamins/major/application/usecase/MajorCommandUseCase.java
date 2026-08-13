package com.group3.vitamins.major.application.usecase;

import com.group3.vitamins.major.application.command.CreateMajorCommand;
import com.group3.vitamins.major.application.command.DeleteMajorCommand;
import com.group3.vitamins.major.application.command.UpdateMajorCommand;
import com.group3.vitamins.major.application.result.MajorResult;

/** 전공 마스터 쓰기 인바운드 포트 (생성·수정·삭제). ADMIN 전용. */
public interface MajorCommandUseCase {

    MajorResult create(CreateMajorCommand command);

    MajorResult update(UpdateMajorCommand command);

    /** hard delete + 참조 차단(MAJ-002). */
    void delete(DeleteMajorCommand command);
}
