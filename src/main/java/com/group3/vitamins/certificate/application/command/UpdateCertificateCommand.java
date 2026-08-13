package com.group3.vitamins.certificate.application.command;

/** 자격증 수정 커맨드. */
public record UpdateCertificateCommand(Long certificateId, String name, String role) {
}
