package com.group3.vitamins.employee.application.result;

/**
 * 사원 등록 결과 (`employee.md` §3). 응답 필드 {@code userId·name·emailRegistered·emailSent} 그대로.
 *
 * <p>{@code emailRegistered} = 이메일을 받았는가(없으면 로그인 불가), {@code emailSent} = 초기 비밀번호 메일이
 * 실제로 나갔는가. 메일 발송이 실패해도 등록은 성공(201)이며 {@code emailSent=false} 로 프론트가 재설정을 유도한다.
 */
public record EmployeeRegisterResult(
        String userId,
        String name,
        boolean emailRegistered,
        boolean emailSent
) {
}
