package kingdom.admincommands;

import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

public record JailedPlayerInfo(
    StoredLocation preJailLocation,
    StoredLocation jailLocation,
    List<ItemStack> mainInventory,
    List<ItemStack> armorInventory,
    List<ItemStack> offhandInventory
) {
    public static final Codec<JailedPlayerInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
        StoredLocation.CODEC.fieldOf("pre_jail_loc").forGetter(JailedPlayerInfo::preJailLocation),
        StoredLocation.CODEC.fieldOf("jail_loc").forGetter(JailedPlayerInfo::jailLocation),
        ItemStack.OPTIONAL_CODEC.listOf().fieldOf("main_inv").forGetter(JailedPlayerInfo::mainInventory),
        ItemStack.OPTIONAL_CODEC.listOf().fieldOf("armor_inv").forGetter(JailedPlayerInfo::armorInventory),
        ItemStack.OPTIONAL_CODEC.listOf().fieldOf("offhand_inv").forGetter(JailedPlayerInfo::offhandInventory)
    ).apply(i, JailedPlayerInfo::new));
}
