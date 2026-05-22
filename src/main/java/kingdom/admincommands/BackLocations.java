package kingdom.admincommands;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;

/**
 * Per-player "last location" store backing {@code /back}. Recorded on death and before
 * a player is moved by a teleport command. In-memory only.
 */
public final class BackLocations {
    private BackLocations() {}

    private static final ConcurrentHashMap<UUID, StoredLocation> LAST = new ConcurrentHashMap<>();

    public static void record(ServerPlayer player) {
        LAST.put(player.getUUID(), StoredLocation.of(player));
    }

    public static void set(UUID uuid, StoredLocation loc) {
        LAST.put(uuid, loc);
    }

    public static StoredLocation get(UUID uuid) {
        return LAST.get(uuid);
    }

    public static void forget(UUID uuid) {
        LAST.remove(uuid);
    }
}
