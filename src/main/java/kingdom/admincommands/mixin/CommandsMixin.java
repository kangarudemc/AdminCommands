package kingdom.admincommands.mixin;

import kingdom.admincommands.AdminStates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class CommandsMixin {

    @Inject(method = "performPrefixedCommand", at = @At("HEAD"), cancellable = true)
    private void admincommands$blockJailedCommands(CommandSourceStack source, String command, CallbackInfo ci) {
        if (source.getEntity() instanceof ServerPlayer player && AdminStates.isJailed(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cYou cannot use commands while jailed!"));
            ci.cancel();
        }
    }
}
