package com.group3.vitamins.issue.infrastructure.cleanup;

import com.group3.vitamins.global.application.cleanup.port.HardDeleteTarget;
import com.group3.vitamins.issue.infrastructure.persistence.SpringDataIssueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Period;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("IssueCleanupConfig")
class IssueCleanupConfigTest {

    @Test
    @DisplayName("issue 리포지토리의 hardDeleteByDeletedAtBefore를 그대로 위임하는 HardDeleteTarget을 만든다")
    void createsTargetDelegatingToRepository() {
        SpringDataIssueRepository repository = mock(SpringDataIssueRepository.class);
        LocalDateTime threshold = LocalDateTime.of(2026, 2, 8, 3, 0);
        when(repository.hardDeleteByDeletedAtBefore(threshold)).thenReturn(4);

        HardDeleteTarget target = new IssueCleanupConfig().issueHardDeleteTarget(repository);

        assertThat(target.targetName()).isEqualTo("issue");
        assertThat(target.retention()).isEqualTo(Period.ofMonths(6));
        assertThat(target.hardDeleteBefore(threshold)).isEqualTo(4);
        verify(repository).hardDeleteByDeletedAtBefore(threshold);
    }
}
