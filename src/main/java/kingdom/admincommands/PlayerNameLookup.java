package kingdom.admincommands;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Resolves online players by Mojang username or by the name shown in tab/chat
 * (e.g. LuckPerms / nickname plugins via NeoForge {@code TabListNameFormat}).
 */
public final class PlayerNameLookup {
    private PlayerNameLookup() {}

    public static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYER_NAMES = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase(Locale.ROOT);
        MinecraftServer server = ctx.getSource().getServer();
        if (server == null) return builder.buildFuture();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            suggestIfMatches(builder, rem, accountName(p));
            String shown = visibleLabel(p);
            if (!shown.equalsIgnoreCase(accountName(p))) {
                suggestIfMatches(builder, rem, shown);
            }
        }
        return builder.buildFuture();
    };

    /** Find an online player by account name or visible nickname (case-insensitive). */
    public static ServerPlayer resolve(MinecraftServer server, String input) {
        if (server == null || input == null || input.isBlank()) return null;
        ServerPlayer direct = server.getPlayerList().getPlayerByName(input);
        if (direct != null) return direct;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (accountName(p).equalsIgnoreCase(input)) return p;
            if (visibleLabel(p).equalsIgnoreCase(input)) return p;
            if (plainChatName(p).equalsIgnoreCase(input)) return p;
        }
        return null;
    }

    /** Mojang account name — never a nickname. */
    public static String accountName(ServerPlayer player) {
        return player.nameAndId().name();
    }

    /** Label other players usually see in the tab list. */
    public static String visibleLabel(ServerPlayer player) {
        Component tab = player.getTabListDisplayName();
        if (tab != null) {
            String plain = tab.getString().trim();
            if (!plain.isEmpty()) return plain;
        }
        return plainChatName(player);
    }

    public static String plainChatName(ServerPlayer player) {
        return player.getDisplayName().getString().trim();
    }

    /** True when tab/chat shows something other than the Mojang username. */
    public static boolean hasDistinctVisibleName(ServerPlayer player) {
        String account = accountName(player);
        return !visibleLabel(player).equalsIgnoreCase(account);
    }

    private static void suggestIfMatches(SuggestionsBuilder builder, String remainingLower, String candidate) {
        if (remainingLower.isEmpty() || candidate.toLowerCase(Locale.ROOT).startsWith(remainingLower)) {
            builder.suggest(candidate);
        }
    }
}
