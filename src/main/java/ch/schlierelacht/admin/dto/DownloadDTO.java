package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DownloadDTO(@NotNull Long id,
                          @NotNull String filename,
                          @NotNull String filetype,
                          @NotNull String description,
                          @NotNull long filesize,
                          @NotNull DownloadCategory category,
                          @NotNull LocalDateTime uploadedAt) {
}
