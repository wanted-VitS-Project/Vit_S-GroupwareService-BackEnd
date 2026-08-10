package com.group3.vitamins.bidding.collectioncondition.application.port;

public interface BiddingPageAccessPort {

    // 현재 사용자가 입찰 관리 페이지에 접근할 수 있는지 확인합니다.
    boolean hasAccess(String userId, String role);
}
