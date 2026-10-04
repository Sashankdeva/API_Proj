package com.squadup.dto;

import com.squadup.entity.Squad;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class SquadDtos {

    private SquadDtos() {
    }

    public record CreateSquadRequest(
            @NotBlank(message = "Squad name and game are required.")
            @Size(max = 60, message = "Squad name must be at most 60 characters.")
            String name,

            @NotBlank(message = "Squad name and game are required.")
            @Size(max = 60, message = "Game must be at most 60 characters.")
            String game,

            @Size(max = 30, message = "Language must be at most 30 characters.")
            String language,

            @Min(value = Squad.MIN_MEMBERS, message = "A squad needs at least 2 members.")
            @Max(value = Squad.MAX_MEMBERS, message = "A squad can have at most 20 members.")
            Integer maxMembers) {
    }

    public record Owner(Long id, String username) {
    }

    /** v1 representation: full member list, as used by the web UI. */
    public record SquadDto(
            Long id,
            String name,
            String game,
            String language,
            int maxMembers,
            List<UserSummary> members,
            Owner createdBy,
            String roomId,
            Instant createdAt) {

        public static SquadDto from(Squad s) {
            return new SquadDto(
                    s.getId(), s.getName(), s.getGame(), s.getLanguage(), s.getMaxMembers(),
                    s.getMembers().stream().map(UserSummary::from).toList(),
                    new Owner(s.getCreatedBy().getId(), s.getCreatedBy().getUsername()),
                    s.getRoomId(), s.getCreatedAt());
        }
    }

    /** v2 representation: compact, no member list, with computed slot information. */
    public record SquadSummaryV2(
            Long id,
            String name,
            String game,
            String language,
            int memberCount,
            int maxMembers,
            int openSlots,
            boolean full,
            String owner,
            Instant createdAt) {

        public static SquadSummaryV2 from(Squad s) {
            int count = s.getMembers().size();
            return new SquadSummaryV2(
                    s.getId(), s.getName(), s.getGame(), s.getLanguage(), count, s.getMaxMembers(),
                    Math.max(0, s.getMaxMembers() - count), count >= s.getMaxMembers(),
                    s.getCreatedBy().getUsername(), s.getCreatedAt());
        }
    }

    /** A squad suggested by the matching engine, with the reasons it scored. */
    public record SquadMatch(SquadDto squad, int score, List<String> reasons) {
    }
}
