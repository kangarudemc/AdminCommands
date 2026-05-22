package kingdom.admincommands;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Per-player named homes for {@code /home} / {@code /sethome}, keyed by player UUID (as a string). */
public class HomeSavedData extends SavedData {

    public static final Codec<HomeSavedData> CODEC =
        Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, StoredLocation.CODEC))
            .xmap(HomeSavedData::new, d -> d.homes);

    public static final SavedDataType<HomeSavedData> TYPE =
        new SavedDataType<>(Identifier.parse("admincommands:homes"),
            HomeSavedData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, Map<String, StoredLocation>> homes;

    public HomeSavedData() { this.homes = new HashMap<>(); }
    private HomeSavedData(Map<String, Map<String, StoredLocation>> homes) {
        this.homes = new HashMap<>();
        homes.forEach((k, v) -> this.homes.put(k, new HashMap<>(v)));
    }

    public static HomeSavedData get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(TYPE);
    }

    public void set(UUID player, String name, StoredLocation loc) {
        homes.computeIfAbsent(player.toString(), k -> new HashMap<>()).put(name.toLowerCase(), loc);
        setDirty();
    }
    public boolean remove(UUID player, String name) {
        Map<String, StoredLocation> mine = homes.get(player.toString());
        if (mine == null) return false;
        boolean r = mine.remove(name.toLowerCase()) != null;
        if (r) setDirty();
        return r;
    }
    public StoredLocation get(UUID player, String name) {
        Map<String, StoredLocation> mine = homes.get(player.toString());
        return mine == null ? null : mine.get(name.toLowerCase());
    }
    public TreeSet<String> names(UUID player) {
        Map<String, StoredLocation> mine = homes.get(player.toString());
        return mine == null ? new TreeSet<>() : new TreeSet<>(mine.keySet());
    }
}
