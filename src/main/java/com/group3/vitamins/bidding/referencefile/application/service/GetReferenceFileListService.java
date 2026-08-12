package com.group3.vitamins.bidding.referencefile.application.service;

import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.referencefile.application.query.GetReferenceFileListQuery;
import com.group3.vitamins.bidding.referencefile.application.result.ReferenceFileListResult;
import com.group3.vitamins.bidding.referencefile.application.result.ReferenceFileListResult.Item;
import com.group3.vitamins.bidding.referencefile.application.usecase.GetReferenceFileListUseCase;
import com.group3.vitamins.bidding.referencefile.domain.model.BidReferenceFile;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import lombok.RequiredArgsConstructor;
import com.group3.vitamins.bidding.referencefile.domain.repository.BidReferenceFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetReferenceFileListService implements GetReferenceFileListUseCase {

    private final BidReferenceFileRepository referenceFileRepository;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public ReferenceFileListResult get(GetReferenceFileListQuery query) {
        biddingAccessPolicy.assertAccess(query.userId(), query.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        return new ReferenceFileListResult(
                referenceFileRepository.findAllActiveByCompanyId(companyId).stream()
                        .map(this::toItem)
                        .toList()
        );
    }

    private Item toItem(BidReferenceFile file) {
        return new Item(
                file.referenceFileId(),
                file.fileName(),
                file.extension(),
                file.mimeType(),
                file.sizeBytes(),
                file.uploadStatus().name(),
                file.indexStatus().name(),
                file.selectable(),
                file.createdAt()
        );
    }
}