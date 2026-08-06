package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;

// Verifies that the mapper interface and XML statements are loaded together.
@MybatisTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vitamate-analysis-mapper;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "mybatis.mapper-locations=classpath:mapper/vitamate/VitamateAnalysisMapper.xml"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.group3.vitamins.vitamate.analysis.infrastructure.persistence.mapper")
@DisplayName("VitamateAnalysisMapper XML loading")
class VitamateAnalysisMapperContextTest {

    @Autowired
    private VitamateAnalysisMapper mapper;

    @Test
    @DisplayName("loads mapper bean from XML configuration")
    void loadsMapperBean() {
        assertThat(mapper).isNotNull();
    }
}
