package com.group3.vitamins.activitylog.application;

import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.activitylog.infrastructure.persistence.ActivityLogEntity;
import com.group3.vitamins.activitylog.infrastructure.persistence.ActivityLogJpaRepository;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.infrastructure.event.SpringDomainEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:activity-log-event;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        ActivityLogEventListener.class,
        ActivityLogWriter.class,
        SpringDomainEventPublisher.class,
        ActivityLogEventListenerTest.TestBeans.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Activity Log 이벤트 수집")
class ActivityLogEventListenerTest {

    @Autowired
    private TestActivityEmitter testActivityEmitter;

    @Autowired
    private ActivityLogJpaRepository activityLogJpaRepository;

    @Test
    @DisplayName("원본 서비스 트랜잭션이 커밋되기 전에 변경 필드별 로그가 저장된다")
    void writesLogsBeforeCommit() {
        testActivityEmitter.emit();

        List<ActivityLogEntity> logs = activityLogJpaRepository.findAll();

        assertThat(logs).hasSize(2);
        assertThat(logs)
                .extracting(ActivityLogEntity::getField)
                .containsExactlyInAnyOrder("title", "rowIndex");
        assertThat(logs)
                .allSatisfy(log -> {
                    assertThat(log.getAct()).isEqualTo(ActivityLogAction.MODIFY);
                    assertThat(log.getBlockId()).isEqualTo(30L);
                    assertThat(log.getResourceId()).isEqualTo(30L);
                    assertThat(log.getUserId()).isEqualTo("EMP001");
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        TestActivityEmitter testActivityEmitter(DomainEventPublisher domainEventPublisher) {
            return new TestActivityEmitter(domainEventPublisher);
        }
    }

    @Service
    static class TestActivityEmitter {

        private final DomainEventPublisher domainEventPublisher;

        TestActivityEmitter(DomainEventPublisher domainEventPublisher) {
            this.domainEventPublisher = domainEventPublisher;
        }

        @Transactional
        void emit() {
            domainEventPublisher.publish(ActivityOccurredEvent.of(
                    ActivityLogAction.MODIFY,
                    30L,
                    30L,
                    "EMP001",
                    List.of(
                            new ActivityFieldChange("title", "제안서", "제안서 작성"),
                            new ActivityFieldChange("rowIndex", "1", "2")
                    )
            ));
        }
    }
}
