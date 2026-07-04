package dev.chocoboy.cascade.net;

import dev.chocoboy.cascade.engine.effect.BeamSpec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public record BeamPayload(BeamSpec spec, Vec3 from, Vec3 to, long seed) implements CustomPacketPayload {

    public static final Type<BeamPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("cascade", "beam"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BeamPayload> STREAM_CODEC = StreamCodec.composite(
            SpecCodecs.BEAM, BeamPayload::spec,
            NetCodecs.VEC3, BeamPayload::from,
            NetCodecs.VEC3, BeamPayload::to,
            ByteBufCodecs.VAR_LONG, BeamPayload::seed,
            BeamPayload::new);

    @Override
    public Type<BeamPayload> type() {
        return TYPE;
    }
}
