package kingdom.admincommands;

import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class AdminEvents {
    private AdminEvents() {}

    // Pre-built immutable packet — avoids allocating a new Component + Packet every tick
    private static final ClientboundSetActionBarTextPacket VANISH_ACTION_BAR =
        new ClientboundSetActionBarTextPacket(Component.literal("\u00a7c\u00a7lYou're Vanished!"));

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        AdminCommandsRegistration.register(event);
    }

    @SubscribeEvent
    public static void onGatherPermissionNodes(PermissionGatherEvent.Nodes event) {
        AdminPermissions.gather(event);
    }

    /** Record where a player died so {@code /back} can return them. */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BackLocations.record(player);
        }
    }

    /** Record a player's pre-teleport location when moved by {@code /teleport}. */
    @SubscribeEvent
    public static void onTeleportCommand(EntityTeleportEvent.TeleportCommand event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Vec3 prev = event.getPrev();
            BackLocations.set(player.getUUID(), new StoredLocation(
                player.level().dimension(), prev.x, prev.y, prev.z,
                player.getYRot(), player.getXRot()));
        }
    }

    /** Drop muted players' chat before it's broadcast. */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        UUID id = event.getPlayer().getUUID();
        if (AdminStates.isMuted(id)) {
            event.setCanceled(true);
            long secs = AdminStates.muteRemainingSeconds(id);
            event.getPlayer().sendSystemMessage(Component.literal(secs == Long.MAX_VALUE
                ? "\u00a7cYou are muted."
                : "\u00a7cYou are muted for " + secs + " more second(s)."));
        }
    }

    /** Frozen players can't jump — neutralise the upward impulse. */
    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!AdminStates.isFrozen(player.getUUID())) return;
        Vec3 motion = player.getDeltaMovement();
        if (motion.y > 0) player.setDeltaMovement(motion.x, 0.0, motion.z);
    }

    /**
     * Per-player server tick. Handles frozen rubber-banding and vanish action bar.
     * Optimised: early-exits immediately when the player has no active states,
     * and re-uses a pre-built packet for the vanish bar to avoid GC pressure.
     */
    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID uuid = player.getUUID();
        boolean vanished = AdminStates.isVanished(uuid);
        boolean frozen = AdminStates.isFrozen(uuid);
        boolean jailed = AdminStates.isJailed(uuid);

        // Fast path: nothing to do for this player
        if (!vanished && !frozen && !jailed) return;

        // Vanish action bar — send every 20 ticks (once per second) instead of every tick.
        // Action bar text stays visible for ~60 ticks, so once per second keeps it always on screen.
        if (vanished && player.tickCount % 20 == 0) {
            player.connection.send(VANISH_ACTION_BAR);
        }

        // Frozen rubber-band
        if (frozen) {
            StoredLocation anchor = AdminStates.freezeAnchor(uuid);
            if (anchor != null) {
                ServerLevel dest = anchor.resolveLevel(player.level().getServer());
                double dx = player.getX() - anchor.x();
                double dy = player.getY() - anchor.y();
                double dz = player.getZ() - anchor.z();
                if (dest != null && (player.level() != dest || dx * dx + dy * dy + dz * dz > 1.0E-4)) {
                    player.teleportTo(dest, anchor.x(), anchor.y(), anchor.z(),
                        java.util.Set.of(), anchor.yaw(), anchor.pitch(), false);
                }
                player.setDeltaMovement(Vec3.ZERO);
            }
        }

        // Jailed rubber-band
        if (jailed) {
            StoredLocation jailLoc = AdminStates.jailAnchor(uuid);
            if (jailLoc != null) {
                MinecraftServer server = player.level().getServer();
                if (server != null) {
                    ServerLevel dest = jailLoc.resolveLevel(server);
                    double dx = player.getX() - jailLoc.x();
                    double dy = player.getY() - jailLoc.y();
                    double dz = player.getZ() - jailLoc.z();
                    if (dest != null && (player.level() != dest || dx * dx + dy * dy + dz * dz > 4.0)) {
                        player.teleportTo(dest, jailLoc.x(), jailLoc.y(), jailLoc.z(),
                            java.util.Set.of(), jailLoc.yaw(), jailLoc.pitch(), false);
                        player.setDeltaMovement(Vec3.ZERO);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        BackLocations.forget(uuid);
        AdminStates.clear(uuid);
    }

    /** Configured owners recover op automatically on join, and jailees recover jailed state. */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        if (Config.isReopOwner(player.nameAndId().name()) && !server.getPlayerList().isOp(player.nameAndId())) {
            server.getPlayerList().op(player.nameAndId());
        }
        if (JailSavedData.get(server).isJailed(player.getUUID())) {
            JailedPlayerInfo info = JailSavedData.get(server).getJailedInfo(player.getUUID());
            AdminStates.setJailed(player.getUUID(), info != null ? info.jailLocation() : null);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && AdminStates.isJailed(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && AdminStates.isJailed(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && AdminStates.isJailed(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && AdminStates.isJailed(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && AdminStates.isJailed(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && AdminStates.isJailed(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        if (event.getPlayer() instanceof ServerPlayer player && AdminStates.isJailed(player.getUUID())) {
            event.setCanPickup(net.minecraft.util.TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && AdminStates.isJailed(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        Component name = buildDisplayName(server, player, player.getName());
        if (name != null) event.setDisplayname(name);
    }

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        Component name = buildDisplayName(server, player, player.getName());
        if (name != null) event.setDisplayName(name);
    }

    /** Builds [prefix] [nickname|original] [suffix], or null if no customizations exist. */
    private static Component buildDisplayName(MinecraftServer server, ServerPlayer player, Component original) {
        java.util.UUID uuid = player.getUUID();
        Component cached = AdminStates.getCachedDisplayName(uuid);
        if (cached != null) return cached;

        NicknameSavedData data = NicknameSavedData.get(server);
        String nick = data.getRaw(uuid);
        String pre = data.getPrefix(uuid);
        String suf = data.getSuffix(uuid);
        if (nick == null && pre == null && suf == null) return null;

        net.minecraft.network.chat.MutableComponent result = Component.empty();
        if (pre != null && !pre.isBlank()) {
            result.append(AdminCommandsRegistration.parseFormatted(pre, null)).append(Component.literal(" "));
        }
        if (nick != null && !nick.isBlank()) {
            result.append(AdminCommandsRegistration.parseFormatted(nick, null));
        } else {
            result.append(original);
        }
        if (suf != null && !suf.isBlank()) {
            result.append(Component.literal(" ")).append(AdminCommandsRegistration.parseFormatted(suf, null));
        }
        
        AdminStates.setCachedDisplayName(uuid, result);
        return result;
    }
}
