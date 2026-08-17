package com.group3.vitamins.approval.infrastructure.catalog;

import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.BlockSummary;
import com.group3.vitamins.approval.infrastructure.persistence.mapper.ApprovalBlockAccessMapper;
import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalBlockPermissionRow;
import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalBlockSummaryRow;
import com.group3.vitamins.project.domain.repository.ProjectMemberRepository;
import com.group3.vitamins.project.domain.model.MemberPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Block/Project 도메인(동훈님 소관) 실 연동.
 *
 * <p>2026-08-16 — 리포지토리 다중 호출에서 <b>MyBatis 조인 조회</b>로 바꿨다
 * ({@link ApprovalBlockAccessMapper}). 포트 메서드의 시그니처·반환 규칙은 하나도 바뀌지 않는다 —
 * 판정 결과는 그대로고 그 결과를 얻는 쿼리 수만 줄었다.
 *
 * <p>왜 바꿨나: {@code block.project_id} 가 폐기돼 {@code step} 을 거쳐야 해서 블록 조회 1회가
 * 실제로는 쿼리 2발이었고, 그게 결재 조회 권한 판정 한 번에 3번 반복돼 <b>8발</b>이 나갔다.
 * 파생 쿼리라 JPA 1차 캐시로도 안 접힌다.
 *
 * <table>
 *   <tr><th>메서드</th><th>이전</th><th>지금</th></tr>
 *   <tr><td>{@code findBlock}</td><td>2 (block + step)</td><td>1</td></tr>
 *   <tr><td>{@code isBlockInCompany}</td><td>3 (+ project)</td><td>1</td></tr>
 *   <tr><td>{@code canViewBlock}/{@code isStepEditor}</td><td>4 (+ member + override)</td><td>1</td></tr>
 * </table>
 *
 * <p>{@code isProjectMember} 만 리포지토리를 그대로 쓴다 — 원래 1쿼리라 접을 것이 없다.
 */
@Component
@RequiredArgsConstructor
public class ApprovalBlockCatalogAdapter implements BlockCatalogPort {

    private final ApprovalBlockAccessMapper blockAccessMapper;
    private final ProjectMemberRepository projectMemberRepository;

    /**
     * {@code block.project_id} 컬럼이 없어(폐기됨) {@code step}을 거쳐야 projectId를 얻는다.
     *
     * <p>🚨 <b>JPA 로 쓰고 MyBatis 로 읽는 유일한 지점이다 — 안전한 이유가 바뀌었으니 읽어라.</b>
     * {@code ApprovalHandlerService.create()} 가 <b>블록 생성과 같은 트랜잭션</b>에서 이걸 부른다
     * (APR-001 — {@code BlockCommandService.createBlock()} → {@code BlockDetailPort.createDetail()}).
     *
     * <ul>
     *   <li><b>예전(JPA 조회)</b>: Hibernate 가 JPQL 실행 전에 <b>자동 flush</b> 해서 방금 INSERT 한
     *       블록이 보였다. ID 생성 전략과 무관하게 안전했다.</li>
     *   <li><b>지금(MyBatis 조회)</b>: 자동 flush 가 <b>없다</b>. 그런데도 보이는 이유는
     *       {@code BlockJpaEntity} 가 {@code GenerationType.IDENTITY}({@code block_id AUTO_INCREMENT})라
     *       {@code save()} 시점에 INSERT 가 <b>즉시 실행</b>되기 때문이다. 같은 트랜잭션·같은 커넥션을
     *       공유하므로({@code application.yml} MyBatis 주석) MyBatis 도 그 행을 본다.</li>
     * </ul>
     *
     * <p>⚠️ 즉 <b>블록 PK 생성 전략을 IDENTITY 에서 시퀀스·테이블 방식으로 바꾸면 결재 블록 생성이
     * 깨진다</b> — INSERT 가 커밋 직전으로 미뤄져 여기서 블록을 못 찾고
     * {@code "block not found right after creation"} 으로 죽는다. 컴파일도 되고 다른 테스트도 다 통과한다
     * (생성 경로 테스트가 이 포트를 mock 한다). 스키마는 Flyway 가 정본이고 {@code ddl-auto: validate}
     * 라 조용히 바뀌지는 않지만, 바꾸려면 여기부터 확인할 것.
     */
    @Override
    public Optional<BlockSummary> findBlock(Long blockId) {
        return blockAccessMapper.findBlockSummary(blockId).map(this::toSummary);
    }

