package com.group3.vitamins.file.application.port;

import com.group3.vitamins.file.application.result.BlockFileProjection;
import com.group3.vitamins.file.application.result.FileVersionProjection;

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

    /** 문서의 완료된 버전 목록(§8 이력) — 차수 내림차순. 실패·미완료 버전은 제외. */
    List<FileVersionProjection> findCompletedVersions(Long fileId);

    /**
     * 문서의 완료(COMPLETED) 버전 기준 최대 차수(§11 latest 판정용). 완료 버전이 없으면 0.
     * §8 이력과 같은 정의를 써서 두 API 의 latest 판정이 어긋나지 않게 한다.
     */
    int findMaxCompletedVersionNo(Long fileId);

    /** 블록의 파일 목록(§3) — 문서별 최신 완료 버전 + 버전 수. deleted=true 면 휴지통, false 면 재직 문서. 연결일 오름차순. */
    List<BlockFileProjection> findBlockFiles(Long blockId, boolean deleted);
}
