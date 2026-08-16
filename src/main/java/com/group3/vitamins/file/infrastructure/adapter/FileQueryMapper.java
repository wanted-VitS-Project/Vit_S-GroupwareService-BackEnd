package com.group3.vitamins.file.infrastructure.adapter;

import com.group3.vitamins.file.application.query.CompanyFileCriteria;
import com.group3.vitamins.file.application.query.MyProjectFileCriteria;
import com.group3.vitamins.file.application.result.AdminTreeProjectProjection;
import com.group3.vitamins.file.application.result.AdminTreeStageProjection;
import com.group3.vitamins.file.application.result.AdminTreeStepProjection;
import com.group3.vitamins.file.application.result.BlockFileProjection;
import com.group3.vitamins.file.application.result.FileVersionProjection;
import com.group3.vitamins.file.application.result.FileViewProjection;
import com.group3.vitamins.file.application.result.ProjectFileProjection;
import com.group3.vitamins.file.application.result.ProjectTrashFileProjection;
import com.group3.vitamins.file.application.result.ProjectFileVersionProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 파일 화면용 조회 (MyBatis · 조회 전용). SQL 은 XML 에 둔다. */
@Mapper
public interface FileQueryMapper {

    boolean existsActiveNameInBlock(@Param("blockId") Long blockId, @Param("name") String name);

    Long findBlockIdByFileId(@Param("fileId") Long fileId);

    List<Long> findActiveFileIdsByBlockId(@Param("blockId") Long blockId);

    Long findStepIdByFileIdIncludingDeletedBlock(@Param("fileId") Long fileId);

    List<FileVersionProjection> findCompletedVersions(@Param("fileId") Long fileId);

    int findMaxCompletedVersionNo(@Param("fileId") Long fileId);

    List<BlockFileProjection> findBlockFiles(@Param("blockId") Long blockId, @Param("deleted") boolean deleted);

    List<ProjectFileVersionProjection> findProjectFileVersions(@Param("projectId") Long projectId);

    List<ProjectFileProjection> findProjectFiles(@Param("projectId") Long projectId);

    List<ProjectTrashFileProjection> findProjectTrashFiles(@Param("projectId") Long projectId);

    long countCompanyFiles(CompanyFileCriteria criteria);

    List<FileViewProjection> findCompanyFiles(CompanyFileCriteria criteria);

    List<FileViewProjection> findMyProjectFiles(MyProjectFileCriteria criteria);

    // ─── 전사 파일 트리 탐색(§14) ───

    List<AdminTreeProjectProjection> findAdminTreeProjects(
            @Param("companyId") long companyId, @Param("limit") int limit, @Param("offset") long offset);

    long countAdminTreeProjects(@Param("companyId") long companyId);

    boolean existsProjectInCompany(@Param("companyId") long companyId, @Param("projectId") Long projectId);

    List<AdminTreeStageProjection> findAdminTreeStages(
            @Param("companyId") long companyId, @Param("projectId") Long projectId);

    boolean existsUnassignedStep(@Param("companyId") long companyId, @Param("projectId") Long projectId);

    List<AdminTreeStepProjection> findAdminTreeSteps(
            @Param("companyId") long companyId, @Param("projectId") Long projectId, @Param("stageId") Long stageId);

    boolean existsStepInCompany(@Param("companyId") long companyId, @Param("stepId") Long stepId);

    List<FileViewProjection> findAdminTreeStepFiles(
            @Param("companyId") long companyId, @Param("stepId") Long stepId,
            @Param("limit") int limit, @Param("offset") long offset);

    long countAdminTreeStepFiles(@Param("companyId") long companyId, @Param("stepId") Long stepId);
}