    private BlockSummary toSummary(ApprovalBlockSummaryRow row) {
        return new BlockSummary(row.blockId(), row.blockType(), row.stepId(),
                row.projectId(), row.createdBy());
    }

    /** 참여자 행이 있으면(권한 레벨 무관) member로 본다 — APR-012는 EDITOR까지 요구하지 않는다 */
    @Override
    public boolean isProjectMember(Long projectId, String userId) {
        return projectMemberRepository.findPermission(projectId, userId).isPresent();
    }

    @Override
    public boolean isBlockInCompany(Long blockId, Long companyId) {
        return blockAccessMapper.existsBlockInCompany(blockId, companyId).isPresent();
    }

    /**
     * 쓰기 판정 — 결재 수정·상신·대행 기안자 선점의 진입 조건이다.
     *
     * <p>⚠️ <b>{@code ADMIN} 은 여기서 계속 {@code false} 다.</b> 2026-08-17 에 열린 것은
     * {@link #canViewBlock} 뿐이다. 두 메서드가 나란히 있어 한쪽만 고치면 대칭이 맞아 보여서
     * 무심코 여기까지 열기 쉬운데, 그러면 인사 role 이 남의 결재를 <b>대행 상신</b>할 수 있게 된다
     * (컴파일도 되고 조회 테스트도 다 통과한다).
     */
    @Override
    public boolean isStepEditor(Long blockId, String userId, String role) {
        if ("ADMIN".equals(role)) {
            return false;
        }
        if ("MASTER".equals(role)) {
            return true;
        }
        return effectivePermission(blockId, userId) == MemberPermission.EDITOR;
    }

    /**
     * 열람 판정 — {@code ADMIN} 은 2026-08-17 부터 스텝 참여와 무관하게 통과한다(인사 담당의 결재 현황 열람).
     *
     * <p>회사 경계는 여기서 보지 않는다. 호출자({@code ApprovalViewPolicy.assertSameCompany})가 role 검사보다
     * <b>먼저</b> 확인하므로 타 회사 ADMIN 은 여기 도달하지 못한다.
     */
    @Override
    public boolean canViewBlock(Long blockId, String userId, String role) {
        if ("MASTER".equals(role) || "ADMIN".equals(role)) {
            return true;
        }
        return effectivePermission(blockId, userId) != MemberPermission.NONE;
    }

    /**
     * 블록이 속한 스텝의 유효 권한을 계산한다 — 프로젝트 권한이 {@code NONE}(미참여 포함)이면
     * 스텝 오버라이드를 보지 않고 {@code NONE}, 아니면 오버라이드 우선·없으면 프로젝트 권한 상속.
     * {@code StepAccessPolicy.resolve()} 와 같은 규칙이며, 블록을 못 찾으면 {@code NONE} 이다.
     *
     * <p>⚠️ <b>미참여({@code projectPermission == null})와 명시적 {@code NONE} 은 같은 결과다.</b>
     * 둘을 갈라 오버라이드를 먼저 보게 만들면, 프로젝트에 없는 사람이 스텝 오버라이드만으로 들어온다.
     */
    private MemberPermission effectivePermission(Long blockId, String userId) {
        return blockAccessMapper.findBlockPermission(blockId, userId)
                .map(this::resolve)
                .orElse(MemberPermission.NONE);
    }

    private MemberPermission resolve(ApprovalBlockPermissionRow row) {
        MemberPermission projectPermission = row.projectPermission() == null
                ? MemberPermission.NONE : row.projectPermission();
        if (projectPermission == MemberPermission.NONE) {
            return MemberPermission.NONE;
        }
        return row.stepPermission() == null ? projectPermission : row.stepPermission();
    }
}
