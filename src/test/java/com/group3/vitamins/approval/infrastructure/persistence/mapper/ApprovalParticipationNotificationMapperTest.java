package com.group3.vitamins.approval.infrastructure.persistence.mapper;

import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalStepEditorRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@MybatisTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:approval-participation-notification;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "mybatis.mapper-locations=classpath:mapper/approval/ApprovalParticipationNotificationMapper.xml"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.group3.vitamins.approval.infrastructure.persistence.mapper")
@Sql(scripts = "/sql/approval-participation-notification.sql",
        config = @SqlConfig(encoding = "UTF-8"))
@DisplayName("ApprovalParticipationNotificationMapper — 유효 스텝 EDITOR 조회")
class ApprovalParticipationNotificationMapperTest {

    @Autowired
    private ApprovalParticipationNotificationMapper mapper;

    @Test
    @DisplayName("활성 MEMBER 중 유효 권한이 EDITOR인 사람만 알림 대상으로 조회한다")
    void findsOnlyActiveEligibleStepEditors() {
        List<ApprovalStepEditorRow> rows = mapper.findActiveStepEditors(List.of(10L), 1L);

        assertThat(rows)
                .extracting(ApprovalStepEditorRow::blockId, ApprovalStepEditorRow::userId)
                .containsExactly(
                        tuple(10L, "EMP_ELIGIBLE"),
                        tuple(10L, "EMP_STEP_EDITOR"));
    }
}
