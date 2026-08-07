package com.group3.vitamins.image.application.command;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record CreateImageItemsCommand(
        String userId,
        Long imgBlockId,
        List<MultipartFile> files,
        List<String> captions,
        String role
) {
}
