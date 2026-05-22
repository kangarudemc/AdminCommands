package kingdom.admincommands;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;

/**
 * In-memory toggle state for commands that need cross-tick enforcement: {@code /vanish}
 * (who is hidden), {@code /freeze} (who is locked, with their anchor), {@code /mute},
 * and {@code /jail}.
 */
public final class AdminStates {
    private AdminStates() {}

    private static final Set<UUID> VANISHED = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, StoredLocation> FROZEN = new ConcurrentHashMap<>();
    /** uuid → epoch millis when the mute lifts; Long.MAX_VALUE = permanent. */
    private static final Map<UUID, Long> MUTED = new ConcurrentHashMap<>();
    private static final Map<UUID, StoredLocation> JAILED = new ConcurrentHashMap<>();

    // ── Vanish ────────────────────────────────────────────────────────────────
    public static boolean isVanished(UUID uuid) { return VANISHED.contains(uuid); }
    public static void setVanished(UUID uuid, boolean vanished) {
        if (vanished) VANISHED.add(uuid); else VANISHED.remove(uuid);
    }

    // ── Freeze ──────────────────────────────────────────────────────────────--
    public static boolean isFrozen(UUID uuid) { return FROZEN.containsKey(uuid); }
    public static StoredLocation freezeAnchor(UUID uuid) { return FROZEN.get(uuid); }
    public static void freeze(UUID uuid, StoredLocation anchor) { FROZEN.put(uuid, anchor); }
    public static void unfreeze(UUID uuid) { FROZEN.remove(uuid); }

    // ── Mute ────────────────────────────────────────────────────────────────--
    public static void mute(UUID uuid, long untilEpochMillis) { MUTED.put(uuid, untilEpochMillis); }
    public static void unmute(UUID uuid) { MUTED.remove(uuid); }

    public static boolean isMuted(UUID uuid) {
        Long until = MUTED.get(uuid);
        if (until == null) return false;
        if (until != Long.MAX_VALUE && System.currentTimeMillis() >= until) {
            MUTED.remove(uuid);
            return false;
        }
        return true;
    }

    public static long muteRemainingSeconds(UUID uuid) {
        Long until = MUTED.get(uuid);
        if (until == null) return 0;
        if (until == Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(0, (until - System.currentTimeMillis()) / 1000);
    }

    // ── Jail ──────────────────────────────────────────────────────────────────
    public static boolean isJailed(UUID uuid) { return JAILED.containsKey(uuid); }
    public static StoredLocation jailAnchor(UUID uuid) { return JAILED.get(uuid); }
    public static void setJailed(UUID uuid, StoredLocation jailLoc) {
        if (jailLoc != null) JAILED.put(uuid, jailLoc); else JAILED.remove(uuid);
    }

    private static final Map<UUID, Component> DISPLAY_NAME_CACHE = new ConcurrentHashMap<>();

    public static Component getCachedDisplayName(UUID uuid) { return DISPLAY_NAME_CACHE.get(uuid); }
    public static void setCachedDisplayName(UUID uuid, Component name) {
        if (name != null) DISPLAY_NAME_CACHE.put(uuid, name); else DISPLAY_NAME_CACHE.remove(uuid);
    }
    public static void clearCachedDisplayName(UUID uuid) { DISPLAY_NAME_CACHE.remove(uuid); }

    /** Drop transient session state on logout. Mutes persist (no relog evasion). */
    public static void clear(UUID uuid) {
        VANISHED.remove(uuid);
        FROZEN.remove(uuid);
        JAILED.remove(uuid);
        DISPLAY_NAME_CACHE.remove(uuid);
    }
}
