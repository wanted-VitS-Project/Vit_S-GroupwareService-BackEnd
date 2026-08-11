package com.group3.vitamins.project.step.application.result;

/**
 * 사번·이름 쌍. 책임자·완료자에 함께 쓴다.
 *
 * @param deleted 그 사원이 논리 삭제됐는지. 이 경우에도 이름은 그대로 담는다 (DELETE.md D-6)
 */
public record StepPerson(String userId, String name, boolean deleted) {
}
