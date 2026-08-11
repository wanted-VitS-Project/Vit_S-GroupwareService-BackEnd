package com.group3.vitamins.project.application.result;

/**
 * @param deleted 그 사원이 삭제됐는지. 삭제됐어도 이름은 그대로 담는다 (DELETE.md D-6)
 */
public record MemberBrief(String userId, String name, boolean deleted) {
}
