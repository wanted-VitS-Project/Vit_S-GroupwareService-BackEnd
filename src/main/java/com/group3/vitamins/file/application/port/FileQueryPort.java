package com.group3.vitamins.file.application.port;

import com.group3.vitamins.file.application.result.BlockFileProjection;
import com.group3.vitamins.file.application.result.FileVersionProjection;
import com.group3.vitamins.file.application.result.ProjectFileProjection;
import com.group3.vitamins.file.application.result.ProjectTrashFileProjection;
import com.group3.vitamins.file.application.result.ProjectFileVersionProjection;

import java.util.List;
import java.util.Optional;

/**
 * 파일 화면용 조회 아웃바운드 포트 (MyBatis). 목록·버전이력 등은 각 엔드포인트 구현 시 확장한다.
 * 구현은 {@code infrastructure/adapter/FileQueryAdapter}.
 */
public interface FileQueryPort {

    /**
     * 블록 안에 같은 표시명의 <b>완료된</b> 문서가 있는지(§1 동명 확인).
     * <b>삭제되지 않은</b> 완료(COMPLETED) 버전을 가진 문서만 센다 — §3 목록(완료본만 표시)과 정합.
     * 완료 전에 만들어져 버려진 file row(UPLOADING/FAILED)나 삭제된 버전(fv.deleted_at)은 이름을 막지 않는다.
     */
    boolean existsActiveNameInBlock(Long blockId, String name);

    /** 문서가 연결된 블록 ID(권한 판정 경로 fileId→block→step). 파일 1 : 블록 1. 링크 없으면 empty. */
    Optional<Long> findBlockIdByFileId(Long fileId);

    /**
     * 문서가 매달린 블록의 스텝 ID — <b>블록이 soft delete 됐어도</b> 돌려준다(§6 복구용).
     * 블록이 삭제돼도 복구는 성공해야 하므로, 삭제된 블록의 스텝으로도 권한을 판정할 수 있어야 한다.
     * block_file 링크가 없으면 empty.
     */
    Optional<Long> findStepIdByFileIdIncludingDeletedBlock(Long fileId);

    /** 문서의 완료된 버전 목록(§8 이력) — 차수 내림차순. 실패·미완료 버전은 제외. */
    List<FileVersionProjection> findCompletedVersions(Long fileId);

    /**
     * 문서의 완료(COMPLETED) 버전 기준 최대 차수(§11 latest 판정용). 완료 버전이 없으면 0.
     * §8 이력과 같은 정의를 써서 두 API 의 latest 판정이 어긋나지 않게 한다.
     */
    int findMaxCompletedVersionNo(Long fileId);

    /** 블록의 파일 목록(§3) — 문서별 최신 완료 버전 + 버전 수. deleted=true 면 휴지통, false 면 재직 문서. 연결일 오름차순. */
    List<BlockFileProjection> findBlockFiles(Long blockId, boolean deleted);

    /**
     * 프로젝트 파일 버전 목록(§11, #138) — 프로젝트에 속한 모든 문서의 완료 버전(과거 버전 포함, 고아 파일 포함, 휴지통 제외).
     * file_index 를 LEFT JOIN 해 indexStatus 를 함께 내려주며, 인덱스 행이 없거나 소프트 삭제된(deleted_at) 경우 'PENDING'.
     * 정렬은 파일(file_id) 오름차순 · 같은 파일 안에서는 차수(version_no) 내림차순(최신 버전 먼저).
     */
    List<ProjectFileVersionProjection> findProjectFileVersions(Long projectId);

    /**
     * 프로젝트 전체 파일 모아보기(§12) — 프로젝트에 속한 <b>활성</b> 문서(file.deleted_at IS NULL)를 문서 단위 최신 완료 버전 1행으로.
     * 완료 버전이 하나도 없는 문서는 제외한다(§3 과 동일). 스텝·블록 위치를 함께 내려주며, 블록이 soft delete 된 고아 파일도 포함한다
     * — 이 경우 blockId·blockTitle 은 null, blockDeleted=true, stepId·stepName 은 삭제된 블록의 step 으로 해석한다.
     * 정렬은 스텝(step_id) → 블록(block_id) → 블록 연결일(linked_at) 오름차순.
     */
    List<ProjectFileProjection> findProjectFiles(Long projectId);

    /**
     * 프로젝트 휴지통 모아보기(§13) — 프로젝트에 속한 <b>휴지통</b> 문서(file.deleted_at IS NOT NULL)를 문서 단위로.
     * 문서 표시정보 + 전체 버전 수 + 최신 버전 원본명/확장자/크기 + 위치(스텝·블록) + 휴지통 진입 시각(deletedAt)을 내린다.
     * 블록도 삭제된 고아 파일 포함(blockId·blockTitle=null, blockDeleted=true). 정렬은 deletedAt 내림차순.
     */
    List<ProjectTrashFileProjection> findProjectTrashFiles(Long projectId);
}
