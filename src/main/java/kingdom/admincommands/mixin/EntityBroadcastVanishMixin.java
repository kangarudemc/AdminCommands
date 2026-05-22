package kingdom.admincommands.mixin;

import kingdom.admincommands.AdminStates;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hard-vanish hook for {@code /vanish}. The entity tracker
 * ({@code ChunkMap.TrackedEntity#updatePlayer}) calls {@link Entity#broadcastToPlayer}
 * every tick to decide who each tracked entity is sent to. Returning {@code false} for a
 * vanished player makes the tracker drop them from every other client entirely — no body,
 * armor, movement, entity sounds, or PvP targetability — and re-pair them automatically on
 * unvanish (the tracker keeps its {@code seenBy} set consistent on its own).
 */
@Mixin(Entity.class)
public abstract class EntityBroadcastVanishMixin {

    @Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
    private void admincommands$hideVanished(ServerPlayer recipient, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer self
                && self != recipient
                && AdminStates.isVanished(self.getUUID())) {
            cir.setReturnValue(false);
        }
    }
}
