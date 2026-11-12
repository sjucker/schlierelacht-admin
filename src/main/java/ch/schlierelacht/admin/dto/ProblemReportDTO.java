package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * A problem a visitor reports from the app ("Problem melden"), e.g. an overflowing toilet or unsecured cables.
 * Sent as the {@code report} part of a multipart request, next to an optional {@code photo} part. Not stored: it is
 * mailed to {@code app.problem-report-email}.
 */
public record ProblemReportDTO(@NotNull String name,
                               @NotNull String description,
                               String phone,
                               Double latitude,
                               Double longitude) {
}
