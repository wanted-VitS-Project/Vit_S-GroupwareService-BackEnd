package com.group3.vitamins.file.infrastructure.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:approval-lock-deleted-reference;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "mybatis.mapper-locations=classpath:mapper/file/ApprovalLockMapper.xml"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan(basePackageClasses = ApprovalLockMapper.class)
@Sql("/sql/approval-lock-deleted-reference.sql")
@DisplayName("파일 영구삭제 — 삭제 결재 참조")
class ApprovalLockMapperDeletionReferenceTest {

    @Autowired private ApprovalLockMapper mapper;

    @Test
    @DisplayName("논리 삭제된 결재 문서도 원본 파일 영구삭제를 막는다")
    void deletedApprovalStillBlocksPermanentFileDeletion() {
        assertThat(mapper.existsAnyApprovalReference(1L)).isTrue();
        assertThat(mapper.findInProgressApproval(1L)).isNull();
    }

    @Test
    @DisplayName("기안자가 DRAFT 에서 연결 해제한 문서는 영구삭제를 막지 않는다")
    void manuallyDetachedDocumentDoesNotBlockPermanentFileDeletion() {
        // 회차가 살아 있으면(ar.deleted_at IS NULL) 상위 삭제가 아니라 사용자의 연결 해제다
        assertThat(mapper.existsAnyApprovalReference(2L)).isFalse();
        assertThat(mapper.findInProgressApproval(2L)).isNull();
    }
}
