package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

public record OkTeamMemberDTO(@NotNull String name,
                              @NotNull OkTeam team,
                              String email) {}
