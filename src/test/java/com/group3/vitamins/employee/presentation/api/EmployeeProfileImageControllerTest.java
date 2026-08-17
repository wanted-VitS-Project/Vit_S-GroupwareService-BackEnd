package com.group3.vitamins.employee.presentation.api;

import com.group3.vitamins.employee.application.usecase.ProfileImageUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("EmployeeProfileImageController — 302 redirect + 짧은 max-age 캐시")
class EmployeeProfileImageControllerTest {

    private final ProfileImageUseCase profileImageUseCase = Mockito.mock(ProfileImageUseCase.class);
    private final EmployeeProfileImageController controller =
            new EmployeeProfileImageController(profileImageUseCase);

    @Test
    @DisplayName("presigned URL 로 302 redirect 하고 Cache-Control 은 max-age=300, private 다")
    void redirectsWithShortMaxAge() {
        when(profileImageUseCase.resolveViewUrl("vitas-EMP001"))
                .thenReturn("https://s3.example/presigned?sig=abc");

        ResponseEntity<Void> response = controller.serve("vitas-EMP001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .hasToString("https://s3.example/presigned?sig=abc");
        // no-store 였다면 브라우저가 매번 왕복 → 깜빡임. 짧은 max-age 로 왕복을 줄이되 presigned(1시간)보다 짧게.
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("max-age=300, private");
    }
}
