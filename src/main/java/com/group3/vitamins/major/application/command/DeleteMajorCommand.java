package com.group3.vitamins.major.application.command;

/** 전공 삭제 커맨드. */
public record DeleteMajorCommand(Long majorId, String role) {
}
