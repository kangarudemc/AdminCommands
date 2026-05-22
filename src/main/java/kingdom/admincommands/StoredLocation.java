package kingdom.admincommands;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** A serializable world position + facing, used by /warp and /home persistence and /back. */
public record StoredLocation(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {

    public static final Codec<StoredLocation> CODEC = RecordCodecBuilder.create(i -> i.group(
        ResourceKey.codec(Registries.DIMENSION).fieldOf("dim").forGetter(StoredLocation::dimension),
        Codec.DOUBLE.fieldOf("x").forGetter(StoredLocation::x),
        Codec.DOUBLE.fieldOf("y").forGetter(StoredLocation::y),
        Codec.DOUBLE.fieldOf("z").forGetter(StoredLocation::z),
        Codec.FLOAT.fieldOf("yaw").forGetter(StoredLocation::yaw),
        Codec.FLOAT.fieldOf("pitch").forGetter(StoredLocation::pitch)
    ).apply(i, StoredLocation::new));

    public static StoredLocation of(ServerPlayer player) {
        return new StoredLocation(player.level().dimension(),
            player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    /** The destination level, or null if that dimension isn't loaded. */
    public ServerLevel resolveLevel(MinecraftServer server) {
        return server.getLevel(dimension);
    }
}
