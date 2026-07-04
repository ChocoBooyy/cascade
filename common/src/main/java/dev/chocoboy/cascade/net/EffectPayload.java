package dev.chocoboy.cascade.net;

import dev.chocoboy.cascade.engine.effect.EffectSpec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public record EffectPayload(EffectSpec spec, Vec3 origin, long seed) implements CustomPacketPayload {

    public static final Type<EffectPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("cascade", "effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EffectPayload> STREAM_CODEC = StreamCodec.composite(
            SpecCodecs.EFFECT, EffectPayload::spec,
            NetCodecs.VEC3, EffectPayload::origin,
            ByteBufCodecs.VAR_LONG, EffectPayload::seed,
            EffectPayload::new);

    @Override
    public Type<EffectPayload> type() {
        return TYPE;
    }
}
