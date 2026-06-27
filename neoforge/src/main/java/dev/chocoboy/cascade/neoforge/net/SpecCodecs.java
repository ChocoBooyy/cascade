package dev.chocoboy.cascade.neoforge.net;

import dev.chocoboy.cascade.engine.effect.BeamSpec;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class SpecCodecs {

    private static final StreamCodec<ByteBuf, Easings> EASING =
            ByteBufCodecs.idMapper(i -> Easings.values()[i], Enum::ordinal);

    private static final StreamCodec<ByteBuf, ShapeSpec.Kind> SHAPE_KIND =
            ByteBufCodecs.idMapper(i -> ShapeSpec.Kind.values()[i], Enum::ordinal);

    private static final StreamCodec<RegistryFriendlyByteBuf, ShapeSpec> SHAPE = StreamCodec.composite(
            SHAPE_KIND, ShapeSpec::kind,
            ByteBufCodecs.FLOAT, ShapeSpec::radius,
            ByteBufCodecs.FLOAT, ShapeSpec::height,
            NetCodecs.VEC3F, ShapeSpec::a,
            NetCodecs.VEC3F, ShapeSpec::b,
            ShapeSpec::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, CurveSpec> CURVE = StreamCodec.composite(
            ByteBufCodecs.FLOAT, CurveSpec::start,
            ByteBufCodecs.FLOAT, CurveSpec::end,
            EASING, CurveSpec::ease,
            CurveSpec::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, EmitterSpec> EMITTER = StreamCodec.of(
            (buf, s) -> {
                SHAPE.encode(buf, s.shape());
                buf.writeVarInt(s.count());
                buf.writeVarInt(s.lifetime());
                buf.writeFloat(s.speed());
                CURVE.encode(buf, s.size());
                CURVE.encode(buf, s.alpha());
                buf.writeInt(s.colorStart());
                buf.writeInt(s.colorEnd());
                EASING.encode(buf, s.colorEase());
            },
            buf -> new EmitterSpec(
                    SHAPE.decode(buf), buf.readVarInt(), buf.readVarInt(), buf.readFloat(),
                    CURVE.decode(buf), CURVE.decode(buf),
                    buf.readInt(), buf.readInt(), EASING.decode(buf)));

    public static final StreamCodec<RegistryFriendlyByteBuf, BeamSpec> BEAM = StreamCodec.composite(
            ByteBufCodecs.INT, BeamSpec::color,
            ByteBufCodecs.FLOAT, BeamSpec::width,
            ByteBufCodecs.FLOAT, BeamSpec::arc,
            ByteBufCodecs.VAR_INT, BeamSpec::duration,
            ByteBufCodecs.VAR_INT, BeamSpec::segments,
            BeamSpec::new);

    private SpecCodecs() {
    }
}
