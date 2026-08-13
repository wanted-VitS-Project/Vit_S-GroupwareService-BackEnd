package com.group3.vitamins.project.application.result;

/**
 * 프로젝트 복제 결과 (PRJ-018).
 *
 * <p>생성 응답({@link ProjectResult})에 <b>복사 수량</b>을 덧붙인 모양이다. 수량을 내리는 이유는
 * 복제 직후 화면이 "스텝 12 · 블록 40개를 복사했습니다" 를 바로 띄우기 위해서다 —
 * 상세로 들어가 눈으로 세게 하면 뭐가 얼마나 복사됐는지 알 방법이 없다.
 */
public record ProjectDuplicateResult(
        ProjectResult project,
        Long sourceProjectId,
        Copied copied,
        Skipped skipped
) {

    public record Copied(int stages, int steps, int blocks) {
    }

    /** 사용자가 만들 수 없는 타입({@code BID_NOTICE})이라 건너뛴 수. 조용히 사라지면 안 된다. */
    public record Skipped(int blocks) {
    }
}
