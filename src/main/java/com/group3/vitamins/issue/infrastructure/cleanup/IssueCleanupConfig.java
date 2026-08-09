package com.group3.vitamins.issue.infrastructure.cleanup;

import com.group3.vitamins.global.application.cleanup.DefaultHardDeleteTarget;
import com.group3.vitamins.global.application.cleanup.port.HardDeleteTarget;
import com.group3.vitamins.issue.infrastructure.persistence.SpringDataIssueRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Period;

/**
 * {@code global} 하드 딜리트 SPI({@code .ai/docs/global/CLEANUP.md} §2)에 대한 시험 적용.
 * 소프트 삭제된 issue는 issue_assign·issue_block에 CASCADE가 걸려 있어 참조 확인 없이 §2-1(단순 리포지토리 참조)로 등록한다.
 */
@Configuration
public class IssueCleanupConfig {

    /** 시험 적용용 임시값 — 실제 보존기간은 팀 정책 확정 후 교체한다. */
    private static final Period RETENTION = Period.ofMonths(6);

    @Bean
    public HardDeleteTarget issueHardDeleteTarget(SpringDataIssueRepository repository) {
        return new DefaultHardDeleteTarget(
                "issue",
                RETENTION,
                repository::hardDeleteByDeletedAtBefore
        );
    }
}
