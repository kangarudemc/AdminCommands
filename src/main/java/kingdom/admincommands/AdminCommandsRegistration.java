package kingdom.admincommands;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class AdminCommandsRegistration {
    private AdminCommandsRegistration() {}

    private static final SuggestionProvider<CommandSourceStack> WARP_NAMES = (ctx, b) -> {
        String r = b.getRemaining().toLowerCase();
        for (String n : WarpSavedData.get(ctx.getSource().getServer()).names()) {
            if (r.isEmpty() || n.startsWith(r)) b.suggest(n);
        }
        return b.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> FORMATTING_SUGGESTOR = (ctx, b) -> {
        String input = b.getRemaining();
        int lastSpace = input.lastIndexOf(' ');
        String currentWord = lastSpace == -1 ? input : input.substring(lastSpace + 1);
        String prefix = lastSpace == -1 ? "" : input.substring(0, lastSpace + 1);

        // Count how many valid formatting words have already been typed
        int validCount = 0;
        boolean hasColor = false;
        boolean hasStyle = false;
        if (lastSpace != -1) {
            String[] prevWords = input.substring(0, lastSpace).split(" ");
            for (String w : prevWords) {
                ChatFormatting f = ChatFormatting.getByName(w);
                if (f == null) return b.buildFuture(); // typed text already, stop suggesting
                validCount++;
                if (f.isColor()) hasColor = true;
                else hasStyle = true;
            }
        }

        // Stage 1: no formatting yet — suggest colors only
        // Stage 2: has a color, no style yet — suggest styles only
        // Stage 3+: already has both or 2+ codes — stop suggesting, let them type text
        if (validCount >= 2) return b.buildFuture();

        for (ChatFormatting f : ChatFormatting.values()) {
            if (f == ChatFormatting.RESET) continue;
            String name = f.getName();
            if (name == null) continue;

            boolean offer;
            if (!hasColor && !hasStyle) {
                // First slot: offer colors
                offer = f.isColor();
            } else if (hasColor && !hasStyle) {
                // Second slot: offer styles (bold, italic, etc.)
                offer = !f.isColor();
            } else {
                offer = false;
            }

            if (offer && name.startsWith(currentWord.toLowerCase())) {
                b.suggest(prefix + name);
            }
        }
        return b.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> HOME_NAMES = (ctx, b) -> {
        if (ctx.getSource().getEntity() instanceof ServerPlayer p) {
            String r = b.getRemaining().toLowerCase();
            for (String n : HomeSavedData.get(p.level().getServer()).names(p.getUUID())) {
                if (r.isEmpty() || n.startsWith(r)) b.suggest(n);
            }
        }
        return b.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> KIT_NAMES = (ctx, b) -> {
        String r = b.getRemaining().toLowerCase();
        for (String n : KitSavedData.get(ctx.getSource().getServer()).names()) {
            if (r.isEmpty() || n.startsWith(r)) b.suggest(n);
        }
        return b.buildFuture();
    };

    public static void register(RegisterCommandsEvent event) {
        var d = event.getDispatcher();
        // ── Help / Info ────────────────────────────────────────────────────────
        reg(d, Commands.literal("admincommands").requires(AdminPermissions.require("help"))
            .executes(ctx -> {
                CommandSourceStack src = ctx.getSource();
                src.sendSuccess(() -> Component.literal("§5§l=================== §6§lAdminCommands §e§lTutorial §5§l==================="), false);
                src.sendSuccess(() -> Component.literal("§bWelcome! §aClick any command below to suggest it. §dHover for permissions/info."), false);
                src.sendSuccess(() -> Component.literal(""), false);
                
                src.sendSuccess(() -> Component.literal("§c§l[🛡️ Moderation]"), false);
                src.sendSuccess(() -> Component.empty().append(cmd("c", "vanish", "Go invisible")).append("   ").append(cmd("c", "freeze <target>", "Freeze player")), false);
                src.sendSuccess(() -> Component.empty().append(cmd("c", "mute <target> [seconds]", "Silence player")).append("   ").append(cmd("c", "unmute <target>", "Unsilence")), false);
                src.sendSuccess(() -> Component.empty().append(cmd("c", "sudo <target> <command>", "Force command")).append("   ").append(cmd("c", "smite <target>", "Strike lightning")), false);
                src.sendSuccess(() -> Component.empty().append(cmd("c", "jail <target> <1-10>", "Jail player")).append("   ").append(cmd("c", "release <target>", "Release player")), false);
                src.sendSuccess(() -> Component.empty().append(cmd("c", "setjail <1-10>", "Set jail location")), false);
                
                src.sendSuccess(() -> Component.literal("§a§l[🏃 Movement / Teleport]"), false);
                src.sendSuccess(() -> Component.empty().append(cmd("a", "back", "Return to death/tp")).append("   ").append(cmd("a", "fly", "Toggle fly")), false);
                src.sendSuccess(() -> Component.empty().append(cmd("a", "top", "Go to surface")).append("   ").append(cmd("a", "thru", "Pass through walls")), false);
                src.sendSuccess(() -> Component.empty().append(cmd("a", "spawn [target]", "Go to spawn")).append("   ").append(cmd("a", "tphere <target>", "Summon player")), false);
                src.sendSuccess(() -> Component.empty().append(cmd("a", "speed <walk|fly> <multiplier>", "Set speed")).append("   ").append(cmd("a", "dimension <dim>", "Change dimension")), false);
                
                src.sendSuccess(() -> Component.literal("§6§l[🎒 Utility & Inventory]"), false);
                src.sendSuccess(() -> Component.empty().append(cmd("6", "seeinv <target>", "View live inv")).append("   ").append(cmd("6", "ec [target]", "View ender chest")), false);
                src.sendSuccess(() -> Component.empty().append(cmd("6", "repair", "Repair held item")).append("   ").append(cmd("6", "repair all", "Repair all items")), false);
                src.sendSuccess(() -> Component.empty().append(cmd("6", "heal [target]", "Restore status")).append("   ").append(cmd("6", "feed [target]", "Restore hunger")), false);
                src.sendSuccess(() -> Component.empty().append(cmd("6", "c", "Creative")).append(" ").append(cmd("6", "s", "Survival")).append(" ").append(cmd("6", "sp", "Spectator")).append(" ").append(cmd("6", "ad", "Adventure")), false);

                src.sendSuccess(() -> Component.literal("§d§l[🎨 Cosmetics & Customization]"), false);
                src.sendSuccess(() -> Component.empty().append(cmd("d", "nickname [target] [name]", "Set nickname")).append("   ").append(cmd("d", "prefix [target] [text]", "Set prefix")), false);
                src.sendSuccess(() -> Component.empty().append(cmd("d", "suffix [target] [text]", "Set suffix")).append("   ").append(cmd("d", "itemname <name>", "Rename held item")), false);
                src.sendSuccess(() -> Component.empty().append(cmd("d", "itemlore add <text>", "Add lore")).append("   ").append(cmd("d", "broadcast <msg>", "Broadcast")), false);

                src.sendSuccess(() -> Component.literal("§b§l[📍 Warps & Homes]"), false);
                src.sendSuccess(() -> Component.empty().append(cmd("b", "warp <name>", "Warp to place")).append("   ").append(cmd("b", "warp set <name>", "Set warp")), false);
                src.sendSuccess(() -> Component.empty().append(cmd("b", "warp del <name>", "Delete warp")).append("   ").append(cmd("b", "warp list", "List warps")), false);
                src.sendSuccess(() -> Component.empty().append(cmd("b", "home [name]", "Go home")).append("   ").append(cmd("b", "sethome [name]", "Set home")), false);
                src.sendSuccess(() -> Component.empty().append(cmd("b", "delhome <name>", "Delete home")).append("   ").append(cmd("b", "homes", "List homes")), false);

                src.sendSuccess(() -> Component.literal("§9§l[ℹ️ Player Info]"), false);
                src.sendSuccess(() -> Component.empty().append(cmd("9", "whois <player>", "View player diagnostic details")), false);

                src.sendSuccess(() -> Component.literal(""), false);
                src.sendSuccess(() -> Component.literal("§e💡 Tip: Prefix any command with §b/ac:<command> §e(e.g., §b/ac:fly§e) to bypass mod conflicts."), false);
                src.sendSuccess(() -> Component.literal("§5§l===================================================="), false);
                return 1;
            }));

        // ── Movement / teleport ────────────────────────────────────────────────
        reg(d, Commands.literal("back").requires(AdminPermissions.require("back"))
            .executes(ctx -> back(ctx.getSource())));
        reg(d, Commands.literal("fly").requires(AdminPermissions.require("fly"))
            .executes(ctx -> toggleFly(ctx.getSource())));
        reg(d, Commands.literal("top").requires(AdminPermissions.require("top"))
            .executes(ctx -> top(ctx.getSource())));
        reg(d, Commands.literal("thru").requires(AdminPermissions.require("thru"))
            .executes(ctx -> thru(ctx.getSource())));
        reg(d, Commands.literal("spawn").requires(AdminPermissions.require("spawn"))
            .executes(ctx -> spawn(ctx.getSource(), null))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> spawn(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));
        reg(d, Commands.literal("tphere").requires(AdminPermissions.require("tphere"))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> tpHere(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));
        reg(d, Commands.literal("dimension").requires(AdminPermissions.require("dimension"))
            .then(Commands.argument("dim", StringArgumentType.word())
                .suggests((ctx, b) -> {
                    for (String s : new String[]{"overworld", "nether", "end"}) {
                        if (s.startsWith(b.getRemaining().toLowerCase())) b.suggest(s);
                    }
                    return b.buildFuture();
                })
                .executes(ctx -> switchDimension(ctx.getSource(), StringArgumentType.getString(ctx, "dim")))));

        // ── Self-restore / cheats ──────────────────────────────────────────────
        reg(d, Commands.literal("heal").requires(AdminPermissions.require("heal"))
            .executes(ctx -> heal(ctx.getSource(), null))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> heal(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));
        reg(d, Commands.literal("feed").requires(AdminPermissions.require("feed"))
            .executes(ctx -> feed(ctx.getSource(), null))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> feed(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));
        reg(d, Commands.literal("god").requires(AdminPermissions.require("god"))
            .executes(ctx -> toggleGod(ctx.getSource())));
        reg(d, Commands.literal("repair").requires(AdminPermissions.require("repair"))
            .executes(ctx -> repairHeld(ctx.getSource()))
            .then(Commands.literal("all").executes(ctx -> repairAll(ctx.getSource()))));

        reg(d, Commands.literal("speed").requires(AdminPermissions.require("speed"))
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((ctx, b) -> {
                    for (String t : new String[]{"walk", "fly"}) {
                        if (t.startsWith(b.getRemaining().toLowerCase())) b.suggest(t);
                    }
                    return b.buildFuture();
                })
                .then(Commands.argument("multiplier", FloatArgumentType.floatArg(0f, 10f))
                    .executes(ctx -> speed(ctx.getSource(), StringArgumentType.getString(ctx, "type"),
                        FloatArgumentType.getFloat(ctx, "multiplier"))))));

        // ── Gamemode shortcuts (one node: gamemode) ────────────────────────────
        reg(d, Commands.literal("c").requires(AdminPermissions.require("gamemode"))
            .executes(ctx -> setSelfGameMode(ctx.getSource(), GameType.CREATIVE)));
        reg(d, Commands.literal("s").requires(AdminPermissions.require("gamemode"))
            .executes(ctx -> setSelfGameMode(ctx.getSource(), GameType.SURVIVAL)));
        reg(d, Commands.literal("sp").requires(AdminPermissions.require("gamemode"))
            .executes(ctx -> setSelfGameMode(ctx.getSource(), GameType.SPECTATOR)));
        reg(d, Commands.literal("ad").requires(AdminPermissions.require("gamemode"))
            .executes(ctx -> setSelfGameMode(ctx.getSource(), GameType.ADVENTURE)));

        // ── Moderation ─────────────────────────────────────────────────────────
        reg(d, Commands.literal("vanish").requires(AdminPermissions.require("vanish"))
            .executes(ctx -> toggleVanish(ctx.getSource())));
        reg(d, Commands.literal("freeze").requires(AdminPermissions.require("freeze"))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> toggleFreeze(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));
        reg(d, Commands.literal("mute").requires(AdminPermissions.require("mute"))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> mute(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), 0))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                    .executes(ctx -> mute(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"),
                        IntegerArgumentType.getInteger(ctx, "seconds"))))));
        reg(d, Commands.literal("unmute").requires(AdminPermissions.require("unmute"))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> unmute(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));
        reg(d, Commands.literal("sudo").requires(AdminPermissions.require("sudo"))
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("command", StringArgumentType.greedyString())
                    .executes(ctx -> sudo(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"),
                        StringArgumentType.getString(ctx, "command"))))));
        reg(d, Commands.literal("smite").requires(AdminPermissions.require("smite"))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> smite(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), null))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(ctx -> smite(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"),
                        StringArgumentType.getString(ctx, "message"))))));
        reg(d, Commands.literal("broadcast").requires(AdminPermissions.require("broadcast"))
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .suggests(FORMATTING_SUGGESTOR)
                .executes(ctx -> broadcast(ctx.getSource(), StringArgumentType.getString(ctx, "message")))));

        com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> nickBuilder = 
            Commands.literal("nickname").requires(AdminPermissions.require("nickname"))
            .then(Commands.literal("clear")
                .executes(ctx -> nicknameClear(ctx.getSource(), null))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> nicknameClear(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("name", StringArgumentType.greedyString())
                    .suggests(FORMATTING_SUGGESTOR)
                    .executes(ctx -> nicknameSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"),
                        StringArgumentType.getString(ctx, "name")))))
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .suggests(FORMATTING_SUGGESTOR)
                .executes(ctx -> nicknameSet(ctx.getSource(), null, StringArgumentType.getString(ctx, "name"))));
                
        reg(d, nickBuilder);
        
        // Register the /nick alias
        reg(d, Commands.literal("nick").requires(AdminPermissions.require("nickname"))
            .then(Commands.literal("clear")
                .executes(ctx -> nicknameClear(ctx.getSource(), null))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> nicknameClear(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("name", StringArgumentType.greedyString())
                    .suggests(FORMATTING_SUGGESTOR)
                    .executes(ctx -> nicknameSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"),
                        StringArgumentType.getString(ctx, "name")))))
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .suggests(FORMATTING_SUGGESTOR)
                .executes(ctx -> nicknameSet(ctx.getSource(), null, StringArgumentType.getString(ctx, "name")))));

        reg(d, Commands.literal("prefix").requires(AdminPermissions.require("prefix"))
            .then(Commands.literal("clear")
                .executes(ctx -> prefixClear(ctx.getSource(), null))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> prefixClear(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .suggests(FORMATTING_SUGGESTOR)
                    .executes(ctx -> prefixSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"),
                        StringArgumentType.getString(ctx, "text")))))
            .then(Commands.argument("text", StringArgumentType.greedyString())
                .suggests(FORMATTING_SUGGESTOR)
                .executes(ctx -> prefixSet(ctx.getSource(), null, StringArgumentType.getString(ctx, "text")))));

        reg(d, Commands.literal("suffix").requires(AdminPermissions.require("suffix"))
            .then(Commands.literal("clear")
                .executes(ctx -> suffixClear(ctx.getSource(), null))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> suffixClear(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .suggests(FORMATTING_SUGGESTOR)
                    .executes(ctx -> suffixSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"),
                        StringArgumentType.getString(ctx, "text")))))
            .then(Commands.argument("text", StringArgumentType.greedyString())
                .suggests(FORMATTING_SUGGESTOR)
                .executes(ctx -> suffixSet(ctx.getSource(), null, StringArgumentType.getString(ctx, "text")))));

        reg(d, Commands.literal("itemname").requires(AdminPermissions.require("itemname"))
            .then(Commands.literal("clear").executes(ctx -> itemnameClear(ctx.getSource())))
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .suggests(FORMATTING_SUGGESTOR)
                .executes(ctx -> itemnameSet(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));

        reg(d, Commands.literal("itemlore").requires(AdminPermissions.require("itemlore"))
            .then(Commands.literal("clear").executes(ctx -> itemloreClear(ctx.getSource())))
            .then(Commands.literal("add").then(Commands.argument("text", StringArgumentType.greedyString())
                .suggests(FORMATTING_SUGGESTOR)
                .executes(ctx -> itemloreAdd(ctx.getSource(), StringArgumentType.getString(ctx, "text"))))));

        // ── Inspection ─────────────────────────────────────────────────────────
        reg(d, Commands.literal("seeinv").requires(AdminPermissions.require("seeinv"))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> seeInventory(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));
        reg(d, Commands.literal("ec").requires(AdminPermissions.require("ec"))
            .executes(ctx -> enderChest(ctx.getSource(), null))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> enderChest(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));
        reg(d, Commands.literal("whois").requires(AdminPermissions.require("whois"))
            .then(Commands.argument("target", StringArgumentType.greedyString())
                .suggests(PlayerNameLookup.ONLINE_PLAYER_NAMES)
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    String query = StringArgumentType.getString(ctx, "target");
                    ServerPlayer target = PlayerNameLookup.resolve(src.getServer(), query);
                    if (target == null) {
                        src.sendFailure(Component.literal(
                            "No online player matching '" + query
                                + "'. Use their tab nickname or Mojang username."));
                        return 0;
                    }
                    return whois(src, target);
                })));

        // ── Warps ──────────────────────────────────────────────────────────────
        reg(d, Commands.literal("warp").requires(AdminPermissions.require("warp"))
            .then(Commands.literal("set").then(Commands.argument("name", StringArgumentType.word())
                .executes(ctx -> warpSet(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("del").then(Commands.argument("name", StringArgumentType.word())
                .suggests(WARP_NAMES)
                .executes(ctx -> warpDelete(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("list").executes(ctx -> warpList(ctx.getSource())))
            .then(Commands.argument("name", StringArgumentType.word()).suggests(WARP_NAMES)
                .executes(ctx -> warpGo(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));

        // ── Homes ──────────────────────────────────────────────────────────────
        reg(d, Commands.literal("sethome").requires(AdminPermissions.require("sethome"))
            .executes(ctx -> homeSet(ctx.getSource(), "home"))
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(ctx -> homeSet(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        reg(d, Commands.literal("home").requires(AdminPermissions.require("home"))
            .executes(ctx -> homeGo(ctx.getSource(), "home"))
            .then(Commands.argument("name", StringArgumentType.word()).suggests(HOME_NAMES)
                .executes(ctx -> homeGo(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        reg(d, Commands.literal("delhome").requires(AdminPermissions.require("delhome"))
            .then(Commands.argument("name", StringArgumentType.word()).suggests(HOME_NAMES)
                .executes(ctx -> homeDelete(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        reg(d, Commands.literal("homes").requires(AdminPermissions.require("homes"))
            .executes(ctx -> homeList(ctx.getSource())));

        // ── Kits ───────────────────────────────────────────────────────────────
        reg(d, Commands.literal("kit").requires(AdminPermissions.require("kit"))
            .then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.word())
                .executes(ctx -> kitCreate(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("delete").then(Commands.argument("name", StringArgumentType.word())
                .suggests(KIT_NAMES)
                .executes(ctx -> kitDelete(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("list").executes(ctx -> kitList(ctx.getSource())))
            .then(Commands.argument("name", StringArgumentType.word()).suggests(KIT_NAMES)
                .executes(ctx -> kitGive(ctx.getSource(), StringArgumentType.getString(ctx, "name"), null))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> kitGive(ctx.getSource(), StringArgumentType.getString(ctx, "name"),
                        EntityArgument.getPlayer(ctx, "target"))))));

        // ── /reop — gated on the owner config list, NOT on a permission node ────
        reg(d, Commands.literal("reop")
            .requires(s -> s.getEntity() instanceof ServerPlayer p && Config.isReopOwner(p.nameAndId().name()))
            .executes(ctx -> reop(ctx.getSource())));

        // ── Jail ───────────────────────────────────────────────────────────────
        reg(d, Commands.literal("setjail").requires(AdminPermissions.require("setjail"))
            .then(Commands.argument("number", IntegerArgumentType.integer(1, 10))
                .executes(ctx -> setJail(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "number")))));

        reg(d, Commands.literal("jail").requires(AdminPermissions.require("jail"))
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("number", IntegerArgumentType.integer(1, 10))
                    .executes(ctx -> jail(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), IntegerArgumentType.getInteger(ctx, "number"))))));

        reg(d, Commands.literal("release").requires(AdminPermissions.require("release"))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> release(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));
    }

    // ════════════════════════════ handlers ════════════════════════════════════

    private static int back(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        StoredLocation dest = BackLocations.get(player.getUUID());
        if (dest == null) {
            src.sendFailure(Component.literal("No previous location — /back works after a death or teleport."));
            return 0;
        }
        ServerLevel level = dest.resolveLevel(src.getServer());
        if (level == null) { src.sendFailure(Component.literal("That location's dimension isn't loaded.")); return 0; }
        BackLocations.set(player.getUUID(), StoredLocation.of(player));
        player.teleportTo(level, dest.x(), dest.y(), dest.z(), java.util.Set.of(), dest.yaw(), dest.pitch(), false);
        src.sendSuccess(() -> Component.literal("Returned to your previous location."), false);
        return 1;
    }

    private static int toggleFly(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        boolean enable = !player.getAbilities().mayfly;
        player.getAbilities().mayfly = enable;
        if (!enable) player.getAbilities().flying = false;
        player.onUpdateAbilities();
        src.sendSuccess(() -> Component.literal("Flight " + (enable ? "enabled." : "disabled.")), false);
        return 1;
    }

    private static int top(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        ServerLevel level = (ServerLevel) player.level();
        int x = player.getBlockX(), z = player.getBlockZ();
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
        player.teleportTo(level, x + 0.5, y, z + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
        src.sendSuccess(() -> Component.literal("Teleported to the surface."), false);
        return 1;
    }

    private static int thru(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        net.minecraft.world.phys.HitResult hit = player.pick(64.0, 1.0F, false);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            src.sendFailure(Component.literal("No block in range to teleport to."));
            return 0;
        }
        BlockPos dest = ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos().above();
        player.teleportTo((ServerLevel) player.level(), dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5,
            java.util.Set.of(), player.getYRot(), player.getXRot(), false);
        src.sendSuccess(() -> Component.literal("Teleported to your crosshair."), false);
        return 1;
    }

    private static int spawn(CommandSourceStack src, ServerPlayer target) {
        ServerPlayer subject = target;
        if (subject == null) {
            if (!(src.getEntity() instanceof ServerPlayer self)) {
                src.sendFailure(Component.literal("Players only — or pass a target player."));
                return 0;
            }
            subject = self;
        }
        ServerLevel overworld = src.getServer().getLevel(Level.OVERWORLD);
        var respawn = overworld.getRespawnData();
        BlockPos pos = respawn.pos();
        subject.teleportTo(overworld, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
            java.util.Set.of(), respawn.yaw(), respawn.pitch(), false);
        boolean self = target == null;
        ServerPlayer moved = subject;
        src.sendSuccess(() -> Component.literal(self
            ? "Teleported to world spawn." : "Sent " + moved.nameAndId().name() + " to world spawn."), !self);
        return 1;
    }

    private static int tpHere(CommandSourceStack src, ServerPlayer target) {
        if (!(src.getEntity() instanceof ServerPlayer me)) { return playersOnly(src); }
        if (target == me) { src.sendFailure(Component.literal("That's you.")); return 0; }
        target.teleportTo((ServerLevel) me.level(), me.getX(), me.getY(), me.getZ(),
            java.util.Set.of(), me.getYRot(), me.getXRot(), false);
        src.sendSuccess(() -> Component.literal("Teleported " + target.nameAndId().name() + " to you."), true);
        return 1;
    }

    private static int switchDimension(CommandSourceStack src, String dim) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        var key = switch (dim.toLowerCase()) {
            case "overworld" -> Level.OVERWORLD;
            case "nether", "the_nether" -> Level.NETHER;
            case "end", "the_end" -> Level.END;
            default -> null;
        };
        if (key == null) { src.sendFailure(Component.literal("Unknown dimension: " + dim)); return 0; }
        ServerLevel level = src.getServer().getLevel(key);
        if (level == null) { src.sendFailure(Component.literal("Dimension not loaded: " + dim)); return 0; }
        if (player.level().dimension() == key) { src.sendFailure(Component.literal("Already in " + dim + ".")); return 0; }
        BackLocations.record(player);
        player.teleportTo(level, player.getX(), player.getY(), player.getZ(),
            java.util.Set.of(), player.getYRot(), player.getXRot(), false);
        src.sendSuccess(() -> Component.literal("Teleported to " + dim + "."), true);
        return 1;
    }

    private static int heal(CommandSourceStack src, ServerPlayer target) {
        ServerPlayer subject = target;
        if (subject == null) {
            if (!(src.getEntity() instanceof ServerPlayer self)) {
                src.sendFailure(Component.literal("Players only — or pass a target player."));
                return 0;
            }
            subject = self;
        }
        subject.setHealth(subject.getMaxHealth());
        subject.getFoodData().setFoodLevel(20);
        subject.getFoodData().setSaturation(20.0F);
        subject.removeAllEffects();
        boolean selfTarget = target == null;
        ServerPlayer healed = subject;
        src.sendSuccess(() -> Component.literal(selfTarget
            ? "Healed to full." : "Healed " + healed.nameAndId().name() + " to full."), !selfTarget);
        return 1;
    }

    private static int feed(CommandSourceStack src, ServerPlayer target) {
        ServerPlayer subject = target;
        if (subject == null) {
            if (!(src.getEntity() instanceof ServerPlayer self)) {
                src.sendFailure(Component.literal("Players only — or pass a target player."));
                return 0;
            }
            subject = self;
        }
        subject.getFoodData().setFoodLevel(20);
        subject.getFoodData().setSaturation(20.0F);
        boolean selfTarget = target == null;
        ServerPlayer fed = subject;
        src.sendSuccess(() -> Component.literal(selfTarget
            ? "Hunger restored." : "Restored " + fed.nameAndId().name() + "'s hunger."), !selfTarget);
        return 1;
    }

    private static int toggleGod(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        boolean enable = !player.getAbilities().invulnerable;
        player.getAbilities().invulnerable = enable;
        player.onUpdateAbilities();
        src.sendSuccess(() -> Component.literal("God mode " + (enable ? "enabled." : "disabled.")), false);
        return 1;
    }

    private static int repairHeld(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !held.isDamageableItem()) {
            src.sendFailure(Component.literal("Hold a damageable item in your main hand."));
            return 0;
        }
        held.setDamageValue(0);
        src.sendSuccess(() -> Component.literal("Repaired " + held.getHoverName().getString() + "."), false);
        return 1;
    }

    private static int repairAll(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        int repaired = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (repairOne(inv.getItem(i))) repaired++;
        for (EquipmentSlot slot : EquipmentSlot.values()) if (repairOne(player.getItemBySlot(slot))) repaired++;
        int count = repaired;
        src.sendSuccess(() -> Component.literal("Repaired " + count + " item(s)."), true);
        return repaired;
    }

    private static boolean repairOne(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getDamageValue() == 0) return false;
        stack.setDamageValue(0);
        return true;
    }



    private static int speed(CommandSourceStack src, String type, float multiplier) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        boolean fly;
        if (type.equalsIgnoreCase("fly")) fly = true;
        else if (type.equalsIgnoreCase("walk")) fly = false;
        else { src.sendFailure(Component.literal("Use 'walk' or 'fly'.")); return 0; }
        float value = Math.min(1.0F, (fly ? 0.05F : 0.1F) * multiplier);
        if (fly) {
            player.getAbilities().setFlyingSpeed(value);
        } else {
            player.getAbilities().setWalkingSpeed(value);
            var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attr != null) attr.setBaseValue(value);
        }
        player.onUpdateAbilities();
        src.sendSuccess(() -> Component.literal((fly ? "Fly" : "Walk") + " speed set to " + multiplier + "x."), false);
        return 1;
    }

    private static int setSelfGameMode(CommandSourceStack src, GameType mode) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        if (player.setGameMode(mode)) {
            src.sendSuccess(() -> Component.literal("Set game mode to ").append(mode.getLongDisplayName()), true);
            return 1;
        }
        src.sendFailure(Component.literal("Unable to change game mode."));
        return 0;
    }

    // ── Moderation ──────────────────────────────────────────────────────────--

    private static int toggleVanish(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        boolean enable = !AdminStates.isVanished(player.getUUID());
        AdminStates.setVanished(player.getUUID(), enable);
        MinecraftServer server = src.getServer();
        if (enable) {
            for (ServerPlayer o : server.getPlayerList().getPlayers()) {
                if (o != player) o.connection.send(new ClientboundPlayerInfoRemovePacket(java.util.List.of(player.getUUID())));
            }
            server.getPlayerList().broadcastSystemMessage(
                Component.translatable("multiplayer.player.left", player.getDisplayName()).withStyle(ChatFormatting.YELLOW), false);
        } else {
            var add = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(java.util.List.of(player));
            for (ServerPlayer o : server.getPlayerList().getPlayers()) {
                if (o != player) o.connection.send(add);
            }
            server.getPlayerList().broadcastSystemMessage(
                Component.translatable("multiplayer.player.joined", player.getDisplayName()).withStyle(ChatFormatting.YELLOW), false);
            // Clear the vanish action bar reminder
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                Component.literal("")));
        }
        src.sendSuccess(() -> Component.literal(
            "Vanish " + (enable ? "enabled — you're fully hidden." : "disabled — you're visible.")), false);
        return 1;
    }

    private static final Identifier FROZEN_MOVE_MOD =
        Identifier.fromNamespaceAndPath(AdminCommandsMod.MODID, "frozen_movement");

    private static void setFreezeMovement(ServerPlayer player, boolean frozen) {
        var inst = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst == null) return;
        inst.removeModifier(FROZEN_MOVE_MOD);
        if (frozen) {
            inst.addTransientModifier(new AttributeModifier(
                FROZEN_MOVE_MOD, -100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static int toggleFreeze(CommandSourceStack src, ServerPlayer target) {
        boolean enable = !AdminStates.isFrozen(target.getUUID());
        if (enable) {
            AdminStates.freeze(target.getUUID(), StoredLocation.of(target));
            setFreezeMovement(target, true);
            target.sendSystemMessage(Component.literal("§cYou have been frozen by an admin."));
        } else {
            AdminStates.unfreeze(target.getUUID());
            setFreezeMovement(target, false);
            target.sendSystemMessage(Component.literal("§aYou have been unfrozen."));
        }
        src.sendSuccess(() -> Component.literal((enable ? "Froze " : "Unfroze ") + target.nameAndId().name() + "."), true);
        return 1;
    }

    private static int mute(CommandSourceStack src, ServerPlayer target, int seconds) {
        long until = seconds <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + seconds * 1000L;
        AdminStates.mute(target.getUUID(), until);
        target.sendSystemMessage(Component.literal(seconds <= 0
            ? "§cYou have been muted." : "§cYou have been muted for " + seconds + " second(s)."));
        String suffix = seconds <= 0 ? " (permanent)." : " for " + seconds + "s.";
        src.sendSuccess(() -> Component.literal("Muted " + target.nameAndId().name() + suffix), true);
        return 1;
    }

    private static int unmute(CommandSourceStack src, ServerPlayer target) {
        if (!AdminStates.isMuted(target.getUUID())) {
            src.sendFailure(Component.literal(target.nameAndId().name() + " isn't muted."));
            return 0;
        }
        AdminStates.unmute(target.getUUID());
        target.sendSystemMessage(Component.literal("§aYou have been unmuted."));
        src.sendSuccess(() -> Component.literal("Unmuted " + target.nameAndId().name() + "."), true);
        return 1;
    }

    private static int sudo(CommandSourceStack src, ServerPlayer target, String command) {
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        src.getServer().getCommands().performPrefixedCommand(target.createCommandSourceStack(), cmd);
        src.sendSuccess(() -> Component.literal("Ran as " + target.nameAndId().name() + ": /" + cmd), true);
        return 1;
    }

    private static int smite(CommandSourceStack src, ServerPlayer target, String message) {
        ServerLevel level = (ServerLevel) target.level();
        net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT
            .create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (bolt == null) { src.sendFailure(Component.literal("Couldn't summon lightning.")); return 0; }
        bolt.snapTo(target.getX(), target.getY(), target.getZ(), 0.0F, 0.0F);
        level.addFreshEntity(bolt);
        if (message != null && !message.isBlank()) {
            src.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal(message).withStyle(ChatFormatting.YELLOW), false);
        }
        src.sendSuccess(() -> Component.literal("Smote " + target.nameAndId().name() + "."), true);
        return 1;
    }

    public static Component parseFormatted(String raw, ChatFormatting defaultColor) {
        String[] words = raw.split(" ");
        java.util.List<ChatFormatting> styles = new java.util.ArrayList<>();
        int i = 0;
        for (; i < words.length; i++) {
            ChatFormatting f = ChatFormatting.getByName(words[i]);
            if (f == null) break;
            styles.add(f);
        }
        String message = String.join(" ", java.util.Arrays.copyOfRange(words, i, words.length));
        if (message.isBlank()) { message = raw; styles.clear(); i = 0; }
        Style style = Style.EMPTY;
        if (styles.isEmpty() && defaultColor != null) {
            style = style.withColor(defaultColor);
        } else {
            for (ChatFormatting f : styles) style = style.applyFormat(f);
        }
        return Component.literal(message).withStyle(style);
    }

    private static int broadcast(CommandSourceStack src, String raw) {
        Component message = parseFormatted(raw, ChatFormatting.GOLD);
        src.getServer().getPlayerList().broadcastSystemMessage(message, false);
        return 1;
    }

    private static int nicknameClear(CommandSourceStack src, ServerPlayer target) {
        ServerPlayer player = target;
        if (player == null) {
            if (!(src.getEntity() instanceof ServerPlayer self)) { return playersOnly(src); }
            player = self;
        }
        if (NicknameSavedData.get(src.getServer()).remove(player.getUUID())) {
            AdminStates.clearCachedDisplayName(player.getUUID());
            player.refreshTabListName();
            ServerPlayer p = player;
            src.sendSuccess(() -> Component.literal("Nickname cleared" + (target != null ? " for " + p.nameAndId().name() : "") + "."), target != null);
        } else {
            src.sendFailure(Component.literal(target != null ? target.nameAndId().name() + " doesn't have a nickname." : "You don't have a nickname set."));
        }
        return 1;
    }

    private static int nicknameSet(CommandSourceStack src, ServerPlayer target, String raw) {
        ServerPlayer player = target;
        if (player == null) {
            if (!(src.getEntity() instanceof ServerPlayer self)) { return playersOnly(src); }
            player = self;
        }
        NicknameSavedData.get(src.getServer()).set(player.getUUID(), raw);
        AdminStates.clearCachedDisplayName(player.getUUID());
        player.refreshTabListName();
        ServerPlayer p = player;
        src.sendSuccess(() -> Component.literal("Nickname" + (target != null ? " for " + p.nameAndId().name() : "") + " set to: ").append(parseFormatted(raw, null)), target != null);
        return 1;
    }

    private static int prefixClear(CommandSourceStack src, ServerPlayer target) {
        ServerPlayer player = target;
        if (player == null) {
            if (!(src.getEntity() instanceof ServerPlayer self)) { return playersOnly(src); }
            player = self;
        }
        if (NicknameSavedData.get(src.getServer()).removePrefix(player.getUUID())) {
            AdminStates.clearCachedDisplayName(player.getUUID());
            player.refreshTabListName();
            ServerPlayer p = player;
            src.sendSuccess(() -> Component.literal("Prefix cleared" + (target != null ? " for " + p.nameAndId().name() : "") + "."), target != null);
        } else {
            src.sendFailure(Component.literal(target != null ? target.nameAndId().name() + " doesn't have a prefix." : "You don't have a prefix set."));
        }
        return 1;
    }

    private static int prefixSet(CommandSourceStack src, ServerPlayer target, String raw) {
        ServerPlayer player = target;
        if (player == null) {
            if (!(src.getEntity() instanceof ServerPlayer self)) { return playersOnly(src); }
            player = self;
        }
        NicknameSavedData.get(src.getServer()).setPrefix(player.getUUID(), raw);
        AdminStates.clearCachedDisplayName(player.getUUID());
        player.refreshTabListName();
        ServerPlayer p = player;
        src.sendSuccess(() -> Component.literal("Prefix" + (target != null ? " for " + p.nameAndId().name() : "") + " set to: ").append(parseFormatted(raw, null)), target != null);
        return 1;
    }

    private static int suffixClear(CommandSourceStack src, ServerPlayer target) {
        ServerPlayer player = target;
        if (player == null) {
            if (!(src.getEntity() instanceof ServerPlayer self)) { return playersOnly(src); }
            player = self;
        }
        if (NicknameSavedData.get(src.getServer()).removeSuffix(player.getUUID())) {
            AdminStates.clearCachedDisplayName(player.getUUID());
            player.refreshTabListName();
            ServerPlayer p = player;
            src.sendSuccess(() -> Component.literal("Suffix cleared" + (target != null ? " for " + p.nameAndId().name() : "") + "."), target != null);
        } else {
            src.sendFailure(Component.literal(target != null ? target.nameAndId().name() + " doesn't have a suffix." : "You don't have a suffix set."));
        }
        return 1;
    }

    private static int suffixSet(CommandSourceStack src, ServerPlayer target, String raw) {
        ServerPlayer player = target;
        if (player == null) {
            if (!(src.getEntity() instanceof ServerPlayer self)) { return playersOnly(src); }
            player = self;
        }
        NicknameSavedData.get(src.getServer()).setSuffix(player.getUUID(), raw);
        AdminStates.clearCachedDisplayName(player.getUUID());
        player.refreshTabListName();
        ServerPlayer p = player;
        src.sendSuccess(() -> Component.literal("Suffix" + (target != null ? " for " + p.nameAndId().name() : "") + " set to: ").append(parseFormatted(raw, null)), target != null);
        return 1;
    }

    private static int itemnameClear(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { src.sendFailure(Component.literal("Hold an item.")); return 0; }
        held.remove(DataComponents.CUSTOM_NAME);
        src.sendSuccess(() -> Component.literal("Item name cleared."), false);
        return 1;
    }

    private static int itemnameSet(CommandSourceStack src, String raw) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { src.sendFailure(Component.literal("Hold an item.")); return 0; }
        held.set(DataComponents.CUSTOM_NAME, parseFormatted(raw, null));
        src.sendSuccess(() -> Component.literal("Item name updated."), false);
        return 1;
    }

    private static int itemloreClear(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { src.sendFailure(Component.literal("Hold an item.")); return 0; }
        held.remove(DataComponents.LORE);
        src.sendSuccess(() -> Component.literal("Item lore cleared."), false);
        return 1;
    }

    private static int itemloreAdd(CommandSourceStack src, String raw) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { src.sendFailure(Component.literal("Hold an item.")); return 0; }
        ItemLore lore = held.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        java.util.List<Component> lines = new java.util.ArrayList<>(lore.lines());
        lines.add(parseFormatted(raw, null));
        held.set(DataComponents.LORE, new ItemLore(lines));
        src.sendSuccess(() -> Component.literal("Line added to item lore."), false);
        return 1;
    }

    // ── Inspection ──────────────────────────────────────────────────────────--

    private static int seeInventory(CommandSourceStack src, ServerPlayer target) {
        if (!(src.getEntity() instanceof ServerPlayer viewer)) { return playersOnly(src); }
        if (target == viewer) { src.sendFailure(Component.literal("That's your own inventory.")); return 0; }
        net.minecraft.world.Container view = new PlayerInventoryView(target);
        viewer.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (id, inv, p) -> new PeekMenu(id, inv, view),
            Component.literal(target.nameAndId().name() + "'s Inventory")));
        src.sendSuccess(() -> Component.literal("Viewing " + target.nameAndId().name() + "'s inventory."), false);
        return 1;
    }

    private static int enderChest(CommandSourceStack src, ServerPlayer target) {
        if (!(src.getEntity() instanceof ServerPlayer viewer)) { return playersOnly(src); }
        ServerPlayer subject = target != null ? target : viewer;
        net.minecraft.world.inventory.PlayerEnderChestContainer ender = subject.getEnderChestInventory();
        String title = (target != null ? subject.nameAndId().name() + "'s" : "Your") + " Ender Chest";
        viewer.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (id, inv, p) -> new net.minecraft.world.inventory.ChestMenu(
                net.minecraft.world.inventory.MenuType.GENERIC_9x3, id, inv, ender, 3),
            Component.literal(title)));
        return 1;
    }

    private static int whois(CommandSourceStack src, ServerPlayer t) {
        MinecraftServer server = src.getServer();
        boolean op = server.getPlayerList().isOp(t.nameAndId());
        String account = PlayerNameLookup.accountName(t);
        String shown = PlayerNameLookup.visibleLabel(t);
        boolean nick = PlayerNameLookup.hasDistinctVisibleName(t);
        String dim = t.level().dimension().identifier().toString();
        String mode = t.gameMode().getName() + (op ? " §a[OP]§r" : "");
        if (nick) {
            src.sendSuccess(() -> Component.literal(
                "Shown as §6" + shown + "§r — account §e" + account + "§r — " + mode), false);
        } else {
            src.sendSuccess(() -> Component.literal("§6" + account + "§r — " + mode), false);
        }
        src.sendSuccess(() -> Component.literal(String.format(
            "§7%s  (%.1f, %.1f, %.1f)  HP %.0f/%.0f  Food %d  XP L%d",
            dim, t.getX(), t.getY(), t.getZ(), t.getHealth(), t.getMaxHealth(),
            t.getFoodData().getFoodLevel(), t.experienceLevel)), false);
        return 1;
    }

    // ── Warps ───────────────────────────────────────────────────────────────--

    private static int warpSet(CommandSourceStack src, String name) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        WarpSavedData.get(src.getServer()).set(name, StoredLocation.of(player));
        src.sendSuccess(() -> Component.literal("Warp '" + name.toLowerCase() + "' set."), true);
        return 1;
    }

    private static int warpDelete(CommandSourceStack src, String name) {
        if (!WarpSavedData.get(src.getServer()).remove(name)) {
            src.sendFailure(Component.literal("No warp named '" + name.toLowerCase() + "'."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Deleted warp '" + name.toLowerCase() + "'."), true);
        return 1;
    }

    private static int warpList(CommandSourceStack src) {
        var names = WarpSavedData.get(src.getServer()).names();
        if (names.isEmpty()) { src.sendSuccess(() -> Component.literal("No warps set."), false); return 0; }
        src.sendSuccess(() -> Component.literal("Warps (" + names.size() + "): " + String.join(", ", names)), false);
        return names.size();
    }

    private static int warpGo(CommandSourceStack src, String name) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        StoredLocation loc = WarpSavedData.get(src.getServer()).get(name);
        if (loc == null) { src.sendFailure(Component.literal("No warp named '" + name.toLowerCase() + "'.")); return 0; }
        if (!teleportToStored(src, player, loc)) return 0;
        src.sendSuccess(() -> Component.literal("Warped to '" + name.toLowerCase() + "'."), false);
        return 1;
    }

    // ── Homes ───────────────────────────────────────────────────────────────--

    private static int homeSet(CommandSourceStack src, String name) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        HomeSavedData.get(src.getServer()).set(player.getUUID(), name, StoredLocation.of(player));
        src.sendSuccess(() -> Component.literal("Home '" + name.toLowerCase() + "' set."), false);
        return 1;
    }

    private static int homeGo(CommandSourceStack src, String name) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        StoredLocation loc = HomeSavedData.get(src.getServer()).get(player.getUUID(), name);
        if (loc == null) {
            src.sendFailure(Component.literal("No home named '" + name.toLowerCase() + "'. Set one with /sethome."));
            return 0;
        }
        if (!teleportToStored(src, player, loc)) return 0;
        src.sendSuccess(() -> Component.literal("Teleported to home '" + name.toLowerCase() + "'."), false);
        return 1;
    }

    private static int homeDelete(CommandSourceStack src, String name) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        if (!HomeSavedData.get(src.getServer()).remove(player.getUUID(), name)) {
            src.sendFailure(Component.literal("No home named '" + name.toLowerCase() + "'."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Deleted home '" + name.toLowerCase() + "'."), false);
        return 1;
    }

    private static int homeList(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        var names = HomeSavedData.get(src.getServer()).names(player.getUUID());
        if (names.isEmpty()) { src.sendSuccess(() -> Component.literal("You have no homes. Set one with /sethome."), false); return 0; }
        src.sendSuccess(() -> Component.literal("Homes (" + names.size() + "): " + String.join(", ", names)), false);
        return names.size();
    }

    private static boolean teleportToStored(CommandSourceStack src, ServerPlayer player, StoredLocation loc) {
        ServerLevel level = loc.resolveLevel(src.getServer());
        if (level == null) { src.sendFailure(Component.literal("That location's dimension isn't loaded.")); return false; }
        player.teleportTo(level, loc.x(), loc.y(), loc.z(), java.util.Set.of(), loc.yaw(), loc.pitch(), false);
        return true;
    }

    // ── Kits ────────────────────────────────────────────────────────────────--

    private static int kitCreate(CommandSourceStack src, String name) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        var inv = player.getInventory();
        int main = Math.min(36, inv.getContainerSize());
        for (int i = 0; i < main; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) items.add(s.copy());
        }
        if (items.isEmpty()) { src.sendFailure(Component.literal("Your inventory is empty — nothing to save.")); return 0; }
        KitSavedData.get(src.getServer()).set(name, items);
        int count = items.size();
        src.sendSuccess(() -> Component.literal("Saved kit '" + name.toLowerCase() + "' (" + count + " stacks)."), true);
        return 1;
    }

    private static int kitGive(CommandSourceStack src, String name, ServerPlayer target) {
        ServerPlayer subject = target;
        if (subject == null) {
            if (!(src.getEntity() instanceof ServerPlayer self)) {
                src.sendFailure(Component.literal("Players only — or pass a target player."));
                return 0;
            }
            subject = self;
        }
        java.util.List<ItemStack> items = KitSavedData.get(src.getServer()).get(name);
        if (items == null) { src.sendFailure(Component.literal("No kit named '" + name.toLowerCase() + "'.")); return 0; }
        for (ItemStack stack : items) {
            if (!subject.getInventory().add(stack)) subject.drop(stack, false);
        }
        ServerPlayer recipient = subject;
        boolean self = target == null;
        src.sendSuccess(() -> Component.literal(self
            ? "Received kit '" + name.toLowerCase() + "'."
            : "Gave kit '" + name.toLowerCase() + "' to " + recipient.nameAndId().name() + "."), !self);
        return 1;
    }

    private static int kitDelete(CommandSourceStack src, String name) {
        if (!KitSavedData.get(src.getServer()).remove(name)) {
            src.sendFailure(Component.literal("No kit named '" + name.toLowerCase() + "'."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Deleted kit '" + name.toLowerCase() + "'."), true);
        return 1;
    }

    private static int kitList(CommandSourceStack src) {
        var names = KitSavedData.get(src.getServer()).names();
        if (names.isEmpty()) { src.sendSuccess(() -> Component.literal("No kits defined."), false); return 0; }
        src.sendSuccess(() -> Component.literal("Kits (" + names.size() + "): " + String.join(", ", names)), false);
        return names.size();
    }

    private static int reop(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        MinecraftServer server = src.getServer();
        if (server.getPlayerList().isOp(player.nameAndId())) {
            src.sendSuccess(() -> Component.literal("You are already an operator."), false);
            return 1;
        }
        server.getPlayerList().op(player.nameAndId());
        src.sendSuccess(() -> Component.literal("Re-opped " + player.nameAndId().name() + "."), true);
        return 1;
    }

    private static int playersOnly(CommandSourceStack src) {
        src.sendFailure(Component.literal("Players only."));
        return 0;
    }

    // ════════════════════════ /seeinv container plumbing ═══════════════════════

    /** Editable view over a target's main inventory (0–35), armor (36–39), and offhand (40). */
    private static final class PlayerInventoryView implements net.minecraft.world.Container {
        private static final int MAIN = 36;
        private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND
        };
        private static final int EQUIP_END = MAIN + ARMOR.length; // 41
        static final int SIZE = 54;

        private final ServerPlayer target;
        PlayerInventoryView(ServerPlayer target) { this.target = target; }

        @Override public int getContainerSize() { return SIZE; }
        @Override public boolean isEmpty() {
            for (int i = 0; i < SIZE; i++) if (!getItem(i).isEmpty()) return false;
            return true;
        }
        @Override public ItemStack getItem(int slot) {
            if (slot < MAIN) return target.getInventory().getItem(slot);
            if (slot < EQUIP_END) return target.getItemBySlot(ARMOR[slot - MAIN]);
            return ItemStack.EMPTY;
        }
        @Override public ItemStack removeItem(int slot, int amount) {
            if (slot < MAIN) return target.getInventory().removeItem(slot, amount);
            if (slot < EQUIP_END) {
                EquipmentSlot eq = ARMOR[slot - MAIN];
                ItemStack cur = target.getItemBySlot(eq);
                if (cur.isEmpty() || amount <= 0) return ItemStack.EMPTY;
                ItemStack split = cur.split(amount);
                target.setItemSlot(eq, cur);
                return split;
            }
            return ItemStack.EMPTY;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            if (slot < MAIN) return target.getInventory().removeItemNoUpdate(slot);
            if (slot < EQUIP_END) {
                EquipmentSlot eq = ARMOR[slot - MAIN];
                ItemStack cur = target.getItemBySlot(eq);
                target.setItemSlot(eq, ItemStack.EMPTY);
                return cur;
            }
            return ItemStack.EMPTY;
        }
        @Override public void setItem(int slot, ItemStack stack) {
            if (slot < MAIN) target.getInventory().setItem(slot, stack);
            else if (slot < EQUIP_END) target.setItemSlot(ARMOR[slot - MAIN], stack);
        }
        @Override public void setChanged() { target.getInventory().setChanged(); }
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player p) { return !target.isRemoved(); }
        @Override public void clearContent() { for (int i = 0; i < EQUIP_END; i++) setItem(i, ItemStack.EMPTY); }
    }

    /** Six-row chest menu with the trailing filler slots (41–53) locked. */
    private static final class PeekMenu extends net.minecraft.world.inventory.ChestMenu {
        PeekMenu(int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.Container container) {
            super(net.minecraft.world.inventory.MenuType.GENERIC_9x6, id, inv, container, 6);
            for (int idx = PlayerInventoryView.SIZE - 13; idx < PlayerInventoryView.SIZE; idx++) {
                net.minecraft.world.inventory.Slot old = this.slots.get(idx);
                net.minecraft.world.inventory.Slot locked =
                    new net.minecraft.world.inventory.Slot(container, idx, old.x, old.y) {
                        @Override public boolean mayPlace(ItemStack s) { return false; }
                        @Override public boolean mayPickup(net.minecraft.world.entity.player.Player p) { return false; }
                    };
                locked.index = idx;
                this.slots.set(idx, locked);
            }
        }
    }

    private static void reg(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> d, com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> builder) {
        com.mojang.brigadier.tree.LiteralCommandNode<net.minecraft.commands.CommandSourceStack> node = builder.build();
        
        // If no other mod has claimed this literal, we attach it to the root.
        // Otherwise, we "bow out" and skip adding the base command, avoiding conflicts!
        if (d.getRoot().getChild(builder.getLiteral()) == null) {
            d.getRoot().addChild(node);
        }

        // We always register our aliases to point to our isolated node tree.
        d.register(net.minecraft.commands.Commands.literal("admincommands:" + builder.getLiteral()).redirect(node));
        d.register(net.minecraft.commands.Commands.literal("ac:" + builder.getLiteral()).redirect(node));
    }

    private static Component cmd(String colorChar, String syntax, String desc) {
        String baseCmd = syntax.split(" ")[0];
        return Component.empty()
            .append(Component.literal("§" + colorChar + "§l/" + syntax)
                .withStyle(style -> style
                    .withClickEvent(new net.minecraft.network.chat.ClickEvent.SuggestCommand("/" + baseCmd + " "))
                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(
                        Component.literal("§" + colorChar + "§l/" + baseCmd + "\n§7Description: §f" + desc + "\n§7Permission: §dadmincommands.command." + baseCmd)))))
            .append(Component.literal(" §7- " + desc));
    }

    private static int setJail(CommandSourceStack src, int number) {
        if (!(src.getEntity() instanceof ServerPlayer player)) { return playersOnly(src); }
        StoredLocation loc = StoredLocation.of(player);
        JailSavedData.get(src.getServer()).setJail(number, loc);
        src.sendSuccess(() -> Component.literal("§aJail §6#" + number + " §ahas been set at your current location."), true);
        return 1;
    }

    private static int jail(CommandSourceStack src, ServerPlayer target, int number) {
        StoredLocation jailLoc = JailSavedData.get(src.getServer()).getJail(number);
        if (jailLoc == null) {
            src.sendFailure(Component.literal("Jail #" + number + " has not been set yet. Set it using /setjail " + number));
            return 0;
        }

        // Store pre-jail location and inventory
        StoredLocation preJailLoc = StoredLocation.of(target);
        java.util.List<ItemStack> mainInv = new java.util.ArrayList<>();
        for (int i = 0; i < 36; i++) {
            mainInv.add(target.getInventory().getItem(i).copy());
        }
        java.util.List<ItemStack> armorInv = new java.util.ArrayList<>();
        for (int i = 36; i < 40; i++) {
            armorInv.add(target.getInventory().getItem(i).copy());
        }
        java.util.List<ItemStack> offhandInv = new java.util.ArrayList<>();
        for (int i = 40; i < 41; i++) {
            offhandInv.add(target.getInventory().getItem(i).copy());
        }

        JailedPlayerInfo info = new JailedPlayerInfo(preJailLoc, jailLoc, mainInv, armorInv, offhandInv);
        JailSavedData.get(src.getServer()).jailPlayer(target.getUUID(), info);
        AdminStates.setJailed(target.getUUID(), jailLoc);

        // Clear target inventory
        target.getInventory().clearContent();
        target.containerMenu.broadcastChanges();
        target.inventoryMenu.slotsChanged(target.getInventory());

        // Teleport target
        ServerLevel dest = jailLoc.resolveLevel(src.getServer());
        if (dest != null) {
            target.teleportTo(dest, jailLoc.x(), jailLoc.y(), jailLoc.z(), java.util.Set.of(), jailLoc.yaw(), jailLoc.pitch(), false);
        }

        src.sendSuccess(() -> Component.literal("§aPlayer §6" + target.getName().getString() + " §ahas been jailed in jail §6#" + number + "§a."), true);
        target.sendSystemMessage(Component.literal("§cYou have been jailed by an administrator!"));
        return 1;
    }

    private static int release(CommandSourceStack src, ServerPlayer target) {
        JailSavedData data = JailSavedData.get(src.getServer());
        if (!data.isJailed(target.getUUID())) {
            src.sendFailure(Component.literal(target.getName().getString() + " is not jailed."));
            return 0;
        }

        JailedPlayerInfo info = data.getJailedInfo(target.getUUID());
        
        // Remove from jailed state/persistence
        data.releasePlayer(target.getUUID());
        AdminStates.setJailed(target.getUUID(), null);

        // Drop any items they picked up while jailed at the jail location before restoring
        target.getInventory().dropAll();

        // Restore saved inventory
        if (info != null) {
            for (int i = 0; i < Math.min(info.mainInventory().size(), 36); i++) {
                target.getInventory().setItem(i, info.mainInventory().get(i));
            }
            for (int i = 0; i < Math.min(info.armorInventory().size(), 4); i++) {
                target.getInventory().setItem(36 + i, info.armorInventory().get(i));
            }
            for (int i = 0; i < Math.min(info.offhandInventory().size(), 1); i++) {
                target.getInventory().setItem(40 + i, info.offhandInventory().get(i));
            }
            
            target.containerMenu.broadcastChanges();
            target.inventoryMenu.slotsChanged(target.getInventory());

            // Teleport back
            StoredLocation preJail = info.preJailLocation();
            ServerLevel dest = preJail.resolveLevel(src.getServer());
            if (dest != null) {
                target.teleportTo(dest, preJail.x(), preJail.y(), preJail.z(), java.util.Set.of(), preJail.yaw(), preJail.pitch(), false);
            }
        }

        src.sendSuccess(() -> Component.literal("§aPlayer §6" + target.getName().getString() + " §ahas been released."), true);
        target.sendSystemMessage(Component.literal("§aYou have been released from jail!"));
        return 1;
    }
}
