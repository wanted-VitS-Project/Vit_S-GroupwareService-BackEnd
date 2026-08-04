package com.group3.vitamins.text.infrastructure.catalog;

import com.group3.vitamins.text.application.port.BlockCatalogPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class CatalogBlockAdapter implements BlockCatalogPort {

    @Override
    public boolean hasEditPermission(String blockType, Long blockTypeId, String userId) {
        // TODO: 공용 block 테이블 조회 + step_permission/project_member 인프라가 아직 없어 항상 true 를 반환한다.
        return true;
    }

    @Override
    public boolean hasViewPermission(String blockType, Long blockTypeId, String userId) {
        // TODO: 공용 block 테이블 조회 + step_permission/project_member 인프라가 아직 없어 항상 true 를 반환한다.
        return true;
    }

    @Override
    public String getBlockTitle(String blockType, Long blockTypeId) {
        // TODO: 공용 block 테이블 연동이 아직 없어 임시로 null 을 반환한다.
        return null;
    }
}
