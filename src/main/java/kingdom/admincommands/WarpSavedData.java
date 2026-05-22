package kingdom.admincommands;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Server-wide named warp points for {@code /warp}, stored on the overworld's data storage. */
public class WarpSavedData extends SavedData {

    public static final Codec<WarpSavedData> CODEC =
        Codec.unboundedMap(Codec.STRING, StoredLocation.CODEC).xmap(WarpSavedData::new, d -> d.warps);

    public static final SavedDataType<WarpSavedData> TYPE =
        new SavedDataType<>(Identifier.parse("admincommands:warps"),
            WarpSavedData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, StoredLocation> warps;

    public WarpSavedData() { this.warps = new HashMap<>(); }
    private WarpSavedData(Map<String, StoredLocation> warps) { this.warps = new HashMap<>(warps); }

    public static WarpSavedData get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(TYPE);
    }

    public void set(String name, StoredLocation loc) { warps.put(name.toLowerCase(), loc); setDirty(); }
    public boolean remove(String name) {
        boolean r = warps.remove(name.toLowerCase()) != null;
        if (r) setDirty();
        return r;
    }
    public StoredLocation get(String name) { return warps.get(name.toLowerCase()); }
    public TreeSet<String> names() { return new TreeSet<>(warps.keySet()); }
}
