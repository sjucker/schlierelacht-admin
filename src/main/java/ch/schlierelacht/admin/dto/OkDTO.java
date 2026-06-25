package ch.schlierelacht.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OkDTO(@NotNull List<OkMemberDTO> members,
                    @NotNull List<OkTeamMemberDTO> teamMembers) {}
