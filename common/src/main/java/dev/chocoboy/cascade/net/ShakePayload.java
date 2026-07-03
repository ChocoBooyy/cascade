package dev.chocoboy.cascade.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record ShakePayload(Vec3 pos, float magnitude, int duration) implements CustomPacketPayload {

    public static final Type<ShakePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cascade", "shake"));

    // the shake's reach. the server only sends to players inside it and the client fades to zero at the
    // edge, so both sides must agree; this is the one type they share, so the constant lives here
    public static final double RADIUS = 32.0;

    public static final StreamCodec<RegistryFriendlyByteBuf, ShakePayload> STREAM_CODEC = StreamCodec.composite(
            NetCodecs.VEC3, ShakePayload::pos,
            ByteBufCodecs.FLOAT, ShakePayload::magnitude,
            ByteBufCodecs.VAR_INT, ShakePayload::duration,
            ShakePayload::new);

    @Override
    public Type<ShakePayload> type() {
        return TYPE;
    }
}
