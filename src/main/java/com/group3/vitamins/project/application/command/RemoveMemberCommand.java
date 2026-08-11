package com.group3.vitamins.project.application.command;

public record RemoveMemberCommand(
        Long projectId,
        Long memberId,
        String requesterUserId,
        String role
) {}