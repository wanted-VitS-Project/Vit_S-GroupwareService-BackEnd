package com.group3.vitamins.certificate.application.command;

/** 자격증 삭제 커맨드. */
public record DeleteCertificateCommand(Long certificateId, String role) {
}
