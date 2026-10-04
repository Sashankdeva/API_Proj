package com.squadup.util;

import java.util.Optional;

/**
 * Chat room naming, kept identical to the original app:
 * <ul>
 *   <li>DM rooms: {@code dm_<idA>_<idB>} with the two ids sorted as strings</li>
 *   <li>Squad rooms: {@code squad_<squadId>}</li>
 * </ul>
 */
public final class RoomIds {

    private static final String DM_PREFIX = "dm_";
    private static final String SQUAD_PREFIX = "squad_";

    private RoomIds() {
    }

    public static String dm(Long a, Long b) {
        String x = a.toString();
        String y = b.toString();
        return x.compareTo(y) <= 0 ? DM_PREFIX + x + "_" + y : DM_PREFIX + y + "_" + x;
    }

    public static String squad(Long squadId) {
        return SQUAD_PREFIX + squadId;
    }

    public static boolean isSquadRoom(String roomId) {
        return roomId != null && roomId.startsWith(SQUAD_PREFIX);
    }

    /** The squad id of a squad room, if {@code roomId} is a well-formed one. */
    public static Optional<Long> squadId(String roomId) {
        if (!isSquadRoom(roomId)) {
            return Optional.empty();
        }
        return parseLong(roomId.substring(SQUAD_PREFIX.length()));
    }

    /** The two participant ids of a DM room, if {@code roomId} is a well-formed one. */
    public static Optional<long[]> dmParticipants(String roomId) {
        if (roomId == null || !roomId.startsWith(DM_PREFIX)) {
            return Optional.empty();
        }
        String[] parts = roomId.substring(DM_PREFIX.length()).split("_");
        if (parts.length != 2) {
            return Optional.empty();
        }
        Optional<Long> a = parseLong(parts[0]);
        Optional<Long> b = parseLong(parts[1]);
        if (a.isEmpty() || b.isEmpty() || a.get().equals(b.get())) {
            return Optional.empty();
        }
        return Optional.of(new long[]{a.get(), b.get()});
    }

    private static Optional<Long> parseLong(String s) {
        try {
            return Optional.of(Long.parseLong(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
