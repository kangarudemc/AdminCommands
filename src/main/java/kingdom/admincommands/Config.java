package kingdom.admincommands;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * AdminCommands server config. Registered as {@code ModConfig.Type.SERVER}, so it lives in
 * the world's {@code serverconfig/admincommands-server.toml} and is never synced to clients.
 *
 * <p>Most commands are gated only by NeoForge permission nodes ({@code admincommands.command.*}).
 * The only config-backed command is {@code /reop}. {@code /whois} nickname resolution uses
 * tab-list / chat display names at runtime and has no config keys.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ── Server owners (/reop) ─────────────────────────────────────────────────
    public static final ModConfigSpec.ConfigValue<List<? extends String>> REOP_OWNERS = BUILDER
        .comment("Mojang account usernames (not nicknames) allowed to use /reop and auto-re-op on login.",
            "Gated on this list, not on permission nodes — deopped owners on the list can recover.",
            "Hidden from players not listed. Copy from ironhold-server.toml reopOwners when migrating.")
        .<String>defineListAllowEmpty("reopOwners", List.of(), () -> "", o -> o instanceof String);

    /** True if {@code username} (case-insensitive) is a configured /reop owner. */
    public static boolean isReopOwner(String username) {
        if (username == null) return false;
        for (Object entry : REOP_OWNERS.get()) {
            if (entry instanceof String s && s.equalsIgnoreCase(username)) return true;
        }
        return false;
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
