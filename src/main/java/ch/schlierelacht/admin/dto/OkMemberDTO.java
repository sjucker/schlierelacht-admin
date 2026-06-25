package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

public record OkMemberDTO(@NotNull String name,
                          @NotNull String role,
                          String email,
                          String cloudflareId) {}
