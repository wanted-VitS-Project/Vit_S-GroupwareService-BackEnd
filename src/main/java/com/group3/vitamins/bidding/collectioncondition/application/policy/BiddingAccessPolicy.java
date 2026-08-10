package com.group3.vitamins.bidding.collectioncondition.application.policy;

import com.group3.vitamins.bidding.collectioncondition.application.port.BiddingPageAccessPort;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BiddingAccessPolicy {

    private final BiddingPageAccessPort biddingPageAccessPort;

    // 입찰 관리 권한이 없으면 유스케이스 실행을 차단합니다.
    public void assertAccess(String userId, String role) {
        if (!biddingPageAccessPort.hasAccess(userId, role)) {
            throw new ForbiddenException(
                    BiddingErrorCode.BIDDING_ACCESS_PERMISSION_REQUIRED
            );
        }
    }
}
