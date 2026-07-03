package dev.chocoboy.cascade.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record LightPayload(Vec3 pos, int color, float radius, int duration) implements CustomPacketPayload {

    public static final Type<LightPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cascade", "light"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LightPayload> STREAM_CODEC = StreamCodec.composite(
            NetCodecs.VEC3, LightPayload::pos,
            ByteBufCodecs.INT, LightPayload::color,
            ByteBufCodecs.FLOAT, LightPayload::radius,
            ByteBufCodecs.VAR_INT, LightPayload::duration,
            LightPayload::new);

    @Override
    public Type<LightPayload> type() {
        return TYPE;
    }
}
