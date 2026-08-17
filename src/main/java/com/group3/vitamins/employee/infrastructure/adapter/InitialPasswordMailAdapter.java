package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.account.application.port.PasswordResetMailPort;
import com.group3.vitamins.employee.application.port.InitialPasswordMailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link InitialPasswordMailPort} 어댑터. 초기 비밀번호 메일은 비밀번호 재설정과 발송 문구·경로가 같아
 * account 의 {@link PasswordResetMailPort} 를 그대로 재사용한다 (로직 복제 금지 — 아키텍처 §2-2).
 */
@Component
@RequiredArgsConstructor
public class InitialPasswordMailAdapter implements InitialPasswordMailPort {

    private final PasswordResetMailPort passwordResetMailPort;

    @Override
    public void sendInitialPassword(String toEmail, String userId, String name, String rawPassword) {
        passwordResetMailPort.sendTempPassword(toEmail, userId, name, rawPassword);
    }
}
