package com.group3.vitamins.file.infrastructure.adapter;

import com.group3.vitamins.file.application.port.ApprovalLockQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** {@link ApprovalLockQueryPort} MyBatis 구현. 결재 테이블을 읽기 전용으로 조인만 한다(§5). */
@Component
@RequiredArgsConstructor
public class ApprovalLockQueryAdapter implements ApprovalLockQueryPort {

    private final ApprovalLockMapper approvalLockMapper;

    @Override
    public Optional<InProgressApproval> findInProgressApproval(Long fileId) {
        return Optional.ofNullable(approvalLockMapper.findInProgressApproval(fileId));
    }

    @Override
    public boolean existsAnyApprovalReference(Long fileId) {
        return approvalLockMapper.existsAnyApprovalReference(fileId);
    }
}
