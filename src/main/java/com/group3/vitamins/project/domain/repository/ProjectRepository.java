package com.group3.vitamins.project.domain.repository;

import com.group3.vitamins.project.domain.model.CloseReasonCode;
import com.group3.vitamins.project.domain.model.Project;
import com.group3.vitamins.project.domain.model.ProjectStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ProjectRepository {
    Project save(Project project);

    /** 같은 회사에서 같은 공고로 이미 만들어진 프로젝트가 있는지 확인한다 (`uk_project_bid_notice_company`). */
    Optional<Project> findByBidNoticeId(Long bidNoticeId, Long companyId);

    /** 논리 삭제되지 않은 프로젝트를 회사 범위로 조회한다. 타사 프로젝트는 조회되지 않아 404 로 귀결된다. */
    Optional<Project> findById(Long projectId, Long companyId);

    /**
     * 기대 버전과 DB 버전이 같을 때만 수정 6필드를 덮어쓰고 version 을 올린다.
     * 바뀐 행 수를 돌려준다 — <b>0 이면 그 사이 남이 먼저 저장한 것이다(충돌)</b>.
     *
     * <p>⚠️ {@code save()} 로 대체하지 마라. 검사와 저장이 한 문장 안에서 원자적으로 일어나야
     * 조회~저장 사이의 갱신 유실을 막는다 (`.ai/docs/global/CONCURRENCY.md` §1-3 · §6-4).
     */
    int updateIfVersionMatches(Long projectId, Long companyId, String name, String description,
                               String clientName, LocalDate startedOn, LocalDate endedOn,
                               BigDecimal contractAmount, LocalDateTime updatedAt,
                               int expectedVersion);

    /**
     * 기대 버전이 같을 때만 상태를 바꾼다. 0 이면 충돌이다.
     *
     * <p>종결 정보는 <b>도메인이 계산한 값을 그대로</b> 넘긴다 — CLOSED 이탈 시 null 로 지우는 규칙이
     * 도메인에 있어서, SQL 에 같은 조건을 다시 쓰면 규칙이 두 곳으로 갈라진다.
     */
    int changeStatusIfVersionMatches(Long projectId, Long companyId, ProjectStatus status,
                                     CloseReasonCode closeReasonCode, String closeReasonNote,
                                     LocalDateTime closedAt, LocalDateTime updatedAt,
                                     int expectedVersion);
}