package dev.chocoboy.cascade.neoforge.net;

import dev.chocoboy.cascade.engine.effect.SdfSpec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record SdfPayload(Vec3 pos, SdfSpec spec, long seed) implements CustomPacketPayload {

    public static final Type<SdfPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("cascade", "sdf"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SdfPayload> STREAM_CODEC = StreamCodec.composite(
            NetCodecs.VEC3, SdfPayload::pos,
            SpecCodecs.SDF, SdfPayload::spec,
            ByteBufCodecs.VAR_LONG, SdfPayload::seed,
            SdfPayload::new);

    @Override
    public Type<SdfPayload> type() {
        return TYPE;
    }
}
