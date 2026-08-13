package com.group3.vitamins.file.application.usecase;

import com.group3.vitamins.file.application.query.CompanyFileQuery;
import com.group3.vitamins.file.application.query.MyProjectFileQuery;
import com.group3.vitamins.file.application.result.CompanyFilePageResult;
import com.group3.vitamins.file.application.result.FileViewResult;

import java.util.List;

/**
 * 파일 관리 화면 조회 인바운드 포트 (FILE-V1 §2-H · FILE-Q). 여러 프로젝트에 걸친 파일 모아보기 2종.
 *
 * <p>기존 {@code FileQueryUseCase}(단일 프로젝트/블록/버전 조회)와 분리한다 — 권한 축과 조회 형태가 다르다
 * (전사=ADMIN 페이지, 내프로젝트=멤버십+스텝 권한 리스트).
 */
public interface FileListViewUseCase {

    /** 전사 파일 관리(FILE-Q-01) — ADMIN 전용, 회사 스코프, 페이지네이션·필터. */
    CompanyFilePageResult getCompanyFiles(CompanyFileQuery query);

    /** 내 프로젝트 파일 모아보기(FILE-Q-03) — 멤버 프로젝트의 파일 중 스텝 VIEWER 이상만. 프로젝트별 정렬. */
    List<FileViewResult> getMyProjectFiles(MyProjectFileQuery query);
}
