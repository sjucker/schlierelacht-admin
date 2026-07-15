package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code POST /api/push/register}: an FCM device token plus the
 * platform it came from and an optional locale, sent by the mobile app so the
 * backend can deliver targeted push notifications.
 */
public record PushRegistrationDTO(@NotNull String token,
                                  @NotNull String platform,
                                  String locale) {
}
