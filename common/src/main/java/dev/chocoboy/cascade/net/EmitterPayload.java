package dev.chocoboy.cascade.net;

import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record EmitterPayload(EmitterSpec spec, Vec3 origin, long seed) implements CustomPacketPayload {

    public static final Type<EmitterPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cascade", "emitter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EmitterPayload> STREAM_CODEC = StreamCodec.composite(
            SpecCodecs.EMITTER, EmitterPayload::spec,
            NetCodecs.VEC3, EmitterPayload::origin,
            ByteBufCodecs.VAR_LONG, EmitterPayload::seed,
            EmitterPayload::new);

    @Override
    public Type<EmitterPayload> type() {
        return TYPE;
    }
}
