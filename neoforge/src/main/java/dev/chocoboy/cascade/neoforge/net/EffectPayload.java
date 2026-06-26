package dev.chocoboy.cascade.neoforge.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record EffectPayload(ResourceLocation effect, Vec3 a, Vec3 b, long seed) implements CustomPacketPayload {

    public static final Type<EffectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cascade", "effect"));

    private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x,
            ByteBufCodecs.DOUBLE, Vec3::y,
            ByteBufCodecs.DOUBLE, Vec3::z,
            Vec3::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, EffectPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, EffectPayload::effect,
            VEC3, EffectPayload::a,
            VEC3, EffectPayload::b,
            ByteBufCodecs.VAR_LONG, EffectPayload::seed,
            EffectPayload::new);

    @Override
    public Type<EffectPayload> type() {
        return TYPE;
    }
}
