package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.OkDTO;
import ch.schlierelacht.admin.dto.OkMemberDTO;
import ch.schlierelacht.admin.dto.OkTeam;
import ch.schlierelacht.admin.dto.OkTeamMemberDTO;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ch.schlierelacht.admin.jooq.Tables.OK_MEMBER;
import static ch.schlierelacht.admin.jooq.Tables.OK_TEAM_MEMBER;

@Service
@RequiredArgsConstructor
public class OkService {

    private final DSLContext dslContext;

    @Transactional(readOnly = true)
    public OkDTO findAll() {
        List<OkMemberDTO> members = dslContext.selectFrom(OK_MEMBER)
                .orderBy(OK_MEMBER.SORT_ORDER.asc(), OK_MEMBER.NAME.asc())
                .fetch(r -> new OkMemberDTO(r.getName(), r.getRole(), r.getEmail(), r.getCloudflareId()));

        List<OkTeamMemberDTO> teamMembers = dslContext.selectFrom(OK_TEAM_MEMBER)
                .orderBy(OK_TEAM_MEMBER.TEAM.asc(), OK_TEAM_MEMBER.NAME.asc())
                .fetch(r -> new OkTeamMemberDTO(r.getName(), OkTeam.fromDb(r.getTeam()).orElseThrow()));

        return new OkDTO(members, teamMembers);
    }
}
