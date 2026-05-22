package kingdom.admincommands;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(AdminCommandsMod.MODID)
public class AdminCommandsMod {
    public static final String MODID = "admincommands";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AdminCommandsMod(IEventBus modEventBus, ModContainer modContainer) {
        // SERVER config: the /reop owner list lives in the world's serverconfig dir.
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        // All command registration, permission-node gathering, and runtime enforcement
        // (freeze / mute / back / vanish cleanup / auto-reop) happens on the game bus.
        NeoForge.EVENT_BUS.register(AdminEvents.class);
    }
}
