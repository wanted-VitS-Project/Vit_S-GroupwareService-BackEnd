package com.group3.vitamins.file.application.port;

// Cleans derived data owned by other domains before a file is permanently deleted.
public interface FileDerivedDataCleanupPort {

    void cleanupByFileId(Long fileId);
}
