package com.group3.vitamins.project.application.port;

import com.group3.vitamins.project.application.result.BusinessCategorySummary;
import com.group3.vitamins.project.domain.model.MemberPermission;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 프로젝트 상세 1건을 스텝 집계·카테고리·요청자 권한까지 한 번에 조회하는 아웃바운드 포트. */
public interface ProjectDetailQueryPort {

    /** 없거나 논리 삭제된 프로젝트면 비어 있다 (404 판정은 호출부). */
    Optional<ProjectDetailView> findDetail(Long projectId, String requesterUserId, Long companyId);

    record ProjectDetailView(
            Long projectId,
            String name,
            String description,
            String clientName,
            String status,
            LocalDate startedOn,
            LocalDate endedOn,
            BigDecimal contractAmount,
            int stepCount,
            int doneStepCount,
            Long bidNoticeId,
            String closeReasonCode,
            String closeReasonNote,
            /** 참여자 행이 없으면 null — 403 판정은 호출부(ProjectAccessPolicy)가 한다. */
            MemberPermission memberPermission,
            LocalDateTime createdAt,
            /**
             * 🚨 조회 응답에 반드시 실어 보낸다 — 프론트가 수정·상태변경 요청에 넣을 값이
             * 여기서만 나온다. 빠뜨리면 <b>모든 저장이 409</b> 인데 컴파일·테스트는 통과한다
             * (`.ai/docs/global/CONCURRENCY.md` §6-3).
             */
            int version,
            List<BusinessCategorySummary> businessCategories
    ) {}
}