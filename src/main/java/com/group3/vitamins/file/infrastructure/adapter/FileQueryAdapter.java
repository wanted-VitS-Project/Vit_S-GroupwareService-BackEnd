package com.group3.vitamins.file.infrastructure.adapter;

import com.group3.vitamins.file.application.port.FileQueryPort;
import com.group3.vitamins.file.application.query.CompanyFileCriteria;
import com.group3.vitamins.file.application.query.MyProjectFileCriteria;
import com.group3.vitamins.file.application.result.AdminTreeProjectProjection;
import com.group3.vitamins.file.application.result.AdminTreeStageProjection;
import com.group3.vitamins.file.application.result.AdminTreeStepProjection;
import com.group3.vitamins.file.application.result.BlockFileProjection;
import com.group3.vitamins.file.application.result.FileVersionProjection;
import com.group3.vitamins.file.application.result.FileViewProjection;
import com.group3.vitamins.file.application.result.ProjectFileProjection;
import com.group3.vitamins.file.application.result.ProjectFileVersionProjection;
import com.group3.vitamins.file.application.result.ProjectTrashFileProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FileQueryAdapter implements FileQueryPort {

    private final FileQueryMapper fileQueryMapper;

    @Override
    public boolean existsActiveNameInBlock(Long blockId, String name) {
        return fileQueryMapper.existsActiveNameInBlock(blockId, name);
    }

    @Override
    public Optional<Long> findBlockIdByFileId(Long fileId) {
        return Optional.ofNullable(fileQueryMapper.findBlockIdByFileId(fileId));
    }

    @Override
    public List<Long> findActiveFileIdsByBlockId(Long blockId) {
        return fileQueryMapper.findActiveFileIdsByBlockId(blockId);
    }

    @Override
    public Optional<Long> findStepIdByFileIdIncludingDeletedBlock(Long fileId) {
        return Optional.ofNullable(fileQueryMapper.findStepIdByFileIdIncludingDeletedBlock(fileId));
    }

    @Override
    public List<FileVersionProjection> findCompletedVersions(Long fileId) {
        return fileQueryMapper.findCompletedVersions(fileId);
    }

    @Override
    public int findMaxCompletedVersionNo(Long fileId) {
        return fileQueryMapper.findMaxCompletedVersionNo(fileId);
    }

    @Override
    public List<BlockFileProjection> findBlockFiles(Long blockId, boolean deleted) {
        return fileQueryMapper.findBlockFiles(blockId, deleted);
    }

    @Override
    public List<ProjectFileVersionProjection> findProjectFileVersions(Long projectId) {
        return fileQueryMapper.findProjectFileVersions(projectId);
    }

    @Override
    public List<ProjectFileProjection> findProjectFiles(Long projectId) {
        return fileQueryMapper.findProjectFiles(projectId);
    }

    @Override
    public List<ProjectTrashFileProjection> findProjectTrashFiles(Long projectId) {
        return fileQueryMapper.findProjectTrashFiles(projectId);
    }

    @Override
    public long countCompanyFiles(CompanyFileCriteria criteria) {
        return fileQueryMapper.countCompanyFiles(criteria);
    }

    @Override
    public List<FileViewProjection> findCompanyFiles(CompanyFileCriteria criteria) {
        return fileQueryMapper.findCompanyFiles(criteria);
    }

    @Override
    public List<FileViewProjection> findMyProjectFiles(MyProjectFileCriteria criteria) {
        return fileQueryMapper.findMyProjectFiles(criteria);
    }

    // ─── 전사 파일 트리 탐색(§14) ───

    @Override
    public List<AdminTreeProjectProjection> findAdminTreeProjects(long companyId, int limit, long offset) {
        return fileQueryMapper.findAdminTreeProjects(companyId, limit, offset);
    }

    @Override
    public long countAdminTreeProjects(long companyId) {
        return fileQueryMapper.countAdminTreeProjects(companyId);
    }

    @Override
    public boolean existsProjectInCompany(long companyId, Long projectId) {
        return fileQueryMapper.existsProjectInCompany(companyId, projectId);
    }

    @Override
    public List<AdminTreeStageProjection> findAdminTreeStages(long companyId, Long projectId) {
        return fileQueryMapper.findAdminTreeStages(companyId, projectId);
    }

    @Override
    public boolean existsUnassignedStep(long companyId, Long projectId) {
        return fileQueryMapper.existsUnassignedStep(companyId, projectId);
    }

    @Override
    public List<AdminTreeStepProjection> findAdminTreeSteps(long companyId, Long projectId, Long stageId) {
        return fileQueryMapper.findAdminTreeSteps(companyId, projectId, stageId);
    }

    @Override
    public boolean existsStepInCompany(long companyId, Long stepId) {
        return fileQueryMapper.existsStepInCompany(companyId, stepId);
    }

    @Override
    public List<FileViewProjection> findAdminTreeStepFiles(long companyId, Long stepId, int limit, long offset) {
        return fileQueryMapper.findAdminTreeStepFiles(companyId, stepId, limit, offset);
    }

    @Override
    public long countAdminTreeStepFiles(long companyId, Long stepId) {
        return fileQueryMapper.countAdminTreeStepFiles(companyId, stepId);
    }
}
