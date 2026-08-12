package com.group3.vitamins.file.application.service;

import com.group3.vitamins.file.application.policy.FileAdminPolicy;
import com.group3.vitamins.file.application.port.FileQueryPort;
import com.group3.vitamins.file.application.query.CompanyFileCriteria;
import com.group3.vitamins.file.application.query.CompanyFileQuery;
import com.group3.vitamins.file.application.query.MyProjectFileCriteria;
import com.group3.vitamins.file.application.query.MyProjectFileQuery;
import com.group3.vitamins.file.application.result.CompanyFilePageResult;
import com.group3.vitamins.file.application.result.FileViewResult;
import com.group3.vitamins.file.application.usecase.FileListViewUseCase;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 파일 관리 화면 조회 서비스 (FILE-Q). 읽기 전용.
 *
 * <p>전사 파일 관리는 ADMIN 을 명시 검사하고 회사 스코프로 페이지 조회한다. 내 프로젝트 파일은 멤버십을 항상 걸고,
 * 스텝 VIEWER 이상만 노출한다(B안 — 전역 ADMIN/MASTER 는 스텝 정책상 EDITOR 라 스텝 필터를 스킵).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileListViewService implements FileListViewUseCase {

    /** 페이지 크기 상한 (ProjectQueryService 와 통일). */
    private static final int MAX_PAGE_SIZE = 100;

    /** 전역 관리 role — 스텝 권한 정책상 EDITOR 취급(StepAccessPolicy 와 동일). */
    private static final Set<String> GLOBAL_ADMIN_ROLES = Set.of("ADMIN", "MASTER");

    private final FileQueryPort fileQueryPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final FileAdminPolicy fileAdminPolicy;

    @Override
    public CompanyFilePageResult getCompanyFiles(CompanyFileQuery query) {
        fileAdminPolicy.assertAdmin(query.role());

        long companyId = currentCompanyIdProvider.currentCompanyId();
        int page = Math.max(query.page(), 0);
        int size = Math.min(Math.max(query.size(), 1), MAX_PAGE_SIZE);

        CompanyFileCriteria criteria = new CompanyFileCriteria(
                companyId, blankToNull(query.keyword()), query.projectId(),
                blankToNull(query.extension()), page * size, size);

        long total = fileQueryPort.countCompanyFiles(criteria);
        List<FileViewResult> content = fileQueryPort.findCompanyFiles(criteria).stream()
                .map(p -> FileViewResult.from(p, isPreviewable(p.extension())))
                .toList();

        return new CompanyFilePageResult(content, page, size, total);
    }

    @Override
    public List<FileViewResult> getMyProjectFiles(MyProjectFileQuery query) {
        long companyId = currentCompanyIdProvider.currentCompanyId();
        boolean adminAll = GLOBAL_ADMIN_ROLES.contains(query.role());

        MyProjectFileCriteria criteria = new MyProjectFileCriteria(
                companyId, query.requesterUserId(), adminAll,
                blankToNull(query.keyword()), query.projectId(), blankToNull(query.extension()));

        return fileQueryPort.findMyProjectFiles(criteria).stream()
                .map(p -> FileViewResult.from(p, isPreviewable(p.extension())))
                .toList();
    }

    private boolean isPreviewable(String extension) {
        return "pdf".equalsIgnoreCase(extension);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }
}
