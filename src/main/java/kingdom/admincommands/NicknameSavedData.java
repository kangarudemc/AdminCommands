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

/** Stores raw formatted nickname, prefix, and suffix strings. */
public class NicknameSavedData extends SavedData {

    public static final Codec<NicknameSavedData> CODEC =
        RecordCodecBuilder.create(inst -> inst.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                .optionalFieldOf("nicknames", Map.of()).forGetter(d -> d.nicknames),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                .optionalFieldOf("prefixes", Map.of()).forGetter(d -> d.prefixes),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                .optionalFieldOf("suffixes", Map.of()).forGetter(d -> d.suffixes)
        ).apply(inst, NicknameSavedData::new));

    public static final SavedDataType<NicknameSavedData> TYPE =
        new SavedDataType<>(Identifier.parse("admincommands:nicknames"),
            NicknameSavedData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, String> nicknames;
    private final Map<String, String> prefixes;
    private final Map<String, String> suffixes;

    public NicknameSavedData() {
        this.nicknames = new HashMap<>();
        this.prefixes = new HashMap<>();
        this.suffixes = new HashMap<>();
    }

    private NicknameSavedData(Map<String, String> nicknames, Map<String, String> prefixes, Map<String, String> suffixes) {
        this.nicknames = new HashMap<>(nicknames);
        this.prefixes = new HashMap<>(prefixes);
        this.suffixes = new HashMap<>(suffixes);
    }

    // Cache the instance per-server to avoid computeIfAbsent on every name format event
    private static volatile NicknameSavedData cached;
    private static volatile MinecraftServer cachedServer;

    public static NicknameSavedData get(MinecraftServer server) {
        if (cachedServer == server && cached != null) return cached;
        cached = server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(TYPE);
        cachedServer = server;
        return cached;
    }

    // ── Nicknames ──────────────────────────────────────────────────────────────
    public void set(UUID player, String rawNickname) {
        nicknames.put(player.toString(), rawNickname);
        setDirty();
    }

    public boolean remove(UUID player) {
        boolean r = nicknames.remove(player.toString()) != null;
        if (r) setDirty();
        return r;
    }

    public String getRaw(UUID player) {
        return nicknames.get(player.toString());
    }

    // ── Prefixes ───────────────────────────────────────────────────────────────
    public void setPrefix(UUID player, String rawPrefix) {
        prefixes.put(player.toString(), rawPrefix);
        setDirty();
    }

    public boolean removePrefix(UUID player) {
        boolean r = prefixes.remove(player.toString()) != null;
        if (r) setDirty();
        return r;
    }

    public String getPrefix(UUID player) {
        return prefixes.get(player.toString());
    }

    // ── Suffixes ───────────────────────────────────────────────────────────────
    public void setSuffix(UUID player, String rawSuffix) {
        suffixes.put(player.toString(), rawSuffix);
        setDirty();
    }

    public boolean removeSuffix(UUID player) {
        boolean r = suffixes.remove(player.toString()) != null;
        if (r) setDirty();
        return r;
    }

    public String getSuffix(UUID player) {
        return suffixes.get(player.toString());
    }
}
