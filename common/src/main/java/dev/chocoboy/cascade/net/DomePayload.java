package dev.chocoboy.cascade.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record DomePayload(Vec3 pos, float radius, int color, int duration) implements CustomPacketPayload {

    public static final Type<DomePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cascade", "dome"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DomePayload> STREAM_CODEC = StreamCodec.composite(
            NetCodecs.VEC3, DomePayload::pos,
            ByteBufCodecs.FLOAT, DomePayload::radius,
            ByteBufCodecs.INT, DomePayload::color,
            ByteBufCodecs.VAR_INT, DomePayload::duration,
            DomePayload::new);

    @Override
    public Type<DomePayload> type() {
        return TYPE;
    }
}
