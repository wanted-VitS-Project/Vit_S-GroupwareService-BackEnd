package com.group3.vitamins.project.stage.application.result;

/** 스테이지 삭제 결과. moveToStageId 가 null 이면 하위 스텝을 미소속으로 뺐다는 뜻이다. */
public record StageDeleteResult(Long deletedStageId, int movedStepCount, Long moveToStageId) {
}
