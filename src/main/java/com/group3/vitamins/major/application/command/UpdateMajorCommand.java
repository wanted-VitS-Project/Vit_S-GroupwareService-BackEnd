package com.group3.vitamins.major.application.command;

/** 전공 수정 커맨드. */
public record UpdateMajorCommand(Long majorId, String name, String role) {
}
