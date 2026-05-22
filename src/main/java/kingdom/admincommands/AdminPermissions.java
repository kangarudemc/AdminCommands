package kingdom.admincommands;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContext;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/**
 * Per-command permission gating. Every command maps to a boolean {@link PermissionNode}
 * named {@code admincommands.command.<key>}. The default resolver grants access to server
 * operators (gamemaster level / op level 2+). When LuckPerms is installed it becomes the
 * active permission handler and resolves these exact node strings from its data, so a
 * non-op player granted e.g. {@code admincommands.command.fly} passes and everyone else
 * is denied — i.e. "ops OR explicitly-permissioned players, nobody else".
 */
public final class AdminPermissions {
    private AdminPermissions() {}

    /** All command keys that get a node. /reop is intentionally absent (gated by owner list). */
    public static final String[] KEYS = {
        "back", "fly", "top", "thru", "heal", "feed", "god", "repair", "spawn", "tphere",
        "vanish", "freeze", "mute", "unmute", "sudo", "speed", "broadcast", "nickname",
        "prefix", "suffix", "itemname", "itemlore", "smite", "kit", "warp", "sethome",
        "home", "delhome", "homes", "ec", "dimension", "gamemode", "seeinv", "whois", "help",
        "setjail", "jail", "release"
    };

    private static final Map<String, PermissionNode<Boolean>> NODES = new LinkedHashMap<>();

    static {
        for (String key : KEYS) {
            NODES.put(key, new PermissionNode<>(
                "admincommands", "command." + key,
                PermissionTypes.BOOLEAN, AdminPermissions::opDefault));
        }
    }

    /** Default value when no override (LuckPerms) is present: operator (gamemaster) level. */
    private static Boolean opDefault(ServerPlayer player, UUID uuid, PermissionDynamicContext<?>... ctx) {
        return player != null
            && player.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /** Register every node — call from {@link PermissionGatherEvent.Nodes} (game bus). */
    public static void gather(PermissionGatherEvent.Nodes event) {
        for (PermissionNode<Boolean> node : NODES.values()) {
            event.addNodes(node);
        }
    }

    /** Brigadier {@code .requires(...)} predicate for the command with the given key. */
    public static Predicate<CommandSourceStack> require(String key) {
        PermissionNode<Boolean> node = NODES.get(key);
        if (node == null) throw new IllegalArgumentException("No permission node for key: " + key);
        return src -> {
            if (src.getEntity() instanceof ServerPlayer player) {
                return PermissionAPI.getPermission(player, node);
            }
            // Console / command block / functions: fall back to vanilla op level.
            return src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        };
    }
}
