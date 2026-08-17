package com.group3.vitamins.employee.presentation.api.response;

import com.group3.vitamins.employee.application.result.EmployeeListRow;
import com.group3.vitamins.employee.application.result.EmployeeSearchRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("사원 응답의 profileImageUrl 매핑 — 키 있으면 서빙 경로, 없으면 null")
class EmployeeResponseProfileImageUrlTest {

    @Test
    @DisplayName("검색 응답: 키가 있으면 서빙 경로(우리 경로, presigned 아님)로 내려간다")
    void searchResponseBuildsServingPathWhenKeyPresent() {
        EmployeeSearchRow row = new EmployeeSearchRow(
                "vitas-EMP001", "김민준", "개발팀", "대리", "profile-images/vitas-EMP001/a.png");

        EmployeeSearchResponse response = EmployeeSearchResponse.from(row);

        assertThat(response.profileImageUrl())
                .isEqualTo("/api/v1/employees/vitas-EMP001/profile-image");
    }

    @Test
    @DisplayName("검색 응답: 키가 없으면 profileImageUrl 은 null (프론트가 호출 자체를 건너뛴다)")
    void searchResponseNullWhenNoKey() {
        EmployeeSearchRow row = new EmployeeSearchRow(
                "vitas-EMP007", "김서연", null, null, null);

        assertThat(EmployeeSearchResponse.from(row).profileImageUrl()).isNull();
    }

    @Test
    @DisplayName("목록 응답: 키가 있으면 서빙 경로로 내려간다")
    void summaryResponseBuildsServingPathWhenKeyPresent() {
        EmployeeListRow row = new EmployeeListRow(
                "vitas-EMP001", "홍길동", "hong@vitamins.com",
                "개발팀", "기술본부", "선임", "MEMBER", "ACTIVE", false, null,
                "profile-images/vitas-EMP001/a.png");

        assertThat(EmployeeSummaryResponse.from(row).profileImageUrl())
                .isEqualTo("/api/v1/employees/vitas-EMP001/profile-image");
    }

    @Test
    @DisplayName("목록 응답: 키가 없으면 profileImageUrl 은 null")
    void summaryResponseNullWhenNoKey() {
        EmployeeListRow row = new EmployeeListRow(
                "vitas-EMP001", "홍길동", "hong@vitamins.com",
                "개발팀", "기술본부", "선임", "MEMBER", "ACTIVE", false, null, null);

        assertThat(EmployeeSummaryResponse.from(row).profileImageUrl()).isNull();
    }
}
