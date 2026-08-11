package com.group3.vitamins.project.block.application.result;

/**
 * 블록 담당자. 미지정이면 이 객체 자체가 null 이다.
 *
 * @param deleted 담당자로 지정된 사원이 논리 삭제됐는지. 이 경우에도 이름은 그대로 담는다 —
 *                담당자를 지우면 "원래 없었다" 와 구분이 안 된다 (DELETE.md D-6)
 */
public record BlockOwner(String userId, String name, boolean deleted) {
}
