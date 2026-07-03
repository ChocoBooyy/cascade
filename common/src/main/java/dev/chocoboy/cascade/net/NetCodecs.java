package dev.chocoboy.cascade.net;

import dev.chocoboy.cascade.engine.math.Vec3f;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public final class NetCodecs {

    // Vec3 has no built-in stream codec, so encode it as three doubles
    public static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x,
            ByteBufCodecs.DOUBLE, Vec3::y,
            ByteBufCodecs.DOUBLE, Vec3::z,
            Vec3::new);

    public static final StreamCodec<ByteBuf, Vec3f> VEC3F = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Vec3f::x,
            ByteBufCodecs.FLOAT, Vec3f::y,
            ByteBufCodecs.FLOAT, Vec3f::z,
            Vec3f::new);

    private NetCodecs() {
    }
}
