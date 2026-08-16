package com.group3.vitamins.file.application.service;

import com.group3.vitamins.file.application.policy.FileAdminPolicy;
import com.group3.vitamins.file.application.port.FileQueryPort;
import com.group3.vitamins.file.application.result.AdminTreeProjectPageResult;
import com.group3.vitamins.file.application.result.AdminTreeStageProjection;
import com.group3.vitamins.file.application.result.AdminTreeStepProjection;
import com.group3.vitamins.file.application.result.CompanyFilePageResult;
import com.group3.vitamins.file.application.result.FileViewResult;
import com.group3.vitamins.file.application.usecase.AdminFileTreeUseCase;
import com.group3.vitamins.file.domain.exception.FileErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 전사 파일 트리 탐색 서비스(§14 · ADMIN). 읽기 전용. 노드마다 자식만 얇게 조회한다.
 *
 * <p>ADMIN 을 명시 검사({@link FileAdminPolicy})하고 회사 스코프는 {@link CurrentCompanyIdProvider} 로만 얻는다
 * — 요청 파라미터가 아니라 서버가 주입하므로 사용자가 다른 회사 리소스를 조회할 수 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminFileTreeService implements AdminFileTreeUseCase {

    /** 페이지 크기 상한(FileListViewService 와 통일). */
    private static final int MAX_PAGE_SIZE = 100;

    /** 미분류 버킷(§14.2) — stage 미소속 스텝을 담는 가상 노드. 항상 맨 뒤라 sortOrder 는 최대값. */
    private static final String UNASSIGNED_BUCKET_NAME = "미분류";
    private static final int UNASSIGNED_BUCKET_SORT_ORDER = Integer.MAX_VALUE;

    private final FileQueryPort fileQueryPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final FileAdminPolicy fileAdminPolicy;

    @Override
    public AdminTreeProjectPageResult getProjects(String role, int page, int size) {
        fileAdminPolicy.assertAdmin(role);
        long companyId = currentCompanyIdProvider.currentCompanyId();
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        long total = fileQueryPort.countAdminTreeProjects(companyId);
        // ⚠️ (long) 캐스팅 — page 가 크면 int 곱셈이 음수로 넘쳐 잘못된 OFFSET 이 나간다(FileListViewService 와 동일 함정).
        return new AdminTreeProjectPageResult(
                fileQueryPort.findAdminTreeProjects(companyId, s, (long) p * s), p, s, total);
    }

    @Override
    public List<AdminTreeStageProjection> getStages(String role, Long projectId) {
        fileAdminPolicy.assertAdmin(role);
        long companyId = currentCompanyIdProvider.currentCompanyId();
        requireProject(companyId, projectId);

        List<AdminTreeStageProjection> stages =
                new ArrayList<>(fileQueryPort.findAdminTreeStages(companyId, projectId));
        // stage 미소속 스텝이 하나라도 있으면 맨 뒤에 미분류 버킷(stageId=null)을 덧붙인다(§14.2).
        if (fileQueryPort.existsUnassignedStep(companyId, projectId)) {
            stages.add(new AdminTreeStageProjection(null, UNASSIGNED_BUCKET_NAME, UNASSIGNED_BUCKET_SORT_ORDER));
        }
        return stages;
    }

    @Override
    public List<AdminTreeStepProjection> getSteps(String role, Long projectId, Long stageId) {
        fileAdminPolicy.assertAdmin(role);
        long companyId = currentCompanyIdProvider.currentCompanyId();
        requireProject(companyId, projectId);
        // stageId 가 null 이면 미분류(stage_id IS NULL) 스텝을 조회한다(§14.3).
        return fileQueryPort.findAdminTreeSteps(companyId, projectId, stageId);
    }

    @Override
    public CompanyFilePageResult getStepFiles(String role, Long stepId, int page, int size) {
        fileAdminPolicy.assertAdmin(role);
        long companyId = currentCompanyIdProvider.currentCompanyId();
        if (!fileQueryPort.existsStepInCompany(companyId, stepId)) {
            throw new NotFoundException(FileErrorCode.FILE_STEP_NOT_FOUND);
        }
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        long total = fileQueryPort.countAdminTreeStepFiles(companyId, stepId);
        List<FileViewResult> content =
                fileQueryPort.findAdminTreeStepFiles(companyId, stepId, s, (long) p * s).stream()
                        .map(pr -> FileViewResult.from(pr, isPreviewable(pr.extension())))
                        .toList();
        return new CompanyFilePageResult(content, p, s, total);
    }

    private void requireProject(long companyId, Long projectId) {
        if (!fileQueryPort.existsProjectInCompany(companyId, projectId)) {
            throw new NotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND);
        }
    }

    private boolean isPreviewable(String extension) {
        return "pdf".equalsIgnoreCase(extension);
    }
}
