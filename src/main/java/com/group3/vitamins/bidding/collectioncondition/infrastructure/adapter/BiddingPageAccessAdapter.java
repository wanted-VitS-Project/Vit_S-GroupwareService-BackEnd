package com.group3.vitamins.bidding.collectioncondition.infrastructure.adapter;

import com.group3.vitamins.bidding.collectioncondition.application.port.BiddingPageAccessPort;
import com.group3.vitamins.pagepermission.application.port.PagePermissionRepository;
import com.group3.vitamins.pagepermission.domain.model.PageCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BiddingPageAccessAdapter implements BiddingPageAccessPort {

    private static final String ADMIN = "ADMIN";
    private static final String MASTER = "MASTER";

    private final PagePermissionRepository pagePermissionRepository;

    // 전역 관리자 또는 BIDDING 권한을 명시적으로 받은 사용자만 허용합니다.
    @Override
    public boolean hasAccess(String userId, String role) {
        if (ADMIN.equals(role) || MASTER.equals(role)) {
            return true;
        }

        return pagePermissionRepository.findGrantedLevels(userId)
                .containsKey(PageCode.BIDDING);
    }
}
