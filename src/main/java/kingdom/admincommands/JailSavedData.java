package kingdom.admincommands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class JailSavedData extends SavedData {

    public static final Codec<JailSavedData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.unboundedMap(Codec.STRING, StoredLocation.CODEC)
            .optionalFieldOf("jails", Map.of()).forGetter(d -> d.jails),
        Codec.unboundedMap(Codec.STRING, JailedPlayerInfo.CODEC)
            .optionalFieldOf("jailed_players", Map.of()).forGetter(d -> d.jailedPlayers)
    ).apply(inst, JailSavedData::new));

    public static final SavedDataType<JailSavedData> TYPE =
        new SavedDataType<>(Identifier.parse("admincommands:jails"),
            JailSavedData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, StoredLocation> jails;
    private final Map<String, JailedPlayerInfo> jailedPlayers;

    public JailSavedData() {
        this.jails = new HashMap<>();
        this.jailedPlayers = new HashMap<>();
    }

    private JailSavedData(Map<String, StoredLocation> jails, Map<String, JailedPlayerInfo> jailedPlayers) {
        this.jails = new HashMap<>(jails);
        this.jailedPlayers = new HashMap<>(jailedPlayers);
    }

    private static volatile JailSavedData cached;
    private static volatile MinecraftServer cachedServer;

    public static JailSavedData get(MinecraftServer server) {
        if (cachedServer == server && cached != null) return cached;
        cached = server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(TYPE);
        cachedServer = server;
        return cached;
    }

    // Jails
    public void setJail(int number, StoredLocation loc) {
        jails.put(String.valueOf(number), loc);
        setDirty();
    }

    public StoredLocation getJail(int number) {
        return jails.get(String.valueOf(number));
    }

    // Jailed Players
    public void jailPlayer(UUID playerUuid, JailedPlayerInfo info) {
        jailedPlayers.put(playerUuid.toString(), info);
        setDirty();
    }

    public void releasePlayer(UUID playerUuid) {
        if (jailedPlayers.remove(playerUuid.toString()) != null) {
            setDirty();
        }
    }

    public boolean isJailed(UUID playerUuid) {
        return jailedPlayers.containsKey(playerUuid.toString());
    }

    public JailedPlayerInfo getJailedInfo(UUID playerUuid) {
        return jailedPlayers.get(playerUuid.toString());
    }

    public Map<String, JailedPlayerInfo> getJailedPlayers() {
        return jailedPlayers;
    }
}
