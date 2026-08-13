package com.group3.vitamins.companydocument.infrastructure.adapter;

import com.group3.vitamins.companydocument.application.port.CompanyDocumentIndexTarget;
import com.group3.vitamins.companydocument.application.port.CompanyDocumentIndexTriggerPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 사내 문서 인덱싱 트리거 **스텁** 어댑터 (COMPANY-DOC-V1 §6-2).
 *
 * <p>AI(vitamate) 도메인이 사내 문서 인덱싱 소비 경로를 확정하기 전까지의 no-op 구현이다 —
 * 인덱싱 대상 등록/제외 의도만 로그로 남긴다. AI 도메인이 실제 소비 어댑터를 붙이면 이 스텁을 대체한다.
 * ⚠️ 스텁이라 인덱싱이 실제로 일어나지 않는다 — 완료/삭제 흐름 자체는 정상 동작한다.
 */
@Slf4j
@Component
public class StubCompanyDocumentIndexTriggerAdapter implements CompanyDocumentIndexTriggerPort {

    @Override
    public void triggerIndexing(CompanyDocumentIndexTarget target) {
        log.info("[사내문서 인덱싱 스텁] 버전 {} 인덱싱 대상 등록 요청(companyId={}, s3Key={}) — AI 도메인 소비 대기(§6-2)",
                target.companyDocumentVersionId(), target.companyId(), target.s3Key());
    }

    @Override
    public void triggerRemoval(Long companyDocumentId) {
        log.info("[사내문서 인덱싱 스텁] 문서 {} 인덱스 제외 요청 — AI 도메인 소비 대기(§6-2)", companyDocumentId);
    }

    @Override
    public void triggerReindex(Long companyDocumentId) {
        log.info("[사내문서 인덱싱 스텁] 문서 {} 인덱스 재등록 요청 — AI 도메인 소비 대기(§6-2)", companyDocumentId);
    }
}
