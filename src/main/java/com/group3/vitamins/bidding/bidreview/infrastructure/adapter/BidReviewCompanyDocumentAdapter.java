package com.group3.vitamins.bidding.bidreview.infrastructure.adapter;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewCompanyDocumentPort;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentReferenceView;
import com.group3.vitamins.companydocument.application.usecase.CompanyDocumentReferenceUseCase;
import com.group3.vitamins.file.application.port.FileStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BidReviewCompanyDocumentAdapter implements BidReviewCompanyDocumentPort {

    private final CompanyDocumentReferenceUseCase referenceUseCase;
    private final FileStoragePort fileStoragePort;

    @Override
    public List<CompanyDocumentReferenceSnapshot> findAccessibleDocuments(
            List<Long> companyDocumentVersionIds
    ) {
        if (companyDocumentVersionIds == null || companyDocumentVersionIds.isEmpty()) {
            return List.of();
        }

        List<CompanyDocumentReferenceSnapshot> snapshots = new ArrayList<>();
        for (Long versionId : companyDocumentVersionIds) {
            referenceUseCase.getSelectableVersion(versionId)
                    .map(view -> new CompanyDocumentReferenceSnapshot(
                            view.companyDocumentVersionId(),
                            view.originalFileName()
                    ))
                    .ifPresent(snapshots::add);
        }

        return snapshots;
    }

    @Override
    public List<DownloadableCompanyDocument> findDownloadableDocuments(
            Long companyId,
            List<Long> companyDocumentVersionIds
    ) {
        if (companyDocumentVersionIds == null || companyDocumentVersionIds.isEmpty()) {
            return List.of();
        }

        List<DownloadableCompanyDocument> downloads = new ArrayList<>();
        for (Long versionId : companyDocumentVersionIds) {
            Optional<CompanyDocumentReferenceView> view =
                    referenceUseCase.getSelectableVersion(versionId, companyId);

            view.map(this::toDownloadable).ifPresent(downloads::add);
        }

        return downloads;
    }

    private DownloadableCompanyDocument toDownloadable(CompanyDocumentReferenceView view) {
        String downloadUrl = fileStoragePort
                .presignDownload(view.storageKey(), view.originalFileName())
                .url();

        return new DownloadableCompanyDocument(
                view.companyDocumentVersionId(),
                view.originalFileName(),
                downloadUrl
        );
    }
}
