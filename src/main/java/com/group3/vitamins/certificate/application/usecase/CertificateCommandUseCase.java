package com.group3.vitamins.certificate.application.usecase;

import com.group3.vitamins.certificate.application.command.CreateCertificateCommand;
import com.group3.vitamins.certificate.application.command.DeleteCertificateCommand;
import com.group3.vitamins.certificate.application.command.UpdateCertificateCommand;
import com.group3.vitamins.certificate.application.result.CertificateResult;

/** 자격증 마스터 쓰기 인바운드 포트 (생성·수정·삭제). ADMIN 전용. */
public interface CertificateCommandUseCase {

    CertificateResult create(CreateCertificateCommand command);

    CertificateResult update(UpdateCertificateCommand command);

    /** hard delete + 참조 차단(MAJ-002). */
    void delete(DeleteCertificateCommand command);
}
