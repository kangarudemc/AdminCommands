package kingdom.admincommands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Server-wide admin item kits for {@code /kit}. The SavedDataStorage serializes with a
 * registry-aware RegistryOps, so {@link ItemStack#CODEC} round-trips correctly here.
 */
public class KitSavedData extends SavedData {

    public static final Codec<KitSavedData> CODEC =
        Codec.unboundedMap(Codec.STRING, ItemStack.CODEC.listOf()).xmap(KitSavedData::new, d -> d.kits);

    public static final SavedDataType<KitSavedData> TYPE =
        new SavedDataType<>(Identifier.parse("admincommands:kits"),
            KitSavedData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, List<ItemStack>> kits;

    public KitSavedData() { this.kits = new HashMap<>(); }
    private KitSavedData(Map<String, List<ItemStack>> kits) {
        this.kits = new HashMap<>();
        kits.forEach((k, v) -> this.kits.put(k, new ArrayList<>(v)));
    }

    public static KitSavedData get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(TYPE);
    }

    public void set(String name, List<ItemStack> items) {
        List<ItemStack> copies = new ArrayList<>(items.size());
        for (ItemStack s : items) copies.add(s.copy());
        kits.put(name.toLowerCase(), copies);
        setDirty();
    }
    public boolean remove(String name) {
        boolean r = kits.remove(name.toLowerCase()) != null;
        if (r) setDirty();
        return r;
    }
    public List<ItemStack> get(String name) {
        List<ItemStack> stored = kits.get(name.toLowerCase());
        if (stored == null) return null;
        List<ItemStack> copies = new ArrayList<>(stored.size());
        for (ItemStack s : stored) copies.add(s.copy());
        return copies;
    }
    public TreeSet<String> names() { return new TreeSet<>(kits.keySet()); }
}
